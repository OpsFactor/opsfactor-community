package com.opsfactor.community.capability.demandplanning.forecast.disaggregation.engine;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionAgregado;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;

/**
 * Contrato de uma estrategia de desagregacao de forecast agregado para DFU.
 *
 * <p>Uma desagregacao consome o forecast ja escrito no agregado principal e
 * materializa as series finais nas projections material/location. Algumas
 * implementacoes Enterprise poderao ser compostas e executar forecasts
 * auxiliares internos antes de reconciliar os resultados, mas o contrato de
 * entrada continua sendo o mesmo agregado da unidade de execucao.</p>
 *
 * <p>Implementacoes devem ser stateless. Toda informacao da rodada deve ficar
 * nos parametros recebidos ou nas projections mutadas pela propria chamada.</p>
 */
public interface DemandForecastDisaggregationSpi {

    /**
     * Modelo de split atendido por esta estrategia.
     */
    Constantes.DPModeloSplit getDpModeloSplit();

    /**
     * Desagrega o forecast do agregado recebido para as series material/location.
     */
    void desagregaForecast(
            Calendario calendario,
            int numeroDiasSplitTopDown,
            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado,
            ClusterEParametrosProjection clusterEParametrosProjection);

}
