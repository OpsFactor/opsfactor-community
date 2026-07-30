package com.opsfactor.community.capability.configuration.repository.cluster.location;

import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsTipoLocation;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA de RegraAlocacaoClusterLocationsTipoLocationRepository.
 */
@Repository
public interface RegraAlocacaoClusterLocationsTipoLocationRepository extends JpaRepository<RegraAlocacaoClusterLocationsTipoLocation, RegraAlocacaoClusterLocationsTipoLocation.RegraAlocacaoClusterLocationsTipoLocationCompositeKey> {

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva regra por tipo de location de cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends RegraAlocacaoClusterLocationsTipoLocation> S save(S entity);

    /**
     * Salva regras por tipo de location de cluster de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterLocationsTipoLocation> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove regra por tipo de location de cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(RegraAlocacaoClusterLocationsTipoLocation entity);

    /**
     * Remove regras por tipo de location de cluster de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends RegraAlocacaoClusterLocationsTipoLocation> entities);

}
