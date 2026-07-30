package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;

import java.util.LinkedHashSet;
import java.util.Set;

/** Bill of materials and the component material-location nodes it consumes. */
public class BillOfMaterialsDependencyDTO extends SupplyNetworkDependencyDTO {

    public String bomId;
    public Boolean active;
    public Set<MaterialLocationDependencyDTO> bomComponentDependencies = new LinkedHashSet<>();
}
