package com.opsfactor.community.capability.masterdata.network.location.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.organization.economicgroup.domain.EconomicGroup;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * Repository JPA de LocationRepository.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location,String> {

    List<Location> findAll();

    List<Location> findByTipoLocationIn(Collection<LocationAbstract.TipoLocation> tiposLocation);

    /**
     * Carrega em uma única consulta as locations pertencentes aos grupos
     * econômicos recebidos.
     *
     * <p>O uso fiscal Enterprise precisa materializar saldos consolidados por
     * grupo sem navegar a coleção inversa inexistente e sem disparar uma
     * consulta por eixo/período de saldo.</p>
     */
    @Query("SELECT location FROM Location location "
            + "WHERE location.economicGroup IN :economicGroups "
            + "ORDER BY location.economicGroup.id, location.id")
    List<Location> findByEconomicGroupInOrderByEconomicGroupIdAscIdAsc(
            @Param("economicGroups") Collection<EconomicGroup> economicGroups);

    /**
     * Realiza join fetch do parametrosLocation, clusterLocations e parametrosClusterLocations
     * @return
     */
    @Query("SELECT DISTINCT l FROM Location l")
    List<Location> customFindAllWithParametros();

    @Query("SELECT DISTINCT l FROM Location l "
            + "LEFT JOIN FETCH l.referenceLocationForProductLocationParameters "
            + "WHERE l.id <> '0'")
    List<Location> customFindAllSemDefault();

    /** Carrega em lote os três to-one de UOM usados pela capacidade logística estática. */
    @Query("SELECT DISTINCT l FROM Location l "
            + "LEFT JOIN FETCH l.unidadeMedidaCapacidadeArmazenagem "
            + "LEFT JOIN FETCH l.unidadeMedidaCapacidadeInbound "
            + "LEFT JOIN FETCH l.unidadeMedidaCapacidadeOutbound")
    List<Location> customFindAllJoinLogisticsCapacityUnits();

    /** Variante limitada ao escopo de locations da rodada, preservando o envelope batch. */
    @Query("SELECT DISTINCT l FROM Location l "
            + "LEFT JOIN FETCH l.unidadeMedidaCapacidadeArmazenagem "
            + "LEFT JOIN FETCH l.unidadeMedidaCapacidadeInbound "
            + "LEFT JOIN FETCH l.unidadeMedidaCapacidadeOutbound "
            + "WHERE l.id IN :locationIds")
    List<Location> customFindAllByIdJoinLogisticsCapacityUnits(Collection<String> locationIds);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva location mestre e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public <S extends Location> S save(S entity);

    /**
     * Salva locations mestres em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends Location> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove location mestre e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(Location entity);

    /**
     * Remove locations mestres em lote e invalida snapshots de parametros/cluster e malha.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends Location> entities);

}
