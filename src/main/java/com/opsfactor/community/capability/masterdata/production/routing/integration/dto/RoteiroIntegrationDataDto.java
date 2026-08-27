package com.opsfactor.community.capability.masterdata.production.routing.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DTO do roteiro operacional Community.
 *
 * <p>O roteiro Community descreve somente location, material de saida,
 * prioridade e possibilidade de uso sem versao de producao. Setup detalhado,
 * manutencao, turnos, custos de recurso, roteiros paralelos e line scheduling
 * pertencem ao OpsFactor Enterprise.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString 
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoteiroIntegrationDataDto extends IntegrationDataDtoAbstract<RoteiroIntegrationDataDto, RoteiroIntegrationDataDto.RoteiroPrimaryKeyIntegrationDTO, Roteiro> {

    public String locationId;
    public String description;
    public Boolean active;
    public String outputMaterialId;
    /**
     * Identificador do cluster de roteiros Enterprise.
     *
     * <p>O campo permanece no DTO compartilhado para que o overlay Enterprise
     * possa administrar o escalar {@code Roteiro.routingClusterId}. A
     * integracao publica Community o rejeita explicitamente, pois line
     * scheduling nao e uma capability Community.</p>
     */
    public String routingClusterId;
    public Boolean canBeUsedWithoutProductionVersion;
    public Integer priority;
    public Double baseQuantity;
    public String baseQuantityUomId;

    @EqualsAndHashCode
    public static class RoteiroPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<RoteiroPrimaryKeyIntegrationDTO, Roteiro> {

        public String id;

        @JsonCreator
        public RoteiroPrimaryKeyIntegrationDTO(@JsonProperty("id") String id) {
            this.id = id;
        }

        @Override
        public boolean hasSameKeyAsEntity(Roteiro entity) {
            return entity.getId().equals(this.id);
        }

    }

}
