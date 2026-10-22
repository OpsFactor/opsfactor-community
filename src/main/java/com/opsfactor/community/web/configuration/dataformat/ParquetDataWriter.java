package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import org.apache.parquet.io.SeekableInputStream;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;

/** Native Parquet groups, including standard LIST/MAP encodings; no Hadoop filesystem. */
final class ParquetDataWriter {
    static void write(ObjectMapper mapper, ColumnarSchema.Field row, Iterable<?> rows,
                      Map<String, String> metadata, OutputStream output) throws IOException {
        List<Type> fields = new ArrayList<>();
        for (var child : row.children) fields.add(type(child, child.name));
        MessageType schema = new MessageType("data", fields);
        SimpleGroupFactory factory = new SimpleGroupFactory(schema);
        try (var writer = ExampleParquetWriter.builder(outputFile(output))
                .withConf(new PlainParquetConfiguration()).withType(schema).withExtraMetaData(metadata)
                .withCompressionCodec(CompressionCodecName.SNAPPY).withRowGroupSize(16L * 1024 * 1024)
                .build()) {
            for (Object object : rows) {
                JsonNode node = mapper.valueToTree(object);
                if (node == null || node.isNull()) throw new IOException("Null DTO rows are not supported");
                Group group = factory.newGroup();
                for (var child : row.children) put(group, child.name, child, node.get(child.name));
                writer.write(group);
            }
        }
    }

