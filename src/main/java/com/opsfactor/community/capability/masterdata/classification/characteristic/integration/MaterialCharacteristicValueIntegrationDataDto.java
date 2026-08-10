package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaProduto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO Enterprise para carga de valores de caracteristica por material.
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaterialCharacteristicValueIntegrationDataDto extends IntegrationDataDtoAbstract<
        MaterialCharacteristicValueIntegrationDataDto,
        MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO,
        ValorCaracteristicaProduto> {

    public String characteristicValue;

    @EqualsAndHashCode(callSuper = false)
    public static class MaterialCharacteristicValuePrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<
            MaterialCharacteristicValuePrimaryKeyIntegrationDTO,
            ValorCaracteristicaProduto> {

        public String materialId;
        public String materialCharacteristicId;

        @JsonCreator
        public MaterialCharacteristicValuePrimaryKeyIntegrationDTO(
                @JsonProperty("materialId")
                String materialId,
                @JsonProperty("materialCharacteristicId")
                String materialCharacteristicId) {

            this.materialId = materialId;
            this.materialCharacteristicId = materialCharacteristicId;

        }

        @Override
        public boolean hasSameKeyAsEntity(ValorCaracteristicaProduto entity) {

            return entity.getProduto().getId().equals(materialId)
                    && entity.getCaracteristicaProduto().getId().equals(materialCharacteristicId);

        }

    }

}
