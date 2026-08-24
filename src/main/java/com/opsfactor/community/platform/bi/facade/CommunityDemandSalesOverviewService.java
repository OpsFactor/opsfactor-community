package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.FiltroMaterialLocationDeCombinacaoCaracteristicasDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.mapper.FiltroLocationDeCombinacaoCaracteristicasMapper;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.mapper.FiltroMaterialDeCombinacaoCaracteristicasMapper;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.demandplanning.forecast.configuration.DemandPlanningModelCatalog;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service.UnidadeMedidaService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityDemandSalesOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityDemandSalesOverviewPeriodDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityDemandSalesOverviewSelectionDTO;
import com.opsfactor.community.capability.demandplanning.service.DemandPlanningService;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Visão Community somente leitura para comparar sell-out histórico e Demand
 * Plan irrestrito no nível material/location.
 *
 * <p>O service materializa uma única Sales Projection e uma única Demand Plan
 * Projection por request. Preço, valores monetários, Custom Key Figures,
 * SpEL, valores financeiros e agregações privadas permanecem fora deste
 * recorte. IDs e características públicas usam os mesmos mappers e projection
 * factories recuperados do contrato legado.</p>
 */
@Service
public class CommunityDemandSalesOverviewService {

    /** Resolve a unidade de medida escolhida para a resposta consolidada. */
    @Autowired
    private UnidadeMedidaService unidadeMedidaService;

    /** Carrega os parâmetros globais e escopos ativos do cluster. */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /** Fornece as conversões de unidade indexadas usadas na consolidação. */
    @Autowired
    private UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory;

    /** Materializa a série histórica de vendas no calendário solicitado. */
    @Autowired
    private SalesProjectionFactory salesProjectionFactory;

    /** Localiza o Demand Plan publicado que delimita a consulta. */
    @Autowired
    private DemandPlanningService demandPlanningService;

    /** Constrói a projection do plano de demanda sem leituras linha a linha. */
    @Autowired
    private DemandPlanProjectionFactory demandPlanProjectionFactory;

    /**
     * Carrega e consolida apenas as duas séries Community no escopo solicitado.
     */
    public CommunityDemandSalesOverviewDTO getDemandSalesOverview(
            CommunityDemandSalesOverviewSelectionDTO selectionDTO) {

        validateSelection(selectionDTO);

        DemandPlan demandPlan = selectionDTO.demandPlanId() == null
                ? null
                : demandPlanningService.getDemandPlanDeId(selectionDTO.demandPlanId());
        UnidadeMedida targetUnitOfMeasure = unidadeMedidaService.getUnidadeMedida(
                selectionDTO.unitOfMeasureId());
        ClusterEParametrosProjection clusterAndParametersProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        UnidadeMedidaProjection unitOfMeasureProjection =
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();

        FiltroDFUProjection dfuProjection = getActiveDfuProjection(
                selectionDTO,
                clusterAndParametersProjection);
        Set<LocalDateTime> selectedDemandPlanPeriodEndDates =
                getSelectedDemandPlanPeriodEndDates(
                        selectionDTO,
                        demandPlan,
                        clusterAndParametersProjection);
        DemandPlanningProjection demandPlanningProjection = demandPlan == null
                ? null
                : selectedDemandPlanPeriodEndDates.isEmpty()
                        ? demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                                demandPlan,
                                dfuProjection,
                                false)
                        : demandPlanProjectionFactory
                                .getDemandPlanningProjectionCompletoComDadosNasDatas(
                                        demandPlan,
                                        dfuProjection,
                                        selectedDemandPlanPeriodEndDates,
                                        false);
        Calendario demandPlanCalendar = demandPlanningProjection == null
                ? null
                : demandPlanningProjection.getCalendario();
        Calendario salesCalendar = getSalesCalendar(
                demandPlanCalendar,
                getHistoricalPeriods(selectionDTO.historicalPeriods()));

        SalesProjectionLocationMaterialData salesProjection =
                salesProjectionFactory.getSalesProjectionLocationMaterialData(
                        getHistoricalSalesDocumentType(selectionDTO, demandPlan),
                        salesCalendar,
                        dfuProjection.getLocations(),
                        dfuProjection.getMateriais(),
                        unitOfMeasureProjection,
                        clusterAndParametersProjection,
                        clusterAndParametersProjection.getSNPUnidadeMedidaPadraoGlobal());

        Map<Location, Map<Produto, Map<LocalDateTime, OverviewValues>>>
                valuesByLocationMaterialAndReferenceDate = new HashMap<>();
        addHistoricalSales(
                valuesByLocationMaterialAndReferenceDate,
                salesProjection,
                targetUnitOfMeasure);
        if (demandPlanningProjection != null) {
            addUnconstrainedPlan(
                    valuesByLocationMaterialAndReferenceDate,
                    demandPlanningProjection,
                    targetUnitOfMeasure);
        }

