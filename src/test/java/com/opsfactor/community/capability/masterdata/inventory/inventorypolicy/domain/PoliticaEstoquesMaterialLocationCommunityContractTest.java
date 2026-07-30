package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato Community das regras material/location de politica de estoque.
 *
 * <p>Safety stock operacional permanece no Community. Ausencia de valor
 * cadastrado continua zero operacional, mas valores presentes precisam ser
 * fisicamente validos antes de alimentar o heuristico e os consumidores
 * Enterprise que reutilizam a projection.</p>
 */
class PoliticaEstoquesMaterialLocationCommunityContractTest {

    @Test
    void inventoryPolicyQuantitiesShouldTreatNullAsOperationalZero() {

        PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation =
                criaPoliticaEstoquesMaterialLocation();

        Assertions.assertEquals(
                0.0d,
                politicaEstoquesMaterialLocation.getEstoqueSegurancaDrpOuTargetKanban());
        Assertions.assertEquals(
                0.0d,
                politicaEstoquesMaterialLocation.getEstoqueMaximoDrp());

    }

    @Test
    void inventoryPolicySafetyStockShouldRejectNegativeRegisteredValue() {

        PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation =
                criaPoliticaEstoquesMaterialLocation();
        politicaEstoquesMaterialLocation.setEstoqueSegurancaDrpOuTargetKanban(-1.0d);

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        politicaEstoquesMaterialLocation::getEstoqueSegurancaDrpOuTargetKanban);

        Assertions.assertEquals(
                "Inventory policy safety stock / Kanban target must be finite and non-negative for material MAT_01 / location LOC_01: -1.0.",
                illegalStateException.getMessage());

    }

    @Test
    void inventoryPolicyMaximumStockShouldRejectNonFiniteRegisteredValue() {

        PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation =
                criaPoliticaEstoquesMaterialLocation();
        politicaEstoquesMaterialLocation.setEstoqueMaximoDrp(Double.NaN);

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        politicaEstoquesMaterialLocation::getEstoqueMaximoDrp);

        Assertions.assertEquals(
                "Inventory policy maximum DRP stock must be finite and non-negative for material MAT_01 / location LOC_01: NaN.",
                illegalStateException.getMessage());

    }

    private static PoliticaEstoquesMaterialLocation criaPoliticaEstoquesMaterialLocation() {

        PoliticaEstoques politicaEstoques = new PoliticaEstoques();
        politicaEstoques.setId("INV_POLICY_01");

        return new PoliticaEstoquesMaterialLocation(
                new PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey(
                        politicaEstoques,
                        new Produto("MAT_01"),
                        new Location("LOC_01")));

    }

}
