package com.opsfactor.community.capability.supplyplanning.supplyplan.integration.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.dto.FulfilledDemandIntegrationDataDto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Exporta a fotografia persistida de demanda atendida de um Supply Plan Community.
 *
 * <p>O contrato publica somente quantidades operacionais. Diferentemente do
 * Production Overview, ele não reconverte todas as séries para uma UOM escolhida
 * na tela: cada linha preserva sua unidade física cadastrada, como o extrato de
 * Direct Demand de referência. Isso mantém a seleção limitada ao plano e ao
 * período, sem introduzir um filtro que altere o recorte do arquivo.</p>
 */
@Service
public class FulfilledDemandIntegrationService {

    private static final double QUANTITY_TOLERANCE = 0.000001D;

    /** Resolve o Supply Plan obrigatório antes de consultar sua fotografia. */
    @Autowired
    private SupplyPlanRepository supplyPlanRepository;

    /** Lê as linhas persistidas com Supply Plan, material, location e UOM em lote. */
    @Autowired
    private DemandaDiretaConsideradaLinhaRepository directDemandLineRepository;

    /**
     * Retorna a extração integral, ordenada e física de demanda atendida do plano.
     *
     * @param supplyPlanId plano cuja fotografia será exportada.
     * @return linhas por location, material e período, na unidade da própria linha.
     */
    public List<FulfilledDemandIntegrationDataDto> getFulfilledDemandDtoList(
            Long supplyPlanId) {

        SupplyPlan supplyPlan = getRequiredSupplyPlan(supplyPlanId);
        return buildDtoList(
                directDemandLineRepository.customFindAllBySupplyPlan(supplyPlan),
                "Fulfilled Demand persisted collection");

    }

    /**
     * Retorna a extração do bucket temporal que contém a data de referência.
     *
     * @param supplyPlanId plano cuja fotografia será exportada.
     * @param referenceDate data pertencente ao bucket requerido pelo operador.
     * @return linhas físicas somente do bucket correspondente.
     */
    public List<FulfilledDemandIntegrationDataDto> getFulfilledDemandDtoListByPeriod(
            Long supplyPlanId,
            LocalDate referenceDate) {

        SupplyPlan supplyPlan = getRequiredSupplyPlan(supplyPlanId);
        LocalDate validatedReferenceDate = validateReferenceDate(referenceDate);
        Constantes.TamanhoBucket bucketSize = supplyPlan.getTamanhoBucket() == null
                ? Constantes.TamanhoBucket.MENSAL
                : supplyPlan.getTamanhoBucket();
        LocalDateTime initialDateTime = Calendario.getPrimeiraDataHorarioPeriodo(
                validatedReferenceDate.atStartOfDay(),
                bucketSize);
        LocalDateTime finalDateTime = Calendario.getUltimaDataHorarioPeriodo(
                validatedReferenceDate.atStartOfDay(),
                bucketSize);

        return buildDtoList(
                directDemandLineRepository.customFindAllBySupplyPlanAndDataReferenciaBetween(
                        supplyPlan,
                        initialDateTime,
                        finalDateTime),
                "Fulfilled Demand period collection");

    }

    /** Converts the complete extraction to the matrix returned by FILE endpoints. */
    public List<List<Object>> getFile(Long supplyPlanId) {

        return toFile(getFulfilledDemandDtoList(supplyPlanId));

    }

    /** Converts one selected plan bucket to the matrix returned by FILE endpoints. */
    public List<List<Object>> getFileByPeriod(
            Long supplyPlanId,
            LocalDate referenceDate) {

        return toFile(getFulfilledDemandDtoListByPeriod(supplyPlanId, referenceDate));

    }

    /** Builds the physical row collection without loading material or location one at a time. */
    private List<FulfilledDemandIntegrationDataDto> buildDtoList(
            Collection<DemandaDiretaConsideradaLinha> directDemandLineCollection,
            String collectionDescription) {

        if (directDemandLineCollection == null) {
            throw new IllegalStateException(collectionDescription + " returned null.");
        }

        return directDemandLineCollection.stream()
                .map(this::buildDto)
                .sorted(Comparator
                        .comparing(FulfilledDemandIntegrationDataDto::getLocationId)
                        .thenComparing(FulfilledDemandIntegrationDataDto::getMaterialId)
                        .thenComparing(FulfilledDemandIntegrationDataDto::getReferenceDate))
                .toList();

    }

