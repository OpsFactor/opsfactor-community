package com.opsfactor.community.capability.demandplanning.configuration.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.utility.Constantes;

import java.util.List;

/**
 * Parametros gerais da configuracao de forecast em nivel cluster location /
 * cluster material.
 *
 * <p>No Community este DTO controla apenas execucao do plano, unidade,
 * arredondamento, tratamento simples de DFUs inativas/descontinuadas e o tipo
 * de agregacao material/location. Campos de auto-fit, budget, material novo e
 * regressores permanecem transicionais para rejeicao defensiva de payloads
 * Enterprise ou legados.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemandPlanningGeneralParametersDTO {

    public Boolean executeDemandPlan;

    /**
     * Campo Enterprise: no Community o perfil nao pode usar modelo gerado por
     * auto-fit. O mapper rejeita valor ativo e retorna default neutro.
     */
    public Boolean useExecutionProfileAutofitModel;

    public String uomId;
    public Boolean roundToSalesUnit;

    public Boolean considerHistoricalSalesOfInactiveDfus;
    /**
     * Indica se materiais descontinuados ainda devem receber forecast quando
     * houver historico elegivel.
     *
     */
    public Boolean generateForecastForDiscontinuedMaterials;

    /*
     * Cada dimensão usa o mesmo enum: BOTTOM_UP significa forecast no menor
     * nível daquela dimensão, TOP_DOWN significa forecast agregado e posterior
     * desagregação. O nome do campo indica a dimensão; o enum indica o tipo de
     * execução.
     */
    public Constantes.DPNivelAgregacao materialAggregationType;
    public Constantes.DPNivelAgregacao locationAggregationType;

    /** Campo Enterprise: Budget as Forecast nao existe no Community. */
    public Long budgetId;

    /**
     * Campo Enterprise: tratamento especifico de material novo nao existe no
     * Community. O status NEW tambem nao deve ser gerado funcionalmente nesta
     * edicao.
     */
    public Integer daysAsNewMaterial;

    public Integer daysSalesHistory; // número de dias históricos considerados para o forecast estatístico
    /*
     * Support series/regression time series sao uma capacidade Enterprise.
     * O campo permanece neutro no DTO Community apenas para que payloads
     * compartilhados ou legados possam ser rejeitados sem expor o dominio JPA.
     */
    public List<?> regressionTimeSeries;

    /*
     * Campos Enterprise para fixar trend/regressores internos de Prophet/ARIMA.
     * O Community executa ARIMA sem regressores e nao executa Prophet.
     */
    public Boolean considerTargetTrendGrowthYoy;
    public Integer numberOfDaysCurrentLevelAsAverageOfHistoricalStl;
    public Double targetGrowthYoy;

    /*
     * Campo Enterprise: working days como regressor estatistico. O calendario
     * tecnico Community ainda pode existir para conversoes, mas nao entra como
     * support series de forecast.
     */
    public Boolean includeWorkingDaysRegressor;

}
