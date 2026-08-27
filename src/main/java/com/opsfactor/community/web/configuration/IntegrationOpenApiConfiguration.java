package com.opsfactor.community.web.configuration;

import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanningExecutionModelCatalog;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.annotation.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Complementa a documentacao OpenAPI para os endpoints de integracao registrados
 * dinamicamente via {@link RequestMappingHandlerMapping#registerMapping(RequestMappingInfo, Object, Method)}.
 *
 * O springdoc documenta muito bem endpoints anotados diretamente com {@code @GetMapping}/{@code @PostMapping},
 * mas os endpoints herdados de {@link IntegrationControllerAbstract} nascem em {@code @PostConstruct} e,
 * por isso, podem nao aparecer automaticamente no Swagger UI. Esta configuracao varre os mappings reais
 * publicados pelo Spring, resolve os tipos genericos da classe concreta e acrescenta as operacoes faltantes
 * no documento OpenAPI sem alterar o comportamento runtime dos controllers.
 *
 * No Community isso documenta somente as subclasses concretas existentes de
 * {@link IntegrationControllerAbstract}. Nao ha endpoint generico de upload de
 * planning data: se uma entidade nao tiver controller concreto no modulo
 * Community, ela nao aparece no OpenAPI nem fica acessivel por estes prefixos.
 */
@Configuration
public class IntegrationOpenApiConfiguration {

    private static final List<String> COMMUNITY_OPENAPI_HIDDEN_SUPPLY_PROFILE_PROPERTIES = List.of(
            "materialFilterId",
            "aiOptimizer",
            "optimizationModelType",
            "customerOrdersAndForecastReconciliationModelForProjectedInventory",
            "customerOrdersAndForecastReconciliationModelForSafetyStock",
            "customerOrderHorizonInDays",
            "demandPlanMetDemandImpactCoefficient",
            "customerOrderMetDemandImpactCoefficient",
            "increaseObjectiveFunctionImpactInEarlierPeriods",
            "maximumPercentageIncreaseObjectiveFunctionImpactAtFirstPeriod",
            "objectiveFunctionTemporalImpactDecayModel",
            "objectiveFunctionTemporalImpactExponentialDecayFactor",
            "objectiveFunctionTemporalImpactMinimumMultiplier",
            "customerDemandPrioritizationModelId",
            "safetyStockPrioritizationModelId",
            "considerForecastForMto",
            "roundPlannedPurchaseOrdersByMinimumLotSize",
            "allocateTransfersInFleets",
            "considerSelloutOrdersBacklog",
            "considerSelloutOrdersFuture",
            "considerSellinOrdersBacklog",
            "considerSellinOrdersFuture",
            "considerTransferOrdersBacklog",
            "considerTransferOrdersFuture",
            "considerPurchaseOrdersBacklog",
            "considerPurchaseOrdersFuture",
            "considerProductionOrdersBacklog",
            "considerProductionOrdersFuture",
            "allowBacklogCarryOver",
            "forceMakeToOrderModel",
            "enableDemandCatchUpFromPastSellout",
            "saveOptimizerVariablesAndConstraints",
            "saveConstraintBacktracking",
            "executeSupplyPlanForAllLocations",
            "ignoreStorageConstraintsForUnconstrainedPlan",
            "ignoreOutboundConstraintsForUnconstrainedPlan",
            "ignoreInboundConstraintsForUnconstrainedPlan",
            "ignoreLeadTimeConstraintsForUnconstrainedPlan",
            "maximumTransferCostImpactForLeadTimeReduction",
            "maximumMaterialObjectiveValueImpactForLeadTimeReduction",
            "ignoreMarginConstraintsForUnconstrainedPlan",
            "metDemandObjectiveValueIncreasePercentage",
            "minimumMetDemandObjectiveValue",
            "generatePL",
            "associateSalesToInputMaterialsInRetroaction",
            "salesMeasure",
            "taxApportionmentModel",
            "optimizationUom",
            "unitValueByOptimizationUom",
            "allowStockAtClients",
            "allowStockAtTransshipmentPoints",
            "considerBudgetForGreenfieldLocationActivation",
            "greenfieldLocationActivationBudget",
            "considerLocationFixedCost",
            "considerProductionResourceFixedCost",
            "considerStorageCost",
            "considerInboundOutboundCosts",
            "considerTransferCost",
            "considerTaxesInTransportationLines",
            "considerProductionCost",
            "considerSupplierPrices",
            "estimateUnitCogsForWorkingCapitalAndInventoryPolicy",
            "considerUnmetClientOrderImpact",
            "considerStorageConstraints",
            "considerInboundConstraints",
            "considerOutboundConstraints",
            "logisticsCapacityLevel",
            "generateProductionScheduling",
            "safetyStockFairShare",
            "numberSegmentsDirectDemandGapLinearization",
            "numberSegmentsSafetyStockGapLinearization",
            "fairShareMaximumPercentagePenaltyUnmetDemand",
            "fairShareMaximumPercentagePenaltySafetyStockGap",
            "safetyStockGapPercentualCost",
            "workingCapitalPercentualCost",
            "maximumOptimizerExecutionTime",
            "entityTabuRatio",
            "acceptedCountLimit",
            "softTargetMaximumPercentPenalty",
            "softTargetDeviationAmplitudeAsTargetPercent",
            "softTargetDeviationLinearizationNumberSegments",
            "firmOrderCogsIncentivePercentage",
            "generateDetailedPlan",
            "detailedPlanBucketSize",
            "detailedPlanPlanningHorizonInBuckets",
            "roundProductionAndSetupsToDetailedPlanBucket",
            "segmentInventoryByBatch",
            "increaseWorkingCapitalImpactForOlderBatches",
            "maximumPercentageIncreaseWorkingCapitalImpactForOldestBatch",
            "temporalSplitCurveIdSet",
            "penalizeUnmetDemand",
            "unmetDemandPenalizationAsFractionOfGrossSales",
            "unmetDemandPenalizationAsUnitImpact",
            "unmetDemandPenalizationAsUnitImpactUomId",
            "logisticsCostCurvesId",
            "applyFreightCostCurves",
            "applyLocationCostCurves"
    );

    private static final List<String> COMMUNITY_OPENAPI_HIDDEN_DEMAND_EXECUTION_PROFILE_PROPERTIES = List.of(
            "mapeMaterialAggregationLevelId",
            "mapeLocationAggregationLevelId",
            "defaultAutoTunedDemandPlanConfigurationId",
            "autofitModelType",
            "modelAutofitObjectiveFunction",
            "modelAutofitNumberOfPeriodsForAccuracyEvaluation",
            "modelAutofitEvaluationLagInPeriods",
            "regressionTreeObjectiveFunction",
            "numberOfDimensionsUsedForCandidateSplits",
            "numberOfCandidateSplitsByDimension",
            "maxDepthAfterLastConfirmedSplit",
            "minimumPercentErrorReductionForNewSplits",
            "numberOfPeriodsForRegressionTreePruning"
    );

    private static final List<String> COMMUNITY_OPENAPI_HIDDEN_DEMAND_GENERAL_PARAMETERS_PROPERTIES = List.of(
            "useExecutionProfileAutofitModel",
            "budgetId",
            "daysAsNewMaterial",
            "regressionTimeSeries",
            "considerTargetTrendGrowthYoy",
            "numberOfDaysCurrentLevelAsAverageOfHistoricalStl",
            "targetGrowthYoy",
            "includeWorkingDaysRegressor"
    );

    private static final List<String> COMMUNITY_OPENAPI_HIDDEN_DEMAND_FORECAST_PARAMETERS_PROPERTIES = List.of(
            "considerStockoutData",
            "daysSmoothingModel",
            "enableUpperPercentileSmoothing",
            "smoothingUpperPercentile",
            "enableLowerPercentileSmoothing",
            "smoothingLowerPercentile",
            "smoothingModel",
            "upliftModel",
            "splitModel",
            "prophetAutoSeasonalityPriorScale",
            "prophetSeasonalityPriorScale",
            "prophetAutoChangepointPriorScale",
            "prophetChangepointPriorScale",
            "prophetAutoYearlyFourierOrder",
            "prophetYearlyFourierOrder",
            "chronosForceAggregatedForecast"
    );

    private static final Map<String, List<String>> COMMUNITY_OPENAPI_HIDDEN_SCHEMA_PROPERTIES = Map.ofEntries(
            Map.entry(
                    "PerfilExecucaoSupplyPlanDTO",
                    COMMUNITY_OPENAPI_HIDDEN_SUPPLY_PROFILE_PROPERTIES),
            Map.entry(
                    "LocationDTO",
                    List.of(
                            "latitude",
                            "longitude",
                            "characteristicValues",
                            "showInDeployment",
                            "applyLogisticsConstraints")),
            Map.entry(
                    "PerfilExecucaoDemandPlanDTO",
                    COMMUNITY_OPENAPI_HIDDEN_DEMAND_EXECUTION_PROFILE_PROPERTIES),
            Map.entry(
                    "DemandPlanningGeneralParametersDTO",
                    COMMUNITY_OPENAPI_HIDDEN_DEMAND_GENERAL_PARAMETERS_PROPERTIES),
            Map.entry(
                    "DemandPlanningForecastParametersDTO",
                    COMMUNITY_OPENAPI_HIDDEN_DEMAND_FORECAST_PARAMETERS_PROPERTIES),
            // springdoc pode nomear classes internas pelo nome simples ou pelo nome qualificado do owner.
            Map.entry(
                    "PoliticaEstoquesMaterialLocationDTO",
                    List.of("frequenciaReabastecimentoDias")),
            Map.entry(
                    "PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO",
                    List.of("frequenciaReabastecimentoDias")),
            Map.entry(
                    "IntegrationDto",
                    List.of("threadSync")),
            Map.entry(
                    "ProdutoIntegrationDataDto",
                    List.of(
                            "valueByCharacteristic",
                            "unitCogs",
                            "unitCogsUnitOfMeasureId")),
            Map.entry(
                    "LocationIntegrationDataDto",
                    List.of(
                            "latitude",
                            "longitude",
                            "expeditionUomId",
                            "economicGroupId",
                            "orderFulfillmentTimeDays",
                            "valueByCharacteristic")),
            Map.entry(
                    "ParametrosMaterialLocationIntegrationDataDto",
                    List.of("reorderFrequencyDays", "valueByCharacteristic")),
            Map.entry(
                    "PoliticaEstoquesMaterialLocationIntegrationDataDto",
                    List.of("reorderFrequencyDays")),
            Map.entry(
                    "LinhaTransporteIntegrationDataDto",
                    List.of("distanceKm")),
            Map.entry(
                    "LinhaTransporteProdutoIntegrationDataDto",
                    List.of("distanceKm")),
            Map.entry(
                    "RecursoProdutivoIntegrationDataDto",
                    List.of("capacityInQuantityUomId")),
            Map.entry(
                    "DisponibilidadeRecursoProdutivoIntegrationDataDto",
                    List.of("capacityInQuantity", "capacityInQuantityUomId")),
            Map.entry(
                    "RoteiroIntegrationDataDto",
                    List.of("routingClusterId")),
            Map.entry(
                    "ConfiguredViewDTO",
                    List.of(
                            "directDemandUpdateKeyFigure",
                            "materialLocationCharacteristicDetailList",
                            "showMaterialLevel",
                            "showLocationLevel",
                            "keyFigureList",
                            "demandPlanWorkflowId",
                            "demandPlanWorkflowStageId")),
            Map.entry(
                    "ConfiguredViewCaracteristicaDTO",
                    List.of("characteristicDescription", "aggregationType", "columnPosition")),
            Map.entry(
                    "ConfiguredViewKeyFigureDTO",
                    List.of("keyFigure", "allowChanges", "position", "userId", "viewName", "viewType")),
            Map.entry(
                    "FiltroMaterialLocationDeCombinacaoCaracteristicasDTO",
                    List.of("valuesByMaterialCharacteristicId", "valuesByLocationCharacteristicId")),
            Map.entry(
                    "RegraAlocaoClusterProdutosDTO",
                    List.of("caracteristicaDTO", "atributosCaracteristica")),
            Map.entry(
                    "RegraAlocaoClusterLocationsCaracteristicaDTO",
                    List.of("caracteristicaDTO", "atributosCaracteristica")),
            Map.entry(
                    "SimulatedDemandPlanDTO",
                    List.of("aggregatedDataAtMapeLevel")),
            Map.entry(
                    "ConfiguredViewSelectionDTO",
                    List.of(
                            "referencePlanId",
                            "materialAggregationLevelId",
                            "locationAggregationLevelId")),
            Map.entry(
                    "SelectedPlanningBookCellDTO",
                    List.of("referencePlanId"))
    );

    private static final List<String> COMMUNITY_OPENAPI_HIDDEN_SCHEMA_PROPERTIES_BY_PROPERTY_NAME = List.of(
            "modeloDemandaBase",
            "diasHistoricosDoh",
            "diasHistoricosDohStockout",
            "modeloNormalizacao",
            "diasHistoricosNormalizacao",
            "percentilOutliersVenda",
            "permiteAjusteAgregadoSemBaselineProduto",
            "permiteAjusteAgregadoSemBaselineLocation",
            "remessasConsomemDisponibilidadeNoPrimeiroPeriodo",
            "unidadeMedidaPadraoCapacidadeLogisticaPeso",
            "unidadeMedidaPadraoCapacidadeLogisticaVolume",
            "diasHistoricosCurva",
            "numeroDiasMaterialNovo",
            "quantidadesEmPedidosRepresentamSaldoRestante"
    );

    /**
     * Monta operacoes OpenAPI apenas para os endpoints herdados da abstracao de integracao.
     * Endpoints concretos anotados diretamente continuam sendo responsabilidade do springdoc padrao.
     */
    @Bean
    public OpenApiCustomizer integrationControllersOpenApiCustomizer(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return openApi -> {
            initializeOpenApiContainers(openApi);

            requestMappingHandlerMapping.getHandlerMethods()
                    .entrySet()
                    .stream()
                    .filter(this::isDynamicIntegrationEndpoint)
                    .sorted(Comparator.comparing(entry -> entry.getKey().getPatternValues().stream().sorted().collect(Collectors.joining(","))))
                    .forEach(entry -> addDynamicEndpoint(openApi, entry.getKey(), entry.getValue()));

            removeCommunityHiddenSchemaProperties(openApi);
        };
    }

    /**
     * Remove dos schemas publicos Community propriedades que existem nos DTOs
     * apenas para rejeicao defensiva de payloads Enterprise/legados ou para
     * manter compatibilidade com o front compartilhado.
     *
     * <p>A remocao e restrita ao OpenAPI: Jackson continua aceitando estes
     * campos no request body para que services/mappers lancem
     * {@code RequiresEnterpriseVersionException} ou normalizem a view com
     * mensagem funcional especifica. Isso evita que o Swagger sugira
     * colunas/opcoes que nao sao usaveis no Community sem enfraquecer a
     * protecao de runtime.</p>
     */
    void removeCommunityHiddenSchemaProperties(OpenAPI openApi) {

        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }

        for (Map.Entry<String, List<String>> hiddenPropertiesEntry : COMMUNITY_OPENAPI_HIDDEN_SCHEMA_PROPERTIES.entrySet()) {
            Schema<?> schema = openApi.getComponents().getSchemas().get(hiddenPropertiesEntry.getKey());
            if (schema == null || schema.getProperties() == null) {
                continue;
            }

            for (String hiddenProperty : hiddenPropertiesEntry.getValue()) {
                schema.getProperties().remove(hiddenProperty);
            }

            removeRequiredProperties(schema, hiddenPropertiesEntry.getValue());
        }

        removeCommunityHiddenSchemaPropertiesByPropertyName(openApi);
        restrictCommunitySchemaEnums(openApi);

    }

    /**
     * Remove propriedades Enterprise quando o nome do schema pode variar entre
     * nome simples e nome qualificado da classe interna gerada pelo springdoc.
     *
     * <p>Esta lista deve ficar restrita a propriedades com nome suficientemente
     * especifico para nao esconder, por acidente, um campo Community homonimo
     * em outro contrato. Hoje ela cobre campos transicionais de Global
     * Parameters que o controller ainda aceita para rejeitar com erro
     * funcional, mas que nao devem ser divulgados como contrato Community.</p>
     */
    private void removeCommunityHiddenSchemaPropertiesByPropertyName(OpenAPI openApi) {

        for (Schema<?> schema : openApi.getComponents().getSchemas().values()) {
            if (schema.getProperties() == null) {
                continue;
            }

            for (String hiddenProperty : COMMUNITY_OPENAPI_HIDDEN_SCHEMA_PROPERTIES_BY_PROPERTY_NAME) {
                schema.getProperties().remove(hiddenProperty);
            }

            removeRequiredProperties(schema, COMMUNITY_OPENAPI_HIDDEN_SCHEMA_PROPERTIES_BY_PROPERTY_NAME);
        }

    }

    /**
     * Ajusta enums de campos que continuam visiveis no OpenAPI Community, mas
     * cujo enum Java compartilhado tambem contem opcoes Enterprise.
     *
     * <p>O runtime ainda valida o payload com
     * {@code RequiresEnterpriseVersionException}; esta restricao e apenas para
     * a documentacao publica nao sugerir valores que a edicao Community rejeita.</p>
     */
    private void restrictCommunitySchemaEnums(OpenAPI openApi) {

        restrictSchemaPropertyEnum(
                openApi,
                "DemandPlanningForecastParametersDTO",
                "statisticalModel",
                DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiCommunity());

        restrictSchemaPropertyEnum(
                openApi,
                "PerfilExecucaoDemandPlanDTO",
                "historicalSalesDocumentType",
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiCommunity());

        restrictSchemaPropertyEnum(
                openApi,
                "PerfilExecucaoSupplyPlanDTO",
                "executionModel",
                SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiCommunity());
        describeSchemaPropertyByPropertyName(
                openApi,
                "executionModel",
                "Community accepts only Heuristic. Optimizer and Process Chain are Enterprise execution engines.");

        restrictSchemaPropertyEnum(
                openApi,
                "PerfilExecucaoSupplyPlanDTO",
                "productiveCapacityType",
                List.of(MetodosUtilidade.getValorJsonPropertyDeEnum(
                        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA)));
        describeSchemaPropertyByPropertyName(
                openApi,
                "productiveCapacityType",
                "Community accepts only Total Hours / Day. Quantity-based capacity and shift allocation are Enterprise features.");

        restrictSchemaPropertyEnumByPropertyName(
                openApi,
                "tipoDocumentoVenda",
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoCanonicalOpenApiCommunity());
        describeSchemaPropertyByPropertyName(
                openApi,
                "tipoDocumentoVenda",
                "Community accepts SELLOUT as the canonical payload value; Sell-out is also accepted as JSON label.");

    }

    private void restrictSchemaPropertyEnum(
            OpenAPI openApi,
            String schemaName,
            String propertyName,
            List<String> allowedValues) {

        Schema<?> schema = openApi.getComponents().getSchemas().get(schemaName);
        if (schema == null || schema.getProperties() == null) {
            return;
        }

        Schema<?> propertySchema = (Schema<?>) schema.getProperties().get(propertyName);
        if (propertySchema == null) {
            return;
        }

        setSchemaEnumValues(propertySchema, allowedValues);

    }

    private void restrictSchemaPropertyEnumByPropertyName(
            OpenAPI openApi,
            String propertyName,
            List<String> allowedValues) {

        openApi.getComponents()
                .getSchemas()
                .values()
                .stream()
                .filter(schema -> schema.getProperties() != null)
                .map(schema -> (Schema<?>) schema.getProperties().get(propertyName))
                .filter(Objects::nonNull)
                .forEach(propertySchema -> setSchemaEnumValues(propertySchema, allowedValues));

    }

    private void describeSchemaPropertyByPropertyName(
            OpenAPI openApi,
            String propertyName,
            String description) {

        openApi.getComponents()
                .getSchemas()
                .values()
                .stream()
                .filter(schema -> schema.getProperties() != null)
                .map(schema -> (Schema<?>) schema.getProperties().get(propertyName))
                .filter(Objects::nonNull)
                .forEach(propertySchema -> propertySchema.setDescription(description));

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setSchemaEnumValues(Schema<?> propertySchema, List<String> allowedValues) {

        /*
         * A API do Swagger modela Schema com wildcard na leitura do mapa de
         * properties. O enum documentado continua sendo lista de strings; o
         * cast bruto fica isolado aqui para nao espalhar warning pelo fluxo.
         */
        ((Schema) propertySchema).setEnum(new ArrayList<>(allowedValues));

    }

    /**
     * Mantem o schema consistente caso alguma propriedade escondida apareca em
     * `required` por anotacao futura. Hoje esses campos sao opcionais, mas a
     * limpeza defensiva evita um OpenAPI invalido se o DTO mudar depois.
     */
    private void removeRequiredProperties(Schema<?> schema, List<String> hiddenProperties) {

        if (schema.getRequired() == null || schema.getRequired().isEmpty()) {
            return;
        }

        schema.setRequired(
                schema.getRequired()
                        .stream()
                        .filter(requiredProperty -> !hiddenProperties.contains(requiredProperty))
                        .collect(Collectors.toList()));

    }

    /**
     * Restringe a customizacao aos cinco endpoints herdados da abstracao.
     * Isso evita sobrescrever documentacao de endpoints concretos ja anotados manualmente.
     */
    private boolean isDynamicIntegrationEndpoint(Map.Entry<RequestMappingInfo, HandlerMethod> handlerMethodEntry) {
        HandlerMethod handlerMethod = handlerMethodEntry.getValue();
        return IntegrationControllerAbstract.class.isAssignableFrom(handlerMethod.getBeanType())
                && handlerMethod.getMethod().getDeclaringClass().equals(IntegrationControllerAbstract.class);
    }

    /**
     * Acrescenta a operacao na rota real registrada pelo Spring apenas quando ela ainda nao existe no OpenAPI.
     * Assim mantemos a prioridade da documentacao gerada automaticamente para endpoints que o springdoc ja cobre.
     */
    private void addDynamicEndpoint(OpenAPI openApi, RequestMappingInfo requestMappingInfo, HandlerMethod handlerMethod) {
        for (String rawPathPattern : requestMappingInfo.getPatternValues()) {
            String normalizedPath = normalizeOpenApiPath(rawPathPattern);
            PathItem pathItem = openApi.getPaths().computeIfAbsent(normalizedPath, ignoredPath -> new PathItem());

            Set<RequestMethod> requestMethods = requestMappingInfo.getMethodsCondition().getMethods();
            for (RequestMethod requestMethod : requestMethods) {
                if (getExistingOperation(pathItem, requestMethod) != null) {
                    continue;
                }

                Operation operation = buildOperation(openApi, handlerMethod, requestMethod, normalizedPath);
                attachOperation(pathItem, requestMethod, operation);
            }
        }
    }

    /**
     * Cria uma operacao descritiva e suficiente para uso no Swagger UI.
     * O corpo e a resposta sao inferidos do metodo herdado, mas os generics sao resolvidos
     * no contexto da classe concreta do controller para que o schema final reflita o DTO real.
     */
    private Operation buildOperation(OpenAPI openApi, HandlerMethod handlerMethod, RequestMethod requestMethod, String normalizedPath) {
        Method controllerMethod = handlerMethod.getMethod();
        Class<?> controllerBeanType = handlerMethod.getBeanType();

        Operation operation = new Operation()
                .operationId(buildOperationId(controllerBeanType, controllerMethod, requestMethod, normalizedPath))
                .summary(buildOperationSummary(controllerBeanType, requestMethod, normalizedPath))
                .description(buildOperationDescription(controllerBeanType, controllerMethod))
                .responses(buildResponses(openApi, controllerMethod, controllerBeanType, requestMethod));

        List<String> explicitTags = resolveExplicitTags(controllerBeanType, controllerMethod);
        if (!explicitTags.isEmpty()) {
            operation.setTags(explicitTags);
        }

        RequestBody requestBody = buildRequestBody(openApi, controllerMethod, controllerBeanType);
        if (requestBody != null) {
            operation.setRequestBody(requestBody);
        }

        List<Parameter> parameters = buildQueryParameters(openApi, controllerMethod, controllerBeanType);
        if (!parameters.isEmpty()) {
            operation.setParameters(parameters);
        }

        return operation;
    }

    /**
     * A abstracao usa apenas query params simples e upload multipart.
     * MultipartFile e tratado como request body; os demais @RequestParam continuam expostos como query string.
     */
    private List<Parameter> buildQueryParameters(OpenAPI openApi, Method controllerMethod, Class<?> controllerBeanType) {
        List<Parameter> parameters = new ArrayList<>();

        for (int parameterIndex = 0; parameterIndex < controllerMethod.getParameterCount(); parameterIndex++) {
            MethodParameter methodParameter = new MethodParameter(controllerMethod, parameterIndex)
                    .withContainingClass(controllerBeanType);

            if (MultipartFile.class.isAssignableFrom(methodParameter.getParameterType())) {
                continue;
            }
            if (methodParameter.getParameterAnnotation(org.springframework.web.bind.annotation.RequestBody.class) != null) {
                continue;
            }

            RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
            if (requestParam == null) {
                continue;
            }

            Schema<?> parameterSchema = resolveSchema(openApi, ResolvableType.forMethodParameter(methodParameter).getType());
            String parameterName = StringUtils.hasText(requestParam.name()) ? requestParam.name()
                    : StringUtils.hasText(requestParam.value()) ? requestParam.value()
                    : methodParameter.getParameterName();

            parameters.add(new Parameter()
                    .in("query")
                    .name(parameterName)
                    .required(requestParam.required())
                    .schema(parameterSchema));
        }

        return parameters;
    }

    /**
     * Constrói o corpo esperado tanto para upload de arquivo quanto para payload JSON.
     * Retorna {@code null} quando o endpoint concreto nao possui request body.
     */
    @Nullable
    private RequestBody buildRequestBody(OpenAPI openApi, Method controllerMethod, Class<?> controllerBeanType) {
        for (int parameterIndex = 0; parameterIndex < controllerMethod.getParameterCount(); parameterIndex++) {
            MethodParameter methodParameter = new MethodParameter(controllerMethod, parameterIndex)
                    .withContainingClass(controllerBeanType);
            Class<?> parameterType = methodParameter.getParameterType();

            if (MultipartFile.class.isAssignableFrom(parameterType)) {
                Schema<?> multipartSchema = new ObjectSchema()
                        .addProperty("file", new BinarySchema())
                        .addRequiredItem("file");

                return new RequestBody()
                        .required(true)
                        .content(new Content().addMediaType("multipart/form-data", new MediaType().schema(multipartSchema)));
            }

            if (methodParameter.getParameterAnnotation(org.springframework.web.bind.annotation.RequestBody.class) != null) {
                Type explicitIntegrationRequestBodyType = resolveExplicitIntegrationRequestBodyType(controllerMethod, controllerBeanType);
                Type requestBodyJavaType = explicitIntegrationRequestBodyType != null
                        ? explicitIntegrationRequestBodyType
                        : ResolvableType.forMethodParameter(methodParameter).getType();
                Schema<?> requestBodySchema = resolveSchema(openApi, requestBodyJavaType);
                return new RequestBody()
                        .required(true)
                        .content(new Content().addMediaType("application/json", new MediaType().schema(requestBodySchema)));
            }
        }

        return null;
    }

    /**
     * Produz respostas coerentes com o verbo HTTP e com o tipo retornado pelo metodo herdado.
     */
    private ApiResponses buildResponses(OpenAPI openApi, Method controllerMethod, Class<?> controllerBeanType, RequestMethod requestMethod) {
        ApiResponses apiResponses = new ApiResponses();
        ApiResponse successResponse = new ApiResponse().description(resolveSuccessDescription(requestMethod));

        Schema<?> responseSchema = resolveResponseSchema(openApi, controllerMethod, controllerBeanType);
        if (responseSchema != null) {
            successResponse.setContent(new Content().addMediaType("application/json", new MediaType().schema(responseSchema)));
        }

        apiResponses.addApiResponse("200", successResponse);
        apiResponses.addApiResponse("401", new ApiResponse().description("Unauthorized"));
        apiResponses.addApiResponse("500", new ApiResponse().description("Internal Server Error"));
        return apiResponses;
    }

    /**
     * Resolve o tipo de retorno efetivo da classe concreta.
     * ResponseEntity e desembrulhado para que o schema documente o payload interno.
     * Retorna {@code null} para metodos void ou sem payload documentavel.
     */
    @Nullable
    private Schema<?> resolveResponseSchema(OpenAPI openApi, Method controllerMethod, Class<?> controllerBeanType) {
        Type explicitIntegrationResponseType = resolveExplicitIntegrationResponseType(controllerMethod, controllerBeanType);
        if (explicitIntegrationResponseType != null) {
            return resolveSchema(openApi, explicitIntegrationResponseType);
        }

        MethodParameter returnMethodParameter = new MethodParameter(controllerMethod, -1)
                .withContainingClass(controllerBeanType);
        ResolvableType resolvableReturnType = ResolvableType.forMethodParameter(returnMethodParameter);

        if (ResponseEntity.class.isAssignableFrom(resolvableReturnType.toClass())) {
            resolvableReturnType = resolvableReturnType.getGeneric(0);
        }

        Class<?> returnClass = resolvableReturnType.toClass();
        if (returnClass == null || Void.TYPE.equals(returnClass) || Void.class.equals(returnClass)) {
            return null;
        }

        return resolveSchema(openApi, resolvableReturnType.getType());
    }

    /**
     * Registra schemas referenciados no componente global do OpenAPI e devolve o schema principal.
     * O tipo Java pode ser nulo quando o caller esta tratando um caminho sem
     * payload; nesse caso tambem nao ha schema a registrar.
     */
    @Nullable
    private Schema<?> resolveSchema(OpenAPI openApi, @Nullable Type javaType) {
        if (javaType == null) {
            return null;
        }

        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(javaType).resolveAsRef(true));

        if (resolvedSchema.referencedSchemas != null) {
            resolvedSchema.referencedSchemas.forEach((schemaName, schema) -> openApi.getComponents().addSchemas(schemaName, schema));
        }

        if (resolvedSchema.schema != null) {
            return resolvedSchema.schema;
        }

        return new ComposedSchema();
    }

    /**
     * Usa explicitamente os generics do controller concreto para documentar o retorno
     * dos endpoints herdados quando a resolução do método abstrato não especializa o tipo.
     * Null significa que o metodo herdado nao precisa de especializacao manual.
     */
    @Nullable
    private Type resolveExplicitIntegrationResponseType(Method controllerMethod, Class<?> controllerBeanType) {
        ResolvableType integrationControllerType = resolveIntegrationControllerType(controllerBeanType);
        ResolvableType dataRecordType = integrationControllerType.getGeneric(1);

        return switch (controllerMethod.getName()) {
            case "getDataRecordDtoList" -> ResolvableType.forClassWithGenerics(List.class, dataRecordType).getType();
            default -> null;
        };
    }

    /**
     * Usa explicitamente os generics do controller concreto para documentar o payload JSON
     * de save/delete, evitando cair no schema do tipo-base `IntegrationDataDtoAbstract`.
     * Null significa que o parametro pode ser resolvido pela inspecao padrao.
     */
    @Nullable
    private Type resolveExplicitIntegrationRequestBodyType(Method controllerMethod, Class<?> controllerBeanType) {
        ResolvableType integrationControllerType = resolveIntegrationControllerType(controllerBeanType);
        ResolvableType dataRecordType = integrationControllerType.getGeneric(1);
        ResolvableType primaryKeyType = integrationControllerType.getGeneric(2);
        ResolvableType dataFilterType = integrationControllerType.getGeneric(3);
        ResolvableType optionsType = integrationControllerType.getGeneric(5);

        return switch (controllerMethod.getName()) {
            case "saveIntegrationDto", "deleteDtoOuFiltro" -> ResolvableType
                    .forClassWithGenerics(
                            com.opsfactor.community.platform.integration.dto.IntegrationDto.class,
                            dataRecordType,
                            primaryKeyType,
                            dataFilterType,
                            optionsType)
                    .getType();
            default -> null;
        };
    }

    private ResolvableType resolveIntegrationControllerType(Class<?> controllerBeanType) {
        return ResolvableType.forClass(controllerBeanType).as(IntegrationControllerAbstract.class);
    }

    private void initializeOpenApiContainers(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        if (openApi.getPaths() == null) {
            openApi.setPaths(new Paths());
        }
    }

    private String normalizeOpenApiPath(String rawPathPattern) {
        return rawPathPattern.startsWith("/") ? rawPathPattern : "/" + rawPathPattern;
    }

    /**
     * Busca a operacao ja registrada no documento OpenAPI. Null indica que o
     * springdoc ainda nao publicou este verbo/caminho e que a configuracao
     * dinamica deve anexar uma operacao nova.
     */
    @Nullable
    private Operation getExistingOperation(PathItem pathItem, RequestMethod requestMethod) {
        return switch (requestMethod) {
            case GET -> pathItem.getGet();
            case POST -> pathItem.getPost();
            case PUT -> pathItem.getPut();
            case DELETE -> pathItem.getDelete();
            case PATCH -> pathItem.getPatch();
            case HEAD -> pathItem.getHead();
            case OPTIONS -> pathItem.getOptions();
            case TRACE -> pathItem.getTrace();
        };
    }

    private void attachOperation(PathItem pathItem, RequestMethod requestMethod, Operation operation) {
        switch (requestMethod) {
            case GET -> pathItem.setGet(operation);
            case POST -> pathItem.setPost(operation);
            case PUT -> pathItem.setPut(operation);
            case DELETE -> pathItem.setDelete(operation);
            case PATCH -> pathItem.setPatch(operation);
            case HEAD -> pathItem.setHead(operation);
            case OPTIONS -> pathItem.setOptions(operation);
            case TRACE -> pathItem.setTrace(operation);
        }
    }

    private String buildOperationId(Class<?> controllerBeanType, Method controllerMethod, RequestMethod requestMethod, String normalizedPath) {
        String sanitizedPath = normalizedPath.replace("/", "_").replace("{", "").replace("}", "").replace("-", "_");
        return requestMethod.name().toLowerCase() + "_" + controllerBeanType.getSimpleName() + "_" + controllerMethod.getName() + sanitizedPath;
    }

    private String buildOperationSummary(Class<?> controllerBeanType, RequestMethod requestMethod, String normalizedPath) {
        return requestMethod.name() + " " + normalizedPath + " (" + controllerBeanType.getSimpleName() + ")";
    }

    private String buildOperationDescription(Class<?> controllerBeanType, Method controllerMethod) {
        return "Endpoint de integração registrado dinamicamente por " + controllerBeanType.getSimpleName()
                + " via IntegrationControllerAbstract." + System.lineSeparator()
                + "Método base: " + controllerMethod.getName() + ".";
    }

    /**
     * Respeita tags declaradas explicitamente no metodo concreto ou na classe concreta.
     * Se nada estiver anotado, o endpoint fica sem agrupamento artificial por controller.
     */
    private List<String> resolveExplicitTags(Class<?> controllerBeanType, Method controllerMethod) {
        List<String> explicitTags = new ArrayList<>();

        collectTagsFromAnnotatedElement(explicitTags, controllerBeanType);

        Method concreteMethod = findConcreteMethod(controllerBeanType, controllerMethod);
        if (concreteMethod != null) {
            collectTagsFromAnnotatedElement(explicitTags, concreteMethod);
        }

        return explicitTags.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Procura override/anotacao no controller concreto. Endpoints herdados
     * podem nao declarar metodo proprio na subclasse; nesse caso o retorno
     * nulo preserva a ausencia de tag explicita.
     */
    @Nullable
    private Method findConcreteMethod(Class<?> controllerBeanType, Method controllerMethod) {
        try {
            return controllerBeanType.getMethod(controllerMethod.getName(), controllerMethod.getParameterTypes());
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private void collectTagsFromAnnotatedElement(List<String> explicitTags, java.lang.reflect.AnnotatedElement annotatedElement) {
        Tag tag = annotatedElement.getAnnotation(Tag.class);
        if (tag != null && StringUtils.hasText(tag.name())) {
            explicitTags.add(tag.name());
        }

        Tags tags = annotatedElement.getAnnotation(Tags.class);
        if (tags != null) {
            for (Tag nestedTag : tags.value()) {
                if (StringUtils.hasText(nestedTag.name())) {
                    explicitTags.add(nestedTag.name());
                }
            }
        }
    }

    private String resolveSuccessDescription(RequestMethod requestMethod) {
        return switch (requestMethod) {
            case GET -> "Successful response";
            case DELETE -> "Deletion request accepted";
            default -> "Request processed successfully";
        };
    }
}
