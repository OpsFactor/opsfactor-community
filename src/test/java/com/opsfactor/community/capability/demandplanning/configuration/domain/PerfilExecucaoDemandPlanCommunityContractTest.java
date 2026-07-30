package com.opsfactor.community.capability.demandplanning.configuration.domain;

import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Congela contratos de dominio do perfil de execucao Demand Planning que sao
 * compartilhados por Community e pelos overlays Enterprise.
 *
 * <p>Os services de borda ja bloqueiam payloads novos invalidos. Estes testes
 * garantem que a entidade tambem nao mascara snapshots antigos, cargas diretas
 * ou chamadas internas que consultem o dominio sem passar pelo front service.</p>
 */
public class PerfilExecucaoDemandPlanCommunityContractTest {

    @Test
    public void planningHorizonShouldDefaultToOnePeriodWhenUnset() {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                new PerfilExecucaoDemandPlan("PERFIL_PADRAO");

        Assertions.assertEquals(
                1,
                perfilExecucaoDemandPlan.getNumeroPeriodosHorizontePlanejamento());

    }

    @Test
    public void planningHorizonShouldUsePositiveConfiguredValue() {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                new PerfilExecucaoDemandPlan("PERFIL_PADRAO");
        perfilExecucaoDemandPlan.setNumeroPeriodosHorizontePlanejamento(6);

        Assertions.assertEquals(
                6,
                perfilExecucaoDemandPlan.getNumeroPeriodosHorizontePlanejamento());

    }

    @Test
    public void planningHorizonShouldRejectNonPositiveConfiguredValue() {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                new PerfilExecucaoDemandPlan("PERFIL_PADRAO");
        perfilExecucaoDemandPlan.setNumeroPeriodosHorizontePlanejamento(0);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                perfilExecucaoDemandPlan::getNumeroPeriodosHorizontePlanejamento);

        Assertions.assertEquals(
                "Demand Planning execution profile planning horizon in periods must be positive when explicitly configured: 0.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void planningHorizonInDaysShouldRejectNonPositiveConfiguredValue() {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                new PerfilExecucaoDemandPlan("PERFIL_PADRAO");
        perfilExecucaoDemandPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        perfilExecucaoDemandPlan.setNumeroPeriodosHorizontePlanejamento(-1);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> perfilExecucaoDemandPlan.getNumeroDiasHorizontePlanejamento(
                        LocalDateTime.of(2026, 7, 1, 0, 0)));

        Assertions.assertEquals(
                "Demand Planning execution profile planning horizon in periods must be positive when explicitly configured: -1.",
                illegalArgumentException.getMessage());

    }

}
