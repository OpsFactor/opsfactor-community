package com.opsfactor.community.platform.bi.facade.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta read-only da visão Community de histórico de vendas e Demand Plan.
 */
public record CommunityDemandSalesOverviewDTO(
        List<LocalDateTime> periods,
        List<CommunityDemandSalesOverviewPeriodDTO> data) {
}
