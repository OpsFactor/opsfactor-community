package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Common data representation engine. The caller supplies the trusted JavaType; input metadata never
 * names a Java class. Binary envelopes use opsfactor.envelope.v1 (JSON of all non-data properties)
 * and opsfactor.shape.v1=envelope. The data property is the physical table. Plain lists need no
 * envelope metadata. Spreadsheet matrices retain their original header and cell order.
 */
public final class DataFormatCodec {
    private static final String SHAPE = "opsfactor.shape.v1";
    private static final String ENVELOPE = "opsfactor.envelope.v1";
    private static final String HEADERS = "opsfactor.headers.v1";
    private static final int MAX_METADATA_CHARACTERS = 1_048_576;
    private final ObjectMapper mapper;

    public DataFormatCodec(ObjectMapper mapper) { this.mapper = Objects.requireNonNull(mapper); }

    public void write(Object value, JavaType javaType, DataFormat format, OutputStream output,
                      Map<String, String> options) throws IOException {
        prepareWrite(value, javaType, format, options).writeTo(Objects.requireNonNull(output));
    }

    /** Prepared representation: validate its schema before HTTP headers or bytes are sent. */
    @FunctionalInterface
    public interface PreparedWrite {
        void writeTo(OutputStream output) throws IOException;
    }

    public PreparedWrite prepareWrite(Object value, JavaType javaType, DataFormat format,
                                      Map<String, String> options) throws IOException {
        Objects.requireNonNull(format);
        if (format == DataFormat.JSON) {
            // Templates historically exposed temporal cells as ISO text, even when Jackson
            // is configured to serialize DTO dates as timestamps. Keep that public contract.
            Object jsonValue = isMatrix(value, javaType) ? textualMatrix((List<?>) value) : value;
            return output -> mapper.writerFor(javaType).writeValue(nonClosing(output), jsonValue);
        }
        Map<String, String> selectedOptions = options == null ? Map.of() : options;
        if (format == DataFormat.CSV || format == DataFormat.XLSX) {
            Table table = table(value, javaType, format);
            return output -> {
                if (format == DataFormat.CSV) writeCsv(table, output, selectedOptions);
                else writeXlsx(table, output);
            };
        }
        Payload payload = payload(value, javaType);
        ColumnarSchema.Field schema = payload.schema;
        for (Object row : payload.rows) {
            if (row == null) throw new IOException("Null DTO rows are not supported");
            schema.observe(mapper.valueToTree(row));
        }
        return output -> {
            if (format == DataFormat.ARROW) ArrowDataWriter.write(mapper, schema, payload.rows, payload.metadata, output);
            else ParquetDataWriter.write(mapper, schema, payload.rows, payload.metadata, output);
        };
    }

    public Object read(InputStream input, JavaType javaType, DataFormat format) throws IOException {
        if (format == DataFormat.JSON) return mapper.readerFor(javaType).readValue(input);
        if (format == DataFormat.XLSX || format == DataFormat.CSV) {
            List<List<Object>> matrix = format == DataFormat.XLSX ? readXlsx(input) : readCsv(input);
            return fromMatrix(matrix, javaType);
        }
        Decoded decoded = format == DataFormat.ARROW ? ArrowDataWriter.read(mapper, input) : ParquetDataWriter.read(mapper, input);
        JsonNode body = decoded.rows;
        if (isMatrixType(javaType)) {
            String headerText = decoded.metadata.get(HEADERS);
            if (headerText == null) throw new IOException("Missing matrix column metadata");
            JsonNode headers = metadata(headerText);
            if (!headers.isArray()) throw new IOException("Invalid matrix column metadata");
            ArrayNode matrix = mapper.createArrayNode();
            matrix.add(headers);
            for (JsonNode row : decoded.rows) {
                ArrayNode cells = matrix.addArray();
                for (JsonNode header : headers) cells.add(row.get(header.asText()));
            }
            body = matrix;
        } else if (!javaType.isCollectionLikeType() && !javaType.isArrayType()) {
            JavaType dataType = dataType(javaType);
            if (dataType != null) {
                String envelope = decoded.metadata.get(ENVELOPE);
                ObjectNode object;
                if (envelope == null) object = mapper.createObjectNode();
                else {
                    JsonNode node = metadata(envelope);
                    if (!node.isObject() || node.has("data")) throw new IOException("Invalid binary integration envelope");
                    object = (ObjectNode) node;
                }
                object.set("data", decoded.rows);
                body = object;
            } else {
                if (decoded.rows.size() != 1) throw new IOException("Expected exactly one DTO row");
                body = decoded.rows.get(0);
            }
        } else if ("envelope".equals(decoded.metadata.get(SHAPE))) {
            throw new IOException("An integration envelope cannot be silently discarded when reading a list");
        }
        return mapper.readerFor(javaType).readValue(mapper.treeAsTokens(body));
    }

