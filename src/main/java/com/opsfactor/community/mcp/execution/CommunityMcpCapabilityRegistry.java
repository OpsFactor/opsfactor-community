package com.opsfactor.community.mcp.execution;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opsfactor.community.capability.configuration.facade.ConfiguredViewFacade;
import com.opsfactor.community.capability.configuration.facade.PerfilExecucaoDemandPlanFacade;
import com.opsfactor.community.capability.configuration.facade.PerfilExecucaoSupplyPlanFacade;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningClusterLevelConfigurationDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.PerfilExecucaoDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.DemandPlanningFacade;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.VersaoDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.facade.DemandSimulationFacade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.UnidadeMedidaDataUploadDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service.UnidadeMedidaIntegrationService;
import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.capability.supplyplanning.configuration.facade.dto.PerfilExecucaoSupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.service.InventoryPlanIntegrationService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.SupplyPlanFacade;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.VersaoSupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.service.FulfilledDemandIntegrationService;
import com.opsfactor.community.platform.bi.facade.CommunityMaterialFlowsService;
import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.service.IntegrationServiceComConfiguracoesInterface;
import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.opsfactor.community.platform.task.DemandPlanningTask;
import com.opsfactor.community.platform.task.SupplyPlanningTask;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry explicito que liga ids de folhas MCP aos services Community reais.
 *
 * <p>Controllers REST e tools MCP sao duas bordas diferentes sobre as mesmas
 * fachadas e services. Esta classe nao chama controllers, nao faz HTTP interno
 * e nao escolhe metodos por reflexao livre. Somente a allow-list abaixo pode
 * ser executada.</p>
 */
@Component
public class CommunityMcpCapabilityRegistry {

    private static final Map<String, String> INTEGRATION_CAPABILITY_BY_SERVICE = Map.ofEntries(
            Map.entry("VersaoMalhaIntegrationService", "data.master-data.supply-network-version"),
            Map.entry("LinhaTransporteIntegrationService", "data.master-data.transportation-lane"),
            Map.entry("LinhaTransporteProdutoIntegrationService", "data.master-data.transportation-lane-material"),
            Map.entry("ProdutoIntegrationService", "data.master-data.material"),
            Map.entry("LocationIntegrationService", "data.master-data.location"),
            Map.entry("ConversaoUnidadeIntegrationService", "data.master-data.unit-conversion"),
            Map.entry("ConversaoUnidadeProdutoIntegrationService", "data.master-data.unit-conversion-material"),
            Map.entry("RecursoProdutivoIntegrationService", "data.master-data.production-resource"),
            Map.entry("DisponibilidadeRecursoProdutivoIntegrationService", "data.master-data.production-resource-availability"),
            Map.entry("RoteiroIntegrationService", "data.master-data.production-routing"),
            Map.entry("ListaTecnicaIntegrationService", "data.master-data.bill-of-material"),
            Map.entry("ListaTecnicaComponenteIntegrationService", "data.master-data.bill-of-material-component"),
            Map.entry("VersaoProducaoSimplesIntegrationService", "data.master-data.simple-production-version"),
            Map.entry("PoliticaEstoquesIntegrationService", "data.master-data.inventory-policy"),
            Map.entry("PoliticaEstoquesMaterialLocationIntegrationService", "data.master-data.inventory-policy-detail"),
            Map.entry("EstoqueIntegrationService", "data.transactional-data.stock"),
            Map.entry("SelloutIntegrationService", "data.transactional-data.sellout"),
            Map.entry("ParametrosMaterialLocationIntegrationService", "data.configuration.material-location-parameters"));

    private final ObjectMapper objectMapper;
    private final Map<String, IntegrationCapabilityAdapter> integrationAdapters;
    private final UnidadeMedidaIntegrationService unidadeMedidaIntegrationService;
    private final InventoryPlanIntegrationService inventoryPlanIntegrationService;
    private final FulfilledDemandIntegrationService fulfilledDemandIntegrationService;
    private final PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFacade;
    private final PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFacade;
    private final DemandSimulationFacade demandSimulationFacade;
    private final CommunityMaterialFlowsService communityMaterialFlowsService;
    private final DemandPlanningFacade demandPlanningFacade;
    private final SupplyPlanFacade supplyPlanFacade;
    private final ConfiguredViewFacade configuredViewFacade;
    private final AuthenticationService authenticationService;
    private final WebControllerTaskSchedulingService webControllerTaskSchedulingService;

