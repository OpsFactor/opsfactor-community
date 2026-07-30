package com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto;

import java.time.LocalDateTime;

/**
 * Typed command that replaces planned production for one Community book cell.
 *
 * <p>The target is always the Working Plan and always represents a planned
 * order. Neither plan type nor firm/scheduling attributes are accepted from
 * the client, so a forged payload cannot reopen private production features.</p>
 */
public record ProductionPlanningBookUpdateDTO(
        Long supplyPlanId,
        String locationId,
        String materialId,
        String productionResourceId,
        LocalDateTime periodEndDate,
        Double plannedQuantity) {

}