    record Decoded(ArrayNode rows, Map<String, String> metadata) { }
    private record Payload(Iterable<?> rows, ColumnarSchema.Field schema, Map<String, String> metadata) { }
    private record Table(List<?> rows, List<String> headers, boolean matrix) { }

    private Payload payload(Object value, JavaType type) throws IOException {
        if (isMatrix(value, type)) return matrixPayload((List<?>) value);
        Map<String, String> metadata = new LinkedHashMap<>();
        Object rows = value;
        JavaType rowType;
        if (type.isCollectionLikeType() || type.isArrayType()) {
            rowType = type.getContentType();
            metadata.put(SHAPE, "list");
        } else {
            JavaType data = dataType(type);
            if (data != null) {
                ObjectNode envelope = mapper.createObjectNode();
                Object extracted = null;
                for (BeanPropertyDefinition property : properties(type)) {
                    try {
                        property.getAccessor().fixAccess(true);
                        Object propertyValue = property.getAccessor().getValue(value);
                        if (property.getName().equals("data")) extracted = propertyValue;
                        else envelope.set(property.getName(), mapper.valueToTree(propertyValue));
                    } catch (RuntimeException exception) { throw new IOException("Cannot read integration envelope", exception); }
                }
                if (extracted == null) throw new IOException("Integration envelope data must be a non-null collection");
                rows = extracted;
                rowType = data.getContentType();
                metadata.put(SHAPE, "envelope");
                String text = mapper.writeValueAsString(envelope);
                if (text.length() > MAX_METADATA_CHARACTERS) throw new IOException("Integration envelope metadata is too large");
                metadata.put(ENVELOPE, text);
            } else {
                rows = Collections.singletonList(value);
                rowType = type;
                metadata.put(SHAPE, "single");
            }
        }
        Iterable<?> iterable = iterable(rows);
        return new Payload(iterable, ColumnarSchema.of(mapper, rowType), metadata);
    }

