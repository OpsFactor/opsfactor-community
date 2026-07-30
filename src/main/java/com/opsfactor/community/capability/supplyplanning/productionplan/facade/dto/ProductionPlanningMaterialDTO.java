package com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto;

import java.util.List;

/**
 * Planned production for one material on one productive resource.
 *
 * <p>The values contain only the planned component of the Working Plan. Firm
 * orders are not added, exposed or editable in Community.</p>
 */
public record ProductionPlanningMaterialDTO(
        String materialId,
        String description,
        String unitOfMeasureId,
        List<Double> plannedQuantityByPeriod) {

}
