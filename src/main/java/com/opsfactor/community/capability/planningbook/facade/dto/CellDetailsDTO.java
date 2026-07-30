package com.opsfactor.community.capability.planningbook.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.platform.utility.Constantes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO enviado ao front quando o usuario abre os detalhes de uma celula.
 *
 * <p>No Community os detalhes permanecem presos ao plano atual, material,
 * location, key figure e periodo. Detalhes derivados de workflows privados,
 * causa raiz de restricoes ou visoes agregadas devem ser adicionados pelo
 * Enterprise quando essas capabilities forem migradas.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CellDetailsDTO {

    public Long planId;
    public String viewName;
    
    public String materialId;
    public String locationId;
    
    public KeyFigureStandardEnum keyFigure;
    public Constantes.TipoPlano tipoPlano;
    
    public LocalDate period;
    
    public List<Map<String,Object>> detailLines = new ArrayList<>();

    public List<Map<String,Object>> columnDefs = new ArrayList<>();
    
}
