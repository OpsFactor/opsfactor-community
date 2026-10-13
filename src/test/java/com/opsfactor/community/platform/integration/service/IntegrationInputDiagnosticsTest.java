package com.opsfactor.community.platform.integration.service;

import com.opsfactor.community.platform.integration.dto.*;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.integration.validation.IntegrationInputContext;
import com.opsfactor.community.platform.integration.validation.IntegrationFileColumn;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Preserves input identity, physical positions and dependency-skip semantics. */
class IntegrationInputDiagnosticsTest {

    @Test
    void duplicateBusinessKeysDoNotChangePhysicalErrorRow() {

        TestService service = new TestService();
        TestDto first = new TestDto("BOM");
        TestDto failing = new TestDto("BOM");
        first.setIntegrationInputContext(new IntegrationInputContext(true, 35, List.of("Id")));
        failing.setIntegrationInputContext(new IntegrationInputContext(true, 181, List.of("Id")));
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> service.convertDTOToEntityAndTreatError(failing, List.of(first, failing), Map.of(), null, null, null, 1000, null));
        assertEquals("Required foreign key field 'Material Id' is missing at line 181", exception.getMessage());

    }

    @Test
    void directBatchCallsUseIdentityAndJsonRecordIndex() {

        TestService service = new TestService();
        TestDto first = new TestDto("BOM");
        TestDto failing = new TestDto("BOM");
        DataUploadException exception = assertThrows(DataUploadException.class,
                () -> service.convertDTOToEntityAndTreatError(failing, List.of(first, failing), Map.of(), null, null, null, 1000, null));
        assertTrue(exception.getMessage().endsWith(" at JSON record 1002"));

    }

    @Test
    void unresolvedReferenceRetainsSubtypeCauseAndSkipBehavior() {

        TestService service = new TestService();
        service.mapper.dependencyFailure = true;
        TestDto failing = new TestDto("BOM");
        failing.setIntegrationInputContext(new IntegrationInputContext(true, 181, List.of("Id")));
        MissingDependencyDataUploadException exception = assertThrows(MissingDependencyDataUploadException.class,
                () -> service.convertDTOToEntityAndTreatError(failing, List.of(failing), Map.of(), null, null, null, 0, null));
        assertTrue(exception.getMessage().endsWith(" at line 181"));
        assertSame(failing, exception.getDto());
        assertNotNull(exception.getCause());
        IntegrationOptionsDto options = new IntegrationOptionsDto();
        options.skipRecordsWithUnresolvedDependencies = true;
        assertNull(service.convertDTOToEntityAndTreatError(failing, List.of(failing), Map.of(), null, null, options, 0, null));

    }

    @Test
    void metadataSurvivesEmptyFileRowsAndRemainsOutsideJson() {

        TestService service = new TestService();
        ProcessedFile file = new ProcessedFile();
        file.addRow(new ProcessedFileRow(List.of("Id")));
        file.addRow(new ProcessedFileRow());
        file.addRow(new ProcessedFileRow(List.of("BOM")).sourceRowNumber(181));
        TestDto dto = service.mapper.convertProcessedFileToDTOList(file, null).get(0);
        assertEquals(181, dto.getIntegrationInputContext().rowNumber());
        assertEquals(dto.getIntegrationInputContext(), dto.primaryKeyDto.getIntegrationInputContext());
        assertEquals(List.of("Id"), dto.getIntegrationInputContext().fileHeaders());

    }

    /** A second batch must retain source position after the first batch is removed. */
    @Test
    void persistenceKeepsPhysicalPositionAcrossBatches() {

        TestService service = new TestService();
        service.mapper.acceptedId = "OK";
        TestDto first = new TestDto("OK");
        TestDto failing = new TestDto("BAD");
        first.setIntegrationInputContext(new IntegrationInputContext(true, 2, List.of("Id")));
        failing.setIntegrationInputContext(new IntegrationInputContext(true, 181, List.of("Id")));
        IntegrationDto<TestDto, TestKey, Void, IntegrationOptionsDto> envelope = new IntegrationDto<>();
        envelope.data = new ArrayList<>(List.of(first, failing));
        DataUploadException exception = assertThrows(DataUploadException.class, () -> service.persistDTOList(envelope, null));
        assertTrue(exception.getMessage().endsWith(" at line 181"));

    }

    /** Required scalar declarations must respect ignored fields and deletion. */
    @Test
    void requiredScalarHonorsIgnoreAndDeletion() {

        TestService service = new TestService();
        service.mapper.acceptedId = "BOM";
        TestDto dto = new TestDto("BOM");
        dto.scalar = null;
        assertEquals("Required field 'scalar' is missing", assertThrows(DataUploadException.class,
                () -> service.mapper.convertDTOToEntity(dto, Map.of(), null, null)).getMessage());
        var ignored = Map.of("scalar", IntegrationMapperInterface.MetodoAtualizacaoCampo.IGNORE);
        assertEquals("BOM", service.mapper.convertDTOToEntity(dto, Map.of(), null, ignored));
        dto.delete = "D";
        assertEquals("BOM", service.mapper.convertDTOToEntity(dto, Map.of(), null, null));

    }

    public static class TestDto extends IntegrationDataDtoAbstract<TestDto, TestKey, String> {

        @IntegrationFileColumn(value = 0, required = true)
        public String scalar = "present";

        TestDto(String id) { this.primaryKeyDto = new TestKey(id); }

    }

    public static class TestKey extends IntegrationPrimaryKeyDTOAbstract<TestKey, String> {

        public String id;
        TestKey(String id) { this.id = id; }
        @Override
        public boolean hasSameKeyAsEntity(String entity) { return id.equals(entity); }
        @Override
        public boolean equals(Object other) { return other instanceof TestKey key && Objects.equals(id, key.id); }
        @Override
        public int hashCode() { return Objects.hashCode(id); }

    }

    private static class TestMapper implements IntegrationMapperInterface<TestDto, TestKey, String, Void> {

        boolean dependencyFailure;
        String acceptedId;
        @Override
        public TestDto getDtoWithoutPrimaryKeyFromEntity(String entity) { return new TestDto(entity); }
        @Override
        public TestKey getPrimaryKeyDtoFromEntity(String entity) { return new TestKey(entity); }
        @Override
        public TestDto getDtoWithoutPrimaryKeyFromProcessedFileRow(ProcessedFileRow row, Void support) { return new TestDto(row.getColumnValueAsString(0)); }
        @Override
        public TestKey getPrimaryKeyDtoFromProcessedFileRow(ProcessedFileRow row, Void support) { return new TestKey(row.getColumnValueAsString(0)); }
        @Override
        public String createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(TestKey key, Void support) { return key.id; }
        @Override
        public void updateEntityNonPrimaryFieldsFromDTO(String entity, TestDto dto, Void support, Map<String, MetodoAtualizacaoCampo> fields) {

            if (entity.equals(acceptedId)) return;
            if (dependencyFailure) throw new MissingDependencyDataUploadException("Foreign key field 'Material Id' not found", dto);
            throw new DataUploadException("Required foreign key field 'Material Id' is missing");

        }
        @Override
        public List<String> getProcessedFileHeaders() { return List.of("Id"); }
        @Override
        public ProcessedFileRow convertEntityToProcessedFileRow(String entity, Void support) { return new ProcessedFileRow(List.of(entity)); }

    }

    private static class TestService implements IntegrationServiceComConfiguracoesInterface<TestDto, TestKey, String, Void, TestMapper, Void, IntegrationOptionsDto> {

        final TestMapper mapper = new TestMapper();
        @Override
        public TestMapper getMapper() { return mapper; }
        @Override
        public List<String> saveEntityList(Collection<String> entities) { return new ArrayList<>(entities); }
        @Override
        public void removeEntityList(Collection<String> entities) { }
        @Override
        public String getSaveSuccessMessage() { return "saved"; }
        @Override
        public Void getSupportData() { return null; }
        @Override
        public int getBatchSize() { return 1; }
        @Override
        public Collection<String> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<TestKey> keys) { return List.of(); }
        @Override
        public Collection<String> getAllPersistedEntities() { return List.of(); }
        @Override
        public void executaAcaoColateralOptionsNaConversaoDtoEmEntidade(TestDto dto, String entity, IntegrationOptionsDto options, Void support) { }

    }

}
