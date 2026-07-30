package com.opsfactor.community.web.restcontroller.dataupload.planning.supply;

import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.dto.InventoryPlanIntegrationDataDto;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.service.InventoryPlanIntegrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller Community read-only do Inventory Plan simples por Supply Plan.
 */
@Tags({
        @Tag(name = "Supply Planning"),
        @Tag(name = "Inventory Plan")
})
@RestController
public class InventoryPlanIntegrationController {

    /**
     * Service Community concreto usado pelos endpoints filtrados por plano.
     */
    @Autowired
    private InventoryPlanIntegrationService inventoryPlanIntegrationService;

    /**
     * Baixa arquivo de Inventory Plan de um Supply Plan especifico.
     */
    @GetMapping("api/secured/data/file/inventoryplan/{supplyPlanId}")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getInventoryPlanFile(
            @PathVariable("supplyPlanId") Long supplyPlanId) {

        return inventoryPlanIntegrationService.getFile(supplyPlanId);

    }

    /**
     * Baixa JSON de Inventory Plan de um Supply Plan especifico.
     */
    @GetMapping("api/secured/data/inventoryplan/{supplyPlanId}")
    @Secured("ROLE_ADMIN")
    public List<InventoryPlanIntegrationDataDto> getInventoryPlanJson(
            @PathVariable("supplyPlanId") Long supplyPlanId) {

        return inventoryPlanIntegrationService.getInventoryPlanDTOList(supplyPlanId);

    }

}
