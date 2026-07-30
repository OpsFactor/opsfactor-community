package com.opsfactor.community.capability.masterdata.production.productionresource.integration.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * DTO da disponibilidade diaria de recurso produtivo Community.
 *
 * <p>O Supply Planning heuristico restrito precisa receber a capacidade
 * produtiva diaria em horas por recurso. O Community nao aceita capacidade em
 * quantidade/UOM nem disponibilidade baseada em turnos; estes campos continuam
 * no DTO somente para rejeitar payloads Enterprise de forma explicita.</p>
 */
@SuperBuilder
@AllArgsConstructor // necessário para que NoArgsConstrutor funcione com @Builder
@NoArgsConstructor // necessário para Jackson deserializar objetos
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisponibilidadeRecursoProdutivoIntegrationDataDto extends IntegrationDataDtoAbstract<DisponibilidadeRecursoProdutivoIntegrationDataDto, DisponibilidadeRecursoProdutivoIntegrationDataDto.DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO, DisponibilidadeRecursoProdutivo> {

    public Float availableHours;

    /**
     * Campo Enterprise transicional. Capacidade por quantidade/UOM nao e aceita
     * no Community; se vier preenchida via JSON, o mapper lança erro antes de
     * persistir.
     */
    public Float capacityInQuantity;

    /**
     * Campo Enterprise transicional pareado com `capacityInQuantity`.
     */
    public String capacityInQuantityUomId;

    @EqualsAndHashCode
    public static class DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO, DisponibilidadeRecursoProdutivo> {

        public String productionResourceId;
        public LocalDate referenceDate;

        @JsonCreator
        public DisponibilidadeRecursoProdutivoPrimaryKeyIntegrationDTO(
                @JsonProperty("productionResourceId") String productionResourceId,
                @JsonProperty("referenceDate") LocalDate referenceDate) {
            this.productionResourceId = productionResourceId;
            this.referenceDate = referenceDate;
        }

        @Override
        public boolean hasSameKeyAsEntity(DisponibilidadeRecursoProdutivo entity) {

            return entity.getRecursoProdutivo().getId().equals(this.productionResourceId)
                    && entity.getDataReferencia().equals(this.referenceDate);

        }

    }

}
