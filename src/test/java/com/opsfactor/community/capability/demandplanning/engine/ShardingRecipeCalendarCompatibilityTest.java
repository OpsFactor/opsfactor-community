package com.opsfactor.community.capability.demandplanning.engine;

import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.demandplanning.configuration.projection.*;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import com.opsfactor.community.platform.utility.Constantes;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/** Valida buckets reais antes de qualquer modelo, sem confundir a janela operacional em dias. */
class ShardingRecipeCalendarCompatibilityTest {

    @Test
    void checksBothEnginesBeforeForecastAndKeepsManualOrAutoFitWithoutFrozenWindowUnchanged() {

        var date = LocalDateTime.of(2026,3,1,0,0);
        var execution = mock(PerfilExecucaoDemandPlan.class);
        when(execution.getTamanhoBucket()).thenReturn(Constantes.TamanhoBucket.MENSAL);
        when(execution.getNumeroDiasHorizontePlanejamento(date)).thenReturn(31);
        var general = mock(ParametrosGeraisDemandPlanningProjection.class);
        general.diasHistoricosForecastEstatistico = 60;
        var cluster = mock(ParametrosDemandPlanNivelClusterProjection.class);
        when(cluster.getParametrosGeraisDemandPlanningProjection()).thenReturn(general);
        int actualPeriods = DemandPlanning.getCalendarioDemandPlanComPeriodosPassadosEFuturosComInputsEmDias(
                Constantes.TamanhoBucket.MENSAL,60,31,date).getNumeroPeriodosPassados();
        for (var engine : List.of(Constantes.DPModeloEstatistico.CHRONOS,Constantes.DPModeloEstatistico.REGRESSAO_SERIE_TEMPORAL)) {
            var parameters = new ParametrosForecastProjection(engine,null,null,null,null,null,null);
            when(cluster.getAllForecastParameters()).thenReturn(List.of(parameters));
            parameters.setNumeroPeriodosHistoricosReceitaSharding(actualPeriods);
            parameters.setTamanhoBucketReceitaSharding(Constantes.TamanhoBucket.MENSAL);
            assertThrows(IllegalArgumentException.class,()->
                    parameters.validarCalendarioReceita(Constantes.TamanhoBucket.SEMANAL,actualPeriods));
            assertEquals(actualPeriods,DemandPlanning.getCalendarioDemandPlanComPeriodosPassadosEFuturos(cluster,execution,date).getNumeroPeriodosPassados());
            parameters.setNumeroPeriodosHistoricosReceitaSharding(actualPeriods+1);
            var failure = assertThrows(IllegalArgumentException.class,()->
                    DemandPlanning.getCalendarioDemandPlanComPeriodosPassadosEFuturos(cluster,execution,date));
            assertTrue(failure.getMessage().contains(String.valueOf(actualPeriods+1)));
            assertTrue(failure.getMessage().contains(String.valueOf(actualPeriods)));
            parameters.setNumeroPeriodosHistoricosReceitaSharding(null);
            parameters.setTamanhoBucketReceitaSharding(null);
            assertDoesNotThrow(()->DemandPlanning.getCalendarioDemandPlanComPeriodosPassadosEFuturos(cluster,execution,date));
        }

    }
}
