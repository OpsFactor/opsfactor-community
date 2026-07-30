package com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto;

/**
 * Typed command that replaces the current Working Plan planned inbound transfer
 * for exactly one origin, destination and material.
 *
 * <p>The command does not accept a plan type, firm flag, stock-days target or
 * aggregate material list. Those choices would reopen Enterprise or separate
 * future capabilities at the public Community boundary.</p>
 */
public record DeploymentOperationalInboundUpdateDTO(
        Long supplyPlanId,
        String originLocationId,
        String destinationLocationId,
        String materialId,
        Double plannedInboundQuantity) {

}
