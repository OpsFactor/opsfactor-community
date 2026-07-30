package com.opsfactor.community.capability.configuration.repository;

import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio dos parametros operacionais material-location disponiveis no
 * Community.
 *
 * <p>A entidade Community nao possui mapa de caracteristicas material-location.
 * Esse mapa alimenta filtros DFU e estruturas de agregacao Enterprise. Por isso
 * as queries deste repositorio fazem fetch somente dos relacionamentos
 * Many-to-One necessarios para carga, projections e calculos basicos.</p>
 */
@Repository
public interface ParametrosProdutoLocationRepository extends JpaRepository<ParametrosProdutoLocation,ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey> {

    @Query("SELECT DISTINCT ppl FROM ParametrosProdutoLocation ppl "
        + "LEFT JOIN FETCH ppl.parametrosProdutoLocationCompositeKey.produto p "
        + "LEFT JOIN FETCH ppl.parametrosProdutoLocationCompositeKey.location l "
        + "LEFT JOIN FETCH ppl.unidadeMedidaLoteMinimoMultiploProducao umlmmp "
        + "LEFT JOIN FETCH ppl.unidadeMedidaPadrao ump")
    public List<ParametrosProdutoLocation> customFindAllComFetchAtributosManyToOne();

    public List<ParametrosProdutoLocation> findByParametrosProdutoLocationCompositeKeyLocationId(String locationId);
    public List<ParametrosProdutoLocation> findByParametrosProdutoLocationCompositeKeyLocationTipoLocation(LocationAbstract.TipoLocation tipoLocation);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva parametros material-location e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends ParametrosProdutoLocation> S save(S entity);

    /**
     * Salva parametros material-location em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ParametrosProdutoLocation> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove parametros material-location e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(ParametrosProdutoLocation entity);

    /**
     * Remove parametros material-location em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ParametrosProdutoLocation> entities);

}