    /**
     * Recebe apenas services/fachadas funcionais e indexa as integracoes
     * genericas pelos tipos concretos resolvidos pelo Spring.
     */
    @Autowired
    public CommunityMcpCapabilityRegistry(
            ObjectMapper objectMapper,
            List<IntegrationServiceComConfiguracoesInterface<?, ?, ?, ?, ?, ?, ?>> integrationServices,
            UnidadeMedidaIntegrationService unidadeMedidaIntegrationService,
            InventoryPlanIntegrationService inventoryPlanIntegrationService,
            FulfilledDemandIntegrationService fulfilledDemandIntegrationService,
            PerfilExecucaoDemandPlanFacade perfilExecucaoDemandPlanFacade,
            PerfilExecucaoSupplyPlanFacade perfilExecucaoSupplyPlanFacade,
            DemandSimulationFacade demandSimulationFacade,
            CommunityMaterialFlowsService communityMaterialFlowsService,
            DemandPlanningFacade demandPlanningFacade,
            SupplyPlanFacade supplyPlanFacade,
            ConfiguredViewFacade configuredViewFacade,
            AuthenticationService authenticationService,
            WebControllerTaskSchedulingService webControllerTaskSchedulingService) {

        this.objectMapper = objectMapper;
        this.integrationAdapters = indexIntegrationAdapters(integrationServices);
        this.unidadeMedidaIntegrationService = unidadeMedidaIntegrationService;
        this.inventoryPlanIntegrationService = inventoryPlanIntegrationService;
        this.fulfilledDemandIntegrationService = fulfilledDemandIntegrationService;
        this.perfilExecucaoDemandPlanFacade = perfilExecucaoDemandPlanFacade;
        this.perfilExecucaoSupplyPlanFacade = perfilExecucaoSupplyPlanFacade;
        this.demandSimulationFacade = demandSimulationFacade;
        this.communityMaterialFlowsService = communityMaterialFlowsService;
        this.demandPlanningFacade = demandPlanningFacade;
        this.supplyPlanFacade = supplyPlanFacade;
        this.configuredViewFacade = configuredViewFacade;
        this.authenticationService = authenticationService;
        this.webControllerTaskSchedulingService = webControllerTaskSchedulingService;

    }

    /**
     * Executa uma consulta read-only permitida pela folha informada.
     */
    public JsonNode query(String capabilityId, JsonNode payload) {

        IntegrationCapabilityAdapter integrationCapabilityAdapter = integrationAdapters.get(capabilityId);
        if (integrationCapabilityAdapter != null) {
            return queryIntegration(integrationCapabilityAdapter, payload);
        }

        return switch (capabilityId) {
            case "data.master-data.unit-of-measure" -> toJson(unidadeMedidaIntegrationService.getDTOList());
            case "data.planning.inventory-plan" -> toJson(inventoryPlanIntegrationService.getInventoryPlanDTOList(
                    requiredLong(payload, "supplyPlanId")));
            case "data.planning.fulfilled-demand" -> toJson(fulfilledDemandIntegrationService.getFulfilledDemandDtoList(
                    requiredLong(payload, "supplyPlanId"),
                    requiredText(payload, "unitOfMeasureId")));
            case "configuration.demand.execution-profile" -> toJson(
                    perfilExecucaoDemandPlanFacade.getPerfilExecucaoDemandPlanDTOSet());
            case "configuration.supply.execution-profile" -> toJson(
                    perfilExecucaoSupplyPlanFacade.getPerfilExecucaoSupplyPlanDTOSet());
            case "configuration.demand.cluster-level" -> toJson(
                    demandSimulationFacade.getDemandPlanningConfigurationDTO(
                            requiredText(payload, "executionProfileId"),
                            requiredLong(payload, "locationClusterId"),
                            requiredLong(payload, "materialClusterId")));
            case "report.supply.material-flows" -> toJson(
                    communityMaterialFlowsService.getMaterialFlows(requiredLong(payload, "supplyPlanId")));
            case "planning-book.demand" -> toJson(demandPlanningFacade.getPlanningBookDTO(
                    convert(payload, ConfiguredViewSelectionDTO.class),
                    authenticationService.getAuthenticatedUserId()));
            case "planning-book.supply" -> toJson(supplyPlanFacade.getPlanningBookDTO(
                    convert(payload, ConfiguredViewSelectionDTO.class),
                    authenticationService.getAuthenticatedUserId()));
            case "admin.user-view.demand" -> toJson(configuredViewFacade.getConfiguredViewDTOListDemandPlanningBook(
                    authenticationService.getAuthenticatedUserId()));
            case "admin.user-view.supply" -> toJson(configuredViewFacade.getConfiguredViewDTOListSupplyPlanningBook(
                    authenticationService.getAuthenticatedUserId()));
            default -> throw new IllegalArgumentException("MCP capability does not support query: " + capabilityId);
        };

    }

