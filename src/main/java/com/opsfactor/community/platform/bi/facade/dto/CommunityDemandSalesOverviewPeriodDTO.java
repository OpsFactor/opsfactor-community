package com.opsfactor.community.platform.bi.facade.dto;

import java.time.LocalDateTime;

/**
 * Valor quantitativo de Sales e Demand Plan por DFU e fechamento de período.
 *
 * <p>Um item só é publicado quando Sales ou Unconstrained Plan for diferente
 * de zero. A lista de períodos da resposta mantém o eixo temporal completo
 * sem materializar o produto cartesiano de todos os DFUs.</p>
 */
public record CommunityDemandSalesOverviewPeriodDTO(
        String locationId,
        String materialId,
        LocalDateTime referenceDate,
        double historicalSales,
        double unconstrainedPlan) {
}
