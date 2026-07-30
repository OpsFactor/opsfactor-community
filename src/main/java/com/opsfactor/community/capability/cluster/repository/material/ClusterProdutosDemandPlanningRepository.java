package com.opsfactor.community.capability.cluster.repository.material;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository JPA de ClusterProdutosDemandPlanningRepository.
 */
@Repository
public interface ClusterProdutosDemandPlanningRepository extends JpaRepository<ClusterProdutosDemandPlanning,Long> {
    Optional<ClusterProdutosDemandPlanning> findByDescricao(String s);

    @EntityGraph(value = "ClusterProdutos.completo")
    List<ClusterProdutosDemandPlanning> findAll();
    List<ClusterProdutosDemandPlanning> findAllByPadraoIsFalse();

    /**
     * Carrega todos os clusters de materiais de Demand Planning com as regras
     * de alocacao e seus status em um unico snapshot administrativo.
     *
     * <p>O mapper da tela percorre tanto as regras quanto seus status. Os dois
     * {@code LEFT JOIN FETCH} evitam uma consulta lazy por cluster/regra sem
     * alterar a projection usada pelos calculos.</p>
     */
    @Query("SELECT DISTINCT cpd FROM ClusterProdutosDemandPlanning cpd "
            + "LEFT JOIN FETCH cpd.regrasAlocacaoClusterProdutos racp "
            + "LEFT JOIN FETCH racp.regraAlocacaoClusterProdutosStatusSet")
    List<ClusterProdutosDemandPlanning> customFindAllComRegrasAlocacaoEStatusProduto();

    /**
     * Carrega somente clusters nao padrao com as regras e status que a lista
     * de manutencao Community precisa materializar.
     */
    @Query("SELECT DISTINCT cpd FROM ClusterProdutosDemandPlanning cpd "
            + "LEFT JOIN FETCH cpd.regrasAlocacaoClusterProdutos racp "
            + "LEFT JOIN FETCH racp.regraAlocacaoClusterProdutosStatusSet "
            + "WHERE cpd.padrao = false")
    List<ClusterProdutosDemandPlanning> customFindAllByPadraoIsFalseComRegrasAlocacaoEStatusProduto();

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva cluster de materiais de Demand Planning e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ClusterProdutosDemandPlanning> S save(S entity);

    /**
     * Salva clusters de materiais de Demand Planning em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ClusterProdutosDemandPlanning> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove cluster de materiais de Demand Planning e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ClusterProdutosDemandPlanning entity);

    /**
     * Remove clusters de materiais de Demand Planning em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ClusterProdutosDemandPlanning> entities);

}
