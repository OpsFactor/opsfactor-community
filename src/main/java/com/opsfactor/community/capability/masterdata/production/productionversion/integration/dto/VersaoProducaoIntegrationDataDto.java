package com.opsfactor.community.capability.masterdata.production.productionversion.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO da versao de producao Community.
 *
 * <p>A versão aponta para as abstrações gerais de roteiro e lista técnica. O
 * Community disponibiliza apenas os subtipos simples desses mestres, enquanto
 * o Enterprise também disponibiliza as especializações de múltiplos outputs.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersaoProducaoIntegrationDataDto extends IntegrationDataDtoAbstract<VersaoProducaoIntegrationDataDto, VersaoProducaoIntegrationDataDto.VersaoProducaoPrimaryKeyIntegrationDTO, VersaoProducao> {

    public String locationId;
    public Integer priority;
    public String outputMaterialId;
    public String routingId;
    public String billOfMaterialsId;
    public Boolean active;

    @EqualsAndHashCode
    public static class VersaoProducaoPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<VersaoProducaoPrimaryKeyIntegrationDTO, VersaoProducao> {

        public String id;

        @JsonCreator
        public VersaoProducaoPrimaryKeyIntegrationDTO(@JsonProperty("id") String id) {
            this.id = id;
        }

        @Override
        public boolean hasSameKeyAsEntity(VersaoProducao entity) {
            return entity.getId().equals(this.id);
        }

    }
}
