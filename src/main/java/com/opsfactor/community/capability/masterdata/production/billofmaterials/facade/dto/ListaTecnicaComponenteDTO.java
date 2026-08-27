package com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListaTecnicaComponenteDTO {

    private String billOfMaterialsId;
        
    private String componentMaterialId;
        
    private String componentMaterialUnitOfMeasureId;
    
    private Double quantity;
    
}
