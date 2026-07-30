package com.opsfactor.community.platform.bi.facade.dto;

import java.time.LocalDateTime;

/** Um bucket físico fechado do Inventory Overview Community. */
public record CommunityInventoryOverviewPeriodDTO(
        LocalDateTime periodEnd,
        double constrainedProjectedStock,
        double unconstrainedProjectedStock,
        double constrainedDaysOfSupply,
        double unconstrainedDaysOfSupply) {
}
