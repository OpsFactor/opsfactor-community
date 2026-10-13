package com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto;

import com.opsfactor.community.platform.integration.validation.IntegrationFileColumn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * DTO da venda historica Community.
 *
 * <p>O payload possui apenas documento, data, location, material, unidade de
 * medida e quantidade. Campos de valor, preco, pedido, campanha/evento ou tipo
 * sell-in pertencem ao OpsFactor Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SelloutIntegrationDataDto extends IntegrationDataDtoAbstract<SelloutIntegrationDataDto, SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO, Sellout> {
    
    @IntegrationFileColumn(1)
    public LocalDateTime referenceDate;
    @IntegrationFileColumn(2)
    public String originLocationId;
    @IntegrationFileColumn(3)
    public String materialId;
    @IntegrationFileColumn(4)
    public String uomId;
    @IntegrationFileColumn(5)
    public Double quantity;

    @EqualsAndHashCode
    public static class SelloutPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<SelloutPrimaryKeyIntegrationDTO, Sellout> {

        @IntegrationFileColumn(0)
        public String documentId;

        @JsonCreator
        public SelloutPrimaryKeyIntegrationDTO(@JsonProperty("documentId") String documentId) {
            this.documentId = documentId;
        }

        @Override
        public boolean hasSameKeyAsEntity(Sellout entity) {
            return entity.getId().equals(this.documentId);
        }

    }
    
}
