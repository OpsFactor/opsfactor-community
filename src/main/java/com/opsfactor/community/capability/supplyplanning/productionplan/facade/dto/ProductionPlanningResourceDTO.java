package com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto;

import java.util.List;

/**
 * Capacity and planned-production rows for one productive resource.
 *
 * <p>Capacity values are consolidated hours for each Planning Book period.
 * The Community projection rejects quantity and shift capacity modes before
 * this DTO can be built.</p>
 */
public record ProductionPlanningResourceDTO(
        String productionResourceId,
        String description,
        List<Double> capacityHoursByPeriod,
        List<ProductionPlanningMaterialDTO> materials) {

}
