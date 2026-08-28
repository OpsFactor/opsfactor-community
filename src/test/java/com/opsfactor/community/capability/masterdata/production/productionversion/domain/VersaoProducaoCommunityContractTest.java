package com.opsfactor.community.capability.masterdata.production.productionversion.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Contratos da entidade única de versão de produção. */
class VersaoProducaoCommunityContractTest {

    @Test
    void shouldRejectRoutingAndBillOfMaterialsWithDifferentOutputs() {

        Location location = new Location("LOC");
        Roteiro roteiro = criaRoteiro(location, new Produto("MAT-ROUTING"));
        ListaTecnica listaTecnica = criaListaTecnica(location, new Produto("MAT-BOM"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> new VersaoProducao("PV", location, 1, roteiro, listaTecnica));

        Assertions.assertEquals(
                "Routing and BOM output materials differ",
                illegalStateException.getMessage());

    }

    @Test
    void shouldRejectMissingGenericMasterReferences() {

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> new VersaoProducao(
                        "PV",
                        new Location("LOC"),
                        1,
                        null,
                        null));

        Assertions.assertEquals(
                "Production version requires location, routing and BOM",
                illegalStateException.getMessage());

    }

    @Test
    void sentinelShouldFailExplicitlyWhenConsumedAsAProductiveVersion() {

        VersaoProducao sentinela = new VersaoProducao();
        sentinela.setId(VersaoProducao.ID_VERSAO_PRODUCAO_VAZIA);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                sentinela::getRoteiros);

        Assertions.assertEquals(
                "Production version sentinel does not expose productive master data",
                illegalStateException.getMessage());

    }

    private static Roteiro criaRoteiro(Location location, Produto materialOutput) {

        Roteiro roteiro = new Roteiro();
        roteiro.setLocation(location);
        roteiro.setMaterialOutput(materialOutput);
        return roteiro;

    }

    private static ListaTecnica criaListaTecnica(Location location, Produto materialOutput) {

        ListaTecnica listaTecnica = new ListaTecnica();
        listaTecnica.setLocation(location);
        listaTecnica.setMaterialOutput(materialOutput);
        return listaTecnica;

    }

}
