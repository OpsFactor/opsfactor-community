package com.opsfactor.community.web.dto.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Definicao generica de coluna AG Grid usada por telas Community.
 *
 * <p>Colunas especificas de Planning Book usam DTO proprio; este contrato
 * permanece para relatorios e grades simples.</p>
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AgGridColumnDefDTO extends DTO {

    public enum FilterType {
        @JsonProperty("agTextColumnFilter") TEXTO,
        @JsonProperty("agNumberColumnFilter") NUMERO,
        @JsonProperty("true") GERAL,
        @JsonProperty("false") DESABILITADO
    }
    
    public String headerName;
    public String field;
    public FilterType filter;
    public Boolean editable;
    
    public List<AgGridColumnDefDTO> children;
    
}
