package com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contrato Community do valor padrao de Key Figure por DFU/data.
 *
 * <p>Key Figures padrao podem representar grandezas com sinal, como ajuste de
 * demanda. Por isso o contrato generico nao bloqueia valores negativos. O que
 * nao pode vazar para Planning Book, projections agregadas ou serializacao do
 * front e `NaN`/infinito, pois esses valores tornam totais e JSON
 * operacionalmente inconsistentes.</p>
 */
class DFUDataKeyFigurePadraoCommunityContractTest {

    @Test
    void keyFigureValueShouldTreatNullAsOperationalZero() {

        DFUDataKeyFigurePadrao dfuDataKeyFigurePadrao =
                criaDFUDataKeyFigurePadrao(null);

        Assertions.assertEquals(
                0.0d,
                dfuDataKeyFigurePadrao.getValor());

    }

    @Test
    void keyFigureValueShouldAllowNegativeFiniteValues() {

        DFUDataKeyFigurePadrao dfuDataKeyFigurePadrao =
                criaDFUDataKeyFigurePadrao(-3.5d);

        Assertions.assertEquals(
                -3.5d,
                dfuDataKeyFigurePadrao.getValor());

    }

    @Test
    void keyFigureValueShouldRejectNonFiniteValues() {

        DFUDataKeyFigurePadrao dfuDataKeyFigurePadrao =
                criaDFUDataKeyFigurePadrao(Double.NaN);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                dfuDataKeyFigurePadrao::getValor);

        Assertions.assertEquals(
                "DFU key figure value must be finite for key figure Demand Adjustment "
                        + "/ material MAT / location PLANT / reference date 2026-01-01T00:00: NaN.",
                illegalStateException.getMessage());

    }

    private static DFUDataKeyFigurePadrao criaDFUDataKeyFigurePadrao(
            Double valor) {

        return DFUDataKeyFigurePadrao.builder()
                .produto(new Produto("MAT"))
                .location(new Location("PLANT"))
                .data(LocalDateTime.of(2026, 1, 1, 0, 0))
                .keyFigure(new KeyFigureStandard(KeyFigureStandardEnum.AJUSTE_DEMANDA))
                .valor(valor)
                .build();

    }

}
