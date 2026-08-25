package com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato de extração da prioridade da versão de produção simples Community.
 */
class VersaoProducaoSimplesIntegrationMapperTest {

    @Test
    void mapperShouldExportBlankPriorityWhenNoneIsRegistered() {

        VersaoProducaoSimplesIntegrationMapper mapper =
                new VersaoProducaoSimplesIntegrationMapper();
        VersaoProducaoSimples simpleProductionVersion = new VersaoProducaoSimples();
        simpleProductionVersion.setId("PV-SIMPLE");
        simpleProductionVersion.setLocation(new Location("LOC", "Location"));
        simpleProductionVersion.setMaterialOutput(new Produto("MAT"));
        simpleProductionVersion.setRoteiro(new Roteiro());
        simpleProductionVersion.getRoteiro().setId("ROUTING");
        simpleProductionVersion.setListaTecnica(new ListaTecnica());
        simpleProductionVersion.getListaTecnica().setId("BOM");

        /*
         * O fallback máximo continua disponível para heurísticas, porém o
         * contrato de data upload representa ausência de cadastro como nulo.
         */
        Assertions.assertEquals(Integer.MAX_VALUE, simpleProductionVersion.getPrioridade());
        Assertions.assertNull(mapper.getDtoWithoutPrimaryKeyFromEntity(simpleProductionVersion).priority);
        Assertions.assertNull(
                mapper.convertEntityToProcessedFileRow(simpleProductionVersion, null)
                        .getColumnValueAsInteger(2));

    }

}
