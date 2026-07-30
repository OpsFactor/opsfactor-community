package com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.forecast.engine.DemandForecastEngineSpi;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.platform.utility.Constantes;

/**
 * Contrato de uma execucao atomica de modelo estatistico de Demand Planning.
 *
 * <p>A engine recebe uma {@link DemandPlanForecastProjection} ja preparada pelo
 * workflow, le a serie historica tratada em
 * {@code vendaHistoricaTratamentoOutliers} e escreve diretamente as series de
 * forecast na propria projection. Nao ha objeto de resultado intermediario
 * porque a projection e o objeto de trabalho compartilhado pelas etapas
 * seguintes de desagregacao e persistencia.</p>
 *
 * <p>Implementacoes devem ser stateless. O service de Demand Planning processa
 * clusters em paralelo, entao nenhum dado da rodada pode ficar guardado em
 * atributos da engine.</p>
 */
public interface DemandForecastStatisticalEngineSpi extends DemandForecastEngineSpi {

    /**
     * Modelo estatistico atendido por esta engine.
     */
    Constantes.DPModeloEstatistico getDpModeloEstatistico();

    /**
     * Modelos estatisticos seguem a configuracao top-down/bottom-up de material
     * e location. Se qualquer dimensao roda top-down, a serie agregada precisa
     * ser desagregada depois da execucao do modelo.
     */
    @Override
    default boolean requerDesagregacao(
            ParametrosForecastProjection parametrosForecastProjection,
            ParametrosAgregacaoForecast parametrosAgregacaoForecast) {

        return parametrosAgregacaoForecast.isQualquerDimensaoTopDown();

    }

}
