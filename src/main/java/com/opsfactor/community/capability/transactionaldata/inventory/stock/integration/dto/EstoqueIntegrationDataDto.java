package com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * DTO do snapshot de estoque inicial Community.
 *
 * <p>O contrato publico e propositalmente pequeno: location/material/data ficam
 * na chave primaria, enquanto o payload traz somente unidade de medida,
 * quantidade e o marcador tecnico de delecao herdado do fluxo generico de
 * integracao. Lote, validade, aging, writeoff e producao em batch pertencem ao
 * OpsFactor Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EstoqueIntegrationDataDto extends IntegrationDataDtoAbstract<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, Estoque> {

    public String uomId;
    public Double quantity;
    public String delete;

    @EqualsAndHashCode
    public static class EstoquePrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<EstoquePrimaryKeyIntegrationDTO, Estoque> {

        public String locationId;
        public String materialId;
        public LocalDateTime referenceDate;

        @JsonCreator
        public EstoquePrimaryKeyIntegrationDTO(
                @JsonProperty("locationId") String locationId,
                @JsonProperty("materialId") String materialId,
                @JsonProperty("referenceDate") LocalDateTime referenceDate) {
            this.locationId = locationId;
            this.materialId = materialId;
            this.referenceDate = referenceDate;
        }

        @Override
        public boolean hasSameKeyAsEntity(Estoque entity) {
            return entity.getLocation().getId().equals(this.locationId)
                    && entity.getProduto().getId().equals(this.materialId)
                    && entity.getDataReferencia().equals(this.referenceDate);
        }

    }

}
