package com.opsfactor.community.capability.cluster.repository.material;

import com.opsfactor.community.capability.cluster.repository.ClusterProfileRepository;
import com.opsfactor.community.capability.cluster.repository.ClusterProfilePersistence;
import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import java.util.List;
import org.springframework.stereotype.Repository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.EntityGraph;

/**
 * Repository JPA dos clusters de materiais.
 */
@Repository
public interface ClusterMateriaisRepository extends ClusterProfileRepository<ClusterMateriais>, ClusterProfilePersistence<ClusterMateriais> {

    /** Snapshot por perfil em uma consulta, sem navegação lazy por regra/cluster. */
    @Override
    @Query("SELECT DISTINCT c FROM ClusterMateriais c LEFT JOIN FETCH c.regrasAlocacaoClusterProdutos WHERE c.perfilClusterizacao.id = :perfilId ORDER BY c.prioridade, c.id")
    List<ClusterMateriais> findAllByPerfilClusterizacaoId(@org.springframework.data.repository.query.Param("perfilId") String perfilClusterizacaoId);


    /** Catálogo leve: projeção escalar sem carregar regras, locais ou materiais. */
    interface IdentityView {
        Long getId();
        String getDescricao();
        String getPerfilClusterizacaoId();
    }

    @Query("SELECT c.id AS id, c.descricao AS descricao, c.perfilClusterizacao.id AS perfilClusterizacaoId FROM ClusterMateriais c WHERE c.perfilClusterizacao.id = 'LEGADO'")
    List<IdentityView> findAllProjectedBy();

    @Query("SELECT c.id AS id, c.descricao AS descricao, c.perfilClusterizacao.id AS perfilClusterizacaoId FROM ClusterMateriais c WHERE c.perfilClusterizacao.id = :perfilId")
    List<IdentityView> findAllProjectedByPerfilClusterizacaoId(@org.springframework.data.repository.query.Param("perfilId") String perfilClusterizacaoId);

    Optional<ClusterMateriais> findByPerfilClusterizacaoIdAndDescricao(String perfilClusterizacaoId, String descricao);

    /** Compatibilidade somente com o perfil LEGADO, nunca busca global por descrição. */
    @Deprecated
    default Optional<ClusterMateriais> findByDescricao(String descricao) {

        return findByPerfilClusterizacaoIdAndDescricao(PerfilClusterizacao.ID_LEGADO, descricao);

    }
    @Query("SELECT c FROM ClusterMateriais c WHERE c.perfilClusterizacao.id = 'LEGADO' AND c.padrao = false")
    List<ClusterMateriais> findAllByPadraoIsFalse();

    /**
     * Carrega todos os clusters de materiais com as regras
     * de alocacao, seus status e valores de caracteristica em um unico snapshot administrativo.
     *
     * <p>O mapper da tela percorre tanto as regras quanto seus status. Os dois
     * {@code LEFT JOIN FETCH} evitam uma consulta lazy por cluster/regra sem
     * alterar a projection usada pelos calculos.</p>
     */
    @Query("SELECT DISTINCT cpd FROM ClusterMateriais cpd LEFT JOIN FETCH cpd.regrasAlocacaoClusterProdutos racp LEFT JOIN FETCH racp.regraAlocacaoClusterProdutosStatusSet LEFT JOIN FETCH racp.regrasAlocacaoClusterProdutosCaracteristicaSet racpc LEFT JOIN FETCH racpc.regraAlocacaoClusterProdutosCaracteristicaCompositeKey.caracteristica WHERE cpd.perfilClusterizacao.id = 'LEGADO'")
    List<ClusterMateriais> customFindAllComRegrasAlocacaoEStatusProduto();

    /**
     * Carrega somente clusters nao padrao com as regras e status que a lista
     * de manutencao Community precisa materializar.
     */
    @Query("SELECT DISTINCT cpd FROM ClusterMateriais cpd LEFT JOIN FETCH cpd.regrasAlocacaoClusterProdutos racp LEFT JOIN FETCH racp.regraAlocacaoClusterProdutosStatusSet LEFT JOIN FETCH racp.regrasAlocacaoClusterProdutosCaracteristicaSet racpc LEFT JOIN FETCH racpc.regraAlocacaoClusterProdutosCaracteristicaCompositeKey.caracteristica WHERE cpd.padrao = false AND cpd.perfilClusterizacao.id = 'LEGADO'")
    List<ClusterMateriais> customFindAllByPadraoIsFalseComRegrasAlocacaoEStatusProduto();

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva cluster de materiais e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ClusterMateriais> S save(S entity);

    /**
     * Salva clusters de materiais em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ClusterMateriais> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove cluster de materiais e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ClusterMateriais entity);

    /**
     * Remove clusters de materiais em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ClusterMateriais> entities);

}