    private Payload matrixPayload(List<?> matrix) throws IOException {
        if (matrix.isEmpty()) throw new IOException("A matrix must include its header row");
        List<String> headers = matrixHeaders(matrix);
        ColumnarSchema.Field root = new ColumnarSchema.Field("row", ColumnarSchema.Kind.STRUCT);
        for (String header : headers) root.children.add(new ColumnarSchema.Field(header, ColumnarSchema.Kind.STRING));
        boolean[] observed = new boolean[headers.size()];
        for (int r = 1; r < matrix.size(); r++) {
            List<?> cells = matrixRow(matrix.get(r));
            if (cells.size() > headers.size()) throw new IOException("Matrix row exceeds header width");
            for (int c = 0; c < cells.size(); c++) {
                Object cell = cells.get(c);
                if (cell == null) continue;
                var kind = scalarKind(cell);
                var field = root.children.get(c);
                if (!observed[c]) { field.kind = kind; observed[c] = true; }
                else if (field.kind != kind) {
                    if ((field.kind == ColumnarSchema.Kind.LONG && kind == ColumnarSchema.Kind.DECIMAL)
                            || (field.kind == ColumnarSchema.Kind.DECIMAL && kind == ColumnarSchema.Kind.LONG))
                        field.kind = ColumnarSchema.Kind.DECIMAL;
                    else throw new IOException("Mixed incompatible types in column " + headers.get(c));
                }
            }
        }
        Iterable<Object> rows = () -> new Iterator<>() {
            int index = 1;
            @Override public boolean hasNext() { return index < matrix.size(); }
            @Override public Object next() {
                List<?> cells = (List<?>) matrix.get(index++);
                Map<String, Object> row = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) row.put(headers.get(c), c < cells.size() ? structuredMatrixValue(cells.get(c)) : null);
                return row;
            }
        };
        return new Payload(rows, root, Map.of(SHAPE, "matrix", HEADERS, mapper.writeValueAsString(headers)));
    }

    private static List<?> textualMatrix(List<?> matrix) {
        return new AbstractList<>() {
            @Override public int size() { return matrix.size(); }
            @Override public Object get(int index) {
                Object row = matrix.get(index);
                return row instanceof List<?> cells ? cells.stream().map(DataFormatCodec::structuredMatrixValue).toList() : row;
            }
        };
    }

    /** Preserve the pre-existing textual temporal contract of Arrow/Parquet matrices. */
    private static Object structuredMatrixValue(Object value) {
        return DataFormatTemporalValues.isDate(value) ? value.toString() : value;
    }

    private static ColumnarSchema.Kind scalarKind(Object value) throws IOException {
        if (value instanceof Boolean) return ColumnarSchema.Kind.BOOLEAN;
        if (value instanceof BigDecimal || value instanceof java.math.BigInteger) return ColumnarSchema.Kind.DECIMAL;
        if (value instanceof Float || value instanceof Double) return ColumnarSchema.Kind.DOUBLE;
        if (value instanceof Number) return ColumnarSchema.Kind.LONG;
        if (value instanceof CharSequence || value instanceof Character || value instanceof Enum<?>
                || DataFormatTemporalValues.isDate(value)) return ColumnarSchema.Kind.STRING;
        throw new IOException("Unsupported matrix cell type: " + value.getClass().getSimpleName());
    }

    private Table table(Object value, JavaType type, DataFormat format) throws IOException {
        if (isMatrix(value, type)) {
            List<?> matrix = (List<?>) value;
            if (matrix.isEmpty()) return new Table(List.of(), List.of(), true);
            List<String> headers = matrixHeaders(matrix);
            // Templates already define a flat table. Check dynamic cells before a CSV
            // header is flushed; never stringify a nested object or send a partial file.
            for (int rowIndex = 1; rowIndex < matrix.size(); rowIndex++) {
                List<?> row;
                try { row = matrixRow(matrix.get(rowIndex)); }
                catch (IllegalArgumentException exception) {
                    throw new TabularExportException(format, "data[" + rowIndex + "]");
                }
                for (int column = 0; column < row.size(); column++) {
                    Object cell = row.get(column);
                    if (cell != null && !(cell instanceof Number || cell instanceof Boolean
                            || cell instanceof CharSequence || cell instanceof Character || cell instanceof Enum<?>
                            || cell instanceof java.time.temporal.TemporalAccessor || cell instanceof Date)) {
                        throw new TabularExportException(format, column < headers.size() ? headers.get(column) : "column " + column);
                    }
                }
            }
            return new Table(matrix, headers, true);
        }
        // Inspect DTO property types before constructing a recursive columnar schema:
        // a null nested DTO, a raw collection or a recursive child is still hierarchical.
        JavaType rowType = type.isCollectionLikeType() || type.isArrayType() ? type.getContentType() : type;
        JavaType envelopeData = dataType(type);
        if (envelopeData != null) rowType = envelopeData.getContentType();
        requireFlatProperties(rowType, format, "", new HashSet<>());
        Payload payload = payload(value, type);
        List<String> names = new ArrayList<>();
        for (var field : payload.schema.children) {
            if (field.kind == ColumnarSchema.Kind.STRUCT || field.kind == ColumnarSchema.Kind.LIST || field.kind == ColumnarSchema.Kind.MAP)
                throw new TabularExportException(format, field.name);
            names.add(field.name);
        }
        List<?> rows = payload.rows instanceof List<?> list ? list : toList(payload.rows);
        return new Table(rows, names, false);
    }

    /** Unwrapped keys are already flat in JSON; all other structured properties are rejected. */
    private void requireFlatProperties(JavaType type, DataFormat format, String path, Set<JavaType> visited)
            throws IOException {

        if (!visited.add(type)) throw new TabularExportException(format, path);
        for (BeanPropertyDefinition property : properties(type)) {
            JavaType propertyType = property.getPrimaryType();
            String field = path.isEmpty() ? property.getName() : path + "." + property.getName();
            var member = property.getPrimaryMember();
            JsonUnwrapped unwrapped = member == null ? null : member.getAnnotation(JsonUnwrapped.class);
            if (unwrapped != null && unwrapped.enabled()) {
                requireFlatProperties(propertyType, format, field, visited);
                continue;
            }
            Class<?> raw = propertyType.getRawClass();
            if (!(raw.isPrimitive() || Number.class.isAssignableFrom(raw) || raw == Boolean.class
                    || CharSequence.class.isAssignableFrom(raw) || raw == Character.class || raw.isEnum()
                    || raw == UUID.class || raw == byte[].class || Date.class.isAssignableFrom(raw)
                    || java.time.temporal.TemporalAccessor.class.isAssignableFrom(raw))) {
                throw new TabularExportException(format, field);
            }
        }
        visited.remove(type);

    }

    private List<?> cells(Table table, Object object, boolean nativeDates) throws IOException {
        if (table.matrix) return matrixRow(object);
        JsonNode node = mapper.valueToTree(object);
        List<Object> result = new ArrayList<>();
        for (String header : table.headers) {
            JsonNode cell = node.get(header);
            Object original = nativeDates ? DataFormatTemporalValues.value(mapper, object, header) : null;
            if (DataFormatTemporalValues.isDate(original)) result.add(original);
            else if (cell == null || cell.isNull()) result.add(null);
            else if (cell.isTextual()) result.add(cell.textValue());
            else if (cell.isBoolean()) result.add(cell.booleanValue());
            // Keep floating-point values distinct from exact decimals for XLSX cell typing.
            else if (cell.isNumber()) result.add(cell.numberValue());
            else throw new IOException("Non-scalar spreadsheet cell at " + header);
        }
        return result;
    }

    private void writeXlsx(Table table, OutputStream output) throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(200);
        workbook.setCompressTempFiles(true);
        try {
            Sheet sheet = workbook.createSheet("Data");
            // Reuse workbook styles: creating one per cell exhausts Excel's style limit.
            Map<String, CellStyle> dateStyles = new HashMap<>();
            int rowIndex = 0;
            if (!table.matrix && !table.headers.isEmpty()) writeXlsxRow(sheet.createRow(rowIndex++), table.headers, dateStyles);
            for (Object row : table.rows) {
                if (rowIndex >= 1_048_576) throw new IOException("XLSX exceeds the worksheet row limit; choose CSV, Arrow or Parquet");
                List<?> values = cells(table, row, true);
                if (values.size() > 16_384) throw new IOException("XLSX exceeds the worksheet column limit");
                writeXlsxRow(sheet.createRow(rowIndex++), values, dateStyles);
            }
            workbook.write(nonClosing(output));
        } finally {
            try { workbook.close(); } finally { workbook.dispose(); }
        }
    }

    private void writeXlsxRow(Row row, List<?> values, Map<String, CellStyle> dateStyles) throws IOException {
        for (int column = 0; column < values.size(); column++) {
            Object value = values.get(column);
            Cell cell = row.createCell(column);
            if (value == null) continue;
            if (DataFormatTemporalValues.isDate(value)) {
                String pattern;
                if (value instanceof LocalTime || value instanceof java.sql.Time) {
                    LocalTime time = value instanceof LocalTime local ? local : ((java.sql.Time) value).toLocalTime();
                    cell.setCellValue(time.toNanoOfDay() / 86_400_000_000_000d);
                    pattern = "hh:mm:ss.000";
                } else if (value instanceof LocalDate date) {
                    cell.setCellValue(date);
                    pattern = "yyyy-mm-dd";
                } else if (value instanceof java.sql.Date date) {
                    cell.setCellValue(date.toLocalDate());
                    pattern = "yyyy-mm-dd";
                } else {
                    LocalDateTime dateTime = value instanceof LocalDateTime local ? local
                            : value instanceof java.sql.Timestamp timestamp ? timestamp.toLocalDateTime()
                            : ((Date) value).toInstant().atZone(mapper.getSerializationConfig().getTimeZone().toZoneId()).toLocalDateTime();
                    cell.setCellValue(dateTime);
                    pattern = "yyyy-mm-dd hh:mm:ss.000";
                }
                if (!DateUtil.isValidExcelDate(cell.getNumericCellValue())) throw new IOException("Date is outside Excel's supported range");
                CellStyle style = dateStyles.computeIfAbsent(pattern, key -> {
                    CellStyle created = row.getSheet().getWorkbook().createCellStyle();
                    created.setDataFormat(row.getSheet().getWorkbook().createDataFormat().getFormat(key));
                    return created;
                });
                cell.setCellStyle(style);
            } else if (value instanceof Boolean bool) cell.setCellValue(bool);
            else if (value instanceof Number number) {
                // Java's native numeric types remain numeric in Excel, including Long.
                // Excel applies its own numeric precision; textual identifiers stay text.
                if (number instanceof Byte || number instanceof Short || number instanceof Integer
                        || number instanceof Long || number instanceof Float || number instanceof Double) {
                    double numericValue = number.doubleValue();
                    if (!Double.isFinite(numericValue)) throw new IOException("Non-finite spreadsheet number");
                    cell.setCellValue(numericValue);
                    continue;
                }
                BigDecimal decimal;
                try { decimal = new BigDecimal(number.toString()); }
                catch (NumberFormatException exception) { throw new IOException("Non-finite spreadsheet number", exception); }
                // Excel stores 15 significant decimal digits. Keep larger exact numbers as text.
                if (decimal.precision() > 15) cell.setCellValue(decimal.toPlainString());
                else cell.setCellValue(number.doubleValue());
            } else if (value instanceof CharSequence || value instanceof Character || value instanceof Enum<?>
                    || value instanceof java.time.temporal.TemporalAccessor || value instanceof Date)
                cell.setCellValue(value.toString());
            else throw new IOException("Non-scalar spreadsheet cell: " + value.getClass().getSimpleName());
        }
    }

    private void writeCsv(Table table, OutputStream output, Map<String, String> options) throws IOException {
        char separator = separator(options.getOrDefault("fieldSeparator", options.getOrDefault("csvDelimiter", ",")));
        String decimal = options.getOrDefault("decimalSeparator", options.getOrDefault("csvDecimalSeparator", "."));
        if ("dot".equals(decimal)) decimal = ".";
        if ("comma".equals(decimal)) decimal = ",";
        if (!decimal.equals(".") && !decimal.equals(",")) throw new IOException("Invalid CSV decimal separator");
        String lineEnding = options.getOrDefault("csvLineEnding", "lf");
        if (!lineEnding.equals("lf") && !lineEnding.equals("crlf")) throw new IOException("Invalid CSV line ending");
        String newline = lineEnding.equals("crlf") ? "\r\n" : "\n";
        Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        if (!table.matrix && !table.headers.isEmpty() && !"false".equals(options.get("csvIncludeHeaders")))
            csvRow(writer, table.headers, separator, decimal, newline);
        for (Object row : table.rows) csvRow(writer, cells(table, row, false), separator, decimal, newline);
        writer.flush();
    }

    private static char separator(String text) throws IOException {
        if ("tab".equals(text) || "\\t".equals(text)) return '\t';
        if (text == null || text.length() != 1 || text.charAt(0) == '"' || text.charAt(0) == '\r' || text.charAt(0) == '\n')
            throw new IOException("CSV field separator must be one character other than quote/newline");
        return text.charAt(0);
    }

    private static void csvRow(Writer writer, List<?> values, char separator, String decimal, String newline) throws IOException {
        for (int column = 0; column < values.size(); column++) {
            if (column > 0) writer.write(separator);
            Object value = values.get(column);
            if (value == null) continue;
            if (!(value instanceof CharSequence || value instanceof Character || value instanceof Number
                    || value instanceof Boolean || value instanceof Enum<?> || value instanceof java.time.temporal.TemporalAccessor
                    || value instanceof Date)) throw new IOException("Non-scalar CSV cell");
            String text = value instanceof BigDecimal number ? number.toPlainString() : value.toString();
            if (value instanceof Number && decimal.equals(",")) text = text.replace('.', ',');
            if (text.indexOf(separator) >= 0 || text.indexOf('"') >= 0 || text.indexOf('\r') >= 0 || text.indexOf('\n') >= 0) {
                writer.write('"'); writer.write(text.replace("\"", "\"\"")); writer.write('"');
            } else writer.write(text);
        }
        writer.write(newline);
    }

    private List<List<Object>> readXlsx(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new FilterInputStream(input) { @Override public void close() { } })) {
            List<List<Object>> matrix = new ArrayList<>();
            if (workbook.getNumberOfSheets() == 0) return matrix;
            Sheet sheet = workbook.getSheetAt(0);
            int width = sheet.getRow(0) == null ? 0 : sheet.getRow(0).getLastCellNum();
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                List<Object> cells = new ArrayList<>();
                int columns = row == null ? width : Math.max(width, row.getLastCellNum());
                for (int c = 0; c < columns; c++) {
                    Cell cell = row == null ? null : row.getCell(c);
                    if (cell == null || cell.getCellType() == CellType.BLANK) cells.add(null);
                    else if (cell.getCellType() == CellType.STRING) cells.add(cell.getStringCellValue());
                    else if (cell.getCellType() == CellType.BOOLEAN) cells.add(cell.getBooleanCellValue());
                    else if (cell.getCellType() == CellType.NUMERIC) {
                        // Excel dates are numeric serials plus a date/time style, never plain decimals.
                        cells.add(DateUtil.isCellDateFormatted(cell) ? cell.getLocalDateTimeCellValue()
                                : BigDecimal.valueOf(cell.getNumericCellValue()));
                    }
                    else throw new IOException("Formula/error cells are not accepted as integration values");
                }
                matrix.add(cells);
            }
            return matrix;
        }
    }

    private List<List<Object>> readCsv(InputStream input) throws IOException {
        PushbackReader reader = new PushbackReader(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)), 8192);
        int firstCharacter = reader.read();
        if (firstCharacter >= 0 && firstCharacter != '\uFEFF') reader.unread(firstCharacter);
        StringBuilder sample = new StringBuilder();
        boolean quoted = false;
        while (sample.length() < 8192) {
            int character = reader.read();
            if (character < 0) break;
            sample.append((char) character);
            if (character == '"') quoted = !quoted;
            if (character == '\n' && !quoted) break;
        }
        reader.unread(sample.toString().toCharArray());
        char separator = detectSeparator(sample.toString());
        List<List<Object>> rows = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        quoted = false;
        boolean afterQuotedCell = false;
        boolean touched = false;
        int character;
        while ((character = reader.read()) >= 0) {
            touched = true;
            char ch = (char) character;
            if (afterQuotedCell && ch != separator && ch != '\n' && ch != '\r')
                throw new IOException("Unexpected character after CSV closing quote");
            if (ch == '"') {
                if (quoted) {
                    int next = reader.read();
                    if (next == '"') cell.append('"');
                    else { quoted = false; afterQuotedCell = true; if (next >= 0) reader.unread(next); }
                } else if (cell.isEmpty()) quoted = true;
                else throw new IOException("Malformed CSV quote");
            } else if (!quoted && ch == separator) { row.add(cell.toString()); cell.setLength(0); afterQuotedCell = false; }
            else if (!quoted && (ch == '\n' || ch == '\r')) {
                if (ch == '\r') { int next = reader.read(); if (next >= 0 && next != '\n') reader.unread(next); }
                row.add(cell.toString()); rows.add(row); row = new ArrayList<>(); cell.setLength(0); touched = false; afterQuotedCell = false;
            } else cell.append(ch);
        }
        if (quoted) throw new IOException("Unclosed CSV quote");
        if (touched || !row.isEmpty() || !cell.isEmpty()) { row.add(cell.toString()); rows.add(row); }
        if (!rows.isEmpty() && !rows.getFirst().isEmpty()) {
            Object first = rows.getFirst().getFirst();
            if (first instanceof String text && text.startsWith("\uFEFF")) rows.getFirst().set(0, text.substring(1));
        }
        return rows;
    }

    private static char detectSeparator(String sample) {
        int[] counts = new int[4]; char[] separators = { ',', ';', '\t', '|' }; boolean quoted = false;
        for (char character : sample.toCharArray()) {
            if (character == '"') quoted = !quoted;
            else if (!quoted) for (int i = 0; i < separators.length; i++) if (character == separators[i]) counts[i]++;
        }
        int best = 0;
        for (int i = 1; i < counts.length; i++) if (counts[i] > counts[best]) best = i;
        return separators[best];
    }

    private Object fromMatrix(List<List<Object>> matrix, JavaType type) throws IOException {
        if (isMatrixType(type)) return mapper.convertValue(matrix, type);
        JavaType rowType = type.isCollectionLikeType() || type.isArrayType() ? type.getContentType() : type;
        JavaType envelopeDataType = dataType(type);
        if (envelopeDataType != null) rowType = envelopeDataType.getContentType();
        Map<String, ColumnarSchema.Kind> kinds = new HashMap<>();
        for (var field : ColumnarSchema.of(mapper, rowType).children) kinds.put(field.name, field.kind);
        ArrayNode rows = mapper.createArrayNode();
        if (!matrix.isEmpty()) {
            List<String> headers = matrixHeaders(matrix);
            for (int i = 1; i < matrix.size(); i++) {
                List<?> cells = matrix.get(i);
                if (cells.size() > headers.size()) throw new IOException("Row exceeds header width");
                ObjectNode row = rows.addObject();
                for (int c = 0; c < headers.size(); c++) {
                    Object cell = c < cells.size() ? cells.get(c) : null;
                    var kind = kinds.get(headers.get(c));
                    if (cell instanceof LocalDateTime dateTime) {
                        cell = DataFormatTemporalValues.imported(mapper, dateTime,
                                DataFormatTemporalValues.propertyType(mapper, rowType, headers.get(c)));
                    }
                    if (cell instanceof String text && !text.isEmpty()) {
                        if (kind == ColumnarSchema.Kind.DECIMAL || kind == ColumnarSchema.Kind.DOUBLE || kind == ColumnarSchema.Kind.LONG) {
                            // A decimal comma is unambiguous here: the target property is numeric,
                            // and CSV delimiters were already parsed. Never rewrite text/SKU fields.
                            String number = text.matches("[-+]?\\d+,\\d+([eE][-+]?\\d+)?") ? text.replace(',', '.') : text;
                            try { cell = new BigDecimal(number); }
                            catch (NumberFormatException exception) { throw new IOException("Invalid numeric value in " + headers.get(c), exception); }
                        }
                    }
                    if (kind == ColumnarSchema.Kind.LONG && cell instanceof Number number) {
                        try { cell = new BigDecimal(number.toString()).longValueExact(); }
                        catch (ArithmeticException | NumberFormatException exception) {
                            throw new IOException("Expected an exact signed 64-bit integer in " + headers.get(c), exception);
                        }
                    }
                    row.set(headers.get(c), cell instanceof String text && text.isEmpty() ? mapper.nullNode() : mapper.valueToTree(cell));
                }
            }
        }
        JsonNode body = rows;
        if (!type.isCollectionLikeType() && !type.isArrayType()) {
            if (dataType(type) != null) { ObjectNode envelope = mapper.createObjectNode(); envelope.set("data", rows); body = envelope; }
            else if (rows.size() == 1) body = rows.get(0);
            else throw new IOException("Expected one spreadsheet record");
        }
        return mapper.readerFor(type).readValue(mapper.treeAsTokens(body));
    }

    private List<BeanPropertyDefinition> properties(JavaType type) {
        return mapper.getSerializationConfig().introspect(type).findProperties().stream()
                .filter(property -> property.couldSerialize() && property.getAccessor() != null).toList();
    }

    private JavaType dataType(JavaType type) {
        for (var property : properties(type)) {
            if (property.getName().equals("data") && (property.getPrimaryType().isCollectionLikeType() || property.getPrimaryType().isArrayType()))
                return property.getPrimaryType();
        }
        return null;
    }

    private JsonNode metadata(String value) throws IOException {
        if (value.length() > MAX_METADATA_CHARACTERS) throw new IOException("Binary metadata is too large");
        return mapper.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(value);
    }

    private static boolean isMatrix(Object value, JavaType type) {
        return value instanceof List<?> list && (isMatrixType(type) || !list.isEmpty() && list.getFirst() instanceof List<?>);
    }
    private static boolean isMatrixType(JavaType type) {
        return type.isCollectionLikeType() && type.getContentType() != null && type.getContentType().isCollectionLikeType();
    }
    private static List<?> matrixRow(Object row) throws IOException {
        if (!(row instanceof List<?> cells)) throw new IOException("Matrix rows must be lists");
        return cells;
    }
    private static List<String> matrixHeaders(List<?> matrix) throws IOException {
        if (matrix.isEmpty()) return List.of();
        List<String> headers = new ArrayList<>(); Set<String> unique = new HashSet<>();
        for (Object header : matrixRow(matrix.getFirst())) {
            if (!(header instanceof String name) || name.isBlank() || !unique.add(name))
                throw new IOException("Column headers must be unique nonblank strings");
            headers.add(name);
        }
        return headers;
    }
    private static Iterable<?> iterable(Object object) throws IOException {
        if (object instanceof Iterable<?> iterable) return iterable;
        if (object != null && object.getClass().isArray()) {
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < java.lang.reflect.Array.getLength(object); i++) values.add(java.lang.reflect.Array.get(object, i));
            return values;
        }
        throw new IOException("Data must be a collection");
    }
    private static List<Object> toList(Iterable<?> rows) { List<Object> values = new ArrayList<>(); rows.forEach(values::add); return values; }
    private static OutputStream nonClosing(OutputStream stream) {
        return new FilterOutputStream(stream) { @Override public void close() throws IOException { flush(); } };
    }
}
