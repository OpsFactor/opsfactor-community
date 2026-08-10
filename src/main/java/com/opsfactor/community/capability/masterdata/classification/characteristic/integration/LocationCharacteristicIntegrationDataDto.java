package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO compartilhado para integracao do catalogo de caracteristicas de location.
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationCharacteristicIntegrationDataDto extends IntegrationDataDtoAbstract<
        LocationCharacteristicIntegrationDataDto,
        LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO,
        CaracteristicaLocation> {

    public String description;
    public Caracteristica.TipoCaracteristica characteristicType;

    @EqualsAndHashCode(callSuper = false)
    public static class LocationCharacteristicPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<
            LocationCharacteristicPrimaryKeyIntegrationDTO,
            CaracteristicaLocation> {

        public String locationCharacteristicId;

        @JsonCreator
        public LocationCharacteristicPrimaryKeyIntegrationDTO(
                @JsonProperty("locationCharacteristicId")
                String locationCharacteristicId) {

            this.locationCharacteristicId = locationCharacteristicId;

        }

        @Override
        public boolean hasSameKeyAsEntity(CaracteristicaLocation entity) {

            return entity.getId().equals(locationCharacteristicId);

        }

    }

}
