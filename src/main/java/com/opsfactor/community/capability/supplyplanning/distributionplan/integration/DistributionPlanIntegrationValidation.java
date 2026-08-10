package com.opsfactor.community.capability.supplyplanning.distributionplan.integration;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Validacoes do contrato Enterprise read-only de Distribution Plan.
 */
public class DistributionPlanIntegrationValidation {

    /**
     * Valida o filtro obrigatorio de Supply Plan usado nos endpoints manuais.
     */
    public static Long validaSupplyPlanId(
            Long supplyPlanId) {

        if (supplyPlanId == null) {
            throw new IllegalArgumentException("Distribution Plan supplyPlanId is required.");
        }
        return supplyPlanId;

    }

    /**
     * Valida parametros globais antes de resolver a UOM padrao SNP usada como
     * fallback pelas linhas sem UOM propria.
     */
    public static UnidadeMedida validaUnidadeMedidaPadraoSnp(
            ParametrosGlobais parametrosGlobais) {

        if (parametrosGlobais == null) {
            throw new IllegalStateException(
                    "Global parameters are required to export Distribution Plan.");
        }
        UnidadeMedida unidadeMedidaPadraoSnp = parametrosGlobais.getUnidadeMedidaPadraoSNP();
        if (unidadeMedidaPadraoSnp == null || isBlank(unidadeMedidaPadraoSnp.getId())) {
            throw new IllegalStateException(
                    "Default SNP unit of measure is required to export Distribution Plan.");
        }
        return unidadeMedidaPadraoSnp;

    }

    /**
     * Valida uma entidade persistida antes de montar o DTO publicado.
     */
    public static void validaDistributionPlanItem(
            DistributionPlanItem distributionPlanItem,
            String collectionDescription,
            int indice) {

        if (distributionPlanItem == null) {
            throw new IllegalStateException(
                    collectionDescription
                            + " returned null item at index "
                            + indice
                            + ".");
        }
        if (distributionPlanItem.getKey() == null) {
            throw new IllegalStateException(
                    collectionDescription
                            + " returned item without composite key at index "
                            + indice
                            + ".");
        }

        SupplyPlan supplyPlan = distributionPlanItem.getSupplyPlan();
        Location originLocation = distributionPlanItem.getLocationOrigem();
        Location destinationLocation = distributionPlanItem.getLocationDestino();
        Produto material = distributionPlanItem.getProduto();
        LocalDateTime plannedDeliveryDate = distributionPlanItem.getDataRecebimento();
        LocalDateTime suggestedOrderEmissionDate = distributionPlanItem.getDataExpedicao();

        if (supplyPlan == null || supplyPlan.getId() == null) {
            throw new IllegalStateException(
                    collectionDescription
                            + " returned item without supply plan at index "
                            + indice
                            + ".");
        }
        if (originLocation == null || isBlank(originLocation.getId())) {
            throw new IllegalStateException(
                    collectionDescription
                            + " returned item without origin location at index "
                            + indice
                            + ".");
        }
        if (destinationLocation == null || isBlank(destinationLocation.getId())) {
            throw new IllegalStateException(
                    collectionDescription
                            + " returned item without destination location at index "
                            + indice
                            + ".");
        }
        if (material == null || isBlank(material.getId())) {
            throw new IllegalStateException(
                    collectionDescription
                            + " returned item without material at index "
                            + indice
                            + ".");
        }
        if (plannedDeliveryDate == null) {
            throw new IllegalStateException(
                    collectionDescription
                            + " returned item without planned delivery date at index "
                            + indice
                            + ".");
        }
        if (suggestedOrderEmissionDate == null) {
            throw new IllegalStateException(
                    collectionDescription
                            + " returned item without suggested order emission date at index "
                            + indice
                            + ".");
        }

    }

    /**
     * Valida colecao de chaves antes de deduplicar o envelope de supply plans.
     */
    public static Collection<DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO> validaPrimaryKeyCollection(
            Collection<DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO> primaryKeyCollection,
            String collectionDescription) {

        if (primaryKeyCollection == null) {
            throw new IllegalArgumentException(collectionDescription + " is required.");
        }

        Set<DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO> primaryKeySet =
                new HashSet<>();
        int indice = 0;
        for (DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO primaryKey : primaryKeyCollection) {
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
    public static Collection<DistributionPlanIntegrationDataDto> validaDtoCollection(
            Collection<DistributionPlanIntegrationDataDto> dtoCollection,
            String collectionDescription) {

        if (dtoCollection == null) {
            throw new IllegalStateException(collectionDescription + " returned null.");
        }

        Set<DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO> primaryKeySet =
                new HashSet<>();
        int indice = 0;
        for (DistributionPlanIntegrationDataDto dto : dtoCollection) {
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
            if (isBlank(dto.unitOfMeasureId)) {
                throw new IllegalStateException(
                        collectionDescription
                                + " returned item without unit of measure at index "
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
     * Valida quantidade fisica de distribuicao, que deve ser finita e nao
     * negativa quando presente.
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
                    "Distribution Plan "
                            + valueDescription
                            + " must be finite and non-negative: "
                            + value
                            + ".");
        }
        return value;

    }

    private static void validaDtoNumerico(
            DistributionPlanIntegrationDataDto dto,
            int indice) {

        try {
            validaNumeroNaoNegativoOuNulo(dto.unconstrainedPlannedOrderQuantity, "unconstrained planned order quantity");
            validaNumeroNaoNegativoOuNulo(dto.unconstrainedFirmOrderQuantity, "unconstrained firm order quantity");
            validaNumeroNaoNegativoOuNulo(dto.constrainedPlannedOrderQuantity, "constrained planned order quantity");
            validaNumeroNaoNegativoOuNulo(dto.constrainedFirmOrderQuantity, "constrained firm order quantity");
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(
                    "Distribution Plan DTO at index "
                            + indice
                            + " is invalid: "
                            + illegalArgumentException.getMessage(),
                    illegalArgumentException);
        }

    }

    private static void validaPrimaryKey(
            DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO primaryKey,
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
        if (isBlank(primaryKey.originLocationId)) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without origin location id at index "
                            + indice
                            + ".");
        }
        if (isBlank(primaryKey.destinationLocationId)) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without destination location id at index "
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
        if (primaryKey.plannedDeliveryDate == null) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without planned delivery date at index "
                            + indice
                            + ".");
        }
        if (primaryKey.suggestedOrderEmissionDate == null) {
            throw new IllegalArgumentException(
                    collectionDescription
                            + " contains primary key without suggested order emission date at index "
                            + indice
                            + ".");
        }

    }

    private static String getPrimaryKeyDescription(
            DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO primaryKey) {

        return "supply plan "
                + primaryKey.supplyPlanId
                + " / origin "
                + primaryKey.originLocationId
                + " / destination "
                + primaryKey.destinationLocationId
                + " / material "
                + primaryKey.materialId
                + " / planned delivery date "
                + primaryKey.plannedDeliveryDate
                + " / suggested order emission date "
                + primaryKey.suggestedOrderEmissionDate;

    }

    private static boolean isBlank(
            String value) {

        return value == null || value.isBlank();

    }

}
