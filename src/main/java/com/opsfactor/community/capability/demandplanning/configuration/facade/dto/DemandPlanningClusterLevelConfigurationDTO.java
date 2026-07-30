package com.opsfactor.community.capability.demandplanning.configuration.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO da tela Community de configuracao/simulacao de forecast por cluster.
 * <p>
 * O Community permite parametrizar o forecast estatistico basico e a
 * desagregacao por historico de vendas. Auto-fit, Demand Accuracy e ajustes
 * avancados pertencem ao OpsFactor Enterprise: a documentacao OpenAPI
 * Community deve esconde-los, e campos transicionais que ainda existam nos
 * DTOs internos sao aceitos apenas para rejeicao defensiva no mapper.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemandPlanningClusterLevelConfigurationDTO {

    public String demandPlanExecutionProfileId;
    public Long materialClusterId;
    public Long locationClusterId;

    public DemandPlanningGeneralParametersDTO demandPlanningGeneralParameters;
    public DemandPlanningForecastParametersDTO demandPlanningForecastParameters;

}
