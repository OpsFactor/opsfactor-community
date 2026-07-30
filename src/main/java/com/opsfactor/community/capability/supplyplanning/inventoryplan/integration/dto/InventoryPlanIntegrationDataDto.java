package com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.dto;

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
 * Linha Community read-only do Inventory Plan simples por supply plan.
 *
 * <p>Este DTO preserva a unica subopcao de Supply Planning Data Upload aberta
 * no Community: estoque projetado por material/location/periodo. Cobertura em
 * dias, lotes, shifts, impostos, P&L e demais extracoes analiticas continuam
 * fora deste contrato.</p>
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryPlanIntegrationDataDto extends IntegrationDataDtoAbstract<
        InventoryPlanIntegrationDataDto,
        InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO,
        InventoryPlanIntegrationDataDto> {

    /**
     * Unidade em que a linha de inventory plan esta persistida.
     */
    public String unitOfMeasureId;

    /**
     * Estoque de seguranca/minimo do plano irrestrito.
     */
    public Double safetyStockQuantity;

    /**
     * Estoque maximo do plano irrestrito.
     */
    public Double maximumStockQuantity;

    /**
     * Estoque projetado da versao de trabalho.
     */
    public Double projectedStockWorkingVersion;

    /**
     * Estoque projetado da versao irrestrita.
     */
    public Double projectedStockUnconstrainedVersion;

    /**
     * Estoque projetado da versao restrita.
     */
    public Double projectedStockConstrainedVersion;

    /**
     * Unidade padrao SNP usada nas colunas derivadas.
     */
    public String defaultSnpUnitOfMeasureId;

    /**
     * Estoque projetado de trabalho convertido para a UOM padrao SNP, ou
     * mensagem textual quando nao houver conversao.
     */
    public Object projectedStockWorkingVersionDefaultSnpUom;

    /**
     * Estoque projetado irrestrito convertido para a UOM padrao SNP, ou
     * mensagem textual quando nao houver conversao.
     */
    public Object projectedStockUnconstrainedVersionDefaultSnpUom;

    /**
     * Estoque projetado restrito convertido para a UOM padrao SNP, ou mensagem
     * textual quando nao houver conversao.
     */
    public Object projectedStockConstrainedVersionDefaultSnpUom;

    /**
     * Chave publica da linha de Inventory Plan.
     */
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class InventoryPlanPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<
            InventoryPlanPrimaryKeyIntegrationDTO,
            InventoryPlanIntegrationDataDto> {

        /**
         * Plano de Supply Planning exportado.
         */
        public Long supplyPlanId;

        /**
         * Location em que o estoque e projetado.
         */
        public String locationId;

        /**
         * Material da linha de estoque.
         */
        public String materialId;

        /**
         * Data de referencia que representa o fim do periodo.
         */
        public LocalDateTime referenceDate;

        @JsonCreator
        public InventoryPlanPrimaryKeyIntegrationDTO(
                @JsonProperty("supplyPlanId") Long supplyPlanId,
                @JsonProperty("locationId") String locationId,
                @JsonProperty("materialId") String materialId,
                @JsonProperty("referenceDate") LocalDateTime referenceDate) {

            this.supplyPlanId = supplyPlanId;
            this.locationId = locationId;
            this.materialId = materialId;
            this.referenceDate = referenceDate;

        }

        @Override
        public boolean hasSameKeyAsEntity(
                InventoryPlanIntegrationDataDto entity) {

            InventoryPlanPrimaryKeyIntegrationDTO entityPrimaryKeyDto = entity.primaryKeyDto;
            return entityPrimaryKeyDto != null
                    && Objects.equals(entityPrimaryKeyDto.supplyPlanId, this.supplyPlanId)
                    && Objects.equals(entityPrimaryKeyDto.locationId, this.locationId)
                    && Objects.equals(entityPrimaryKeyDto.materialId, this.materialId)
                    && Objects.equals(entityPrimaryKeyDto.referenceDate, this.referenceDate);

        }

    }

}
