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
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanProjectionFactory;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Visão Community somente leitura para comparar sell-out histórico e Demand
 * Plan irrestrito no nível material/location.
 *
 * <p>O service materializa uma única Sales Projection e uma única Demand Plan
 * Projection por request. Preço, valores monetários, Custom Key Figures,
 * SpEL, características e agregação são capacidades fora deste recorte e não
 * são carregados como dependências ocultas.</p>
 */
@Service
public class CommunityDemandSalesOverviewService {

    /** Key Figure quantitativa que representa o plano irrestrito publicado. */
    private static final KeyFigureStandard DIRECT_DEMAND_KEY_FIGURE = new KeyFigureStandard(
            KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP);

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

        DemandPlan demandPlan = demandPlanningService.getDemandPlanDeId(selectionDTO.demandPlanId());
        UnidadeMedida targetUnitOfMeasure = unidadeMedidaService.getUnidadeMedida(
                selectionDTO.unitOfMeasureId());
        ClusterEParametrosProjection clusterAndParametersProjection =
                clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        UnidadeMedidaProjection unitOfMeasureProjection =
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache();

        FiltroDFUProjection dfuProjection = getActiveDfuProjection(
                selectionDTO,
                clusterAndParametersProjection);
        DemandPlanningProjection demandPlanningProjection =
                demandPlanProjectionFactory.getDemandPlanningProjectionCompleto(
                        demandPlan,
                        dfuProjection,
                        false);
        Calendario demandPlanCalendar = demandPlanningProjection.getCalendario();
        Calendario salesCalendar = getSalesCalendar(
                demandPlanCalendar,
                getHistoricalPeriods(selectionDTO.historicalPeriods()));

