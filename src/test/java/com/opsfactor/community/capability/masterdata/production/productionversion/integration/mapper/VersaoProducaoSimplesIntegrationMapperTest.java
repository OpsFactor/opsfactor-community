package com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato de extração da prioridade da entidade única de versão de produção.
 */
class VersaoProducaoIntegrationMapperTest {

    @Test
    void mapperShouldExportBlankPriorityWhenNoneIsRegistered() {

        VersaoProducaoIntegrationMapper mapper =
                new VersaoProducaoIntegrationMapper();
        VersaoProducao versaoProducao = new VersaoProducao();
        versaoProducao.setId("PV");
        versaoProducao.setLocation(new Location("LOC", "Location"));
        Produto outputMaterial = new Produto("MAT");
        versaoProducao.setRoteiro(new Roteiro());
        versaoProducao.getRoteiro().setId("ROUTING");
        versaoProducao.getRoteiro().setMaterialOutput(outputMaterial);
        versaoProducao.setListaTecnica(new ListaTecnica());
        versaoProducao.getListaTecnica().setId("BOM");
        versaoProducao.getListaTecnica().setMaterialOutput(outputMaterial);

        /*
         * O fallback máximo continua disponível para heurísticas, porém o
         * contrato de data upload representa ausência de cadastro como nulo.
         */
        Assertions.assertEquals(Integer.MAX_VALUE, versaoProducao.getPrioridade());
        Assertions.assertNull(mapper.getDtoWithoutPrimaryKeyFromEntity(versaoProducao).priority);
        Assertions.assertNull(
                mapper.convertEntityToProcessedFileRow(versaoProducao, null)
                        .getColumnValueAsInteger(2));

    }

}
