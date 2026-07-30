package com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Builder;

@Data 
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InconsistenciaReceitaProducaoDTO {
    
    public String productionRoutingId;
    public Integer lastOperationPosition;
    
    public String productionRoutingOutputMaterial;
    public String operationBillOfMaterials;
    public String operationBillOfMaterialsOutputMaterial;
    
    public String inconsistency;

}
