package com.opsfactor.community.web.restcontroller.planning;

import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.DeploymentOperationalFacade;
import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto.DeploymentOperationalInboundUpdateDTO;
import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto.DeploymentOperationalLineDTO;
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
 * Community API for one physical deployment transfer at a time.
 */
@Slf4j
@RestController
public class DeploymentOperationalController {

    /** Fornece a leitura e a edição atômica da transferência operacional. */
    @Autowired
    private DeploymentOperationalFacade deploymentOperationalFrontService;

    /**
     * Reads a current Working Plan planned transfer by origin, destination and material.
     */
    @GetMapping("api/secured/planning/supply/deployment")
    @Secured(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)
    public ResponseEntity<DeploymentOperationalLineDTO> getDeploymentOperationalLine(
            @RequestParam Long supplyPlanId,
            @RequestParam String originLocationId,
            @RequestParam String destinationLocationId,
            @RequestParam String materialId) {

        try {
            return ResponseEntity.ok(
                    deploymentOperationalFrontService.getDeploymentOperationalLine(
                            supplyPlanId,
                            originLocationId,
                            destinationLocationId,
                            materialId));
        } catch (RuntimeException runtimeException) {
            log.error("Error loading Community deployment operational line", runtimeException);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    runtimeException.getMessage(),
                    runtimeException);
        }

    }

    /**
     * Atomically replaces the current planned inbound transfer for one route.
     */
    @PostMapping("api/secured/planning/supply/deployment/update")
    @Secured(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)
    public ResponseEntity<DeploymentOperationalLineDTO> updatePlannedInbound(
            @RequestBody DeploymentOperationalInboundUpdateDTO deploymentOperationalInboundUpdateDTO) {

        try {
            return ResponseEntity.ok(
                    deploymentOperationalFrontService.updatePlannedInbound(
                            deploymentOperationalInboundUpdateDTO));
        } catch (RuntimeException runtimeException) {
            log.error("Error updating Community deployment operational line", runtimeException);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    runtimeException.getMessage(),
                    runtimeException);
        }

    }
}
