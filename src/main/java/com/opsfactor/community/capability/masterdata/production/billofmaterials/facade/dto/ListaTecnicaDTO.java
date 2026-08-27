package com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListaTecnicaDTO {

    private String id;
        
    private String description;
        
    private String outputMaterialId;
    
    private String outputUnitOfMeasureId;
    
    private Double outputQuantity;
        
    private Boolean active;
    
}
