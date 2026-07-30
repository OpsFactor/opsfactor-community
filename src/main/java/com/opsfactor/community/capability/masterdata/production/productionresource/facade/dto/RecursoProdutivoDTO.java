package com.opsfactor.community.capability.masterdata.production.productionresource.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;

@Data 
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecursoProdutivoDTO {
    
    public String productionResourceId;
    public String locationId;
    public String description;
    public Boolean active;
    public Float efficiency;
    
}
