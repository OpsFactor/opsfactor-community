package com.opsfactor.community.capability.masterdata.network.location.facade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract.TipoLocation;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO publico de location usado pelas telas e cargas Community.
 *
 * <p>O controle de restricoes inbound e uma capacidade Community porque o
 * heuristico de Supply o consome diretamente. O override local de demanda
 * indireta no safety stock tambem e Community e preserva a heranca do
 * parametro global quando permanece nulo. Campos de mapa, deployment e demais
 * restricoes logisticas permanecem no contrato compartilhado apenas para
 * compatibilidade transicional; mappers/services Community nao persistem essas
 * capabilities Enterprise.</p>
 */
@Data
@ToString @NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationDTO {
    /**
     * Não é o codigo da location no BD e sim o campo codigoErp (não necessariamente um número)
     */
    public String id;
    public TipoLocation locationType;
    /**
     * Indica se a location participa dos recortes operacionais. Quando o
     * atributo persistido nao foi configurado, o dominio a considera ativa.
     */
    public Boolean active;

    /**
     * Descricao da location.
     */
    public String description;
    
    public String country;
    public String state;
    public String city;
    
    public Double latitude;
    public Double longitude;

    // Codigo Caracteristica Location -> Valor Atributo
    public Map<String, String> characteristicValues = new HashMap<>();

    public Boolean showInSupplyPlanningBook;
    public Boolean showInProductionPlanningBook;
    /**
     * Deployment e restricoes logisticas gerais sao Enterprise. A restricao
     * inbound e a excecao: ela controla o respeito ao lead time de recebimento
     * pelo heuristico Community e, por isso, e publicada e persistida aqui.
     */
    public Boolean showInDeployment;
    public Boolean applyInboundConstraints; 
    /**
     * Override local opcional da inclusao de demanda indireta no safety stock.
     * `null` limpa o cadastro local e restaura a heranca de ParametrosGlobais.
     */
    public Boolean safetyStockConsiderIndirectDemand;
    public Boolean applyLogisticsConstraints;
    public Boolean applyProductionConstraints;
        
    public LocationDTO(String locationId, String descricao) {
        this.id = locationId;
        this.description = descricao;
    }
}
