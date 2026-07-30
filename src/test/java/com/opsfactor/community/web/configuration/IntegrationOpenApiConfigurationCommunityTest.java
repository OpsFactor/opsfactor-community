package com.opsfactor.community.web.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Valida a documentacao OpenAPI dos contratos Community que compartilham DTOs
 * com o Enterprise.
 *
 * <p>Alguns campos permanecem nos DTOs apenas para rejeicao defensiva de
 * payloads Enterprise ou arquivos legados. O Swagger Community, porem, nao deve
 * divulgar esses campos como parte do contrato publico utilizavel.</p>
 */
public class IntegrationOpenApiConfigurationCommunityTest {

    @Test
    public void optionalOpenApiHelpersShouldDeclareNullableContracts() throws Exception {

        /*
         * Esses helpers privados retornam null em contratos esperados do
         * springdoc/OpenAPI: endpoint sem body, retorno void, generics
         * explicitamente nao aplicaveis ou operacao ainda ausente em PathItem.
         * A anotacao deixa claro para futuros recortes Community/Enterprise que
         * null aqui nao representa feature pendente.
         */
        assertNullableMethod(
                "buildRequestBody",
                OpenAPI.class,
                Method.class,
                Class.class);
        assertNullableMethod(
                "resolveResponseSchema",
                OpenAPI.class,
                Method.class,
                Class.class);
        assertNullableMethod(
                "resolveSchema",
                OpenAPI.class,
                Type.class);
        assertNullableMethod(
                "resolveExplicitIntegrationResponseType",
                Method.class,
                Class.class);
        assertNullableMethod(
                "resolveExplicitIntegrationRequestBodyType",
                Method.class,
                Class.class);
        assertNullableMethod(
                "getExistingOperation",
                PathItem.class,
                RequestMethod.class);
        assertNullableMethod(
                "findConcreteMethod",
                Class.class,
                Method.class);

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldRemoveEveryConfiguredHiddenProperty() throws Exception {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        OpenAPI openApi = new OpenAPI()
                .components(new Components());
        Map<String, List<String>> hiddenSchemaProperties = getHiddenSchemaProperties();

        /*
         * Este teste e propositalmente generico: sempre que uma propriedade for
         * adicionada a lista privada de ocultacao, ela passa automaticamente a
         * ter cobertura. O campo sentinel garante que a limpeza nao remova o
         * schema inteiro por acidente.
         */
        hiddenSchemaProperties.forEach((schemaName, hiddenPropertyList) -> {
            ObjectSchema objectSchema = new ObjectSchema();
            objectSchema.addProperty("communityVisibleSentinel", new StringSchema());
            hiddenPropertyList.forEach(hiddenProperty -> objectSchema.addProperty(hiddenProperty, new StringSchema()));
            openApi.getComponents().addSchemas(schemaName, objectSchema);
        });

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        hiddenSchemaProperties.forEach((schemaName, hiddenPropertyList) -> {
            Assertions.assertTrue(
                    openApi.getComponents().getSchemas().get(schemaName).getProperties().containsKey("communityVisibleSentinel"),
                    schemaName + " deve preservar campos Community visiveis.");
            hiddenPropertyList.forEach(hiddenProperty -> Assertions.assertFalse(
                    openApi.getComponents().getSchemas().get(schemaName).getProperties().containsKey(hiddenProperty),
                    schemaName + " deve ocultar " + hiddenProperty + " no OpenAPI Community."));
        });

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldRemoveEveryConfiguredHiddenPropertyByName() throws Exception {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        List<String> hiddenPropertyNameList = getHiddenSchemaPropertiesByPropertyName();
        ObjectSchema schemaWithGeneratedName =
                (ObjectSchema) new ObjectSchema()
                        .addProperty("communityVisibleSentinel", new StringSchema());

        /*
         * Alguns schemas de classes internas podem receber nomes diferentes em
         * versoes do springdoc ou conforme o contexto que referencia o DTO.
         * Para esses casos, a guarda por nome de propriedade garante que a
         * ocultacao continue valendo mesmo que o nome do schema mude.
         */
        hiddenPropertyNameList.forEach(hiddenProperty ->
                schemaWithGeneratedName.addProperty(hiddenProperty, new StringSchema()));

        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "ParametrosGlobaisController.ParametrosGlobaisDTO",
                                schemaWithGeneratedName));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                schemaWithGeneratedName.getProperties().containsKey("communityVisibleSentinel"));
        hiddenPropertyNameList.forEach(hiddenProperty ->
                Assertions.assertFalse(
                        schemaWithGeneratedName.getProperties().containsKey(hiddenProperty),
                        hiddenProperty + " deve ser removido de qualquer schema Community."));

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldHideIntegrationEnterpriseOnlyFields() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "ProdutoIntegrationDataDto",
                                new ObjectSchema()
                                        .addProperty("description", new StringSchema())
                                        .addProperty("valueByCharacteristic", new ObjectSchema())
                                        .addProperty("unitCogs", new NumberSchema())
                                        .addProperty("unitCogsUnitOfMeasureId", new StringSchema()))
                        .addSchemas(
                                "LocationIntegrationDataDto",
                                new ObjectSchema()
                                        .addProperty("description", new StringSchema())
                                        .addProperty("latitude", new StringSchema())
                                        .addProperty("longitude", new StringSchema())
                                        .addProperty("expeditionUomId", new StringSchema())
                                        .addProperty("economicGroupId", new StringSchema())
                                        .addProperty("orderFulfillmentTimeDays", new StringSchema())
                                        .addProperty("valueByCharacteristic", new ObjectSchema()))
                        .addSchemas(
                                "IntegrationDto",
                                new ObjectSchema()
                                        .addProperty("data", new ObjectSchema())
                                        .addProperty("threadSync", new StringSchema()))
                        .addSchemas(
                                "DisponibilidadeRecursoProdutivoIntegrationDataDto",
                                new ObjectSchema()
                                        .addProperty("availableHours", new StringSchema())
                                        .addProperty("capacityInQuantity", new StringSchema())
                                        .addProperty("capacityInQuantityUomId", new StringSchema()))
                        .addSchemas(
                                "PoliticaEstoquesMaterialLocationIntegrationDataDto",
                                new ObjectSchema()
                                        .addProperty("drpMaximumStockValue", new StringSchema())
                                        .addProperty("reorderFrequencyDays", new StringSchema())));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("ProdutoIntegrationDataDto").getProperties().containsKey("description"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ProdutoIntegrationDataDto").getProperties().containsKey("valueByCharacteristic"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ProdutoIntegrationDataDto").getProperties().containsKey("unitCogs"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ProdutoIntegrationDataDto").getProperties().containsKey("unitCogsUnitOfMeasureId"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("LocationIntegrationDataDto").getProperties().containsKey("description"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationIntegrationDataDto").getProperties().containsKey("latitude"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationIntegrationDataDto").getProperties().containsKey("longitude"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationIntegrationDataDto").getProperties().containsKey("expeditionUomId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationIntegrationDataDto").getProperties().containsKey("economicGroupId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationIntegrationDataDto").getProperties().containsKey("orderFulfillmentTimeDays"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationIntegrationDataDto").getProperties().containsKey("valueByCharacteristic"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("IntegrationDto").getProperties().containsKey("data"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("IntegrationDto").getProperties().containsKey("threadSync"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("DisponibilidadeRecursoProdutivoIntegrationDataDto").getProperties().containsKey("availableHours"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DisponibilidadeRecursoProdutivoIntegrationDataDto").getProperties().containsKey("capacityInQuantity"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DisponibilidadeRecursoProdutivoIntegrationDataDto").getProperties().containsKey("capacityInQuantityUomId"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PoliticaEstoquesMaterialLocationIntegrationDataDto").getProperties().containsKey("drpMaximumStockValue"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PoliticaEstoquesMaterialLocationIntegrationDataDto").getProperties().containsKey("reorderFrequencyDays"));

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldCleanRequiredList() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        ObjectSchema linhaTransporteIntegrationDataDtoSchema =
                (ObjectSchema) new ObjectSchema()
                        .addProperty("leadTimeDays", new StringSchema())
                        .addProperty("distanceKm", new StringSchema());
        linhaTransporteIntegrationDataDtoSchema.setRequired(List.of("leadTimeDays", "distanceKm"));
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "LinhaTransporteIntegrationDataDto",
                                linhaTransporteIntegrationDataDtoSchema));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                linhaTransporteIntegrationDataDtoSchema.getProperties().containsKey("leadTimeDays"));
        Assertions.assertFalse(
                linhaTransporteIntegrationDataDtoSchema.getProperties().containsKey("distanceKm"));
        Assertions.assertEquals(
                List.of("leadTimeDays"),
                linhaTransporteIntegrationDataDtoSchema.getRequired());

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldHideConfiguredViewEnterpriseOnlyFields() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "ConfiguredViewDTO",
                                new ObjectSchema()
                                        .addProperty("viewName", new StringSchema())
                                        .addProperty("directDemandUpdateKeyFigure", new StringSchema())
                                        .addProperty("materialCharacteristicDetailList", new ObjectSchema())
                                        .addProperty("locationCharacteristicDetailList", new ObjectSchema())
                                        .addProperty("materialLocationCharacteristicDetailList", new ObjectSchema())
                                        .addProperty("showMaterialLevel", new StringSchema())
                                        .addProperty("showLocationLevel", new StringSchema())
                                        .addProperty("keyFigureList", new ObjectSchema())
                                        .addProperty("demandPlanWorkflowId", new StringSchema())
                                        .addProperty("demandPlanWorkflowStageId", new StringSchema()))
                        .addSchemas(
                                "ConfiguredViewCaracteristicaDTO",
                                new ObjectSchema()
                                        .addProperty("characteristicId", new StringSchema())
                                        .addProperty("filteredValues", new ObjectSchema()))
                        .addSchemas(
                                "ConfiguredViewKeyFigureDTO",
                                new ObjectSchema()
                                        .addProperty("keyFigure", new StringSchema())
                                        .addProperty("allowChanges", new StringSchema())
                                        .addProperty("position", new StringSchema())
                                        .addProperty("userId", new StringSchema())
                                        .addProperty("viewName", new StringSchema())
                                        .addProperty("viewType", new StringSchema())));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("viewName"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("directDemandUpdateKeyFigure"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("materialCharacteristicDetailList"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("locationCharacteristicDetailList"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("materialLocationCharacteristicDetailList"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("showMaterialLevel"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("showLocationLevel"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("keyFigureList"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("demandPlanWorkflowId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewDTO").getProperties().containsKey("demandPlanWorkflowStageId"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("ConfiguredViewCaracteristicaDTO").getProperties().isEmpty());
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("ConfiguredViewKeyFigureDTO").getProperties().isEmpty());

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldHideCharacteristicMapsFromCommunityFilters() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "FiltroMaterialLocationDeCombinacaoCaracteristicasDTO",
                                new ObjectSchema()
                                        .addProperty("materialIds", new ObjectSchema())
                                        .addProperty("locationIds", new ObjectSchema())
                                        .addProperty("valuesByMaterialCharacteristicId", new ObjectSchema())
                                        .addProperty("valuesByLocationCharacteristicId", new ObjectSchema())));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("FiltroMaterialLocationDeCombinacaoCaracteristicasDTO").getProperties().containsKey("materialIds"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("FiltroMaterialLocationDeCombinacaoCaracteristicasDTO").getProperties().containsKey("locationIds"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("FiltroMaterialLocationDeCombinacaoCaracteristicasDTO").getProperties().containsKey("valuesByMaterialCharacteristicId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("FiltroMaterialLocationDeCombinacaoCaracteristicasDTO").getProperties().containsKey("valuesByLocationCharacteristicId"));

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldHideDemandAnalysisAndPlanningBookEnterpriseFields() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "SimulatedDemandPlanDTO",
                                new ObjectSchema()
                                        .addProperty("materialLocationData", new ObjectSchema())
                                        .addProperty("aggregatedDataAtMapeLevel", new ObjectSchema()))
                        .addSchemas(
                                "ConfiguredViewSelectionDTO",
                                new ObjectSchema()
                                        .addProperty("planId", new StringSchema())
                                        .addProperty("referencePlanId", new StringSchema())
                                        .addProperty("materialAggregationLevelId", new StringSchema())
                                        .addProperty("locationAggregationLevelId", new StringSchema())
                                        .required(List.of(
                                                "planId",
                                                "referencePlanId",
                                                "materialAggregationLevelId",
                                                "locationAggregationLevelId")))
                        .addSchemas(
                                "SelectedPlanningBookCellDTO",
                                new ObjectSchema()
                                        .addProperty("planId", new StringSchema())
                                        .addProperty("referencePlanId", new StringSchema())));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("SimulatedDemandPlanDTO").getProperties().containsKey("materialLocationData"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("SimulatedDemandPlanDTO").getProperties().containsKey("aggregatedDataAtMapeLevel"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("ConfiguredViewSelectionDTO").getProperties().containsKey("planId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewSelectionDTO").getProperties().containsKey("referencePlanId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewSelectionDTO").getProperties().containsKey("materialAggregationLevelId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("ConfiguredViewSelectionDTO").getProperties().containsKey("locationAggregationLevelId"));
        Assertions.assertEquals(
                List.of("planId"),
                openApi.getComponents().getSchemas().get("ConfiguredViewSelectionDTO").getRequired());
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("SelectedPlanningBookCellDTO").getProperties().containsKey("planId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("SelectedPlanningBookCellDTO").getProperties().containsKey("referencePlanId"));

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldHideSupplyProfileEnterpriseFields() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "PerfilExecucaoSupplyPlanDTO",
                                new ObjectSchema()
                                        .addProperty("planHorizonInDays", new StringSchema())
                                        .addProperty("generatePlannedInboundOrders", new StringSchema())
                                        .addProperty("considerInitialStock", new StringSchema())
                                        .addProperty("materialFilterId", new StringSchema())
                                        .addProperty("executionModel", new StringSchema())
                                        .addProperty("aiOptimizer", new StringSchema())
                                        .addProperty("optimizationModelType", new StringSchema())
                                        .addProperty("maximumOptimizerExecutionTime", new StringSchema())
                                        .addProperty("saveOptimizerVariablesAndConstraints", new StringSchema())
                                        .addProperty("saveConstraintBacktracking", new StringSchema())
                                        .addProperty("customerDemandPrioritizationModelId", new StringSchema())
                                        .addProperty("safetyStockPrioritizationModelId", new StringSchema())
                                        .addProperty("ignoreLeadTimeConstraintsForUnconstrainedPlan", new StringSchema())
                                        .addProperty("ignoreStorageConstraintsForUnconstrainedPlan", new StringSchema())
                                        .addProperty("ignoreInboundConstraintsForUnconstrainedPlan", new StringSchema())
                                        .addProperty("ignoreOutboundConstraintsForUnconstrainedPlan", new StringSchema())
                                        .addProperty("ignoreMarginConstraintsForUnconstrainedPlan", new StringSchema())
                                        .addProperty("demandPlanMetDemandImpactCoefficient", new StringSchema())
                                        .addProperty("considerForecastForMto", new StringSchema())
                                        .addProperty("considerSellinOrdersFuture", new StringSchema())
                                        .addProperty("allowBacklogCarryOver", new StringSchema())
                                        .addProperty("forceMakeToOrderModel", new StringSchema())
                                        .addProperty("enableDemandCatchUpFromPastSellout", new StringSchema())
                                        .addProperty("considerUnmetClientOrderImpact", new StringSchema())
                                        .addProperty("generatePL", new StringSchema())
                                        .addProperty("generateProfitLoss", new StringSchema())
                                        .addProperty("allowSalesProfitLossBomRetroaction", new StringSchema())
                                        .addProperty("productiveCapacityType", new StringSchema())
                                        .addProperty("logisticsCapacityLevel", new StringSchema())
                                        .addProperty("considerStorageConstraints", new StringSchema())
                                        .addProperty("considerInboundConstraints", new StringSchema())
                                        .addProperty("considerOutboundConstraints", new StringSchema())
                                        .addProperty("allowStockAtClients", new StringSchema())
                                        .addProperty("allowStockAtTransshipmentPoints", new StringSchema())
                                        .addProperty("generateProductionScheduling", new StringSchema())
                                        .addProperty("safetyStockFairShare", new StringSchema())
                                        .addProperty("allocateTransfersInFleets", new StringSchema())
                                        .addProperty("greenfieldLocationActivationBudget", new StringSchema())
                                        .addProperty("segmentInventoryByBatch", new StringSchema())
                                        .addProperty("applyFreightCostCurves", new StringSchema())
                                        .addProperty("applyLocationCostCurves", new StringSchema())
                                        .addProperty("softTargetMaximumPercentPenalty", new StringSchema())
                                        .addProperty("softTargetDeviationAmplitudeAsTargetPercent", new StringSchema())
                                        .addProperty("softTargetDeviationLinearizationNumberSegments", new StringSchema())
                                        .addProperty("penalizeUnmetDemand", new StringSchema())
                                        .addProperty("unmetDemandPenalizationAsFractionOfGrossSales", new StringSchema())
                                        .addProperty("unmetDemandPenalizationAsUnitImpact", new StringSchema())
                                        .addProperty("unmetDemandPenalizationAsUnitImpactUomId", new StringSchema())
                                        .addProperty("temporalSplitCurveIdSet", new ObjectSchema())
                                        .required(List.of("planHorizonInDays", "optimizationModelType"))));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("planHorizonInDays"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("generatePlannedInboundOrders"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("considerInitialStock"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("materialFilterId"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("executionModel"));
        Assertions.assertEquals(
                List.of("Heuristic"),
                ((StringSchema) openApi.getComponents()
                        .getSchemas()
                        .get("PerfilExecucaoSupplyPlanDTO")
                        .getProperties()
                        .get("executionModel"))
                        .getEnum());
        Assertions.assertTrue(
                ((StringSchema) openApi.getComponents()
                        .getSchemas()
                        .get("PerfilExecucaoSupplyPlanDTO")
                        .getProperties()
                        .get("executionModel"))
                        .getDescription()
                        .contains("Optimizer"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("aiOptimizer"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("optimizationModelType"));
        Assertions.assertEquals(
                List.of("planHorizonInDays"),
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getRequired());
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("maximumOptimizerExecutionTime"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("saveOptimizerVariablesAndConstraints"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("saveConstraintBacktracking"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("customerDemandPrioritizationModelId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("safetyStockPrioritizationModelId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("ignoreLeadTimeConstraintsForUnconstrainedPlan"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("ignoreStorageConstraintsForUnconstrainedPlan"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("ignoreInboundConstraintsForUnconstrainedPlan"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("ignoreOutboundConstraintsForUnconstrainedPlan"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("ignoreMarginConstraintsForUnconstrainedPlan"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("demandPlanMetDemandImpactCoefficient"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("considerForecastForMto"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("considerSellinOrdersFuture"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("allowBacklogCarryOver"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("forceMakeToOrderModel"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("enableDemandCatchUpFromPastSellout"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("considerUnmetClientOrderImpact"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("generatePL"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("generateProfitLoss"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("allowSalesProfitLossBomRetroaction"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("productiveCapacityType"));
        Assertions.assertEquals(
                List.of("Total Hours / Day"),
                ((StringSchema) openApi.getComponents()
                        .getSchemas()
                        .get("PerfilExecucaoSupplyPlanDTO")
                        .getProperties()
                        .get("productiveCapacityType"))
                        .getEnum());
        Assertions.assertTrue(
                ((StringSchema) openApi.getComponents()
                        .getSchemas()
                        .get("PerfilExecucaoSupplyPlanDTO")
                        .getProperties()
                        .get("productiveCapacityType"))
                        .getDescription()
                        .contains("Quantity-based capacity"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("logisticsCapacityLevel"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("considerStorageConstraints"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("considerInboundConstraints"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("considerOutboundConstraints"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("allowStockAtClients"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("allowStockAtTransshipmentPoints"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("generateProductionScheduling"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("safetyStockFairShare"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("allocateTransfersInFleets"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("greenfieldLocationActivationBudget"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("segmentInventoryByBatch"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("applyFreightCostCurves"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("applyLocationCostCurves"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("softTargetMaximumPercentPenalty"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("softTargetDeviationAmplitudeAsTargetPercent"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("softTargetDeviationLinearizationNumberSegments"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("penalizeUnmetDemand"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("unmetDemandPenalizationAsFractionOfGrossSales"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("unmetDemandPenalizationAsUnitImpact"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("unmetDemandPenalizationAsUnitImpactUomId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoSupplyPlanDTO").getProperties().containsKey("temporalSplitCurveIdSet"));

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldHideDemandPlanningEnterpriseFields() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        StringSchema statisticalModelSchema = new StringSchema();
        statisticalModelSchema.setEnum(List.of(
                "Moving Average",
                "Chronos",
                "STL"));

        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "PerfilExecucaoDemandPlanDTO",
                                new ObjectSchema()
                                        .addProperty("bucketSize", new StringSchema())
                                        .addProperty("planningHorizonInPeriods", new StringSchema())
                                        .addProperty("historicalSalesDocumentType", new StringSchema())
                                        .addProperty("mapeMaterialAggregationLevelId", new StringSchema())
                                        .addProperty("mapeLocationAggregationLevelId", new StringSchema())
                                        .addProperty("defaultAutoTunedDemandPlanConfigurationId", new StringSchema())
                                        .addProperty("autofitModelType", new StringSchema())
                                        .addProperty("modelAutofitObjectiveFunction", new StringSchema())
                                        .addProperty("modelAutofitNumberOfPeriodsForAccuracyEvaluation", new StringSchema())
                                        .addProperty("modelAutofitEvaluationLagInPeriods", new StringSchema())
                                        .addProperty("regressionTreeObjectiveFunction", new StringSchema()))
                        .addSchemas(
                                "DemandPlanningGeneralParametersDTO",
                                new ObjectSchema()
                                        .addProperty("executeDemandPlan", new StringSchema())
                                        .addProperty("materialAggregationType", new StringSchema())
                                        .addProperty("generateForecastForDiscontinuedMaterials", new StringSchema())
                                        .addProperty("useExecutionProfileAutofitModel", new StringSchema())
                                        .addProperty("budgetId", new StringSchema())
                                        .addProperty("daysAsNewMaterial", new StringSchema())
                                        .addProperty("regressionTimeSeries", new ObjectSchema())
                                        .addProperty("considerTargetTrendGrowthYoy", new StringSchema())
                                        .addProperty("numberOfDaysCurrentLevelAsAverageOfHistoricalStl", new StringSchema())
                                        .addProperty("targetGrowthYoy", new StringSchema())
                                        .addProperty("includeWorkingDaysRegressor", new StringSchema()))
                        .addSchemas(
                                "DemandPlanningForecastParametersDTO",
                                new ObjectSchema()
                                        .addProperty("statisticalModel", statisticalModelSchema)
                                        .addProperty("daysMovingAverageModel", new StringSchema())
                                        .addProperty("alpha", new StringSchema())
                                        .addProperty("splitModel", new StringSchema())
                                        .addProperty("considerStockoutData", new StringSchema())
                                        .addProperty("daysSmoothingModel", new StringSchema())
                                        .addProperty("enableUpperPercentileSmoothing", new StringSchema())
                                        .addProperty("smoothingUpperPercentile", new StringSchema())
                                        .addProperty("enableLowerPercentileSmoothing", new StringSchema())
                                        .addProperty("smoothingLowerPercentile", new StringSchema())
                                        .addProperty("smoothingModel", new StringSchema())
                                        .addProperty("upliftModel", new StringSchema())
                                        .addProperty("prophetAutoSeasonalityPriorScale", new StringSchema())
                                        .addProperty("prophetSeasonalityPriorScale", new StringSchema())
                                        .addProperty("prophetAutoChangepointPriorScale", new StringSchema())
                                        .addProperty("prophetChangepointPriorScale", new StringSchema())
                                        .addProperty("prophetAutoYearlyFourierOrder", new StringSchema())
                                        .addProperty("prophetYearlyFourierOrder", new StringSchema())
                                        .addProperty("chronosForceAggregatedForecast", new StringSchema())));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("bucketSize"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("planningHorizonInPeriods"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("historicalSalesDocumentType"));
        Assertions.assertEquals(
                List.of("Sell-out"),
                ((StringSchema) openApi.getComponents()
                        .getSchemas()
                        .get("PerfilExecucaoDemandPlanDTO")
                        .getProperties()
                        .get("historicalSalesDocumentType"))
                        .getEnum());
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("mapeMaterialAggregationLevelId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("mapeLocationAggregationLevelId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("defaultAutoTunedDemandPlanConfigurationId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("autofitModelType"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("modelAutofitObjectiveFunction"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("modelAutofitNumberOfPeriodsForAccuracyEvaluation"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("modelAutofitEvaluationLagInPeriods"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PerfilExecucaoDemandPlanDTO").getProperties().containsKey("regressionTreeObjectiveFunction"));

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("executeDemandPlan"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("materialAggregationType"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("generateForecastForDiscontinuedMaterials"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("generateForecastForDiscontinuedProducts"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("useExecutionProfileAutofitModel"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("budgetId"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("daysAsNewMaterial"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("daysAsNewProduct"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("regressionTimeSeries"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("considerTargetTrendGrowthYoy"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("numberOfDaysCurrentLevelAsAverageOfHistoricalStl"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("targetGrowthYoy"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningGeneralParametersDTO").getProperties().containsKey("includeWorkingDaysRegressor"));

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("statisticalModel"));
        Assertions.assertEquals(
                List.of(
                        "Moving Average",
                        "Rolling Moving Average",
                        "ARIMA",
                        "Holt-Winters",
                        "Exponential Smoothing"),
                ((StringSchema) openApi.getComponents()
                        .getSchemas()
                        .get("DemandPlanningForecastParametersDTO")
                        .getProperties()
                        .get("statisticalModel"))
                        .getEnum());
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("daysMovingAverageModel"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("alpha"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("splitModel"));
        Assertions.assertEquals(
                List.of("Historical Sales"),
                ((StringSchema) openApi.getComponents()
                        .getSchemas()
                        .get("DemandPlanningForecastParametersDTO")
                        .getProperties()
                        .get("splitModel"))
                        .getEnum());
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("considerStockoutData"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("daysSmoothingModel"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("enableUpperPercentileSmoothing"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("smoothingUpperPercentile"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("enableLowerPercentileSmoothing"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("smoothingLowerPercentile"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("smoothingModel"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("upliftModel"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("prophetAutoSeasonalityPriorScale"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("prophetSeasonalityPriorScale"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("prophetAutoChangepointPriorScale"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("prophetChangepointPriorScale"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("prophetAutoYearlyFourierOrder"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("prophetYearlyFourierOrder"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("DemandPlanningForecastParametersDTO").getProperties().containsKey("chronosForceAggregatedForecast"));

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldRestrictGlobalParametersSalesDocumentType() throws Exception {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        List<String> hiddenPropertyNameList = getHiddenSchemaPropertiesByPropertyName();
        ObjectSchema parametrosGlobaisDTOSchema =
                (ObjectSchema) new ObjectSchema()
                        .addProperty("timeZone", new StringSchema())
                        .addProperty("tipoDocumentoVenda", new StringSchema());
        hiddenPropertyNameList.forEach(hiddenProperty ->
                parametrosGlobaisDTOSchema.addProperty(hiddenProperty, new StringSchema()));

        List<String> requiredPropertyList = new ArrayList<>();
        requiredPropertyList.add("timeZone");
        requiredPropertyList.addAll(hiddenPropertyNameList);
        parametrosGlobaisDTOSchema.setRequired(requiredPropertyList);

        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "ParametrosGlobaisDTO",
                                parametrosGlobaisDTOSchema));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("ParametrosGlobaisDTO").getProperties().containsKey("timeZone"));
        hiddenPropertyNameList.forEach(hiddenProperty ->
                Assertions.assertFalse(
                        openApi.getComponents().getSchemas().get("ParametrosGlobaisDTO").getProperties().containsKey(hiddenProperty),
                        hiddenProperty + " deve ficar fora do contrato OpenAPI Community."));
        Assertions.assertEquals(
                List.of("timeZone"),
                parametrosGlobaisDTOSchema.getRequired());
        Assertions.assertEquals(
                List.of("SELLOUT"),
                ((StringSchema) openApi.getComponents()
                        .getSchemas()
                        .get("ParametrosGlobaisDTO")
                        .getProperties()
                        .get("tipoDocumentoVenda"))
                        .getEnum());
        Assertions.assertTrue(
                ((StringSchema) openApi.getComponents()
                        .getSchemas()
                        .get("ParametrosGlobaisDTO")
                        .getProperties()
                        .get("tipoDocumentoVenda"))
                        .getDescription()
                        .contains("Sell-out"));

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldKeepInboundConstraintAndHideLocationEnterpriseFields() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "LocationDTO",
                                new ObjectSchema()
                                        .addProperty("id", new StringSchema())
                                        .addProperty("description", new StringSchema())
                                        .addProperty("showInSupplyPlanningBook", new StringSchema())
                                        .addProperty("applyProductionConstraints", new StringSchema())
                                        .addProperty("latitude", new StringSchema())
                                        .addProperty("longitude", new StringSchema())
                                        .addProperty("characteristicValues", new ObjectSchema())
                                        .addProperty("showInDeployment", new StringSchema())
                                        .addProperty("applyInboundConstraints", new StringSchema())
                                        .addProperty("applyLogisticsConstraints", new StringSchema())));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("id"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("description"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("showInSupplyPlanningBook"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("applyProductionConstraints"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("applyInboundConstraints"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("latitude"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("longitude"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("characteristicValues"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("showInDeployment"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("LocationDTO").getProperties().containsKey("applyLogisticsConstraints"));

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldHideClusterCharacteristicRuleFields() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "RegraAlocaoClusterProdutosDTO",
                                new ObjectSchema()
                                        .addProperty("id", new StringSchema())
                                        .addProperty("criterio", new StringSchema())
                                        .addProperty("caracteristicaDTO", new ObjectSchema())
                                        .addProperty("atributosCaracteristica", new ObjectSchema()))
                        .addSchemas(
                                "RegraAlocaoClusterLocationsCaracteristicaDTO",
                                new ObjectSchema()
                                        .addProperty("id", new StringSchema())
                                        .addProperty("criterio", new StringSchema())
                                        .addProperty("caracteristicaDTO", new ObjectSchema())
                                        .addProperty("atributosCaracteristica", new ObjectSchema())));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("RegraAlocaoClusterProdutosDTO").getProperties().containsKey("criterio"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("RegraAlocaoClusterProdutosDTO").getProperties().containsKey("caracteristicaDTO"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("RegraAlocaoClusterProdutosDTO").getProperties().containsKey("atributosCaracteristica"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("RegraAlocaoClusterLocationsCaracteristicaDTO").getProperties().containsKey("criterio"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("RegraAlocaoClusterLocationsCaracteristicaDTO").getProperties().containsKey("caracteristicaDTO"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("RegraAlocaoClusterLocationsCaracteristicaDTO").getProperties().containsKey("atributosCaracteristica"));

    }

    @Test
    public void removeCommunityHiddenSchemaPropertiesShouldHideInventoryPolicyOptimizationFields() {

        IntegrationOpenApiConfiguration integrationOpenApiConfiguration =
                new IntegrationOpenApiConfiguration();
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas(
                                "PoliticaEstoquesMaterialLocationDTO",
                                new ObjectSchema()
                                        .addProperty("materialId", new StringSchema())
                                        .addProperty("estoqueSegurancaDrpOuTargetKanban", new StringSchema())
                                        .addProperty("frequenciaReabastecimentoDias", new StringSchema()))
                        .addSchemas(
                                "PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO",
                                new ObjectSchema()
                                        .addProperty("locationId", new StringSchema())
                                        .addProperty("estoqueMaximoDrp", new StringSchema())
                                        .addProperty("frequenciaReabastecimentoDias", new StringSchema())));

        integrationOpenApiConfiguration.removeCommunityHiddenSchemaProperties(openApi);

        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PoliticaEstoquesMaterialLocationDTO").getProperties().containsKey("materialId"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PoliticaEstoquesMaterialLocationDTO").getProperties().containsKey("estoqueSegurancaDrpOuTargetKanban"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PoliticaEstoquesMaterialLocationDTO").getProperties().containsKey("frequenciaReabastecimentoDias"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO").getProperties().containsKey("locationId"));
        Assertions.assertTrue(
                openApi.getComponents().getSchemas().get("PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO").getProperties().containsKey("estoqueMaximoDrp"));
        Assertions.assertFalse(
                openApi.getComponents().getSchemas().get("PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO").getProperties().containsKey("frequenciaReabastecimentoDias"));

    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> getHiddenSchemaProperties() throws Exception {

        Field hiddenSchemaPropertiesField = IntegrationOpenApiConfiguration.class.getDeclaredField(
                "COMMUNITY_OPENAPI_HIDDEN_SCHEMA_PROPERTIES");
        hiddenSchemaPropertiesField.setAccessible(true);
        return (Map<String, List<String>>) hiddenSchemaPropertiesField.get(null);

    }

    @SuppressWarnings("unchecked")
    private static List<String> getHiddenSchemaPropertiesByPropertyName() throws Exception {

        Field hiddenSchemaPropertiesByPropertyNameField = IntegrationOpenApiConfiguration.class.getDeclaredField(
                "COMMUNITY_OPENAPI_HIDDEN_SCHEMA_PROPERTIES_BY_PROPERTY_NAME");
        hiddenSchemaPropertiesByPropertyNameField.setAccessible(true);
        return (List<String>) hiddenSchemaPropertiesByPropertyNameField.get(null);

    }

    private static void assertNullableMethod(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {

        Method method = IntegrationOpenApiConfiguration.class.getDeclaredMethod(methodName, parameterTypes);
        Assertions.assertTrue(
                method.isAnnotationPresent(Nullable.class),
                methodName + " deve declarar @Nullable porque null e contrato OpenAPI esperado.");

    }

}
