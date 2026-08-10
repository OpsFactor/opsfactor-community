package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO compartilhado para integracao do catalogo de caracteristicas de material.
 *
 * <p>Este DTO cadastra somente a definicao da caracteristica. Os valores por
 * material ja usam data upload proprio, porque dependem de ids de materiais e
 * de validacoes de cardinalidade diferentes.</p>
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaterialCharacteristicIntegrationDataDto extends IntegrationDataDtoAbstract<
        MaterialCharacteristicIntegrationDataDto,
        MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO,
        CaracteristicaProduto> {

    public String description;
    public Caracteristica.TipoCaracteristica characteristicType;

    @EqualsAndHashCode(callSuper = false)
    public static class MaterialCharacteristicPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<
            MaterialCharacteristicPrimaryKeyIntegrationDTO,
            CaracteristicaProduto> {

        public String materialCharacteristicId;

        @JsonCreator
        public MaterialCharacteristicPrimaryKeyIntegrationDTO(
                @JsonProperty("materialCharacteristicId")
                String materialCharacteristicId) {

            this.materialCharacteristicId = materialCharacteristicId;

        }

        @Override
        public boolean hasSameKeyAsEntity(CaracteristicaProduto entity) {

            return entity.getId().equals(materialCharacteristicId);

        }

    }

}
