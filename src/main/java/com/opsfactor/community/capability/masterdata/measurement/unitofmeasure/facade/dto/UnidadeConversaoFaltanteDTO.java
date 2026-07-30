package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@Data 
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
public class UnidadeConversaoFaltanteDTO {
        
    public String originUnitOfMeasure;
    public String targetUnitOfMeasure;
    
    public String locationId;
    public String materialId;
    
    public Constantes.TaskTipo originTask;
    public Constantes.TaskTipo targetTask;
    
    public NecessidadeConversao originConversionRequirementType;
    public String originConversionRequirementId;
    
    public NecessidadeConversao targetConversionRequirementType;
    public String targetConversionRequirementId;
    
    
    // Enum para distinção da causa da falta de conversão
    public enum NecessidadeConversao {
        @JsonProperty("Production Routing Operation UOM") ROTEIRO_OPERACAO,
        @JsonProperty("Bill of Materials output UOM") LISTA_TECNICA_OUTPUT,
        @JsonProperty("Bill of Materials component UOM") LISTA_TECNICA_COMPONENTE,
        @JsonProperty("Material/Location Default UOM") PADRAO_MATERIAL_LOCATION,
        @JsonProperty("Material Default UOM") PADRAO_MATERIAL,
        @JsonProperty("Outbound requisition") REQUISICAO_OUTBOUND,
        @JsonProperty("Transfer lot size / multiple") MINIMO_MULTIPLO_TRANSFERENCIA,
        @JsonProperty("Demand Plan UOM") PLANO_DEMANDA,
        @JsonProperty("Stock UOM") ESTOQUE,
        @JsonProperty("Sales UOM") VENDAS,
        @JsonProperty("Planning Book / Report UOM") PLANNING_BOOK_OU_RELATORIO,
        @JsonProperty("Expedition UOM") EXPEDICAO,
        @JsonProperty("Transfer UOM") TRANSFERENCIA;
        
    }
    
}
