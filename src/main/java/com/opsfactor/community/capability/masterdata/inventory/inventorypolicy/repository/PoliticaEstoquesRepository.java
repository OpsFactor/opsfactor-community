package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository;


import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA de PoliticaEstoquesRepository.
 */
@Repository
public interface PoliticaEstoquesRepository extends JpaRepository<PoliticaEstoques,String> {

    @Query("SELECT DISTINCT pe FROM PoliticaEstoques pe " +
            "LEFT JOIN FETCH pe.politicaEstoquesMaterialLocationList pemll " +
            "LEFT JOIN FETCH pemll.politicaEstoquesMaterialLocationCompositeKey.material mat " +
            "LEFT JOIN FETCH pemll.politicaEstoquesMaterialLocationCompositeKey.location loc " +
            "WHERE pe.id = :id")
    Optional<PoliticaEstoques> customFindById(String id);

    @Query("SELECT DISTINCT pe FROM PoliticaEstoques pe " +
            "LEFT JOIN FETCH pe.politicaEstoquesMaterialLocationList pemll " +
            "LEFT JOIN FETCH pemll.politicaEstoquesMaterialLocationCompositeKey.material mat " +
            "LEFT JOIN FETCH pemll.politicaEstoquesMaterialLocationCompositeKey.location loc")
    List<PoliticaEstoques> customFindAllWithMaterialLocation();

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva politica de estoques e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends PoliticaEstoques> S save(S entity);

    /**
     * Salva politicas de estoques em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends PoliticaEstoques> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove politica de estoques e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(PoliticaEstoques entity);

    /**
     * Remove politicas de estoques em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends PoliticaEstoques> entities);

}
