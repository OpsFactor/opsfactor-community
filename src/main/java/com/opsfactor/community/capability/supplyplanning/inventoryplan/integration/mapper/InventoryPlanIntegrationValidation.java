package com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.mapper;

import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.dto.InventoryPlanIntegrationDataDto;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Validacoes do contrato read-only de Inventory Plan Community.
 */
public class InventoryPlanIntegrationValidation {

    /**
     * Valida o filtro obrigatorio de Supply Plan usado nos endpoints manuais.
     */
    public static Long validaSupplyPlanId(
            Long supplyPlanId) {

        if (supplyPlanId == null) {
            throw new IllegalArgumentException("Inventory Plan supplyPlanId is required.");
        }
        return supplyPlanId;

    }

    /**
     * Valida parametros globais antes de usar a UOM padrao SNP no export.
     */
    public static UnidadeMedida validaUnidadeMedidaPadraoSnp(
            ParametrosGlobais parametrosGlobais) {

        if (parametrosGlobais == null) {
            throw new IllegalStateException("Global parameters are required to export Inventory Plan.");
        }
        UnidadeMedida unidadeMedidaPadraoSnp = parametrosGlobais.getUnidadeMedidaPadraoSNP();
        if (unidadeMedidaPadraoSnp == null || isBlank(unidadeMedidaPadraoSnp.getId())) {
            throw new IllegalStateException("Default SNP unit of measure is required to export Inventory Plan.");
        }
        return unidadeMedidaPadraoSnp;

    }

    /**
     * Valida colecao de chaves antes de deduplicar o envelope de supply plans.
     */
    public static Collection<InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO> validaPrimaryKeyCollection(
            Collection<InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO> primaryKeyCollection,
            String collectionDescription) {

        if (primaryKeyCollection == null) {
            throw new IllegalArgumentException(collectionDescription + " is required.");
        }

        Set<InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO> primaryKeySet =
                new HashSet<>();
        int indice = 0;
        for (InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO primaryKey : primaryKeyCollection) {
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
     * Valida DTOs produzidos para arquivo/JSON.
     */
    public static Collection<InventoryPlanIntegrationDataDto> validaDtoCollection(
            Collection<InventoryPlanIntegrationDataDto> dtoCollection,
            String collectionDescription) {

        if (dtoCollection == null) {
            throw new IllegalStateException(collectionDescription + " returned null.");
        }

        Set<InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO> primaryKeySet =
                new HashSet<>();
        int indice = 0;
        for (InventoryPlanIntegrationDataDto dto : dtoCollection) {
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
            validaDtoNumerico(
                    dto,
                    indice);
            indice++;
        }
        return dtoCollection;

    }

    /**
     * Valida quantidade preenchida sem impedir estoque projetado negativo.
     */
    @Nullable
    public static Double validaNumeroFinitoOuNulo(
            @Nullable Double value,
            String valueDescription) {

        if (value == null) {
            return null;
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Inventory Plan "
                            + valueDescription
                            + " must be finite: "
                            + value
                            + ".");
        }
        return value;

    }

    /**
     * Valida quantidade fisica que o dominio ja trata como nao negativa.
     */
    @Nullable
    public static Double validaNumeroNaoNegativoOuNulo(
            @Nullable Double value,
            String valueDescription) {

        if (value == null) {
            return null;
        }
        if (!Double.isFinite(value) || value < 0.0d) {
            throw new IllegalArgumentException(
                    "Inventory Plan "
                            + valueDescription
                            + " must be finite and non-negative: "
                            + value
                            + ".");
        }
        return value;

    }

    private static void validaDtoNumerico(
            InventoryPlanIntegrationDataDto dto,
            int indice) {

        try {
            validaNumeroNaoNegativoOuNulo(dto.safetyStockQuantity, "safety stock quantity");
            validaNumeroNaoNegativoOuNulo(dto.maximumStockQuantity, "maximum stock quantity");
            validaNumeroFinitoOuNulo(dto.projectedStockWorkingVersion, "projected stock working version");
            validaNumeroFinitoOuNulo(dto.projectedStockUnconstrainedVersion, "projected stock unconstrained version");
            validaNumeroFinitoOuNulo(dto.projectedStockConstrainedVersion, "projected stock constrained version");
            validaValorNumericoFinitoOuMensagem(
                    dto.projectedStockWorkingVersionDefaultSnpUom,
                    "projected stock working version in default SNP unit");
            validaValorNumericoFinitoOuMensagem(
                    dto.projectedStockUnconstrainedVersionDefaultSnpUom,
                    "projected stock unconstrained version in default SNP unit");
            validaValorNumericoFinitoOuMensagem(
                    dto.projectedStockConstrainedVersionDefaultSnpUom,
                    "projected stock constrained version in default SNP unit");
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(
                    "Inventory Plan DTO at index "
                            + indice
                            + " is invalid: "
                            + illegalArgumentException.getMessage(),
                    illegalArgumentException);
        }

    }

    private static void validaValorNumericoFinitoOuMensagem(
            Object valueOrMessage,
            String valueDescription) {

        if (!(valueOrMessage instanceof Number valueNumber)) {
            return;
        }
        double value = valueNumber.doubleValue();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Inventory Plan "
                            + valueDescription
                            + " must be finite: "
                            + value
                            + ".");
        }

    }

    private static void validaPrimaryKey(
            InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO primaryKey,
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
        if (isBlank(primaryKey.materialId)) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without material id at index "
                            + indice
                            + ".");
        }
        if (primaryKey.referenceDate == null) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without reference date at index "
                            + indice
                            + ".");
        }

    }

    private static String getPrimaryKeyDescription(
            InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO primaryKey) {

        return "supply plan "
                + primaryKey.supplyPlanId
                + " / location "
                + primaryKey.locationId
                + " / material "
                + primaryKey.materialId
                + " / reference date "
                + primaryKey.referenceDate;

    }

    private static boolean isBlank(
            String value) {

        return value == null || value.isBlank();

    }

}
