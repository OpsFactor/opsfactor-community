package com.opsfactor.community.capability.demandplanning.configuration.repository;

import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA de PerfilExecucaoDemandPlanRepository.
 */
@Repository
public interface PerfilExecucaoDemandPlanRepository extends JpaRepository<PerfilExecucaoDemandPlan, String> {

    /*
     * Community carrega apenas os relacionamentos usados pela tela e pela rodada
     * estatistica basica. Campos de MAPE/auto-fit existem temporariamente na
     * entidade para compatibilidade JPA, mas nao devem ser buscados como grafo
     * padrao desta edicao.
     */
    @Query("SELECT pedp FROM PerfilExecucaoDemandPlan pedp " +
            "LEFT JOIN FETCH pedp.unidadeMedidaPadraoDP")
    public List<PerfilExecucaoDemandPlan> customFindAll();

    @Query("SELECT pedp FROM PerfilExecucaoDemandPlan pedp " +
            "LEFT JOIN FETCH pedp.unidadeMedidaPadraoDP " +
            "WHERE pedp.id = :id")
    public Optional<PerfilExecucaoDemandPlan> customFindById(String id);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva perfil de execucao Demand Planning e invalida snapshot de parametros Demand Planning.
     */
    @Override
    @CacheEvict(value = "parametrosDemandPlanProjection", key = "#entity")
    public <S extends PerfilExecucaoDemandPlan> S save(S entity);

    /**
     * Salva perfis de execucao Demand Planning em lote e invalida snapshot de parametros Demand Planning.
     */
    @Override
    @CacheEvict(value = "parametrosDemandPlanProjection", key = "#entity")
    <S extends PerfilExecucaoDemandPlan> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove perfil de execucao Demand Planning e invalida snapshot de parametros Demand Planning.
     */
    @Override
    @CacheEvict(value = "parametrosDemandPlanProjection", key = "#entity")
    public void delete(PerfilExecucaoDemandPlan entity);

    /**
     * Remove perfis de execucao Demand Planning em lote e invalida snapshot de parametros Demand Planning.
     */
    @Override
    @CacheEvict(value = "parametrosDemandPlanProjection", key = "#entity")
    void deleteAll(Iterable<? extends PerfilExecucaoDemandPlan> entities);

}
