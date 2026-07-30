package com.opsfactor.community.capability.planningbook.facade.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Celula selecionada para leitura/alteracao pelo Planning Book.
 *
 * <p>No Community, `referencePlanId` existe apenas para payloads legados ou
 * transicionais. A edicao aberta permite ajuste direto do plano atual em nivel
 * material/location; comparacao com reference plan e uma capacidade Enterprise
 * e deve ser bloqueada antes da aplicacao do ajuste.</p>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelectedPlanningBookCellDTO {

    public Long planId;
    // Campo Enterprise no Community: plano mostrado a titulo de comparacao/referencia.
    public String referencePlanId;
    
    public ConfiguredView.TipoView viewType;
    public String viewName;
    public String locationId; // por ora só usado no supply plan, pois há seleção explícita de location na tela do planning book
    
    /*
     * Dimensoes exibidas na linha do Planning Book. No Community, os mapas
     * carregam o nivel material/location e filtros ad-hoc da propria visao;
     * caracteristicas dinamicas e agrupamentos configuraveis permanecem
     * Enterprise.
     */
    public Map<String,String> locationDescriptionCols;
    public Map<String,String> materialDescriptionCols;

    /*
     * Reservado para uma futura selecao Enterprise de pai agregado. O fluxo
     * Community o rejeita explicitamente antes de tentar interpretar mapas de
     * dimensoes como uma folha material/location.
     */
    public PlanningBookParentSelectionDTO parentSelection;
    
    public String keyFigure;
    public LocalDate period;
    public String uom;
    public Double oldValue;
    public Double newValue;
    
    @JsonIgnore // necessário pois Jackson tenta serializar métodos e gera erro quando valor é nulo
    public boolean isMaterialLevel() {
        if (materialDescriptionCols == null) return false;
        return materialDescriptionCols.keySet().stream().anyMatch(x -> x.equals("materialId"));
    }
    
    @JsonIgnore // necessário pois Jackson tenta serializar métodos e gera erro quando valor é nulo
    public boolean isLocationLevel() {
        if (locationDescriptionCols == null) return false;
        return locationDescriptionCols.keySet().stream().anyMatch(x -> x.equals("locationId"));
    }
    
    @JsonIgnore // necessário pois Jackson tenta serializar métodos e gera erro quando valor é nulo
    public String getMaterialId() {
        if (materialDescriptionCols == null) return null;
        if (!materialDescriptionCols.containsKey("materialId")) return null;
        return materialDescriptionCols.get("materialId");
    }

    public String getLocationId() {
        if (locationId != null) return locationId;
        if (locationDescriptionCols == null) return null;
        if (!locationDescriptionCols.containsKey("locationId")) return null;
        return locationDescriptionCols.get("locationId");
    }
    
    public String toString() {
        
        return "Plan: " + planId
                + ", View: " + viewName
                + ", Material Dimensions: " + getFormattedDescriptionCols(materialDescriptionCols)
                + ", Location Dimensions: " + getFormattedDescriptionCols(locationDescriptionCols)
                + ", Key Figure: " + keyFigure
                + ", Period: " + period
                + ", UOM: " + uom
                + ", Old Value: " + oldValue
                + ", New Value: " + newValue;
                
    }

    /**
     * Formata os mapas de dimensoes sem transformar payload incompleto em
     * erro tecnico. A validacao funcional de material/location obrigatorios
     * continua centralizada nos services de Planning Book Community.
     */
    private String getFormattedDescriptionCols(Map<String,String> descriptionCols) {

        if (descriptionCols == null) {
            return "";
        }

        return descriptionCols.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .reduce("", (a,b) -> a + "," + b);

    }

    
}
