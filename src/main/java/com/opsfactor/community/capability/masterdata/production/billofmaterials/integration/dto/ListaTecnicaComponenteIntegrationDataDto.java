package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO do componente de lista tecnica Community.
 *
 * <p>O contrato Community persiste somente material componente, quantidade e
 * UOM de consumo. Substituicoes, custos, perdas economicas e regras avancadas
 * de line scheduling pertencem ao OpsFactor Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListaTecnicaComponenteIntegrationDataDto extends IntegrationDataDtoAbstract<ListaTecnicaComponenteIntegrationDataDto, ListaTecnicaComponenteIntegrationDataDto.ListaTecnicaComponentePrimaryKeyIntegrationDTO, ListaTecnicaComponente> {

    public String componentMaterialQuantityUomId;
    public Double componentMaterialQuantity;

    @EqualsAndHashCode
    public static class ListaTecnicaComponentePrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<ListaTecnicaComponentePrimaryKeyIntegrationDTO, ListaTecnicaComponente> {

        public String bomId;
        public String componentMaterialId;

        @JsonCreator
        public ListaTecnicaComponentePrimaryKeyIntegrationDTO(
                @JsonProperty("bomId") String bomId,
                @JsonProperty("componentMaterialId") String componentMaterialId) {
            this.bomId = bomId;
            this.componentMaterialId = componentMaterialId;
        }

        @Override
        public boolean hasSameKeyAsEntity(ListaTecnicaComponente entity) {
            return entity.getListaTecnica().getId().equals(bomId)
                    && entity.getMaterialComponente().getId().equals(componentMaterialId);
        }

    }
    
}
