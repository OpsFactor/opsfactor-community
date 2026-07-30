package com.opsfactor.community.capability.configuration.repository.cluster.produto;

import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutos;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA de RegraAlocacaoClusterProdutosRepository.
 */
@Repository
public interface RegraAlocacaoClusterProdutosRepository extends JpaRepository<RegraAlocacaoClusterProdutos, Long> {

    /**
     * Remove regras de alocacao ligadas ao cluster de produtos informado.
     */
    @CacheEvict(value = "clusterEParametrosProjection", allEntries = true)
    void deleteAllByClusterProdutosId(Long cluster_produto_id);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva regra de alocacao de cluster de produtos e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends RegraAlocacaoClusterProdutos> S save(S entity);

    /**
     * Salva regras de alocacao de cluster de produtos em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends RegraAlocacaoClusterProdutos> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove regra de alocacao de cluster de produtos e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(RegraAlocacaoClusterProdutos entity);

    /**
     * Remove regras de alocacao de cluster de produtos em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends RegraAlocacaoClusterProdutos> entities);

}
