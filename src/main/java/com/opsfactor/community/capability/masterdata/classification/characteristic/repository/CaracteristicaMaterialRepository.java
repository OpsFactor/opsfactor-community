package com.opsfactor.community.capability.masterdata.classification.characteristic.repository;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/** Shared repository for the public material-characteristic catalog. */
@Repository
public interface CaracteristicaMaterialRepository extends JpaRepository<CaracteristicaProduto, String> {

    /** Loads catalog and values in one round-trip so UI lookup never causes N+1 queries. */
    @Query("""
            SELECT DISTINCT materialCharacteristic
            FROM CaracteristicaProduto materialCharacteristic
            LEFT JOIN FETCH materialCharacteristic.listaValorCaracteristicaProduto characteristicValue
            LEFT JOIN FETCH characteristicValue.valorCaracteristicaProdutoCompositeKey.produto
            """)
    List<CaracteristicaProduto> findAllWithValues();

    /**
     * Loads only the requested material characteristics and their assigned values in one query.
     *
     * <p>The Demand Planning scope resolver must validate the submitted characteristic values
     * against the persisted catalog without causing one lazy-load round-trip per characteristic.
     * This is deliberately distinct from {@link #findAllWithValues()}, which is used when the
     * complete catalog is required by a selector.</p>
     */
    @Query("""
            SELECT DISTINCT materialCharacteristic
            FROM CaracteristicaProduto materialCharacteristic
            LEFT JOIN FETCH materialCharacteristic.listaValorCaracteristicaProduto characteristicValue
            LEFT JOIN FETCH characteristicValue.valorCaracteristicaProdutoCompositeKey.produto
            WHERE materialCharacteristic.id IN :characteristicIds
            """)
    List<CaracteristicaProduto> findByIdInWithValues(@Param("characteristicIds") Set<String> characteristicIds);

}
