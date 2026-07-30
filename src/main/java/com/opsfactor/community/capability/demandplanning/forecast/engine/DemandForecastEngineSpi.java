package com.opsfactor.community.capability.demandplanning.forecast.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.calendar.Calendario;

/**
 * Contrato comum de uma engine de forecast de Demand Planning.
 *
 * <p>Este SPI e propositalmente pequeno. Ele nao registra engines, nao escolhe
 * implementacao e nao carrega estado da rodada. O objetivo e apenas documentar
 * o que qualquer engine precisa responder: se a saida exige desagregacao
 * posterior e como a projection recebida deve ser populada.</p>
 *
 * <p>No workflow Community atual, a decisao operacional de desagregar ainda
 * vem da projection criada pela factory: projection agregada desagrega,
 * projection material/location nao desagrega. O metodo
 * {@link #requerDesagregacao(ParametrosForecastProjection, ParametrosAgregacaoForecast)}
 * documenta o contrato que o overlay Enterprise deve usar quando introduzir
 * engines que possam gerar saida agregada ou desagregada independentemente da
 * forma inicial da projection.</p>
 *
 * <p>No Community, as implementacoes atuais sao estatisticas. No Enterprise, o
 * mesmo contrato pode ser especializado por foundation models, como Chronos,
 * quando houver implementacao real no overlay privado.</p>
 */
public interface DemandForecastEngineSpi {

    /**
     * Indica se a forecast gerada pela engine precisa ser aberta novamente para
     * material/location.
     *
     * <p>Modelos estatisticos top-down normalmente retornam {@code true}.
     * Foundation models Enterprise podem retornar {@code false} quando a saida
     * configurada ja for material/location.</p>
     */
    boolean requerDesagregacao(
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast);

    /**
     * Executa o forecast e escreve o resultado diretamente na projection de
     * execucao recebida.
     */
    void executaForecast(
            Calendario calendario,
            ParametrosForecastProjection parametrosForecastProjection,
            DemandPlanForecastProjection demandPlanForecastProjection);

}
