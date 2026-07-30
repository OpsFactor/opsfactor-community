package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Common contract for every vertex returned by the supply-network explorer. */
public abstract class SupplyNetworkDependencyDTO {

    public enum ElementType {
        @JsonProperty("Material-Location") MATERIAL_LOCATION,
        @JsonProperty("Production Version") PRODUCTION_VERSION,
        @JsonProperty("Routing-Bom Combination") ROUTING_BOM_COMBINATION,
        @JsonProperty("Bill of Materials") BILL_OF_MATERIALS,
        @JsonProperty("Routing") ROUTING,
        @JsonProperty("Production Resource") PRODUCTION_RESOURCE,
        @JsonProperty("Transportation Line") TRANSPORTATION_LINE
    }

    public Boolean viableStep;
    public ElementType elementType;
}
