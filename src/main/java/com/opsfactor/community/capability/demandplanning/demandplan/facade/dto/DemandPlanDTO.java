package com.opsfactor.community.capability.demandplanning.demandplan.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.utility.Constantes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO resumido/detalhado de Demand Plan exposto ao front Community.
 *
 * <p>Quando usado em listagens, {@link #demandPlanDetail} fica nulo para
 * evitar carregar linhas desnecessarias. Quando usado no detalhe, as linhas
 * representam apenas baseline, ajuste de demanda e totais Community.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemandPlanDTO {

    /**
     * Identificador da versao de Demand Plan.
     */
    public Long demandPlanId;

    /**
     * Descricao funcional informada na geracao.
     */
    public String description;

    /**
     * Perfil de execucao usado para gerar o plano.
     */
    public String executionProfileId;

    /**
     * Granularidade temporal do plano.
     */
    public Constantes.TamanhoBucket bucketSize;

    /**
     * Data/hora em que a versao foi gerada.
     */
    public LocalDateTime timeOfExecution;

    /**
     * Periodo de referencia textual quando aplicavel ao front.
     */
    public String referencePeriod;

    /**
     * Primeira data do horizonte planejado.
     */
    public LocalDateTime beginsOn;

    /**
     * Usuario que disparou a geracao.
     */
    public String generatedBy;

    /**
     * Linhas material/location do plano. Nulo em respostas resumidas.
     */
    public List<DemandPlanItemDTO> demandPlanDetail;

}
