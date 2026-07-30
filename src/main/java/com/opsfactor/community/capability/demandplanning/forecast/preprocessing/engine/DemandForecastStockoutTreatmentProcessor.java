package com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosLimpezaHistoricoForecast;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Processor Community de tratamento de stockout para historico de forecast.
 *
 * <p>Stockout treatment real e capacidade Enterprise. No Community, a etapa
 * continua existindo para preservar o contrato do workflow, mas seu efeito e
 * copiar a venda historica observada em {@code demanda} para
 * {@code vendaHistoricaTratamentoStockouts}.</p>
 *
 * <p>A classe e stateless. Ela nao pode guardar dados da rodada porque a
 * execucao de Demand Planning pode processar clusters em paralelo.</p>
 */
@Component
public class DemandForecastStockoutTreatmentProcessor {

    /**
     * Materializa a serie pos-stockout usada pela proxima etapa do forecast.
     */
    @SuppressWarnings("unused")
    public void processa(
            Calendario calendario,
            DemandPlanForecastProjection demandPlanForecastProjection,
            ParametrosForecastProjection parametrosForecastProjection) {

        processa(
                calendario,
                demandPlanForecastProjection,
                parametrosForecastProjection,
                null);

    }

    /**
     * Materializa a serie pos-stockout usando o contexto pre-carregado da
     * rodada quando um overlay Enterprise o disponibilizar.
     *
     * <p>O Community ignora o contexto e preserva a copia neutra. A assinatura
     * existe para que o workflow continue stateless quando o Enterprise trocar
     * apenas este processor por uma implementacao {@code @Primary}.</p>
     */
    public void processa(
            Calendario calendario,
            DemandPlanForecastProjection demandPlanForecastProjection,
            ParametrosForecastProjection parametrosForecastProjection,
            DemandForecastStockoutContext demandForecastStockoutContext) {

        validaConfiguracaoStockoutCommunity(parametrosForecastProjection);
        validaSeriesStockout(demandPlanForecastProjection);

        /*
         * A projection pode carregar `demanda` com horizonte total quando a
         * simulacao precisa exibir venda observada futura. A serie tratada,
         * porem, continua sendo exclusivamente historica: engines estatisticas
         * nao devem receber periodos futuros como entrada de treinamento.
         */
        demandPlanForecastProjection.vendaHistoricaTratamentoStockouts =
                Arrays.copyOf(
                        demandPlanForecastProjection.demanda,
                        demandPlanForecastProjection.vendaHistoricaTratamentoStockouts.length);

    }

    /**
     * Garante que a etapa Community nao execute um no-op silencioso quando o
     * payload/configuracao transicional pedir tratamento real de stockouts.
     *
     * <p>O front e as APIs Community ja bloqueiam esta opcao, mas o processor
     * tambem fica protegido porque ele pode ser chamado por testes, simulacoes
     * ou fluxos internos que montem {@link ParametrosForecastProjection}
     * diretamente. Sem esta guarda, {@code consideraDadosEstoque=true} copiaria
     * a serie observada e pareceria uma execucao valida, mascarando a ausencia
     * da projection historica de estoque que pertence ao Enterprise.</p>
     */
    private void validaConfiguracaoStockoutCommunity(
            ParametrosForecastProjection parametrosForecastProjection) {

        ParametrosLimpezaHistoricoForecast parametrosLimpezaHistoricoForecast =
                parametrosForecastProjection == null
                        ? null
                        : parametrosForecastProjection.getParametrosLimpezaHistoricoForecast();

        if (parametrosLimpezaHistoricoForecast != null
                && parametrosLimpezaHistoricoForecast.isConsideraDadosEstoque()) {
            throw new RequiresEnterpriseVersionException(
                    "Demand Planning stockout treatment");
        }

    }

    /**
     * Valida as series minimas da etapa Community de stockout.
     *
     * <p>A factory normal cria essas series antes do workflow. Se uma projection
     * incompleta chegar aqui, falhar nesta borda deixa claro que o erro esta no
     * snapshot de forecast, em vez de produzir um `NullPointerException` dentro
     * do `Arrays.copyOf` ou preencher a serie historica com zeros artificiais.</p>
     */
    protected void validaSeriesStockout(
            DemandPlanForecastProjection demandPlanForecastProjection) {

        if (demandPlanForecastProjection == null) {
            throw new IllegalArgumentException(
                    "Demand Plan forecast projection is required for stockout treatment.");
        }
        if (demandPlanForecastProjection.demanda == null) {
            throw new IllegalArgumentException(
                    "Historical demand series is required for stockout treatment.");
        }
        if (demandPlanForecastProjection.vendaHistoricaTratamentoStockouts == null) {
            throw new IllegalArgumentException(
                    "Stockout treatment target series is required.");
        }
        if (demandPlanForecastProjection.demanda.length
                < demandPlanForecastProjection.vendaHistoricaTratamentoStockouts.length) {
            throw new IllegalArgumentException(
                    "Stockout treatment target series cannot be longer than historical demand series.");
        }
        for (int periodo = 0;
             periodo < demandPlanForecastProjection.vendaHistoricaTratamentoStockouts.length;
             periodo++) {
            if (!Double.isFinite(demandPlanForecastProjection.demanda[periodo])) {
                throw new IllegalArgumentException(
                        "Historical demand series must contain only finite values before stockout treatment. Invalid value at period "
                                + periodo
                                + ".");
            }
        }

    }

    /**
     * Compatibilidade para testes/rotinas transicionais que ainda chamam a
     * etapa sem parametros. O workflow Spring novo sempre usa a assinatura
     * completa para permitir overlays Enterprise sem estado interno.
     */
    public void processa(DemandPlanForecastProjection demandPlanForecastProjection) {

        processa(null, demandPlanForecastProjection, null, null);

    }

    /**
     * Compatibilidade para callers que ja passaram a projection de parametros,
     * mas ainda nao precisam do calendario porque o Community continua no-op.
     */
    public void processa(
            DemandPlanForecastProjection demandPlanForecastProjection,
            ParametrosForecastProjection parametrosForecastProjection) {

        processa(null, demandPlanForecastProjection, parametrosForecastProjection, null);

    }

}
