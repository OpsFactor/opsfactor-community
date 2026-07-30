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
 * Contrato estrutural dos atributos de COGS mantidos na tabela compartilhada
 * de material.
 */
class ProdutoUnitCogsCommunityContractTest {

    @Test
    void unitCogsShouldPreserveRegisteredValueAndResolveItsOwnUomBeforeGlobalSnpFallback() {

        Produto material = new Produto("MAT-UNIT-COGS");
        UnidadeMedida unidadeMedidaSnpGlobal = new UnidadeMedida("SNP-GLOBAL");
        UnidadeMedida unidadeMedidaCogs = new UnidadeMedida("COGS-UOM");
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setUnidadeMedidaPadraoSNP(unidadeMedidaSnpGlobal);

        material.setUnitCogs(17.5f);

        Assertions.assertEquals(17.5f, material.getUnitCogsCadastrado());
        Assertions.assertSame(
                unidadeMedidaSnpGlobal,
                material.getUnitCogsUnitOfMeasure(parametrosGlobais));

        material.setUnitCogsUnitOfMeasure(unidadeMedidaCogs);

        Assertions.assertSame(
                unidadeMedidaCogs,
                material.getUnitCogsUnitOfMeasureCadastrada());
        Assertions.assertSame(
                unidadeMedidaCogs,
                material.getUnitCogsUnitOfMeasure(parametrosGlobais));

    }

    @Test
    void unitCogsUomShouldRemainLazyCommunityRelationWithoutExplicitColumnName() throws Exception {

        Field unitCogsUnitOfMeasureField = Produto.class.getDeclaredField("unitCogsUnitOfMeasure");
        ManyToOne manyToOne = unitCogsUnitOfMeasureField.getAnnotation(ManyToOne.class);

        Assertions.assertNotNull(manyToOne);
        Assertions.assertEquals(FetchType.LAZY, manyToOne.fetch());
        Assertions.assertNull(unitCogsUnitOfMeasureField.getAnnotation(JoinColumn.class));

    }

}
