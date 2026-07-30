package com.opsfactor.community.platform.bi.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Contrato JSON da matriz de fluxos físicos usada pela visualização de
 * material flows.
 *
 * <p>Os nomes dos campos são deliberadamente os mesmos do endpoint legado:
 * cada posição em {@link #locationAndColorList} identifica simultaneamente a
 * linha e a coluna de {@link #flowData}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommunityMaterialFlowsDTO {

    /** Locations ordenadas e suas cores de apresentação. */
    public List<CommunityMaterialFlowsLocationAndColorDTO> locationAndColorList = new ArrayList<>();

    /** Matriz quadrada de quantidades irrestritas por origem e destino. */
    public List<List<Double>> flowData = new ArrayList<>();
}
