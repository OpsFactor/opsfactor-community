package com.opsfactor.community.capability.transactionaldata.inventory.stock.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDateImpl;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMImpl;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

/**
 * Contratos defensivos das projections Community de estoque transacional.
 *
 * <p>A factory ja valida o retorno dos repositories, mas projections tambem
 * podem ser materializadas por testes, workflows Enterprise ou factories
 * alternativas. Estes testes garantem que chamadas diretas nao consigam
 * corromper os indices internos com chaves funcionais quebradas.</p>
 */
class EstoqueProjectionCommunityContractTest {

    @Test
    void builderCreatedSnapshotsShouldStartWithUsableEmptyIndexes() {

        EstoqueProjectionLocationProdutoData estoqueProjectionLocationProdutoData =
                EstoqueProjectionLocationProdutoData.builder().build();
        EstoqueProjectionLocationProduto estoqueProjectionLocationProduto =
                EstoqueProjectionLocationProduto.builder().build();
        EstoqueProjectionProduto estoqueProjectionProduto =
                EstoqueProjectionProduto.builder().build();

        Assertions.assertTrue(estoqueProjectionLocationProdutoData.getEstoques().isEmpty());
        Assertions.assertTrue(
                estoqueProjectionLocationProduto.getEstoques(new Produto("MAT")).isEmpty());
        Assertions.assertTrue(
                estoqueProjectionProduto.getEstoques(new Produto("MAT")).isEmpty());

    }

    @Test
    void locationMaterialDateProjectionShouldRejectBrokenAggregateBeforeIndexMutation() {

        EstoqueProjectionLocationProdutoData estoqueProjectionLocationProdutoData =
                EstoqueProjectionLocationProdutoData.builder().build();

        IllegalArgumentException nullAggregateException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> estoqueProjectionLocationProdutoData.addEstoque(null));
        Assertions.assertEquals(
                "Stock projection aggregate is required for location/material/date stock projection.",
                nullAggregateException.getMessage());

        IllegalArgumentException materialWithoutIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> estoqueProjectionLocationProdutoData.addEstoque(
                        AggregatedByLocationMaterialUOMDateImpl.builder()
                                .material(new Produto(" "))
                                .location(new Location("LOC"))
                                .referenceDate(LocalDate.of(2026, 1, 1))
                                .totalQuantity(10.0d)
                                .build()));
        Assertions.assertEquals(
                "Stock projection aggregate material id is required for location/material/date stock projection.",
                materialWithoutIdException.getMessage());

        IllegalArgumentException locationWithoutIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> estoqueProjectionLocationProdutoData.addEstoque(
                        AggregatedByLocationMaterialUOMDateImpl.builder()
                                .material(new Produto("MAT"))
                                .location(new Location(" "))
                                .referenceDate(LocalDate.of(2026, 1, 1))
                                .totalQuantity(10.0d)
                                .build()));
        Assertions.assertEquals(
                "Stock projection aggregate location id is required for location/material/date stock projection.",
                locationWithoutIdException.getMessage());

        IllegalArgumentException missingDateException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> estoqueProjectionLocationProdutoData.addEstoque(
                        AggregatedByLocationMaterialUOMDateImpl.builder()
                                .material(new Produto("MAT"))
                                .location(new Location("LOC"))
                                .totalQuantity(10.0d)
                                .build()));
        Assertions.assertEquals(
                "Stock projection aggregate reference date is required for location/material/date stock projection.",
                missingDateException.getMessage());

        IllegalArgumentException nonFiniteQuantityException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> estoqueProjectionLocationProdutoData.addEstoque(
                        AggregatedByLocationMaterialUOMDateImpl.builder()
                                .material(new Produto("MAT"))
                                .location(new Location("LOC"))
                                .referenceDate(LocalDate.of(2026, 1, 1))
                                .totalQuantity(Double.POSITIVE_INFINITY)
                                .build()));
        Assertions.assertEquals(
                "Stock projection aggregate quantity must be finite for location/material/date stock projection.",
                nonFiniteQuantityException.getMessage());

        Assertions.assertTrue(estoqueProjectionLocationProdutoData.getEstoques().isEmpty());

    }

    @Test
    void projectionsShouldAllowMissingUomBecauseFactoryHasOperationalFallback() {

        EstoqueProjectionLocationProdutoData estoqueProjectionLocationProdutoData =
                EstoqueProjectionLocationProdutoData.builder().build();
        EstoqueProjectionLocationProduto estoqueProjectionLocationProduto =
                EstoqueProjectionLocationProduto.builder().build();
        EstoqueProjectionProduto estoqueProjectionProduto =
                EstoqueProjectionProduto.builder().build();

        estoqueProjectionLocationProdutoData.addEstoque(
                AggregatedByLocationMaterialUOMDateImpl.builder()
                        .material(new Produto("MAT"))
                        .location(new Location("LOC"))
                        .referenceDate(LocalDate.of(2026, 1, 1))
                        .totalQuantity(10.0d)
                        .build());
        estoqueProjectionLocationProduto.addEstoque(
                AggregatedByLocationMaterialUOMImpl.builder()
                        .material(new Produto("MAT"))
                        .location(new Location("LOC"))
                        .totalQuantity(10.0d)
                        .build());
        estoqueProjectionProduto.addEstoque(
                AggregatedByMaterialUOMImpl.builder()
                        .material(new Produto("MAT"))
                        .totalQuantity(10.0d)
                        .build());

        Assertions.assertEquals(1, estoqueProjectionLocationProdutoData.getEstoques().size());
        Assertions.assertEquals(
                1,
                estoqueProjectionLocationProduto.getEstoques(new Produto("MAT")).size());
        Assertions.assertEquals(1, estoqueProjectionProduto.getEstoques(new Produto("MAT")).size());

    }

    @Test
    void nonDatedAndMaterialOnlyProjectionsShouldApplySameFunctionalKeyContract() {

        EstoqueProjectionLocationProduto estoqueProjectionLocationProduto =
                EstoqueProjectionLocationProduto.builder().build();
        EstoqueProjectionProduto estoqueProjectionProduto =
                EstoqueProjectionProduto.builder().build();

        IllegalArgumentException missingLocationException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> estoqueProjectionLocationProduto.addEstoque(
                        AggregatedByLocationMaterialUOMImpl.builder()
                                .material(new Produto("MAT"))
                                .totalQuantity(10.0d)
                                .build()));
        Assertions.assertEquals(
                "Stock projection aggregate location is required for location/material stock projection.",
                missingLocationException.getMessage());

        IllegalArgumentException missingMaterialQuantityException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> estoqueProjectionProduto.addEstoque(
                        AggregatedByMaterialUOMImpl.builder()
                                .material(new Produto("MAT"))
                                .build()));
        Assertions.assertEquals(
                "Stock projection aggregate quantity is required for material stock projection.",
                missingMaterialQuantityException.getMessage());

    }

}
