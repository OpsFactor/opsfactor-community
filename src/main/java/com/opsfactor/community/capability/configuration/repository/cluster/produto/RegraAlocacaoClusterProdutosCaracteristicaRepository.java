package com.opsfactor.community.capability.configuration.repository.cluster.produto;

import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosCaracteristica;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repository for the explicit characteristic values used by material-cluster rules. */
@Repository
public interface RegraAlocacaoClusterProdutosCaracteristicaRepository extends JpaRepository<
        RegraAlocacaoClusterProdutosCaracteristica,
        RegraAlocacaoClusterProdutosCaracteristica.RegraAlocacaoClusterProdutosCaracteristicaCompositeKey> {

    /** Saves one value and invalidates the projection that resolves material membership. */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterProdutosCaracteristica> S save(S entity);

    /** Saves a batch of values and invalidates the same projection snapshot once. */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterProdutosCaracteristica> List<S> saveAll(Iterable<S> entities);

    /** Removes one value and invalidates the projection that resolves material membership. */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void delete(RegraAlocacaoClusterProdutosCaracteristica entity);

    /** Removes a batch of values and invalidates the projection that resolves material membership. */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends RegraAlocacaoClusterProdutosCaracteristica> entities);
}
