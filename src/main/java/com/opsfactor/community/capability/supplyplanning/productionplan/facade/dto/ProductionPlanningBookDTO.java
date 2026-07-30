package com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Typed Community contract for the operational production Planning Book.
 *
 * <p>The book is deliberately a Working Plan read: it exposes only daily-hour
 * capacity and planned production by productive resource and material. Firm
 * production orders, scheduling rows, setup, maintenance and Gantt state are
 * Enterprise concerns and are intentionally absent from this contract.</p>
 */
public record ProductionPlanningBookDTO(
        Long supplyPlanId,
        String locationId,
        List<LocalDateTime> periodEndDates,
        List<ProductionPlanningResourceDTO> resources) {

}
