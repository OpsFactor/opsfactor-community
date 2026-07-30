package com.opsfactor.community.capability.lowlevelcode.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;

@Data 
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DFUMalhaCircularDTO {
    
    // Transportation Line ou Bill of Materials
    public String masterData;
    public String masterDataId;
        
    public Integer lowLevelCode;
    
    public Integer circularNetworkId;
        
    public String materialId;
    public String outputMaterialId;
    
    
}
