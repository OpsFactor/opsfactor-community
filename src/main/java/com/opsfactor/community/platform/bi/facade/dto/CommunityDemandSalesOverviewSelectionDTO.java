package com.opsfactor.community.platform.bi.facade.dto;

import com.opsfactor.community.platform.utility.Constantes;

import java.util.List;
import java.util.Map;

/**
 * Seleção explícita da visão Community de vendas e Demand Plan.
 *
 * <p>A visão aceita um Demand Plan opcional. Sem plano, publica somente uma
 * série histórica do documento explicitamente escolhido; com plano, acrescenta
 * a série quantitativa irrestrita. Filtros vazios representam o escopo ativo
 * completo do snapshot. IDs e características públicas podem restringir as
 * duas dimensões; valores financeiros e agrupamentos privados permanecem fora
 * do contrato Community.</p>
 */
public record CommunityDemandSalesOverviewSelectionDTO(
        Long demandPlanId,
        Constantes.TipoDocumentoVenda historicalSalesDocumentType,
        String unitOfMeasureId,
        Integer historicalPeriods,
        List<String> materialIds,
        List<String> locationIds,
        Map<String, List<String>> valuesByMaterialCharacteristicId,
        Map<String, List<String>> valuesByLocationCharacteristicId) {
}
