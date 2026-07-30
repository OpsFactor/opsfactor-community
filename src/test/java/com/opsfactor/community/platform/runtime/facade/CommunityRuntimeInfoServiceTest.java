package com.opsfactor.community.platform.runtime.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanningExecutionModelCatalog;
import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.capability.demandplanning.planningbook.domain.DemandPlanningPlanningBookCatalog;
import com.opsfactor.community.capability.supplyplanning.planningbook.domain.SupplyPlanningPlanningBookCatalog;
import com.opsfactor.community.platform.runtime.facade.dto.RuntimeInfoDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

public class CommunityRuntimeInfoServiceTest {

    @Test
    public void getRuntimeInfoShouldReturnCommunityEdition() {

        CommunityRuntimeInfoService communityRuntimeInfoService = new CommunityRuntimeInfoService();

        RuntimeInfoDTO runtimeInfoDTO = communityRuntimeInfoService.getRuntimeInfo();

        Assertions.assertEquals("community", runtimeInfoDTO.edition());
        Assertions.assertEquals(
                List.of(
                        "Moving Average",
                        "Rolling Moving Average",
                        "ARIMA",
                        "Holt-Winters",
                        "Exponential Smoothing"),
                runtimeInfoDTO.availableDemandPlanningForecastModels());
        Assertions.assertEquals(
                List.of(
                        option("Moving Average", "community", true),
                        option("Rolling Moving Average", "community", true),
                        option("ARIMA", "community", true),
                        option("Holt-Winters", "community", true),
                        option("Exponential Smoothing", "community", true),
                        option("Seasonal Naive", "enterprise", false),
                        option("STL", "enterprise", false),
                        option("Prophet", "enterprise", false),
                        option("ETS", "enterprise", false),
                        option("TBATS", "enterprise", false),
                        option("Budget as Forecast", "enterprise", false),
                        option("Chronos", "enterprise", false)),
                runtimeInfoDTO.demandPlanningForecastModelOptions());
        Assertions.assertEquals(
                List.of("Historical Sales"),
                runtimeInfoDTO.availableDemandPlanningSplitModels());
        Assertions.assertEquals(
                List.of(
                        option("Historical Sales", "community", true),
                        option("DFU-Level STL Forecast", "enterprise", false),
                        option("Hierarchical Reconciliation", "enterprise", false)),
                runtimeInfoDTO.demandPlanningSplitModelOptions());
        Assertions.assertEquals(
                List.of("Inactive"),
                runtimeInfoDTO.availableDemandPlanningStockoutTreatmentModels());
        Assertions.assertEquals(
                List.of(
                        option("Inactive", "community", true),
                        option("Smoothing of Stockout Periods", "enterprise", false)),
                runtimeInfoDTO.demandPlanningStockoutTreatmentModelOptions());
        Assertions.assertEquals(
                List.of("Inactive"),
                runtimeInfoDTO.availableDemandPlanningSmoothingModels());
        Assertions.assertEquals(
                List.of(
                        option("Inactive", "community", true),
                        option("Percentile", "enterprise", false),
                        option("Campaign", "enterprise", false)),
                runtimeInfoDTO.demandPlanningSmoothingModelOptions());
        Assertions.assertEquals(
                List.of("No Uplift Calculation"),
                runtimeInfoDTO.availableDemandPlanningUpliftModels());
        Assertions.assertEquals(
                List.of(
                        option("No Uplift Calculation", "community", true),
                        option("Event Uplift", "enterprise", false)),
                runtimeInfoDTO.demandPlanningUpliftModelOptions());
        Assertions.assertEquals(
                List.of("Sell-out"),
                runtimeInfoDTO.availableDemandPlanningHistoricalDocumentTypes());
        Assertions.assertEquals(
                List.of(
                        option("Sell-out", "community", true),
                        option("Sell-in", "enterprise", false),
                        option("Sales Orders", "enterprise", false)),
                runtimeInfoDTO.demandPlanningHistoricalDocumentTypeOptions());
        Assertions.assertEquals(
                List.of("Heuristic"),
                runtimeInfoDTO.availableSupplyPlanningExecutionModels());
        Assertions.assertEquals(
                List.of(
                        option("Heuristic", "community", true),
                        option("Optimizer", "enterprise", false),
                        option("Process Chain", "enterprise", false)),
                runtimeInfoDTO.supplyPlanningExecutionModelOptions());
        Assertions.assertEquals(
                List.of(
                        "Direct Demand",
                        "Historical Sales",
                        "Baseline",
                        "Demand Adjustment"),
                runtimeInfoDTO.visibleDemandPlanningBookKeyFigures());
        Assertions.assertEquals(
                List.of(
                        "Direct Demand",
                        "Historical Sales",
                        "Baseline",
                        "Demand Adjustment"),
                runtimeInfoDTO.selectableDemandPlanningBookKeyFigures());
        Assertions.assertEquals(
                List.of(
                        "Direct Demand",
                        "Demand Adjustment"),
                runtimeInfoDTO.editableDemandPlanningBookKeyFigures());
        Assertions.assertEquals(
                List.of(
                        "Total Demand-Working Plan",
                        "Direct Demand-Working Plan",
                        "Direct Demand - Demand Plan-Working Plan",
                        "Indirect Demand-Working Plan",
                        "Safety Stock-Working Plan",
                        "Stock-Working Plan",
                        "Planned Production-Working Plan",
                        "Planned Inbound-Working Plan"),
                runtimeInfoDTO.visibleSupplyPlanningBookKeyFigures());
        Assertions.assertEquals(
                List.of(
                        "Total Demand-Working Plan",
                        "Direct Demand-Working Plan",
                        "Direct Demand - Demand Plan-Working Plan",
                        "Indirect Demand-Working Plan",
                        "Safety Stock-Working Plan",
                        "Stock-Working Plan",
                        "Planned Production-Working Plan",
                        "Planned Inbound-Working Plan"),
                runtimeInfoDTO.selectableSupplyPlanningBookKeyFigures());
        Assertions.assertEquals(
                List.of(
                        "Stock-Working Plan",
                        "Planned Production-Working Plan",
                        "Planned Inbound-Working Plan"),
                runtimeInfoDTO.editableSupplyPlanningBookKeyFigures());

    }

