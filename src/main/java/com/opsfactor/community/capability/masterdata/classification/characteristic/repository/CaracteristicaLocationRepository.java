package com.opsfactor.community.capability.masterdata.classification.characteristic.repository;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Shared repository for the public location-characteristic catalog. */
@Repository
public interface CaracteristicaLocationRepository extends JpaRepository<CaracteristicaLocation, String> {

    /** Loads catalog and values in one round-trip so UI lookup never causes N+1 queries. */
    @Query("""
            SELECT DISTINCT locationCharacteristic
            FROM CaracteristicaLocation locationCharacteristic
            LEFT JOIN FETCH locationCharacteristic.listaValorCaracteristicaLocation
            """)
    List<CaracteristicaLocation> findAllWithValues();

}
