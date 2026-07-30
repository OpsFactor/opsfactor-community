package com.opsfactor.community.capability.supplyplanning.inventoryplan.repository;

import java.time.LocalDateTime;

/**
 * Recorte escalar de uma linha de Inventory Plan que ainda possui quantidade
 * efetiva em alguma coluna baseline depreciada.
 *
 * <p>O preflight usa somente ids, data e valores escalares. Assim ele explica
 * o bloqueio sem materializar entidades ou disparar carregamentos lazy.</p>
 */
public interface InventoryPlanLegacyBaselineRequirement {

    Long getSupplyPlanId();

    String getLocationId();

    String getMaterialId();

    LocalDateTime getReferenceDate();

    Float getSafetyStockBaselineUnconstrained();

    Float getMaximumStockBaselineUnconstrained();

    Float getSafetyStockBaselineConstrained();

    Float getMaximumStockBaselineConstrained();

    Float getProjectedStockBaselineUnconstrained();

    Float getProjectedStockBaselineConstrained();

}
