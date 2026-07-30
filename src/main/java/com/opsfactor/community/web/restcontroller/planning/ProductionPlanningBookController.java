package com.opsfactor.community.web.restcontroller.planning;

import com.opsfactor.community.capability.supplyplanning.productionplan.facade.ProductionPlanningBookFacade;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningBookDTO;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningBookUpdateDTO;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Community REST surface for the basic Production Planning Book.
 *
 * <p>Only current/future Working Plan planned-production adjustments are
 * published here. Gantt, line scheduling, firm production orders, setup and
 * maintenance remain absent and must be supplied by Enterprise overlays.</p>
 */
@Slf4j
@RestController
public class ProductionPlanningBookController {

    /** Carrega e atualiza o livro de produção Community pelo fluxo central. */
    @Autowired
    private ProductionPlanningBookFacade productionPlanningBookFrontService;

    /**
     * Loads capacity hours and planned production for one Community location.
     */
    @GetMapping("api/secured/planning/production/planningbook")
    @Secured(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)
    public ResponseEntity<ProductionPlanningBookDTO> getProductionPlanningBook(
            @RequestParam String locationId,
            @RequestParam Long supplyPlanId) {

        try {
            return ResponseEntity.ok(
                    productionPlanningBookFrontService.getProductionPlanningBook(
                            locationId,
                            supplyPlanId));
        } catch (RuntimeException runtimeException) {
            log.error(
                    "Error loading Community Production Planning Book for location {} and Supply Plan {}",
                    locationId,
                    supplyPlanId,
                    runtimeException);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    runtimeException.getMessage(),
                    runtimeException);
        }

    }

    /**
     * Atomically replaces planned production in one Working Plan book cell.
     */
    @PostMapping("api/secured/planning/production/planningbook/update")
    @Secured(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)
    public ResponseEntity<ProductionPlanningBookDTO> updatePlannedProduction(
            @RequestBody ProductionPlanningBookUpdateDTO productionPlanningBookUpdateDTO) {

        try {
            return ResponseEntity.ok(
                    productionPlanningBookFrontService.updatePlannedProduction(
                            productionPlanningBookUpdateDTO));
        } catch (RuntimeException runtimeException) {
            log.error("Error updating Community Production Planning Book", runtimeException);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    runtimeException.getMessage(),
                    runtimeException);
        }

    }

}
