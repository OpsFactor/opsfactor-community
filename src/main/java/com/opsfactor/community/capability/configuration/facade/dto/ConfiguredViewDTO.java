package com.opsfactor.community.capability.configuration.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;

import java.util.List;

/**
 * Contrato de configuracao das views de Planning Book exposto ao novo front.
 *
 * <p>O DTO permanece amplo porque o front Community e o front Enterprise
 * compartilham boa parte das telas e payloads. No Community, o service
 * consumidor normaliza ou rejeita explicitamente os campos Enterprise para que
 * uma chamada manual de API nao grave uma configuracao que o backend nao sabe
 * executar.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfiguredViewDTO {
    
    public String userId;
    public String viewName;
    public ConfiguredView.TipoView viewType;
    
    public String directDemandUpdateKeyFigure;

    /**
     * Filtros Community por atributos públicos de material. O backend aceita
     * somente identidade e valores; apresentação, posição e agrupamento são Pro.
     */
    public List<ConfiguredViewCaracteristicaDTO> materialCharacteristicDetailList;

    /** Filtros equivalentes sobre os atributos públicos de location. */
    public List<ConfiguredViewCaracteristicaDTO> locationCharacteristicDetailList;

    /**
     * Campo Enterprise. No Community, filtros DFU material-location sao
     * bloqueados integralmente no ConfiguredViewFrontService.
     */
    public List<ConfiguredViewCaracteristicaDTO> materialLocationCharacteristicDetailList;
    
    public Boolean showMaterialLevel;
    public Boolean showLocationLevel;
    
    public String unitOfMeasure;
    
    public Integer numberHistoricalSalesPeriodsDemandPlanningBook;
    
    public List<ConfiguredViewKeyFigureDTO> keyFigureList;
    
    public Boolean autoSubmitChanges;
    
    public Boolean allowInputFrozenHorizon;
    
    public Boolean showHistoricalAverage;
    
    /**
     * Exibe materiais descontinuados nas views material/location.
     */
    public Boolean showDiscontinuedMaterials;
    
    public Boolean showAverageHistoricalSales;
    
    public Boolean showDfusWithoutHistoricalSalesOverHistoricalPeriod;

    public String demandPlanWorkflowId;
    public String demandPlanWorkflowStageId;

    /**
     * Identificadores de material aceitos pela edição Community para limitar
     * diretamente o escopo da view. Lista vazia significa todos os materiais.
     */
    public List<String> materialIdFilterList;

    /**
     * Identificadores de location aceitos pela edição Community para limitar
     * diretamente o escopo da view. Lista vazia significa todas as locations.
     */
    public List<String> locationIdFilterList;
    
}
