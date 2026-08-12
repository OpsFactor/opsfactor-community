package com.opsfactor.community.platform.bi.facade.dto;

import java.util.List;
import java.util.Map;

/**
 * Recorte físico do Inventory Overview Community.
 *
 * <p>A unidade é obrigatória porque estoque e consumo são agregados antes do
 * cálculo de cobertura. Não há eixo monetário nesta superfície: COGS e
 * write-off são capabilities Enterprise e não pertencem ao contrato físico.</p>
 */
public record CommunityInventoryOverviewSelectionDTO(
        Long supplyPlanId,
        String unitOfMeasureId,
        List<String> materialIds,
        List<String> locationIds,
        Map<String, List<String>> valuesByMaterialCharacteristicId,
        Map<String, List<String>> valuesByLocationCharacteristicId,
        CommunityInventoryOverviewPostHorizonPolicy postHorizonPolicy) {
}
