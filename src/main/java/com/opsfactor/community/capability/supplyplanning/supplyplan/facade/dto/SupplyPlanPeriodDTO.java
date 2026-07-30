package com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.utility.Constantes;

import java.time.LocalDateTime;

/**
 * Periodo de calendario disponivel para filtros e navegacao de um Supply Plan.
 *
 * <p>O Community usa esse DTO para o Planning Book material/location e para
 * consultas de plano heuristico. Nao ha metadados de solver, line scheduling ou
 * diagnostico de restricoes neste contrato.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplyPlanPeriodDTO {

    /** Indice sequencial do periodo dentro do horizonte planejado. */
    public Integer periodIndex;

    /** Rotulo amigavel exibido no front. */
    public String label;

    /** Granularidade temporal do periodo. */
    public Constantes.TamanhoBucket bucketSize;

    /** Data de referencia usada para filtros e exibicao. */
    public LocalDateTime referenceDate;

    /** Inicio exato do bucket. */
    public LocalDateTime startDateTime;

    /** Fim exato do bucket. */
    public LocalDateTime endDateTime;

    public SupplyPlanPeriodDTO(
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
