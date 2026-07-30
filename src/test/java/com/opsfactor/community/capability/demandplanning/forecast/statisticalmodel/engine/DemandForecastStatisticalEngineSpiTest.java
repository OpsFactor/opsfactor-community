package com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos comuns das engines estatisticas Community.
 */
class DemandForecastStatisticalEngineSpiTest {

    @Test
    void requerDesagregacaoShouldFollowMaterialAndLocationAggregationLevels() {

        DemandForecastStatisticalEngineSpi demandForecastStatisticalEngineSpi =
                new MovingAverageForecastEngine();
        ParametrosForecastProjection parametrosForecastProjection =
                new ParametrosForecastProjection(
                        Constantes.DPModeloEstatistico.MM,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        /*
         * Engines estatisticas respeitam a unidade de execucao preparada pela
         * factory de projection. So quando material e location sao bottom-up a
         * serie ja nasce no menor nivel funcional e a desagregacao vira no-op.
         * Qualquer dimensao top-down gera uma serie agregada que precisa ser
         * aberta ate material/location antes da persistencia.
         */
        Assertions.assertFalse(
                demandForecastStatisticalEngineSpi.requerDesagregacao(
                        parametrosForecastProjection,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.BOTTOM_UP,
                                Constantes.DPNivelAgregacao.BOTTOM_UP)));
        Assertions.assertTrue(
                demandForecastStatisticalEngineSpi.requerDesagregacao(
                        parametrosForecastProjection,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.TOP_DOWN,
                                Constantes.DPNivelAgregacao.BOTTOM_UP)));
        Assertions.assertTrue(
                demandForecastStatisticalEngineSpi.requerDesagregacao(
                        parametrosForecastProjection,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.BOTTOM_UP,
                                Constantes.DPNivelAgregacao.TOP_DOWN)));
        Assertions.assertTrue(
                demandForecastStatisticalEngineSpi.requerDesagregacao(
                        parametrosForecastProjection,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.TOP_DOWN,
                                Constantes.DPNivelAgregacao.TOP_DOWN)));

    }

}
