package com.opsfactor.community.web.configuration.dataformat;

import com.opsfactor.community.platform.utility.fileprocessing.FileProcessing;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for Excel-native dates, source typing and both import paths. */
class DataFormatTemporalCellsTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final DataFormatCodec codec = new DataFormatCodec(mapper);
    private final LocalDate date = LocalDate.of(2026, 11, 5);
    private final LocalTime time = LocalTime.of(13, 45, 27, 123_000_000);
    private final LocalDateTime dateTime = LocalDateTime.of(date, time);

    public static class Key { public LocalDate date; }
    public static class EdgeKeyDto {
        @JsonUnwrapped(prefix = "_", suffix = "_") public Key key;
        @JsonProperty("_") public String identifier;
    }
    public static class Dto {
        @JsonUnwrapped(prefix = "key_") public Key key;
        @JsonProperty("scheduled_at") public LocalDateTime dateTime;
        public LocalTime time;
        public String identifier;
        public Date legacyDate;
        public java.sql.Date sqlDate;
        public java.sql.Time sqlTime;
        public java.sql.Timestamp sqlTimestamp;
    }
    public static class AgGridDTO {
        public List<Column> columnDefs;
        public List<Object> data;
    }
    public static class Column {
        public String field;
        public String headerName;
        public List<Column> children;
        Column(String field) { this.field = field; this.headerName = field; }
    }

    private Dto dto() {
        Dto row = new Dto();
        row.key = new Key(); row.key.date = date;
        row.dateTime = dateTime; row.time = time;
        row.identifier = date.toString();
        row.legacyDate = Date.from(dateTime.toInstant(ZoneOffset.UTC));
        row.sqlDate = java.sql.Date.valueOf(date);
        row.sqlTime = java.sql.Time.valueOf(time);
        row.sqlTimestamp = java.sql.Timestamp.valueOf(dateTime);
        return row;
    }

    private byte[] encode(Object value, com.fasterxml.jackson.databind.JavaType type, DataFormat format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        codec.write(value, type, format, output, Map.of());
        return output.toByteArray();
    }

    private byte[] matrix(List<List<Object>> value, DataFormat format) throws Exception {
        var type = mapper.getTypeFactory().constructCollectionType(List.class,
                mapper.getTypeFactory().constructCollectionType(List.class, Object.class));
        return encode(value, type, format);
    }

    private void assertDate(Cell cell, LocalDateTime expected) {
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertTrue(DateUtil.isCellDateFormatted(cell));
        assertEquals(expected, cell.getLocalDateTimeCellValue());
    }

    @Test void templateKeepsDatesTimesAndDateTimesNumericButIsoIdentifiersTextual() throws Exception {
        ProcessedFile file = new ProcessedFile();
        file.addNewRow();
        for (String header : List.of("date", "datetime", "time", "identifier", "blank")) file.addToLastRow(header);
        file.addNewRow();
        for (Object value : Arrays.asList(date, dateTime, time, date.toString(), null)) file.addToLastRow(value);
        List<List<Object>> values = new ArrayList<>();
        file.getFileRows().forEach(row -> values.add(row.row()));
        byte[] bytes = matrix(values, DataFormat.XLSX);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Row row = workbook.getSheetAt(0).getRow(1);
            assertDate(row.getCell(0), date.atStartOfDay());
            assertDate(row.getCell(1), dateTime);
            assertEquals(CellType.NUMERIC, row.getCell(2).getCellType());
            assertTrue(DateUtil.isCellDateFormatted(row.getCell(2)));
            assertEquals(time, row.getCell(2).getLocalDateTimeCellValue().toLocalTime());
            assertEquals(CellType.STRING, row.getCell(3).getCellType());
            assertEquals(date.toString(), row.getCell(3).getStringCellValue());
            assertEquals(CellType.BLANK, row.getCell(4).getCellType());
        }
        var imported = FileProcessing.getProcessedFileFromMultipartFile(new MockMultipartFile("file", "dates.xlsx", "application/octet-stream", bytes));
        var row = imported.getFileRows().get(1);
        assertEquals(date, row.getColumnValueAsLocalDate(0));
        assertEquals(dateTime, row.getColumnValueAsLocalDateTime(1));
        assertEquals(time, row.getColumnValueAsLocalTime(2));
    }

    @Test void dtoUsesOriginalTemporalTypesWithJacksonNamesAndUnwrappedKeys() throws Exception {
        var type = mapper.getTypeFactory().constructCollectionType(List.class, Dto.class);
        byte[] bytes = encode(List.of(dto(), new Dto()), type, DataFormat.XLSX);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Cell> cells = new HashMap<>();
            for (Cell header : sheet.getRow(0)) cells.put(header.getStringCellValue(), sheet.getRow(1).getCell(header.getColumnIndex()));
            assertDate(cells.get("key_date"), date.atStartOfDay());
            assertDate(cells.get("scheduled_at"), dateTime);
            assertDate(cells.get("legacyDate"), dateTime);
            assertEquals(CellType.NUMERIC, cells.get("time").getCellType());
            assertEquals(CellType.STRING, cells.get("identifier").getCellType());
        }
        @SuppressWarnings("unchecked") List<Dto> restored = (List<Dto>) codec.read(new ByteArrayInputStream(bytes), type, DataFormat.XLSX);
        assertEquals(date, restored.getFirst().key.date);
        assertEquals(dateTime, restored.getFirst().dateTime);
        assertEquals(time, restored.getFirst().time);
        assertEquals(dto().legacyDate, restored.getFirst().legacyDate);
        assertEquals(date.toString(), restored.getFirst().identifier);
        assertEquals(dto().sqlDate, restored.getFirst().sqlDate);
        assertEquals(dto().sqlTime, restored.getFirst().sqlTime);
        assertEquals(dto().sqlTimestamp, restored.getFirst().sqlTimestamp);
        assertNull(restored.get(1).dateTime);
    }

    @Test void gridPreservesTypedMapBeanAndNestedPathDatesWithoutGuessingText() throws Exception {
        AgGridDTO grid = new AgGridDTO();
        grid.columnDefs = List.of(new Column("scheduled_at"), new Column("identifier"), new Column("nested.date"));
        Map<String, Object> map = new HashMap<>();
        map.put("scheduled_at", dateTime); map.put("identifier", date.toString()); map.put("nested", Map.of("date", date));
        grid.data = List.of(map, dto());
        byte[] bytes = matrix(DataFormatGridAdapter.matrix(grid, mapper, true), DataFormat.XLSX);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertDate(sheet.getRow(1).getCell(0), dateTime);
            assertDate(sheet.getRow(1).getCell(2), date.atStartOfDay());
            assertDate(sheet.getRow(2).getCell(0), dateTime);
            assertEquals(CellType.STRING, sheet.getRow(1).getCell(1).getCellType());
        }
        assertEquals(dateTime.toString(), DataFormatGridAdapter.matrix(grid, mapper).get(1).get(0));
    }

    @Test void matrixDatesKeepTheirTextContractOutsideExcel() throws Exception {
        List<List<Object>> values = List.of(List.of("date", "datetime", "time"), List.of(date, dateTime, time));
        byte[] json = matrix(values, DataFormat.JSON);
        assertEquals(List.of(date.toString(), dateTime.toString(), time.toString()),
                mapper.convertValue(mapper.readTree(json).get(1), List.class));
        ObjectMapper timestampMapper = mapper.copy().enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalDateTime minute = date.atTime(13, 45);
        var timestampType = timestampMapper.getTypeFactory().constructCollectionType(List.class,
                timestampMapper.getTypeFactory().constructCollectionType(List.class, Object.class));
        ByteArrayOutputStream timestampOutput = new ByteArrayOutputStream();
        new DataFormatCodec(timestampMapper).write(List.of(List.of("date", "datetime", "time"), List.of(date, minute, LocalTime.NOON)),
                timestampType, DataFormat.JSON, timestampOutput, Map.of());
        assertEquals(List.of(date.toString(), minute.toString(), LocalTime.NOON.toString()),
                timestampMapper.convertValue(timestampMapper.readTree(timestampOutput.toByteArray()).get(1), List.class));
        String csv = new String(matrix(values, DataFormat.CSV), StandardCharsets.UTF_8);
        assertTrue(csv.contains(date + "," + dateTime + "," + time));
        var matrixType = mapper.getTypeFactory().constructCollectionType(List.class,
                mapper.getTypeFactory().constructCollectionType(List.class, Object.class));
        for (DataFormat format : List.of(DataFormat.ARROW, DataFormat.PARQUET)) {
            @SuppressWarnings("unchecked") var decoded = (List<List<Object>>) codec.read(new ByteArrayInputStream(matrix(values, format)), matrixType, format);
            assertEquals(List.of(date.toString(), dateTime.toString(), time.toString()), decoded.get(1));
        }
    }

    @Test void shortTextFieldDoesNotMatchOverlappingUnwrappedAffixes() throws Exception {
        EdgeKeyDto value = new EdgeKeyDto(); value.key = new Key(); value.key.date = date; value.identifier = date.toString();
        var type = mapper.getTypeFactory().constructCollectionType(List.class, EdgeKeyDto.class);
        byte[] bytes = encode(List.of(value), type, DataFormat.XLSX);
        @SuppressWarnings("unchecked") List<EdgeKeyDto> restored = (List<EdgeKeyDto>) codec.read(new ByteArrayInputStream(bytes), type, DataFormat.XLSX);
        assertEquals(date, restored.getFirst().key.date);
        assertEquals(date.toString(), restored.getFirst().identifier);
    }

    @Test void stylesAreReusedAcrossLargeExports() throws Exception {
        List<List<Object>> rows = new ArrayList<>(); rows.add(List.of("date", "datetime", "time"));
        for (int i = 0; i < 1000; i++) rows.add(List.of(date, dateTime, time));
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(matrix(rows, DataFormat.XLSX)))) {
            assertTrue(workbook.getNumCellStyles() <= 4);
            assertDate(workbook.getSheetAt(0).getRow(1000).getCell(0), date.atStartOfDay());
        }
    }

    @Test void readerHonorsExcel1904DateWindowing() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.getCTWorkbook().getWorkbookPr().setDate1904(true);
            Sheet sheet = workbook.createSheet(); sheet.createRow(0).createCell(0).setCellValue("scheduled_at");
            Cell cell = sheet.createRow(1).createCell(0);
            cell.setCellValue(DateUtil.getExcelDate(dateTime, true));
            CellStyle style = workbook.createCellStyle(); style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss.000")); cell.setCellStyle(style);
            workbook.write(output); bytes = output.toByteArray();
        }
        var type = mapper.getTypeFactory().constructCollectionType(List.class, Dto.class);
        @SuppressWarnings("unchecked") List<Dto> restored = (List<Dto>) codec.read(new ByteArrayInputStream(bytes), type, DataFormat.XLSX);
        assertEquals(dateTime, restored.getFirst().dateTime);
    }
}
