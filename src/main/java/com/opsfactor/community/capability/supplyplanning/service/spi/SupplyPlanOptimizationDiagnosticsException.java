package com.opsfactor.community.capability.supplyplanning.service.spi;

/**
 * The optimization failed, but the plan must remain available for inspecting
 * the requested optimizer diagnostics.
 */
public final class SupplyPlanOptimizationDiagnosticsException extends RuntimeException {

    public SupplyPlanOptimizationDiagnosticsException(
            Long supplyPlanId, RuntimeException cause, boolean diagnosticsSaved) {

        super("Optimization failed for Supply Plan " + supplyPlanId
                + (diagnosticsSaved ? ". Optimizer diagnostics saved. " : ". Saving optimizer diagnostics failed. ")
                + cause.getMessage(), cause);

    }
}
