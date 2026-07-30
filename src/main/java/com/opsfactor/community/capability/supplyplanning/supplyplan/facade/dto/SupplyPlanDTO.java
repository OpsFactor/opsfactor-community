package com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanDTO;
import com.opsfactor.community.platform.utility.Constantes;
import java.time.LocalDateTime;

/**
 * Resumo de Supply Plan retornado para a lista Community.
 *
 * <p>Este DTO nao carrega resultados de otimizador, P&L, cost-to-serve,
 * constraint tracking ou line scheduling. Ele existe para selecao/navegacao do
 * plano heuristico e para ligar o Supply Plan ao Demand Plan que o alimentou.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplyPlanDTO {

    /** Identificador do Supply Plan persistido. */
    public Long supplyPlanId;

    /** Versao da malha usada na geracao do plano. */
    public String supplyNetworkVersionId;

    /** Perfil heuristico usado na execucao. */
    public String executionProfileId;

    /** Descricao informada pelo usuario para a rodada. */
    public String description;

    /** Bucket temporal do plano. */
    public Constantes.TamanhoBucket bucketSize;

    /** Timestamp de geracao do plano. */
    public LocalDateTime timeOfExecution;

    /** Inicio do horizonte planejado. */
    public LocalDateTime beginsOn;

    /** Usuario que disparou a rodada. */
    public String generatedBy;

    /** Demand Plan usado como fonte de demanda futura no Community. */
    public DemandPlanDTO demandPlanDTO;

}
