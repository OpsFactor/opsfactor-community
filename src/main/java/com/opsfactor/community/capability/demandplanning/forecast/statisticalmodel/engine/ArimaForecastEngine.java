package com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine;

import com.opsfactor.community.platform.rinstance.InstanciaRCaller;
import com.opsfactor.community.platform.rinstance.model.ResultadoForecastEstatistico;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;

/**
 * Engine Community para Auto ARIMA.
 *
 * <p>O Community executa ARIMA sem regressores externos, trend target, support
 * series ou features de eventos. Esses complementos ficam para engines
 * Enterprise, preservando esta implementacao como o ARIMA estatistico basico.</p>
 *
 * <p>A classe nao guarda estado em atributos. O historico tratado e o resultado
 * de forecast ficam sempre na {@link DemandPlanForecastProjection} recebida,
 * mantendo a execucao paralela por cluster segura.</p>
 */
public class ArimaForecastEngine implements DemandForecastStatisticalEngineSpi {

    @Override
    public Constantes.DPModeloEstatistico getDpModeloEstatistico() {

        return Constantes.DPModeloEstatistico.ARIMA;

    }

    @Override
    public void executaForecast(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            DemandPlanForecastProjection demandPlanForecastProjection) {

        ResultadoForecastEstatistico resultadoForecastEstatistico = InstanciaRCaller.geraForecastAutoArima(
                demandPlanForecastProjection.vendaHistoricaTratamentoOutliers,
                calendario);

        demandPlanForecastProjection.forecastBaseline = MetodosUtilidade.atualizaArrayComValorMinimo(
                resultadoForecastEstatistico.forecast,
                0);
        demandPlanForecastProjection.fitModeloHistorico = resultadoForecastEstatistico.fitHistorico;
        demandPlanForecastProjection.lowerBound = resultadoForecastEstatistico.lowerBound;
        demandPlanForecastProjection.upperBound = resultadoForecastEstatistico.upperBound;

    }

}
