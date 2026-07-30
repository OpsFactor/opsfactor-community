package com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosMediaMovel;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contratos diretos das engines Community de media movel.
 *
 * <p>Essas engines sao chamadas por um workflow maior, mas a validacao local
 * protege testes, simulacoes e overlays que instanciem a engine diretamente. A
 * classe cobre tanto a formula basica quanto snapshots transicionais
 * corrompidos que poderiam virar forecast aparentemente valido.</p>
 */
class MovingAverageForecastEnginesTest {

    @Test
    void movingAverageShouldKeepSimpleAverageAcrossFutureHorizon() {

        Calendario calendario = getCalendarioTeste();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(calendario);

        new MovingAverageForecastEngine().executaForecast(
                calendario,
                getParametrosForecastProjection(new ParametrosMediaMovel(2)),
                demandPlanForecastProjectionMaterialLocation);

        Assertions.assertArrayEquals(
                new double[]{0.0d, 0.0d, 0.0d, 25.0d, 25.0d},
                demandPlanForecastProjectionMaterialLocation.forecastBaseline,
                0.0001d);
        Assertions.assertArrayEquals(
                new double[]{25.0d, 25.0d, 25.0d},
                demandPlanForecastProjectionMaterialLocation.fitModeloHistorico,
                0.0001d);

    }

    @Test
    void rollingMovingAverageShouldFeedFutureForecastBackIntoWindow() {

        Calendario calendario = getCalendarioTeste();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(calendario);

        new RollingMovingAverageForecastEngine().executaForecast(
                calendario,
                getParametrosForecastProjection(new ParametrosMediaMovel(2)),
                demandPlanForecastProjectionMaterialLocation);

        Assertions.assertArrayEquals(
                new double[]{0.0d, 0.0d, 0.0d, 25.0d, 27.5d},
                demandPlanForecastProjectionMaterialLocation.forecastBaseline,
                0.0001d);
        Assertions.assertArrayEquals(
                new double[]{25.0d, 25.0d, 25.0d},
                demandPlanForecastProjectionMaterialLocation.fitModeloHistorico,
                0.0001d);

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

    private static DemandPlanForecastProjectionMaterialLocation getDemandPlanForecastProjectionMaterialLocation(
            Calendario calendario) {

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[0] = 10.0d;
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[1] = 20.0d;
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[2] = 30.0d;
        return demandPlanForecastProjectionMaterialLocation;

    }

    private static ParametrosForecastProjection getParametrosForecastProjection(
            ParametrosMediaMovel parametrosMediaMovel) {

        return new ParametrosForecastProjection(
                Constantes.DPModeloEstatistico.MM,
                parametrosMediaMovel,
                null,
                null,
                null,
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                30);

    }

}
