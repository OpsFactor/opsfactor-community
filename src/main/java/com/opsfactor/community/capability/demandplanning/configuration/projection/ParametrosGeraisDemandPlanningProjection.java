package com.opsfactor.community.capability.demandplanning.configuration.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ForecastInternalRegressorParameters;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParametrosGeraisDemandPlanningProjection {

    public boolean executaPlanoDemanda;

    public UnidadeMedida unidadeMedidaDP;
    public boolean arredondaParaUnidadeVenda;

    public ParametrosAgregacaoForecast parametrosAgregacaoForecast;

    /**
     * Numero de dias historicos considerados para a geracao do forecast.
     *
     * <p>{@code null} no perfil usa o default de ParametrosGlobais. Valor
     * preenchido menor ou igual a zero representa configuracao quebrada e deve
     * falhar antes da factory recortar historico de vendas para a rodada.</p>
     */
    public int diasHistoricosForecastEstatistico;
    public boolean dpUsaHistoricoDemandaInativos;
    public boolean dpGeraForecastParaDescontinuados;

    /**
     * Id escalar do Budget Enterprise selecionado para a unidade de forecast.
     * O Community pode transportar o valor no snapshot compartilhado, mas nao
     * o consulta nem o interpreta durante a execucao aberta.
     */
    public Long budgetId;

    /**
     * Janela funcional de tratamento de material novo.
     *
     * <p>No Community este valor e sempre 0, mesmo quando um perfil transicional
     * no banco ou uma chamada direta do construtor traz valor positivo. A borda
     * de configuracao ja bloqueia payloads novos; esta trava local protege a
     * execucao de dados legados que nao passaram pelo mapper atual.</p>
     */
    private final int numeroDiasProdutoNovo;

    /**
     * Parametros dos regressores calculados internamente pelo Enterprise.
     *
     * <p>O value object pertence ao snapshot compartilhado, pois preserva a
     * mesma tabela e evita um segundo lookup por serie no workflow Enterprise.
     * O Community o transporta sem interpretacao: nenhum consumidor Community
     * ativa esses sinais. A leitura e a materializacao dos sinais pertencem ao
     * workflow Enterprise.</p>
     */
    private ForecastInternalRegressorParameters internalRegressorParameters;

    /*
     * Support series externas, Budget as Forecast e a interpretacao de
     * regressores baseados em STL/dias uteis pertencem ao Enterprise. O
     * Community pode transportar valores persistidos por compatibilidade de
     * tabela, mas seus fluxos nao os leem; payloads Community que tentem
     * configura-los sao bloqueados na borda.
     */

    public ParametrosGeraisDemandPlanningProjection(
            Boolean executaPlanoDemanda,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast,
            Integer diasHistoricosForecastEstatistico,
            Boolean dpUsaHistoricoDemandaInativos,
            Boolean dpGeraForecastParaDescontinuados,
            Integer numeroDiasProdutoNovo,
            UnidadeMedida unidadeMedidaDP,
            Boolean arredondaParaUnidadeVenda,
            ParametrosGlobais parametrosGlobais) {

        this.executaPlanoDemanda = (executaPlanoDemanda == null) ? true : executaPlanoDemanda;
        this.parametrosAgregacaoForecast = parametrosAgregacaoForecast;
        this.diasHistoricosForecastEstatistico =
                getDiasHistoricosForecastEstatistico(
                        diasHistoricosForecastEstatistico,
                        parametrosGlobais);
        this.dpUsaHistoricoDemandaInativos = (dpUsaHistoricoDemandaInativos == null) ? parametrosGlobais.getDpUsaHistoricoDemandaInativos() : dpUsaHistoricoDemandaInativos;
        this.dpGeraForecastParaDescontinuados = (dpGeraForecastParaDescontinuados == null) ? parametrosGlobais.getDpGeraForecastParaDescontinuados() : dpGeraForecastParaDescontinuados;
        this.numeroDiasProdutoNovo = normalizaNumeroDiasProdutoNovoCommunity(
                numeroDiasProdutoNovo,
                parametrosGlobais);
        this.unidadeMedidaDP = (unidadeMedidaDP == null) ? parametrosGlobais.getUnidadeMedidaPadraoDP() : unidadeMedidaDP;
        this.arredondaParaUnidadeVenda = (arredondaParaUnidadeVenda == null) ? false : arredondaParaUnidadeVenda;
        this.internalRegressorParameters = ForecastInternalRegressorParameters.neutral();

    }


    public ParametrosGeraisDemandPlanningProjection(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster,
            ParametrosGlobais parametrosGlobais,
            boolean forcarNivelAgregacaoGeracaoForecastNoCluster) {
    
        this(
                parametrosDemandPlanNivelCluster.getExecutaDp(),
                new ParametrosAgregacaoForecast(
                        (forcarNivelAgregacaoGeracaoForecastNoCluster) ? Constantes.DPNivelAgregacao.TOP_DOWN : parametrosDemandPlanNivelCluster.getLocationAggregationType(),
                        (forcarNivelAgregacaoGeracaoForecastNoCluster) ? Constantes.DPNivelAgregacao.TOP_DOWN : parametrosDemandPlanNivelCluster.getMaterialAggregationType()),
                parametrosDemandPlanNivelCluster.getDiasHistoricosForecastEstatistico(),
                parametrosDemandPlanNivelCluster.getDpUsaHistoricoDemandaInativos(),
                parametrosDemandPlanNivelCluster.getDpGeraForecastParaDescontinuados(),
                parametrosDemandPlanNivelCluster.getNumeroDiasProdutoNovo(),
                parametrosDemandPlanNivelCluster.getUnidadeMedidaPadraoDP(),
                parametrosDemandPlanNivelCluster.getArredondaParaUnidadeVenda(),
                parametrosGlobais);

        this.internalRegressorParameters = new ForecastInternalRegressorParameters(
                parametrosDemandPlanNivelCluster.getIncludeTargetTrendGrowthRegressor(),
                parametrosDemandPlanNivelCluster.getTrendHistoricalWindowInDays(),
                parametrosDemandPlanNivelCluster.getTargetTrendGrowthYearOverYear(),
                parametrosDemandPlanNivelCluster.getIncludeWorkingDaysRegressor());
        this.budgetId = parametrosDemandPlanNivelCluster.getBudgetId();

    }

    private int getDiasHistoricosForecastEstatistico(
            Integer diasHistoricosForecastEstatistico,
            ParametrosGlobais parametrosGlobais) {

        int diasHistoricosForecastEstatisticoResolvido =
                diasHistoricosForecastEstatistico == null
                        ? parametrosGlobais.getDiasHistoricosForecastEstatistico()
                        : diasHistoricosForecastEstatistico;
        if (diasHistoricosForecastEstatisticoResolvido <= 0) {
            throw new IllegalArgumentException(
                    "Dias historicos de forecast estatistico devem ser positivos.");
        }

        return diasHistoricosForecastEstatisticoResolvido;

    }

    /**
     * Neutraliza tratamento de New Materials no Community.
     *
     * <p>O parametro bruto e o objeto de parametros globais aparecem na
     * assinatura para documentar as duas origens historicas possiveis desse
     * valor. Nenhuma delas deve ativar a feature nesta edicao.</p>
     */
    private int normalizaNumeroDiasProdutoNovoCommunity(
            Integer numeroDiasProdutoNovo,
            ParametrosGlobais parametrosGlobais) {

        return 0;

    }

}
