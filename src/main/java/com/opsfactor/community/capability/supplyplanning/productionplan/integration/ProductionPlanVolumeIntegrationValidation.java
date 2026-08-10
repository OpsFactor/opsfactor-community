package com.opsfactor.community.capability.supplyplanning.productionplan.integration;

import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Validacoes defensivas do Data Upload read-only de Production Plan Volume.
 */
public class ProductionPlanVolumeIntegrationValidation {

    /**
     * Valida o filtro obrigatorio dos endpoints customizados por supply plan.
     */
    public static Long validaSupplyPlanId(
            Long supplyPlanId) {

        if (supplyPlanId == null) {
            throw new IllegalArgumentException("Production Plan Volume supplyPlanId is required.");
        }
        return supplyPlanId;

    }

    /**
     * Valida e preserva a colecao de chaves primarias recebida pela
     * infraestrutura generica de batch.
     */
    public static Collection<ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO> validaPrimaryKeyCollection(
            Collection<ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO> primaryKeyCollection,
            String collectionDescription) {

        if (primaryKeyCollection == null) {
            throw new IllegalArgumentException(collectionDescription + " is required.");
        }

        Set<ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO> primaryKeySet =
                new HashSet<>();
        int indice = 0;
        for (ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO primaryKey : primaryKeyCollection) {
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
    public static Collection<ProductionPlanVolumeIntegrationDataDto> validaDtoCollection(
            Collection<ProductionPlanVolumeIntegrationDataDto> dtoCollection,
            String collectionDescription) {

        if (dtoCollection == null) {
            throw new IllegalStateException(collectionDescription + " returned null.");
        }

        Set<ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO> primaryKeySet =
                new HashSet<>();
        int indice = 0;
        for (ProductionPlanVolumeIntegrationDataDto dto : dtoCollection) {
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
                    dto.constrainedPlannedOrderQuantity,
                    "constrained planned order quantity");
            validaQuantidadeNaoNegativaOuNula(
                    dto.constrainedFirmOrderQuantity,
                    "constrained firm order quantity");
            validaQuantidadeDerivadaOuMensagem(
                    dto.unconstrainedPlannedOrderQuantityDefaultSnpUom,
                    "unconstrained planned order quantity in default SNP unit");
            validaQuantidadeDerivadaOuMensagem(
                    dto.unconstrainedFirmOrderQuantityDefaultSnpUom,
                    "unconstrained firm order quantity in default SNP unit");
            validaQuantidadeDerivadaOuMensagem(
                    dto.constrainedPlannedOrderQuantityDefaultSnpUom,
                    "constrained planned order quantity in default SNP unit");
            validaQuantidadeDerivadaOuMensagem(
                    dto.constrainedFirmOrderQuantityDefaultSnpUom,
                    "constrained firm order quantity in default SNP unit");
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
                    "Production Plan Volume "
                            + quantityDescription
                            + " must be finite and non-negative: "
                            + quantity
                            + ".");
        }
        return quantity;

    }

    private static void validaQuantidadeDerivadaOuMensagem(
            Object quantityOrMessage,
            String quantityDescription) {

        if (!(quantityOrMessage instanceof Number quantityNumber)) {
            return;
        }

        double quantity = quantityNumber.doubleValue();
        validaQuantidadeNaoNegativaOuNula(
                quantity,
                quantityDescription);

    }

    private static void validaPrimaryKey(
            ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO primaryKey,
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

    }

    private static String getPrimaryKeyDescription(
            ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO primaryKey) {

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
                + primaryKey.billOfMaterialsId;

    }

    private static boolean isBlank(
            String value) {

        return value == null || value.isBlank();

    }

}
