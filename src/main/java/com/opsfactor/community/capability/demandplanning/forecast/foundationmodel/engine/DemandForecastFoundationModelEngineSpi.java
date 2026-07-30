package com.opsfactor.community.capability.demandplanning.forecast.foundationmodel.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.forecast.engine.DemandForecastEngineSpi;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.utility.Constantes;

/**
 * Contrato de uma engine de foundation model de Demand Planning.
 *
 * <p>Foundation models, como Chronos, podem quebrar o fluxo estatistico
 * tradicional "executa uma serie agregada e depois desagrega". Dependendo dos
 * parametros do modelo, a mesma engine pode produzir forecast no agregado ou
 * diretamente nas series material/location. Este SPI documenta essa diferenca
 * sem introduzir registry, sem comparar edicao e sem guardar estado de rodada.</p>
 *
 * <p>O Community nao implementa este contrato. Ele fica aqui porque o workflow
 * compartilhado conhece a pergunta comum de toda engine:
 * {@link #requerDesagregacao(ParametrosForecastProjection, ParametrosAgregacaoForecast)}.
 * Implementacoes reais devem ficar no Enterprise e escrever seus resultados
 * diretamente na {@link DemandPlanForecastProjection} recebida.</p>
 */
public interface DemandForecastFoundationModelEngineSpi extends DemandForecastEngineSpi {

    /**
     * Modelo estatistico/foundation model atendido por esta engine.
     */
    Constantes.DPModeloEstatistico getDpModeloEstatistico();

    /**
     * Informa se a execucao do foundation model escreve a saida final nas
     * series material/location.
     *
     * <p>Quando retornar {@code true}, o workflow deve tratar a desagregacao
     * como no-op: a engine ja materializou o menor nivel funcional. Quando
     * retornar {@code false}, a saida autoritativa permanece agregada e o
     * workflow ainda precisa aplicar o split configurado.</p>
     *
     * <p>Se a saida for material/location, a implementacao Enterprise tambem
     * fica responsavel por preencher nos leafs as series historicas tratadas
     * que seriam normalmente propagadas pela desagregacao
     * ({@code vendaHistoricaTratamentoStockouts} e
     * {@code vendaHistoricaTratamentoOutliers}). A persistencia de historico
     * le essas series nos leafs, nao apenas no agregado.</p>
     */
    boolean geraSaidaMaterialLocationDiretamente(
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast);

    /**
     * Foundation models exigem split somente quando sua saida autoritativa
     * permaneceu agregada.
     */
    @Override
    default boolean requerDesagregacao(
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast) {

        return !geraSaidaMaterialLocationDiretamente(
                parametrosForecastProjection,
                parametrosAgregacaoForecast);

    }

}
