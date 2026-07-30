package com.opsfactor.community.capability.masterdata.network.location.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract.TipoLocation;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO de data upload Community para locations operacionais.
 * <p>
 * O Community usa locations em Demand/Supply Planning no nivel material/location.
 * GIS/mapa, estruturas logisticas avancadas, filtros por caracteristicas e
 * capacidade logistica detalhada pertencem ao Enterprise.
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationIntegrationDataDto extends IntegrationDataDtoAbstract<LocationIntegrationDataDto, LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO, Location> {

    public String description;
    public Boolean active;
    public TipoLocation locationType;
    public String country;
    public String state;
    public String city;
    public Double latitude;
    public Double longitude;
    public Boolean availableInProductionPlanningBook;
    public Boolean availableInSupplyPlanningBook;
    public Boolean finiteProductionCapacity;
    public String defaultSNPUomId;
    public String expeditionUomId;
    /**
     * Identificador do cabeçalho de grupo econômico associado à location.
     *
     * <p>O campo permanece no contrato compartilhado para overlays Enterprise.
     * O Community não permite configurá-lo pela integração pública, pois os
     * fluxos que administram essa associação são uma capability Enterprise.</p>
     */
    public String economicGroupId;
    /**
     * Identificador da location usada como referencia para os parametros de
     * material/location desta location.
     */
    public String referenceLocationForProductLocationParameters;
    public Boolean safetyStockConsiderIndirectDemand;
    public Integer orderFulfillmentTimeDays;

    /**
     * Caracteristicas de location sao Enterprise. O campo permanece no DTO
     * compartilhado para compatibilidade de contrato e para que o mapper
     * Community consiga rejeitar payloads Enterprise com mensagem explicita.
     */
    public Map<String,String> valueByCharacteristic = new HashMap<>();

    @EqualsAndHashCode
    public static class LocationPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<LocationPrimaryKeyIntegrationDTO, Location> {

        public String id;

        @JsonCreator
        public LocationPrimaryKeyIntegrationDTO(@JsonProperty("id") String id) {
            this.id = id;
        }

        @Override
        public boolean hasSameKeyAsEntity(Location entity) {
            return entity.getId().equals(this.id);
        }

    }
    
}
