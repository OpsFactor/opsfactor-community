package com.opsfactor.community.platform.bi.facade.dto;

import java.util.List;

/**
 * Séries físicas agregadas de estoque e Days of Supply do Inventory Overview.
 *
 * <p>A cobertura parte do saldo observado no fim de cada período e consome
 * somente buckets posteriores. Ela não incorpora recebimentos futuros,
 * transferências internas, COGS ou write-off.</p>
 */
public record CommunityInventoryOverviewDTO(
        String unitOfMeasureId,
        List<CommunityInventoryOverviewPeriodDTO> periods,
        List<Double> daysInPeriod,
        List<CommunityInventoryOverviewMaterialLocationDetailDTO> materialLocationDetails) {
}
