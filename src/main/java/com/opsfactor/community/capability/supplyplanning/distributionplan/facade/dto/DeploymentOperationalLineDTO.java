package com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One operational deployment decision for an origin, destination and material.
 *
 * <p>The Community contract reports only the current Working Plan planned
 * inbound transfer. It intentionally excludes order portfolio, loading,
 * aggregation, costs, analytics and firm quantities.</p>
 */
public record DeploymentOperationalLineDTO(
        Long supplyPlanId,
        String originLocationId,
        String destinationLocationId,
        String materialId,
        String materialDescription,
        LocalDateTime currentPeriodEndDate,
        int leadTimeDays,
        LocalDate expectedReceiptDate,
        String unitOfMeasureId,
        double plannedInboundQuantity) {

}