    /**
     * Executa uma alteracao permitida, reutilizando exatamente os DTOs e
     * validacoes dos services Community.
     */
    public JsonNode update(String capabilityId, JsonNode payload) {

        IntegrationCapabilityAdapter integrationCapabilityAdapter = integrationAdapters.get(capabilityId);
        if (integrationCapabilityAdapter != null) {
            return updateIntegration(integrationCapabilityAdapter, payload);
        }

        return switch (capabilityId) {
            case "data.master-data.unit-of-measure" -> toJson(unidadeMedidaIntegrationService.saveDTOList(
                    convertList(payload, UnidadeMedidaDataUploadDTO.class)));
            case "configuration.demand.execution-profile" -> {
                perfilExecucaoDemandPlanFacade.savePerfilExecucaoDemandPlanDTO(
                        convert(payload, PerfilExecucaoDemandPlanDTO.class));
                yield toJson("Demand Planning execution profile saved");
            }
            case "configuration.supply.execution-profile" -> {
                perfilExecucaoSupplyPlanFacade.savePerfilExecucaoSupplyPlanDTO(
                        convert(payload, PerfilExecucaoSupplyPlanDTO.class));
                yield toJson("Supply Planning execution profile saved");
            }
            case "configuration.demand.cluster-level" -> {
                demandSimulationFacade.saveParametrosDemandPlanning(
                        convert(payload, DemandPlanningClusterLevelConfigurationDTO.class));
                yield toJson("Demand Planning cluster-level configuration saved");
            }
            case "planning-book.demand" -> toJson(demandPlanningFacade.atualizaDemandPlan(
                    convertList(payload, SelectedPlanningBookCellDTO.class),
                    authenticationService.getAuthenticatedUserId()));
            case "planning-book.supply" -> toJson(supplyPlanFacade.modificaSupplyPlan(
                    convertList(payload, SelectedPlanningBookCellDTO.class),
                    authenticationService.getAuthenticatedUserId()));
            case "admin.user-view.demand", "admin.user-view.supply" -> {
                configuredViewFacade.saveConfiguredViewDTO(
                        convert(payload, ConfiguredViewDTO.class),
                        authenticationService.getAuthenticatedUserId(),
                        true);
                yield toJson("Community user view saved");
            }
            default -> throw new IllegalArgumentException("MCP capability does not support update: " + capabilityId);
        };

    }

    /**
     * Dispara somente os processos sincronizados permitidos no Community.
     */
    public JsonNode run(String capabilityId, JsonNode payload) {

        return switch (capabilityId) {
            case "process.demand.execute" -> {
                VersaoDemandPlanDTO versaoDemandPlanDTO = convert(payload, VersaoDemandPlanDTO.class);
                ResponseEntity<?> responseEntity = webControllerTaskSchedulingService.runImediato(
                        DemandPlanningTask.class,
                        versaoDemandPlanDTO,
                        "Demand Planning",
                        versaoDemandPlanDTO.getDescricao(),
                        webControllerTaskSchedulingService.getPlanningProcessExecutionMode());
                yield toJson(responseEntity.getBody());
            }
            case "process.supply.execute" -> {
                VersaoSupplyPlanDTO versaoSupplyPlanDTO = convert(payload, VersaoSupplyPlanDTO.class);
                ResponseEntity<?> responseEntity = webControllerTaskSchedulingService.runImediato(
                        SupplyPlanningTask.class,
                        versaoSupplyPlanDTO,
                        "Supply Planning",
                        versaoSupplyPlanDTO.getDescricaoSupplyPlan(),
                        webControllerTaskSchedulingService.getPlanningProcessExecutionMode());
                yield toJson(responseEntity.getBody());
            }
            default -> throw new IllegalArgumentException("MCP capability does not support run: " + capabilityId);
        };

    }

    private Map<String, IntegrationCapabilityAdapter> indexIntegrationAdapters(
            List<IntegrationServiceComConfiguracoesInterface<?, ?, ?, ?, ?, ?, ?>> integrationServices) {

        Map<String, IntegrationCapabilityAdapter> indexedAdapters = new LinkedHashMap<>();
        for (IntegrationServiceComConfiguracoesInterface<?, ?, ?, ?, ?, ?, ?> integrationService : integrationServices) {
            Class<?> serviceClass = AopUtils.getTargetClass(integrationService);
            String capabilityId = INTEGRATION_CAPABILITY_BY_SERVICE.get(serviceClass.getSimpleName());
            if (capabilityId == null) {
                continue;
            }

            ResolvableType integrationType = ResolvableType.forClass(serviceClass)
                    .as(IntegrationServiceComConfiguracoesInterface.class);
            Class<?> dataDtoClass = requireResolvedGeneric(integrationType, 0, serviceClass);
            Class<?> primaryKeyClass = requireResolvedGeneric(integrationType, 1, serviceClass);
            Class<?> filterClass = requireResolvedGeneric(integrationType, 5, serviceClass);
            Class<?> optionsClass = requireResolvedGeneric(integrationType, 6, serviceClass);

            IntegrationCapabilityAdapter previousAdapter = indexedAdapters.put(
                    capabilityId,
                    new IntegrationCapabilityAdapter(
                            integrationService,
                            dataDtoClass,
                            primaryKeyClass,
                            filterClass,
                            optionsClass));
            if (previousAdapter != null) {
                throw new IllegalStateException("Duplicated MCP integration capability: " + capabilityId);
            }
        }

        return Map.copyOf(indexedAdapters);

    }

