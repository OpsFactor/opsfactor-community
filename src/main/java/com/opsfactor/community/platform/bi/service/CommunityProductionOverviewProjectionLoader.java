package com.opsfactor.community.platform.bi.service;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutivaFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.DemandaDiretaConsideradaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.DemandaDiretaConsideradaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningMultiplasLocationsProjection;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.platform.calendar.Calendario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Carrega as projections Community necessárias ao Production Overview.
 *
 * <p>A factory concentra a carga batch das projections antes de qualquer
 * agregação de DTO. Assim, cada leitura monta índices de Supply Plan uma única
 * vez e o service consumidor percorre apenas coleções em memória.</p>
 */
@Service
public class CommunityProductionOverviewProjectionLoader {

    /** Recupera o plano e o perfil de execução que delimitam a leitura. */
    @Autowired
    private SupplyPlanService supplyPlanService;
    /** Carrega parâmetros globais e escopos ativos em cache. */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterAndParametersProjectionFactory;
    /** Fornece conversões de unidade para a seleção quantitativa. */
    @Autowired
    private UnidadeMedidaProjectionFactory unitOfMeasureProjectionFactory;
    /** Materializa a malha de master data sem joins sob demanda. */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;
    /** Carrega as políticas de estoque aplicáveis ao perfil do plano. */
    @Autowired
    private PoliticaEstoquesProjectionFactory inventoryPolicyProjectionFactory;
    /** Calcula a disponibilidade e ocupação de capacidade por recurso. */
    @Autowired
    private BIProjectionCapacidadeProdutivaFactory productionCapacityProjectionFactory;
    /** Obtém a série de demanda direta já considerada pelo Supply Plan. */
    @Autowired
    private DemandaDiretaConsideradaProjectionFactory consideredDirectDemandProjectionFactory;
    /** Constrói a projection de planejamento que será percorrida pelo service. */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;

    /**
     * Constrói o contexto completo sem produzir resposta HTTP nem consultar linha a linha.
     *
     * <p>A seleção é obrigatória; sua ausência falha explicitamente antes de
     * qualquer projection ser carregada.</p>
     */
    public CommunityProductionOverviewProjectionContext load(
            CommunityProductionOverviewSelectionDTO selectionDTO) {

        if (selectionDTO == null) {
            throw new IllegalArgumentException("Production Overview selection is required.");
        }
        UnidadeMedidaProjection unitOfMeasureProjection = unitOfMeasureProjectionFactory
                .getUnidadeMedidaProjectionCompletoDeCache();
        UnidadeMedida targetUnitOfMeasure = getRequiredQuantityUnitOfMeasure(
                unitOfMeasureProjection, selectionDTO.uomId);
        ClusterEParametrosProjection clusterAndParametersProjection = clusterAndParametersProjectionFactory
                .getParametrosProjectionCompletoDeCache();
        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory
                .getSupplyNetworkProjectionCompletoDeCache();
        SupplyPlan supplyPlan = supplyPlanService.getSupplyPlanDeId(selectionDTO.supplyPlanId);
        PerfilExecucaoSupplyPlan supplyExecutionProfile = supplyPlan.getPerfilExecucaoSupplyPlan();
        Calendario supplyPlanCalendar = supplyPlan.getCalendarioDoSupplyPlan(
                clusterAndParametersProjection.getParametrosGlobais());
        LocationProjection locationProjection = getLocationProjection(
                selectionDTO, clusterAndParametersProjection);
        MaterialProjection materialProjection = MaterialProjectionFactory
                .getProjectionByMaterialCharacteristicValues(
                        selectionDTO.valuesByMaterialCharacteristicId,
                        clusterAndParametersProjection,
                        false);
        PoliticaEstoquesProjection inventoryPolicyProjection = inventoryPolicyProjectionFactory
                .getPoliticaEstoquesProjection(
                        supplyPlanCalendar,
                        clusterAndParametersProjection,
                        supplyExecutionProfile);
        SupplyPlanningMultiplasLocationsProjection supplyPlanningProjection = supplyPlanProjectionFactory
                .getSupplyPlanningMultiplasLocationsProjectionVazio(
                        supplyPlan,
                        supplyExecutionProfile,
                        supplyNetworkProjection,
                        inventoryPolicyProjection,
                        materialProjection,
                        locationProjection);

        supplyPlanProjectionFactory.populaSupplyPlanningMultiplasLocationsProjectionComInventoryPlan(
                supplyPlanningProjection);
        supplyPlanProjectionFactory.populaSupplyPlanningMultiplasLocationsProjectionComDistributionPlanInbound(
                supplyPlanningProjection);
        supplyPlanProjectionFactory.populaSupplyPlanningMultiplasLocationsProjectionComDistributionPlanOutbound(
                supplyPlanningProjection);
        supplyPlanProjectionFactory.populaSupplyPlanningMultiplasLocationsProjectionComProductionPlanOutput(
                supplyPlanningProjection);
        supplyPlanProjectionFactory.populaSupplyPlanningMultiplasLocationsProjectionComProductionPlanInput(
                supplyPlanningProjection);
        DemandaDiretaConsideradaProjection consideredDirectDemandProjection =
                consideredDirectDemandProjectionFactory.getDemandaDiretaConsideradaProjectionCompleto(
                        supplyPlan, supplyPlanCalendar);
        supplyPlanProjectionFactory.populaSupplyPlanningMultiplasLocationsProjectionComDemandaDiretaConsideradaProjection(
                supplyPlanningProjection, consideredDirectDemandProjection);

        return new CommunityProductionOverviewProjectionContext(
                supplyPlan,
                supplyPlanCalendar,
                supplyExecutionProfile,
                targetUnitOfMeasure,
                materialProjection,
                supplyNetworkProjection,
                productionCapacityProjectionFactory.getBIProjectionCapacidadeProdutivaDeSupplyPlan(
                        supplyPlan, supplyPlanCalendar),
                supplyPlanningProjection,
                getEligibleLocations(locationProjection));

    }

