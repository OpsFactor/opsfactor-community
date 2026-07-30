package com.opsfactor.community.capability.masterdata.product.material.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/**
 * Contrato estrutural do preco padrao mantido na tabela compartilhada de
 * material.
 */
class ProdutoUnitPriceCommunityContractTest {

    @Test
    void unitPricesShouldPreserveRegisteredValuesAndResolveSalesUomOnlyWhenRead() {

        Produto material = new Produto("MAT-UNIT-PRICE");
        UnidadeMedida unidadeMedidaSnpGlobal = new UnidadeMedida("SNP-GLOBAL");
        UnidadeMedida unidadeMedidaVendas = new UnidadeMedida("SALES-UOM");
        UnidadeMedida unidadeMedidaPreco = new UnidadeMedida("PRICE-UOM");
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setUnidadeMedidaPadraoSNP(unidadeMedidaSnpGlobal);
        material.setUnidadeMedidaVendas(unidadeMedidaVendas);
        material.setUnitGrossPrice(17.5d);
        material.setUnitNetPrice(14.0d);

        Assertions.assertEquals(17.5d, material.getUnitGrossPriceCadastrado());
        Assertions.assertEquals(14.0d, material.getUnitNetPriceCadastrado());
        Assertions.assertNull(material.getUnitPriceUnitOfMeasureCadastrada());
        Assertions.assertSame(
                unidadeMedidaVendas,
                material.getUnitPriceUnitOfMeasure(parametrosGlobais));

        material.setUnitPriceUnitOfMeasure(unidadeMedidaPreco);

        Assertions.assertSame(
                unidadeMedidaPreco,
                material.getUnitPriceUnitOfMeasureCadastrada());
        Assertions.assertSame(
                unidadeMedidaPreco,
                material.getUnitPriceUnitOfMeasure(parametrosGlobais));

    }

    @Test
    void unitPriceUomShouldRemainLazyCommunityRelationWithoutExplicitColumnName()
            throws Exception {

        Field unitPriceUnitOfMeasureField = Produto.class.getDeclaredField(
                "unitPriceUnitOfMeasure");
        ManyToOne manyToOne = unitPriceUnitOfMeasureField.getAnnotation(ManyToOne.class);

        Assertions.assertNotNull(manyToOne);
        Assertions.assertEquals(FetchType.LAZY, manyToOne.fetch());
        Assertions.assertNull(unitPriceUnitOfMeasureField.getAnnotation(JoinColumn.class));

    }

}