    private static Type type(ColumnarSchema.Field field, String name) {
        return switch (field.kind) {
            case BOOLEAN -> Types.optional(PrimitiveTypeName.BOOLEAN).named(name);
            case LONG -> Types.optional(PrimitiveTypeName.INT64).named(name);
            case DOUBLE -> Types.optional(PrimitiveTypeName.DOUBLE).named(name);
            case STRING -> Types.optional(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named(name);
            case DECIMAL -> Types.optional(PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY).length(16)
                    .as(LogicalTypeAnnotation.decimalType(field.scale, field.precision())).named(name);
            case STRUCT -> {
                List<Type> children = new ArrayList<>();
                for (var child : field.children) children.add(type(child, child.name));
                yield new GroupType(Type.Repetition.OPTIONAL, name, children);
            }
            case LIST -> Types.optionalGroup().as(LogicalTypeAnnotation.listType())
                    .addField(Types.repeatedGroup().addField(type(field.children.getFirst(), "element")).named("list"))
                    .named(name);
            case MAP -> Types.optionalGroup().as(LogicalTypeAnnotation.mapType())
                    .addField(Types.repeatedGroup()
                            .addField(Types.required(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("key"))
                            .addField(type(field.children.getFirst(), "value")).named("key_value"))
                    .named(name);
        };
    }

    private static void put(Group group, String name, ColumnarSchema.Field field, JsonNode value) {
        if (value == null || value.isNull()) return;
        switch (field.kind) {
            case BOOLEAN -> group.add(name, value.booleanValue());
            case LONG -> group.add(name, value.longValue());
            case DOUBLE -> group.add(name, value.doubleValue());
            case STRING -> group.add(name, value.asText());
            case DECIMAL -> {
                byte[] number = value.decimalValue().setScale(field.scale).unscaledValue().toByteArray();
                byte[] fixed = new byte[16];
                if (number[0] < 0) Arrays.fill(fixed, (byte) 0xff);
                System.arraycopy(number, 0, fixed, fixed.length - number.length, number.length);
                group.add(name, Binary.fromConstantByteArray(fixed));
            }
            case STRUCT -> {
                Group nested = group.addGroup(name);
                for (var child : field.children) put(nested, child.name, child, value.get(child.name));
            }
            case LIST -> {
                Group list = group.addGroup(name);
                for (JsonNode item : value) put(list.addGroup("list"), "element", field.children.getFirst(), item);
            }
            case MAP -> {
                Group map = group.addGroup(name);
                var entries = value.fields();
                while (entries.hasNext()) {
                    var entry = entries.next();
                    Group pair = map.addGroup("key_value");
                    pair.add("key", entry.getKey());
                    put(pair, "value", field.children.getFirst(), entry.getValue());
                }
            }
        }
    }

    static DataFormatCodec.Decoded read(ObjectMapper mapper, InputStream input) throws IOException {
        // Parquet footers require seeking. Spool to a private temporary file, not a giant byte array.
        Path temporary = Files.createTempFile("opsfactor-parquet-", ".parquet");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) { input.transferTo(output); }
            try (ParquetFileReader reader = ParquetFileReader.open(inputFile(temporary))) {
                MessageType schema = reader.getFooter().getFileMetaData().getSchema();
                Map<String, String> metadata = reader.getFooter().getFileMetaData().getKeyValueMetaData();
                var columns = new ColumnIOFactory().getColumnIO(schema);
                ArrayNode rows = mapper.createArrayNode();
                PageReadStore pages;
                while ((pages = reader.readNextRowGroup()) != null) {
                    try (PageReadStore rowGroup = pages) {
                        var records = columns.getRecordReader(rowGroup, new GroupRecordConverter(schema));
                        for (long index = 0; index < rowGroup.getRowCount(); index++) rows.add(object(mapper, records.read()));
                    }
                }
                return new DataFormatCodec.Decoded(rows, metadata);
            }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static ObjectNode object(ObjectMapper mapper, Group group) throws IOException {
        ObjectNode result = mapper.createObjectNode();
        for (Type field : group.getType().getFields()) result.set(field.getName(), get(mapper, group, field, 0));
        return result;
    }

    private static JsonNode get(ObjectMapper mapper, Group parent, Type type, int index) throws IOException {
        String name = type.getName();
        if (parent.getFieldRepetitionCount(name) <= index) return mapper.nullNode();
        if (!type.isPrimitive()) {
            Group group = parent.getGroup(name, index);
            if (type.getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
                ArrayNode result = mapper.createArrayNode();
                for (int i = 0; i < group.getFieldRepetitionCount("list"); i++) {
                    Group item = group.getGroup("list", i);
                    result.add(get(mapper, item, type.asGroupType().getType("list").asGroupType().getType("element"), 0));
                }
                return result;
            }
            if (type.getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
                ObjectNode result = mapper.createObjectNode();
                for (int i = 0; i < group.getFieldRepetitionCount("key_value"); i++) {
                    Group pair = group.getGroup("key_value", i);
                    String key = pair.getBinary("key", 0).toStringUsingUTF8();
                    if (result.has(key)) throw new IOException("Duplicate Parquet map key");
                    result.set(key,
                            get(mapper, pair, pair.getType().getType("value"), 0));
                }
                return result;
            }
            return object(mapper, group);
        }
        var primitive = type.asPrimitiveType();
        if (type.getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation decimal) {
            BigInteger unscaled = switch (primitive.getPrimitiveTypeName()) {
                case INT32 -> BigInteger.valueOf(parent.getInteger(name, index));
                case INT64 -> BigInteger.valueOf(parent.getLong(name, index));
                case BINARY, FIXED_LEN_BYTE_ARRAY -> new BigInteger(parent.getBinary(name, index).getBytes());
                default -> throw new IOException("Unsupported Parquet decimal physical type");
            };
            return mapper.getNodeFactory().numberNode(new BigDecimal(unscaled, decimal.getScale()));
        }
        return switch (primitive.getPrimitiveTypeName()) {
            case BOOLEAN -> mapper.getNodeFactory().booleanNode(parent.getBoolean(name, index));
            case INT32 -> mapper.getNodeFactory().numberNode(parent.getInteger(name, index));
            case INT64 -> mapper.getNodeFactory().numberNode(parent.getLong(name, index));
            case FLOAT -> mapper.getNodeFactory().numberNode(parent.getFloat(name, index));
            case DOUBLE -> mapper.getNodeFactory().numberNode(parent.getDouble(name, index));
            case BINARY -> {
                if (!(type.getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.StringLogicalTypeAnnotation))
                    throw new IOException("Unannotated binary fields require an explicit binary schema mapping");
                yield mapper.getNodeFactory().textNode(parent.getBinary(name, index).toStringUsingUTF8());
            }
            default -> throw new IOException("Unsupported Parquet physical type: " + primitive.getPrimitiveTypeName());
        };
    }

    private static OutputFile outputFile(OutputStream output) {
        return new OutputFile() {
            @Override public PositionOutputStream create(long blockSizeHint) { return positionStream(output); }
            @Override public PositionOutputStream createOrOverwrite(long blockSizeHint) { return positionStream(output); }
            @Override public boolean supportsBlockSize() { return false; }
            @Override public long defaultBlockSize() { return 0; }
        };
    }

    private static PositionOutputStream positionStream(OutputStream output) {
        return new PositionOutputStream() {
            private long position;
            @Override public long getPos() { return position; }
            @Override public void write(int value) throws IOException { output.write(value); position++; }
            @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                output.write(bytes, offset, length); position += length;
            }
            @Override public void flush() throws IOException { output.flush(); }
            @Override public void close() throws IOException { flush(); }
        };
    }

    private static InputFile inputFile(Path file) {
        return new InputFile() {
            @Override public long getLength() throws IOException { return Files.size(file); }
            @Override public SeekableInputStream newStream() throws IOException {
                RandomAccessFile random = new RandomAccessFile(file.toFile(), "r");
                return new SeekableInputStream() {
                    @Override public long getPos() throws IOException { return random.getFilePointer(); }
                    @Override public void seek(long position) throws IOException { random.seek(position); }
                    @Override public int read() throws IOException { return random.read(); }
                    @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                        return random.read(bytes, offset, length);
                    }
                    @Override public void readFully(byte[] bytes) throws IOException { random.readFully(bytes); }
                    @Override public void readFully(byte[] bytes, int offset, int length) throws IOException {
                        random.readFully(bytes, offset, length);
                    }
                    @Override public int read(ByteBuffer buffer) throws IOException { return random.getChannel().read(buffer); }
                    @Override public void readFully(ByteBuffer buffer) throws IOException {
                        while (buffer.hasRemaining()) if (read(buffer) < 0) throw new EOFException();
                    }
                    @Override public void close() throws IOException { random.close(); }
                };
            }
        };
    }
}
