package com.opsfactor.community.capability.masterdata.production.operation.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperacaoRoteiroDTO {

    private String routingId;
        
    private Integer operationPosition;
        
    private String productionResourceId;
    
    private Double operationDuration;

    /** S, M, H ou D; ausência significa H. */
    private String timeUnit;
    
}
