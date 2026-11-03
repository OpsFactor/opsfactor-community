package com.opsfactor.community.capability.configuration.repository.cluster.produto;

import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosCaracteristica;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repository for the explicit characteristic values used by material-cluster rules. */
@Repository
public interface RegraAlocacaoClusterProdutosCaracteristicaRepository extends JpaRepository<
        RegraAlocacaoClusterProdutosCaracteristica,
        RegraAlocacaoClusterProdutosCaracteristica.RegraAlocacaoClusterProdutosCaracteristicaCompositeKey> {
    /** Carrega somente o perfil solicitado, com a regra em lote, sem N+1. */
    @Query("SELECT v FROM RegraAlocacaoClusterProdutosCaracteristica v JOIN FETCH v.regraAlocacaoClusterProdutosCaracteristicaCompositeKey.regraAlocacaoClusterProdutos r JOIN FETCH r.clusterProdutos c WHERE c.perfilClusterizacao.id = :perfilId")
    List<RegraAlocacaoClusterProdutosCaracteristica> findAllForClusteringProfile(@Param("perfilId") String perfilId);


    /** Saves one value and invalidates the projection that resolves material membership. */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterProdutosCaracteristica> S save(S entity);

    /** Saves a batch of values and invalidates the same projection snapshot once. */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterProdutosCaracteristica> List<S> saveAll(Iterable<S> entities);

    /** Removes one value and invalidates the projection that resolves material membership. */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    void delete(RegraAlocacaoClusterProdutosCaracteristica entity);

    /** Removes a batch of values and invalidates the projection that resolves material membership. */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends RegraAlocacaoClusterProdutosCaracteristica> entities);
}
