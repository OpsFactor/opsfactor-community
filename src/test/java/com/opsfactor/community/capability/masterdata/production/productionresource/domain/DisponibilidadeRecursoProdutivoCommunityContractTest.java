package com.opsfactor.community.capability.masterdata.production.productionresource.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

/**
 * Contratos Community de disponibilidade diaria de recurso produtivo.
 */
class DisponibilidadeRecursoProdutivoCommunityContractTest {

    @Test
    void availableHoursShouldTreatNullAsOperationalAbsence() {

        DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo =
                criaDisponibilidadeRecursoProdutivo();

        Assertions.assertEquals(
                0.0f,
                disponibilidadeRecursoProdutivo.getHorasDisponiveis());

    }

    @Test
    void availableHoursShouldRejectNegativeValue() {

        DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo =
                criaDisponibilidadeRecursoProdutivo();
        disponibilidadeRecursoProdutivo.setHorasDisponiveis(-1.0f);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                disponibilidadeRecursoProdutivo::getHorasDisponiveis);

        Assertions.assertEquals(
                "Production resource available hours must be finite and non-negative for resource RES / date 2026-01-01: -1.0.",
                illegalStateException.getMessage());

    }

    @Test
    void quantityCapacityShouldRejectNonFiniteValue() {

        DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo =
                criaDisponibilidadeRecursoProdutivo();
        disponibilidadeRecursoProdutivo.setCapacidadeEmQuantidade(Float.NaN);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                disponibilidadeRecursoProdutivo::getCapacidadeEmQuantidade);

        Assertions.assertEquals(
                "Production resource quantity capacity must be finite and non-negative for resource RES / date 2026-01-01: NaN.",
                illegalStateException.getMessage());

    }

    private static DisponibilidadeRecursoProdutivo criaDisponibilidadeRecursoProdutivo() {

        RecursoProdutivo recursoProdutivo =
                new RecursoProdutivo();
        recursoProdutivo.setId("RES");

        return new DisponibilidadeRecursoProdutivo(
                new DisponibilidadeRecursoProdutivo.DisponibilidadeRecursoProdutivoCompositeKey(
                        recursoProdutivo,
                        LocalDate.of(2026, 1, 1)));

    }
}
