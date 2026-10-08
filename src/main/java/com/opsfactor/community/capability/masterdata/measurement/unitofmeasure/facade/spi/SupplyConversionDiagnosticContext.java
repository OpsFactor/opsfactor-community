package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.spi;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.platform.calendar.Calendario;

/**
 * Read-only snapshots shared with installed edition diagnostic contributors.
 * The selected profile owns the planning calendar; extensions retain the same
 * material, location, network and demand scope as the quantitative diagnostic.
 */
public record SupplyConversionDiagnosticContext(
        VersaoMalha networkVersion,
        PerfilExecucaoSupplyPlan executionProfile,
        Calendario calendar,
        DemandPlanningProjection demand,
        SupplyNetworkProjection network,
        ClusterEParametrosProjection parameters,
        UnidadeMedidaProjection units) {

}
