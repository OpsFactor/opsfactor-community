package com.opsfactor.community.capability.demandplanning.demandplan.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.utility.Constantes;

import java.time.LocalDateTime;

/**
 * Período persistido de um Demand Plan disponível para filtros de leitura.
 *
 * <p>`referenceDate` é sempre o início do bucket e pode ser usado diretamente
 * por consumidores que consultam uma fotografia do Demand Plan por período.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemandPlanPeriodDTO {

    public Integer periodIndex;
    public String label;
    public Constantes.TamanhoBucket bucketSize;
    public LocalDateTime referenceDate;
    public LocalDateTime startDateTime;
    public LocalDateTime endDateTime;

    public DemandPlanPeriodDTO(
            Integer periodIndex,
            String label,
            Constantes.TamanhoBucket bucketSize,
            LocalDateTime referenceDate,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {

        this.periodIndex = periodIndex;
        this.label = label;
        this.bucketSize = bucketSize;
        this.referenceDate = referenceDate;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;

    }

}
