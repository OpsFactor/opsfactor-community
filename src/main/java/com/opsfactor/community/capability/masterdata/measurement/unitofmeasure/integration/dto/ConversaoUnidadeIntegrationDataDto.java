package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidade;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO de data upload Community para conversoes globais entre unidades de
 * medida.
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversaoUnidadeIntegrationDataDto extends IntegrationDataDtoAbstract<ConversaoUnidadeIntegrationDataDto, ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO, ConversaoUnidade> {

    public Double originQuantity;
    public Double targetQuantity;

    @EqualsAndHashCode
    public static class ConversaoUnidadePrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<ConversaoUnidadePrimaryKeyIntegrationDTO, ConversaoUnidade> {

        public String originUomId;
        public String targetUomId;

        @JsonCreator
        public ConversaoUnidadePrimaryKeyIntegrationDTO(
                @JsonProperty("originUomId") String originUomId,
                @JsonProperty("targetUomId") String targetUomId) {
            this.originUomId = originUomId;
            this.targetUomId = targetUomId;
        }

        @Override
        public boolean hasSameKeyAsEntity(ConversaoUnidade entity) {
            return entity.getUnidadeMedidaOrigem().getId().equals(this.originUomId)
                    && entity.getUnidadeMedidaDestino().getId().equals(this.targetUomId);
        }

    }

}
