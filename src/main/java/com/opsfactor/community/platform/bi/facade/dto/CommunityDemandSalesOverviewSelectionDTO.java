package com.opsfactor.community.platform.bi.facade.dto;

import java.util.List;

/**
 * Seleção explícita da visão Community de vendas e Demand Plan.
 *
 * <p>A visão é deliberadamente limitada a um Demand Plan e a séries
 * quantitativas no nível material/location. Filtros vazios representam o
 * escopo ativo completo do snapshot; valores preenchidos são ids explícitos,
 * nunca valores de característica ou agrupamentos Enterprise.</p>
 */
public record CommunityDemandSalesOverviewSelectionDTO(
        Long demandPlanId,
        String unitOfMeasureId,
        Integer historicalPeriods,
        List<String> materialIds,
        List<String> locationIds) {
}
