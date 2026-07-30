package com.opsfactor.community.web.restcontroller.dataupload.masterdata.inventorypolicy;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.service.PoliticaEstoquesIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community do cabecalho de politica operacional de estoque.
 *
 * <p>O controller publica o subpath legado {@code inventorypolicy} apenas para
 * prioridade e vigencia. O detalhe material/location possui controller proprio
 * para manter o contrato de arquivo e JSON separado.</p>
 */
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Inventory Policy")
})
@RestController
public class InventoryPolicyIntegrationController extends IntegrationControllerAbstract<PoliticaEstoques, PoliticaEstoquesIntegrationDataDto, PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, PoliticaEstoquesIntegrationService, IntegrationOptionsDto> {

    /**
     * Service concreto usado pelos endpoints de cabecalho de politica de estoque.
     */
    @Autowired
    private PoliticaEstoquesIntegrationService inventoryPolicyIntegrationService;

    @Override
    protected String getSubPath() {

        return "inventorypolicy";

    }

}
