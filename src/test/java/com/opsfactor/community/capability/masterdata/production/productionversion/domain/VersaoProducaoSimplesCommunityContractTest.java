package com.opsfactor.community.capability.masterdata.production.productionversion.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos Community da versao de producao simples.
 */
class VersaoProducaoSimplesCommunityContractTest {

    @Test
    void constructorShouldRejectRoutingAndBillOfMaterialsWithDifferentOutputs() {

        Location location = new Location("LOC");
        Roteiro roteiro = new Roteiro();
        roteiro.setLocation(location);
        roteiro.setMaterialOutput(new Produto("MAT-ROUTING"));
        ListaTecnica listaTecnica = new ListaTecnica();
        listaTecnica.setLocation(location);
        listaTecnica.setMaterialOutput(new Produto("MAT-BOM"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> new VersaoProducaoSimples(
                        "PV",
                        location,
                        1,
                        roteiro,
                        listaTecnica));

        Assertions.assertEquals(
                "Bill of Materials output material MAT-BOM different than version material MAT-ROUTING",
                illegalStateException.getMessage());

    }

    @Test
    void constructorShouldRejectMissingRoutingWithExplicitMessage() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new VersaoProducaoSimples(
                        "PV",
                        new Location("LOC"),
                        1,
                        null,
                        new ListaTecnica()));

        Assertions.assertEquals(
                "Simple production version routing is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void constructorShouldRejectMissingBillOfMaterialsWithExplicitMessage() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new VersaoProducaoSimples(
                        "PV",
                        new Location("LOC"),
                        1,
                        new Roteiro(),
                        null));

        Assertions.assertEquals(
                "Simple production version bill of materials is required",
                illegalArgumentException.getMessage());

    }

}
