package com.opsfactor.community.capability.supplyplanning.productionplan.integration;

import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutivaFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.service.SupplyPlanPersistedBaselinePreflight;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.SetupPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.SetupPlanLinhaRepository;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Enterprise read-only para exportacao do Production Plan no nivel de
 * ocupacao por recurso produtivo.
 */
@Service
public class ProductionPlanOccupationIntegrationService implements IntegrationServiceInterface<
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto,
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO,
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto,
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationSupportData,
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationMapper,
        EmptyIntegrationDataFilter> {

    private static final String ROUTING_WITHOUT_OPERATIONS_RESOURCE_ID =
            "NO OPERATIONS / PRODUCTION RESOURCES ASSOCIATED WITH ROUTING";

    /**
     * Repository Community dos supply plans usados como filtro obrigatorio.
     */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /**
     * Repository Community das linhas de Production Plan persistidas pelo
     * calculo de Supply Planning.
     */
    @Autowired
    private ProductionPlanLinhaRepository productionPlanLinhaRepository;

    /** Gate Community compartilhado das series baseline persistidas antigas. */
    @Autowired
    private SupplyPlanPersistedBaselinePreflight supplyPlanPersistedBaselinePreflight;

    /**
     * Repository Enterprise dos setups persistidos por scheduling/optimizer.
     */
    @Autowired
    private SetupPlanLinhaRepository setupPlanLinhaRepository;

    /**
     * Service Community de parametros globais para resolver UOM padrao SNP e
     * calendario do supply plan.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Factory Community da malha produtiva usada para calcular consumo por
     * recurso a partir do roteiro.
     */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /**
     * Factory Community da projection de conversoes de unidade.
     */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /**
     * Factory da projection de capacidade produtiva por periodo.
     */
    @Autowired
    private BIProjectionCapacidadeProdutivaFactory biProjectionCapacidadeProdutivaFactory;

    /**
     * Mapper Enterprise do contrato de arquivo/API de Production Plan
     * Occupation.
     */
    @Autowired
    private com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationMapper productionPlanOccupationIntegrationMapper;

    @Override
    public com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationMapper getMapper() {

        return productionPlanOccupationIntegrationMapper;

    }

    /**
     * Bloqueia persistencia manual: Production Plan Occupation e output do
     * motor de Supply Planning e do line scheduling.
     */
    @Override
    public List<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> saveEntityList(
            Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> entityList) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia remocao manual pelo Data Upload generico.
     */
    @Override
    public void removeEntityList(
            Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> entityList) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia upload por arquivo antes de qualquer parse de dados.
     */
    @Override
    public String saveFile(
            MultipartFile multipartFile) {

        throw getReadOnlyException();

    }

    /**
     * Bloqueia upload JSON antes de reconciliar linhas persistidas.
     */
    @Override
    public String saveDTOList(
            IntegrationDto<
                    com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto,
                    com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO,
                    EmptyIntegrationDataFilter,
                    IntegrationOptionsDto> integrationDto) {

        throw getReadOnlyException();

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Production Plan Occupation data exported";

    }

    @Override
    public com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationSupportData getSupportData() {

        return new com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationSupportData();

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Reconciliacao por envelope de supply plans, usada apenas se a superficie
     * generica tentar materializar chaves deste recorte read-only.
     */
    @Override
    public Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO> primaryKeyCollection =
                com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationValidation.validaPrimaryKeyCollection(
                        dtoBatchList,
                        "Production Plan Occupation primary key collection");

        if (primaryKeyCollection.isEmpty()) {
            return List.of();
        }

        Set<Long> supplyPlanIds = primaryKeyCollection.stream()
                .map(primaryKey -> primaryKey.supplyPlanId)
                .collect(Collectors.toSet());

        supplyPlanPersistedBaselinePreflight.assertSupplyPlanIdsReadyForCanonicalRuntime(
                supplyPlanIds);

        return buildDtoListForSupplyPlanIds(
                supplyPlanIds,
                productionPlanLinhaRepository.customFindBySupplyPlanIdInForProductionPlanOccupationExport(
                        supplyPlanIds),
                setupPlanLinhaRepository.customFindBySupplyPlanIdInForProductionPlanOccupationExport(
                        supplyPlanIds),
                "Production Plan Occupation persisted collection");

    }

    /**
     * Full export sem filtro e bloqueado para evitar dataset amplo demais.
     */
    @Override
    public Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> getAllPersistedEntities() {

        throw new UnsupportedOperationException(
                "Full Production Plan Occupation export requires a supplyPlanId filter.");

    }

    /**
     * Extrai o Production Plan Occupation de um supply plan especifico.
     */
    public List<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> getProductionPlanOccupationDTOList(
            Long supplyPlanId) {

        Long validatedSupplyPlanId =
                com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationValidation.validaSupplyPlanId(supplyPlanId);

        SupplyPlan supplyPlan = supplyPlanRepository.customFindById(supplyPlanId)
                .orElseThrow(() -> new DataUploadException("Supply Plan Id not found"));
        supplyPlanPersistedBaselinePreflight.assertSupplyPlanReadyForCanonicalRuntime(
                validatedSupplyPlanId);
        return buildDtoListForSupplyPlan(
                supplyPlan,
                productionPlanLinhaRepository.customFindBySupplyPlanIdForProductionPlanOccupationExport(
                        validatedSupplyPlanId),
                setupPlanLinhaRepository.customFindBySupplyPlanIdForProductionPlanOccupationExport(
                        validatedSupplyPlanId),
                "Production Plan Occupation persisted collection");

    }

    /**
     * Gera arquivo processado filtrado por supply plan.
     */
    public ProcessedFile getProcessedFileBySupplyPlanId(
            Long supplyPlanId) {

        return getMapper().convertEntityCollectionToProcessedFile(
                getProductionPlanOccupationDTOList(supplyPlanId),
                getSupportData());

    }

    /**
     * Compatibilidade com o nome usado pelo controller legado.
     */
    public List<List<Object>> getFile(
            Long supplyPlanId) {

        return getProcessedFileBySupplyPlanId(supplyPlanId)
                .getFileContentsAsObjects();

    }

    private List<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> buildDtoListForSupplyPlanIds(
            Collection<Long> supplyPlanIds,
            Collection<ProductionPlanLinha> productionPlanLinhas,
            Collection<SetupPlanLinha> setupPlanLinhas,
            String collectionDescription) {

        if (productionPlanLinhas == null) {
            throw new IllegalStateException(collectionDescription + " production plan query returned null.");
        }
        if (setupPlanLinhas == null) {
            throw new IllegalStateException(collectionDescription + " setup query returned null.");
        }

        List<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> dtoList = new ArrayList<>();
        for (Long supplyPlanId : supplyPlanIds.stream().sorted().toList()) {
            SupplyPlan supplyPlan = supplyPlanRepository.customFindById(supplyPlanId)
                .orElseThrow(() -> new DataUploadException("Supply Plan Id not found"));
            dtoList.addAll(buildDtoListForSupplyPlan(
                    supplyPlan,
                    filterProductionPlanLinhasBySupplyPlanId(
                            productionPlanLinhas,
                            supplyPlanId),
                    filterSetupPlanLinhasBySupplyPlanId(
                            setupPlanLinhas,
                            supplyPlanId),
                    collectionDescription + " for supply plan " + supplyPlanId));
        }

        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationValidation.validaDtoCollection(
                dtoList,
                collectionDescription);
        return sortDtoList(dtoList);

    }

    private List<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> buildDtoListForSupplyPlan(
            SupplyPlan supplyPlan,
            Collection<ProductionPlanLinha> productionPlanLinhas,
            Collection<SetupPlanLinha> setupPlanLinhas,
            String collectionDescription) {

        if (productionPlanLinhas == null) {
            throw new IllegalStateException(collectionDescription + " production plan query returned null.");
        }
        if (setupPlanLinhas == null) {
            throw new IllegalStateException(collectionDescription + " setup query returned null.");
        }

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        SupplyNetworkProjection supplyNetworkProjection =
                supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        UnidadeMedidaProjection unidadeMedidaProjection =
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();
        Calendario calendarioSupplyPlan =
                supplyPlan.getCalendarioDoSupplyPlan(parametrosGlobais);
        BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva =
                biProjectionCapacidadeProdutivaFactory.getBIProjectionCapacidadeProdutiva(
                        supplyPlan,
                        calendarioSupplyPlan);
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan =
                supplyPlan.getPerfilExecucaoSupplyPlanCadastrado();
        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva =
                perfilExecucaoSupplyPlan.getTipoCapacidadeProdutiva();

        List<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> dtoList = new ArrayList<>();
        Map<Long, Map<String, Map<String, Map<String, Map<String, Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto>>>>>>
                dtoByIndexKey =
                new HashMap<>();

        for (ProductionPlanLinha productionPlanLinha : productionPlanLinhas) {
            addProductionPlanLinhaDtos(
                    dtoList,
                    dtoByIndexKey,
                    productionPlanLinha,
                    supplyPlan,
                    tipoCapacidadeProdutiva,
                    parametrosGlobais,
                    supplyNetworkProjection,
                    unidadeMedidaProjection,
                    calendarioSupplyPlan,
                    biProjectionCapacidadeProdutiva);
        }

        for (SetupPlanLinha setupPlanLinha : setupPlanLinhas) {
            addOrUpdateSetupDto(
                    dtoList,
                    dtoByIndexKey,
                    setupPlanLinha,
                    supplyPlan,
                    tipoCapacidadeProdutiva,
                    parametrosGlobais,
                    calendarioSupplyPlan,
                    biProjectionCapacidadeProdutiva);
        }

        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationValidation.validaDtoCollection(
                dtoList,
                collectionDescription);
        return sortDtoList(dtoList);

    }

    private void addProductionPlanLinhaDtos(
            List<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> dtoList,
            Map<Long, Map<String, Map<String, Map<String, Map<String, Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto>>>>>>
                    dtoByIndexKey,
            ProductionPlanLinha productionPlanLinha,
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva,
            ParametrosGlobais parametrosGlobais,
            SupplyNetworkProjection supplyNetworkProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            Calendario calendarioSupplyPlan,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva) {

        Map<RecursoProdutivo, Double> unrestrictedResourceConsumptionMap =
                new LinkedHashMap<>(productionPlanLinha.getCapacidadeConsumidaPorRecursoProdutivoEmHorasOuQuantidade(
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        Constantes.FirmePlanejado.TOTAL,
                        tipoCapacidadeProdutiva,
                        supplyNetworkProjection));
        Map<RecursoProdutivo, Double> constrainedResourceConsumptionMap =
                new LinkedHashMap<>(productionPlanLinha.getCapacidadeConsumidaPorRecursoProdutivoEmHorasOuQuantidade(
                        Constantes.TipoPlano.PLANO_RESTRITO,
                        Constantes.FirmePlanejado.TOTAL,
                        tipoCapacidadeProdutiva,
                        supplyNetworkProjection));

        boolean routingHasOperations = !unrestrictedResourceConsumptionMap.isEmpty();
        if (!routingHasOperations) {
            RecursoProdutivo routingWithoutOperationsResource =
                    buildRoutingWithoutOperationsResource(
                            productionPlanLinha.getLocation(),
                            parametrosGlobais);
            unrestrictedResourceConsumptionMap.put(
                    routingWithoutOperationsResource,
                    0.0d);
            constrainedResourceConsumptionMap.put(
                    routingWithoutOperationsResource,
                    0.0d);
        }

        for (Map.Entry<RecursoProdutivo, Double> resourceConsumptionEntry : unrestrictedResourceConsumptionMap.entrySet()) {
            RecursoProdutivo recursoProdutivo = resourceConsumptionEntry.getKey();
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto dto =
                    buildProductionDto(
                            productionPlanLinha,
                            recursoProdutivo,
                            routingHasOperations,
                            supplyPlan,
                            tipoCapacidadeProdutiva,
                            parametrosGlobais,
                            unidadeMedidaProjection,
                            calendarioSupplyPlan,
                            biProjectionCapacidadeProdutiva,
                            resourceConsumptionEntry.getValue(),
                            constrainedResourceConsumptionMap.getOrDefault(
                                    recursoProdutivo,
                                    0.0d));
            putDtoByPrimaryKey(
                    dtoByIndexKey,
                    dto.primaryKeyDto,
                    dto);
            dtoList.add(dto);
        }

    }

    private void addOrUpdateSetupDto(
            List<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> dtoList,
            Map<Long, Map<String, Map<String, Map<String, Map<String, Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto>>>>>>
                    dtoByIndexKey,
            SetupPlanLinha setupPlanLinha,
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva,
            ParametrosGlobais parametrosGlobais,
            Calendario calendarioSupplyPlan,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva) {

        Long supplyPlanId = supplyPlan.getId();
        String resourceId = setupPlanLinha.getRecursoProdutivo().getId();
        String productionVersionId =
                getProductionVersionId(setupPlanLinha.getVersaoProducaoCadastrada());
        String routingId = setupPlanLinha.getRoteiro().getId();
        String billOfMaterialsId = setupPlanLinha.getListaTecnica().getId();
        java.time.LocalDateTime plannedDate = setupPlanLinha.getDataReferencia();
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto dto = getDtoByPrimaryKey(
                dtoByIndexKey,
                supplyPlanId,
                resourceId,
                productionVersionId,
                routingId,
                billOfMaterialsId,
                plannedDate);

        if (dto == null) {
            dto = buildSetupOnlyDto(
                    setupPlanLinha,
                    supplyPlan,
                    tipoCapacidadeProdutiva,
                    parametrosGlobais,
                    calendarioSupplyPlan,
                    biProjectionCapacidadeProdutiva);
            putDtoByPrimaryKey(
                    dtoByIndexKey,
                    dto.primaryKeyDto,
                    dto);
            dtoList.add(dto);
        }

        populateSetupColumns(
                dto,
                setupPlanLinha,
                tipoCapacidadeProdutiva);

    }

    private com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto buildProductionDto(
            ProductionPlanLinha productionPlanLinha,
            RecursoProdutivo recursoProdutivo,
            boolean routingHasOperations,
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva,
            ParametrosGlobais parametrosGlobais,
            UnidadeMedidaProjection unidadeMedidaProjection,
            Calendario calendarioSupplyPlan,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            Double unrestrictedResourceConsumption,
            Double constrainedResourceConsumption) {

        Produto materialOutput = productionPlanLinha.getMaterialOutput();
        UnidadeMedida productionPlanLinhaUnitOfMeasure =
                productionPlanLinha.getUnidadeMedida(parametrosGlobais);
        VersaoProducao versaoProducao =
                productionPlanLinha.getVersaoProducaoCadastrada();
        int periodPosition =
                calendarioSupplyPlan.getPosicaoPeriodo(productionPlanLinha.getDataReferencia());
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto dto =
                com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.builder()
                        .primaryKeyDto(new com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO(
                                supplyPlan.getId(),
                                productionPlanLinha.getLocation().getId(),
                                materialOutput.getId(),
                                productionPlanLinha.getDataReferencia(),
                                getProductionVersionId(versaoProducao),
                                productionPlanLinha.getRoteiro().getId(),
                                productionPlanLinha.getListaTecnica().getId(),
                                recursoProdutivo.getId()))
                        .routingClusterId(getRoutingClusterId(
                                productionPlanLinha.getRoteiro()))
                        .resourceCapacityUnitOfMeasureId(getResourceCapacityUnitOfMeasureId(
                                recursoProdutivo,
                                routingHasOperations,
                                tipoCapacidadeProdutiva,
                                parametrosGlobais))
                        .resourceCapacityPeriod(getResourceCapacityPeriod(
                                recursoProdutivo,
                                routingHasOperations,
                                periodPosition,
                                tipoCapacidadeProdutiva,
                                biProjectionCapacidadeProdutiva))
                        .unitOfMeasureId(productionPlanLinhaUnitOfMeasure.getId())
                        .unconstrainedPlannedOrderQuantity(
                                productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoIrrestrita())
                        .unconstrainedFirmOrderQuantity(
                                productionPlanLinha.getQuantidadeOrdemFirmeProducaoIrrestrita())
                        .unconstrainedTotalResourceConsumption(unrestrictedResourceConsumption)
                        .constrainedPlannedOrderQuantity(
                                productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoRestrita())
                        .constrainedFirmOrderQuantity(
                                productionPlanLinha.getQuantidadeOrdemFirmeProducaoRestrita())
                        .constrainedTotalResourceConsumption(constrainedResourceConsumption)
                        .defaultSnpUnitOfMeasureId(
                                parametrosGlobais.getUnidadeMedidaPadraoSNP().getId())
                        .build();

        populateDefaultSnpUnitQuantities(
                dto,
                productionPlanLinha,
                materialOutput,
                productionPlanLinhaUnitOfMeasure,
                parametrosGlobais,
                unidadeMedidaProjection);
        return dto;

    }

    private com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto buildSetupOnlyDto(
            SetupPlanLinha setupPlanLinha,
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva,
            ParametrosGlobais parametrosGlobais,
            Calendario calendarioSupplyPlan,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva) {

        Roteiro roteiro = setupPlanLinha.getRoteiro();
        ListaTecnica listaTecnica = setupPlanLinha.getListaTecnica();
        RecursoProdutivo recursoProdutivo = setupPlanLinha.getRecursoProdutivo();
        int periodPosition =
                calendarioSupplyPlan.getPosicaoPeriodo(setupPlanLinha.getDataReferencia());

        return com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.builder()
                .primaryKeyDto(new com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO(
                        supplyPlan.getId(),
                        recursoProdutivo.getLocation().getId(),
                        roteiro.getMaterialOutput().getId(),
                        setupPlanLinha.getDataReferencia(),
                        getProductionVersionId(setupPlanLinha.getVersaoProducaoCadastrada()),
                        roteiro.getId(),
                        listaTecnica.getId(),
                        recursoProdutivo.getId()))
                .routingClusterId(getRoutingClusterId(roteiro))
                .resourceCapacityUnitOfMeasureId(getResourceCapacityUnitOfMeasureId(
                        recursoProdutivo,
                        true,
                        tipoCapacidadeProdutiva,
                        parametrosGlobais))
                .resourceCapacityPeriod(getResourceCapacityPeriod(
                        recursoProdutivo,
                        true,
                        periodPosition,
                        tipoCapacidadeProdutiva,
                        biProjectionCapacidadeProdutiva))
                .build();

    }

    private void populateSetupColumns(
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto dto,
            SetupPlanLinha setupPlanLinha,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva) {

        if (PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM.equals(tipoCapacidadeProdutiva)) {
            dto.setupTimeHours = "Resource Capacity defined in Quantity - Set-up calculation not possible";
            dto.setupSequence = "Resource Capacity defined in Quantity - Set-up calculation not possible";
            return;
        }

        dto.setupTimeHours = setupPlanLinha.getNumeroHorasSetupSequenciamentoSNP();
        dto.setupSequence = setupPlanLinha.getPosicaoSequenciaPeriodoSequenciamentoSNP();

    }

    private void populateDefaultSnpUnitQuantities(
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto dto,
            ProductionPlanLinha productionPlanLinha,
            Produto materialOutput,
            UnidadeMedida productionPlanLinhaUnitOfMeasure,
            ParametrosGlobais parametrosGlobais,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        try {
            double conversionToDefaultSnpUnit =
                    unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                            materialOutput,
                            productionPlanLinhaUnitOfMeasure,
                            parametrosGlobais.getUnidadeMedidaPadraoSNP());

            dto.unconstrainedPlannedOrderQuantityDefaultSnpUom =
                    productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoIrrestrita()
                            * conversionToDefaultSnpUnit;
            dto.unconstrainedFirmOrderQuantityDefaultSnpUom =
                    productionPlanLinha.getQuantidadeOrdemFirmeProducaoIrrestrita()
                            * conversionToDefaultSnpUnit;
            dto.constrainedPlannedOrderQuantityDefaultSnpUom =
                    productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoRestrita()
                            * conversionToDefaultSnpUnit;
            dto.constrainedFirmOrderQuantityDefaultSnpUom =
                    productionPlanLinha.getQuantidadeOrdemFirmeProducaoRestrita()
                            * conversionToDefaultSnpUnit;
        } catch (UnitOfMeasureConversionException unitOfMeasureConversionException) {
            String mensagemErroConversao = "No conversion from "
                    + productionPlanLinhaUnitOfMeasure.getId()
                    + " to "
                    + parametrosGlobais.getUnidadeMedidaPadraoSNP().getId();

            dto.unconstrainedPlannedOrderQuantityDefaultSnpUom = mensagemErroConversao;
            dto.unconstrainedFirmOrderQuantityDefaultSnpUom = mensagemErroConversao;
            dto.constrainedPlannedOrderQuantityDefaultSnpUom = mensagemErroConversao;
            dto.constrainedFirmOrderQuantityDefaultSnpUom = mensagemErroConversao;
        }

    }

    @Nullable
    private String getResourceCapacityUnitOfMeasureId(
            RecursoProdutivo recursoProdutivo,
            boolean routingHasOperations,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva,
            ParametrosGlobais parametrosGlobais) {

        if (!routingHasOperations) {
            return null;
        }
        if (PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM.equals(tipoCapacidadeProdutiva)) {
            return recursoProdutivo.getUnidadeMedidaCapacidadeEmUom(parametrosGlobais).getId();
        }
        return "Hours";

    }

    @Nullable
    private Object getResourceCapacityPeriod(
            RecursoProdutivo recursoProdutivo,
            boolean routingHasOperations,
            int periodPosition,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva) {

        if (!routingHasOperations) {
            return null;
        }
        if (PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM.equals(tipoCapacidadeProdutiva)) {
            return "Quantity-based resource capacity requires the Enterprise capacity projection.";
        }
        if (PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.ALOCACAO_TURNOS.equals(tipoCapacidadeProdutiva)) {
            return "Shift-based resource capacity requires the Enterprise capacity projection.";
        }

        return biProjectionCapacidadeProdutiva.getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                periodPosition,
                recursoProdutivo,
                BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA);

    }

    private RecursoProdutivo buildRoutingWithoutOperationsResource(
            Location location,
            ParametrosGlobais parametrosGlobais) {

        return RecursoProdutivo.builder()
                .id(ROUTING_WITHOUT_OPERATIONS_RESOURCE_ID)
                .location(location)
                .descricao("")
                .ativo(true)
                .eficiencia(1.0f)
                .unidadeMedidaCapacidadeEmUom(parametrosGlobais.getUnidadeMedidaPadraoSNP())
                .build();

    }

    @Nullable
    private String getRoutingClusterId(Roteiro roteiro) {

        /*
         * O roteiro Community preserva somente o identificador escalar do
         * cluster Enterprise. Como as queries deste export ja carregam o
         * roteiro, devolver esse valor nao cria join, projection, tabela ou
         * associacao JPA Community -> Enterprise.
         */
        return roteiro.getRoutingClusterId();

    }

    @Nullable
    private String getProductionVersionId(
            VersaoProducao versaoProducao) {

        return versaoProducao.isVersaoProducaoInexistente()
                ? null
                : versaoProducao.getId();

    }

    private com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto getDtoByPrimaryKey(
            Map<Long, Map<String, Map<String, Map<String, Map<String, Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto>>>>>>
                    dtoByIndexKey,
            Long supplyPlanId,
            String resourceId,
            String productionVersionId,
            String routingId,
            String billOfMaterialsId,
            java.time.LocalDateTime plannedDate) {

        Map<String, Map<String, Map<String, Map<String, Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto>>>>>
                dtoByResource = dtoByIndexKey.get(supplyPlanId);
        Map<String, Map<String, Map<String, Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto>>>>
                dtoByProductionVersion = dtoByResource == null ? null : dtoByResource.get(resourceId);
        Map<String, Map<String, Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto>>>
                dtoByRouting = dtoByProductionVersion == null
                ? null
                : dtoByProductionVersion.get(productionVersionId);
        Map<String, Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto>>
                dtoByBillOfMaterials = dtoByRouting == null ? null : dtoByRouting.get(routingId);
        Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> dtoByPlannedDate =
                dtoByBillOfMaterials == null
                        ? null
                        : dtoByBillOfMaterials.get(billOfMaterialsId);
        return dtoByPlannedDate == null ? null : dtoByPlannedDate.get(plannedDate);

    }

    private void putDtoByPrimaryKey(
            Map<Long, Map<String, Map<String, Map<String, Map<String, Map<java.time.LocalDateTime, com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto>>>>>>
                    dtoByIndexKey,
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO primaryKeyDto,
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto dto) {

        dtoByIndexKey
                .computeIfAbsent(primaryKeyDto.supplyPlanId, ignored -> new HashMap<>())
                .computeIfAbsent(primaryKeyDto.resourceId, ignored -> new HashMap<>())
                .computeIfAbsent(primaryKeyDto.productionVersionId, ignored -> new HashMap<>())
                .computeIfAbsent(primaryKeyDto.routingId, ignored -> new HashMap<>())
                .computeIfAbsent(primaryKeyDto.billOfMaterialsId, ignored -> new HashMap<>())
                .put(primaryKeyDto.plannedDate, dto);

    }

    private List<ProductionPlanLinha> filterProductionPlanLinhasBySupplyPlanId(
            Collection<ProductionPlanLinha> productionPlanLinhas,
            Long supplyPlanId) {

        return productionPlanLinhas.stream()
                .filter(productionPlanLinha -> Objects.equals(
                        productionPlanLinha.getSupplyPlan().getId(),
                        supplyPlanId))
                .toList();

    }

    private List<SetupPlanLinha> filterSetupPlanLinhasBySupplyPlanId(
            Collection<SetupPlanLinha> setupPlanLinhas,
            Long supplyPlanId) {

        return setupPlanLinhas.stream()
                .filter(setupPlanLinha -> Objects.equals(
                        setupPlanLinha.getSupplyPlan().getId(),
                        supplyPlanId))
                .toList();

    }

    private List<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> sortDtoList(
            Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> dtoCollection) {

        return dtoCollection.stream()
                .sorted(Comparator
                        .comparing((com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto dto) -> safeString(dto.primaryKeyDto.locationId))
                        .thenComparing(dto -> safeString(dto.primaryKeyDto.outputMaterialId))
                        .thenComparing(dto -> dto.primaryKeyDto.plannedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(dto -> safeString(dto.primaryKeyDto.resourceId))
                        .thenComparing(dto -> safeString(dto.primaryKeyDto.routingId))
                        .thenComparing(dto -> safeString(dto.primaryKeyDto.billOfMaterialsId)))
                .toList();

    }

    private String safeString(
            String value) {

        return value == null ? "" : value;

    }

    private UnsupportedOperationException getReadOnlyException() {

        return new UnsupportedOperationException(
                "Production Plan Occupation Data Upload is read-only; quantities and setup are persisted by Supply Planning execution.");

    }

}
