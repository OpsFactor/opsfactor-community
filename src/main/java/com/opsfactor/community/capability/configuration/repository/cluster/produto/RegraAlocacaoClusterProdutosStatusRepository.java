package com.opsfactor.community.capability.configuration.repository.cluster.produto;

import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosStatus;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA de RegraAlocacaoClusterProdutosStatusRepository.
 */
@Repository
public interface RegraAlocacaoClusterProdutosStatusRepository extends JpaRepository<RegraAlocacaoClusterProdutosStatus, RegraAlocacaoClusterProdutosStatus.RegraAlocacaoClusterProdutosStatusCompositeKey> {

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva regra de status de cluster de produtos e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends RegraAlocacaoClusterProdutosStatus> S save(S entity);

    /**
     * Salva regras de status de cluster de produtos em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterProdutosStatus> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove regra de status de cluster de produtos e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(RegraAlocacaoClusterProdutosStatus entity);

    /**
     * Remove regras de status de cluster de produtos em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends RegraAlocacaoClusterProdutosStatus> entities);

}
