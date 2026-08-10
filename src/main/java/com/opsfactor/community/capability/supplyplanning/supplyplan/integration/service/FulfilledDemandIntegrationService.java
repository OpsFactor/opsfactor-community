package com.opsfactor.community.capability.supplyplanning.supplyplan.integration.service;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.dto.FulfilledDemandIntegrationDataDto;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.bi.facade.CommunityProductionOverviewService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunitySupplyOverviewBaseDTO.DirectAndIndirectDemandDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exporta a fotografia fisica de demanda atendida de um Supply Plan Community.
 */
@Service
public class FulfilledDemandIntegrationService {

    private static final double QUANTITY_TOLERANCE = 0.000001D;

    /** Fonte canonica das mesmas series exibidas pelo Production Overview. */
    @Autowired
    private CommunityProductionOverviewService communityProductionOverviewService;

    /** Carga batch das descricoes das locations publicadas. */
    @Autowired
    private LocationRepository locationRepository;

    /** Carga batch das descricoes dos materiais publicados. */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Retorna a extracao ordenada de demanda atendida do plano informado.
     *
     * @param supplyPlanId plano cuja fotografia sera exportada.
     * @return linhas fisicas por location, material e periodo.
     */
    public List<FulfilledDemandIntegrationDataDto> getFulfilledDemandDtoList(
            Long supplyPlanId,
            String unitOfMeasureId) {

        Long validatedSupplyPlanId = validateSupplyPlanId(supplyPlanId);
        String validatedUnitOfMeasureId = validateUnitOfMeasureId(unitOfMeasureId);

        CommunityProductionOverviewSelectionDTO selection =
                new CommunityProductionOverviewSelectionDTO();
        selection.supplyPlanId = validatedSupplyPlanId;
        selection.uomId = validatedUnitOfMeasureId;
        CommunityProductionOverviewDTO productionOverview =
                communityProductionOverviewService.getProductionOverview(selection);
        if (productionOverview == null) {
            throw new IllegalStateException("Production Overview returned null for Fulfilled Demand export.");
        }

        Map<String, String> locationDescriptionById = loadLocationDescriptions(productionOverview);
        Map<String, String> materialDescriptionById = loadMaterialDescriptions(productionOverview);
        List<FulfilledDemandIntegrationDataDto> fulfilledDemandList = new ArrayList<>();

        for (DirectAndIndirectDemandDTO demandSeries :
                productionOverview.directAndIndirectDemandByLocationAndMaterialGrouping) {
            String materialId = demandSeries.getMaterialCharacteristicValues().get("materialId");
            if (materialId == null || materialId.isBlank()) {
                throw new DataUploadException("Fulfilled Demand series has no material id.");
            }

            int numberOfPeriods = Math.min(
                    productionOverview.finalDateTimeByPeriod.size(),
                    Math.min(
                            demandSeries.unconstrainedDirectDemand.length,
                            demandSeries.constrainedDirectDemand.length));
            for (int period = 0; period < numberOfPeriods; period++) {
                double unconstrainedDemand = demandSeries.unconstrainedDirectDemand[period];
                double fulfilledDemand = demandSeries.constrainedDirectDemand[period];
                if (Math.abs(unconstrainedDemand) <= QUANTITY_TOLERANCE
                        && Math.abs(fulfilledDemand) <= QUANTITY_TOLERANCE) {
                    continue;
                }

                validateQuantities(
                        unconstrainedDemand,
                        fulfilledDemand,
                        materialId);
                fulfilledDemandList.add(buildDto(
                        validatedSupplyPlanId,
                        demandSeries.locationId,
                        locationDescriptionById.get(demandSeries.locationId),
                        materialId,
                        materialDescriptionById.get(materialId),
                        productionOverview.finalDateTimeByPeriod.get(period),
                        demandSeries.quantityUomId,
                        unconstrainedDemand,
                        fulfilledDemand));
            }
        }

        return fulfilledDemandList;

    }

