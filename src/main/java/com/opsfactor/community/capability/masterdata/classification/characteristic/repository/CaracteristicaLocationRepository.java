package com.opsfactor.community.capability.masterdata.classification.characteristic.repository;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/** Shared repository for the public location-characteristic catalog. */
@Repository
public interface CaracteristicaLocationRepository extends JpaRepository<CaracteristicaLocation, String> {

    /** Loads catalog and values in one round-trip so UI lookup never causes N+1 queries. */
    @Query("""
            SELECT DISTINCT locationCharacteristic
            FROM CaracteristicaLocation locationCharacteristic
            LEFT JOIN FETCH locationCharacteristic.listaValorCaracteristicaLocation characteristicValue
            LEFT JOIN FETCH characteristicValue.valorCaracteristicaLocationCompositeKey.location
            """)
    List<CaracteristicaLocation> findAllWithValues();

    /**
     * Loads only the requested location characteristics and their assigned values in one query.
     *
     * <p>The Demand Planning scope resolver validates submitted characteristic values through
     * this bounded read.  Fetching the values here prevents an N+1 lookup while retaining the
     * explicit set of requested characteristic identifiers.</p>
     */
    @Query("""
            SELECT DISTINCT locationCharacteristic
            FROM CaracteristicaLocation locationCharacteristic
            LEFT JOIN FETCH locationCharacteristic.listaValorCaracteristicaLocation characteristicValue
            LEFT JOIN FETCH characteristicValue.valorCaracteristicaLocationCompositeKey.location
            WHERE locationCharacteristic.id IN :characteristicIds
            """)
    List<CaracteristicaLocation> findByIdInWithValues(@Param("characteristicIds") Set<String> characteristicIds);

}
