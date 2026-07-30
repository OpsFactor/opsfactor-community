package com.opsfactor.community.web.restcontroller.dataupload.masterdata.inventorypolicy;

import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesMaterialLocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.service.PoliticaEstoquesMaterialLocationIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data upload Community do detalhe material/location de politica operacional de estoque.
 *
 * <p>O contrato cobre parametros de safety stock, DRP e Kanban simples. Campos
 * de Inventory Policy Optimization Enterprise, como frequencia de
 * reabastecimento, ficam fora do arquivo Community e sao rejeitados pelo mapper
 * se enviados via JSON.</p>
 */
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Inventory Policy")
})
@RestController
public class InventoryPolicyDetailIntegrationController extends IntegrationControllerAbstract<PoliticaEstoquesMaterialLocation, PoliticaEstoquesMaterialLocationIntegrationDataDto, PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO, EmptyIntegrationDataFilter, PoliticaEstoquesMaterialLocationIntegrationService, IntegrationOptionsDto> {

    /**
     * Service concreto usado pelos endpoints de detalhe de politica de estoque.
     */
    @Autowired
    private PoliticaEstoquesMaterialLocationIntegrationService inventoryPolicyDetailIntegrationService;

    @Override
    protected String getSubPath() {

        return "inventorypolicydetail";

    }

}
