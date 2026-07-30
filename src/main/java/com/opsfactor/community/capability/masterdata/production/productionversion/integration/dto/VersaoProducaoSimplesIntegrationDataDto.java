package com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO da versao de producao simples Community.
 *
 * <p>Uma versao simples aponta uma combinacao unica de location, material de
 * saida, roteiro e BOM. Parallel routing/output, multiplos outputs e selecao
 * avancada de versoes sao capacidades Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersaoProducaoSimplesIntegrationDataDto extends IntegrationDataDtoAbstract<VersaoProducaoSimplesIntegrationDataDto, VersaoProducaoSimplesIntegrationDataDto.VersaoProducaoSimplesPrimaryKeyIntegrationDTO, VersaoProducaoSimples> {

    public String locationId;
    public Integer priority;
    public String outputMaterialId;
    public String routingId;
    public String billOfMaterialsId;
    public Boolean active;

    @EqualsAndHashCode
    public static class VersaoProducaoSimplesPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<VersaoProducaoSimplesPrimaryKeyIntegrationDTO, VersaoProducaoSimples> {

        public String id;

        @JsonCreator
        public VersaoProducaoSimplesPrimaryKeyIntegrationDTO(@JsonProperty("id") String id) {
            this.id = id;
        }

        @Override
        public boolean hasSameKeyAsEntity(VersaoProducaoSimples entity) {
            return entity.getId().equals(this.id);
        }

    }
}
