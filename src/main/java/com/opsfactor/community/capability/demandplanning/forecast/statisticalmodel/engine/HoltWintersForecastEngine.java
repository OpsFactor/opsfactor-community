package com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine;

import com.opsfactor.community.platform.rinstance.InstanciaRCaller;
import com.opsfactor.community.platform.rinstance.model.ResultadoForecastEstatistico;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosHoltWinters;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;

/**
 * Engine Community para Holt-Winters via runtime R compartilhado.
 *
 * <p>Esta implementacao executa apenas Holt-Winters estatistico basico sobre
 * {@code vendaHistoricaTratamentoOutliers}. Support series, tratamento real de
 * outliers/eventos, uplift e regressores externos pertencem ao Enterprise e nao
 * entram nesta engine. A classe permanece stateless para ser usada em rodadas
 * paralelas por cluster.</p>
 */
public class HoltWintersForecastEngine implements DemandForecastStatisticalEngineSpi {

    @Override
    public Constantes.DPModeloEstatistico getDpModeloEstatistico() {

        return Constantes.DPModeloEstatistico.HOLT_WINTERS;

    }

    @Override
    public void executaForecast(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            DemandPlanForecastProjection demandPlanForecastProjection) {

        ParametrosHoltWinters parametrosHoltWinters =
                parametrosForecastProjection.getParametrosHoltWinters();

        ResultadoForecastEstatistico resultadoForecastEstatistico = InstanciaRCaller.geraForecastHoltWinters(
                demandPlanForecastProjection.vendaHistoricaTratamentoOutliers,
                calendario,
                parametrosHoltWinters.getAlfa(),
                parametrosHoltWinters.getBeta(),
                parametrosHoltWinters.getGama());

        demandPlanForecastProjection.forecastBaseline = MetodosUtilidade.atualizaArrayComValorMinimo(
                resultadoForecastEstatistico.forecast,
                0);
        demandPlanForecastProjection.fitModeloHistorico = resultadoForecastEstatistico.fitHistorico;
        demandPlanForecastProjection.trend = resultadoForecastEstatistico.trend;
        demandPlanForecastProjection.seasonal = resultadoForecastEstatistico.seasonal;
        demandPlanForecastProjection.lowerBound = resultadoForecastEstatistico.lowerBound;
        demandPlanForecastProjection.upperBound = resultadoForecastEstatistico.upperBound;

    }

}
