package com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosLimpezaHistoricoForecast;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Processor Community de limpeza historica de forecast.
 *
 * <p>Limpeza por outliers, eventos/campanhas e normalizacoes avancadas sao
 * capacidades Enterprise. No Community, a etapa materializa
 * {@code vendaHistoricaTratamentoOutliers} como copia da serie ja tratada por
 * stockouts, mantendo explicita a fronteira entre as etapas.</p>
 *
 * <p>A classe e stateless. Toda informacao da execucao fica na projection
 * recebida e nenhum estado e compartilhado entre clusters paralelos.</p>
 */
@Component
public class DemandForecastHistoryCleaningProcessor {

    /**
     * Materializa a serie historica final consumida pelas engines estatisticas.
     */
    public void processa(
            Calendario calendario,
            DemandPlanForecastProjection demandPlanForecastProjection,
            ParametrosForecastProjection parametrosForecastProjection) {

        validaConfiguracaoLimpezaHistoricaCommunity(parametrosForecastProjection);
        /*
         * A saida desta etapa deve manter a mesma janela historica ja
         * inicializada pela projection. Isso evita que fluxos de simulacao com
         * `demanda` em horizonte total carreguem periodos futuros para a serie
         * final consumida pelas engines estatisticas.
         */
        demandPlanForecastProjection.vendaHistoricaTratamentoOutliers =
                Arrays.copyOf(
                        demandPlanForecastProjection.vendaHistoricaTratamentoStockouts,
                        demandPlanForecastProjection.vendaHistoricaTratamentoOutliers.length);

    }

    /**
     * Bloqueia modelos Enterprise de limpeza historica antes da copia no-op.
     *
     * <p>O Community so materializa {@code vendaHistoricaTratamentoOutliers}
     * como copia da serie pos-stockout. Se uma projection transicional trouxer
     * PERCENTIS ou CAMPANHA, seguir copiando a serie esconderia que a limpeza
     * real nao aconteceu. O Enterprise reabre PERCENTIS e CAMPANHA por
     * processor {@code @Primary}; no Community ambos continuam falhando nesta
     * borda antes da copia no-op.</p>
     */
    private void validaConfiguracaoLimpezaHistoricaCommunity(
            ParametrosForecastProjection parametrosForecastProjection) {

        ParametrosLimpezaHistoricoForecast parametrosLimpezaHistoricoForecast =
                parametrosForecastProjection == null
                        ? null
                        : parametrosForecastProjection.getParametrosLimpezaHistoricoForecast();

        if (parametrosLimpezaHistoricoForecast == null
                || Constantes.DPModeloNormalizacao.DESATIVADO.equals(
                parametrosLimpezaHistoricoForecast.getModeloNormalizacao())) {
            return;
        }

        throw new RequiresEnterpriseVersionException(
                "Demand Planning outlier/campaign history cleaning");

    }

    /**
     * Compatibilidade para testes/rotinas transicionais que ainda chamam a
     * etapa sem parametros. O workflow Spring novo sempre usa a assinatura
     * completa para permitir overlays Enterprise sem estado interno.
     */
    public void processa(DemandPlanForecastProjection demandPlanForecastProjection) {

        processa(null, demandPlanForecastProjection, null);

    }

    /**
     * Compatibilidade para callers que ja passaram a projection de parametros,
     * mas ainda nao precisam do calendario porque o Community continua no-op.
     */
    public void processa(
            DemandPlanForecastProjection demandPlanForecastProjection,
            ParametrosForecastProjection parametrosForecastProjection) {

        processa(null, demandPlanForecastProjection, parametrosForecastProjection);

    }

}
