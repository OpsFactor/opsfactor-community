package com.opsfactor.community.capability.configuration.repository.cluster.location;

import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsPaisEstado;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA de RegraAlocacaoClusterLocationsPaisEstadoRepository.
 */
@Repository
public interface RegraAlocacaoClusterLocationsPaisEstadoRepository extends JpaRepository<RegraAlocacaoClusterLocationsPaisEstado, RegraAlocacaoClusterLocationsPaisEstado.RegraAlocacaoClusterLocationsPaisEstadoCompositeKey> {

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva regra pais/estado de cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends RegraAlocacaoClusterLocationsPaisEstado> S save(S entity);

    /**
     * Salva regras pais/estado de cluster de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterLocationsPaisEstado> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove regra pais/estado de cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(RegraAlocacaoClusterLocationsPaisEstado entity);

    /**
     * Remove regras pais/estado de cluster de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends RegraAlocacaoClusterLocationsPaisEstado> entities);

}
