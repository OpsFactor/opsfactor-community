package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain;

import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * Contratos Community das conversoes globais de unidade.
 */
class ConversaoUnidadeCommunityContractTest {

    @Test
    void globalConversionWithoutQuantityShouldFailExplicitly() {

        ConversaoUnidade conversaoUnidadeSemRazao = new ConversaoUnidade(
                new ConversaoUnidade.ConversaoUnidadeCompositeKey(
                        new UnidadeMedida("KG"),
                        new UnidadeMedida("UN")));

        /*
         * A projection de UOM consome conversoes globais diretamente. Cadastro
         * incompleto precisa produzir erro funcional legivel, nao NullPointer
         * durante divisao ou unboxing para double.
         */
        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                conversaoUnidadeSemRazao::getQuantidadeUnidadeDestinoPorUnidadeOrigem);

        Assertions.assertEquals(
                "Global UOM conversion has no quantity ratio from KG to UN",
                unitOfMeasureConversionException.getMessage());

    }

    @Test
    void globalOwnRatioResolverShouldDeclareNullableContract() throws NoSuchMethodException {

        Method getQuantidadeUnidadeDestinoPorUnidadeOrigemOuNullMethod =
                ConversaoUnidade.class.getDeclaredMethod(
                        "getQuantidadeUnidadeDestinoPorUnidadeOrigemOuNull");

        /*
         * O helper privado usa null apenas como sinal interno de linha global
         * incompleta. O metodo publico converte esse estado em erro funcional,
         * preservando mensagens claras para projections e calculos.
         */
        Assertions.assertTrue(
                getQuantidadeUnidadeDestinoPorUnidadeOrigemOuNullMethod.isAnnotationPresent(Nullable.class),
                "Conversao global incompleta deve declarar @Nullable no helper interno.");

    }

    @Test
    void globalConversionShouldFailExplicitlyWhenDeprecatedAndCanonicalRatiosConflict() {

        ConversaoUnidade conversaoUnidade = new ConversaoUnidade(
                new ConversaoUnidade.ConversaoUnidadeCompositeKey(
                        new UnidadeMedida("KG"),
                        new UnidadeMedida("UN")));
        conversaoUnidade.setQuantidadeUnidadeDestinoPorUnidadeOrigem(2.0);
        conversaoUnidade.setQuantidadeUnidadeOrigem(1.0);
        conversaoUnidade.setQuantidadeUnidadeDestino(3.0);

        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                conversaoUnidade::getQuantidadeUnidadeDestinoPorUnidadeOrigem);

        Assertions.assertEquals(
                "Global UOM conversion has conflicting deprecated and canonical quantity ratios from KG to UN",
                unitOfMeasureConversionException.getMessage());
        Assertions.assertEquals(
                UnitOfMeasureConversionLegacyRatioState.CONFLICTING_LEGACY_AND_CANONICAL_RATIOS,
                conversaoUnidade.getLegacyRatioState());

    }

}
