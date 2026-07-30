package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository;


import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import java.util.Collection;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository JPA de PoliticaEstoquesMaterialLocationRepository.
 */
@Repository
public interface PoliticaEstoquesMaterialLocationRepository extends JpaRepository<PoliticaEstoquesMaterialLocation,PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey> {

    @Query("SELECT peml FROM PoliticaEstoquesMaterialLocation peml " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.politicaEstoques pe " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.material m " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.location l ")
    List<PoliticaEstoquesMaterialLocation> customFindAll();

    @Query("SELECT peml FROM PoliticaEstoquesMaterialLocation peml " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.politicaEstoques pe " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.material m " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.location l " +
            "WHERE pe.id = :politicaEstoquesId")
    List<PoliticaEstoquesMaterialLocation> customFindByPoliticaEstoquesId(String politicaEstoquesId);

    @Query("SELECT peml FROM PoliticaEstoquesMaterialLocation peml " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.politicaEstoques pe " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.material m " +
            "LEFT JOIN FETCH peml.politicaEstoquesMaterialLocationCompositeKey.location l " +
            "WHERE pe.id IN :politicaEstoquesIdCollection")
    List<PoliticaEstoquesMaterialLocation> customFindByPoliticaEstoquesIdIn(Collection<String> politicaEstoquesIdCollection);

    /**
     * Remove vinculos material-location da politica de estoques informada.
     */
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    @Transactional
    public void removeByPoliticaEstoquesMaterialLocationCompositeKeyPoliticaEstoquesId(String politicaEstoquesId);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva vinculo material-location de politica de estoques e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends PoliticaEstoquesMaterialLocation> S save(S entity);

    /**
     * Salva vinculos material-location de politica de estoques em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends PoliticaEstoquesMaterialLocation> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove vinculo material-location de politica de estoques e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(PoliticaEstoquesMaterialLocation entity);

    /**
     * Remove vinculos material-location de politica de estoques em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends PoliticaEstoquesMaterialLocation> entities);

}
