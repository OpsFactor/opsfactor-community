package com.opsfactor.community.capability.supplyplanning.inventoryplan.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contrato Community das linhas de estoque do Supply Plan.
 *
 * <p>Safety stock, estoque maximo e estoque em transito sao quantidades
 * fisicas usadas pelo heuristico Community e por consumidores Enterprise. Elas
 * preservam `null` como ausencia operacional zero para compatibilidade de
 * snapshots, mas valores presentes negativos ou nao finitos indicam dado
 * corrompido. Estoque projetado fica fora deste teste porque pode carregar uma
 * semantica propria de falta/posicao de estoque que sera revisada em recorte
 * separado.</p>
 */
class InventoryPlanLinhaCommunityContractTest {

    @Test
    void nonProjectedInventoryQuantitiesShouldTreatNullAsOperationalZero() {

        InventoryPlanLinha inventoryPlanLinha =
                criaInventoryPlanLinha();

        Assertions.assertEquals(
                0.0d,
                inventoryPlanLinha.getQuantidadeEstoqueTransitoInbound());
        Assertions.assertEquals(
                0.0d,
                inventoryPlanLinha.getQuantidadeEstoqueSegurancaIrrestrito());
        Assertions.assertEquals(
                0.0d,
                inventoryPlanLinha.getQuantidadeEstoqueMaximoIrrestrito());

    }

    @Test
    void inboundTransitStockShouldRejectNegativeRegisteredValue() {

        InventoryPlanLinha inventoryPlanLinha =
                criaInventoryPlanLinha();
        inventoryPlanLinha.setQuantidadeEstoqueTransitoInbound(-1.0d);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                inventoryPlanLinha::getQuantidadeEstoqueTransitoInbound);

        Assertions.assertEquals(
                "Inventory plan quantity inbound transit stock must be finite and non-negative for "
                        + "material MAT / location PLANT / reference date 2026-01-01T00:00: -1.0.",
                illegalStateException.getMessage());

    }

    @Test
    void inboundTransitStockShouldRejectNonFiniteRegisteredValue() {

        InventoryPlanLinha inventoryPlanLinha =
                criaInventoryPlanLinha();
        inventoryPlanLinha.setQuantidadeEstoqueTransitoInbound(Double.NaN);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                inventoryPlanLinha::getQuantidadeEstoqueTransitoInbound);

        Assertions.assertEquals(
                "Inventory plan quantity inbound transit stock must be finite and non-negative for "
                        + "material MAT / location PLANT / reference date 2026-01-01T00:00: NaN.",
                illegalStateException.getMessage());

    }

    @Test
    void safetyStockShouldRejectNegativeRegisteredValue() {

        InventoryPlanLinha inventoryPlanLinha =
                criaInventoryPlanLinha();
        inventoryPlanLinha.setQuantidadeEstoqueSegurancaIrrestrito(-1.0d);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                inventoryPlanLinha::getQuantidadeEstoqueSegurancaIrrestrito);

        Assertions.assertEquals(
                "Inventory plan quantity unrestricted safety stock must be finite and non-negative for "
                        + "material MAT / location PLANT / reference date 2026-01-01T00:00: -1.0.",
                illegalStateException.getMessage());

    }

    @Test
    void maximumStockShouldRejectNonFiniteRegisteredValue() {

        InventoryPlanLinha inventoryPlanLinha =
                criaInventoryPlanLinha();
        inventoryPlanLinha.setQuantidadeEstoqueMaximoRestrito(Double.POSITIVE_INFINITY);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                inventoryPlanLinha::getQuantidadeEstoqueMaximoRestrito);

        Assertions.assertEquals(
                "Inventory plan quantity restricted maximum stock must be finite and non-negative for "
                        + "material MAT / location PLANT / reference date 2026-01-01T00:00: Infinity.",
                illegalStateException.getMessage());

    }

    @Test
    void maximumStockShouldNeverBeLowerThanSafetyStock() {

        InventoryPlanLinha inventoryPlanLinha =
                criaInventoryPlanLinha();
        inventoryPlanLinha.setQuantidadeEstoqueSegurancaIrrestrito(15.0d);
        inventoryPlanLinha.setQuantidadeEstoqueMaximoIrrestrito(10.0d);

        Assertions.assertEquals(
                15.0d,
                inventoryPlanLinha.getEstoqueMaximoIrrestrito());

    }

    private static InventoryPlanLinha criaInventoryPlanLinha() {

        InventoryPlanLinha.InventoryPlanLinhaCompositeKey inventoryPlanLinhaCompositeKey =
                new InventoryPlanLinha.InventoryPlanLinhaCompositeKey(
                        new SupplyPlan(),
                        new Location("PLANT"),
                        new Produto("MAT"),
                        LocalDateTime.of(2026, 1, 1, 0, 0));

        return new InventoryPlanLinha(inventoryPlanLinhaCompositeKey);

    }

}
