package com.opsfactor.community.capability.demandplanning.demandplan.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Linha material/location do Demand Plan exposta para o front novo.
 *
 * <p>O Community nao expõe colaboracao agregada nesta estrutura: cada linha
 * representa exatamente um material, uma location, uma data e uma unidade de
 * medida. A entidade JPA ainda usa `Produto` como nome fisico do material, mas
 * o contrato publico do DTO deve permanecer em material/location.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemandPlanItemDTO {

    /**
     * Location da linha de demanda.
     */
    public String locationId;

    /**
     * Material da linha de demanda.
     */
    public String materialId;

    /**
     * Data de referencia do bucket.
     */
    public LocalDateTime referenceDate;

    /**
     * Unidade de medida em que as quantidades foram exportadas.
     */
    public String uomId;

    /**
     * Baseline irrestrito gerado pelo forecast/desagregacao Community.
     */
    public Double baselineQtyUnconstrained;

    /**
     * Ajuste manual irrestrito realizado via Planning Book.
     */
    public Double demandAdjustmentQtyUnconstrained;

    /**
     * Total irrestrito calculado como baseline + ajuste de demanda.
     */
    public Double totalQtyUnconstrained;

    /**
     * Baseline atendido apos propagacao/restricao pelo Supply Planning.
     */
    public Double baselineQtyConstrained;

    /**
     * Ajuste de demanda atendido apos propagacao/restricao pelo Supply Planning.
     */
    public Double demandAdjustmentQtyConstrained;

    /**
     * Total restrito calculado como baseline atendido + ajuste atendido.
     */
    public Double totalQtyConstrained;

}
