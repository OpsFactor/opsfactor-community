package com.opsfactor.community.capability.masterdata.classification.characteristic.repository;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaLocation;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Repository Enterprise dos valores de caracteristicas por location.
 */
@Repository
public interface EnterpriseValorCaracteristicaLocationRepository extends JpaRepository<
        ValorCaracteristicaLocation,
        ValorCaracteristicaLocation.ValorCaracteristicaLocationCompositeKey> {

    /**
     * Carrega o snapshot completo de valores por location com location e
     * caracteristica inicializadas para Data Upload/exportacao, filtros de
     * primary key e projections sem N+1.
     */
    @Query("""
            SELECT valorCaracteristicaLocation
            FROM ValorCaracteristicaLocation valorCaracteristicaLocation
            LEFT JOIN FETCH valorCaracteristicaLocation.valorCaracteristicaLocationCompositeKey.location
            LEFT JOIN FETCH valorCaracteristicaLocation.valorCaracteristicaLocationCompositeKey.caracteristicaLocation
            """)
    List<ValorCaracteristicaLocation> customFindAllComCaracteristicaELocation();

    /**
     * Carrega todos os atributos de um conjunto de locations com os dois
     * lados da associação inicializados, para relatórios batch sem N+1.
     */
    @Query("""
            SELECT valorCaracteristicaLocation
            FROM ValorCaracteristicaLocation valorCaracteristicaLocation
            LEFT JOIN FETCH valorCaracteristicaLocation.valorCaracteristicaLocationCompositeKey.location
            LEFT JOIN FETCH valorCaracteristicaLocation.valorCaracteristicaLocationCompositeKey.caracteristicaLocation
            WHERE valorCaracteristicaLocation.valorCaracteristicaLocationCompositeKey.location.id IN :locationIds
            """)
    List<ValorCaracteristicaLocation> findAllWithCharacteristicAndLocationByLocationIds(
            @Param("locationIds") Set<String> locationIds);

    /**
     * Carrega apenas os valores de característica location necessários para
     * uma rodada, mantendo location e característica inicializados em uma
     * única query batch.
     */
    @Query("""
            SELECT valorCaracteristicaLocation
            FROM ValorCaracteristicaLocation valorCaracteristicaLocation
            LEFT JOIN FETCH valorCaracteristicaLocation.valorCaracteristicaLocationCompositeKey.location
            LEFT JOIN FETCH valorCaracteristicaLocation.valorCaracteristicaLocationCompositeKey.caracteristicaLocation
            WHERE valorCaracteristicaLocation.valorCaracteristicaLocationCompositeKey.location.id IN :locationIds
              AND valorCaracteristicaLocation.valorCaracteristicaLocationCompositeKey.caracteristicaLocation.id IN :characteristicIds
            """)
    List<ValorCaracteristicaLocation> findAllWithCharacteristicAndLocationByLocationIdsAndCharacteristicIds(
            @Param("locationIds") Set<String> locationIds,
            @Param("characteristicIds") Set<String> characteristicIds);

    /**
     * Salva valores de caracteristica de location e invalida projections compartilhadas.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ValorCaracteristicaLocation> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove valores de caracteristica de location e invalida projections compartilhadas.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ValorCaracteristicaLocation> entities);

}