    /**
     * Converte a mesma extracao em matriz com cabecalho para download tabular.
     */
    public List<List<Object>> getFile(
            Long supplyPlanId,
            String unitOfMeasureId) {

        List<List<Object>> fileContents = new ArrayList<>();
        fileContents.add(List.of(
                "Supply Plan Id",
                "Location Id",
                "Location Description",
                "Material Id",
                "Material Description",
                "Reference Date",
                "Unit of Measure Id",
                "Unconstrained Demand",
                "Fulfilled Demand",
                "Unmet Demand",
                "Fulfillment Rate"));

        for (FulfilledDemandIntegrationDataDto fulfilledDemand :
                getFulfilledDemandDtoList(supplyPlanId, unitOfMeasureId)) {
            fileContents.add(List.of(
                    fulfilledDemand.getSupplyPlanId(),
                    fulfilledDemand.getLocationId(),
                    nullableCell(fulfilledDemand.getLocationDescription()),
                    fulfilledDemand.getMaterialId(),
                    nullableCell(fulfilledDemand.getMaterialDescription()),
                    fulfilledDemand.getReferenceDate(),
                    fulfilledDemand.getUnitOfMeasureId(),
                    fulfilledDemand.getUnconstrainedDemand(),
                    fulfilledDemand.getFulfilledDemand(),
                    fulfilledDemand.getUnmetDemand(),
                    nullableCell(fulfilledDemand.getFulfillmentRate())));
        }

        return fileContents;

    }

    private FulfilledDemandIntegrationDataDto buildDto(
            Long supplyPlanId,
            String locationId,
            String locationDescription,
            String materialId,
            String materialDescription,
            LocalDateTime referenceDate,
            String unitOfMeasureId,
            double unconstrainedDemand,
            double fulfilledDemand) {

        return FulfilledDemandIntegrationDataDto.builder()
                .supplyPlanId(supplyPlanId)
                .locationId(locationId)
                .locationDescription(locationDescription)
                .materialId(materialId)
                .materialDescription(materialDescription)
                .referenceDate(referenceDate)
                .unitOfMeasureId(unitOfMeasureId)
                .unconstrainedDemand(unconstrainedDemand)
                .fulfilledDemand(fulfilledDemand)
                .unmetDemand(unconstrainedDemand - fulfilledDemand)
                .fulfillmentRate(unconstrainedDemand == 0D
                        ? null
                        : fulfilledDemand / unconstrainedDemand)
                .build();

    }

    private void validateQuantities(
            double unconstrainedDemand,
            double fulfilledDemand,
            String materialId) {

        if (!Double.isFinite(unconstrainedDemand) || !Double.isFinite(fulfilledDemand)) {
            throw new DataUploadException(
                    "Fulfilled Demand contains a non-finite quantity for material "
                            + materialId + ".");
        }

        if (fulfilledDemand - unconstrainedDemand > QUANTITY_TOLERANCE) {
            throw new DataUploadException(
                    "Fulfilled Demand exceeds unconstrained demand for material "
                            + materialId + ".");
        }

    }

    private Map<String, String> loadLocationDescriptions(
            CommunityProductionOverviewDTO productionOverview) {

        Set<String> locationIds = productionOverview
                .directAndIndirectDemandByLocationAndMaterialGrouping.stream()
                .map(demandSeries -> demandSeries.locationId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> descriptionById = new HashMap<>();
        for (Location location : locationRepository.findAllById(locationIds)) {
            descriptionById.put(location.getId(), location.getDescricao());
        }
        return descriptionById;

    }

    private Map<String, String> loadMaterialDescriptions(
            CommunityProductionOverviewDTO productionOverview) {

        Set<String> materialIds = productionOverview
                .directAndIndirectDemandByLocationAndMaterialGrouping.stream()
                .map(demandSeries -> demandSeries.getMaterialCharacteristicValues().get("materialId"))
                .filter(materialId -> materialId != null && !materialId.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> descriptionById = new HashMap<>();
        for (Produto material : produtoRepository.findAllById(materialIds)) {
            descriptionById.put(material.getId(), material.getDescricao());
        }
        return descriptionById;

    }

    private Long validateSupplyPlanId(
            Long supplyPlanId) {

        if (supplyPlanId == null || supplyPlanId <= 0L) {
            throw new DataUploadException("A positive Supply Plan Id is required");
        }

        return supplyPlanId;

    }

    private String validateUnitOfMeasureId(
            String unitOfMeasureId) {

        if (unitOfMeasureId == null || unitOfMeasureId.isBlank()) {
            throw new DataUploadException("A unit of measure is required for Fulfilled Demand");
        }

        return unitOfMeasureId.trim();

    }

    private Object nullableCell(
            Object value) {

        return value == null ? "" : value;

    }

}
