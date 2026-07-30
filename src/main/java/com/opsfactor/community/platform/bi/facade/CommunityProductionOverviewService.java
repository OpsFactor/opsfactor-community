package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.platform.bi.service.CommunityProductionOverviewSnapshotFactory;
import com.opsfactor.community.platform.bi.service.CommunityProductionOverviewSnapshotFactory.CommunityProductionOverviewSnapshot;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO.DirectAndIndirectDemandDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO.ProductionResourceCapacityDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO.ProductionResourceOccupationDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO.StockAndProductionDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Lê o Production Overview Community a partir de uma fotografia de projections.
 *
 * <p>Não consulta entidades enquanto agrega: todas as séries são resolvidas
 * pelos índices do snapshot construído uma única vez pela factory parceira.</p>
 */
@Service
public class CommunityProductionOverviewService {

    /** Materializa a fotografia indexada que abastece todas as séries do overview. */
    @Autowired
    private CommunityProductionOverviewSnapshotFactory productionOverviewSnapshotFactory;

    /** Publica as séries irrestrita e restrita sem inventar uma ocupação efetiva única. */
    public CommunityProductionOverviewDTO getProductionOverview(
            CommunityProductionOverviewSelectionDTO selectionDTO) {

        CommunityProductionOverviewSnapshot snapshot = productionOverviewSnapshotFactory
                .createSnapshot(selectionDTO);
        CommunityProductionOverviewDTO response = new CommunityProductionOverviewDTO();
        response.finalDateTimeByPeriod.addAll(snapshot.calendar().getListaDatasHorarios());

        for (Location location : snapshot.eligibleLocations()) {
            SupplyPlanningProjection locationProjection = snapshot.supplyPlanningProjection()
                    .getSupplyPlanningProjectionDeLocation(location);
            Calendario calendar = locationProjection.getCalendario();
            addBaseSeries(response, snapshot, location, locationProjection, calendar);
            addProductionResourceSeries(response, snapshot, location, locationProjection, calendar);
        }
        return response;

    }

    /** Agrega o bloco comum de estoque, produção, inbound e demanda por agrupamento. */
    private void addBaseSeries(
            CommunityProductionOverviewDTO response,
            CommunityProductionOverviewSnapshot snapshot,
            Location location,
            SupplyPlanningProjection locationProjection,
            Calendario calendar) {

        ClusterEParametrosProjection clusterAndParametersProjection = snapshot.supplyNetworkProjection()
                .getClusterEParametrosProjection();
        for (Produto material : snapshot.materialProjection().getMateriaisAtivos()) {
            Map<String, String> materialCharacteristicValues = Map.of("materialId", material.getId());
            StockAndProductionDTO stockAndProduction = new StockAndProductionDTO(
                    location.getId(),
                    materialCharacteristicValues,
                    snapshot.targetUnitOfMeasure().getId(),
                    calendar.getNumeroPeriodosFuturos());
            DirectAndIndirectDemandDTO demand = new DirectAndIndirectDemandDTO(
                    location.getId(),
                    materialCharacteristicValues,
                    snapshot.targetUnitOfMeasure().getId(),
                    calendar.getNumeroPeriodosFuturos());
            response.stockAndProductionByLocationAndMaterialGrouping.add(stockAndProduction);
            response.directAndIndirectDemandByLocationAndMaterialGrouping.add(demand);

            for (int period = calendar.getPosicaoPeriodoPresente();
                 period <= calendar.getPosicaoPeriodoFinalFuturo();
                 period++) {
                if (!clusterAndParametersProjection.isDfuAtiva(material, location)) {
                    continue;
                }
                addDemandMeasures(demand, locationProjection, period, material, snapshot);
                addStockAndSupplyMeasures(stockAndProduction, locationProjection, period, material, snapshot);
            }
        }
    }

    /** Soma demanda direta e indireta nas duas séries de plano. */
    private void addDemandMeasures(
            DirectAndIndirectDemandDTO demand,
            SupplyPlanningProjection locationProjection,
            int period,
            Produto material,
            CommunityProductionOverviewSnapshot snapshot) {

        demand.unconstrainedDirectDemand[period] += (float) SupplyPlanning
                .getDemandaDiretaConsideradaParaEstoqueProjetado(
                        locationProjection,
                        period,
                        material,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        snapshot.targetUnitOfMeasure());
        demand.constrainedDirectDemand[period] += (float) SupplyPlanning
                .getDemandaDiretaConsideradaParaEstoqueProjetado(
                        locationProjection,
                        period,
                        material,
                        Constantes.TipoPlano.PLANO_RESTRITO,
                        snapshot.targetUnitOfMeasure());
        demand.unconstrainedIndirectDemand[period] += (float) SupplyPlanning.getDemandaIndireta(
                locationProjection,
                period,
                material,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                snapshot.targetUnitOfMeasure());
        demand.constrainedIndirectDemand[period] += (float) SupplyPlanning.getDemandaIndireta(
                locationProjection,
                period,
                material,
                Constantes.TipoPlano.PLANO_RESTRITO,
                snapshot.targetUnitOfMeasure());

    }