        Map<Location, Map<String, String>> locationCharacteristicValuesByLocation = new HashMap<>();
        Map<Produto, Map<String, String>> materialCharacteristicValuesByMaterial = new HashMap<>();
        List<CommunityDemandSalesOverviewPeriodDTO> data =
                valuesByLocationMaterialAndReferenceDate.entrySet().stream()
                .flatMap(locationEntry -> locationEntry.getValue().entrySet().stream()
                        .flatMap(materialEntry -> materialEntry.getValue().entrySet().stream()
                                .map(referenceDateEntry -> new CommunityDemandSalesOverviewPeriodDTO(
                                        locationEntry.getKey().getId(),
                                        materialEntry.getKey().getId(),
                                        locationCharacteristicValuesByLocation.computeIfAbsent(
                                                locationEntry.getKey(),
                                                location -> getLocationCharacteristicValues(
                                                        location,
                                                        clusterAndParametersProjection)),
                                        materialCharacteristicValuesByMaterial.computeIfAbsent(
                                                materialEntry.getKey(),
                                                material -> getMaterialCharacteristicValues(
                                                        material,
                                                        clusterAndParametersProjection)),
                                        referenceDateEntry.getKey(),
                                        referenceDateEntry.getValue().historicalSales,
                                        referenceDateEntry.getValue().unconstrainedPlan))))
                .filter(period -> period.historicalSales() != 0.0d
                        || period.unconstrainedPlan() != 0.0d)
                .sorted(Comparator
                        .comparing(CommunityDemandSalesOverviewPeriodDTO::referenceDate)
                        .thenComparing(CommunityDemandSalesOverviewPeriodDTO::locationId)
                        .thenComparing(CommunityDemandSalesOverviewPeriodDTO::materialId))
                .toList();

