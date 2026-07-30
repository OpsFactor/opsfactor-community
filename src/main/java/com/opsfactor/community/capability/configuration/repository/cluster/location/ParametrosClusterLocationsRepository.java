package com.opsfactor.community.capability.configuration.repository.cluster.location;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.configuration.domain.cluster.location.ParametrosClusterLocations;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA de ParametrosClusterLocationsRepository.
 */
@Repository
public interface ParametrosClusterLocationsRepository extends CrudRepository<ParametrosClusterLocations,ClusterLocations> {

    Optional<ParametrosClusterLocations> findByParametrosClusterLocationsCompositeKeyClusterLocations(ClusterLocations clusterLocations);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva parametros de cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ParametrosClusterLocations> S save(S entity);

    /**
     * Salva parametros de cluster de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ParametrosClusterLocations> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove parametros de cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ParametrosClusterLocations entity);

    /**
     * Remove parametros de cluster de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ParametrosClusterLocations> entities);

}
