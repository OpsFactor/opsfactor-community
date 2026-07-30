package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO da lista tecnica operacional Community.
 *
 * <p>A lista tecnica Community representa apenas BOM simples com material de
 * saida, quantidade, UOM, location, prioridade e status ativo. Co-produtos,
 * outputs paralelos e qualquer custo/analise economica associada a BOM ficam
 * no OpsFactor Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListaTecnicaIntegrationDataDto extends IntegrationDataDtoAbstract<ListaTecnicaIntegrationDataDto, ListaTecnicaIntegrationDataDto.ListaTecnicaPrimaryKeyIntegrationDTO, ListaTecnica> {

    public String description;
    public String locationId;
    public String outputMaterialId;
    public Double outputQuantity;
    public String outputUomId;
    public Integer priority;
    public Boolean active;
    public Boolean canBeUsedWithoutProductionVersion;

    @EqualsAndHashCode
    public static class ListaTecnicaPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<ListaTecnicaPrimaryKeyIntegrationDTO, ListaTecnica> {

        public String id;

        @JsonCreator
        public ListaTecnicaPrimaryKeyIntegrationDTO(@JsonProperty("id") String id) {
            this.id = id;
        }

        @Override
        public boolean hasSameKeyAsEntity(ListaTecnica entity) {
            return entity.getId().equals(this.id);
        }

    }

}
