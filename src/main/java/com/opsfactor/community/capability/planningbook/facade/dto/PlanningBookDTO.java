package com.opsfactor.community.capability.planningbook.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * DTO principal do Planning Book Community.
 * <p>
 * A edicao aberta publica Planning Book em nivel material/location, com key
 * figures padrao e ajustes diretos pela tela. Niveis agregados configuraveis,
 * selecao dinamica de KFs e apresentacao por caracteristicas pertencem ao
 * Enterprise.
 */

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanningBookDTO {

    public String viewName;
    public ConfiguredView.TipoView viewType;
    public Boolean autoSubmitChanges;
    
    public List<String> keyFigures;
    
    public List<ColumnDefDTO> columnDefs;
    
    public List<GroupDTO> groups;
    
    public Map<String,String> additionalParameters;
    
    public List<String> periodList;
    
    public String bucketSize;
        
    public String uom;
    
    public List<String> errorMessage;

}
