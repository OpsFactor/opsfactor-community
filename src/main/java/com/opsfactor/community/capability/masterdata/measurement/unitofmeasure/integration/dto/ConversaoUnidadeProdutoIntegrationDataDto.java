package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO de data upload Community para conversoes de unidade especificas por
 * material.
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversaoUnidadeProdutoIntegrationDataDto extends IntegrationDataDtoAbstract<ConversaoUnidadeProdutoIntegrationDataDto, ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO, ConversaoUnidadeProduto> {

    public Double originQuantity;
    public Double targetQuantity;
    @EqualsAndHashCode
    public static class ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO, ConversaoUnidadeProduto> {

        public String materialId;
        public String originUomId;
        public String targetUomId;

        @JsonCreator
        public ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO(
                @JsonProperty("materialId") String materialId,
                @JsonProperty("originUomId") String originUomId,
                @JsonProperty("targetUomId") String targetUomId) {
            this.materialId = materialId;
            this.originUomId = originUomId;
            this.targetUomId = targetUomId;
        }

        @Override
        public boolean hasSameKeyAsEntity(ConversaoUnidadeProduto entity) {
            return entity.getProduto().getId().equals(this.materialId)
                    && entity.getUnidadeMedidaOrigem().getId().equals(this.originUomId)
                    && entity.getUnidadeMedidaDestino().getId().equals(this.targetUomId);
        }

    }

}
