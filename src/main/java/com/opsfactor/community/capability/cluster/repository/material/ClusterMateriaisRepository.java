package com.opsfactor.community.capability.cluster.repository.material;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository JPA dos clusters de materiais.
 */
@Repository
public interface ClusterMateriaisRepository extends JpaRepository<ClusterMateriais,Long> {
    Optional<ClusterMateriais> findByDescricao(String s);

    @EntityGraph(value = "ClusterProdutos.completo")
    List<ClusterMateriais> findAll();
    List<ClusterMateriais> findAllByPadraoIsFalse();

    /**
     * Carrega todos os clusters de materiais com as regras
     * de alocacao, seus status e valores de caracteristica em um unico snapshot administrativo.
     *
     * <p>O mapper da tela percorre tanto as regras quanto seus status. Os dois
     * {@code LEFT JOIN FETCH} evitam uma consulta lazy por cluster/regra sem
     * alterar a projection usada pelos calculos.</p>
     */
    @Query("SELECT DISTINCT cpd FROM ClusterMateriais cpd "
            + "LEFT JOIN FETCH cpd.regrasAlocacaoClusterProdutos racp "
            + "LEFT JOIN FETCH racp.regraAlocacaoClusterProdutosStatusSet "
            + "LEFT JOIN FETCH racp.regrasAlocacaoClusterProdutosCaracteristicaSet racpc "
            + "LEFT JOIN FETCH racpc.regraAlocacaoClusterProdutosCaracteristicaCompositeKey.caracteristica")
    List<ClusterMateriais> customFindAllComRegrasAlocacaoEStatusProduto();

    /**
     * Carrega somente clusters nao padrao com as regras e status que a lista
     * de manutencao Community precisa materializar.
     */
    @Query("SELECT DISTINCT cpd FROM ClusterMateriais cpd "
            + "LEFT JOIN FETCH cpd.regrasAlocacaoClusterProdutos racp "
            + "LEFT JOIN FETCH racp.regraAlocacaoClusterProdutosStatusSet "
            + "LEFT JOIN FETCH racp.regrasAlocacaoClusterProdutosCaracteristicaSet racpc "
            + "LEFT JOIN FETCH racpc.regraAlocacaoClusterProdutosCaracteristicaCompositeKey.caracteristica "
            + "WHERE cpd.padrao = false")
    List<ClusterMateriais> customFindAllByPadraoIsFalseComRegrasAlocacaoEStatusProduto();

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva cluster de materiais e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ClusterMateriais> S save(S entity);

    /**
     * Salva clusters de materiais em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ClusterMateriais> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove cluster de materiais e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ClusterMateriais entity);

    /**
     * Remove clusters de materiais em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ClusterMateriais> entities);

}
