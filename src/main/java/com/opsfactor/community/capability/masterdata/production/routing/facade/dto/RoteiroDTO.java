package com.opsfactor.community.capability.masterdata.production.routing.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoteiroDTO {

    private String id;
        
    private String description;
        
    private Integer priority;
    
    private String locationId;
    
    private String outputMaterialId;

    /**
     * Valor explicitamente configurado para permitir o uso do roteiro sem
     * versão de produção. {@code null} preserva o default efetivo da entidade.
     */
    private Boolean canBeUsedWithoutProductionVersion;
    
    private Boolean active;
    
}
