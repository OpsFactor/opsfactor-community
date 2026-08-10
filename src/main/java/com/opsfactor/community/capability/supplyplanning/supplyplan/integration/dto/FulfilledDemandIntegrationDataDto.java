package com.opsfactor.community.capability.supplyplanning.supplyplan.integration.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Linha fisica e read-only de atendimento da demanda de um Supply Plan.
 *
 * <p>O contrato Community publica somente quantidades operacionais. Pedidos,
 * clientes, precos, custos, margens e demais dimensoes comerciais permanecem
 * fora desta extracao.</p>
 */
@Value
@Builder
public class FulfilledDemandIntegrationDataDto {

    /** Supply Plan que originou a fotografia de atendimento. */
    Long supplyPlanId;

    /** Location em que a demanda direta foi considerada. */
    String locationId;

    /** Descricao publica da location. */
    String locationDescription;

    /** Material demandado. */
    String materialId;

    /** Descricao publica do material. */
    String materialDescription;

    /** Data de referencia do bucket de planejamento. */
    LocalDateTime referenceDate;

    /** Unidade fisica das quantidades exportadas. */
    String unitOfMeasureId;

    /** Demanda que o plano irrestrito tentaria atender. */
    Double unconstrainedDemand;

    /** Parcela atendida pelo plano depois das restricoes. */
    Double fulfilledDemand;

    /** Parcela nao atendida depois das restricoes. */
    Double unmetDemand;

    /** Razao entre demanda atendida e irrestrita, entre zero e um. */
    Double fulfillmentRate;

}