    /** Soma somente as medidas físicas que fazem parte do contrato histórico do overview. */
    private void addStockAndSupplyMeasures(
            StockAndProductionDTO stockAndProduction,
            SupplyPlanningProjection locationProjection,
            int period,
            Produto material,
            CommunityProductionOverviewSnapshot snapshot) {

        stockAndProduction.unconstrainedInventory[period] += (float) locationProjection
                .getQuantidadeEstoqueProjetado(
                        period,
                        material,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        snapshot.targetUnitOfMeasure());
        stockAndProduction.constrainedInventory[period] += (float) locationProjection
                .getQuantidadeEstoqueProjetado(
                        period,
                        material,
                        Constantes.TipoPlano.PLANO_RESTRITO,
                        snapshot.targetUnitOfMeasure());
        stockAndProduction.unconstrainedProduction[period] += (float) locationProjection
                .getQuantidadeProductionPlan(
                        period,
                        material,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        Constantes.FirmePlanejado.TOTAL,
                        snapshot.targetUnitOfMeasure());
        stockAndProduction.constrainedProduction[period] += (float) locationProjection
                .getQuantidadeProductionPlan(
                        period,
                        material,
                        Constantes.TipoPlano.PLANO_RESTRITO,
                        Constantes.FirmePlanejado.TOTAL,
                        snapshot.targetUnitOfMeasure());
        stockAndProduction.unconstrainedInbound[period] += (float) locationProjection
                .getQuantidadeDistributionPlanInbound(
                        Constantes.ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL,
                        period,
                        material,
                        Constantes.FirmePlanejado.PLANEJADO,
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        snapshot.targetUnitOfMeasure());
        stockAndProduction.constrainedInbound[period] += (float) locationProjection
                .getQuantidadeDistributionPlanInbound(
                        Constantes.ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL,
                        period,
                        material,
                        Constantes.FirmePlanejado.PLANEJADO,
                        Constantes.TipoPlano.PLANO_RESTRITO,
                        snapshot.targetUnitOfMeasure());

    }

    /** Agrega capacidade registrada e ocupação/volume por recurso produtivo. */
    private void addProductionResourceSeries(
            CommunityProductionOverviewDTO response,
            CommunityProductionOverviewSnapshot snapshot,
            Location location,
            SupplyPlanningProjection locationProjection,
            Calendario calendar) {

        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productionCapacityType = snapshot
                .supplyExecutionProfile().getTipoCapacidadeProdutiva();
        for (RecursoProdutivo productionResource : snapshot.supplyNetworkProjection()
                .getRecursoProdutivoAtivoSet(location)) {
            ProductionResourceCapacityDTO capacity = new ProductionResourceCapacityDTO(
                    location.getId(), productionResource.getId(), calendar.getNumeroPeriodosFuturos());
            response.capacityByProductionResource.add(capacity);
            for (int period = calendar.getPosicaoPeriodoPresente();
                 period <= calendar.getPosicaoPeriodoFinalFuturo();
                 period++) {
                capacity.capacityInHoursOrQuantity[period] = (float) snapshot.productionCapacityProjection()
                        .getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                                period,
                                productionResource,
                                BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA);
            }

            for (Produto material : snapshot.materialProjection().getMateriaisAtivos()) {
                ProductionResourceOccupationDTO occupation = new ProductionResourceOccupationDTO(
                        location.getId(),
                        productionResource.getId(),
                        Map.of("materialId", material.getId()),
                        snapshot.targetUnitOfMeasure().getId(),
                        calendar.getNumeroPeriodosFuturos());
                response.occupationAndProductionByProductionResourceAndMaterialGrouping.add(occupation);
                addOccupationMeasures(
                        occupation,
                        snapshot,
                        location,
                        locationProjection,
                        productionResource,
                        material,
                        calendar,
                        productionCapacityType);
            }
        }
    }

    /** Percorre índices já materializados e mantém irrestito/restrito explicitamente separados. */
    private void addOccupationMeasures(
            ProductionResourceOccupationDTO occupation,
            CommunityProductionOverviewSnapshot snapshot,
            Location location,
            SupplyPlanningProjection locationProjection,
            RecursoProdutivo productionResource,
            Produto material,
            Calendario calendar,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productionCapacityType) {

        ClusterEParametrosProjection clusterAndParametersProjection = snapshot.supplyNetworkProjection()
                .getClusterEParametrosProjection();
        for (int period = calendar.getPosicaoPeriodoPresente();
             period <= calendar.getPosicaoPeriodoFinalFuturo();
             period++) {
            if (!clusterAndParametersProjection.isDfuAtiva(material, location)) {
                continue;
            }
            occupation.unconstrainedProductionQuantity[period] += (float) locationProjection
                    .getQuantidadeProductionPlan(
                            period,
                            material,
                            productionResource,
                            Constantes.TipoPlano.PLANO_IRRESTRITO,
                            Constantes.FirmePlanejado.TOTAL,
                            snapshot.targetUnitOfMeasure());
            occupation.constrainedProductionQuantity[period] += (float) locationProjection
                    .getQuantidadeProductionPlan(
                            period,
                            material,
                            productionResource,
                            Constantes.TipoPlano.PLANO_RESTRITO,
                            Constantes.FirmePlanejado.TOTAL,
                            snapshot.targetUnitOfMeasure());
            occupation.unconstrainedOccupationInHoursOrQuantity[period] += (float) locationProjection
                    .getConsumoCapacidadeEmQuantidadeOuHorasProductionPlan(
                            period,
                            productionResource,
                            material,
                            Constantes.TipoPlano.PLANO_IRRESTRITO,
                            Constantes.FirmePlanejado.TOTAL,
                            productionCapacityType);
            occupation.constrainedOccupationInHoursOrQuantity[period] += (float) locationProjection
                    .getConsumoCapacidadeEmQuantidadeOuHorasProductionPlan(
                            period,
                            productionResource,
                            material,
                            Constantes.TipoPlano.PLANO_RESTRITO,
                            Constantes.FirmePlanejado.TOTAL,
                            productionCapacityType);
        }
    }
}
