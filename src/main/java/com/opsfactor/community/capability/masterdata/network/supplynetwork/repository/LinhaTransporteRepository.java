package com.opsfactor.community.capability.masterdata.network.supplynetwork.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte.LinhaTransporteCompositeKey;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository JPA de LinhaTransporteRepository.
 */
@Repository
public interface LinhaTransporteRepository extends JpaRepository<LinhaTransporte,LinhaTransporteCompositeKey> {

    public List<LinhaTransporte> findByLinhaTransporteCompositeKeyVersaoMalha(VersaoMalha versaoMalha);

    /**
     * Carrega a fotografia administrativa de transportation lanes de uma
     * versao de malha. A listagem Community mapeia a chave composta e a UOM
     * configurada da lane; todas essas relacoes precisam vir no mesmo select
     * para nao converter uma lista em N+1 consultas lazy.
     */
    @Query("SELECT DISTINCT lt FROM LinhaTransporte lt "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.versaoMalha vm "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld "
            + "LEFT JOIN FETCH lt.unidadeMedidaLoteMinimoMultiploTransporte uom "
            + "WHERE vm = :versaoMalha")
    List<LinhaTransporte> customFindForFrontByVersaoMalha(
            @Param("versaoMalha") VersaoMalha versaoMalha);

    /**
     * Encontra todas as linhas de transporte com destino na location indicada
     * @return
     */
    public List<LinhaTransporte> findByLinhaTransporteCompositeKeyVersaoMalhaAndLinhaTransporteCompositeKeyLocationDestino(
            VersaoMalha versaoMalha, Location locationDestino);

    /**
     * Retorna conjunto de linhas de transporte que tenham origem ou destino
     * em um conjunto de locations
     */
    @Query("SELECT lt FROM LinhaTransporte lt "
            + "WHERE lt.linhaTransporteCompositeKey.versaoMalha = :versaoMalha "
            + "AND (lt.linhaTransporteCompositeKey.locationOrigem IN :locationsOrigem "
            + "AND lt.linhaTransporteCompositeKey.locationDestino IN :locationsDestino)")
    public List<LinhaTransporte> findByLinhaTransporteCompositeKeyVersaoAndLinhaTransporteCompositeKeyLocationOrigemInAndLinhaTransporteCompositeKeyLocationDestinoIn(
            @Param("versaoMalha") VersaoMalha versaoMalha, @Param("locationsOrigem") Collection<Location> locationsOrigem, @Param("locationsDestino") Collection<Location> locationsDestino);

    /**
     * Retorna conjunto de linhas de transporte que tenham origem ou destino
     * em um conjunto de locations e versão malha em um conjunto de versões
     */
    @Query("SELECT lt FROM LinhaTransporte lt "
            + "WHERE lt.linhaTransporteCompositeKey.versaoMalha.id IN :versaoMalhaIds "
            + "AND (lt.linhaTransporteCompositeKey.locationOrigem.id IN :locationOrigemIds "
            + "AND lt.linhaTransporteCompositeKey.locationDestino.id IN :locationDestinoIds)")
    public List<LinhaTransporte> findByLinhaTransporteCompositeKeyVersaoIdInAndLinhaTransporteCompositeKeyLocationOrigemIdInAndLinhaTransporteCompositeKeyLocationDestinoIdIn(
            @Param("versaoMalhaIds") Collection<String> versaoMalhaIds, @Param("locationOrigemIds") Collection<String> locationOrigemIds, @Param("locationDestinoIds") Collection<String> locationDestinoIds);

    /**
     * Encontra todas as linhas de transporte com origem na location indicada
     * @return
     */
    public List<LinhaTransporte> findByLinhaTransporteCompositeKeyVersaoMalhaAndLinhaTransporteCompositeKeyLocationOrigem(VersaoMalha versaoMalha, Location locationDestino);

    @Query("SELECT DISTINCT lt FROM LinhaTransporte lt "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.versaoMalha vm "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld")
    List<LinhaTransporte> customFindAll();

    @Query("SELECT DISTINCT lt FROM LinhaTransporte lt "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.versaoMalha vm "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld "
            + "WHERE vm = :versaoMalha")
    List<LinhaTransporte> customFindByVersaoMalha(VersaoMalha versaoMalha);

    @Query("SELECT DISTINCT lt FROM LinhaTransporte lt "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.versaoMalha vm "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld "
            + "LEFT JOIN FETCH lt.mapaLinhaTransporteProduto ltp ")
    List<LinhaTransporte> customFindAllComMapaLinhaTransporteProduto();

    @Query("SELECT DISTINCT lt FROM LinhaTransporte lt "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.versaoMalha vm "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo "
            + "LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld "
            + "LEFT JOIN FETCH lt.mapaLinhaTransporteProduto ltp "
            + "WHERE vm = :versaoMalha")
    List<LinhaTransporte> customFindAllComMapaLinhaTransporteProduto(@Param("versaoMalha") VersaoMalha versaoMalha);

    /**
     * Remove linhas de transporte da versao de malha com origem nas locations informadas.
     */
    void removeByLinhaTransporteCompositeKeyVersaoMalhaAndLinhaTransporteCompositeKeyLocationOrigemIn(VersaoMalha versaoMalha, Collection<Location> locations);

    /**
     * Remove linhas de transporte da versao de malha com destino nas locations informadas.
     */
    void removeByLinhaTransporteCompositeKeyVersaoMalhaAndLinhaTransporteCompositeKeyLocationDestinoIn(VersaoMalha versaoMalha, Collection<Location> locations);

    /**
     * Remove linhas de transporte que usam as locations informadas como origem.
     */
    void removeByLinhaTransporteCompositeKeyLocationOrigemIn(Collection<Location> locations);

    /**
     * Remove linhas de transporte que usam as locations informadas como destino.
     */
    void removeByLinhaTransporteCompositeKeyLocationDestinoIn(Collection<Location> locations);

    // OVERRIDES SAVE E DELETE PARA @CACHEEVICT -------------------------------------------------------------------------------------------
    // limpa caches dependentes em chamadas de saveAll e deleteAll (cacheEvict nao funciona em metodos @Override dos serviços de integração)
    /**
     * Salva linha de transporte e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public <S extends LinhaTransporte> S save(S entity);

    /**
     * Salva linhas de transporte em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    <S extends LinhaTransporte> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove linha de transporte e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    public void delete(LinhaTransporte entity);

    /**
     * Remove linhas de transporte em lote e invalida snapshot da malha de suprimentos.
     */
    @Override
    @CacheEvict(value = "supplyNetworkProjection", allEntries = true)
    void deleteAll(Iterable<? extends LinhaTransporte> entities);

}
