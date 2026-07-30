package com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosMediaMovel;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Arrays;

/**
 * Engine Community para Rolling Moving Average.
 *
 * <p>Diferente da media movel simples, a janela FIFO passa a consumir tambem o
 * forecast calculado em periodos futuros. Isso cria uma propagacao rolling ao
 * longo do horizonte.</p>
 */
public class RollingMovingAverageForecastEngine implements DemandForecastStatisticalEngineSpi {

    @Override
    public Constantes.DPModeloEstatistico getDpModeloEstatistico() {

        return Constantes.DPModeloEstatistico.RMM;

    }

    @Override
    public void executaForecast(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            DemandPlanForecastProjection demandPlanForecastProjection) {

        ParametrosMediaMovel parametrosMediaMovel =
                parametrosForecastProjection.getParametrosMediaMovel();
        int periodosMediaMovel = (int) Math.ceil(
                calendario.converteDiasParaPeriodosCalendario(
                        parametrosMediaMovel.getDiasHistoricosMediaMovel()));

        demandPlanForecastProjection.forecastBaseline = MetodosUtilidade.atualizaArrayComValorMinimo(
                calculaForecastMediaMovelRolling(
                        calendario,
                        demandPlanForecastProjection.vendaHistoricaTratamentoOutliers,
                        demandPlanForecastProjection.forecastBaseline,
                        periodosMediaMovel),
                0);

        Arrays.fill(
                demandPlanForecastProjection.fitModeloHistorico,
                demandPlanForecastProjection.forecastBaseline[calendario.getPosicaoPeriodoPresente()]);

    }

    private double[] calculaForecastMediaMovelRolling(
            Calendario calendario,
            double[] demandaHistorica,
            double[] forecast,
            int periodosMediaMovel) {

        DescriptiveStatistics historicoDemanda = new DescriptiveStatistics();

        historicoDemanda.setWindowSize(periodosMediaMovel);

        /*
         * O loop unico preserva o contrato rolling: antes do presente ele
         * carrega historico observado; a partir do presente ele primeiro calcula
         * o forecast e depois coloca esse forecast na propria janela.
         */
        for (int periodo = Math.max(0, calendario.getPosicaoPeriodoPresente() - periodosMediaMovel);
             periodo <= calendario.getPosicaoPeriodoFinalFuturo();
             periodo++) {
            if (periodo >= calendario.getPosicaoPeriodoPresente() && historicoDemanda.getN() > 0) {
                forecast[periodo] = historicoDemanda.getMean();
            }

            if (periodo <= calendario.getPosicaoPeriodoFinalPassado()) {
                historicoDemanda.addValue(demandaHistorica[periodo]);
            } else {
                historicoDemanda.addValue(forecast[periodo]);
            }
        }

        return forecast;

    }

}
