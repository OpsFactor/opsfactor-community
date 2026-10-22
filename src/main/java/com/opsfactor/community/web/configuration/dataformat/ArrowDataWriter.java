package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.MapVector;
import org.apache.arrow.vector.complex.StructVector;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;

/** Arrow IPC stream: one HTTP response, internally bounded vector batches. */
final class ArrowDataWriter {
    private static final int BATCH_SIZE = 2048;

    static void write(ObjectMapper mapper, ColumnarSchema.Field row, Iterable<?> rows,
                      Map<String, String> metadata, OutputStream output) throws IOException {
        List<Field> fields = new ArrayList<>();
        for (var child : row.children) fields.add(field(child));
        try (RootAllocator allocator = new RootAllocator();
             VectorSchemaRoot root = VectorSchemaRoot.create(new Schema(fields, metadata), allocator);
             ArrowStreamWriter writer = new ArrowStreamWriter(root, null, new FilterOutputStream(output) {
                 @Override public void close() throws IOException { flush(); }
             })) {
            writer.start();
            root.allocateNew();
            int index = 0;
            for (Object object : rows) {
                JsonNode node = mapper.valueToTree(object);
                if (node == null || node.isNull()) throw new IOException("Null DTO rows are not supported");
                for (int column = 0; column < row.children.size(); column++) {
                    var child = row.children.get(column);
                    put(root.getVector(column), child, index, node.get(child.name));
                }
                if (++index == BATCH_SIZE) {
                    root.setRowCount(index);
                    writer.writeBatch();
                    root.clear();
                    root.allocateNew();
                    index = 0;
                }
            }
            if (index > 0) {
                root.setRowCount(index);
                writer.writeBatch();
            }
            writer.end();
        }
    }

    private static Field field(ColumnarSchema.Field column) {
        List<Field> children = new ArrayList<>();
        ArrowType type = switch (column.kind) {
            case BOOLEAN -> ArrowType.Bool.INSTANCE;
            case LONG -> new ArrowType.Int(64, true);
            case DOUBLE -> new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE);
            case STRING -> ArrowType.Utf8.INSTANCE;
            case DECIMAL -> new ArrowType.Decimal(column.precision(), column.scale, 128);
            case STRUCT -> {
                for (var child : column.children) children.add(field(child));
                yield ArrowType.Struct.INSTANCE;
            }
            case LIST -> {
                children.add(field(column.children.getFirst()));
                yield ArrowType.List.INSTANCE;
            }
            case MAP -> {
                Field key = new Field(MapVector.KEY_NAME, FieldType.notNullable(ArrowType.Utf8.INSTANCE), List.of());
                Field originalValue = field(column.children.getFirst());
                Field value = new Field(MapVector.VALUE_NAME, originalValue.getFieldType(), originalValue.getChildren());
                children.add(new Field(MapVector.DATA_VECTOR_NAME,
                        FieldType.notNullable(ArrowType.Struct.INSTANCE), List.of(key, value)));
                yield new ArrowType.Map(false);
            }
        };
        return new Field(column.name, FieldType.nullable(type), children);
    }

    private static void put(FieldVector vector, ColumnarSchema.Field field, int index, JsonNode value)
            throws IOException {
        if (value == null || value.isNull()) { vector.setNull(index); return; }
        switch (field.kind) {
            case BOOLEAN -> ((BitVector) vector).setSafe(index, value.booleanValue() ? 1 : 0);
            case LONG -> ((BigIntVector) vector).setSafe(index, value.longValue());
            case DOUBLE -> ((Float8Vector) vector).setSafe(index, value.doubleValue());
            case DECIMAL -> ((DecimalVector) vector).setSafe(index, value.decimalValue().setScale(field.scale));
            case STRING -> ((VarCharVector) vector).setSafe(index, value.asText().getBytes(StandardCharsets.UTF_8));
            case STRUCT -> {
                StructVector struct = (StructVector) vector;
                struct.setIndexDefined(index);
                for (var child : field.children) put(struct.getChild(child.name), child, index, value.get(child.name));
            }
            case LIST -> {
                ListVector list = (ListVector) vector;
                int start = list.startNewValue(index);
                int length = 0;
                for (JsonNode item : value) put(list.getDataVector(), field.children.getFirst(), start + length++, item);
                list.endValue(index, length);
            }
            case MAP -> {
                MapVector map = (MapVector) vector;
                int start = map.startNewValue(index);
                StructVector entries = (StructVector) map.getDataVector();
                int length = 0;
                var items = value.fields();
                while (items.hasNext()) {
                    var entry = items.next();
                    int offset = start + length++;
                    entries.setIndexDefined(offset);
                    ((VarCharVector) entries.getChild(MapVector.KEY_NAME)).setSafe(offset,
                            entry.getKey().getBytes(StandardCharsets.UTF_8));
                    put(entries.getChild(MapVector.VALUE_NAME), field.children.getFirst(), offset, entry.getValue());
                }
                map.endValue(index, length);
            }
        }
    }

    static DataFormatCodec.Decoded read(ObjectMapper mapper, InputStream input) throws IOException {
        ArrayNode rows = mapper.createArrayNode();
        try (RootAllocator allocator = new RootAllocator();
             ArrowStreamReader reader = new ArrowStreamReader(new FilterInputStream(input) {
                 @Override public void close() { /* HTTP input is owned by Spring. */ }
             }, allocator)) {
            VectorSchemaRoot root = reader.getVectorSchemaRoot();
            Map<String, String> metadata = root.getSchema().getCustomMetadata();
            while (reader.loadNextBatch()) {
                for (int index = 0; index < root.getRowCount(); index++) {
                    ObjectNode row = rows.addObject();
                    for (FieldVector vector : root.getFieldVectors()) row.set(vector.getName(), get(mapper, vector, index));
                }
            }
            return new DataFormatCodec.Decoded(rows, metadata);
        }
    }

    private static JsonNode get(ObjectMapper mapper, FieldVector vector, int index) throws IOException {
        if (vector.isNull(index)) return mapper.nullNode();
        if (vector instanceof MapVector map) {
            ObjectNode object = mapper.createObjectNode();
            StructVector entries = (StructVector) map.getDataVector();
            for (int i = map.getElementStartIndex(index); i < map.getElementEndIndex(index); i++) {
                Object key = entries.getChild(MapVector.KEY_NAME).getObject(i);
                if (key == null || object.has(key.toString())) throw new IOException("Null or duplicate Arrow map key");
                object.set(key.toString(), get(mapper, entries.getChild(MapVector.VALUE_NAME), i));
            }
            return object;
        }
        if (vector instanceof ListVector list) {
            ArrayNode array = mapper.createArrayNode();
            for (int i = list.getElementStartIndex(index); i < list.getElementEndIndex(index); i++)
                array.add(get(mapper, list.getDataVector(), i));
            return array;
        }
        if (vector instanceof StructVector struct) {
            ObjectNode object = mapper.createObjectNode();
            for (FieldVector child : struct.getChildrenFromFields()) object.set(child.getName(), get(mapper, child, index));
            return object;
        }
        Object value = vector.getObject(index);
        if (value instanceof org.apache.arrow.vector.util.Text text) return mapper.getNodeFactory().textNode(text.toString());
        return mapper.valueToTree(value);
    }
}
