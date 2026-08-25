package com.opsfactor.community.platform.integration.service;

import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface.MetodoAtualizacaoCampo;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.FileProcessing;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrato comum dos services de importacao/exportacao de dados.
 *
 * <p>A interface fica no Community porque o backend aberto ainda publica
 * cargas operacionais como sell-out, estoque inicial, material, location, UOM e
 * malha produtiva minima. Cada service concreto decide quais filtros,
 * sobreposicoes de campo e remocoes sao aceitos; o default deve ser conservador
 * para impedir carga ou delecao ampla quando o contrato nao foi declarado pelo
 * service.</p>
 */
public interface IntegrationServiceComConfiguracoesInterface<
        DTO extends IntegrationDataDtoAbstract<DTO, PRIMARYKEYDTO, ENTITY>,
        PRIMARYKEYDTO extends IntegrationPrimaryKeyDTOAbstract<PRIMARYKEYDTO, ENTITY>,
        ENTITY,
        SUPPORTDATA,
        MAPPER extends IntegrationMapperInterface<DTO, PRIMARYKEYDTO, ENTITY, SUPPORTDATA>,
        DATAFILTER,
        OPTIONS extends IntegrationOptionsDto> {

    Logger log = LoggerFactory.getLogger(IntegrationServiceComConfiguracoesInterface.class);

    public MAPPER getMapper();

    /**
     * Persiste um lote de entidades e retorna as entidades efetivamente gravadas.
     */
    public List<ENTITY> saveEntityList(Collection<ENTITY> entityList);

    /**
     * Persiste um batch preservando quais entidades foram criadas pelo mapper
     * neste upload, em vez de apenas atualizadas a partir do snapshot carregado.
     *
     * <p>A implementação padrão mantém a compatibilidade dos services existentes.
     * Services com chave atribuída precisam sobrescrever este ponto para usar
     * {@code persist} nas entidades novas, pois {@code CrudRepository.saveAll}
     * normalmente as trata como destacadas e executa {@code merge}.</p>
     */
    public default List<ENTITY> saveEntityList(
            Collection<ENTITY> entityList,
            Collection<ENTITY> newEntityList) {

        return saveEntityList(entityList);

    }

    /**
     * Valida o retorno de um repository depois de uma persistencia em lote.
     *
     * <p>A infraestrutura de upload atualiza o mapa de entidades persistidas
     * com base no retorno de {@link #saveEntityList(Collection)}. Por isso, um
     * retorno nulo ou contendo item nulo deve falhar aqui com mensagem
     * funcional, antes de virar erro generico no contador de registros ou na
     * reconstrucao das chaves primarias do batch. Services que ainda preservem
     * retorno vazio por compatibilidade podem usar este helper logo depois do
     * `saveAll` apenas como validação de contrato do repository.</p>
     */
    public default List<ENTITY> validaSavedEntityCollection(
            Collection<ENTITY> savedEntityCollection,
            String savedEntityCollectionDescription) {

        return IntegrationPersistenceValidation.validaSavedEntityCollection(
                savedEntityCollection,
                savedEntityCollectionDescription);

    }

    /**
     * Valida retorno de `saveAll` quando o service conhece o tamanho do lote
     * enviado ao repository.
     *
     * <p>Este overload deve ficar restrito ao ponto imediatamente posterior ao
     * `saveAll`. Leituras por filtro continuam usando
     * {@link #validaPersistedEntityCollectionForRead(Collection, String)} ou o
     * overload sem tamanho esperado.</p>
     */
    public default List<ENTITY> validaSavedEntityCollection(
            Collection<ENTITY> savedEntityCollection,
            String savedEntityCollectionDescription,
            int expectedSize) {

        return IntegrationPersistenceValidation.validaSavedEntityCollection(
                savedEntityCollection,
                savedEntityCollectionDescription,
                expectedSize);

    }

    public void removeEntityList(Collection<ENTITY> entityList);

    public default void persistDTOBatchList(
            List<DTO> dtoBatchList,
            Map<PRIMARYKEYDTO, ENTITY> currentlyPersistedEntitiesByPrimaryKey, // mapa completo com valores nao so relativos a este lote
            SUPPORTDATA supportData,
            Integer initialBatchPosition,
            @Nullable Map<String,MetodoAtualizacaoCampo> metodoAtualizacaoPorCampo,
            @Nullable OPTIONS options) {

        List<DTO> dtosToRemove = dtoBatchList.stream()
                .filter(dto -> dto.delete != null && dto.delete.trim().equalsIgnoreCase("D"))
                .collect(Collectors.toList());

        List<DTO> dtosToSave = new ArrayList<DTO>(dtoBatchList);
        dtosToSave.removeAll(dtosToRemove);
        IntegrationLoggingContext integrationLoggingContext = IntegrationLoggingContext.getCurrent();

        // popula as listas de entidades a remover e salvar. registros ignorados por dependencia nao
        // resolvida sao marcados para nao seguirem para importacao neste lote.
        List<ENTITY> entitiesToRemove = dtosToRemove.parallelStream()
                .filter(dto -> !dto.allFieldsAreEmpty())
                .map(dto -> convertDTOToEntityAndTreatError(dto, dtoBatchList, currentlyPersistedEntitiesByPrimaryKey, supportData, metodoAtualizacaoPorCampo, options, initialBatchPosition, integrationLoggingContext))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        /*
         * Mantém o DTO ao lado da entidade convertida para classificar, sem
         * ambiguidade, se a chave já constava no snapshot JPA do upload.
         */
        List<Map.Entry<DTO, ENTITY>> dtoEntityEntryList = dtosToSave.parallelStream()
                .filter(dto -> !dto.allFieldsAreEmpty())
                .map(dto -> new AbstractMap.SimpleImmutableEntry<>(
                        dto,
                        convertDTOToEntityAndTreatError(
                                dto,
                                dtoBatchList,
                                currentlyPersistedEntitiesByPrimaryKey,
                                supportData,
                                metodoAtualizacaoPorCampo,
                                options,
                                initialBatchPosition,
                                integrationLoggingContext)))
                .filter(dtoEntityEntry -> dtoEntityEntry.getValue() != null)
                .collect(Collectors.toList());
        List<ENTITY> entitiesToSave = dtoEntityEntryList.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        List<ENTITY> newEntitiesToSave = dtoEntityEntryList.stream()
                .filter(dtoEntityEntry -> !currentlyPersistedEntitiesByPrimaryKey.containsKey(
                        dtoEntityEntry.getKey().primaryKeyDto))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        if (!entitiesToRemove.isEmpty()) {
            removeEntityList(entitiesToRemove);
            if (integrationLoggingContext != null) {
                integrationLoggingContext.addRemovedRecords(entitiesToRemove.size());
            }
            entitiesToRemove.stream() // não é paralelo pois se trata de um hashmap regular
                    .map(removedEntity -> getMapper().getPrimaryKeyDtoFromEntity(removedEntity))
                    .forEach(removedPrimaryKey -> currentlyPersistedEntitiesByPrimaryKey.remove(removedPrimaryKey));
        }

        if (!entitiesToSave.isEmpty()) {
            List<ENTITY> persistedSavedEntities = saveEntityList(
                    entitiesToSave,
                    newEntitiesToSave);
            if (integrationLoggingContext != null) {
                integrationLoggingContext.addSavedRecords(persistedSavedEntities.size());
            }
            persistedSavedEntities.stream() // não é paralelo pois se trata de um hashmap regular
                    .forEach(savedEntity -> {
                        PRIMARYKEYDTO savedPrimaryKey = getMapper().getPrimaryKeyDtoFromEntity(savedEntity);
                        currentlyPersistedEntitiesByPrimaryKey.put(savedPrimaryKey, savedEntity);
                    });
        }


    }

    public default void removeDtoList(Collection<PRIMARYKEYDTO> primaryKeyDtoCollection) {
        Collection<ENTITY> entityCollection = getPersistedEntityCollectionFromPrimaryKeyDtoCollection(primaryKeyDtoCollection);
        removeEntityList(entityCollection);

        IntegrationLoggingContext integrationLoggingContext = IntegrationLoggingContext.getCurrent();
        if (integrationLoggingContext != null) {
            integrationLoggingContext.addRemovedRecords(entityCollection.size());
        }
    }

    public default void persistDTOList(
            IntegrationDto<DTO,PRIMARYKEYDTO,DATAFILTER,OPTIONS> integrationDto,
            SUPPORTDATA supportData) {

        if (integrationDto == null) {
            throw new DataUploadException("Integration DTO envelope is required.");
        }
        if (integrationDto.data == null) {
            throw new DataUploadException("Integration DTO data list is required.");
        }

        MAPPER mapper = getMapper();

        integrationDto.data.forEach(IntegrationDataDtoAbstract::normalizaCamposTextoEntradaIntegracao);

        Set<PRIMARYKEYDTO> primaryKeyDtoSet =
                getPrimaryKeyDtoSetObrigatorioParaPayload(
                        integrationDto.data);
        // mapa de entidades relativa a 100% dos DTOs lançados
        Map<PRIMARYKEYDTO, ENTITY> currentlyPersistedEntitiesByPrimaryKey = getPersistedEntityMapFromPrimaryKeyDtoCollection(primaryKeyDtoSet);

        int linesPerBatch = getBatchSize();
        if (linesPerBatch <= 0) linesPerBatch = integrationDto.data.size();

        int initialBatchPosition = 0;

        while (integrationDto.data.size() > 0) {

            linesPerBatch = Math.min(integrationDto.data.size(), linesPerBatch);

            List<DTO> dtoBatchList = integrationDto.data.subList(0, linesPerBatch);

            persistDTOBatchList(
                    dtoBatchList,
                    currentlyPersistedEntitiesByPrimaryKey,
                    supportData,
                    initialBatchPosition,
                    integrationDto.dataFieldConfiguration,
                    integrationDto.options);

            int currentBatchSize = dtoBatchList.size();

            for (int i=0; i<currentBatchSize; i++) {
                integrationDto.data.remove(0);
            }
            initialBatchPosition += currentBatchSize;

        }

    }

    /**
     * MultipartFile -> ProcessedFile -> lista de DTOs -> lista de entidades, que será salva
     * Salva o ProcessedFile
     * @param multipartFile
     * @return
     */
    public default String saveFile(
            MultipartFile multipartFile,
            @Nullable IntegrationDto<DTO,PRIMARYKEYDTO,DATAFILTER,OPTIONS> integrationDto) {

        try {
            return saveFile(
                    FileProcessing.getProcessedFileFromMultipartFile(multipartFile),
                    getSupportData(),
                    integrationDto);
        } catch (IOException ioe) {
            /*
             * A leitura do multipart e a unica parte checked deste fluxo. O
             * erro publico continua funcional, mas a causa original precisa
             * sobreviver para diagnostico de arquivo corrompido/stream quebrada.
             */
            log.error("Erro ao converter arquivo multipart de integracao para ProcessedFile", ioe);
            throw new DataUploadException(ioe.getMessage(), ioe);
        }

    }

    public default String saveFile(
            ProcessedFile processedFile,
            SUPPORTDATA supportData,
            @Nullable IntegrationDto<DTO,PRIMARYKEYDTO,DATAFILTER,OPTIONS> integrationDto) {

        MAPPER mapper = getMapper();

        if (integrationDto == null) {
            integrationDto = new IntegrationDto<>();
        }
        List<DTO> dtoList = mapper.convertProcessedFileToDTOList(processedFile, supportData);
        integrationDto.data = dtoList;
        persistDTOList(integrationDto, supportData);

        return getSaveSuccessMessage();

    }

    public String getSaveSuccessMessage();

    public SUPPORTDATA getSupportData();

    /**
     * Se 0, nao dividir a importacao em lotes.
     * @return numero de linhas salvas por lote; se 0, nao dividir em lotes.
     */
    public int getBatchSize();

    public Collection<ENTITY> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<PRIMARYKEYDTO> dtoBatchList);
    public default Map<PRIMARYKEYDTO,ENTITY> getPersistedEntityMapFromPrimaryKeyDtoCollection(Collection<PRIMARYKEYDTO> dtoBatchList) {
        Collection<ENTITY> persistedEntityCollection =
                getPersistedEntityCollectionFromPrimaryKeyDtoCollection(dtoBatchList);
        if (persistedEntityCollection == null) {
            throw new DataUploadException("Persisted integration entity snapshot returned null.");
        }

        /*
         * Este mapa alimenta todos os upserts/deletes do batch. Falhar aqui com
         * contexto de snapshot evita que entidade nula, chave nula ou chave
         * duplicada vire erro generico de stream dentro de uma carga funcional.
         */
        Map<PRIMARYKEYDTO, ENTITY> currentlyPersistedEntitiesByPrimaryKey = new LinkedHashMap<>();
        int indice = 0;
        for (ENTITY entity : persistedEntityCollection) {
            if (entity == null) {
                throw new DataUploadException("Persisted integration entity snapshot returned null item at index " + indice + ".");
            }

            PRIMARYKEYDTO primaryKeyDto = getMapper().getPrimaryKeyDtoFromEntity(entity);
            if (primaryKeyDto == null) {
                throw new DataUploadException("Persisted integration entity snapshot returned item without primary key at index " + indice + ".");
            }
            if (currentlyPersistedEntitiesByPrimaryKey.put(primaryKeyDto, entity) != null) {
                throw new DataUploadException(
                        "Persisted integration entity snapshot returned duplicated primary key "
                                + primaryKeyDto
                                + ".");
            }

            indice++;
        }
        return currentlyPersistedEntitiesByPrimaryKey;
    }
    public Collection<ENTITY> getAllPersistedEntities();


    /**
     * Retorna as entidades persistidas depois de aplicar o filtro funcional da
     * integracao, quando a carga oferecer esse tipo de recorte.
     *
     * <p>A implementacao padrao retorna a base completa porque varias cargas
     * Community, como materiais e locations, nao possuem filtro funcional no
     * download/template. Services que exponham filtros devem sobrescrever este
     * metodo e documentar o subconjunto aceito.</p>
     */
    public default Collection<ENTITY> getFilteredPersistedEntities(DATAFILTER dataFilter) {
        return getAllPersistedEntities();
    }

    /**
     * Valida o snapshot de entidades usado por downloads/listagens de
     * integracao antes de entregar a colecao ao mapper.
     *
     * <p>Lista vazia e uma fotografia valida para cadastros ainda nao
     * carregados. Colecao nula ou item nulo indicam quebra do repository/service
     * concreto e devem falhar com contexto antes de gerar arquivo, DTO ou NPE
     * dentro do mapper.</p>
     */
    public default Collection<ENTITY> validaPersistedEntityCollectionForRead(
            Collection<ENTITY> persistedEntityCollection,
            String persistedEntityCollectionDescription) {

        if (persistedEntityCollection == null) {
            throw new IllegalStateException(persistedEntityCollectionDescription + " returned null.");
        }

        int indice = 0;
        for (ENTITY entity : persistedEntityCollection) {
            if (entity == null) {
                throw new IllegalStateException(
                        persistedEntityCollectionDescription
                                + " returned null item at index "
                                + indice
                                + ".");
            }
            indice++;
        }

        return persistedEntityCollection;

    }

    /**
     * Valida a lista DTO produzida pelo mapper de integracao para download ou
     * consumo direto da SPA.
     *
     * <p>Todo DTO de integracao precisa ter chave primaria porque a chave e
     * desdobrada no arquivo/template e tambem identifica linhas em remocoes ou
     * merges futuros. Validar esse snapshot na borda generica evita que um
     * mapper Community/Enterprise quebrado publique linhas sem identidade.</p>
     */
    public default List<DTO> validaIntegrationDTOListForRead(
            List<DTO> dtoList,
            String dtoListDescription) {

        if (dtoList == null) {
            throw new IllegalStateException(dtoListDescription + " returned null.");
        }

        for (int indice = 0; indice < dtoList.size(); indice++) {
            DTO dto = dtoList.get(indice);
            if (dto == null) {
                throw new IllegalStateException(
                        dtoListDescription + " returned null item at index " + indice + ".");
            }
            if (dto.primaryKeyDto == null) {
                throw new IllegalStateException(
                        dtoListDescription + " returned item without primary key at index " + indice + ".");
            }
            if (dto.primaryKeyDto.allFieldsAreEmpty()) {
                throw new IllegalStateException(
                        dtoListDescription + " returned item with empty primary key at index " + indice + ".");
            }
        }

        return dtoList;

    }

    /**
     * Remove entidades filtradas quando a carga publica suportar delecao por
     * filtro. O default falha explicitamente para impedir remocoes amplas
     * acidentais em services que nao implementam esse contrato.
     *
     * <p>Services Community que realmente suportam remocao filtrada, como
     * sell-out e estoque inicial, devem sobrescrever este metodo e validar o
     * filtro obrigatorio antes de chamar repository. Services Enterprise seguem
     * o mesmo contrato no overlay privado.</p>
     */
    public default void removeFilteredPersistedEntities(DATAFILTER dataFilter) {
        throw new IllegalStateException(
                "Filtered data deletion reached an integration service that does not implement this contract");
    }

    /**
     * Valida e indexa as chaves primarias declaradas no payload de integracao.
     *
     * <p>O fluxo generico consulta entidades persistidas uma vez por lote
     * logico usando um {@link Set} de primary keys. Sem esta validacao, duas
     * linhas do arquivo/API com a mesma chave seriam deduplicadas antes do
     * lookup, e a ambiguidade so apareceria tarde ou seria mascarada pelo
     * ultimo save. Linhas totalmente vazias continuam no-op, porque o batch
     * concreto tambem as ignora ao salvar/remover.</p>
     */
    public default Set<PRIMARYKEYDTO> getPrimaryKeyDtoSetObrigatorioParaPayload(
            List<DTO> dtoList) {

        if (dtoList == null) {
            throw new DataUploadException("Integration DTO data list is required.");
        }

        Set<PRIMARYKEYDTO> primaryKeyDtoSet = new LinkedHashSet<>();
        for (int indice = 0; indice < dtoList.size(); indice++) {
            DTO dto = dtoList.get(indice);
            if (dto == null) {
                throw new DataUploadException("Integration DTO data list returned null item at index " + indice + ".");
            }
            if (dto.allFieldsAreEmpty()) {
                continue;
            }
            if (dto.primaryKeyDto == null) {
                throw new DataUploadException("Integration DTO data list returned item without primary key at index " + indice + ".");
            }
            if (dto.primaryKeyDto.allFieldsAreEmpty()) {
                throw new DataUploadException("Integration DTO data list returned item with empty primary key at index " + indice + ".");
            }
            if (!primaryKeyDtoSet.add(dto.primaryKeyDto)) {
                throw new DataUploadException(
                        "Integration DTO data list returned duplicated primary key "
                                + dto.primaryKeyDto
                                + " at index "
                                + indice
                                + ".");
            }
        }

        return primaryKeyDtoSet;

    }

    /**
     *
     * @param dto
     * @param dtoBatchList
     * @param currentlyPersistedEntitiesByPrimaryKey
     * @param supportData
     * @param camposASobrecrever
     * @param options
     * @param initialBatchPosition
     * @param integrationLoggingContext contexto de logging da request, usado para registrar registros ignorados
     * @return
     */
    public default ENTITY convertDTOToEntityAndTreatError(
            DTO dto, List<DTO> dtoBatchList,
            Map<PRIMARYKEYDTO, ENTITY> currentlyPersistedEntitiesByPrimaryKey,
            SUPPORTDATA supportData,
            @Nullable Map<String,MetodoAtualizacaoCampo> camposASobrecrever,
            @Nullable OPTIONS options,
            Integer initialBatchPosition,
            @Nullable IntegrationLoggingContext integrationLoggingContext) {
        try {
            ENTITY entity = getMapper().convertDTOToEntity(
                    dto,
                    currentlyPersistedEntitiesByPrimaryKey,
                    supportData,
                    camposASobrecrever);

            if (options != null) executaAcaoColateralOptionsNaConversaoDtoEmEntidade(dto, entity, options, supportData);

            return entity;
        } catch (MissingDependencyDataUploadException missingDependencyDataUploadException) {
            // Se skipRecordsWithUnresolvedDependencies estiver ativo, o DTO problemático é descartado e contabilizado no contexto da request.
            if (options != null && options.skipRecordsWithUnresolvedDependencies != null && options.skipRecordsWithUnresolvedDependencies) {
                if (integrationLoggingContext != null) {
                    int posicaoDTONoBatchAtual = dtoBatchList.indexOf(dto);
                    integrationLoggingContext.recordIgnoredError(
                            initialBatchPosition + posicaoDTONoBatchAtual + 1 + 1,
                            missingDependencyDataUploadException,
                            dto);
                }
                return null; // para que saia do filtro na stream
            }
            throw missingDependencyDataUploadException;
        } catch (RuntimeException runtimeException) {
            /*
             * Os mappers de integracao nao declaram excecoes checked. Esta
             * borda converte falhas funcionais/unchecked em DataUploadException
             * com numero de linha, mantendo a causa tecnica original para
             * diagnostico sem alterar o contrato publico do upload.
             */
            int posicaoDTONoBatchAtual = dtoBatchList.indexOf(dto);
            log.error("Erro ao converter DTO de integracao na linha {}", initialBatchPosition + posicaoDTONoBatchAtual + 2, runtimeException);
            // + 1 para transformar de escala com inicio em 0 para escala com inicio em 1
            // outro +1 para considerar cabeçalho
            throw new DataUploadException(
                    runtimeException.getMessage() + " at line " + (initialBatchPosition + posicaoDTONoBatchAtual + 1 + 1),
                    runtimeException);
        }

    }

    /**
     * Metodo chamado quando um OPTIONS é passado como argumento. Pode por exemplo salvar um registro em outra
     * entidade/tabela que não seja o alvo desta integração
     */
    public void executaAcaoColateralOptionsNaConversaoDtoEmEntidade(DTO dto, ENTITY entity, OPTIONS options, SUPPORTDATA supportData);

    public default ProcessedFile getFullFile() {

        MAPPER mapper = getMapper();
        SUPPORTDATA supportData = getSupportData();
        Collection<ENTITY> persistedEntityCollection =
                validaPersistedEntityCollectionForRead(
                        getAllPersistedEntities(),
                        "Full integration entity snapshot");

        return mapper.convertEntityCollectionToProcessedFile(
                persistedEntityCollection,
                supportData);

    }

    public default ProcessedFile getFilteredFile(DATAFILTER dataFilter) {

        MAPPER mapper = getMapper();
        SUPPORTDATA supportData = getSupportData();
        Collection<ENTITY> persistedEntityCollection =
                validaPersistedEntityCollectionForRead(
                        getFilteredPersistedEntities(dataFilter),
                        "Filtered integration entity snapshot");

        return mapper.convertEntityCollectionToProcessedFile(
                persistedEntityCollection,
                supportData);

    }



    public default List<List<Object>> getFullFileContents() {

        return getFullFile().getFileContentsAsObjects();

    }

    public default List<List<Object>> getFilteredFileContents(DATAFILTER dataFilter) {

        return getFilteredFile(dataFilter).getFileContentsAsObjects();

    }

    public default List<DTO> getFullDTOList() {

        MAPPER mapper = getMapper();
        Collection<ENTITY> persistedEntityCollection =
                validaPersistedEntityCollectionForRead(
                        getAllPersistedEntities(),
                        "Full integration entity snapshot");

        return validaIntegrationDTOListForRead(
                mapper.convertEntityCollectionToDTOList(persistedEntityCollection),
                "Full integration DTO list snapshot");

    }

    public default List<DTO> getFilteredDTOList(DATAFILTER dataFilter) {

        MAPPER mapper = getMapper();
        Collection<ENTITY> persistedEntityCollection =
                validaPersistedEntityCollectionForRead(
                        getFilteredPersistedEntities(dataFilter),
                        "Filtered integration entity snapshot");

        return validaIntegrationDTOListForRead(
                mapper.convertEntityCollectionToDTOList(persistedEntityCollection),
                "Filtered integration DTO list snapshot");

    }

    public default String saveDTOList(IntegrationDto<DTO,PRIMARYKEYDTO,DATAFILTER,OPTIONS> integrationDto) {
        persistDTOList(integrationDto, getSupportData());
        return getSaveSuccessMessage();
    }

    /**
     * Atalho usado por cargas que nao precisam de configuracoes adicionais no
     * payload. O fluxo principal continua sendo o overload com
     * {@link IntegrationDto}, onde filtros/opcoes e configuracao de campos
     * podem ser informados explicitamente.
     */
    public default String saveFile(MultipartFile multipartFile) {
        return saveFile(multipartFile, null);
    }

    /**
     * Atalho para importar DTOs ja materializados quando a chamada nao possui
     * filtro, opcoes ou configuracao de campos.
     */
    public default String saveDTOList(List<DTO> dtoList) {
        IntegrationDto<DTO,PRIMARYKEYDTO,DATAFILTER,OPTIONS> integrationDto = IntegrationDto.<DTO,PRIMARYKEYDTO,DATAFILTER,OPTIONS>builder()
                .data(dtoList)
                .dataFilter(null)
                .options(null)
                .dataFieldConfiguration(null)
                .build();
        return saveDTOList(integrationDto);
    }

}
