package com.opsfactor.community.capability.supplyplanning.productionplan.integration;

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
 * Linha Enterprise read-only do Production Plan no nivel de ocupacao de
 * recurso produtivo.
 *
 * <p>O DTO preserva o contrato legado de arquivo e JSON, combinando a linha de
 * producao planejada com o recurso consumido pelo roteiro e, quando houver
 * snapshot Enterprise de setup, as colunas de horas e sequencia de setup. A
 * importacao permanece bloqueada porque volume, consumo e setup sao resultados
 * de Supply Planning/line scheduling.</p>
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductionPlanOccupationIntegrationDataDto extends IntegrationDataDtoAbstract<
        ProductionPlanOccupationIntegrationDataDto,
        ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO,
        ProductionPlanOccupationIntegrationDataDto> {

    /**
     * Cluster de roteiros do legado. No build achatado atual, o vinculo
     * Roteiro -> ClusterRoteiros ainda pertence ao recorte futuro de line
     * scheduling, portanto a exportacao pode permanecer nula.
     */
    public String routingClusterId;

    /**
     * Unidade de medida da capacidade do recurso, ou "Hours" quando a
     * capacidade do perfil e baseada em horas.
     */
    public String resourceCapacityUnitOfMeasureId;

    /**
     * Capacidade do recurso no periodo. O campo e Object porque o contrato
     * legado tambem admite mensagens textuais quando uma dimensao Enterprise
     * ainda nao esta materializada.
     */
    public Object resourceCapacityPeriod;

    /**
     * Unidade operacional das quantidades da linha de Production Plan.
     */
    public String unitOfMeasureId;

    /**
     * Quantidade planejada do plano irrestrito na unidade da linha.
     */
    public Double unconstrainedPlannedOrderQuantity;

    /**
     * Quantidade firme do plano irrestrito na unidade da linha.
     */
    public Double unconstrainedFirmOrderQuantity;

    /**
     * Consumo total de capacidade do recurso no plano irrestrito.
     */
    public Double unconstrainedTotalResourceConsumption;

    /**
     * Quantidade planejada do plano restrito na unidade da linha.
     */
    public Double constrainedPlannedOrderQuantity;

    /**
     * Quantidade firme do plano restrito na unidade da linha.
     */
    public Double constrainedFirmOrderQuantity;

    /**
     * Consumo total de capacidade do recurso no plano restrito.
     */
    public Double constrainedTotalResourceConsumption;

    /**
     * Unidade padrao SNP usada para as colunas derivadas de comparacao.
     */
    public String defaultSnpUnitOfMeasureId;

    /**
     * Quantidade planejada irrestrita convertida para a unidade padrao SNP, ou
     * mensagem textual quando a conversao nao existir.
     */
    public Object unconstrainedPlannedOrderQuantityDefaultSnpUom;

    /**
     * Quantidade firme irrestrita convertida para a unidade padrao SNP, ou
     * mensagem textual quando a conversao nao existir.
     */
    public Object unconstrainedFirmOrderQuantityDefaultSnpUom;

    /**
     * Quantidade planejada restrita convertida para a unidade padrao SNP, ou
     * mensagem textual quando a conversao nao existir.
     */
    public Object constrainedPlannedOrderQuantityDefaultSnpUom;

    /**
     * Quantidade firme restrita convertida para a unidade padrao SNP, ou
     * mensagem textual quando a conversao nao existir.
     */
    public Object constrainedFirmOrderQuantityDefaultSnpUom;

    /**
     * Horas de setup do snapshot Enterprise quando aplicavel.
     */
    public Object setupTimeHours;

    /**
     * Sequencia do setup no periodo quando aplicavel.
     */
    public Object setupSequence;

    /**
     * Chave composta publica da linha de ocupacao do Production Plan.
     */
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class ProductionPlanOccupationPrimaryKeyIntegrationDTO extends IntegrationPrimaryKeyDTOAbstract<
            ProductionPlanOccupationPrimaryKeyIntegrationDTO,
            ProductionPlanOccupationIntegrationDataDto> {

        /**
         * Identificador do Supply Plan exportado.
         */
        public Long supplyPlanId;

        /**
         * Location produtiva da linha.
         */
        public String locationId;

        /**
         * Material output planejado.
         */
        public String outputMaterialId;

        /**
         * Data/bucket planejado da producao ou setup.
         */
        public LocalDateTime plannedDate;

        /**
         * Versao de producao usada, nula quando a linha veio da sentinela
         * "sem versao de producao".
         */
        public String productionVersionId;

        /**
         * Roteiro produtivo associado a ocupacao.
         */
        public String routingId;

        /**
         * Lista tecnica associada a ocupacao.
         */
        public String billOfMaterialsId;

        /**
         * Recurso produtivo ocupado pelo volume ou setup.
         */
        public String resourceId;

        @Override
        public boolean hasSameKeyAsEntity(
                ProductionPlanOccupationIntegrationDataDto entity) {

            ProductionPlanOccupationPrimaryKeyIntegrationDTO entityPrimaryKeyDto = entity.primaryKeyDto;
            return entityPrimaryKeyDto != null
                    && java.util.Objects.equals(entityPrimaryKeyDto.supplyPlanId, this.supplyPlanId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.locationId, this.locationId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.outputMaterialId, this.outputMaterialId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.plannedDate, this.plannedDate)
                    && java.util.Objects.equals(entityPrimaryKeyDto.productionVersionId, this.productionVersionId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.routingId, this.routingId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.billOfMaterialsId, this.billOfMaterialsId)
                    && java.util.Objects.equals(entityPrimaryKeyDto.resourceId, this.resourceId);

        }

    }

}
