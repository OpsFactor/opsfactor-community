package com.opsfactor.community.capability.masterdata.production.productionresource.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos Community do recurso produtivo.
 */
class RecursoProdutivoCommunityContractTest {

    @Test
    void efficiencyShouldDefaultToOneWhenUnset() {

        RecursoProdutivo recursoProdutivo =
                criaRecursoProdutivo();

        Assertions.assertEquals(
                1.0f,
                recursoProdutivo.getEficiencia());

    }

    @Test
    void efficiencyShouldRejectZeroBecauseItIsUsedAsCapacityDivisor() {

        RecursoProdutivo recursoProdutivo =
                criaRecursoProdutivo();
        recursoProdutivo.setEficiencia(0.0f);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                recursoProdutivo::getEficiencia);

        Assertions.assertEquals(
                "Production resource efficiency must be finite and positive for resource RES: 0.0.",
                illegalStateException.getMessage());

    }

    @Test
    void efficiencyShouldRejectNonFiniteValue() {

        RecursoProdutivo recursoProdutivo =
                criaRecursoProdutivo();
        recursoProdutivo.setEficiencia(Float.NaN);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                recursoProdutivo::getEficiencia);

        Assertions.assertEquals(
                "Production resource efficiency must be finite and positive for resource RES: NaN.",
                illegalStateException.getMessage());

    }

    private static RecursoProdutivo criaRecursoProdutivo() {

        RecursoProdutivo recursoProdutivo =
                new RecursoProdutivo();
        recursoProdutivo.setId("RES");
        return recursoProdutivo;

    }
}
