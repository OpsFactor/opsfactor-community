package com.opsfactor.community.capability.configuration.repository.cluster.location;

import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocations;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA de RegraAlocacaoClusterLocationsRepository.
 */
@Repository
public interface RegraAlocacaoClusterLocationsRepository extends JpaRepository<RegraAlocacaoClusterLocations, Long> {

    /**
     * Remove regras de alocacao ligadas ao cluster de locations informado.
     */
    @CacheEvict(value = "clusterEParametrosProjection", allEntries = true)
    void deleteAllByClusterLocationsId(Long cluster_location_id);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva regra de alocacao de cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends RegraAlocacaoClusterLocations> S save(S entity);

    /**
     * Salva regras de alocacao de cluster de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterLocations> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove regra de alocacao de cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(RegraAlocacaoClusterLocations entity);

    /**
     * Remove regras de alocacao de cluster de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends RegraAlocacaoClusterLocations> entities);

}