    private Class<?> requireResolvedGeneric(
            ResolvableType integrationType,
            int genericIndex,
            Class<?> serviceClass) {

        Class<?> resolvedClass = integrationType.getGeneric(genericIndex).resolve();
        if (resolvedClass == null) {
            throw new IllegalStateException(
                    "Could not resolve integration generic " + genericIndex + " for " + serviceClass.getName());
        }

        return resolvedClass;

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JsonNode queryIntegration(
            IntegrationCapabilityAdapter integrationCapabilityAdapter,
            JsonNode payload) {

        IntegrationServiceComConfiguracoesInterface integrationService = integrationCapabilityAdapter.service();
        if (payload == null || payload.isNull() || payload.isEmpty()) {
            return toJson(integrationService.getFullDTOList());
        }

        JsonNode filterPayload = payload.has("dataFilter") ? payload.get("dataFilter") : payload;
        Object dataFilter = objectMapper.convertValue(filterPayload, integrationCapabilityAdapter.filterClass());
        return toJson(integrationService.getFilteredDTOList(dataFilter));

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JsonNode updateIntegration(
            IntegrationCapabilityAdapter integrationCapabilityAdapter,
            JsonNode payload) {

        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("Integration update payload is required.");
        }

        JsonNode envelopePayload = payload;
        if (payload.isArray()) {
            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.set("data", payload);
            envelopePayload = wrapper;
        }

        JavaType envelopeType = objectMapper.getTypeFactory().constructParametricType(
                IntegrationDto.class,
                integrationCapabilityAdapter.dataDtoClass(),
                integrationCapabilityAdapter.primaryKeyClass(),
                integrationCapabilityAdapter.filterClass(),
                integrationCapabilityAdapter.optionsClass());
        IntegrationDto integrationDto = objectMapper.convertValue(envelopePayload, envelopeType);
        IntegrationServiceComConfiguracoesInterface integrationService = integrationCapabilityAdapter.service();
        return toJson(integrationService.saveDTOList(integrationDto));

    }

    private Long requiredLong(JsonNode payload, String fieldName) {

        JsonNode value = requiredField(payload, fieldName);
        if (!value.canConvertToLong()) {
            throw new IllegalArgumentException("MCP payload field must be an integer: " + fieldName);
        }

        return value.longValue();

    }

    private String requiredText(JsonNode payload, String fieldName) {

        JsonNode value = requiredField(payload, fieldName);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("MCP payload field must be non-empty text: " + fieldName);
        }

        return value.textValue();

    }

    private JsonNode requiredField(JsonNode payload, String fieldName) {

        if (payload == null || payload.isNull() || !payload.hasNonNull(fieldName)) {
            throw new IllegalArgumentException("Required MCP payload field is missing: " + fieldName);
        }

        return payload.get(fieldName);

    }

    private <T> T convert(JsonNode payload, Class<T> targetClass) {

        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("MCP payload is required for " + targetClass.getSimpleName());
        }

        return objectMapper.convertValue(payload, targetClass);

    }

    private <T> List<T> convertList(JsonNode payload, Class<T> elementClass) {

        if (payload == null || !payload.isArray()) {
            throw new IllegalArgumentException("MCP payload must be a JSON array of " + elementClass.getSimpleName());
        }

        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass);
        return objectMapper.convertValue(payload, listType);

    }

    private JsonNode toJson(Object value) {

        return objectMapper.valueToTree(value);

    }

    /**
     * Tipos concretos necessarios para desserializar o envelope generico de
     * uma integracao sem duplicar DTOs na camada MCP.
     */
    private record IntegrationCapabilityAdapter(
            IntegrationServiceComConfiguracoesInterface<?, ?, ?, ?, ?, ?, ?> service,
            Class<?> dataDtoClass,
            Class<?> primaryKeyClass,
            Class<?> filterClass,
            Class<?> optionsClass) {
    }

}
