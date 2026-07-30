package com.opsfactor.community.capability.supplyplanning.service.spi;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;

/**
 * Optional extension point for narrowing the material scope of a Supply Plan
 * execution profile.
 *
 * <p>The Community first creates its standard complete material projection. An
 * Enterprise overlay may only remove materials from that already valid scope;
 * it cannot introduce materials which were not active in the shared snapshot.
 * The interface deliberately exchanges only Community projections, keeping
 * private material-filter entities out of the shared profile and its JPA
 * mapping.</p>
 */
public interface SupplyPlanExecutionProfileMaterialScopeSpi {

    /**
     * Resolves the effective material scope for an already loaded profile.
     *
     * <p>When the private overlay is absent, callers retain
     * {@code materialProjectionCommunity} unchanged. Implementations must
     * preserve the supplied cluster/parameter snapshot and may only narrow its
     * active material set.</p>
     */
    MaterialProjection getMaterialProjectionParaExecucaoSupplyPlan(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            ClusterEParametrosProjection clusterEParametrosProjection,
            MaterialProjection materialProjectionCommunity);

}
