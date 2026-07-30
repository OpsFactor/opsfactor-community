package com.opsfactor.community.capability.cluster.repository.location;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA de ClusterLocationsRepository.
 */
@Repository
public interface ClusterLocationsRepository extends JpaRepository<ClusterLocations,Long> {

    @Query("SELECT DISTINCT cl FROM ClusterLocations cl "
            + "LEFT JOIN FETCH cl.regrasAlocacaoClusterLocations racl "
            + "LEFT JOIN FETCH racl.regrasAlocacaoClusterLocationsPaisEstadoSet "
            + "LEFT JOIN FETCH racl.regrasAlocacaoClusterLocationsTipoLocationSet "
            + "LEFT JOIN FETCH cl.parametrosClusterLocations pcl")
    List<ClusterLocations> customFindAll();

    ClusterProdutos findByDescricao(String s);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ClusterLocations> S save(S entity);

    /**
     * Salva clusters de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ClusterLocations> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ClusterLocations entity);

    /**
     * Remove clusters de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ClusterLocations> entities);

}