    /** A unidade de quantidade é obrigatória para evitar agregação incoerente entre materiais. */
    static UnidadeMedida getRequiredQuantityUnitOfMeasure(
            UnidadeMedidaProjection unitOfMeasureProjection,
            String unitOfMeasureId) {

        UnidadeMedida targetUnitOfMeasure = unitOfMeasureProjection.getUnidadeMedidaFromId(unitOfMeasureId);
        if (targetUnitOfMeasure == null) {
            throw new IllegalArgumentException("uomId is required for Production Overview");
        }
        return targetUnitOfMeasure;

    }

    /** Converte uma seleção vazia na projection completa, como fazia a rota histórica. */
    private LocationProjection getLocationProjection(
            CommunityProductionOverviewSelectionDTO selectionDTO,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        Set<String> locationIds = selectionDTO.locationDTOs.stream()
                .map(locationDTO -> locationDTO.id)
                .collect(Collectors.toSet());
        return locationIds.isEmpty()
                ? LocationProjectionFactory.getLocationProjectionCompleto(clusterAndParametersProjection)
                : LocationProjectionFactory.getProjectionSetLocationIds(
                        locationIds, clusterAndParametersProjection);

    }

    /** Mantém somente as locations que participam da fotografia agregada do legado. */
    private Set<Location> getEligibleLocations(LocationProjection locationProjection) {

        return new LinkedHashSet<>(locationProjection.getLocationsAtivasSetComTiposLocation(
                LocationAbstract.TipoLocation.INTERNA,
                LocationAbstract.TipoLocation.PONTO_TRANSBORDO,
                LocationAbstract.TipoLocation.FORNECEDOR));

    }

    /** Projections já carregadas, restritas à montagem da resposta da requisição atual. */
    public record CommunityProductionOverviewProjectionContext(
            SupplyPlan supplyPlan,
            Calendario calendar,
            PerfilExecucaoSupplyPlan supplyExecutionProfile,
            UnidadeMedida targetUnitOfMeasure,
            MaterialProjection materialProjection,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva productionCapacityProjection,
            SupplyPlanningMultiplasLocationsProjection supplyPlanningProjection,
            Set<Location> eligibleLocations) {
    }
}