        return new CommunityDemandSalesOverviewDTO(getPeriods(salesCalendar), data);

    }

    /**
     * Publishes the public location characteristics already indexed in the
     * active master-data snapshot, so the loaded overview can be refined
     * locally without issuing one lookup per row.
     */
    private Map<String, String> getLocationCharacteristicValues(
            Location location,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        Map<String, String> values = new LinkedHashMap<>();
        clusterAndParametersProjection.getCaracteristicaLocationMap()
                .forEach((characteristicId, characteristic) ->
                        characteristic.findValorCaracteristicaDeLocation(location)
                                .ifPresent(value -> values.put(characteristicId, value)));
        return Map.copyOf(values);

    }

    /**
     * Publishes the public material characteristics already indexed in the
     * active master-data snapshot, keeping the local filters consistent with
     * the material/location scope used for the backend read.
     */
    private Map<String, String> getMaterialCharacteristicValues(
            Produto material,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        Map<String, String> values = new LinkedHashMap<>();
        clusterAndParametersProjection.getCaracteristicaProdutoMap()
                .forEach((characteristicId, characteristic) ->
                        characteristic.findValorCaracteristicaDeProduto(material)
                                .ifPresent(value -> values.put(characteristicId, value)));
        return Map.copyOf(values);

    }

    /**
     * Resolve IDs e características públicas e mantém apenas DFUs ativos, evitando que
     * uma tela de leitura abra um produto cartesiano de combinações inativas.
     */
    private FiltroDFUProjection getActiveDfuProjection(
            CommunityDemandSalesOverviewSelectionDTO selectionDTO,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filterDTO =
                getMaterialLocationFilter(selectionDTO);
        MaterialProjection materialProjection =
                FiltroMaterialDeCombinacaoCaracteristicasMapper.getMaterialProjection(
                        filterDTO,
                        clusterAndParametersProjection,
                        true);
        LocationProjection locationProjection =
                FiltroLocationDeCombinacaoCaracteristicasMapper.getLocationProjection(
                        filterDTO,
                        clusterAndParametersProjection,
                        true);
        List<DFU> activeDfus = new ArrayList<>();
        for (Location location : locationProjection.getLocationSet()) {
            /*
             * O snapshot de dados mestres ja indexa os materiais ativos por
             * location. A interseccao com a selecao explicita evita percorrer
             * o produto cartesiano location x material apenas para descartar
             * DFUs inativas em seguida.
             */
            for (Produto activeMaterial : clusterAndParametersProjection.getMateriaisAtivosEmLocation(location)) {
                if (materialProjection.getMaterialSet().contains(activeMaterial)) {
                    activeDfus.add(new DFU(activeMaterial, location));
                }
            }
        }
        return new FiltroDFUProjection(activeDfus, clusterAndParametersProjection);

    }

    /** Adapta o record específico da tela ao contrato compartilhado do legado. */
    private FiltroMaterialLocationDeCombinacaoCaracteristicasDTO getMaterialLocationFilter(
            CommunityDemandSalesOverviewSelectionDTO selectionDTO) {

        FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filterDTO =
                new FiltroMaterialLocationDeCombinacaoCaracteristicasDTO();
        filterDTO.materialIds = selectionDTO.materialIds();
        filterDTO.locationIds = selectionDTO.locationIds();
        filterDTO.valuesByMaterialCharacteristicId =
                selectionDTO.valuesByMaterialCharacteristicId();
        filterDTO.valuesByLocationCharacteristicId =
                selectionDTO.valuesByLocationCharacteristicId();
        return filterDTO;

    }

    /** Consolida vendas usando a conversão já indexada pela Sales Projection. */
    private void addHistoricalSales(
            Map<Location, Map<Produto, Map<LocalDateTime, OverviewValues>>>
                    valuesByLocationMaterialAndReferenceDate,
            SalesProjectionLocationMaterialData salesProjection,
            UnidadeMedida targetUnitOfMeasure) {

        Map<Location, Map<Produto, Set<LocalDateTime>>>
                processedReferenceDatesByLocationAndMaterial = new HashMap<>();
        for (AggregatedByLocationMaterialUOMDate aggregatedSales : salesProjection.getSetSalesConsolidado()) {
            Location location = aggregatedSales.getLocation();
            Produto material = aggregatedSales.getMaterial();
            LocalDateTime referenceDate =
                    salesProjection.getCalendario().getUltimaDataHorarioPeriodo(
                            salesProjection.getCalendario().getPosicaoPeriodo(
                                    aggregatedSales.getReferenceDate()));
            /*
             * Uma mesma DFU/data pode possuir agregados de UOM diferentes. A
             * projection já converte e totaliza todos eles na consulta abaixo;
             * por isso cada chave temporal deve ser lida uma única vez.
             */
            if (processedReferenceDatesByLocationAndMaterial
                    .computeIfAbsent(location, ignored -> new HashMap<>())
                    .computeIfAbsent(material, ignored -> new HashSet<>())
                    .add(referenceDate)) {
                valuesByLocationMaterialAndReferenceDate
                        .computeIfAbsent(location, ignored -> new HashMap<>())
                        .computeIfAbsent(material, ignored -> new HashMap<>())
                        .computeIfAbsent(referenceDate, ignored -> new OverviewValues())
                        .historicalSales +=
                        salesProjection.getQuantidadeSales(
                                material,
                                location,
                                aggregatedSales.getReferenceDate(),
                                targetUnitOfMeasure);
            }
        }

    }

    /**
     * Consolida Direct Demand irrestrita de cada linha já materializada na
     * Demand Plan Projection, convertendo antes de qualquer soma.
     */
    private void addUnconstrainedPlan(
            Map<Location, Map<Produto, Map<LocalDateTime, OverviewValues>>>
                    valuesByLocationMaterialAndReferenceDate,
            DemandPlanningProjection demandPlanningProjection,
            UnidadeMedida targetUnitOfMeasure) {

        for (DemandPlanItem demandPlanItem : demandPlanningProjection.getTodosDemandPlanItems()) {
            /*
             * Direct Demand is a derived Community total, not a physical field
             * stored in DemandPlanItem. The projection owns its Baseline plus
             * Demand Adjustment semantics and performs the UOM conversion.
             */
            double unconstrainedPlan = demandPlanningProjection.getValorDemandPlanItem(
                    demandPlanItem,
                    Constantes.TipoDemanda.TOTAL,
                    Constantes.TipoPlano.PLANO_IRRESTRITO,
                    targetUnitOfMeasure);
            valuesByLocationMaterialAndReferenceDate
                    .computeIfAbsent(demandPlanItem.getLocation(), ignored -> new HashMap<>())
                    .computeIfAbsent(demandPlanItem.getProduto(), ignored -> new HashMap<>())
                    .computeIfAbsent(demandPlanItem.getDataReferencia(), ignored -> new OverviewValues())
                    .unconstrainedPlan +=
                    unconstrainedPlan;
        }

    }

    /**
     * Mantém o eixo do legado: com plano, usa seu bucket e horizonte; sem
     * plano, abre o histórico mensal até o instante atual.
     */
    private Calendario getSalesCalendar(Calendario demandPlanCalendar, int historicalPeriods) {

        if (demandPlanCalendar == null) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                    Constantes.TamanhoBucket.MENSAL,
                    currentDateTime.minusMonths(historicalPeriods),
                    currentDateTime);
        }

        LocalDateTime historicalStart = demandPlanCalendar.getPrimeiraDataPeriodo(
                demandPlanCalendar.getPosicaoPeriodoPresente() - historicalPeriods).atStartOfDay();
        return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                demandPlanCalendar.getTamanhoBucket(),
                historicalStart,
                demandPlanCalendar.getDataHorarioFinal());

    }

    /**
     * Converte as datas iniciais publicadas pelo seletor nos fechamentos que
     * compõem a chave física de DemandPlanItem.
     *
     * <p>Sem seleção temporal, o conjunto vazio preserva a leitura integral.
     * Com plano ausente, qualquer período recebido é inválido porque não há
     * horizonte ao qual a seleção possa pertencer.</p>
     */
    private Set<LocalDateTime> getSelectedDemandPlanPeriodEndDates(
            CommunityDemandSalesOverviewSelectionDTO selectionDTO,
            DemandPlan demandPlan,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        List<LocalDateTime> selectedReferenceDates =
                selectionDTO.demandPlanPeriodReferenceDates();
        if (selectedReferenceDates == null || selectedReferenceDates.isEmpty()) {
            return Set.of();
        }
        if (demandPlan == null) {
            throw new IllegalArgumentException(
                    "Demand Plan periods can only be selected when a Demand Plan is selected.");
        }
        if (selectedReferenceDates.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Demand Plan selected periods must not contain null reference dates.");
        }

        Calendario demandPlanHorizon = demandPlan.getCalendarioDoDemandPlanSemHistorico(
                clusterAndParametersProjection);
        Map<LocalDateTime, LocalDateTime> periodEndDateByStartDate = new HashMap<>();
        for (int period = demandPlanHorizon.getPosicaoPeriodoPresente();
             period <= demandPlanHorizon.getPosicaoPeriodoFinalFuturo();
             period++) {
            periodEndDateByStartDate.put(
                    demandPlanHorizon.getPrimeiraDataHorarioPeriodo(period),
                    demandPlanHorizon.getUltimoSegundoPeriodo(period));
        }

        Set<LocalDateTime> selectedPeriodEndDates = new HashSet<>();
        for (LocalDateTime selectedReferenceDate : selectedReferenceDates) {
            LocalDateTime periodEndDate = periodEndDateByStartDate.get(selectedReferenceDate);
            if (periodEndDate == null) {
                throw new IllegalArgumentException(
                        "Demand Plan selected period does not belong to the selected Demand Plan: "
                                + selectedReferenceDate + ".");
            }
            selectedPeriodEndDates.add(periodEndDate);
        }
        return Set.copyOf(selectedPeriodEndDates);

    }

    /** Publica todos os fechamentos mesmo quando nenhuma série possui valor. */
    private List<LocalDateTime> getPeriods(Calendario calendar) {

        List<LocalDateTime> periods = new ArrayList<>();
        for (int period = 0; period <= calendar.getPosicaoPeriodoFinalFuturo(); period++) {
            periods.add(calendar.getUltimaDataHorarioPeriodo(period));
        }
        return periods;

    }

    private int getHistoricalPeriods(Integer historicalPeriods) {

        return historicalPeriods == null ? 1 : Math.max(1, historicalPeriods);

    }

    /**
     * Em um plano Community o histórico permanece Sell-out. Sem plano, o
     * documento precisa ser explícito para preservar o contrato de abertura do
     * legado e impedir que um payload incompleto silenciosamente escolha uma
     * fonte de vendas.
     */
    private Constantes.TipoDocumentoVenda getHistoricalSalesDocumentType(
            CommunityDemandSalesOverviewSelectionDTO selectionDTO,
            DemandPlan demandPlan) {

        if (demandPlan != null) {
            return Constantes.TipoDocumentoVenda.SELLOUT;
        }
        if (selectionDTO.historicalSalesDocumentType() == null) {
            throw new IllegalArgumentException(
                    "Historical sales document type is required when Demand Plan is not selected.");
        }
        if (!DemandPlanningModelCatalog.isTipoDocumentoHistoricoCommunity(
                selectionDTO.historicalSalesDocumentType())) {
            throw new IllegalArgumentException(
                    "The selected historical sales document type requires Enterprise.");
        }
        return selectionDTO.historicalSalesDocumentType();

    }

    private void validateSelection(CommunityDemandSalesOverviewSelectionDTO selectionDTO) {

        if (selectionDTO == null) {
            throw new IllegalArgumentException("Demand Sales Overview selection is required.");
        }
        if (selectionDTO.unitOfMeasureId() == null || selectionDTO.unitOfMeasureId().isBlank()) {
            throw new IllegalArgumentException("Demand Sales Overview unit of measure id is required.");
        }

    }

    /** Acumuladores mutáveis locais, descartados ao construir o DTO imutável. */
    private static class OverviewValues {
        private double historicalSales;
        private double unconstrainedPlan;
    }
}
