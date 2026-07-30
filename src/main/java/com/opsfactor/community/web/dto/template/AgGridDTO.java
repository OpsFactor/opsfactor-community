package com.opsfactor.community.web.dto.template;

import java.util.List;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Envelope generico para respostas tabulares consumidas pelo AG Grid.
 *
 * <p>Para usar o builder com generics, manter a forma
 * {@code AgGridDTO.<ClasseData>builder()}.</p>
 */
@SuperBuilder
@NoArgsConstructor
public class AgGridDTO <DATA extends DTO> extends DTO {
    
    public List<AgGridColumnDefDTO> columnDefs;
    public List<DATA> data;
        
}
