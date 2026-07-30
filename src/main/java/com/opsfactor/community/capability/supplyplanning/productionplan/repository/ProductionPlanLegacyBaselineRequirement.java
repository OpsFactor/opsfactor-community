package com.opsfactor.community.capability.supplyplanning.productionplan.repository;

import java.time.LocalDateTime;

/**
 * Recorte escalar de uma linha de producao que ainda depende de quantidade
 * baseline depreciada.
 */
public interface ProductionPlanLegacyBaselineRequirement {

    Long getSupplyPlanId();

    String getLocationId();

    String getOutputMaterialId();

    String getProductionVersionId();

    String getRoutingId();

    String getBillOfMaterialsId();

    LocalDateTime getReferenceDate();

    Float getPlannedProductionBaselineUnconstrained();

    Float getPlannedProductionBaselineConstrained();

    Float getFirmProductionBaselineUnconstrained();

    Float getFirmProductionBaselineConstrained();

}
