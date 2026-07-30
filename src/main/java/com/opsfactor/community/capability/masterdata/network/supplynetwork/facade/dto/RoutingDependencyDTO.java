package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;

import java.util.LinkedHashSet;
import java.util.Set;

/** Routing and the productive resources required by it. */
public class RoutingDependencyDTO extends SupplyNetworkDependencyDTO {

    public String routingId;
    public Boolean active;
    public Set<ProductionResourceDependencyDTO> productionResourceDependencies = new LinkedHashSet<>();
}
