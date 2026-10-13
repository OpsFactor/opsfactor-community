package com.opsfactor.community.capability.supplyplanning.productionplan.integration;

import com.opsfactor.community.platform.integration.validation.IntegrationFileColumn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Linha Enterprise read-only do Production Plan no nivel de volume por
 * roteiro/BOM.
 *
 * <p>O DTO preserva o contrato legado de arquivo e JSON, incluindo as
 * quantidades na unidade operacional da linha e as colunas derivadas na unidade
 * padrao SNP. A importacao permanece bloqueada porque o Production Plan e
 * persistido pelas rotinas de Supply Planning, nao por Data Upload manual.</p>
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductionPlanVolumeIntegrationDataDto extends IntegrationDataDtoAbstract<
        ProductionPlanVolumeIntegrationDataDto,
        ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO,
        ProductionPlanVolumeIntegrationDataDto> {

    /**
     * Unidade de medida operacional em que as quantidades da linha foram
     * persistidas.
     */
    @IntegrationFileColumn(7)
    public String unitOfMeasureId;

    /**
     * Quantidade planejada do plano irrestrito na unidade da linha.
     */
    @IntegrationFileColumn(8)
    public Double unconstrainedPlannedOrderQuantity;

    /**
     * Quantidade firme do plano irrestrito na unidade da linha.
     */
    @IntegrationFileColumn(9)
    public Double unconstrainedFirmOrderQuantity;

    /**
     * Quantidade planejada do plano restrito na unidade da linha.
     */
    @IntegrationFileColumn(10)
    public Double constrainedPlannedOrderQuantity;

    /**
     * Quantidade firme do plano restrito na unidade da linha.
     */
    @IntegrationFileColumn(11)
    public Double constrainedFirmOrderQuantity;

    /**
     * Unidade padrao SNP usada para as colunas derivadas de comparacao.
     */
    @IntegrationFileColumn(12)
    public String defaultSnpUnitOfMeasureId;

    /**
     * Quantidade planejada irrestrita convertida para a unidade padrao SNP, ou
     * mensagem textual quando a conversao nao existir.
     */
    @IntegrationFileColumn(13)
    public Object unconstrainedPlannedOrderQuantityDefaultSnpUom;

    /**
     * Quantidade firme irrestrita convertida para a unidade padrao SNP, ou
     * mensagem textual quando a conversao nao existir.
     */
    @IntegrationFileColumn(14)
    public Object unconstrainedFirmOrderQuantityDefaultSnpUom;

    /**
     * Quantidade planejada restrita convertida para a unidade padrao SNP, ou
     * mensagem textual quando a conversao nao existir.
     */
    @IntegrationFileColumn(15)
    public Object constrainedPlannedOrderQuantityDefaultSnpUom;

    /**
     * Quantidade firme restrita convertida para a unidade padrao SNP, ou
     * mensagem textual quando a conversao nao existir.
     */
    @IntegrationFileColumn(16)
    public Object constrainedFirmOrderQuantityDefaultSnpUom;

    /**
     * Chave composta publica da linha de volume do Production Plan.
     */
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class ProductionPlanVolumePrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<
            ProductionPlanVolumePrimaryKeyIntegrationDTO,
            ProductionPlanVolumeIntegrationDataDto> {

        /**
         * Identificador do Supply Plan exportado.
         */
        @IntegrationFileColumn(0)
        public Long supplyPlanId;

        /**
         * Location produtiva da linha.
         */
        @IntegrationFileColumn(1)
        public String locationId;

        /**
         * Material output planejado.
         */
        @IntegrationFileColumn(2)
        public String outputMaterialId;

        /**
         * Data/bucket planejado da producao.
         */
        @IntegrationFileColumn(3)
        public LocalDateTime plannedDate;

        /**
         * Versao de producao usada, nula quando a linha veio da sentinela
         * "sem versao de producao".
         */
        @IntegrationFileColumn(4)
        public String productionVersionId;

        /**
         * Roteiro produtivo associado a linha.
         */
        @IntegrationFileColumn(5)
        public String routingId;

        /**
         * Lista tecnica associada a linha.
         */
        @IntegrationFileColumn(6)
        public String billOfMaterialsId;

        @Override
        public boolean hasSameKeyAsEntity(
                ProductionPlanVolumeIntegrationDataDto entity) {

            ProductionPlanVolumePrimaryKeyIntegrationDTO entityPrimaryKeyDto = entity.primaryKeyDto;
            return entityPrimaryKeyDto != null
                    && java.util.Objects.equals(entityPrimaryKeyDto.supplyPlanId, this.supplyPlanId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.locationId, this.locationId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.outputMaterialId, this.outputMaterialId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.plannedDate, this.plannedDate)
                    && java.util.Objects.equals(entityPrimaryKeyDto.productionVersionId, this.productionVersionId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.routingId, this.routingId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.billOfMaterialsId, this.billOfMaterialsId);

        }

    }

}
