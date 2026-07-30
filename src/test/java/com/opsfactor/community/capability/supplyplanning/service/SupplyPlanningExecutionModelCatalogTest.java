package com.opsfactor.community.capability.supplyplanning.service;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

/**
 * Contrato do catalogo Community de motores de Supply Planning.
 */
class SupplyPlanningExecutionModelCatalogTest {

    @Test
    void communityCatalogShouldExposeOnlyHeuristicExecutionModel() {

        Assertions.assertEquals(
                Set.of(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO),
                SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanCommunity());
        Assertions.assertTrue(
                SupplyPlanningExecutionModelCatalog.isModoExecucaoSupplyPlanCommunity(
                        PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO));
        Assertions.assertFalse(
                SupplyPlanningExecutionModelCatalog.isModoExecucaoSupplyPlanCommunity(
                        PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR));
        Assertions.assertFalse(
                SupplyPlanningExecutionModelCatalog.isModoExecucaoSupplyPlanCommunity(
                        PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN));

    }

    @Test
    void communityExecutionModelSetShouldBeImmutable() {

        Set<PerfilExecucaoSupplyPlan.ModoExecucao> modosExecucaoSupplyPlanCommunity =
                SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanCommunity();

        /*
         * O catalogo funcional alimenta validações de backend e RuntimeInfo. Ele
         * nao pode ser alterado por consumidores de teste, mapper ou service,
         * pois isso vazaria Optimizer/Process Chain para a edicao Community.
         */
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> modosExecucaoSupplyPlanCommunity.add(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR));

    }

    @Test
    void communityOpenApiListShouldBeImmutableAndUseJsonLabels() {

        List<String> modosExecucaoSupplyPlanOpenApiCommunity =
                SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiCommunity();

        Assertions.assertEquals(
                List.of("Heuristic"),
                modosExecucaoSupplyPlanOpenApiCommunity);
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> modosExecucaoSupplyPlanOpenApiCommunity.add("Process Chain"));

    }

    @Test
    void runtimeOptionsShouldExposeEnterpriseExecutionModelsForBlockedUi() {

        List<String> modosExecucaoSupplyPlanOpenApiRuntimeOptions =
                SupplyPlanningExecutionModelCatalog.getModosExecucaoSupplyPlanOpenApiRuntimeOptions();

        Assertions.assertEquals(
                List.of(
                        "Heuristic",
                        "Optimizer",
                        "Process Chain"),
                modosExecucaoSupplyPlanOpenApiRuntimeOptions);
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> modosExecucaoSupplyPlanOpenApiRuntimeOptions.add("AI Optimizer"));

    }

}
