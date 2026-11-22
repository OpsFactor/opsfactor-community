package com.opsfactor.community.capability.cluster.repository.location;

import com.opsfactor.community.capability.cluster.repository.ClusterProfileRepository;
import com.opsfactor.community.capability.cluster.repository.ClusterProfilePersistence;
import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA de ClusterLocationsRepository.
 */
@Repository
public interface ClusterLocationsRepository extends ClusterProfileRepository<ClusterLocations>, ClusterProfilePersistence<ClusterLocations> {

    /**
     * Busca primeiro as raizes unicas e inicializa suas regras em lote na
     * mesma sessao. O resultado do fetch nao e usado como lista de raizes:
     * joins de colecoes podem retornar uma linha por regra no Hibernate.
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    default List<ClusterLocations> findAllByPerfilClusterizacaoId(String perfilClusterizacaoId) {

        List<ClusterLocations> clusters = findProfileRoots(perfilClusterizacaoId);
        if (!clusters.isEmpty()) {
            loadProfileRules(perfilClusterizacaoId);
        }
        return clusters;

    }

    @Query("SELECT c FROM ClusterLocations c WHERE c.perfilClusterizacao.id = :perfilId ORDER BY c.prioridade, c.id")
    List<ClusterLocations> findProfileRoots(@org.springframework.data.repository.query.Param("perfilId") String perfilClusterizacaoId);

    @Query("SELECT DISTINCT c FROM ClusterLocations c LEFT JOIN FETCH c.regrasAlocacaoClusterLocations LEFT JOIN FETCH c.parametrosClusterLocations WHERE c.perfilClusterizacao.id = :perfilId ORDER BY c.prioridade, c.id")
    List<ClusterLocations> loadProfileRules(@org.springframework.data.repository.query.Param("perfilId") String perfilClusterizacaoId);


    /**
     * Carrega as regras e seus valores do cluster selecionado em uma consulta,
     * para que o mapper da simulacao nao dependa de uma sessao JPA aberta.
     */
    @Query("SELECT DISTINCT c FROM ClusterLocations c LEFT JOIN FETCH c.regrasAlocacaoClusterLocations r LEFT JOIN FETCH r.regrasAlocacaoClusterLocationsPaisEstadoSet LEFT JOIN FETCH r.regrasAlocacaoClusterLocationsTipoLocationSet WHERE c.id = :clusterId AND c.perfilClusterizacao.id = :perfilId")
    Optional<ClusterLocations> findForSimulation(@org.springframework.data.repository.query.Param("clusterId") Long clusterId, @org.springframework.data.repository.query.Param("perfilId") String perfilId);

    /** Catálogo leve: projeção escalar sem carregar regras, locais ou materiais. */
    interface IdentityView {
        Long getId();
        String getDescricao();
        String getPerfilClusterizacaoId();
    }

    @Query("SELECT c.id AS id, c.descricao AS descricao, c.perfilClusterizacao.id AS perfilClusterizacaoId FROM ClusterLocations c WHERE c.perfilClusterizacao.id = 'LEGADO'")
    List<IdentityView> findAllProjectedBy();

    @Query("SELECT c.id AS id, c.descricao AS descricao, c.perfilClusterizacao.id AS perfilClusterizacaoId FROM ClusterLocations c WHERE c.perfilClusterizacao.id = :perfilId")
    List<IdentityView> findAllProjectedByPerfilClusterizacaoId(@org.springframework.data.repository.query.Param("perfilId") String perfilClusterizacaoId);


    @Query("SELECT DISTINCT cl FROM ClusterLocations cl LEFT JOIN FETCH cl.regrasAlocacaoClusterLocations racl LEFT JOIN FETCH racl.regrasAlocacaoClusterLocationsPaisEstadoSet LEFT JOIN FETCH racl.regrasAlocacaoClusterLocationsTipoLocationSet LEFT JOIN FETCH cl.parametrosClusterLocations pcl WHERE cl.perfilClusterizacao.id = 'LEGADO'")
    List<ClusterLocations> customFindAll();

    ClusterLocations findByPerfilClusterizacaoIdAndDescricao(String perfilClusterizacaoId, String descricao);

    /** Compatibilidade somente com o perfil LEGADO, nunca busca global por descrição. */
    @Deprecated
    default ClusterLocations findByDescricao(String descricao) {

        return findByPerfilClusterizacaoIdAndDescricao(PerfilClusterizacao.ID_LEGADO, descricao);

    }


    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ClusterLocations> S save(S entity);

    /**
     * Salva clusters de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ClusterLocations> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove cluster de locations e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ClusterLocations entity);

    /**
     * Remove clusters de locations em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ClusterLocations> entities);

}
