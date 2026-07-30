package com.opsfactor.community.platform.bi.service;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
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
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanBiProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningBiProjection;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewSelectionDTO;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.platform.calendar.Calendario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Materializa a fotografia física mínima do Inventory Overview.
 *
 * <p>A carga reaproveita a projection central de Supply Planning: Inventory
 * Plan, consumo de BOM e demanda direta são lidos uma vez em lote. A montagem
 * do DTO não acessa entidades ou repositories por DFU.</p>
 */
@Service
public class CommunityInventoryOverviewSnapshotFactory {

    /** Resolve o Supply Plan e seu calendário para o request físico. */
    @Autowired
    private SupplyPlanService supplyPlanService;

    /** Carrega a projection de clusters e parâmetros sem navegar entidades por DFU. */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterAndParametersProjectionFactory;

    /** Resolve a unidade solicitada para a agregação de estoque. */
    @Autowired
    private UnidadeMedidaProjectionFactory unitOfMeasureProjectionFactory;

    /** Materializa a malha usada pela projection central de Supply Planning. */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /** Carrega as políticas de estoque aplicáveis ao perfil de execução do plano. */
    @Autowired
    private PoliticaEstoquesProjectionFactory inventoryPolicyProjectionFactory;

    /** Monta a projection única indexada que contém as séries físicas do plano. */
    @Autowired
    private SupplyPlanBiProjectionFactory supplyPlanBiProjectionFactory;

    /**
     * Resolve o recorte e carrega a fotografia indexada antes da agregação.
     */
    public CommunityInventoryOverviewSnapshot createSnapshot(CommunityInventoryOverviewSelectionDTO selectionDTO) {

        if (selectionDTO == null) {
            throw new IllegalArgumentException("Inventory Overview selection is required");
        }
        if (selectionDTO.supplyPlanId() == null) {
            throw new IllegalArgumentException("supplyPlanId is required for Inventory Overview");
        }
        ClusterEParametrosProjection clusterAndParametersProjection = clusterAndParametersProjectionFactory
                .getParametrosProjectionCompletoDeCache();
        UnidadeMedida targetUnitOfMeasure = getRequiredUnitOfMeasure(
                unitOfMeasureProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache(),
                selectionDTO.unitOfMeasureId());
        SupplyPlan supplyPlan = supplyPlanService.getSupplyPlanDeId(selectionDTO.supplyPlanId());
        PerfilExecucaoSupplyPlan supplyExecutionProfile = supplyPlan.getPerfilExecucaoSupplyPlan();
        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory
                .getSupplyNetworkProjectionCompletoDeCache();
        Calendario calendar = supplyPlan.getCalendarioDoSupplyPlan(
                clusterAndParametersProjection.getParametrosGlobais());
        LocationProjection locationProjection = getLocationProjection(selectionDTO.locationIds(), clusterAndParametersProjection);
        MaterialProjection materialProjection = getMaterialProjection(selectionDTO.materialIds(), clusterAndParametersProjection);
        PoliticaEstoquesProjection inventoryPolicyProjection = inventoryPolicyProjectionFactory
                .getPoliticaEstoquesProjection(
                        calendar,
                        clusterAndParametersProjection,
                        supplyExecutionProfile);
        SupplyPlanningBiProjection supplyPlanningBiProjection = supplyPlanBiProjectionFactory
                .getSupplyPlanningBiProjectionCompleto(
                        supplyPlan,
                        supplyExecutionProfile,
                        supplyNetworkProjection,
                        inventoryPolicyProjection,
                        materialProjection,
                        locationProjection);

        return new CommunityInventoryOverviewSnapshot(
                calendar,
                targetUnitOfMeasure,
                materialProjection,
                supplyPlanningBiProjection,
                getEligibleLocations(locationProjection));

    }

    /** Valida a unidade antes da carga das linhas para não construir um snapshot sem destino de conversão. */
    private UnidadeMedida getRequiredUnitOfMeasure(
            UnidadeMedidaProjection unitOfMeasureProjection,
            String unitOfMeasureId) {

        if (unitOfMeasureId == null || unitOfMeasureId.isBlank()) {
            throw new IllegalArgumentException("unitOfMeasureId is required for Inventory Overview");
        }
        UnidadeMedida targetUnitOfMeasure = unitOfMeasureProjection.getUnidadeMedidaFromId(unitOfMeasureId);
        if (targetUnitOfMeasure == null) {
            throw new IllegalArgumentException("Unknown Inventory Overview unit of measure: " + unitOfMeasureId);
        }
        return targetUnitOfMeasure;

    }

    /** Limita locations por ids explícitos, sem transportar DTOs ou agrupamentos legados. */
    private LocationProjection getLocationProjection(
            List<String> locationIds,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        if (locationIds == null || locationIds.isEmpty()) {
            return LocationProjectionFactory.getLocationProjectionCompleto(clusterAndParametersProjection);
        }
        if (new LinkedHashSet<>(locationIds).size() != locationIds.size()) {
            throw new IllegalArgumentException("Inventory Overview locationIds must not contain duplicates");
        }
        return LocationProjectionFactory.getProjectionSetLocationIds(locationIds, clusterAndParametersProjection);

    }

    /** Valida cada id de material contra o snapshot em cache e monta o menor escopo necessário. */
    private MaterialProjection getMaterialProjection(
            List<String> materialIds,
            ClusterEParametrosProjection clusterAndParametersProjection) {

        Set<Produto> activeMaterials = clusterAndParametersProjection.getMateriaisAtivos();
        if (materialIds == null || materialIds.isEmpty()) {
            return MaterialProjectionFactory.getProjectionSetMateriais(activeMaterials, clusterAndParametersProjection);
        }
        if (new LinkedHashSet<>(materialIds).size() != materialIds.size()) {
            throw new IllegalArgumentException("Inventory Overview materialIds must not contain duplicates");
        }
        Set<String> requestedMaterialIds = new LinkedHashSet<>(materialIds);
        Set<Produto> selectedMaterials = activeMaterials.stream()
                .filter(material -> requestedMaterialIds.contains(material.getId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (selectedMaterials.size() != requestedMaterialIds.size()) {
            Set<String> selectedMaterialIds = selectedMaterials.stream()
                    .map(Produto::getId)
                    .collect(Collectors.toSet());
            requestedMaterialIds.removeAll(selectedMaterialIds);
            throw new IllegalArgumentException("Unknown or inactive Inventory Overview material ids: " + requestedMaterialIds);
        }
        return MaterialProjectionFactory.getProjectionSetMateriais(selectedMaterials, clusterAndParametersProjection);

    }

    /** Mantém somente as locations que possuem saldo projetado na semântica de Supply. */
    private Set<Location> getEligibleLocations(LocationProjection locationProjection) {

        return new LinkedHashSet<>(locationProjection.getLocationsAtivasSetComTiposLocation(
                LocationAbstract.TipoLocation.INTERNA,
                LocationAbstract.TipoLocation.PONTO_TRANSBORDO,
                LocationAbstract.TipoLocation.FORNECEDOR));

    }

    /** Contexto imutável, descartado ao fim da requisição. */
    public record CommunityInventoryOverviewSnapshot(
            Calendario calendar,
            UnidadeMedida targetUnitOfMeasure,
            MaterialProjection materialProjection,
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            Collection<Location> eligibleLocations) {
    }

}
