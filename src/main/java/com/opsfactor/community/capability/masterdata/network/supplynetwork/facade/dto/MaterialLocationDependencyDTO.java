package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;

import java.util.LinkedHashSet;
import java.util.Set;

/** Material-location node and its inbound and production alternatives. */
public class MaterialLocationDependencyDTO extends SupplyNetworkDependencyDTO {

    public String materialId;
    public String locationId;
    public Boolean active;
    public Boolean viableProduction;
    public Boolean viableInbound;
    public Boolean recursionCut;
    public Integer depth;
    public Set<ProductionVersionDependencyDTO> productionVersionDependencies = new LinkedHashSet<>();
    public Set<TransportationLineDependencyDTO> inboundTransportationLineDependencies = new LinkedHashSet<>();
}
