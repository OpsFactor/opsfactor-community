package com.opsfactor.community.capability.masterdata.demand.dfu.facade.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Identificador material/location de uma DFU exposta ao front.
 */

@Data
@Builder
public class DFUDTO {

    public String locationId;
    public String materialId;
    
}
