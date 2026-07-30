package com.opsfactor.community.capability.planningbook.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Identidade tecnica de um pai agregado do Planning Book Enterprise.
 *
 * <p>Os quatro identificadores permitem que o servidor refaca a associacao
 * das DFUs dentro da view autorizada. Caracteristicas exibidas na grade sao
 * deliberadamente excluidas: elas descrevem o grupo, mas nao sao sua chave de
 * selecao.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlanningBookParentSelectionDTO(
        String materialAggregationLevelId,
        String locationAggregationLevelId,
        String materialAggregationValueId,
        String locationAggregationValueId) {
}
