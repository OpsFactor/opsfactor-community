package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;

import java.util.LinkedHashSet;
import java.util.Set;

/** Production-version node, including synthetic diagnostic versions. */
public class ProductionVersionDependencyDTO extends SupplyNetworkDependencyDTO {

    public String productionVersionId;
    public Boolean active;
    public Boolean parallelRoutingsOmitted;
    public Integer omittedParallelRoutingCount;
    public Set<RoutingBomCombinationDependencyDTO> routingAndBomCombinationDependencies = new LinkedHashSet<>();
}
