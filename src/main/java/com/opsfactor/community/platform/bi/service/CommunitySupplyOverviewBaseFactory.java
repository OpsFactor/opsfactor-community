package com.opsfactor.community.platform.bi.service;

import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningMultiplasLocationsProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.platform.bi.facade.dto.CommunitySupplyOverviewBaseDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunitySupplyOverviewBaseDTO.DirectAndIndirectDemandDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunitySupplyOverviewBaseDTO.StockAndProductionDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Monta o bloco físico compartilhado pelos overviews de Supply.
 *
 * <p>É intencionalmente pura: recebe projections já carregadas pelo caso de
 * uso consumidor e não toca repositories. Assim Community e Enterprise usam
 * uma única leitura multi-location por requisição, como no legado.</p>
 */
@Component
public class CommunitySupplyOverviewBaseFactory {

    /** Cria séries físicas no grão location/material para as locations do caso de uso. */
    public CommunitySupplyOverviewBaseDTO create(
            Calendario calendar,
            Collection<Location> locations,
            MaterialProjection materialProjection,
            ClusterEParametrosProjection clusterAndParametersProjection,
            UnidadeMedida targetUnitOfMeasure,
            SupplyPlanningMultiplasLocationsProjection supplyPlanningProjection) {

        CommunitySupplyOverviewBaseDTO response = new CommunitySupplyOverviewBaseDTO();
        response.finalDateTimeByPeriod.addAll(calendar.getListaDatasHorarios());
        for (Location location : locations) {

            SupplyPlanningProjection locationProjection = supplyPlanningProjection
                    .getSupplyPlanningProjectionDeLocation(location);
            addLocationSeries(response, location, locationProjection, materialProjection,
                    clusterAndParametersProjection, targetUnitOfMeasure);
        }
        return response;

    }

    /** Agrega estoque, produção, inbound e demanda sem acessar entidades JPA. */
    private void addLocationSeries(
            CommunitySupplyOverviewBaseDTO response,
            Location location,
            SupplyPlanningProjection locationProjection,
            MaterialProjection materialProjection,
            ClusterEParametrosProjection clusterAndParametersProjection,
            UnidadeMedida targetUnitOfMeasure) {

        Calendario calendar = locationProjection.getCalendario();
        for (Produto material : materialProjection.getMateriaisAtivos()) {

            if (!clusterAndParametersProjection.isDfuAtiva(material, location)) {
                continue;
            }
            StockAndProductionDTO stockAndProduction = new StockAndProductionDTO(
                    location.getId(), Map.of("materialId", material.getId()),
                    targetUnitOfMeasure.getId(), calendar.getNumeroPeriodosFuturos());
            DirectAndIndirectDemandDTO demand = new DirectAndIndirectDemandDTO(
                    location.getId(), Map.of("materialId", material.getId()),
                    targetUnitOfMeasure.getId(), calendar.getNumeroPeriodosFuturos());
            response.stockAndProductionByLocationAndMaterialGrouping.add(stockAndProduction);
            response.directAndIndirectDemandByLocationAndMaterialGrouping.add(demand);

            for (int period = calendar.getPosicaoPeriodoPresente();
                    period <= calendar.getPosicaoPeriodoFinalFuturo(); period++) {

                addMeasures(stockAndProduction, demand, locationProjection, period, material,
                        targetUnitOfMeasure);
            }
        }

    }

    /** Mantém as duas rodadas explícitas em todas as medidas físicas. */
    private void addMeasures(
            StockAndProductionDTO stockAndProduction,
            DirectAndIndirectDemandDTO demand,
            SupplyPlanningProjection locationProjection,
            int period,
            Produto material,
            UnidadeMedida targetUnitOfMeasure) {

        stockAndProduction.unconstrainedInventory[period] += (float) locationProjection
                .getQuantidadeEstoqueProjetado(period, material, Constantes.TipoPlano.PLANO_IRRESTRITO,
                        targetUnitOfMeasure);
        stockAndProduction.constrainedInventory[period] += (float) locationProjection
                .getQuantidadeEstoqueProjetado(period, material, Constantes.TipoPlano.PLANO_RESTRITO,
                        targetUnitOfMeasure);
        stockAndProduction.unconstrainedProduction[period] += (float) locationProjection
                .getQuantidadeProductionPlan(period, material, Constantes.TipoPlano.PLANO_IRRESTRITO,
                        Constantes.FirmePlanejado.TOTAL, targetUnitOfMeasure);
        stockAndProduction.constrainedProduction[period] += (float) locationProjection
                .getQuantidadeProductionPlan(period, material, Constantes.TipoPlano.PLANO_RESTRITO,
                        Constantes.FirmePlanejado.TOTAL, targetUnitOfMeasure);
        stockAndProduction.unconstrainedInbound[period] += (float) locationProjection
                .getQuantidadeDistributionPlanInbound(Constantes.ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL,
                        period, material, Constantes.FirmePlanejado.PLANEJADO,
                        Constantes.TipoPlano.PLANO_IRRESTRITO, targetUnitOfMeasure);
        stockAndProduction.constrainedInbound[period] += (float) locationProjection
                .getQuantidadeDistributionPlanInbound(Constantes.ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL,
                        period, material, Constantes.FirmePlanejado.PLANEJADO,
                        Constantes.TipoPlano.PLANO_RESTRITO, targetUnitOfMeasure);
        demand.unconstrainedDirectDemand[period] += (float) SupplyPlanning
                .getDemandaDiretaConsideradaParaEstoqueProjetado(locationProjection, period, material,
                        Constantes.TipoPlano.PLANO_IRRESTRITO, targetUnitOfMeasure);
        demand.constrainedDirectDemand[period] += (float) SupplyPlanning
                .getDemandaDiretaConsideradaParaEstoqueProjetado(locationProjection, period, material,
                        Constantes.TipoPlano.PLANO_RESTRITO, targetUnitOfMeasure);
        demand.unconstrainedIndirectDemand[period] += (float) SupplyPlanning.getDemandaIndireta(
                locationProjection, period, material, Constantes.TipoPlano.PLANO_IRRESTRITO,
                targetUnitOfMeasure);
        demand.constrainedIndirectDemand[period] += (float) SupplyPlanning.getDemandaIndireta(
                locationProjection, period, material, Constantes.TipoPlano.PLANO_RESTRITO,
                targetUnitOfMeasure);

    }
}
