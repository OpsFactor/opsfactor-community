package com.opsfactor.community.capability.supplyplanning.inventoryplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Contratos Community da projection de safety stock consumida por rotinas e
 * modelos internos.
 *
 * <p>A projection nao recalcula politica: ela apenas escolhe a fonte fisica da
 * quantidade. `QUANTITY` vem da politica de estoques; `DAYS` deve ter sido
 * convertido em quantidade por etapa anterior. Qualquer projection incompleta
 * precisa falhar aqui com erro de contrato, nao com NPE.</p>
 */
class SafetyStockProjectionCommunityContractTest {

    @Test
    void quantityCalculationShouldReadQuantityDirectlyFromInventoryPolicy() {

        Produto material = new Produto("MAT-01");
        Location location = new Location("LOC-01");
        SafetyStockProjection safetyStockProjection = new SafetyStockProjection(
                location,
                new FakePoliticaEstoquesProjection(Constantes.SNPCalculoSafetyStock.QUANTITY, 42.0d),
                Map.of(material, Map.of(3, 12.0d)));

        Assertions.assertEquals(
                42.0d,
                safetyStockProjection.getQuantidadeEstoqueSeguranca(3, material));

    }

    @Test
    void daysCalculationShouldReadPreviouslyResolvedPhysicalQuantity() {

        Produto material = new Produto("MAT-02");
        Location location = new Location("LOC-02");
        SafetyStockProjection safetyStockProjection = new SafetyStockProjection(
                location,
                new FakePoliticaEstoquesProjection(Constantes.SNPCalculoSafetyStock.DAYS, 42.0d),
                Map.of(material, Map.of(4, 19.5d)));

        Assertions.assertEquals(
                19.5d,
                safetyStockProjection.getQuantidadeEstoqueSeguranca(4, material));

    }

    @Test
    void missingCalculationModelShouldFailBeforeSwitchingPhysicalQuantitySource() {

        Produto material = new Produto("MAT-03");
        Location location = new Location("LOC-03");
        SafetyStockProjection safetyStockProjection = new SafetyStockProjection(
                location,
                new FakePoliticaEstoquesProjection(null, 42.0d),
                Map.of(material, Map.of(5, 19.5d)));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> safetyStockProjection.getQuantidadeEstoqueSeguranca(5, material));

        Assertions.assertTrue(illegalStateException.getMessage().contains(
                "SafetyStockProjection requires QUANTITY or DAYS safety stock calculation"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("calculation model=null"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("material=MAT-03"));
        Assertions.assertTrue(illegalStateException.getMessage().contains("location=LOC-03"));

    }

    @Test
    void multiLocationProjectionShouldTreatNullCollectionAsEmptySnapshot() {

        SafetyStockMultiplasLocationsProjection safetyStockMultiplasLocationsProjection =
                new SafetyStockMultiplasLocationsProjection(null);

        Assertions.assertTrue(
                safetyStockMultiplasLocationsProjection
                        .getMapaSafetyStockProjectionPorLocation()
                        .isEmpty());
        Assertions.assertFalse(
                safetyStockMultiplasLocationsProjection
                        .verificaSeHaPoliticaEstoquesMaterialLocationCadastrada());

    }

    @Test
    void multiLocationProjectionShouldRejectNullItemBeforeIndexingByLocation() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new SafetyStockMultiplasLocationsProjection(Collections.singletonList(null)));

        Assertions.assertEquals(
                "SafetyStockMultiplasLocationsProjection received null safety stock projection at index 0.",
                illegalArgumentException.getMessage());

    }

    @Test
    void multiLocationProjectionShouldRejectProjectionWithoutLocationBeforeIndexingByLocation() {

        SafetyStockProjection safetyStockProjectionSemLocation = new SafetyStockProjection(
                null,
                null,
                null);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new SafetyStockMultiplasLocationsProjection(List.of(safetyStockProjectionSemLocation)));

        Assertions.assertEquals(
                "SafetyStockMultiplasLocationsProjection received safety stock projection without location at index 0.",
                illegalArgumentException.getMessage());

    }

    @Test
    void multiLocationProjectionShouldRejectDuplicatedLocationBeforeOverwrite() {

        Location location = new Location("LOC-DUP");
        SafetyStockProjection primeiraSafetyStockProjection = new SafetyStockProjection(
                location,
                null,
                null);
        SafetyStockProjection segundaSafetyStockProjection = new SafetyStockProjection(
                location,
                null,
                null);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new SafetyStockMultiplasLocationsProjection(List.of(
                        primeiraSafetyStockProjection,
                        segundaSafetyStockProjection)));

        Assertions.assertEquals(
                "SafetyStockMultiplasLocationsProjection received duplicated safety stock projection for location LOC-DUP.",
                illegalArgumentException.getMessage());

    }

    private static class FakePoliticaEstoquesProjection extends PoliticaEstoquesProjection {

        private final Constantes.SNPCalculoSafetyStock snpCalculoSafetyStock;
        private final double estoqueSeguranca;

        private FakePoliticaEstoquesProjection(
                Constantes.SNPCalculoSafetyStock snpCalculoSafetyStock,
                double estoqueSeguranca) {

            this.snpCalculoSafetyStock = snpCalculoSafetyStock;
            this.estoqueSeguranca = estoqueSeguranca;

        }

        @Override
        public boolean verificaSeHaPoliticaEstoquesMaterialLocationCadastrada() {

            return true;

        }

        @Override
        public Constantes.SNPCalculoSafetyStock getSNPModeloCalculoSafetyStock(
                int posicaoPeriodo,
                Produto material,
                Location location) {

            return snpCalculoSafetyStock;

        }

        @Override
        public double getSNPEstoqueSegurancaDrpOuTargetKanban(
                int posicaoPeriodo,
                Produto material,
                Location location) {

            return estoqueSeguranca;

        }

    }

}
