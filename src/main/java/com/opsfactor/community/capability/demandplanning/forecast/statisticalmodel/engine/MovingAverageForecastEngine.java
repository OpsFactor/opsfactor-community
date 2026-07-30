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
 * Engine Community para Moving Average.
 *
 * <p>Este modelo calcula uma media simples sobre a janela historica anterior ao
 * periodo presente e replica esse valor para todo o horizonte futuro. O metodo
 * nao atualiza a janela com forecasts futuros, preservando o comportamento
 * historico do modelo MM.</p>
 */
public class MovingAverageForecastEngine implements DemandForecastStatisticalEngineSpi {

    @Override
    public Constantes.DPModeloEstatistico getDpModeloEstatistico() {

        return Constantes.DPModeloEstatistico.MM;

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
                calculaForecastMediaMovel(
                        calendario,
                        demandPlanForecastProjection.vendaHistoricaTratamentoOutliers,
                        demandPlanForecastProjection.forecastBaseline,
                        periodosMediaMovel),
                0);

        /*
         * O fit historico do modelo de media movel e representado pelo forecast
         * do primeiro periodo futuro, que e o contrato historico usado pela
         * simulacao/inspecao do Demand Planning.
         */
        Arrays.fill(
                demandPlanForecastProjection.fitModeloHistorico,
                demandPlanForecastProjection.forecastBaseline[calendario.getPosicaoPeriodoPresente()]);

    }

    private double[] calculaForecastMediaMovel(
            Calendario calendario,
            double[] demandaHistorica,
            double[] forecast,
            int periodosMediaMovel) {

        DescriptiveStatistics historicoDemanda = new DescriptiveStatistics();

        historicoDemanda.setWindowSize(periodosMediaMovel);

        /*
         * A janela inicial contem apenas periodos anteriores ao presente. Quando
         * a janela cruza uma posicao futura por configuracao de calendario, o
         * proprio forecast ja calculado e reaproveitado como no legado.
         */
        for (int periodo = Math.max(0, calendario.getPosicaoPeriodoPresente() - periodosMediaMovel);
             periodo < calendario.getPosicaoPeriodoPresente();
             periodo++) {
            if (periodo <= calendario.getPosicaoPeriodoFinalPassado()) {
                historicoDemanda.addValue(demandaHistorica[periodo]);
            } else {
                historicoDemanda.addValue(forecast[periodo]);
            }
        }

        for (int periodo = calendario.getPosicaoPeriodoPresente();
             periodo <= calendario.getPosicaoPeriodoFinalFuturo()
                     && periodo < calendario.getNumeroPeriodosTotais();
             periodo++) {
            if (historicoDemanda.getN() > 0) {
                forecast[periodo] = historicoDemanda.getMean();
            }
        }

        return forecast;

    }

}
