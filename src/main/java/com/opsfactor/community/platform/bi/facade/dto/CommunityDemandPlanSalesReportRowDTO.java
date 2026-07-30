package com.opsfactor.community.platform.bi.facade.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Linha financeira e quantitativa do relatório de vendas por Demand Plan.
 *
 * <p>O contrato identifica a DFU, o fechamento do período e a unidade de
 * medida antes dos valores. A montagem da linha fica fora deste DTO: a borda
 * que a produzir deve entregar valores já agregados e convertidos para a UOM
 * declarada, sem acoplar a resposta a entidades JPA.</p>
 */
public record CommunityDemandPlanSalesReportRowDTO(
        String productId,
        String locationId,
        LocalDateTime periodReferenceDate,
        String unitOfMeasureId,
        double quantity,
        double grossSales,
        double netSales,
        double cogs) implements Serializable {
}
