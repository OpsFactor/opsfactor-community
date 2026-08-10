package com.opsfactor.community.capability.demandplanning.export;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato compartilhado da extracao detalhada de Demand Plan.
 *
 * <p>O Community entrega as key figures standard. O Enterprise pode enriquecer
 * o mesmo arquivo com Custom Key Figures sem alterar a rota publica.</p>
 */
public interface DemandPlanDetailedExportService {

    /** Exporta todos os periodos persistidos do Demand Plan. */
    List<List<Object>> getFile(Long demandPlanId);

    /** Exporta somente o bucket que contem a data de referencia. */
    List<List<Object>> getFileByPeriod(Long demandPlanId, LocalDate referenceDate);

}
