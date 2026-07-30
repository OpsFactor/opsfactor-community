package com.opsfactor.community.capability.demandplanning.configuration.projection;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Builder
public class ParametrosDemandPlanProjection {

    @Getter
    PerfilExecucaoDemandPlan perfilExecucaoDemandPlan;
    @Getter
    ParametrosGlobais parametrosGlobais;

    Map<ClusterLocations, Map<ClusterProdutosDemandPlanning, ParametrosDemandPlanNivelClusterProjection>> mapaParametrosPorClusterLocationsClusterMateriais = new HashMap<>();

    /**
     * Recupera a configuracao efetiva de Demand Planning para a combinacao
     * cluster location / cluster material.
     *
     * <p>O tipo fisico ainda se chama {@link ClusterProdutosDemandPlanning}
     * porque a entidade JPA transicional herda de `ClusterProdutos`. O
     * parametro local usa a nomenclatura funcional do Community: cluster de
     * materiais.</p>
     */
    public ParametrosDemandPlanNivelClusterProjection getParametrosDemandPlanNivelClusterProjection(
            ClusterLocations clusterLocations,
            ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning) {
        return mapaParametrosPorClusterLocationsClusterMateriais
                .getOrDefault(clusterLocations, new HashMap<>())
                .get(clusterMateriaisDemandPlanning);
    }

    public Stream<ParametrosDemandPlanNivelClusterProjection> getStreamParametrosDemandPlanNivelClusterProjection() {
        return mapaParametrosPorClusterLocationsClusterMateriais
                .values()
                .stream()
                .flatMap(subMapa -> subMapa.values().stream());

    }

    public int getNumeroMaximoDiasHistoricoVendasParaForecast() {
        return getStreamParametrosDemandPlanNivelClusterProjection()
                .mapToInt(x -> x.getParametrosGeraisDemandPlanningProjection().diasHistoricosForecastEstatistico)
                .max()
                .orElse(1);
    }

    public ParametrosDemandPlanNivelClusterProjection getParametrosDemandPlanNivelClusterProjection(
            Location location,
            Produto material,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        ClusterLocations clusterLocations = clusterEParametrosProjection.getClusterLocationsDeLocation(location);
        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning =
                clusterEParametrosProjection.getClusterMateriaisDemandPlanning(material, location);

        return getParametrosDemandPlanNivelClusterProjection(clusterLocations, clusterMateriaisDemandPlanning);

    }

}
