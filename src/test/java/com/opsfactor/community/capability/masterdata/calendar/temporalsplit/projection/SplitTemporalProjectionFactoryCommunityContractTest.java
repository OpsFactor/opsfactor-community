package com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * Contrato Community do split temporal.
 *
 * <p>Curvas temporais configuraveis por DFU pertencem ao Enterprise. A factory
 * Community precisa sempre construir a curva flat implicita, tanto para agregar
 * periodos mais detalhados quanto para desagregar um periodo mais agregado.</p>
 */
public class SplitTemporalProjectionFactoryCommunityContractTest {

    @Test
    public void communitySplitTemporalShouldAggregateDailyValuesIntoWeekBySummingDays() {

        Calendario calendarioDiario = Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 5, 0, 0),
                LocalDateTime.of(2026, 1, 11, 0, 0));
        Calendario calendarioSemanal = Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.SEMANAL,
                LocalDateTime.of(2026, 1, 5, 0, 0),
                LocalDateTime.of(2026, 1, 5, 0, 0));

        SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu =
                new SplitTemporalProjectionFactory().geraSplitTemporalProjectionPorDfu(
                        calendarioDiario,
                        calendarioSemanal);

        double valorSemanal = splitTemporalProjectionPorDfu.getValorNoCalendarioTargetSplitTemporal(
                null,
                null,
                periodoDiario -> periodoDiario + 1.0d,
                0);

        Assertions.assertEquals(28.0d, valorSemanal, 0.0001d);

    }

    @Test
    public void communitySplitTemporalShouldDisaggregateWeekIntoDailyFlatValues() {

        Calendario calendarioSemanal = Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.SEMANAL,
                LocalDateTime.of(2026, 1, 5, 0, 0),
                LocalDateTime.of(2026, 1, 5, 0, 0));
        Calendario calendarioDiario = Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 5, 0, 0),
                LocalDateTime.of(2026, 1, 11, 0, 0));

        SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu =
                new SplitTemporalProjectionFactory().geraSplitTemporalProjectionPorDfu(
                        calendarioSemanal,
                        calendarioDiario);

        for (int periodoDiario = 0; periodoDiario < calendarioDiario.getNumeroPeriodosTotais(); periodoDiario++) {
            double valorDiario = splitTemporalProjectionPorDfu.getValorNoCalendarioTargetSplitTemporal(
                    null,
                    null,
                    periodoSemanal -> 70.0d,
                    periodoDiario);

            Assertions.assertEquals(
                    10.0d,
                    valorDiario,
                    0.0001d,
                    "A curva flat Community deve distribuir a semana igualmente por dia.");
        }

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static class TestClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        private TestClusterEParametrosProjectionFactory(ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return clusterEParametrosProjection;

        }

    }

}
