package com.opsfactor.community.capability.configuration.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

/**
 * Contratos Community dos parametros material/location usados por planejamento.
 */
class ParametrosProdutoLocationCommunityContractTest {

    @Test
    void productionMinimumLotShouldTreatNullAsOperationalAbsence() {

        ParametrosProdutoLocation parametrosProdutoLocation =
                criaParametrosProdutoLocation();

        Assertions.assertTrue(parametrosProdutoLocation.getLoteMinimoProducao().isEmpty());

    }

    @Test
    void productionMultipleShouldReturnPositiveValueWhenRegistered() {

        ParametrosProdutoLocation parametrosProdutoLocation =
                criaParametrosProdutoLocation();
        parametrosProdutoLocation.setMultiploProducao(8.0d);

        OptionalDouble optionalMultiploProducao =
                parametrosProdutoLocation.getMultiploProducao();

        Assertions.assertTrue(optionalMultiploProducao.isPresent());
        Assertions.assertEquals(
                8.0d,
                optionalMultiploProducao.getAsDouble());

    }

    @Test
    void frozenDemandPlanningHorizonShouldTreatNullAsZeroDays() {

        ParametrosProdutoLocation parametrosProdutoLocation =
                criaParametrosProdutoLocation();

        Assertions.assertEquals(
                0,
                parametrosProdutoLocation.getNumeroDiasHorizonteCongeladoDp());

    }

    @Test
    void frozenDemandPlanningHorizonShouldRejectNegativeValue() {

        ParametrosProdutoLocation parametrosProdutoLocation =
                criaParametrosProdutoLocation();
        parametrosProdutoLocation.setNumeroDiasHorizonteCongeladoDp(-1);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                parametrosProdutoLocation::getNumeroDiasHorizonteCongeladoDp);

        Assertions.assertEquals(
                "Frozen Demand Planning horizon must be non-negative for material MAT / location PLANT: -1.",
                illegalStateException.getMessage());

    }

    private static ParametrosProdutoLocation criaParametrosProdutoLocation() {

        return new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(
                        new Produto("MAT"),
                        new Location("PLANT")));

    }
}
