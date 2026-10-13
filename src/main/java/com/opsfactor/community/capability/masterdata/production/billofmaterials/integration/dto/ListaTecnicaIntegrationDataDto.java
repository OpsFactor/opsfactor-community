package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.dto;

import com.opsfactor.community.platform.integration.validation.IntegrationFileColumn;

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

    @IntegrationFileColumn(1)
    public String description;
    @IntegrationFileColumn(2)
    public String locationId;
    @IntegrationFileColumn(3)
    public String outputMaterialId;
    @IntegrationFileColumn(4)
    public Double outputQuantity;
    @IntegrationFileColumn(5)
    public String outputUomId;
    @IntegrationFileColumn(6)
    public Integer priority;
    @IntegrationFileColumn(7)
    public Boolean active;
    @IntegrationFileColumn(8)
    public Boolean canBeUsedWithoutProductionVersion;

    @EqualsAndHashCode
    public static class ListaTecnicaPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<ListaTecnicaPrimaryKeyIntegrationDTO, ListaTecnica> {

        @IntegrationFileColumn(value = 0, required = true)
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
