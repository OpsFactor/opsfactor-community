package com.opsfactor.community.capability.cluster.repository.material;

import com.opsfactor.community.capability.cluster.repository.ClusterProfileRepository;
import com.opsfactor.community.capability.cluster.repository.ClusterProfilePersistence;
import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA de ClusterProdutosRepository.
 */
@Repository
public interface ClusterProdutosRepository extends ClusterProfileRepository<ClusterProdutos>, ClusterProfilePersistence<ClusterProdutos> {

    /** Snapshot por perfil em uma consulta, sem navegação lazy por regra/cluster. */
    @Override
    @Query("SELECT DISTINCT c FROM ClusterProdutos c LEFT JOIN FETCH c.regrasAlocacaoClusterProdutos WHERE c.perfilClusterizacao.id = :perfilId ORDER BY c.prioridade, c.id")
    List<ClusterProdutos> findAllByPerfilClusterizacaoId(@org.springframework.data.repository.query.Param("perfilId") String perfilClusterizacaoId);

    ClusterProdutos findByPerfilClusterizacaoIdAndDescricao(String perfilClusterizacaoId, String descricao);

    /** Compatibilidade somente com o perfil LEGADO, nunca busca global por descrição. */
    @Deprecated
    default ClusterProdutos findByDescricao(String descricao) {

        return findByPerfilClusterizacaoIdAndDescricao(PerfilClusterizacao.ID_LEGADO, descricao);

    }


    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva cluster de materiais e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ClusterProdutos> S save(S entity);

    /**
     * Salva clusters de materiais em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ClusterProdutos> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove cluster de materiais e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ClusterProdutos entity);

    /**
     * Remove clusters de materiais em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ClusterProdutos> entities);

}
