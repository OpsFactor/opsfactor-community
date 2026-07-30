package com.opsfactor.community.platform.runtime;

import com.opsfactor.community.platform.runtime.facade.dto.RuntimeInfoDTO;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contratos estruturais do DTO aberto de runtime-info.
 */
class RuntimeInfoDTOTest {

    @Test
    void recordShouldExposeOnlyEditionAsRuntimeIdentity() {

        List<String> runtimeInfoDTOComponentNames = Arrays.stream(RuntimeInfoDTO.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        /*
         * O front deve receber apenas `edition` como identidade do runtime.
         * Campos como productName, displayName ou booleano enterprise duplicam
         * informacao, tendem a virar regra paralela na SPA e nao fazem parte do
         * contrato Community/Enterprise atual.
         */
        Assertions.assertEquals(
                List.of(
                        "edition",
                        "availableDemandPlanningForecastModels",
                        "demandPlanningForecastModelOptions",
                        "availableDemandPlanningSplitModels",
                        "demandPlanningSplitModelOptions",
                        "availableDemandPlanningStockoutTreatmentModels",
                        "demandPlanningStockoutTreatmentModelOptions",
                        "availableDemandPlanningSmoothingModels",
                        "demandPlanningSmoothingModelOptions",
                        "availableDemandPlanningUpliftModels",
                        "demandPlanningUpliftModelOptions",
                        "availableDemandPlanningHistoricalDocumentTypes",
                        "demandPlanningHistoricalDocumentTypeOptions",
                        "availableSupplyPlanningExecutionModels",
                        "supplyPlanningExecutionModelOptions",
                        "visibleDemandPlanningBookKeyFigures",
                        "selectableDemandPlanningBookKeyFigures",
                        "editableDemandPlanningBookKeyFigures",
                        "visibleSupplyPlanningBookKeyFigures",
                        "selectableSupplyPlanningBookKeyFigures",
                        "editableSupplyPlanningBookKeyFigures"),
                runtimeInfoDTOComponentNames);

    }

    @Test
    void constructorShouldDefensivelyCopyAvailableDemandPlanningForecastModels() {

        List<String> availableDemandPlanningForecastModels = new ArrayList<>(
                List.of("Moving Average"));

        RuntimeInfoDTO runtimeInfoDTO = createValidRuntimeInfoDTO(availableDemandPlanningForecastModels);
        availableDemandPlanningForecastModels.add("Prophet");

        Assertions.assertEquals(
                List.of("Moving Average"),
                runtimeInfoDTO.availableDemandPlanningForecastModels());
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.availableDemandPlanningForecastModels().add("Chronos"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.demandPlanningForecastModelOptions().add(
                        option("Chronos", "enterprise", false)));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.availableDemandPlanningSplitModels().add("Hierarchical Reconciliation"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.availableDemandPlanningSmoothingModels().add("Percentile"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.availableDemandPlanningHistoricalDocumentTypes().add("Sell-in"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.availableSupplyPlanningExecutionModels().add("Process Chain"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.visibleDemandPlanningBookKeyFigures().add("Uplift"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.editableDemandPlanningBookKeyFigures().add("Historical Sales"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.selectableDemandPlanningBookKeyFigures().add("Gross Sales"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.visibleSupplyPlanningBookKeyFigures().add("Production Orders-Working Plan"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.selectableSupplyPlanningBookKeyFigures().add("Inbound Orders"));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> runtimeInfoDTO.editableSupplyPlanningBookKeyFigures().add("Indirect Demand-Working Plan"));

    }

    @Test
    void constructorShouldRejectUnknownEdition() {

        /*
         * `edition` e o unico identificador canonico que a SPA carrega uma vez
         * e reaproveita. Qualquer novo valor precisa ser discutido como uma
         * edicao de produto real antes de entrar no contrato publico.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> createValidRuntimeInfoDTO("enterprise-trial"));

        Assertions.assertEquals(
                "RuntimeInfoDTO.edition must be community or enterprise.",
                illegalArgumentException.getMessage());

    }

    @Test
    void constructorShouldRejectNullRuntimeCatalogListWithFieldName() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new RuntimeInfoDTO(
                        "community",
                        null,
                        List.of(option("Moving Average", "community", true)),
                        List.of("Historical Sales"),
                        List.of(option("Historical Sales", "community", true)),
                        List.of("Inactive"),
                        List.of(option("Inactive", "community", true)),
                        List.of("Inactive"),
                        List.of(option("Inactive", "community", true)),
                        List.of("No Uplift Calculation"),
                        List.of(option("No Uplift Calculation", "community", true)),
                        List.of("Sell-out"),
                        List.of(option("Sell-out", "community", true)),
                        List.of("Heuristic"),
                        List.of(option("Heuristic", "community", true)),
                        List.of("Direct Demand", "Historical Sales", "Baseline", "Demand Adjustment"),
                        List.of("Direct Demand", "Historical Sales", "Baseline", "Demand Adjustment"),
                        List.of("Direct Demand", "Demand Adjustment"),
                        List.of(
                                "Total Demand-Working Plan",
                                "Direct Demand-Working Plan",
                                "Direct Demand - Demand Plan-Working Plan",
                                "Indirect Demand-Working Plan",
                                "Safety Stock-Working Plan",
                                "Stock-Working Plan",
                                "Planned Production-Working Plan",
                                "Planned Inbound-Working Plan"),
                        List.of(
                                "Total Demand-Working Plan",
                                "Direct Demand-Working Plan",
                                "Direct Demand - Demand Plan-Working Plan",
                                "Indirect Demand-Working Plan",
                                "Safety Stock-Working Plan",
                                "Stock-Working Plan",
                                "Planned Production-Working Plan",
                                "Planned Inbound-Working Plan"),
                        List.of(
                                "Stock-Working Plan",
                                "Planned Production-Working Plan",
                                "Planned Inbound-Working Plan")));

        Assertions.assertEquals(
                "RuntimeInfoDTO.availableDemandPlanningForecastModels is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    void optionListBuilderShouldMarkEnterpriseOptionsBlockedInCommunity() {

        List<RuntimeInfoDTO.RuntimeInfoOptionDTO> runtimeInfoOptionDTOList =
                RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        List.of("Historical Sales", "Hierarchical Reconciliation"),
                        List.of("Historical Sales"),
                        List.of("Historical Sales"));

        Assertions.assertEquals(
                List.of(
                        option("Historical Sales", "community", true),
                        option("Hierarchical Reconciliation", "enterprise", false)),
                runtimeInfoOptionDTOList);

    }

    @Test
    void runtimeInfoOptionShouldExposeDisabledMarkerAndReason() {

        RuntimeInfoDTO.RuntimeInfoOptionDTO communityOption =
                option("Historical Sales", "community", true);
        RuntimeInfoDTO.RuntimeInfoOptionDTO enterpriseOption =
                option("Chronos", "enterprise", false);

        /*
         * O front pode usar `availableInCurrentRuntime` como allowlist
         * funcional, mas `disabled` e `disabledReason` deixam o comportamento
         * visual pronto para seletores/cards que misturam Community e
         * Enterprise na mesma tela.
         */
        Assertions.assertFalse(communityOption.disabled());
        Assertions.assertNull(communityOption.disabledReason());
        Assertions.assertTrue(enterpriseOption.disabled());
        Assertions.assertEquals(
                "Requires OpsFactor Enterprise.",
                enterpriseOption.disabledReason());

    }

    @Test
    void runtimeInfoOptionDisabledReasonNullableContractShouldBeExplicit() throws Exception {

        Method disabledReasonAccessor = RuntimeInfoDTO.RuntimeInfoOptionDTO.class.getMethod("disabledReason");
        Method getDefaultDisabledReasonMethod = RuntimeInfoDTO.class.getDeclaredMethod(
                "getDefaultDisabledReason",
                String.class,
                boolean.class);
        Method normalizeDisabledReasonMethod = RuntimeInfoDTO.class.getDeclaredMethod(
                "normalizeDisabledReason",
                boolean.class,
                String.class);

        /*
         * Motivo de bloqueio nulo e contrato valido quando a opcao esta
         * habilitada. O front usa `disabled` como indicador binario e so exibe
         * tooltip quando `disabledReason` existe.
         */
        Assertions.assertTrue(disabledReasonAccessor.isAnnotationPresent(Nullable.class));
        Assertions.assertTrue(getDefaultDisabledReasonMethod.isAnnotationPresent(Nullable.class));
        Assertions.assertTrue(normalizeDisabledReasonMethod.isAnnotationPresent(Nullable.class));
        Assertions.assertTrue(normalizeDisabledReasonMethod.getParameters()[1].isAnnotationPresent(Nullable.class));

    }

    @Test
    void runtimeInfoOptionShouldRejectInconsistentDisabledMarker() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new RuntimeInfoDTO.RuntimeInfoOptionDTO(
                        "Chronos",
                        "enterprise",
                        false,
                        false,
                        null));

        Assertions.assertEquals(
                "RuntimeInfoOptionDTO.disabled must be the inverse of availableInCurrentRuntime.",
                illegalArgumentException.getMessage());

    }

    @Test
    void optionListBuilderShouldRejectCommunityValueOutsideVisualCatalog() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        List.of("Historical Sales"),
                        List.of("Historical Sales", "Hierarchical Reconciliation"),
                        List.of("Historical Sales")));

        Assertions.assertEquals(
                "RuntimeInfoDTO.communityValues contains values absent from RuntimeInfoDTO.productCatalogValues: [Hierarchical Reconciliation]",
                illegalArgumentException.getMessage());

    }

    @Test
    void optionListBuilderShouldRejectAvailableValueOutsideVisualCatalog() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RuntimeInfoDTO.buildRuntimeInfoOptionList(
                        List.of("Historical Sales"),
                        List.of("Historical Sales"),
                        List.of("Historical Sales", "Hierarchical Reconciliation")));

        Assertions.assertEquals(
                "RuntimeInfoDTO.currentRuntimeAvailableValues contains values absent from RuntimeInfoDTO.productCatalogValues: [Hierarchical Reconciliation]",
                illegalArgumentException.getMessage());

    }

