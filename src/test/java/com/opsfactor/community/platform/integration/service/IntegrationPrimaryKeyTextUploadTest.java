package com.opsfactor.community.platform.integration.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.*;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.integration.validation.IntegrationFileColumn;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the upload boundary without a database or application context. */
public class IntegrationPrimaryKeyTextUploadTest {

    @Test
    void rejectsLineBreaksInEveryKeyPositionBeforeNormalizationOrPersistence() {

        for (String lineBreak : List.of("\n", "\r", "\r\n", "\u0085", "\u2028", "\u2029")) {
            for (String id : List.of(lineBreak + "ITEM", "IT" + lineBreak + "EM", "ITEM" + lineBreak)) {
                TestService service = new TestService();
                TestDto dto = new TestDto(id, "Description  ");
                DataUploadException exception = assertThrows(DataUploadException.class,
                        () -> service.persistDTOList(payload(dto), null));

                assertEquals("Primary key field 'materialId' contains a line break (Enter); remove it before uploading at JSON record 1",
                        exception.getMessage());
                assertEquals(id, dto.primaryKeyDto.id);
                assertEquals("Description  ", dto.description);
                assertFalse(dto.normalized);
                assertNoDatabaseOperations(service);
            }
        }

    }

    @Test
    void rejectsLeadingAndTrailingWhitespaceBeforeNormalization() {

        for (String id : List.of(" ITEM", "ITEM ", "\tITEM", "ITEM\t", "\u00a0ITEM", "ITEM\u00a0")) {
            TestService service = new TestService();
            TestDto dto = new TestDto(id, "Description");
            DataUploadException exception = assertThrows(DataUploadException.class,
                    () -> service.persistDTOList(payload(dto), null));

            assertTrue(exception.getMessage().contains("leading or trailing whitespace"));
            assertEquals(id, dto.primaryKeyDto.id);
            assertFalse(dto.normalized);
            assertNoDatabaseOperations(service);
        }

    }

    @Test
    void checksEachTextualComponentOfCompositePrimaryKeys() {

        TestService service = new TestService();
        TestDto dto = new TestDto("ITEM", "Description");
        dto.primaryKeyDto.locationId = "SITE\n";
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> service.persistDTOList(payload(dto), null));

