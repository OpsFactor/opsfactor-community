package com.opsfactor.community.capability.supplyplanning.configuration.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Contrato das pontes escalares do perfil Supply compartilhado.
 */
class PerfilExecucaoSupplyPlanEnterpriseBridgeContractTest {

    @Test
    void logisticsCostCurvesShouldUseOnlyTheScalarIdentifier() {

        PerfilExecucaoSupplyPlan executionProfile = new PerfilExecucaoSupplyPlan();
        executionProfile.setCurvasCustoLogisticoId(77L);

        Assertions.assertEquals(77L, executionProfile.getCurvasCustoLogisticoId());
        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("curvasCustoLogistico")));
        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("getCurvasCustoLogistico")
                        || method.getName().equals("setCurvasCustoLogistico")));

    }

    @Test
    void prioritizationModelsShouldUseOnlyScalarIdentifiers() {

        PerfilExecucaoSupplyPlan executionProfile = new PerfilExecucaoSupplyPlan();
        executionProfile.setModeloPriorizacaoDemandaId("DEMAND_PRIORITY");
        executionProfile.setModeloPriorizacaoSafetyStockId("SAFETY_PRIORITY");

        Assertions.assertEquals("DEMAND_PRIORITY", executionProfile.getModeloPriorizacaoDemandaId());
        Assertions.assertEquals("SAFETY_PRIORITY", executionProfile.getModeloPriorizacaoSafetyStockId());
        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("modeloPriorizacaoDemanda")
                        || field.getName().equals("modeloPriorizacaoSafetyStock")));
        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("getModeloPriorizacaoDemandaOptional")
                        || method.getName().equals("setModeloPriorizacaoDemanda")
                        || method.getName().equals("getModeloPriorizacaoSafetyStockOptional")
                        || method.getName().equals("setModeloPriorizacaoSafetyStock")));

    }

    @Test
    void optimizationModelTypeShouldUseOnlyTheStringContract() throws NoSuchFieldException, NoSuchMethodException {

        PerfilExecucaoSupplyPlan executionProfile = new PerfilExecucaoSupplyPlan();
        executionProfile.setTipoModeloOtimizacao("MIP");
        Field optimizationModelTypeField = PerfilExecucaoSupplyPlan.class
                .getDeclaredField("tipoModeloOtimizacao");

        Assertions.assertEquals("MIP", executionProfile.getTipoModeloOtimizacao());
        Assertions.assertEquals(String.class, optimizationModelTypeField.getType());
        Assertions.assertEquals(
                String.class,
                PerfilExecucaoSupplyPlan.class
                        .getDeclaredMethod("getTipoModeloOtimizacao")
                        .getReturnType());
        Assertions.assertEquals(
                String.class,
                PerfilExecucaoSupplyPlan.class
                        .getDeclaredMethod("setTipoModeloOtimizacao", String.class)
                        .getParameterTypes()[0]);
        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("setTipoModeloOtimizacao")
                        && Arrays.asList(method.getParameterTypes()).contains(Object.class)));

    }

    @Test
    void materialFilterShouldUseOnlyTheScalarIdentifier() {

        PerfilExecucaoSupplyPlan executionProfile = new PerfilExecucaoSupplyPlan();
        executionProfile.setMaterialFilterId("MATERIAL_FILTER");

        Assertions.assertEquals("MATERIAL_FILTER", executionProfile.getMaterialFilterId());
        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("filtroProdutos")));
        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("getFiltroProdutos")
                        || method.getName().equals("setFiltroProdutos")));

    }

    @Test
    void optimizerScalarControlsShouldRemainInTheSharedExecutionProfile() {

        PerfilExecucaoSupplyPlan executionProfile = new PerfilExecucaoSupplyPlan();

        Assertions.assertEquals(0.1, executionProfile.getEntityTabuRatio());
        Assertions.assertEquals(10_000, executionProfile.getAcceptedCountLimit());
        Assertions.assertFalse(executionProfile.getConsiderBudgetForGreenfieldLocationActivation());
        Assertions.assertEquals(0.0, executionProfile.getGreenfieldLocationActivationBudget());
        Assertions.assertTrue(executionProfile.getRoundProductionAndSetupsToDetailedPlanBucket());

        executionProfile.setEntityTabuRatio(0.25);
        executionProfile.setAcceptedCountLimit(500);
        executionProfile.setConsiderBudgetForGreenfieldLocationActivation(true);
        executionProfile.setGreenfieldLocationActivationBudget(-1.0);
        executionProfile.setRoundProductionAndSetupsToDetailedPlanBucket(false);

        Assertions.assertEquals(0.25, executionProfile.getEntityTabuRatio());
        Assertions.assertEquals(500, executionProfile.getAcceptedCountLimit());
        Assertions.assertTrue(executionProfile.getConsiderBudgetForGreenfieldLocationActivation());
        Assertions.assertEquals(0.0, executionProfile.getGreenfieldLocationActivationBudget());
        Assertions.assertFalse(executionProfile.getRoundProductionAndSetupsToDetailedPlanBucket());
        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("consideraBudgetAtivacaoCentrosGreenfield")
                        || field.getName().equals("budgetAtivacaoCentrosGreenfield")
                        || field.getName().equals("arredondaProducaoESetupsParaTamanhoBucketPlanoDetalhado")));

    }

    @Test
    void locationOverridesShouldNotLeaveAGenericTransientMapInTheCommunityProfile() {

        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("mapaPerfilExecucaoSupplyPlanPorLocation")));
        Assertions.assertFalse(Arrays.stream(PerfilExecucaoSupplyPlan.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("getMapaPerfilExecucaoSupplyPlanPorLocation")));

    }

}