    @Test
    void constructorShouldRejectAvailableValueWithoutMatchingOption() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> createValidRuntimeInfoDTO(
                        List.of("Moving Average", "Chronos"),
                        List.of(option("Moving Average", "community", true))));

        Assertions.assertEquals(
                "RuntimeInfoDTO.availableDemandPlanningForecastModels contains values absent from RuntimeInfoDTO.demandPlanningForecastModelOptions: [Chronos]",
                illegalArgumentException.getMessage());

    }

    @Test
    void constructorShouldRejectOptionMarkedAvailableOutsideAvailableList() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> createValidRuntimeInfoDTO(
                        List.of("Moving Average"),
                        List.of(
                                option("Moving Average", "community", true),
                                option("Chronos", "enterprise", true))));

        Assertions.assertEquals(
                "RuntimeInfoDTO.demandPlanningForecastModelOptions marks values as available but they are absent from RuntimeInfoDTO.availableDemandPlanningForecastModels: [Chronos]",
                illegalArgumentException.getMessage());

    }

    @Test
    void runtimeInfoOptionShouldRejectUnknownRequiredEdition() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> option("Chronos", "unknown-edition", false));

        Assertions.assertEquals(
                "RuntimeInfoDTO.edition must be community or enterprise.",
                illegalArgumentException.getMessage());

    }

    private static RuntimeInfoDTO createValidRuntimeInfoDTO(String edition) {

        return createValidRuntimeInfoDTO(
                edition,
                List.of("Moving Average"));

    }

    private static RuntimeInfoDTO createValidRuntimeInfoDTO(
            List<String> availableDemandPlanningForecastModels) {

        return createValidRuntimeInfoDTO(
                "community",
                availableDemandPlanningForecastModels);

    }

    private static RuntimeInfoDTO createValidRuntimeInfoDTO(
            String edition,
            List<String> availableDemandPlanningForecastModels) {

        return createValidRuntimeInfoDTO(
                edition,
                availableDemandPlanningForecastModels,
                List.of(option("Moving Average", "community", true)));

    }

    private static RuntimeInfoDTO createValidRuntimeInfoDTO(
            List<String> availableDemandPlanningForecastModels,
            List<RuntimeInfoDTO.RuntimeInfoOptionDTO> demandPlanningForecastModelOptions) {

        return createValidRuntimeInfoDTO(
                "community",
                availableDemandPlanningForecastModels,
                demandPlanningForecastModelOptions);

    }

    private static RuntimeInfoDTO createValidRuntimeInfoDTO(
            String edition,
            List<String> availableDemandPlanningForecastModels,
            List<RuntimeInfoDTO.RuntimeInfoOptionDTO> demandPlanningForecastModelOptions) {

        return new RuntimeInfoDTO(
                edition,
                availableDemandPlanningForecastModels,
                demandPlanningForecastModelOptions,
                List.of("Historical Sales"),
                List.of(option("Historical Sales", "community", true)),
                List.of("Inactive"),
                List.of(option("Inactive", "community", true)),
                List.of("Inactive"),
                List.of(option("Inactive", "community", true)),
                List.of("No Uplift Calculation"),
                List.of(option("No Uplift Calculation", "community", true)),
                List.of("Sell-out"),
                List.of(option("Sell-out", "community", true)),
                List.of("Heuristic"),
                List.of(option("Heuristic", "community", true)),
                List.of("Direct Demand", "Historical Sales", "Baseline", "Demand Adjustment"),
                List.of("Direct Demand", "Historical Sales", "Baseline", "Demand Adjustment"),
                List.of("Direct Demand", "Demand Adjustment"),
                List.of(
                        "Total Demand-Working Plan",
                        "Direct Demand-Working Plan",
                        "Direct Demand - Demand Plan-Working Plan",
                        "Indirect Demand-Working Plan",
                        "Safety Stock-Working Plan",
                        "Stock-Working Plan",
                        "Planned Production-Working Plan",
                        "Planned Inbound-Working Plan"),
                List.of(
                        "Total Demand-Working Plan",
                        "Direct Demand-Working Plan",
                        "Direct Demand - Demand Plan-Working Plan",
                        "Indirect Demand-Working Plan",
                        "Safety Stock-Working Plan",
                        "Stock-Working Plan",
                        "Planned Production-Working Plan",
                        "Planned Inbound-Working Plan"),
                List.of(
                        "Stock-Working Plan",
                        "Planned Production-Working Plan",
                        "Planned Inbound-Working Plan"));

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
