package com.opsfactor.community.capability.demandplanning.demandplan.domain;

import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemandPlanCalendarioTest {

    @Test
    void deveIncluirPeriodoFinalDoPlanoQuandoUsaHistoricoFixoNoPlanningBook() {
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("PERFIL_TESTE");
        perfilExecucaoDemandPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        perfilExecucaoDemandPlan.setNumeroPeriodosHorizontePlanejamento(4);

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setPerfilExecucaoDemandPlan(perfilExecucaoDemandPlan);
        demandPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        demandPlan.setDataInicioPlano(LocalDateTime.of(2026, 5, 1, 0, 0));
        demandPlan.setDataFimPlano(LocalDateTime.of(2026, 8, 31, 23, 59, 59));

        Calendario calendario = demandPlan.getCalendarioDoDemandPlanComNumeroPeriodosHistoricosFixo(
                perfilExecucaoDemandPlan,
                4);

        assertEquals(8, calendario.getNumeroPeriodosTotais());
        assertEquals(4, calendario.getNumeroPeriodosPassados());
        assertEquals(4, calendario.getNumeroPeriodosFuturos());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), calendario.getDataHorarioInicial());
        assertEquals(LocalDateTime.of(2026, 5, 1, 0, 0), calendario.getDataHorarioInicialPresente());
        assertEquals(LocalDateTime.of(2026, 8, 31, 23, 59, 59), calendario.getDataHorarioFinalFutura());
    }
}
