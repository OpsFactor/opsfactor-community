package com.opsfactor.community.capability.demandplanning.configuration.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.utility.Constantes;

/**
 * Parametros de forecast configurados no nivel cluster/location e
 * cluster/material.
 *
 * <p>Alguns campos Enterprise permanecem desserializaveis apenas para rejeicao
 * defensiva de payloads legados ou transicionais. No Community, o OpenAPI
 * esconde esses campos e o mapper aceita apenas modelos estatisticos suportados,
 * split por Historical Sales e ausencia de stockout/outlier/uplift/foundation
 * models.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemandPlanningForecastParametersDTO {

    public Constantes.DPModeloEstatistico statisticalModel;

    /**
     * Numero de periodos usado pelo Moving Average e pelo split Historical
     * Sales. No Community nao aciona limpeza historica real.
     */
    public Integer daysMovingAverageModel;

    /**
     * Campos Enterprise mantidos apenas para compatibilidade do payload
     * compartilhado. Qualquer valor ativo ou diferente do default neutro e
     * rejeitado pelo DemandPlanningConfigurationMapper no Community.
     */
    public Boolean considerStockoutData;
    public Integer daysSmoothingModel;
    public Boolean enableUpperPercentileSmoothing;
    public Double smoothingUpperPercentile;
    public Boolean enableLowerPercentileSmoothing;
    public Double smoothingLowerPercentile;
    public Constantes.DPModeloNormalizacao smoothingModel;
    public Constantes.DPModeloUplift upliftModel;

    /**
     * Campo transicional aceito apenas para compatibilidade e rejeicao
     * defensiva de modelos Enterprise. O contrato publico Community o omite e
     * sempre assume Historical Sales.
     */
    public Constantes.DPModeloSplit splitModel;

    /**
     * Unica configuracao Community da etapa fixa de split Historical Sales:
     * quantidade de dias/periodos historicos usada para calcular a proporcao
     * final material/location.
     */
    public Integer daysTopDownSplit;

    // Parametros dos modelos Exponential Smoothing e Holt-Winters. null = selecao automatica.
    public Double alpha;
    public Double beta;
    public Double gamma;
    
    /*
     * Parametros Prophet/Chronos permanecem apenas para rejeicao defensiva de
     * payloads Enterprise/legados. No Community o mapper devolve defaults
     * neutros e rejeita qualquer valor nao-default antes de persistir a
     * configuracao.
     */
    public Boolean prophetAutoSeasonalityPriorScale;
    public Double prophetSeasonalityPriorScale;
    public Boolean prophetAutoChangepointPriorScale;
    public Double prophetChangepointPriorScale;
    public Boolean prophetAutoYearlyFourierOrder;
    public Integer prophetYearlyFourierOrder;

    public Boolean chronosForceAggregatedForecast;

}
