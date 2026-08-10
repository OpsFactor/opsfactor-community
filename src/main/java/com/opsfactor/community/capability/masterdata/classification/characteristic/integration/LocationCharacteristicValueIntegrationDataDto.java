package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaLocation;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO Enterprise para carga de valores de caracteristica por location.
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationCharacteristicValueIntegrationDataDto extends IntegrationDataDtoAbstract<
        LocationCharacteristicValueIntegrationDataDto,
        LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO,
        ValorCaracteristicaLocation> {

    public String characteristicValue;

    @EqualsAndHashCode(callSuper = false)
    public static class LocationCharacteristicValuePrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<
            LocationCharacteristicValuePrimaryKeyIntegrationDTO,
            ValorCaracteristicaLocation> {

        public String locationId;
        public String locationCharacteristicId;

        @JsonCreator
        public LocationCharacteristicValuePrimaryKeyIntegrationDTO(
                @JsonProperty("locationId")
                String locationId,
                @JsonProperty("locationCharacteristicId")
                String locationCharacteristicId) {

            this.locationId = locationId;
            this.locationCharacteristicId = locationCharacteristicId;

        }

        @Override
        public boolean hasSameKeyAsEntity(ValorCaracteristicaLocation entity) {

            return entity.getLocation().getId().equals(locationId)
                    && entity.getCaracteristicaLocation().getId().equals(locationCharacteristicId);

        }

    }

}
