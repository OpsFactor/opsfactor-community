package com.opsfactor.community.capability.cluster.repository.material;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA de ClusterProdutosRepository.
 */
@Repository
public interface ClusterProdutosRepository extends CrudRepository<ClusterProdutos,Long> {

    ClusterProdutos findByDescricao(String s);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva cluster de materiais e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ClusterProdutos> S save(S entity);

    /**
     * Salva clusters de materiais em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ClusterProdutos> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove cluster de materiais e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ClusterProdutos entity);

    /**
     * Remove clusters de materiais em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ClusterProdutos> entities);

}