    /** Translates a persisted direct-demand line into the reduced Community fulfillment contract. */
    private FulfilledDemandIntegrationDataDto buildDto(DemandaDiretaConsideradaLinha directDemandLine) {

        double unconstrainedDemand = directDemandLine.getQuantidadeDemandaDiretaIrrestrita();
        double fulfilledDemand = directDemandLine.getQuantidadeDemandaDiretaRestrita();
        validateQuantities(unconstrainedDemand, fulfilledDemand, directDemandLine.getMaterial().getId());
        UnidadeMedida unitOfMeasure = directDemandLine.getUnidadeMedidaCadastrado();

        return FulfilledDemandIntegrationDataDto.builder()
                .supplyPlanId(directDemandLine.getSupplyPlan().getId())
                .locationId(directDemandLine.getLocation().getId())
                .locationDescription(directDemandLine.getLocation().getDescricao())
                .materialId(directDemandLine.getMaterial().getId())
                .materialDescription(directDemandLine.getMaterial().getDescricao())
                .referenceDate(directDemandLine.getDataReferencia())
                .unitOfMeasureId(unitOfMeasure == null ? null : unitOfMeasure.getId())
                .unconstrainedDemand(unconstrainedDemand)
                .fulfilledDemand(fulfilledDemand)
                .unmetDemand(unconstrainedDemand - fulfilledDemand)
                .fulfillmentRate(unconstrainedDemand == 0D ? null : fulfilledDemand / unconstrainedDemand)
                .build();

    }

    /** Produces a browser-independent matrix with the public Community columns. */
    private List<List<Object>> toFile(List<FulfilledDemandIntegrationDataDto> fulfilledDemandList) {

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

        for (FulfilledDemandIntegrationDataDto fulfilledDemand : fulfilledDemandList) {
            fileContents.add(List.of(
                    fulfilledDemand.getSupplyPlanId(),
                    fulfilledDemand.getLocationId(),
                    nullableCell(fulfilledDemand.getLocationDescription()),
                    fulfilledDemand.getMaterialId(),
                    nullableCell(fulfilledDemand.getMaterialDescription()),
                    fulfilledDemand.getReferenceDate(),
                    nullableCell(fulfilledDemand.getUnitOfMeasureId()),
                    fulfilledDemand.getUnconstrainedDemand(),
                    fulfilledDemand.getFulfilledDemand(),
                    fulfilledDemand.getUnmetDemand(),
                    nullableCell(fulfilledDemand.getFulfillmentRate())));
        }

        return fileContents;

    }

    /** Rejects invalid persisted snapshots instead of emitting misleading fulfillment ratios. */
    private void validateQuantities(
            double unconstrainedDemand,
            double fulfilledDemand,
            String materialId) {

        if (!Double.isFinite(unconstrainedDemand) || !Double.isFinite(fulfilledDemand)) {
            throw new DataUploadException(
                    "Fulfilled Demand contains a non-finite quantity for material " + materialId + ".");
        }

        if (fulfilledDemand - unconstrainedDemand > QUANTITY_TOLERANCE) {
            throw new DataUploadException(
                    "Fulfilled Demand exceeds unconstrained demand for material " + materialId + ".");
        }

    }

    /** Resolves the plan once before the repository query and fails explicitly for invalid ids. */
    private SupplyPlan getRequiredSupplyPlan(Long supplyPlanId) {

        if (supplyPlanId == null || supplyPlanId <= 0L) {
            throw new DataUploadException("A positive Supply Plan Id is required.");
        }

        return supplyPlanRepository.findById(supplyPlanId)
                .orElseThrow(() -> new DataUploadException("Supply Plan Id not found."));

    }

    /** Validates the date that identifies a calendar bucket. */
    private LocalDate validateReferenceDate(LocalDate referenceDate) {

        if (referenceDate == null) {
            throw new DataUploadException("A plan reference date is required.");
        }

        return referenceDate;

    }

    /** Keeps matrix rows non-null because spreadsheet serializers do not accept null cells. */
    private Object nullableCell(Object value) {

        return value == null ? "" : value;

    }

}
