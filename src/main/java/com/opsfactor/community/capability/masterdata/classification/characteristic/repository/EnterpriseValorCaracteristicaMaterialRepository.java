package com.opsfactor.community.capability.masterdata.classification.characteristic.repository;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaProduto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Repository Enterprise dos valores de caracteristicas por material.
 */
@Repository
public interface EnterpriseValorCaracteristicaMaterialRepository extends JpaRepository<
        ValorCaracteristicaProduto,
        ValorCaracteristicaProduto.ValorCaracteristicaProdutoCompositeKey> {

    /**
     * Carrega o snapshot completo de valores por material com material e
     * caracteristica inicializados para Data Upload/exportacao, filtros de
     * primary key e projections sem N+1.
     */
    @Query("""
            SELECT valorCaracteristicaProduto
            FROM ValorCaracteristicaProduto valorCaracteristicaProduto
            LEFT JOIN FETCH valorCaracteristicaProduto.valorCaracteristicaProdutoCompositeKey.produto
            LEFT JOIN FETCH valorCaracteristicaProduto.valorCaracteristicaProdutoCompositeKey.caracteristicaProduto
            """)
    List<ValorCaracteristicaProduto> customFindAllComCaracteristicaEMaterial();

    /**
     * Carrega todos os atributos de um conjunto de materiais com os dois
     * lados da associação inicializados, para relatórios batch sem N+1.
     */
    @Query("""
            SELECT valorCaracteristicaProduto
            FROM ValorCaracteristicaProduto valorCaracteristicaProduto
            LEFT JOIN FETCH valorCaracteristicaProduto.valorCaracteristicaProdutoCompositeKey.produto
            LEFT JOIN FETCH valorCaracteristicaProduto.valorCaracteristicaProdutoCompositeKey.caracteristicaProduto
            WHERE valorCaracteristicaProduto.valorCaracteristicaProdutoCompositeKey.produto.id IN :materialIds
            """)
    List<ValorCaracteristicaProduto> findAllWithCharacteristicAndMaterialByMaterialIds(
            @Param("materialIds") Set<String> materialIds);

    /**
     * Carrega apenas os valores de característica material necessários para
     * uma rodada, mantendo material e característica inicializados em uma
     * única query batch.
     */
    @Query("""
            SELECT valorCaracteristicaProduto
            FROM ValorCaracteristicaProduto valorCaracteristicaProduto
            LEFT JOIN FETCH valorCaracteristicaProduto.valorCaracteristicaProdutoCompositeKey.produto
            LEFT JOIN FETCH valorCaracteristicaProduto.valorCaracteristicaProdutoCompositeKey.caracteristicaProduto
            WHERE valorCaracteristicaProduto.valorCaracteristicaProdutoCompositeKey.produto.id IN :materialIds
              AND valorCaracteristicaProduto.valorCaracteristicaProdutoCompositeKey.caracteristicaProduto.id IN :characteristicIds
            """)
    List<ValorCaracteristicaProduto> findAllWithCharacteristicAndMaterialByMaterialIdsAndCharacteristicIds(
            @Param("materialIds") Set<String> materialIds,
            @Param("characteristicIds") Set<String> characteristicIds);

    /**
     * Salva valores de caracteristica de material e invalida projections compartilhadas.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    <S extends ValorCaracteristicaProduto> List<S> saveAll(Iterable<S> entities);

    /**
     * Remove valores de caracteristica de material e invalida projections compartilhadas.
     */
    @Override
    @CacheEvict(value = {"clusterEParametrosProjection", "supplyNetworkProjection"}, allEntries = true)
    void deleteAll(Iterable<? extends ValorCaracteristicaProduto> entities);

}
