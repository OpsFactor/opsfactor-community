package com.opsfactor.community.platform.bi.service;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.FiltroMaterialLocationDeCombinacaoCaracteristicasDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.mapper.FiltroLocationDeCombinacaoCaracteristicasMapper;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.mapper.FiltroMaterialDeCombinacaoCaracteristicasMapper;
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
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Carrega as projections físicas mínimas do Inventory Overview.
 *
 * <p>A carga reaproveita a projection central de Supply Planning: Inventory
 * Plan, consumo de BOM e demanda direta são lidos uma vez em lote. A montagem
 * do DTO não acessa entidades ou repositories por DFU.</p>
 */
@Service
public class CommunityInventoryOverviewProjectionLoader {

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
    public CommunityInventoryOverviewProjectionContext load(CommunityInventoryOverviewSelectionDTO selectionDTO) {

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
        FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filterDTO =
                getMaterialLocationFilter(selectionDTO);
        LocationProjection locationProjection =
                FiltroLocationDeCombinacaoCaracteristicasMapper.getLocationProjection(
                        filterDTO,
                        clusterAndParametersProjection,
                        true);
        MaterialProjection materialProjection =
                FiltroMaterialDeCombinacaoCaracteristicasMapper.getMaterialProjection(
                        filterDTO,
                        clusterAndParametersProjection,
                        true);
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

        return new CommunityInventoryOverviewProjectionContext(
                calendar,
                targetUnitOfMeasure,
                materialProjection,
                supplyPlanningBiProjection,
                getEligibleLocations(locationProjection),
                clusterAndParametersProjection.getCaracteristicaProdutoMap(),
                clusterAndParametersProjection.getCaracteristicaLocationMap());

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

    /** Mantém somente as locations que possuem saldo projetado na semântica de Supply. */
    private Set<Location> getEligibleLocations(LocationProjection locationProjection) {

        return new LinkedHashSet<>(locationProjection.getLocationsAtivasSetComTiposLocation(
                LocationAbstract.TipoLocation.INTERNA,
                LocationAbstract.TipoLocation.PONTO_TRANSBORDO,
                LocationAbstract.TipoLocation.FORNECEDOR));

    }

    /** Adapta o record específico da tela ao contrato compartilhado do legado. */
    private FiltroMaterialLocationDeCombinacaoCaracteristicasDTO getMaterialLocationFilter(
            CommunityInventoryOverviewSelectionDTO selectionDTO) {

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

    /** Projections imutáveis, descartadas ao fim da requisição. */
    public record CommunityInventoryOverviewProjectionContext(
            Calendario calendar,
            UnidadeMedida targetUnitOfMeasure,
            MaterialProjection materialProjection,
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            Collection<Location> eligibleLocations,
            Map<String, CaracteristicaProduto> materialCharacteristics,
            Map<String, CaracteristicaLocation> locationCharacteristics) {
    }

}