        SalesProjectionLocationMaterialData salesProjection =
                salesProjectionFactory.getSalesProjectionLocationMaterialData(
                        Constantes.TipoDocumentoVenda.SELLOUT,
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
        addUnconstrainedPlan(
                valuesByLocationMaterialAndReferenceDate,
                demandPlanningProjection,
                targetUnitOfMeasure,
                unitOfMeasureProjection);

        List<CommunityDemandSalesOverviewPeriodDTO> data =
                valuesByLocationMaterialAndReferenceDate.entrySet().stream()
                .flatMap(locationEntry -> locationEntry.getValue().entrySet().stream()
                        .flatMap(materialEntry -> materialEntry.getValue().entrySet().stream()
                                .map(referenceDateEntry -> new CommunityDemandSalesOverviewPeriodDTO(
                                        locationEntry.getKey().getId(),
                                        materialEntry.getKey().getId(),
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
     * Resolve somente IDs explícitos e mantém apenas DFUs ativos, evitando que
     * uma tela de leitura abra um produto cartesiano de combinações inativas.
     */
    private FiltroDFUProjection getActiveDfuProjection(
            CommunityDemandSalesOverviewSelectionDTO selectionDTO,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        Set<Location> locations = resolveLocations(
                selectionDTO.locationIds(),
                clusterAndParametersProjection);
        Set<Produto> materials = resolveMaterials(
                selectionDTO.materialIds(),
                clusterAndParametersProjection);
        List<DFU> activeDfus = new ArrayList<>();
        for (Location location : locations) {
            /*
             * O snapshot de dados mestres ja indexa os materiais ativos por
             * location. A interseccao com a selecao explicita evita percorrer
             * o produto cartesiano location x material apenas para descartar
             * DFUs inativas em seguida.
             */
            for (Produto activeMaterial : clusterAndParametersProjection.getMateriaisAtivosEmLocation(location)) {
                if (materials.contains(activeMaterial)) {
                    activeDfus.add(new DFU(activeMaterial, location));
                }
            }
        }
        return new FiltroDFUProjection(activeDfus, clusterAndParametersProjection);

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
            UnidadeMedida targetUnitOfMeasure,
            UnidadeMedidaProjection unitOfMeasureProjection) {

        for (DemandPlanItem demandPlanItem : demandPlanningProjection.getTodosDemandPlanItems()) {
            double unconstrainedPlan = demandPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                    DIRECT_DEMAND_KEY_FIGURE,
                    targetUnitOfMeasure,
                    unitOfMeasureProjection);
            valuesByLocationMaterialAndReferenceDate
                    .computeIfAbsent(demandPlanItem.getLocation(), ignored -> new HashMap<>())
                    .computeIfAbsent(demandPlanItem.getProduto(), ignored -> new HashMap<>())
                    .computeIfAbsent(demandPlanItem.getDataReferencia(), ignored -> new OverviewValues())
                    .unconstrainedPlan +=
                    unconstrainedPlan;
        }

    }

    /** Mantém o eixo do legado: histórico solicitado até o fim do plano ou hoje. */
    private Calendario getSalesCalendar(Calendario demandPlanCalendar, int historicalPeriods) {

        LocalDateTime historicalStart = demandPlanCalendar.getPrimeiraDataPeriodo(
                demandPlanCalendar.getPosicaoPeriodoPresente() - historicalPeriods).atStartOfDay();
        return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                demandPlanCalendar.getTamanhoBucket(),
                historicalStart,
                Calendario.getMaxDataHorario(
                        demandPlanCalendar.getDataHorarioFinal(),
                        LocalDateTime.now()));

    }

    /** Publica todos os fechamentos mesmo quando nenhuma série possui valor. */
    private List<LocalDateTime> getPeriods(Calendario calendar) {

        List<LocalDateTime> periods = new ArrayList<>();
        for (int period = 0; period <= calendar.getPosicaoPeriodoFinalFuturo(); period++) {
            periods.add(calendar.getUltimaDataHorarioPeriodo(period));
        }
        return periods;

    }

    private Set<Location> resolveLocations(
            Collection<String> locationIds,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        if (locationIds == null || locationIds.isEmpty()) {
            return new HashSet<>(clusterAndParametersProjection.getLocationsAtivas());
        }
        Set<Location> locations = new HashSet<>();
        Set<String> uniqueIds = new HashSet<>();
        for (String locationId : locationIds) {
            if (locationId == null || locationId.isBlank()) {
                throw new IllegalArgumentException("Demand Sales Overview location filter id is required.");
            }
            if (!uniqueIds.add(locationId)) {
                throw new IllegalArgumentException(
                        "Demand Sales Overview location filter contains duplicated id " + locationId + ".");
            }
            Location location = clusterAndParametersProjection.getLocationPersistida(locationId);
            if (location == null) {
                throw new IllegalArgumentException(
                        "Demand Sales Overview location filter was not found in master data: " + locationId + ".");
            }
            locations.add(location);
        }
        return locations;

    }

    private Set<Produto> resolveMaterials(
            Collection<String> materialIds,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        if (materialIds == null || materialIds.isEmpty()) {
            Set<Produto> materials = new HashSet<>();
            for (Location location : clusterAndParametersProjection.getLocationsAtivas()) {
                materials.addAll(clusterAndParametersProjection.getMateriaisAtivosEmLocation(location));
            }
            return materials;
        }
        Set<Produto> materials = new HashSet<>();
        Set<String> uniqueIds = new HashSet<>();
        for (String materialId : materialIds) {
            if (materialId == null || materialId.isBlank()) {
                throw new IllegalArgumentException("Demand Sales Overview material filter id is required.");
            }
            if (!uniqueIds.add(materialId)) {
                throw new IllegalArgumentException(
                        "Demand Sales Overview material filter contains duplicated id " + materialId + ".");
            }
            Produto material = clusterAndParametersProjection.getMaterialPersistido(materialId);
            if (material == null) {
                throw new IllegalArgumentException(
                        "Demand Sales Overview material filter was not found in master data: " + materialId + ".");
            }
            materials.add(material);
        }
        return materials;

    }

    private int getHistoricalPeriods(Integer historicalPeriods) {

        return historicalPeriods == null ? 1 : Math.max(1, historicalPeriods);

    }

    private void validateSelection(CommunityDemandSalesOverviewSelectionDTO selectionDTO) {

        if (selectionDTO == null) {
            throw new IllegalArgumentException("Demand Sales Overview selection is required.");
        }
        if (selectionDTO.demandPlanId() == null) {
            throw new IllegalArgumentException("Demand Sales Overview Demand Plan id is required.");
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
