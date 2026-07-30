package com.opsfactor.community.platform.integration.service;

import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contratos da infraestrutura generica de data upload Community.
 *
 * <p>Os services concretos carregam snapshots de entidades persistidas e a
 * interface comum transforma esse snapshot em mapa por chave primaria. Esse
 * ponto precisa falhar com contexto funcional quando o snapshot vem quebrado,
 * pois ele e usado por todas as cargas Community/Enterprise baseadas no
 * contrato generico.</p>
 */
class IntegrationServiceComConfiguracoesInterfaceTest {

    @Test
    void persistedEntityMapShouldRejectNullSnapshot() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(null);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(List.of()));

        Assertions.assertEquals(
                "Persisted integration entity snapshot returned null.",
                dataUploadException.getMessage());

    }

    @Test
    void persistedEntityMapShouldRejectNullItem() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(Arrays.asList((String) null));

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(List.of()));

        Assertions.assertEquals(
                "Persisted integration entity snapshot returned null item at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void persistedEntityMapShouldRejectNullPrimaryKey() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of("NO_KEY"));

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(List.of()));

        Assertions.assertEquals(
                "Persisted integration entity snapshot returned item without primary key at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void persistedEntityMapShouldRejectDuplicatedPrimaryKey() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of("A", "A"));

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(List.of()));

        Assertions.assertEquals(
                "Persisted integration entity snapshot returned duplicated primary key A.",
                dataUploadException.getMessage());

    }

    @Test
    void persistedEntityMapShouldIndexValidSnapshot() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of("A", "B"));

        Map<DummyPrimaryKeyDto, String> persistedEntityMap =
                dummyIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(List.of());

        Assertions.assertEquals(2, persistedEntityMap.size());
        Assertions.assertEquals("A", persistedEntityMap.get(new DummyPrimaryKeyDto("A")));
        Assertions.assertEquals("B", persistedEntityMap.get(new DummyPrimaryKeyDto("B")));

    }

    @Test
    void payloadPrimaryKeySetShouldRejectDuplicatedPrimaryKeyBeforeLookup() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of());
        DummyDto firstDummyDto = new DummyDto(new DummyPrimaryKeyDto("A"));
        DummyDto duplicatedDummyDto = new DummyDto(new DummyPrimaryKeyDto("A"));

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.getPrimaryKeyDtoSetObrigatorioParaPayload(List.of(
                        firstDummyDto,
                        duplicatedDummyDto)));

        Assertions.assertEquals(
                "Integration DTO data list returned duplicated primary key A at index 1.",
                dataUploadException.getMessage());

    }

    @Test
    void payloadPrimaryKeySetShouldRejectMissingPrimaryKeyForNonEmptyRow() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of());
        DummyDto dummyDtoSemChave = new DummyDto(null);
        dummyDtoSemChave.value = "filled";

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.getPrimaryKeyDtoSetObrigatorioParaPayload(List.of(dummyDtoSemChave)));

        Assertions.assertEquals(
                "Integration DTO data list returned item without primary key at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void payloadPrimaryKeySetShouldIgnoreEmptyRowsAndKeepStableKeys() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of());
        DummyDto emptyDummyDto = new DummyDto();
        DummyDto validDummyDto = new DummyDto(new DummyPrimaryKeyDto("A"));

        Set<DummyPrimaryKeyDto> primaryKeyDtoSet =
                dummyIntegrationService.getPrimaryKeyDtoSetObrigatorioParaPayload(List.of(
                        emptyDummyDto,
                        validDummyDto));

        Assertions.assertEquals(Set.of(new DummyPrimaryKeyDto("A")), primaryKeyDtoSet);

    }

    @Test
    void persistDTOListShouldRejectDuplicatedPayloadPrimaryKeyBeforeRepositoryLookup() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of());
        IntegrationDto<DummyDto, DummyPrimaryKeyDto, Void, IntegrationOptionsDto> integrationDto =
                new IntegrationDto<>();
        integrationDto.data = new ArrayList<>(List.of(
                new DummyDto(new DummyPrimaryKeyDto("A")),
                new DummyDto(new DummyPrimaryKeyDto("A"))));

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.persistDTOList(
                        integrationDto,
                        null));

        Assertions.assertEquals(
                "Integration DTO data list returned duplicated primary key A at index 1.",
                dataUploadException.getMessage());

    }

    @Test
    void convertDTOToEntityAndTreatErrorShouldPreserveOriginalMapperException() {

        IllegalStateException originalException = new IllegalStateException("Invalid dummy value");
        DummyDto dummyDto = new DummyDto(new DummyPrimaryKeyDto("A"));
        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(
                        List.of(),
                        new DummyMapper() {

                            @Override
                            public String convertDTOToEntity(
                                    DummyDto dummyDto,
                                    Map<DummyPrimaryKeyDto, String> currentlyPersistedEntitiesByPrimaryKey,
                                    Void supportData,
                                    @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

                                throw originalException;

                            }

                        });

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.convertDTOToEntityAndTreatError(
                        dummyDto,
                        List.of(dummyDto),
                        Map.of(),
                        null,
                        null,
                        null,
                        0,
                        null));

        Assertions.assertEquals("Invalid dummy value at line 2", dataUploadException.getMessage());
        Assertions.assertSame(originalException, dataUploadException.getCause());

    }

    @Test
    void saveMultipartFileShouldPreserveOriginalIOExceptionCause() {

        IOException originalException = new IOException("Broken multipart stream");
        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of());

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.saveFile(
                        new FailingMultipartFile(originalException),
                        null));

        Assertions.assertEquals("Broken multipart stream", dataUploadException.getMessage());
        Assertions.assertSame(originalException, dataUploadException.getCause());

    }

    @Test
    void savedEntityCollectionShouldRejectNullSnapshot() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of());

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.validaSavedEntityCollection(
                        null,
                        "Dummy saved collection"));

        Assertions.assertEquals(
                "Dummy saved collection returned null.",
                dataUploadException.getMessage());

    }

    @Test
    void savedEntityCollectionShouldRejectNullItem() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of());

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.validaSavedEntityCollection(
                        Arrays.asList((String) null),
                        "Dummy saved collection"));

        Assertions.assertEquals(
                "Dummy saved collection returned null item at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void savedEntityCollectionShouldRejectUnexpectedSizeWhenExpectedSizeIsProvided() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of());

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> dummyIntegrationService.validaSavedEntityCollection(
                        List.of("A"),
                        "Dummy saved collection",
                        2));

        Assertions.assertEquals(
                "Dummy saved collection size 1 differs from expected saved batch size 2.",
                dataUploadException.getMessage());

    }

    @Test
    void savedEntityCollectionShouldReturnStableListCopy() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of());

        List<String> savedEntityList = dummyIntegrationService.validaSavedEntityCollection(
                List.of("A", "B"),
                "Dummy saved collection");

        Assertions.assertEquals(List.of("A", "B"), savedEntityList);

    }

    @Test
    void fullDTOListShouldRejectNullEntitySnapshotBeforeMapper() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(null);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                dummyIntegrationService::getFullDTOList);

        Assertions.assertEquals(
                "Full integration entity snapshot returned null.",
                illegalStateException.getMessage());

    }

    @Test
    void fullDTOListShouldRejectNullEntityItemBeforeMapper() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(Arrays.asList((String) null));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                dummyIntegrationService::getFullDTOList);

        Assertions.assertEquals(
                "Full integration entity snapshot returned null item at index 0.",
                illegalStateException.getMessage());

    }

    @Test
    void fullDTOListShouldRejectNullMapperResultBeforeReturning() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(
                        List.of("A"),
                        new DummyMapper() {

                            @Override
                            public List<DummyDto> convertEntityCollectionToDTOList(Collection<String> entityList) {

                                return null;

                            }

                        });

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                dummyIntegrationService::getFullDTOList);

        Assertions.assertEquals(
                "Full integration DTO list snapshot returned null.",
                illegalStateException.getMessage());

    }

    @Test
    void fullDTOListShouldRejectNullDTOItemBeforeReturning() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(
                        List.of("A"),
                        new DummyMapper() {

                            @Override
                            public List<DummyDto> convertEntityCollectionToDTOList(Collection<String> entityList) {

                                return Arrays.asList((DummyDto) null);

                            }

                        });

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                dummyIntegrationService::getFullDTOList);

        Assertions.assertEquals(
                "Full integration DTO list snapshot returned null item at index 0.",
                illegalStateException.getMessage());

    }

    @Test
    void fullDTOListShouldRejectDTOWithoutPrimaryKeyBeforeReturning() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(
                        List.of("A"),
                        new DummyMapper() {

                            @Override
                            public List<DummyDto> convertEntityCollectionToDTOList(Collection<String> entityList) {

                                return List.of(new DummyDto());

                            }

                        });

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                dummyIntegrationService::getFullDTOList);

        Assertions.assertEquals(
                "Full integration DTO list snapshot returned item without primary key at index 0.",
                illegalStateException.getMessage());

    }

    @Test
    void fullDTOListShouldRejectDTOWithEmptyPrimaryKeyBeforeReturning() {

        DummyDto dummyDto = new DummyDto();
        dummyDto.primaryKeyDto = new DummyPrimaryKeyDto(null);
        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(
                        List.of("A"),
                        new DummyMapper() {

                            @Override
                            public List<DummyDto> convertEntityCollectionToDTOList(Collection<String> entityList) {

                                return List.of(dummyDto);

                            }

                        });

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                dummyIntegrationService::getFullDTOList);

        Assertions.assertEquals(
                "Full integration DTO list snapshot returned item with empty primary key at index 0.",
                illegalStateException.getMessage());

    }

    @Test
    void fullDTOListShouldReturnValidMapperSnapshot() {

        DummyIntegrationService dummyIntegrationService =
                new DummyIntegrationService(List.of("A", "B"));

        List<DummyDto> dummyDtoList = dummyIntegrationService.getFullDTOList();

        Assertions.assertEquals(2, dummyDtoList.size());
        Assertions.assertEquals("A", dummyDtoList.get(0).primaryKeyDto.id);
        Assertions.assertEquals("B", dummyDtoList.get(1).primaryKeyDto.id);

    }

    @Test
    void integrationServicesShouldValidateSaveAllReturnWithExpectedBatchSize() throws IOException {

        Path integrationSourceDirectory =
                Path.of("src/main/java/com/opsfactor/community/platform/integration");
        List<Path> integrationSourceFiles;
        try (var pathStream = Files.walk(integrationSourceDirectory)) {
            integrationSourceFiles = pathStream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }

        List<String> unvalidatedSaveAllCalls = new ArrayList<>();
        for (Path integrationSourceFile : integrationSourceFiles) {
            List<String> sourceLines = Files.readAllLines(integrationSourceFile);
            for (int indice = 0; indice < sourceLines.size(); indice++) {
                if (!sourceLines.get(indice).contains(".saveAll(")) {
                    continue;
                }

                /*
                 * A chamada pode estar quebrada em multiplas linhas:
                 * validaSavedEntityCollection(
                 *     repository.saveAll(collection),
                 *     "...",
                 *     collection.size()
                 * )
                 *
                 * Por isso olhamos algumas linhas anteriores ao ponto exato do
                 * saveAll e algumas posteriores, sem tentar interpretar Java
                 * via regex. Todo saveAll de integracao Community persiste uma
                 * colecao conhecida pelo caller e precisa validar tambem a
                 * quantidade devolvida pelo repository.
                 */
                int primeiraLinhaContexto = Math.max(
                        0,
                        indice - 4);
                int ultimaLinhaContexto = Math.min(
                        sourceLines.size(),
                        indice + 7);
                String contextoSaveAll = String.join(
                        "\n",
                        sourceLines.subList(
                                primeiraLinhaContexto,
                                ultimaLinhaContexto));

                if (!contextoSaveAll.contains("validaSavedEntityCollection(")
                        || !contextoSaveAll.contains(".size()")) {
                    unvalidatedSaveAllCalls.add(
                            integrationSourceFile
                                    + ":"
                                    + (indice + 1));
                }
            }
        }

        Assertions.assertTrue(
                unvalidatedSaveAllCalls.isEmpty(),
                "saveAll(...) in integration services must validate repository return and "
                        + "expected batch size through validaSavedEntityCollection(..., expectedSize): "
                        + unvalidatedSaveAllCalls);

    }

    /**
     * Multipart minimo para exercitar a borda checked de leitura sem depender
     * de parser CSV/XLSX real.
     */
    private record FailingMultipartFile(IOException originalException) implements MultipartFile {

        @Override
        public String getName() {

            return "broken";

        }

        @Override
        public String getOriginalFilename() {

            return "broken.csv";

        }

        @Override
        public String getContentType() {

            return "text/csv";

        }

        @Override
        public boolean isEmpty() {

            return false;

        }

        @Override
        public long getSize() {

            return 1L;

        }

        @Override
        public byte[] getBytes() throws IOException {

            throw originalException;

        }

        @Override
        public InputStream getInputStream() throws IOException {

            throw originalException;

        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {

            throw originalException;

        }

    }

    private static class DummyIntegrationService implements IntegrationServiceComConfiguracoesInterface<
            DummyDto,
            DummyPrimaryKeyDto,
            String,
            Void,
            DummyMapper,
            Void,
            IntegrationOptionsDto> {

        private final Collection<String> persistedEntityCollection;
        private final DummyMapper dummyMapper;

        private DummyIntegrationService(Collection<String> persistedEntityCollection) {

            this(
                    persistedEntityCollection,
                    new DummyMapper());

        }

        private DummyIntegrationService(
                Collection<String> persistedEntityCollection,
                DummyMapper dummyMapper) {

            this.persistedEntityCollection = persistedEntityCollection;
            this.dummyMapper = dummyMapper;

        }

        @Override
        public DummyMapper getMapper() {

            return dummyMapper;

        }

        @Override
        public List<String> saveEntityList(Collection<String> entityList) {

            return List.copyOf(entityList);

        }

        @Override
        public void removeEntityList(Collection<String> entityList) {

        }

        @Override
        public String getSaveSuccessMessage() {

            return "dummy";

        }

        @Override
        public Void getSupportData() {

            return null;

        }

        @Override
        public int getBatchSize() {

            return 1000;

        }

        @Override
        public Collection<String> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                Collection<DummyPrimaryKeyDto> dtoBatchList) {

            return persistedEntityCollection;

        }

        @Override
        public Collection<String> getAllPersistedEntities() {

            return persistedEntityCollection;

        }

        @Override
        public void executaAcaoColateralOptionsNaConversaoDtoEmEntidade(
                DummyDto dummyDto,
                String entity,
                IntegrationOptionsDto integrationOptionsDto,
                Void supportData) {

        }

    }

    private static class DummyMapper implements IntegrationMapperInterface<
            DummyDto,
            DummyPrimaryKeyDto,
            String,
            Void> {

        @Override
        public DummyDto getDtoWithoutPrimaryKeyFromEntity(String entity) {

            return new DummyDto();

        }

        @Override
        public DummyPrimaryKeyDto getPrimaryKeyDtoFromEntity(String entity) {

            if ("NO_KEY".equals(entity)) {
                return null;
            }
            return new DummyPrimaryKeyDto(entity);

        }

        @Override
        public DummyDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
                ProcessedFileRow processedFileRow,
                Void supportData) {

            return new DummyDto();

        }

        @Override
        public DummyPrimaryKeyDto getPrimaryKeyDtoFromProcessedFileRow(
                ProcessedFileRow processedFileRow,
                Void supportData) {

            return new DummyPrimaryKeyDto("processed");

        }

        @Override
        public List<String> getProcessedFileHeaders() {

            return List.of("Id");

        }

        @Override
        public String createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
                DummyPrimaryKeyDto dummyPrimaryKeyDto,
                Void supportData) {

            return dummyPrimaryKeyDto.id;

        }

        @Override
        public void updateEntityNonPrimaryFieldsFromDTO(
                String entity,
                DummyDto dummyDto,
                Void supportData,
                @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        }

        @Override
        public ProcessedFileRow convertEntityToProcessedFileRow(String entity, Void supportData) {

            ProcessedFileRow processedFileRow = new ProcessedFileRow();
            processedFileRow.addContent(entity);
            return processedFileRow;

        }

    }

    public static class DummyDto extends IntegrationDataDtoAbstract<
            DummyDto,
            DummyPrimaryKeyDto,
            String> {

        public String value;

        public DummyDto() {

        }

        public DummyDto(DummyPrimaryKeyDto dummyPrimaryKeyDto) {

            this.primaryKeyDto = dummyPrimaryKeyDto;

        }

    }

    public static class DummyPrimaryKeyDto extends IntegrationPrimaryKeyDTOAbstract<
            DummyPrimaryKeyDto,
            String> {

        public String id;

        private DummyPrimaryKeyDto(String id) {

            this.id = id;

        }

        @Override
        public boolean hasSameKeyAsEntity(String entity) {

            return id.equals(entity);

        }

        @Override
        public boolean equals(Object object) {

            if (this == object) {
                return true;
            }
            if (!(object instanceof DummyPrimaryKeyDto dummyPrimaryKeyDto)) {
                return false;
            }
            return id.equals(dummyPrimaryKeyDto.id);

        }

        @Override
        public int hashCode() {

            return id.hashCode();

        }

        @Override
        public String toString() {

            return id;

        }

    }

}
