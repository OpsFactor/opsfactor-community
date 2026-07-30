package com.opsfactor.community.capability.supplyplanning.distributionplan.repository;

import java.time.LocalDateTime;

/**
 * Recorte escalar de uma transferencia que ainda depende de colunas baseline
 * depreciadas do Distribution Plan.
 */
public interface DistributionPlanLegacyBaselineRequirement {

    Long getSupplyPlanId();

    String getOriginLocationId();

    String getDestinationLocationId();

    String getMaterialId();

    LocalDateTime getShippingDate();

    LocalDateTime getReceivingDate();

    Float getPlannedOrderBaselineUnconstrained();

    Float getPlannedOrderBaselineConstrained();

    Float getFirmOrderBaselineUnconstrained();

    Float getFirmOrderBaselineConstrained();

}
