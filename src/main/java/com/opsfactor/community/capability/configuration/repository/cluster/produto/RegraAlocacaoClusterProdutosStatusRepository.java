package com.opsfactor.community.capability.configuration.repository.cluster.produto;

import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosStatus;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA de RegraAlocacaoClusterProdutosStatusRepository.
 */
@Repository
public interface RegraAlocacaoClusterProdutosStatusRepository extends JpaRepository<RegraAlocacaoClusterProdutosStatus, RegraAlocacaoClusterProdutosStatus.RegraAlocacaoClusterProdutosStatusCompositeKey> {
    /** Carrega somente o perfil solicitado, com a regra em lote, sem N+1. */
    @Query("SELECT v FROM RegraAlocacaoClusterProdutosStatus v JOIN FETCH v.regraAlocacaoClusterProdutosStatusCompositeKey.regraAlocacaoClusterProdutos r JOIN FETCH r.clusterProdutos c WHERE c.perfilClusterizacao.id = :perfilId")
    List<RegraAlocacaoClusterProdutosStatus> findAllForClusteringProfile(@Param("perfilId") String perfilId);


    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva regra de status de cluster de produtos e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends RegraAlocacaoClusterProdutosStatus> S save(S entity);

    /**
     * Salva regras de status de cluster de produtos em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterProdutosStatus> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove regra de status de cluster de produtos e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(RegraAlocacaoClusterProdutosStatus entity);

    /**
     * Remove regras de status de cluster de produtos em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends RegraAlocacaoClusterProdutosStatus> entities);

}