    @Test
    public void getRuntimeInfoShouldStayAlignedWithCommunityStaticCatalogs() {

        CommunityRuntimeInfoService communityRuntimeInfoService = new CommunityRuntimeInfoService();

        RuntimeInfoDTO runtimeInfoDTO = communityRuntimeInfoService.getRuntimeInfo();

        /*
         * RuntimeInfo e a fonte consumida pela SPA, mas nao deve virar uma
         * allowlist paralela. A mesma lista precisa alimentar runtime-info,
         * OpenAPI e validacoes de service.
         */
        Assertions.assertEquals(
                DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiCommunity(),
                runtimeInfoDTO.availableDemandPlanningForecastModels());
        Assertions.assertEquals(
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiCommunity()),
                runtimeInfoDTO.demandPlanningForecastModelOptions());
        Assertions.assertEquals(
                DemandPlanningModelCatalog.getDpModelosSplitOpenApiCommunity(),
                runtimeInfoDTO.availableDemandPlanningSplitModels());
        Assertions.assertEquals(
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosSplitOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosSplitOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosSplitOpenApiCommunity()),
                runtimeInfoDTO.demandPlanningSplitModelOptions());
        Assertions.assertEquals(
                DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiCommunity(),
                runtimeInfoDTO.availableDemandPlanningStockoutTreatmentModels());
        Assertions.assertEquals(
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiCommunity()),
                runtimeInfoDTO.demandPlanningStockoutTreatmentModelOptions());
        Assertions.assertEquals(
                DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiCommunity(),
                runtimeInfoDTO.availableDemandPlanningSmoothingModels());
        Assertions.assertEquals(
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiCommunity()),
                runtimeInfoDTO.demandPlanningSmoothingModelOptions());
        Assertions.assertEquals(
                DemandPlanningModelCatalog.getDpModelosUpliftOpenApiCommunity(),
                runtimeInfoDTO.availableDemandPlanningUpliftModels());
        Assertions.assertEquals(
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getDpModelosUpliftOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getDpModelosUpliftOpenApiCommunity(),
                        DemandPlanningModelCatalog.getDpModelosUpliftOpenApiCommunity()),
                runtimeInfoDTO.demandPlanningUpliftModelOptions());
        Assertions.assertEquals(
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiCommunity(),
                runtimeInfoDTO.availableDemandPlanningHistoricalDocumentTypes());
        Assertions.assertEquals(
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiRuntimeOptions(),
                        DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiCommunity(),
                        DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiCommunity()),
                runtimeInfoDTO.demandPlanningHistoricalDocumentTypeOptions());
        Assertions.assertEquals(
                SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiCommunity(),
                runtimeInfoDTO.availableSupplyPlanningExecutionModels());
        Assertions.assertEquals(
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiRuntimeOptions(),
                        SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiCommunity(),
                        SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiCommunity()),
                runtimeInfoDTO.supplyPlanningExecutionModelOptions());
        Assertions.assertEquals(
                DemandPlanningPlanningBookCatalog.getKeyFiguresVisiveisDemandPlanningBookCommunity(),
                runtimeInfoDTO.visibleDemandPlanningBookKeyFigures());
        Assertions.assertEquals(
                DemandPlanningPlanningBookCatalog.getKeyFiguresSelecionaveisDemandPlanningBookCommunity(),
                runtimeInfoDTO.selectableDemandPlanningBookKeyFigures());
        Assertions.assertEquals(
                DemandPlanningPlanningBookCatalog.getKeyFiguresEditaveisDemandPlanningBookCommunity(),
                runtimeInfoDTO.editableDemandPlanningBookKeyFigures());
        Assertions.assertEquals(
                SupplyPlanningPlanningBookCatalog.getKeyFiguresVisiveisSupplyPlanningBookCommunity(),
                runtimeInfoDTO.visibleSupplyPlanningBookKeyFigures());
        Assertions.assertEquals(
                SupplyPlanningPlanningBookCatalog.getKeyFiguresSelecionaveisSupplyPlanningBookCommunity(),
                runtimeInfoDTO.selectableSupplyPlanningBookKeyFigures());
        Assertions.assertEquals(
                SupplyPlanningPlanningBookCatalog.getKeyFiguresEditaveisSupplyPlanningBookCommunity(),
                runtimeInfoDTO.editableSupplyPlanningBookKeyFigures());

    }

    @Test
    public void getRuntimeInfoShouldSerializeCommunitySelectableKeyFiguresAsNonNullAdditiveField() throws Exception {

        /*
         * RuntimeInfo e resposta JSON aberta para a SPA. O novo campo precisa
         * ser aditivo: preserva os catalogos default/editavel existentes e
         * publica somente as KFs Community que uma Configured View pode enviar.
         */
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode runtimeInfoJson = objectMapper.readTree(
                objectMapper.writeValueAsString(
                        new CommunityRuntimeInfoService().getRuntimeInfo()));

        JsonNode selectableKeyFiguresJson = runtimeInfoJson.required(
                "selectableDemandPlanningBookKeyFigures");
        Assertions.assertTrue(selectableKeyFiguresJson.isArray());
        Assertions.assertEquals(
                DemandPlanningPlanningBookCatalog.getKeyFiguresSelecionaveisDemandPlanningBookCommunity(),
                StreamSupport.stream(selectableKeyFiguresJson.spliterator(), false)
                        .map(JsonNode::asText)
                        .toList());
        Assertions.assertTrue(runtimeInfoJson.required("visibleDemandPlanningBookKeyFigures").isArray());
        Assertions.assertTrue(runtimeInfoJson.required("editableDemandPlanningBookKeyFigures").isArray());
        JsonNode selectableSupplyKeyFiguresJson = runtimeInfoJson.required(
                "selectableSupplyPlanningBookKeyFigures");
        Assertions.assertTrue(selectableSupplyKeyFiguresJson.isArray());
        Assertions.assertEquals(
                SupplyPlanningPlanningBookCatalog.getKeyFiguresSelecionaveisSupplyPlanningBookCommunity(),
                StreamSupport.stream(selectableSupplyKeyFiguresJson.spliterator(), false)
                        .map(JsonNode::asText)
                        .toList());
        Assertions.assertTrue(runtimeInfoJson.required("visibleSupplyPlanningBookKeyFigures").isArray());
        Assertions.assertTrue(runtimeInfoJson.required("editableSupplyPlanningBookKeyFigures").isArray());

    }

    @Test
    public void communityRuntimeInfoServiceShouldBeDefaultService() {

        /*
         * O Community publica a implementacao padrao do contrato. Ela nao deve
         * ser @Primary: quando o modulo Enterprise estiver no classpath, o overlay
         * privado e quem assume a prioridade do bean.
         */
        Assertions.assertTrue(CommunityRuntimeInfoService.class.isAnnotationPresent(Service.class));
        Assertions.assertFalse(CommunityRuntimeInfoService.class.isAnnotationPresent(Primary.class));

    }

    @Test
    public void communityRuntimeInfoServiceShouldRemainStaticAndTenantIndependent() {

        /*
         * RuntimeInfo e carregado uma vez pelo front e reaproveitado pela SPA.
         * Por isso a implementacao nao pode depender de repository, service de
         * tenant, usuario logado ou cache mutavel. Catalogos estaticos devem
         * continuar sendo a unica fonte da resposta Community.
         */
        long runtimeInfoServiceStateFields = Arrays.stream(CommunityRuntimeInfoService.class.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .filter(field -> !field.getName().startsWith("$"))
                .count();

        Assertions.assertEquals(0, runtimeInfoServiceStateFields);

    }

    private static RuntimeInfoDTO.RuntimeInfoOptionDTO option(
            String value,
            String requiredEdition,
            boolean availableInCurrentRuntime) {

        return new RuntimeInfoDTO.RuntimeInfoOptionDTO(
                value,
                requiredEdition,
                availableInCurrentRuntime);

    }

}
