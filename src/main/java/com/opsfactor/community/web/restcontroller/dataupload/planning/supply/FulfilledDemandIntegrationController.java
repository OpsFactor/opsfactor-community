package com.opsfactor.community.web.restcontroller.dataupload.planning.supply;

import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.dto.FulfilledDemandIntegrationDataDto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.service.FulfilledDemandIntegrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller Community read-only da demanda atendida por Supply Plan.
 */
@Tags({
        @Tag(name = "Supply Planning"),
        @Tag(name = "Fulfilled Demand")
})
@RestController
public class FulfilledDemandIntegrationController {

    /** Service do contrato fisico e simplificado de atendimento. */
    @Autowired
    private FulfilledDemandIntegrationService fulfilledDemandIntegrationService;

    /** Retorna matriz tabular para download XLSX ou CSV no front-end. */
    @GetMapping("api/secured/data/file/fulfilleddemand/{supplyPlanId}/{unitOfMeasureId}")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getFulfilledDemandFile(
            @PathVariable("supplyPlanId") Long supplyPlanId,
            @PathVariable("unitOfMeasureId") String unitOfMeasureId) {

        return fulfilledDemandIntegrationService.getFile(supplyPlanId, unitOfMeasureId);

    }

    /** Retorna JSON fisico da demanda atendida pelo plano. */
    @GetMapping("api/secured/data/fulfilleddemand/{supplyPlanId}/{unitOfMeasureId}")
    @Secured("ROLE_ADMIN")
    public List<FulfilledDemandIntegrationDataDto> getFulfilledDemandJson(
            @PathVariable("supplyPlanId") Long supplyPlanId,
            @PathVariable("unitOfMeasureId") String unitOfMeasureId) {

        return fulfilledDemandIntegrationService.getFulfilledDemandDtoList(
                supplyPlanId,
                unitOfMeasureId);

    }

}
