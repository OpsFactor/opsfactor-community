package com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine;

import com.opsfactor.community.platform.rinstance.InstanciaRCaller;
import com.opsfactor.community.platform.rinstance.model.ResultadoForecastEstatistico;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;

/**
 * Engine Community para Exponential Smoothing.
 *
 * <p>O modelo aberto usa somente a serie historica ja preparada pelo workflow.
 * ETS, Prophet, Chronos, regressores e support series permanecem no Enterprise.
 * A engine nao guarda estado da rodada em atributos, preservando a execucao
 * paralela do Demand Planning service.</p>
 */
public class ExponentialSmoothingForecastEngine implements DemandForecastStatisticalEngineSpi {

    @Override
    public Constantes.DPModeloEstatistico getDpModeloEstatistico() {

        return Constantes.DPModeloEstatistico.ES;

    }

    @Override
    public void executaForecast(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            DemandPlanForecastProjection demandPlanForecastProjection) {

        ResultadoForecastEstatistico resultadoForecastEstatistico = InstanciaRCaller.geraForecastExponentialSmoothing(
                demandPlanForecastProjection.vendaHistoricaTratamentoOutliers,
                calendario);

        demandPlanForecastProjection.forecastBaseline = MetodosUtilidade.atualizaArrayComValorMinimo(
                resultadoForecastEstatistico.forecast,
                0);
        demandPlanForecastProjection.lowerBound = resultadoForecastEstatistico.lowerBound;
        demandPlanForecastProjection.upperBound = resultadoForecastEstatistico.upperBound;

    }

}
