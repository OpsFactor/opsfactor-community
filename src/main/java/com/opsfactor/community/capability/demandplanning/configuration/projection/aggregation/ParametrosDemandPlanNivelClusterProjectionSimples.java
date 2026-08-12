package com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosGeraisDemandPlanningProjection;
import lombok.Getter;

/**
 * Implementacao Community da projection de parametros por cluster.
 *
 * <p>Ela representa o caso manual e uniforme: todas as DFUs material/location
 * da combinacao cluster material / cluster location usam os mesmos parametros
 * estatisticos. Qualquer variacao por node de arvore, auto-fit ou outra regra
 * por DFU deve nascer em uma projection Enterprise separada.</p>
 */
public class ParametrosDemandPlanNivelClusterProjectionSimples extends ParametrosDemandPlanNivelClusterProjection {

    /**
     * Parametros estatisticos ja validados para o subconjunto Community. O
     * objeto permanece imutavel na rodada; services que processam clusters em
     * paralelo nao devem guardar estado adicional nesta projection.
     */
    @Getter
    private ParametrosForecastProjection parametrosForecastProjection;

    /**
     * Cria a unidade simples com os parametros gerais e estatisticos ja
     * resolvidos pela factory Community.
     */
    public ParametrosDemandPlanNivelClusterProjectionSimples(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            ClusterLocations clusterLocations,
            ClusterMateriais clusterMateriaisDemandPlanning,
            ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
            ParametrosForecastProjection parametrosForecastProjection) {
        super(perfilExecucaoDemandPlan,
                clusterLocations, clusterMateriaisDemandPlanning,
                parametrosGeraisDemandPlanningProjection);
        this.parametrosForecastProjection = parametrosForecastProjection;
    }

    @Override
    public ParametrosForecastProjection getParametrosForecastProjection(Location location, Produto material) {

        /*
         * No Community, location e material nao alteram o modelo estatistico:
         * a configuracao e unica para toda a combinacao cluster material /
         * cluster location. Os parametros permanecem na assinatura para manter
         * a extensao Enterprise simples e explicita.
         */
        return parametrosForecastProjection;

    }

}
