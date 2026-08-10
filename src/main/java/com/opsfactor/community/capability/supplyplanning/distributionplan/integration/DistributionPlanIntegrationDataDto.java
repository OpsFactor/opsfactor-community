package com.opsfactor.community.capability.supplyplanning.distributionplan.integration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO Enterprise read-only do Distribution Plan por Supply Plan.
 *
 * <p>O arquivo legado e filtrado por {@code supplyPlanId} no path e nao possui
 * coluna de Supply Plan. A chave JSON conserva o id do plano para manter
 * rastreabilidade do output fisico de Supply Planning.</p>
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistributionPlanIntegrationDataDto extends IntegrationDataDtoAbstract<
        DistributionPlanIntegrationDataDto,
        DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO,
        DistributionPlanIntegrationDataDto> {

    /**
     * Unidade de medida em que as quantidades da transferencia/compra foram
     * persistidas.
     */
    public String unitOfMeasureId;

    /**
     * Quantidade planejada no plano irrestrito.
     */
    public Double unconstrainedPlannedOrderQuantity;

    /**
     * Quantidade firme no plano irrestrito.
     */
    public Double unconstrainedFirmOrderQuantity;

    /**
     * Quantidade planejada no plano restrito.
     */
    public Double constrainedPlannedOrderQuantity;

    /**
     * Quantidade firme no plano restrito.
     */
    public Double constrainedFirmOrderQuantity;

    /**
     * Chave publica da linha de Distribution Plan.
     */
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class DistributionPlanPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<
            DistributionPlanPrimaryKeyIntegrationDTO,
            DistributionPlanIntegrationDataDto> {

        /**
         * Supply Plan exportado.
         */
        public Long supplyPlanId;

        /**
         * Location de origem do abastecimento.
         */
        public String originLocationId;

        /**
         * Location de destino do abastecimento.
         */
        public String destinationLocationId;

        /**
         * Material transferido ou comprado.
         */
        public String materialId;

        /**
         * Data planejada de entrega/recebimento.
         */
        public LocalDateTime plannedDeliveryDate;

        /**
         * Data sugerida para emissao da ordem.
         */
        public LocalDateTime suggestedOrderEmissionDate;

        @JsonCreator
        public DistributionPlanPrimaryKeyIntegrationDTO(
                @JsonProperty("supplyPlanId") Long supplyPlanId,
                @JsonProperty("originLocationId") String originLocationId,
                @JsonProperty("destinationLocationId") String destinationLocationId,
                @JsonProperty("materialId") String materialId,
                @JsonProperty("plannedDeliveryDate") LocalDateTime plannedDeliveryDate,
                @JsonProperty("suggestedOrderEmissionDate") LocalDateTime suggestedOrderEmissionDate) {

            this.supplyPlanId = supplyPlanId;
            this.originLocationId = originLocationId;
            this.destinationLocationId = destinationLocationId;
            this.materialId = materialId;
            this.plannedDeliveryDate = plannedDeliveryDate;
            this.suggestedOrderEmissionDate = suggestedOrderEmissionDate;

        }

        @Override
        public boolean hasSameKeyAsEntity(
                DistributionPlanIntegrationDataDto entity) {

            DistributionPlanPrimaryKeyIntegrationDTO entityPrimaryKeyDto = entity.primaryKeyDto;
            return entityPrimaryKeyDto != null
                    && Objects.equals(entityPrimaryKeyDto.supplyPlanId, this.supplyPlanId)
                    && Objects.equals(entityPrimaryKeyDto.originLocationId, this.originLocationId)
                    && Objects.equals(entityPrimaryKeyDto.destinationLocationId, this.destinationLocationId)
                    && Objects.equals(entityPrimaryKeyDto.materialId, this.materialId)
                    && Objects.equals(entityPrimaryKeyDto.plannedDeliveryDate, this.plannedDeliveryDate)
                    && Objects.equals(entityPrimaryKeyDto.suggestedOrderEmissionDate, this.suggestedOrderEmissionDate);

        }

    }

}
