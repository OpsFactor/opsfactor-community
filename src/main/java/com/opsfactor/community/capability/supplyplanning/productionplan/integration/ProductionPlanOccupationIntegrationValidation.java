package com.opsfactor.community.capability.supplyplanning.productionplan.integration;

import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Validacoes defensivas do Data Upload read-only de Production Plan Occupation.
 */
public final class ProductionPlanOccupationIntegrationValidation {

    private ProductionPlanOccupationIntegrationValidation() {
    }

    /**
     * Valida o filtro obrigatorio dos endpoints customizados por supply plan.
     */
    public static Long validaSupplyPlanId(
            Long supplyPlanId) {

        if (supplyPlanId == null) {
            throw new IllegalArgumentException("Production Plan Occupation supplyPlanId is required.");
        }
        return supplyPlanId;

    }

    /**
     * Valida e preserva a colecao de chaves primarias recebida pela
     * infraestrutura generica de batch.
     */
    public static Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO> validaPrimaryKeyCollection(
            Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO> primaryKeyCollection,
            String collectionDescription) {

        if (primaryKeyCollection == null) {
            throw new IllegalArgumentException(collectionDescription + " is required.");
        }

        Set<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO> primaryKeySet =
                new HashSet<>();
        int indice = 0;
        for (com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO primaryKey : primaryKeyCollection) {
            validaPrimaryKey(
                    primaryKey,
                    collectionDescription,
                    indice);
            if (!primaryKeySet.add(primaryKey)) {
                throw new IllegalArgumentException(
                        collectionDescription
                                + " contains duplicated key "
                                + getPrimaryKeyDescription(primaryKey)
                                + " at index "
                                + indice
                                + ".");
            }
            indice++;
        }
        return primaryKeyCollection;

    }

    /**
     * Valida uma lista de DTOs produzida para exportacao.
     */
    public static Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> validaDtoCollection(
            Collection<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto> dtoCollection,
            String collectionDescription) {

        if (dtoCollection == null) {
            throw new IllegalStateException(collectionDescription + " returned null.");
        }

        Set<com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO> primaryKeySet =
                new HashSet<>();
        int indice = 0;
        for (com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto dto : dtoCollection) {
            if (dto == null) {
                throw new IllegalStateException(
                        collectionDescription
                                + " returned null item at index "
                                + indice
                                + ".");
            }
            if (dto.primaryKeyDto == null) {
                throw new IllegalStateException(
                        collectionDescription
                                + " returned item without primary key at index "
                                + indice
                                + ".");
            }
            validaPrimaryKey(
                    dto.primaryKeyDto,
                    collectionDescription,
                    indice);
            if (!primaryKeySet.add(dto.primaryKeyDto)) {
                throw new IllegalStateException(
                        collectionDescription
                                + " contains duplicated key "
                                + getPrimaryKeyDescription(dto.primaryKeyDto)
                                + " at index "
                                + indice
                                + ".");
            }
            validaQuantidadeNaoNegativaOuNula(
                    dto.unconstrainedPlannedOrderQuantity,
                    "unconstrained planned order quantity");
            validaQuantidadeNaoNegativaOuNula(
                    dto.unconstrainedFirmOrderQuantity,
                    "unconstrained firm order quantity");
            validaQuantidadeNaoNegativaOuNula(
                    dto.unconstrainedTotalResourceConsumption,
                    "unconstrained total resource consumption");
            validaQuantidadeNaoNegativaOuNula(
                    dto.constrainedPlannedOrderQuantity,
                    "constrained planned order quantity");
            validaQuantidadeNaoNegativaOuNula(
                    dto.constrainedFirmOrderQuantity,
                    "constrained firm order quantity");
            validaQuantidadeNaoNegativaOuNula(
                    dto.constrainedTotalResourceConsumption,
                    "constrained total resource consumption");
            validaValorNumericoOuMensagemNaoNegativo(
                    dto.resourceCapacityPeriod,
                    "resource capacity period");
            validaValorNumericoOuMensagemNaoNegativo(
                    dto.unconstrainedPlannedOrderQuantityDefaultSnpUom,
                    "unconstrained planned order quantity in default SNP unit");
            validaValorNumericoOuMensagemNaoNegativo(
                    dto.unconstrainedFirmOrderQuantityDefaultSnpUom,
                    "unconstrained firm order quantity in default SNP unit");
            validaValorNumericoOuMensagemNaoNegativo(
                    dto.constrainedPlannedOrderQuantityDefaultSnpUom,
                    "constrained planned order quantity in default SNP unit");
            validaValorNumericoOuMensagemNaoNegativo(
                    dto.constrainedFirmOrderQuantityDefaultSnpUom,
                    "constrained firm order quantity in default SNP unit");
            validaValorNumericoOuMensagemNaoNegativo(
                    dto.setupTimeHours,
                    "setup time hours");
            validaValorNumericoOuMensagemNaoNegativo(
                    dto.setupSequence,
                    "setup sequence");
            indice++;
        }
        return dtoCollection;

    }

    /**
     * Valida quantidade preenchida sem transformar erro de dado em zero.
     */
    @Nullable
    public static Double validaQuantidadeNaoNegativaOuNula(
            @Nullable Double quantity,
            String quantityDescription) {

        if (quantity == null) {
            return null;
        }
        if (!Double.isFinite(quantity)
                || quantity < 0.0d) {
            throw new IllegalArgumentException(
                    "Production Plan Occupation "
                            + quantityDescription
                            + " must be finite and non-negative: "
                            + quantity
                            + ".");
        }
        return quantity;

    }

    private static void validaValorNumericoOuMensagemNaoNegativo(
            Object valueOrMessage,
            String valueDescription) {

        if (!(valueOrMessage instanceof Number valueNumber)) {
            return;
        }

        double value = valueNumber.doubleValue();
        validaQuantidadeNaoNegativaOuNula(
                value,
                valueDescription);

    }

    private static void validaPrimaryKey(
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO primaryKey,
            String collectionDescription,
            int indice) {

        if (primaryKey == null) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains null primary key at index "
                            + indice
                            + ".");
        }
        if (primaryKey.supplyPlanId == null) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without supply plan id at index "
                            + indice
                            + ".");
        }
        if (isBlank(primaryKey.locationId)) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without location id at index "
                            + indice
                            + ".");
        }
        if (isBlank(primaryKey.outputMaterialId)) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without output material id at index "
                            + indice
                            + ".");
        }
        if (primaryKey.plannedDate == null) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without planned date at index "
                            + indice
                            + ".");
        }
        if (isBlank(primaryKey.routingId)) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without routing id at index "
                            + indice
                            + ".");
        }
        if (isBlank(primaryKey.billOfMaterialsId)) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without bill of materials id at index "
                            + indice
                            + ".");
        }
        if (isBlank(primaryKey.resourceId)) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without resource id at index "
                            + indice
                            + ".");
        }

    }

    private static String getPrimaryKeyDescription(
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO primaryKey) {

        return "supply plan "
                + primaryKey.supplyPlanId
                + " / location "
                + primaryKey.locationId
                + " / output material "
                + primaryKey.outputMaterialId
                + " / planned date "
                + primaryKey.plannedDate
                + " / production version "
                + primaryKey.productionVersionId
                + " / routing "
                + primaryKey.routingId
                + " / bill of materials "
                + primaryKey.billOfMaterialsId
                + " / resource "
                + primaryKey.resourceId;

    }

    private static boolean isBlank(
            String value) {

        return value == null || value.isBlank();

    }

}
