package com.opsfactor.community.capability.planningbook.keyfigure.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Modo padrao de edicao de uma Key Figure no Planning Book.
 *
 * <p>O Community usa apenas os modos necessarios para edicao material/location
 * direta. Modos de detalhe/agregacao permanecem no enum porque o front
 * compartilhado pode decodificar catalogos Enterprise, mas a selecao efetiva
 * e filtrada pelos catalogos RuntimeInfo/Planning Book da edicao.</p>
 */
public enum EditMode {
    
    @JsonProperty("noEdit") NOEDIT,
    @JsonProperty("cellEdit") CELLEDIT,
    @JsonProperty("detailOrCellEdit") DETAIL_OR_CELL_EDIT,
    @JsonProperty("detailDisaggregatedOnly") DETAIL_DISAGGREGATED_ONLY,
    @JsonProperty("detailAggregatedDisaggregated") DETAIL_AGGREGATED_DISAGGREGATED;
    
}
