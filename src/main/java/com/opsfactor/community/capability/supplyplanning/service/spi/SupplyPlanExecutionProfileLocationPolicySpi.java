package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;

/**
 * Optional Enterprise extension point for resolving effective Supply Planning
 * generation flags by location.
 *
 * <p>The Community projection first derives its values from the shared parent
 * profile. An Enterprise overlay may replace only that already typed local
 * policy through a detached batch snapshot; private JPA entities never cross
 * this boundary.</p>
 */
public interface SupplyPlanExecutionProfileLocationPolicySpi {

    /**
     * Builds one resolver for the whole heuristic execution of a persisted
     * profile. Implementations must perform any private loading before the
     * returned resolver is used in the low-level-code loop.
     */
    SupplyPlanningProjectionLocationPolicy getSupplyPlanningProjectionLocationPolicy(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan);

    /**
     * Applies the typed effective policy to one already constructed Community
     * projection.
     */
    interface SupplyPlanningProjectionLocationPolicy {

        void applyTo(SupplyPlanningProjection supplyPlanningProjection);

    }

}