        assertTrue(exception.getMessage().contains("field 'locationId' contains a line break"));
        assertEquals("SITE\n", dto.primaryKeyDto.locationId);
        assertFalse(dto.normalized);
        assertNoDatabaseOperations(service);

    }

    @Test
    void validatesAllJsonRecordsBeforeLoadingOrSavingAnyEntity() {

        TestService service = new TestService();
        TestDto valid = new TestDto("VALID", "Description");
        TestDto invalid = new TestDto("INVALID\n", "Description");
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> service.persistDTOList(payload(valid, invalid), null));

        assertTrue(exception.getMessage().endsWith(" at JSON record 2"));
        assertEquals("INVALID\n", invalid.primaryKeyDto.id);
        assertFalse(invalid.normalized);
        assertNoDatabaseOperations(service);

    }

    @Test
    void processedFileReportsPhysicalRowAndPublishedColumn() {

        TestService service = new TestService();
        ProcessedFile file = new ProcessedFile();
        file.addRow(new ProcessedFileRow(List.of("Material Id", "Location Id", "Description")));
        file.addRow(new ProcessedFileRow());
        file.addRow(new ProcessedFileRow(List.of("ITEM\r\n", "SITE", "Description")).sourceRowNumber(181));
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> service.saveFile(file, null, null));

        assertEquals("Primary key field 'Material Id' contains a line break (Enter); remove it before uploading at line 181",
                exception.getMessage());
        assertEquals("ITEM\r\n", service.mapper.lastFileDto.primaryKeyDto.id);
        assertFalse(service.mapper.lastFileDto.normalized);
        assertNoDatabaseOperations(service);

    }

    @Test
    void xlsxUploadRetainsEmbeddedEnterAndSparsePhysicalRow() throws Exception {

        TestService service = new TestService();
        MockMultipartFile file = xlsxInput("ITEM\n", false, 0);
        DataUploadException exception = assertThrows(DataUploadException.class, () -> service.saveFile(file));

        assertTrue(exception.getMessage().contains("field 'Material Id' contains a line break"));
        assertTrue(exception.getMessage().endsWith(" at line 181"));
        assertEquals("ITEM\n", service.mapper.lastFileDto.primaryKeyDto.id);
        assertFalse(service.mapper.lastFileDto.normalized);
        assertNoDatabaseOperations(service);

    }

    @Test
    void acceptsInternalSpacesAndMultilineDescriptions() {

        TestService service = new TestService();
        TestDto dto = new TestDto("ITEM 123", "First line\nSecond line\r\nThird line");
        assertDoesNotThrow(() -> service.persistDTOList(payload(dto), null));

        assertTrue(dto.normalized);
        assertEquals(1, service.lookupCalls);
        assertEquals(1, service.saveCalls);
        assertEquals("ITEM 123", service.saved.get(0).id);
        assertEquals("First line\nSecond line\r\nThird line", service.saved.get(0).description);

    }

    @Test
    void persistedMalformedKeysDoNotBlockValidUploadsOrExports() {

        for (String oldId : List.of(" OLD", "OLD ", "OL\nD", "OLD\r\n", "=OLD")) {
            TestService service = new TestService();
            TestEntity oldEntity = new TestEntity(oldId, "SITE", "Existing description");
            service.existing.add(oldEntity);
            assertDoesNotThrow(() -> service.persistDTOList(payload(new TestDto("VALID", "New description")), null));

            assertEquals("VALID", service.saved.get(0).id);
            assertEquals(oldId, oldEntity.id);
            assertEquals(oldId, service.mapper.convertEntityToDTO(oldEntity).primaryKeyDto.id);
            assertEquals(oldId, service.getFullDTOList().get(0).primaryKeyDto.id);
            assertEquals(oldId, service.getFullFile().getFileRows().get(1).getColumnValueAsString(0));
        }

    }

    @Test
    void rejectsFormulaPrefixesInAllJsonKeyComponentsBeforeAnySideEffect() {

        for (boolean secondComponent : List.of(false, true)) {
            TestService service = new TestService();
            TestDto dto = new TestDto(secondComponent ? "ITEM" : "=SUM(A1:A2)", "=Description  ");
            if (secondComponent) dto.primaryKeyDto.locationId = "=SUM(A1:A2)";
            String field = secondComponent ? "locationId" : "materialId";
            var formulaPayload = payload(dto);
            formulaPayload.options = new IntegrationOptionsDto();
            formulaPayload.options.skipRecordsWithUnresolvedDependencies = secondComponent;
            DataUploadException exception = assertThrows(DataUploadException.class,
                    () -> service.persistDTOList(formulaPayload, null));

            assertEquals("Primary key field '" + field
                    + "': Excel formulas are not accepted at JSON record 1",
                    exception.getMessage());
            assertEquals("=SUM(A1:A2)", secondComponent ? dto.primaryKeyDto.locationId : dto.primaryKeyDto.id);
            assertEquals("=Description  ", dto.description);
            assertFalse(dto.normalized);
            assertEquals(1, formulaPayload.data.size());
            assertSame(dto, formulaPayload.data.get(0));
            assertNoDatabaseOperations(service);
        }

    }

    @Test
    void rejectsLiteralAndRealXlsxFormulasAtTheirPhysicalRowBeforeAnySideEffect() throws Exception {

        for (boolean actualFormula : List.of(false, true)) {
            for (int keyColumn : List.of(0, 1)) {
                TestService service = new TestService();
                String rawIdentifier = actualFormula ? "=\"ITEM\"" : "=SUM(A1:A2)";
                String field = keyColumn == 0 ? "Material Id" : "Location Id";
                DataUploadException exception = assertThrows(DataUploadException.class,
                        () -> service.saveFile(xlsxInput(rawIdentifier, actualFormula, keyColumn)));

                assertEquals("Primary key field '" + field
                        + "': Excel formulas are not accepted at line 181",
                        exception.getMessage());
                assertEquals(rawIdentifier, keyColumn == 0 ? service.mapper.lastFileDto.primaryKeyDto.id
                        : service.mapper.lastFileDto.primaryKeyDto.locationId);
                assertFalse(service.mapper.lastFileDto.normalized);
                assertNoDatabaseOperations(service);
            }
        }

    }

    @Test
    void acceptsEqualsInsideIdentifiersAndFormulaLikeDescriptions() {

        TestService service = new TestService();
        TestDto dto = new TestDto("ITEM=123", "=SUM(A1:A2)");
        dto.primaryKeyDto.locationId = "SITE=1";
        assertDoesNotThrow(() -> service.persistDTOList(payload(dto), null));
        assertEquals("ITEM=123", service.saved.get(0).id);
        assertEquals("SITE=1", service.saved.get(0).locationId);
        assertEquals("=SUM(A1:A2)", service.saved.get(0).description);

    }

    /** Builds STRING or FORMULA cells using the real XLSX parser and a sparse row. */
    private static MockMultipartFile xlsxInput(String rawIdentifier, boolean actualFormula, int keyColumn) throws Exception {

        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Upload");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Material Id");
            header.createCell(1).setCellValue("Location Id");
            header.createCell(2).setCellValue("Description");
            var row = sheet.createRow(180);
            row.createCell(0).setCellValue("ITEM");
            row.createCell(1).setCellValue("SITE");
            row.createCell(2).setCellValue("Description");
            if (actualFormula) row.getCell(keyColumn).setCellFormula(rawIdentifier.substring(1));
            else row.getCell(keyColumn).setCellValue(rawIdentifier);
            if (actualFormula) workbook.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(row.getCell(keyColumn));
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }
        return new MockMultipartFile("file", "upload.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookBytes);

    }

    /** Mutable input mirrors the list consumed by the real generic service. */
    private static IntegrationDto<TestDto, TestKey, Void, IntegrationOptionsDto> payload(TestDto... records) {

        IntegrationDto<TestDto, TestKey, Void, IntegrationOptionsDto> payload = new IntegrationDto<>();
        payload.data = new ArrayList<>(Arrays.asList(records));
        return payload;

    }

    private static void assertNoDatabaseOperations(TestService service) {

        assertEquals(0, service.lookupCalls);
        assertEquals(0, service.saveCalls);
        assertEquals(0, service.removeCalls);

    }

    /** Tracks whether the actual normalization hook ran after input validation. */
    public static class TestDto extends IntegrationDataDtoAbstract<TestDto, TestKey, TestEntity> {

        public String description;
        private boolean normalized;

        TestDto(String id, String description) {

            primaryKeyDto = new TestKey(id, "SITE");
            this.description = description;

        }

        @Override
        public void normalizaCamposTextoEntradaIntegracao() {

            normalized = true;
            super.normalizaCamposTextoEntradaIntegracao();

        }

    }

    public static class TestKey extends IntegrationPrimaryKeyDTOAbstract<TestKey, TestEntity> {

        @IntegrationFileColumn(value = 0, required = true)
        @JsonProperty("materialId")
        public String id;

        @IntegrationFileColumn(value = 1, required = true)
        public String locationId;

        TestKey(String id, String locationId) {

            this.id = id;
            this.locationId = locationId;

        }

        @Override
        public boolean hasSameKeyAsEntity(TestEntity entity) {

            return Objects.equals(id, entity.id) && Objects.equals(locationId, entity.locationId);

        }

        @Override
        public boolean equals(Object other) {

            return other instanceof TestKey key && Objects.equals(id, key.id) && Objects.equals(locationId, key.locationId);

        }

        @Override
        public int hashCode() {

            return Objects.hash(id, locationId);

        }

    }

    private static class TestEntity {

        final String id;
        final String locationId;
        String description;

        TestEntity(String id, String locationId, String description) {

            this.id = id;
            this.locationId = locationId;
            this.description = description;

        }

    }

    /** Maps only fixture fields while retaining every production default hook. */
    private static class TestMapper implements IntegrationMapperInterface<TestDto, TestKey, TestEntity, Void> {

        TestDto lastFileDto;

        @Override
        public TestDto getDtoWithoutPrimaryKeyFromEntity(TestEntity entity) {

            return new TestDto(entity.id, entity.description);

        }

        @Override
        public TestKey getPrimaryKeyDtoFromEntity(TestEntity entity) {

            return new TestKey(entity.id, entity.locationId);

        }

        @Override
        public TestDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow row, Void support) {

            lastFileDto = new TestDto(row.getColumnValueAsString(0), row.getColumnValueAsString(2));
            return lastFileDto;

        }

        @Override
        public TestKey getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow row, Void support) {

            return new TestKey(row.getColumnValueAsString(0), row.getColumnValueAsString(1));

        }

        @Override
        public TestEntity createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(TestKey key, Void support) {

            return new TestEntity(key.id, key.locationId, null);

        }

        @Override
        public void updateEntityNonPrimaryFieldsFromDTO(TestEntity entity, TestDto dto, Void support, Map<String, MetodoAtualizacaoCampo> fields) {

            entity.description = dto.description;

        }

        @Override
        public List<String> getProcessedFileHeaders() {

            return List.of("Material Id", "Location Id", "Description");

        }

        @Override
        public ProcessedFileRow convertEntityToProcessedFileRow(TestEntity entity, Void support) {

            return new ProcessedFileRow(Arrays.asList(entity.id, entity.locationId, entity.description));

        }

    }

    /** Counters expose database side effects without replacing service logic. */
    private static class TestService implements IntegrationServiceComConfiguracoesInterface<TestDto, TestKey, TestEntity, Void, TestMapper, Void, IntegrationOptionsDto> {

        final TestMapper mapper = new TestMapper();
        final List<TestEntity> existing = new ArrayList<>();
        final List<TestEntity> saved = new ArrayList<>();
        int lookupCalls;
        int saveCalls;
        int removeCalls;

        @Override
        public TestMapper getMapper() { return mapper; }

        @Override
        public List<TestEntity> saveEntityList(Collection<TestEntity> entities) {

            saveCalls++;
            saved.addAll(entities);
            return new ArrayList<>(entities);

        }

        @Override
        public void removeEntityList(Collection<TestEntity> entities) { removeCalls++; }

        @Override
        public String getSaveSuccessMessage() { return "saved"; }

        @Override
        public Void getSupportData() { return null; }

        @Override
        public int getBatchSize() { return 1; }

        @Override
        public Collection<TestEntity> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<TestKey> keys) {

            lookupCalls++;
            return existing;

        }

        @Override
        public Collection<TestEntity> getAllPersistedEntities() { return existing; }

        @Override
        public void executaAcaoColateralOptionsNaConversaoDtoEmEntidade(TestDto dto, TestEntity entity, IntegrationOptionsDto options, Void support) { }

    }

}
