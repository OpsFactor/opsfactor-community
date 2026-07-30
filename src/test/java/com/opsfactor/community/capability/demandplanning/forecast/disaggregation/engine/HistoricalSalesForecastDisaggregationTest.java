package com.opsfactor.community.capability.demandplanning.forecast.disaggregation.engine;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionAgregado;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contratos do split Community por historico de vendas.
 *
 * <p>O split e Community, mas tambem e reutilizado por workflows Enterprise
 * compostos, como HTS depois da reconciliacao MAPE. Por isso ele precisa
 * propagar todas as series auxiliares produzidas pela engine agregada, inclusive
 * os componentes historicos especificos de STL.</p>
 */
class HistoricalSalesForecastDisaggregationTest {

    @Test
    void shouldPropagateStlHistoricalSeasonalSeriesAlongWithTrend() {

        Calendario calendario = getCalendarioTeste();
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                getDemandPlanForecastProjectionAgregadoComDoisLeafs(calendario);
        DemandPlanForecastProjectionMaterialLocation primeiraDemandPlanForecastProjectionMaterialLocation =
                demandPlanForecastProjectionAgregado
                        .getDemandPlanForecastProjectionMaterialLocationList()
                        .get(0);
        DemandPlanForecastProjectionMaterialLocation segundaDemandPlanForecastProjectionMaterialLocation =
                demandPlanForecastProjectionAgregado
                        .getDemandPlanForecastProjectionMaterialLocationList()
                        .get(1);
        demandPlanForecastProjectionAgregado.trendStlHistorico =
                new double[]{100.0d, 200.0d, 300.0d};
        demandPlanForecastProjectionAgregado.seasonalStlHistorico =
                new double[]{10.0d, 20.0d, 30.0d};

        new HistoricalSalesForecastDisaggregation().desagregaForecast(
                calendario,
                30,
                demandPlanForecastProjectionAgregado,
                new ClusterEParametrosProjectionComTodasDfusAtivas());

        Assertions.assertArrayEquals(
                new double[]{62.5d, 125.0d, 187.5d},
                primeiraDemandPlanForecastProjectionMaterialLocation.trendStlHistorico,
                0.0001d);
        Assertions.assertArrayEquals(
                new double[]{6.25d, 12.5d, 18.75d},
                primeiraDemandPlanForecastProjectionMaterialLocation.seasonalStlHistorico,
                0.0001d);
        Assertions.assertArrayEquals(
                new double[]{37.5d, 75.0d, 112.5d},
                segundaDemandPlanForecastProjectionMaterialLocation.trendStlHistorico,
                0.0001d);
        Assertions.assertArrayEquals(
                new double[]{3.75d, 7.5d, 11.25d},
                segundaDemandPlanForecastProjectionMaterialLocation.seasonalStlHistorico,
                0.0001d);

    }

    @Test
    void shouldLeaveLeafSeriesUntouchedWhenAllDfusAreInactive() {

        Calendario calendario = getCalendarioTeste();
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                getDemandPlanForecastProjectionAgregadoComDoisLeafs(calendario);
        DemandPlanForecastProjectionMaterialLocation primeiraDemandPlanForecastProjectionMaterialLocation =
                demandPlanForecastProjectionAgregado
                        .getDemandPlanForecastProjectionMaterialLocationList()
                        .get(0);
        DemandPlanForecastProjectionMaterialLocation segundaDemandPlanForecastProjectionMaterialLocation =
                demandPlanForecastProjectionAgregado
                        .getDemandPlanForecastProjectionMaterialLocationList()
                        .get(1);
        demandPlanForecastProjectionAgregado.trend =
                new double[]{100.0d, 200.0d, 300.0d, 400.0d, 500.0d};

        new HistoricalSalesForecastDisaggregation().desagregaForecast(
                calendario,
                30,
                demandPlanForecastProjectionAgregado,
                new ClusterEParametrosProjectionSemDfusAtivas());

        /*
         * Sem DFUs ativas, o split nao deve criar participacao uniforme falsa.
         * As series dos leafs permanecem no estado inicial da projection e as
         * series opcionais do agregado nao sao materializadas nos filhos.
         */
        Assertions.assertArrayEquals(
                new double[calendario.getNumeroPeriodosTotais()],
                primeiraDemandPlanForecastProjectionMaterialLocation.forecastBaseline,
                0.0001d);
        Assertions.assertArrayEquals(
                new double[calendario.getNumeroPeriodosTotais()],
                segundaDemandPlanForecastProjectionMaterialLocation.forecastBaseline,
                0.0001d);
        Assertions.assertNull(primeiraDemandPlanForecastProjectionMaterialLocation.trend);
        Assertions.assertNull(segundaDemandPlanForecastProjectionMaterialLocation.trend);

    }

    private static Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);

    }

    private static DemandPlanForecastProjectionAgregado getDemandPlanForecastProjectionAgregadoComDoisLeafs(
            Calendario calendario) {

        DemandPlanForecastProjectionMaterialLocation primeiraDemandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        "MATERIAL_1",
                        new double[]{10.0d, 20.0d, 30.0d});
        DemandPlanForecastProjectionMaterialLocation segundaDemandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        "MATERIAL_2",
                        new double[]{30.0d, 20.0d, 10.0d});
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                new DemandPlanForecastProjectionAgregado(
                        calendario,
                        new UnidadeMedida("UN"),
                        false);
        demandPlanForecastProjectionAgregado
                .getDemandPlanForecastProjectionDesagregados()
                .add(primeiraDemandPlanForecastProjectionMaterialLocation);
        demandPlanForecastProjectionAgregado
                .getDemandPlanForecastProjectionDesagregados()
                .add(segundaDemandPlanForecastProjectionMaterialLocation);
        return demandPlanForecastProjectionAgregado;

    }

    private static DemandPlanForecastProjectionMaterialLocation getDemandPlanForecastProjectionMaterialLocation(
            Calendario calendario,
            String materialId,
            double[] demandaHistorica) {

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto(materialId),
                        false);
        System.arraycopy(
                demandaHistorica,
                0,
                demandPlanForecastProjectionMaterialLocation.demanda,
                0,
                demandaHistorica.length);
        return demandPlanForecastProjectionMaterialLocation;

    }

    private static class ClusterEParametrosProjectionComTodasDfusAtivas extends ClusterEParametrosProjection {

        @Override
        public boolean isDfuAtiva(
                Produto material,
                Location location) {

            return true;

        }

    }

    private static class ClusterEParametrosProjectionSemDfusAtivas extends ClusterEParametrosProjection {

        @Override
        public boolean isDfuAtiva(
                Produto material,
                Location location) {

            return false;

        }

    }

}
