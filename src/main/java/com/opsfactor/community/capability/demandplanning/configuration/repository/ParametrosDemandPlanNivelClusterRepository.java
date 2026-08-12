package com.opsfactor.community.capability.demandplanning.configuration.repository;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import jakarta.persistence.LockModeType;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA de ParametrosDemandPlanNivelClusterRepository.
 */
@Repository
public interface ParametrosDemandPlanNivelClusterRepository extends CrudRepository<
        ParametrosDemandPlanNivelCluster,
        ParametrosDemandPlanNivelCluster.ParametrosDemandPlanNivelClusterCompositeKey> {

    public List<ParametrosDemandPlanNivelCluster> findAll();

    /*
     * As queries Community buscam perfil, unidade padrao e clusters necessarios
     * ao forecast estatistico. Configuracoes de auto-fit/regression tree ficam
     * fora do fetch porque pertencem ao Enterprise e sao bloqueadas na borda de
     * execucao antes da criacao das projections.
     */
    @Query("SELECT pcpdpcl FROM ParametrosDemandPlanNivelCluster pcpdpcl " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan pedp " +
            "LEFT JOIN FETCH pedp.unidadeMedidaPadraoDP uomdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterMateriais cpdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterLocations cl " +
            "WHERE pedp = :perfilExecucaoDemandPlan")
    public List<ParametrosDemandPlanNivelCluster> customFindAllDePerfilExecucaoDemandPlan(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan);

    @Query("SELECT pcpdpcl FROM ParametrosDemandPlanNivelCluster pcpdpcl " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan pedp " +
            "LEFT JOIN FETCH pedp.unidadeMedidaPadraoDP uomdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterMateriais cpdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterLocations cl " +
            "WHERE cpdp = :clusterMateriais " +
            "AND cl = :clusterLocations " +
            "AND pedp = :perfilExecucaoDemandPlan")
    public Optional<ParametrosDemandPlanNivelCluster> findByParametrosClusterProdutosDemandPlanningClusterLocationsCompositeKeyClusterProdutosDemandPlanningAndParametrosClusterProdutosDemandPlanningClusterLocationsCompositeKeyClusterLocations(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            ClusterMateriais clusterMateriais,
            ClusterLocations clusterLocations);

    /*
     * Método equivalente ao de cima mas usando o id do perfil de execução para evitar erro derivado de chamadas de repositório
     * em threads diferentes daquela usada para extração do perfilExecucaoDemandPlan
     * Erro: org.springframework.dao.InvalidDataAccessApiUsageException: Parameter value [PerfilExecucaoDemandPlan(id=PADRAO)] did not match expected type [com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan (n/a)]; nested exception is java.lang.IllegalArgumentException: Parameter value [PerfilExecucaoDemandPlan(id=PADRAO)] did not match expected type [com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan (n/a)]
     */
    @Query("SELECT pcpdpcl FROM ParametrosDemandPlanNivelCluster pcpdpcl " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan pedp " +
            "LEFT JOIN FETCH pedp.unidadeMedidaPadraoDP uomdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterMateriais cpdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterLocations cl " +
            "WHERE cpdp.id = :clusterProdutosDemandPlanningId " +
            "AND cl.id = :clusterLocationsId " +
            "AND pedp.id = :perfilExecucaoDemandPlanId")
    public Optional<ParametrosDemandPlanNivelCluster> findByParametrosClusterProdutosDemandPlanningClusterLocationsCompositeKeyClusterProdutosDemandPlanningAndParametrosClusterProdutosDemandPlanningClusterLocationsCompositeKeyClusterLocations(
            String perfilExecucaoDemandPlanId,
            Long clusterProdutosDemandPlanningId,
            Long clusterLocationsId);

    /**
     * Resolves and locks one existing Community cluster-level configuration.
     *
     * <p>Private Enterprise overlays that do not yet have their physical
     * unique key use this owner-row lock to serialize find-or-create updates.
     * Ordinary Demand Planning reads keep using the non-locking query above.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pcpdpcl FROM ParametrosDemandPlanNivelCluster pcpdpcl " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan pedp " +
            "LEFT JOIN FETCH pedp.unidadeMedidaPadraoDP uomdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterMateriais cpdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterLocations cl " +
            "WHERE cpdp.id = :clusterProdutosDemandPlanningId " +
            "AND cl.id = :clusterLocationsId " +
            "AND pedp.id = :perfilExecucaoDemandPlanId")
    Optional<ParametrosDemandPlanNivelCluster> findByKeyForEnterpriseOverlayUpdate(
            String perfilExecucaoDemandPlanId,
            Long clusterProdutosDemandPlanningId,
            Long clusterLocationsId);

    /*
     * Método equivalente ao de cima mas usando o id do perfil de execução para evitar erro derivado de chamadas de repositório
     * em threads diferentes daquela usada para extração do perfilExecucaoDemandPlan
     * Erro: org.springframework.dao.InvalidDataAccessApiUsageException: Parameter value [PerfilExecucaoDemandPlan(id=PADRAO)] did not match expected type [com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan (n/a)]; nested exception is java.lang.IllegalArgumentException: Parameter value [PerfilExecucaoDemandPlan(id=PADRAO)] did not match expected type [com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan (n/a)]
     */
    @Query("SELECT pcpdpcl FROM ParametrosDemandPlanNivelCluster pcpdpcl " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan pedp " +
            "LEFT JOIN FETCH pedp.unidadeMedidaPadraoDP uomdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterMateriais cpdp " +
            "LEFT JOIN FETCH pcpdpcl.parametrosDemandPlanNivelClusterCompositeKey.clusterLocations cl " +
            "WHERE pedp.id = :perfilExecucaoDemandPlanId")
    public List<ParametrosDemandPlanNivelCluster> findByPerfilExecucaoDemandPlanId(
            String perfilExecucaoDemandPlanId);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva parametro Demand Planning por nivel de cluster e invalida snapshot do perfil associado.
     */
    @Override
    @CacheEvict(value = "parametrosDemandPlanProjection", key = "#entity.parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan")
    public <S extends ParametrosDemandPlanNivelCluster> S save(S entity);

    /**
     * Salva parametros Demand Planning por nivel de cluster em lote e invalida snapshot do perfil associado.
     */
    @Override
    @CacheEvict(value = "parametrosDemandPlanProjection", key = "#entity.parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan")
    <S extends ParametrosDemandPlanNivelCluster> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove parametro Demand Planning por nivel de cluster e invalida snapshot do perfil associado.
     */
    @Override
    @CacheEvict(value = "parametrosDemandPlanProjection", key = "#entity.parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan")
    public void delete(ParametrosDemandPlanNivelCluster entity);

    /**
     * Remove parametros Demand Planning por nivel de cluster em lote e invalida snapshot do perfil associado.
     */
    @Override
    @CacheEvict(value = "parametrosDemandPlanProjection", key = "#entity.parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan")
    void deleteAll(Iterable<? extends ParametrosDemandPlanNivelCluster> entities);

}
