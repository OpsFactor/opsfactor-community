package com.opsfactor.community.capability.demandplanning.configuration.facade.dto;

import java.time.LocalDate;

/**
 * Request da simulacao Community de forecast de Demand Planning.
 *
 * <p>A simulacao recebe uma data de referencia e a configuracao cluster-level
 * editavel na tela. O fluxo reutiliza as mesmas validacoes do save de
 * configuracao, portanto modelos Enterprise, support series, auto-fit,
 * stockout/outlier reais, uplift e splits privados falham antes de qualquer
 * repository/factory.</p>
 */
public class DemandPlanningPreviaForecastRequestDTO {

    /**
     * Data usada como presente da simulacao.
     */
    public LocalDate referenceDate;

    /**
     * Configuracao cluster material/location a ser simulada.
     */
    public DemandPlanningClusterLevelConfigurationDTO demandPlanningConfiguration;

}
