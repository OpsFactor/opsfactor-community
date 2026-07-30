package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;

/** One routing/BOM pair selected by a production version. */
public class RoutingBomCombinationDependencyDTO extends SupplyNetworkDependencyDTO {

    public RoutingDependencyDTO routingDependency;
    public BillOfMaterialsDependencyDTO bomDependency;
    public Boolean parallelRoutingsOmitted;
    public Integer omittedParallelRoutingCount;
}
