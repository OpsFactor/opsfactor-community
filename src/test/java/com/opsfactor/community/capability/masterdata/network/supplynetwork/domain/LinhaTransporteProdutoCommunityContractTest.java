package com.opsfactor.community.capability.masterdata.network.supplynetwork.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

/**
 * Contratos Community dos overrides fisicos de transporte por material.
 */
class LinhaTransporteProdutoCommunityContractTest {

    @Test
    void minimumLotShouldInheritTransportationLineWhenMaterialOverrideIsNull() {

        LinhaTransporte linhaTransporte =
                criaLinhaTransporte();
        linhaTransporte.setLoteMinimoTransporte(20.0d);
        LinhaTransporteProduto linhaTransporteProduto =
                criaLinhaTransporteProduto(linhaTransporte);

        Assertions.assertEquals(
                20.0d,
                linhaTransporteProduto.getLoteMinimoTransporte());

    }

    @Test
    void leadTimeOverrideShouldRejectNegativeValueInsteadOfMaskingAsZero() {

        LinhaTransporteProduto linhaTransporteProduto =
                criaLinhaTransporteProduto(criaLinhaTransporte());
        linhaTransporteProduto.setLeadTimeDias(-1);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                linhaTransporteProduto::getLeadTimeDias);

        Assertions.assertEquals(
                "Transportation line material lead time days must be non-negative for ORIGIN -> DEST / material MAT: -1.",
                illegalStateException.getMessage());

    }

    @Test
    void multipleShouldInheritTransportationLineWhenMaterialOverrideIsNull() {

        LinhaTransporte linhaTransporte =
                criaLinhaTransporte();
        linhaTransporte.setMultiploTransporte(5.0d);
        LinhaTransporteProduto linhaTransporteProduto =
                criaLinhaTransporteProduto(linhaTransporte);

        OptionalDouble optionalMultiploTransporte =
                linhaTransporteProduto.getMultiploTransporte();

        Assertions.assertTrue(optionalMultiploTransporte.isPresent());
        Assertions.assertEquals(
                5.0d,
                optionalMultiploTransporte.getAsDouble());

    }

    private static LinhaTransporte criaLinhaTransporte() {

        return new LinhaTransporte(
                new LinhaTransporte.LinhaTransporteCompositeKey(
                        new VersaoMalha("NETWORK"),
                        new Location("ORIGIN"),
                        new Location("DEST")));

    }

    private static LinhaTransporteProduto criaLinhaTransporteProduto(
            LinhaTransporte linhaTransporte) {

        return new LinhaTransporteProduto(
                new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                        linhaTransporte,
                        new Produto("MAT")));

    }
}
