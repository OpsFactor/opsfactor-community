package com.opsfactor.community.platform.bi;

import com.google.common.collect.Sets;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilitarios Community para expandir combinacoes material/location em DFUs.
 *
 * <p>Filtros por caracteristica material-location/DFU sao recursos Enterprise e
 * nao devem aparecer nesta classe. O Community trabalha apenas com filtros
 * diretos de material, location e filtros ad-hoc internos definidos pelos
 * services que montam a visao.</p>
 */
public class AgregacaoDFU {

    /**
     * Extrai todas as DFUs ativas do produto cartesiano de locations e materiais.
     */
    public static Set<DFU> getDFUListDeProdutoCartesianoLocationMaterial(
            Set<Location> locations, Set<Produto> materiais,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        Set<List<Object>> listaCombinacoes = Sets.cartesianProduct(materiais, locations);

        return listaCombinacoes.stream()
                .map(x -> new DFU((Produto) x.get(0), (Location) x.get(1)))
                .filter(x -> clusterEParametrosProjection.isDfuAtiva(x.getProduto(), x.getLocation()))
                .collect(Collectors.toSet());

    }

    /**
     * Gera uma DFU agregada por location para cada location com ao menos um
     * material ativo no recorte informado.
     */
    public static Set<DFU> getDFUListDeLocations(
            Set<Location> locations,
            Set<Produto> materiais,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        return locations.parallelStream()
                .filter(location -> materiais.stream()
                        .anyMatch(material -> clusterEParametrosProjection.isDfuAtiva(material, location)))
                .map(location -> new DFU(null, location))
                .collect(Collectors.toSet());

    }

    /**
     * Gera uma DFU agregada por material para cada material ativo em ao menos
     * uma location do recorte informado.
     */
    public static Set<DFU> getDFUListDeMateriais(
            Set<Location> locations,
            Set<Produto> materiais,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        return materiais.parallelStream()
                .filter(material -> locations.stream()
                        .anyMatch(location -> clusterEParametrosProjection.isDfuAtiva(material, location)))
                .map(material -> new DFU(material, null))
                .collect(Collectors.toSet());

    }

    /**
     * Verifica se existe ao menos uma combinacao material/location ativa.
     */
    public static boolean verificaSeAlgumaCombinacaoViavel(
            Set<Location> locations,
            Set<Produto> materiais,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        return materiais.parallelStream()
                .anyMatch(material -> locations.stream()
                        .anyMatch(location -> clusterEParametrosProjection.isDfuAtiva(material, location)));

    }

}
