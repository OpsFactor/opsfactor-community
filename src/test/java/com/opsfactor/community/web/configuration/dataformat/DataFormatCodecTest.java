package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataFormatCodecTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final DataFormatCodec codec = new DataFormatCodec(mapper);

    enum State { @JsonProperty("approved-state") APPROVED }
    static class Key {
        public String material;
        public String location;
        Key() { }
        Key(String material, String location) { this.material = material; this.location = location; }
    }
    static class Child {
        public String name;
        public BigDecimal amount;
        Child() { }
        Child(String name, String amount) { this.name = name; this.amount = new BigDecimal(amount); }
    }
    static class Row {
        @JsonUnwrapped public Key key;
        @JsonProperty("output_quantity") public BigDecimal quantity;
        public Child child;
        public List<Child> alternatives;
        public Map<String, Child> factories;
        public LocalDate date;
        public State state;
        public Boolean enabled;
        public Long nullable;
        @JsonIgnore public String internal = "must-not-be-exported";
    }
    static class Envelope<T> {
        public List<T> data;
        public Map<String, Object> options;
        public Map<String, Object> dataFilter;
        public String threadSync;
    }
    static class FlatRow {
        public String id;
        public BigDecimal amount;
        public LocalDate date;
    }
    static class WithList { public List<Child> inputs; }
    static class WithMap { public Map<String, Object> inputs; }
    static class RecursiveRow { public RecursiveRow child; }

    private Row row() {
        Row row = new Row();
        row.key = new Key("20104414", "1110");
        row.quantity = new BigDecimal("12345678901234567890.12345678901234567");
        row.child = new Child("bobina", "3.912");
        row.alternatives = Arrays.asList(new Child("MP1", "2.00001"), null, new Child("MP2", "-0.002"));
        row.factories = new LinkedHashMap<>(); row.factories.put("A", new Child("MP A", "5.25")); row.factories.put("B", null);
        row.date = LocalDate.of(2026, 10, 22);
        row.state = State.APPROVED;
        row.enabled = true;
        return row;
    }

    @Test void defaultIsJsonAndUnknownFormatsAreRejected() {
        assertEquals(DataFormat.JSON, DataFormat.fromParameter(null));
        assertEquals(DataFormat.JSON, DataFormat.fromParameter(" "));
        assertEquals(DataFormat.ARROW, DataFormat.fromParameter("Arrow"));
        assertThrows(IllegalArgumentException.class, () -> DataFormat.fromParameter("xls"));
    }

    @Test void columnarFormatsRoundTripNestedDtosUnwrappedKeysEnumsDatesAndExactDecimals() throws Exception {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Row.class);
        for (DataFormat format : List.of(DataFormat.ARROW, DataFormat.PARQUET)) {
            byte[] bytes = encode(List.of(row()), type, format, Map.of());
            if (format == DataFormat.PARQUET) assertEquals("PAR1", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
            else assertArrayEquals(new byte[] {-1, -1, -1, -1}, Arrays.copyOf(bytes, 4));
            @SuppressWarnings("unchecked") List<Row> restored = (List<Row>) codec.read(new ByteArrayInputStream(bytes), type, format);
            assertEquals(1, restored.size());
            Row actual = restored.getFirst();
            assertEquals("20104414", actual.key.material);
            assertEquals("1110", actual.key.location);
            assertEquals(0, row().quantity.compareTo(actual.quantity), format.toString());
            assertEquals(0, row().child.amount.compareTo(actual.child.amount));
            assertEquals(3, actual.alternatives.size());
            assertNull(actual.alternatives.get(1));
            assertEquals(0, new BigDecimal("-0.002").compareTo(actual.alternatives.get(2).amount));
            assertTrue(actual.factories.containsKey("B"));
            assertNull(actual.factories.get("B"));
            assertEquals(LocalDate.of(2026, 10, 22), actual.date);
            assertEquals(State.APPROVED, actual.state);
            assertEquals(Boolean.TRUE, actual.enabled);
            assertNull(actual.nullable);
        }
    }

    @Test void emptyCollectionsStillHaveAConcreteSchemaAndCanBeRead() throws Exception {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Row.class);
        for (DataFormat format : List.of(DataFormat.ARROW, DataFormat.PARQUET)) {
            byte[] bytes = encode(List.of(), type, format, Map.of());
            assertEquals(List.of(), codec.read(new ByteArrayInputStream(bytes), type, format));
            if (format == DataFormat.ARROW) {
                try (RootAllocator allocator = new RootAllocator(); ArrowStreamReader reader = new ArrowStreamReader(new ByteArrayInputStream(bytes), allocator)) {
                    var schema = reader.getVectorSchemaRoot().getSchema();
                    assertNotNull(schema.findField("material"));
                    assertNotNull(schema.findField("output_quantity"));
                    assertFalse(schema.getFields().stream().anyMatch(field -> field.getName().equals("internal")));
                    assertFalse(reader.loadNextBatch());
                }
            }
        }
    }

    @Test void integrationEnvelopeIsStoredAsMetadataWithoutJavaTypeNames() throws Exception {
        JavaType type = mapper.getTypeFactory().constructParametricType(Envelope.class, Row.class);
        Envelope<Row> envelope = new Envelope<>();
        envelope.data = List.of(row()); envelope.options = Map.of("replace", true,
                "threshold", new BigDecimal("12345678901234567890.12345678901234567"));
        envelope.dataFilter = Map.of("location", "1110"); envelope.threadSync = "SYNC";
        for (DataFormat format : List.of(DataFormat.ARROW, DataFormat.PARQUET)) {
            byte[] bytes = encode(envelope, type, format, Map.of());
            @SuppressWarnings("unchecked") Envelope<Row> restored = (Envelope<Row>) codec.read(new ByteArrayInputStream(bytes), type, format);
            assertEquals(envelope.options, restored.options);
            assertEquals(envelope.dataFilter, restored.dataFilter);
            assertEquals("SYNC", restored.threadSync);
            assertEquals("20104414", restored.data.getFirst().key.material);
            assertFalse(new String(bytes, StandardCharsets.ISO_8859_1).contains(getClass().getName()));
        }
    }

    @Test void existingSpreadsheetMatrixPreservesHeadingsValuesAndOrder() throws Exception {
        List<List<Object>> matrix = List.of(List.of("Bill of Materials Id", "Output Quantity", "Active", "Notes"),
                Arrays.asList("LT_1110_L4_20104414", new BigDecimal("1000"), true, "a;\"b\"\nline"),
                Arrays.asList("=literal", null, false, ""));
        JavaType type = mapper.getTypeFactory().constructType(new TypeReference<List<List<Object>>>() { });
        byte[] xlsx = encode(matrix, type, DataFormat.XLSX, Map.of());
        assertEquals('P', xlsx[0]); assertEquals('K', xlsx[1]);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("Bill of Materials Id", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(1000d, sheet.getRow(1).getCell(1).getNumericCellValue());
            assertEquals("=literal", sheet.getRow(2).getCell(0).getStringCellValue());
        }
        byte[] csv = encode(matrix, type, DataFormat.CSV, Map.of("fieldSeparator", ";", "decimalSeparator", "comma"));
        String text = new String(csv, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("Bill of Materials Id;Output Quantity;Active;Notes\n"));
        assertTrue(text.contains("\"a;\"\"b\"\"\nline\""));
        @SuppressWarnings("unchecked") List<List<Object>> restored = (List<List<Object>>) codec.read(new ByteArrayInputStream(csv), type, DataFormat.CSV);
        assertEquals(matrix.getFirst(), restored.getFirst());
        assertEquals(matrix.get(1).get(3), restored.get(1).get(3));
        for (DataFormat format : List.of(DataFormat.ARROW, DataFormat.PARQUET)) {
            byte[] binary = encode(matrix, type, format, Map.of());
            @SuppressWarnings("unchecked") List<List<Object>> actual = (List<List<Object>>) codec.read(new ByteArrayInputStream(binary), type, format);
            assertEquals(matrix.getFirst(), actual.getFirst());
            assertEquals("LT_1110_L4_20104414", actual.get(1).getFirst());
            assertEquals(true, actual.get(1).get(2));
            assertEquals(matrix.get(1).get(3), actual.get(1).get(3));
        }
    }

    @Test void decimalCsvPolicyEscapesCommaAndXlsxDoesNotLoseLargeNumbers() throws Exception {
        List<List<Object>> matrix = List.of(List.of("amount"), List.of(new BigDecimal("3.912")),
                List.of(new BigDecimal("12345678901234567890.123456789")));
        JavaType type = mapper.getTypeFactory().constructType(new TypeReference<List<List<Object>>>() { });
        String csv = new String(encode(matrix, type, DataFormat.CSV, Map.of("decimalSeparator", "comma")), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"3,912\""));
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(encode(matrix, type, DataFormat.XLSX, Map.of())))) {
            assertEquals("12345678901234567890.123456789", workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue());
        }
    }

    @Test void unsupportedInputsFailExplicitlyRatherThanLosingData() throws Exception {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Row.class);
        Row row = row(); row.quantity = new BigDecimal("123456789012345678901234567890123456789");
        assertThrows(IOException.class, () -> encode(List.of(row), type, DataFormat.ARROW, Map.of()));
        JavaType matrix = mapper.getTypeFactory().constructType(new TypeReference<List<List<Object>>>() { });
        assertThrows(IOException.class, () -> encode(List.of(List.of("v"), List.of(1), List.of("a")), matrix, DataFormat.PARQUET, Map.of()));
        assertThrows(IOException.class, () -> encode(List.of(row()), type, DataFormat.XLSX, Map.of()));
        assertThrows(IOException.class, () -> codec.read(new ByteArrayInputStream("nullable\n3.5\n".getBytes(StandardCharsets.UTF_8)), type, DataFormat.CSV));
        assertThrows(IOException.class, () -> codec.read(new ByteArrayInputStream("nullable\n9223372036854775808\n".getBytes(StandardCharsets.UTF_8)), type, DataFormat.CSV));
    }

    @Test void nativeArrowBatchesAreOneCompleteStreamAndKeepNullEmptyAndUnicodeValues() throws Exception {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Row.class);
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 2051; i++) {
            Row row = row(); row.key.material = "material-" + i;
            if (i == 2048) { row.child = null; row.alternatives = List.of(); row.factories = Map.of(); row.quantity = null; }
            if (i == 2050) row.child.name = "Bobina São Paulo 日本";
            rows.add(row);
        }
        byte[] bytes = encode(rows, type, DataFormat.ARROW, Map.of());
        @SuppressWarnings("unchecked") List<Row> actual = (List<Row>) codec.read(new ByteArrayInputStream(bytes), type, DataFormat.ARROW);
        assertEquals(2051, actual.size());
        assertEquals("material-2050", actual.get(2050).key.material);
        assertEquals("Bobina São Paulo 日本", actual.get(2050).child.name);
        assertNull(actual.get(2048).child);
        assertNull(actual.get(2048).quantity);
        assertEquals(List.of(), actual.get(2048).alternatives);
        assertEquals(Map.of(), actual.get(2048).factories);
    }

    @Test void csvReadsDecimalCommaOnlyForNumericPropertiesAndBinaryDatesHonorJacksonSettings() throws Exception {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, FlatRow.class);
        String csv = "\uFEFF\"id\";amount;date\r\n00123;3,912;2026-10-22\r\n";
        @SuppressWarnings("unchecked") List<FlatRow> restored = (List<FlatRow>) codec.read(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), type, DataFormat.CSV);
        assertEquals("00123", restored.getFirst().id);
        assertEquals(new BigDecimal("3.912"), restored.getFirst().amount);
        assertEquals(LocalDate.of(2026, 10, 22), restored.getFirst().date);
        ObjectMapper timestampMapper = new ObjectMapper().findAndRegisterModules();
        DataFormatCodec timestamps = new DataFormatCodec(timestampMapper);
        FlatRow row = new FlatRow(); row.id = "a"; row.date = LocalDate.of(2026, 10, 22);
        row.amount = new BigDecimal("0.12345678901234567890123456789012345678");
        for (DataFormat format : List.of(DataFormat.ARROW, DataFormat.PARQUET)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            timestamps.write(List.of(row), type, format, output, Map.of());
            @SuppressWarnings("unchecked") List<FlatRow> actual = (List<FlatRow>) timestamps.read(
                    new ByteArrayInputStream(output.toByteArray()), type, format);
            assertEquals(row.date, actual.getFirst().date);
            assertEquals(0, row.amount.compareTo(actual.getFirst().amount));
        }
    }

    private byte[] encode(Object value, JavaType type, DataFormat format, Map<String, String> options) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        codec.write(value, type, format, output, options);
        return output.toByteArray();
    }

    @Test void spreadsheetHierarchyValidationUsesDeclaredSchemaEvenForEmptyOrNullValues() throws Exception {
        JavaType nested = mapper.getTypeFactory().constructCollectionType(List.class, Row.class);
        for (DataFormat format : List.of(DataFormat.CSV, DataFormat.XLSX)) {
            for (List<Row> rows : List.of(List.<Row>of(), List.of(new Row()), List.of(row()))) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                TabularExportException error = assertThrows(TabularExportException.class,
                        () -> codec.write(rows, nested, format, output, Map.of()));
                assertTrue(error.getMessage().contains("múltiplos níveis hierárquicos"));
                assertTrue(error.getMessage().contains("child"));
                assertEquals(0, output.size());
            }
            JavaType matrix = mapper.getTypeFactory().constructType(new TypeReference<List<List<Object>>>() { });
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            assertThrows(TabularExportException.class, () -> codec.write(
                    List.of(List.of("details"), List.of("flat"), List.of(Map.of("nested", "value"))),
                    matrix, format, output, Map.of()));
            assertEquals(0, output.size());

            // A serialized, unwrapped key is flat and remains eligible.
            JavaType flat = mapper.getTypeFactory().constructCollectionType(List.class, Key.class);
            assertTrue(encode(List.of(new Key("0001", "0002")), flat, format, Map.of()).length > 0);
        }
    }

    @Test void flatFileValidationRejectsCollectionsMapsAndRecursivePropertiesBeforeSchemaConstruction() {
        for (DataFormat format : List.of(DataFormat.CSV, DataFormat.XLSX)) {
            for (Class<?> rowClass : List.of(WithList.class, WithMap.class, RecursiveRow.class)) {
                JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, rowClass);
                TabularExportException error = assertThrows(TabularExportException.class,
                        () -> codec.prepareWrite(List.of(), type, format, Map.of()));
                assertTrue(error.getMessage().contains(rowClass == RecursiveRow.class ? "child" : "inputs"));
            }
        }
    }
}
