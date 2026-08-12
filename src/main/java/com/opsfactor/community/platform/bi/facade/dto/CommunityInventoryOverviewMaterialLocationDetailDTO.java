package com.opsfactor.community.platform.bi.facade.dto;

import java.util.Map;

/**
 * Fotografia física de uma combinação material-location do Inventory Overview.
 *
 * <p>As séries permanecem na unidade solicitada e correspondem exatamente aos
 * mesmos buckets publicados no agregado. Os mapas de características só
 * transportam valores públicos já presentes no snapshot; não introduzem eixo
 * financeiro, write-off ou outra capability privada.</p>
 */
public record CommunityInventoryOverviewMaterialLocationDetailDTO(
        String locationId,
        String locationDescription,
        String materialId,
        String materialDescription,
        Map<String, String> valuesByLocationCharacteristicId,
        Map<String, String> valuesByMaterialCharacteristicId,
        double[] constrainedProjectedStock,
        double[] unconstrainedProjectedStock,
        double[] constrainedConsumption,
        double[] unconstrainedConsumption) {
}
