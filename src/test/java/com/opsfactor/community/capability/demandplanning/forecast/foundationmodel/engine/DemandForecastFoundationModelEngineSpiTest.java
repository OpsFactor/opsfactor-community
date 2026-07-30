package com.opsfactor.community.capability.demandplanning.forecast.foundationmodel.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato base para foundation models futuros.
 *
 * <p>O teste usa uma implementacao fake local, sem registrar bean Spring e sem
 * abrir Chronos no Community. A protecao importante e semantica: saida
 * material/location torna a desagregacao no-op; saida agregada exige split.</p>
 */
class DemandForecastFoundationModelEngineSpiTest {

    @Test
    void requerDesagregacaoShouldFollowFoundationModelOutputLevel() {

        ParametrosForecastProjection parametrosForecastProjection =
                new ParametrosForecastProjection(
                        Constantes.DPModeloEstatistico.CHRONOS,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Constantes.DPModeloSplit.HISTORICAL_SALES,
                        90);
        ParametrosAgregacaoForecast parametrosAgregacaoForecast =
                new ParametrosAgregacaoForecast(
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.TOP_DOWN);

        DemandForecastFoundationModelEngineSpi engineSaidaMaterialLocation =
                new FakeFoundationModelForecastEngine(true);
        DemandForecastFoundationModelEngineSpi engineSaidaAgregada =
                new FakeFoundationModelForecastEngine(false);

        Assertions.assertFalse(
                engineSaidaMaterialLocation.requerDesagregacao(
                        parametrosForecastProjection,
                        parametrosAgregacaoForecast));
        Assertions.assertTrue(
                engineSaidaAgregada.requerDesagregacao(
                        parametrosForecastProjection,
                        parametrosAgregacaoForecast));

    }

    private static class FakeFoundationModelForecastEngine implements DemandForecastFoundationModelEngineSpi {

        private final boolean geraSaidaMaterialLocationDiretamente;

        private FakeFoundationModelForecastEngine(boolean geraSaidaMaterialLocationDiretamente) {

            this.geraSaidaMaterialLocationDiretamente = geraSaidaMaterialLocationDiretamente;

        }

        @Override
        public Constantes.DPModeloEstatistico getDpModeloEstatistico() {

            return Constantes.DPModeloEstatistico.CHRONOS;

        }

        @Override
        public boolean geraSaidaMaterialLocationDiretamente(
                ParametrosForecastProjection parametrosForecastProjection,
                ParametrosAgregacaoForecast parametrosAgregacaoForecast) {

            return geraSaidaMaterialLocationDiretamente;

        }

        @Override
        public void executaForecast(
                Calendario calendario,
                ParametrosForecastProjection parametrosForecastProjection,
                DemandPlanForecastProjection demandPlanForecastProjection) {

            throw new UnsupportedOperationException("Teste de contrato nao executa foundation model.");

        }

    }

}
