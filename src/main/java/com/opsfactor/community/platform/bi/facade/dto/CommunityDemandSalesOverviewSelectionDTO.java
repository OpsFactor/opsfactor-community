package com.opsfactor.community.platform.bi.facade.dto;

import com.opsfactor.community.platform.utility.Constantes;

import java.time.LocalDateTime;
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
        List<LocalDateTime> demandPlanPeriodReferenceDates,
        List<String> materialIds,
        List<String> locationIds,
        Map<String, List<String>> valuesByMaterialCharacteristicId,
        Map<String, List<String>> valuesByLocationCharacteristicId) {

    /**
     * Preserva o contrato anterior para consumidores que ainda não enviam o
     * filtro temporal. Uma lista vazia significa todo o horizonte do plano.
     */
    public CommunityDemandSalesOverviewSelectionDTO(
            Long demandPlanId,
            Constantes.TipoDocumentoVenda historicalSalesDocumentType,
            String unitOfMeasureId,
            Integer historicalPeriods,
            List<String> materialIds,
            List<String> locationIds,
            Map<String, List<String>> valuesByMaterialCharacteristicId,
            Map<String, List<String>> valuesByLocationCharacteristicId) {

        this(
                demandPlanId,
                historicalSalesDocumentType,
                unitOfMeasureId,
                historicalPeriods,
                List.of(),
                materialIds,
                locationIds,
                valuesByMaterialCharacteristicId,
                valuesByLocationCharacteristicId);

    }
}
