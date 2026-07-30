package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;

/** Inbound transportation alternative and the material at its origin. */
public class TransportationLineDependencyDTO extends SupplyNetworkDependencyDTO {

    public String originLocationId;
    public String destinationLocationId;
    public String materialId;
    public Boolean active;
    public MaterialLocationDependencyDTO materialAtOriginLocationDependency;
}
