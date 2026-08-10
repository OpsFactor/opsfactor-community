package com.opsfactor.community.capability.masterdata.classification.characteristic.repository;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Shared repository for the public material-characteristic catalog. */
@Repository
public interface CaracteristicaMaterialRepository extends JpaRepository<CaracteristicaProduto, String> {

    /** Loads catalog and values in one round-trip so UI lookup never causes N+1 queries. */
    @Query("""
            SELECT DISTINCT materialCharacteristic
            FROM CaracteristicaProduto materialCharacteristic
            LEFT JOIN FETCH materialCharacteristic.listaValorCaracteristicaProduto
            """)
    List<CaracteristicaProduto> findAllWithValues();

}
