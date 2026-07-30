package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Contratos Community das conversoes por material.
 *
 * <p>Demand Planning, Supply Planning e Planning Book usam a projection de UOM
 * como infraestrutura fisica comum. Conversoes especificas por material podem
 * sobrescrever a conversao global, mas uma linha especifica incompleta nao deve
 * quebrar fallback global com NPE nem ser aceita silenciosamente quando a
 * projection tenta materializa-la como conversao propria.</p>
 */
class ConversaoUnidadeProdutoCommunityContractTest {

    @Test
    void materialConversionWithoutOwnQuantityShouldFallbackToDefaultConversionWhenDefaultsAreProvided() {

        UnidadeMedida unidadeMedidaOrigem = new UnidadeMedida("KG");
        UnidadeMedida unidadeMedidaDestino = new UnidadeMedida("UN");
        Produto material = new Produto("MAT-1");

        ConversaoUnidadeProduto conversaoUnidadeProdutoSemRazaoPropria = new ConversaoUnidadeProduto(
                new ConversaoUnidadeProduto.ConversaoUnidadeProdutoCompositeKey(
                        material,
                        unidadeMedidaOrigem,
                        unidadeMedidaDestino));

        ConversaoUnidade conversaoUnidadePadrao = new ConversaoUnidade(
                new ConversaoUnidade.ConversaoUnidadeCompositeKey(
                        unidadeMedidaOrigem,
                        unidadeMedidaDestino));
        conversaoUnidadePadrao.setQuantidadeUnidadeOrigem(1.0);
        conversaoUnidadePadrao.setQuantidadeUnidadeDestino(2.5);

        /*
         * Este overload e usado quando uma conversao especifica por material
         * pode delegar para a conversao global equivalente. Antes do guard, a
         * linha especifica incompleta tentava dividir null/null antes de chegar
         * ao fallback.
         */
        Assertions.assertEquals(
                2.5,
                conversaoUnidadeProdutoSemRazaoPropria.getQuantidadeUnidadeDestinoPorUnidadeOrigem(
                        List.of(conversaoUnidadePadrao)),
                0.0001d);

    }

    @Test
    void materialConversionWithoutOwnQuantityShouldFailExplicitlyWhenConsumedAsOwnConversion() {

        UnidadeMedida unidadeMedidaOrigem = new UnidadeMedida("KG");
        UnidadeMedida unidadeMedidaDestino = new UnidadeMedida("UN");
        Produto material = new Produto("MAT-1");

        ConversaoUnidadeProduto conversaoUnidadeProdutoSemRazaoPropria = new ConversaoUnidadeProduto(
                new ConversaoUnidadeProduto.ConversaoUnidadeProdutoCompositeKey(
                        material,
                        unidadeMedidaOrigem,
                        unidadeMedidaDestino));

        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                conversaoUnidadeProdutoSemRazaoPropria::getQuantidadeUnidadeDestinoPorUnidadeOrigem);

        Assertions.assertEquals(
                "Material-level UOM conversion has no quantity ratio from KG to UN for material MAT-1",
                unitOfMeasureConversionException.getMessage());

    }

    @Test
    void materialConversionWithoutOwnQuantityShouldRejectMissingDefaultConversions() {

        UnidadeMedida unidadeMedidaOrigem = new UnidadeMedida("KG");
        UnidadeMedida unidadeMedidaDestino = new UnidadeMedida("UN");
        Produto material = new Produto("MAT-1");

        ConversaoUnidadeProduto conversaoUnidadeProdutoSemRazaoPropria = new ConversaoUnidadeProduto(
                new ConversaoUnidadeProduto.ConversaoUnidadeProdutoCompositeKey(
                        material,
                        unidadeMedidaOrigem,
                        unidadeMedidaDestino));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> conversaoUnidadeProdutoSemRazaoPropria
                        .getQuantidadeUnidadeDestinoPorUnidadeOrigem(null));

        Assertions.assertEquals(
                "Default UOM conversions are required when material-level conversion has no own ratio",
                illegalArgumentException.getMessage());

    }

    @Test
    void materialOwnRatioResolversShouldDeclareNullableContracts() throws NoSuchMethodException {

        Method getQuantidadeUnidadeDestinoPorUnidadeOrigemPropriaOuNullMethod =
                ConversaoUnidadeProduto.class.getDeclaredMethod(
                        "getQuantidadeUnidadeDestinoPorUnidadeOrigemPropriaOuNull");
        Method getQuantidadeUnidadeDestinoPorUnidadeOrigemMethod =
                ConversaoUnidadeProduto.class.getDeclaredMethod(
                        "getQuantidadeUnidadeDestinoPorUnidadeOrigem",
                        Produto.class,
                        UnidadeMedida.class,
                        UnidadeMedida.class,
                        Collection.class,
                        Collection.class);

        /*
         * Conversao especifica por material pode estar ausente e, nesse caso,
         * o fluxo com fallback usa a conversao global. Quando nenhuma conversao
         * existe, o metodo estatico tambem devolve null para que o caller decida
         * se ausencia real e aceitavel no contexto do calculo.
         */
        Assertions.assertTrue(
                getQuantidadeUnidadeDestinoPorUnidadeOrigemPropriaOuNullMethod.isAnnotationPresent(Nullable.class),
                "Conversao propria por material incompleta deve declarar @Nullable no helper interno.");
        Assertions.assertTrue(
                getQuantidadeUnidadeDestinoPorUnidadeOrigemMethod.isAnnotationPresent(Nullable.class),
                "Lookup estatico de conversao por material deve declarar @Nullable quando nenhuma conversao existe.");

    }

    @Test
    void materialConversionShouldFailExplicitlyWhenDeprecatedAndCanonicalRatiosConflict() {

        ConversaoUnidadeProduto conversaoUnidadeProduto = new ConversaoUnidadeProduto(
                new ConversaoUnidadeProduto.ConversaoUnidadeProdutoCompositeKey(
                        new Produto("MAT-1"),
                        new UnidadeMedida("KG"),
                        new UnidadeMedida("UN")));
        conversaoUnidadeProduto.setQuantidadeUnidadeDestinoPorUnidadeOrigem(2.0);
        conversaoUnidadeProduto.setQuantidadeUnidadeOrigem(1.0);
        conversaoUnidadeProduto.setQuantidadeUnidadeDestino(3.0);

        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                conversaoUnidadeProduto::getQuantidadeUnidadeDestinoPorUnidadeOrigem);

        Assertions.assertEquals(
                "Material-level UOM conversion has conflicting deprecated and canonical quantity ratios from KG to UN for material MAT-1",
                unitOfMeasureConversionException.getMessage());
        Assertions.assertEquals(
                UnitOfMeasureConversionLegacyRatioState.CONFLICTING_LEGACY_AND_CANONICAL_RATIOS,
                conversaoUnidadeProduto.getLegacyRatioState());

    }

}
