package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;

/**
 * Optional extension point for resolving the location scope of a Supply Plan
 * execution profile.
 *
 * <p>The Community always creates its own complete location projection first.
 * An Enterprise overlay may then return a narrower immutable projection backed
 * by private configuration. Keeping the common contract expressed only in
 * terms of projections prevents private JPA entities from becoming a reverse
 * relation of the shared execution profile.</p>
 */
public interface SupplyPlanExecutionProfileLocationScopeSpi {

    /**
     * Resolves the effective execution scope for one already loaded profile.
     *
     * <p>The supplied projection is the Community scope after its standard
     * demand-propagation filter. Implementations may only remove locations from
     * that scope; adding a location would bypass the shared active-location and
     * propagation rules.</p>
     */
    LocationProjection getLocationProjectionParaExecucaoSupplyPlan(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            ClusterEParametrosProjection clusterEParametrosProjection,
            LocationProjection locationProjectionCommunity);

}
