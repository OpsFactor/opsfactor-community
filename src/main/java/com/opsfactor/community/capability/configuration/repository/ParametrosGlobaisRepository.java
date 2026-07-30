package com.opsfactor.community.capability.configuration.repository;


import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA de ParametrosGlobaisRepository.
 */
@Repository
public interface ParametrosGlobaisRepository extends JpaRepository<ParametrosGlobais,Long> {

    @Query("SELECT pg FROM ParametrosGlobais pg "
        + "LEFT JOIN FETCH pg.unidadeMedidaPadraoDP umpdp "
        + "LEFT JOIN FETCH pg.unidadeMedidaPadraoSNP umpsnp "
        + "WHERE pg.id = 0")
    public Optional<ParametrosGlobais> customFindComDependencias();

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva parametros globais e invalida snapshots de parametros, cluster, malha e Demand Planning.
     */
    @Override
    @CacheEvict(value = {"parametrosGlobais", "clusterEParametrosProjection", "supplyNetworkProjection", "parametrosDemandPlanProjection"}, allEntries = true)
    public <S extends ParametrosGlobais> S save(S entity);

    /**
     * Salva parametros globais em lote e invalida snapshots de parametros, cluster, malha e Demand Planning.
     */
    @Override
    @CacheEvict(value = {"parametrosGlobais", "clusterEParametrosProjection", "supplyNetworkProjection", "parametrosDemandPlanProjection"}, allEntries = true)
    <S extends ParametrosGlobais> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove parametros globais e invalida snapshots de parametros, cluster, malha e Demand Planning.
     */
    @Override
    @CacheEvict(value = {"parametrosGlobais", "clusterEParametrosProjection", "supplyNetworkProjection", "parametrosDemandPlanProjection"}, allEntries = true)
    public void delete(ParametrosGlobais entity);

    /**
     * Remove parametros globais em lote e invalida snapshots de parametros, cluster, malha e Demand Planning.
     */
    @Override
    @CacheEvict(value = {"parametrosGlobais", "clusterEParametrosProjection", "supplyNetworkProjection", "parametrosDemandPlanProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ParametrosGlobais> entities);

}
