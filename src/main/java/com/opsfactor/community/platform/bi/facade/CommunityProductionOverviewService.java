package com.opsfactor.community.platform.bi.facade;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.platform.bi.service.CommunityProductionOverviewProjectionLoader;
import com.opsfactor.community.platform.bi.service.CommunityProductionOverviewProjectionLoader.CommunityProductionOverviewProjectionContext;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO.ProductionResourceCapacityDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO.ProductionResourceOccupationDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunitySupplyOverviewBaseDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.bi.service.CommunitySupplyOverviewBaseFactory;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Lê o Production Overview Community a partir de projections carregadas em lote.
 *
 * <p>Não consulta entidades enquanto agrega: todas as séries são resolvidas
 * pelos índices preparados uma única vez pelo loader parceiro.</p>
 */
@Service
public class CommunityProductionOverviewService {

    /** Materializa a fotografia indexada que abastece todas as séries do overview. */
    @Autowired
    private CommunityProductionOverviewProjectionLoader productionOverviewProjectionLoader;

    /** Constrói o bloco físico que também pode ser reutilizado pelo overlay Enterprise. */
    @Autowired
    private CommunitySupplyOverviewBaseFactory supplyOverviewBaseFactory;

    /** Publica as séries irrestrita e restrita sem inventar uma ocupação efetiva única. */
    public CommunityProductionOverviewDTO getProductionOverview(
            CommunityProductionOverviewSelectionDTO selectionDTO) {

        CommunityProductionOverviewProjectionContext projectionContext = productionOverviewProjectionLoader
                .load(selectionDTO);
        CommunitySupplyOverviewBaseDTO base = supplyOverviewBaseFactory.create(
                projectionContext.calendar(), projectionContext.eligibleLocations(), projectionContext.materialProjection(),
                projectionContext.supplyNetworkProjection().getClusterEParametrosProjection(),
                projectionContext.targetUnitOfMeasure(), projectionContext.supplyPlanningProjection());
        CommunityProductionOverviewDTO response = new CommunityProductionOverviewDTO(base);

        for (Location location : projectionContext.eligibleLocations()) {
            SupplyPlanningProjection locationProjection = projectionContext.supplyPlanningProjection()
                    .getSupplyPlanningProjectionDeLocation(location);
            Calendario calendar = locationProjection.getCalendario();
            addProductionResourceSeries(response, projectionContext, location, locationProjection, calendar);
        }
        return response;

    }

    /** Agrega capacidade registrada e ocupação/volume por recurso produtivo. */
    private void addProductionResourceSeries(
            CommunityProductionOverviewDTO response,
            CommunityProductionOverviewProjectionContext projectionContext,
            Location location,
            SupplyPlanningProjection locationProjection,
            Calendario calendar) {

        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productionCapacityType = projectionContext
                .supplyExecutionProfile().getTipoCapacidadeProdutiva();
        for (RecursoProdutivo productionResource : projectionContext.supplyNetworkProjection()
                .getRecursoProdutivoAtivoSet(location)) {
            ProductionResourceCapacityDTO capacity = new ProductionResourceCapacityDTO(
                    location.getId(), productionResource.getId(), calendar.getNumeroPeriodosFuturos());
            response.capacityByProductionResource.add(capacity);
            for (int period = calendar.getPosicaoPeriodoPresente();
                 period <= calendar.getPosicaoPeriodoFinalFuturo();
                 period++) {
                capacity.capacityInHoursOrQuantity[period] = (float) projectionContext.productionCapacityProjection()
                        .getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                                period,
                                productionResource,
                                BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA);
            }

            for (Produto material : projectionContext.materialProjection().getMateriaisAtivos()) {
                ProductionResourceOccupationDTO occupation = new ProductionResourceOccupationDTO(
                        location.getId(),
                        productionResource.getId(),
                        Map.of("materialId", material.getId()),
                        projectionContext.targetUnitOfMeasure().getId(),
                        calendar.getNumeroPeriodosFuturos());
                response.occupationAndProductionByProductionResourceAndMaterialGrouping.add(occupation);
                addOccupationMeasures(
                        occupation,
                        projectionContext,
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
            CommunityProductionOverviewProjectionContext projectionContext,
            Location location,
            SupplyPlanningProjection locationProjection,
            RecursoProdutivo productionResource,
            Produto material,
            Calendario calendar,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva productionCapacityType) {

        ClusterEParametrosProjection clusterAndParametersProjection = projectionContext.supplyNetworkProjection()
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
                            projectionContext.targetUnitOfMeasure());
            occupation.constrainedProductionQuantity[period] += (float) locationProjection
                    .getQuantidadeProductionPlan(
                            period,
                            material,
                            productionResource,
                            Constantes.TipoPlano.PLANO_RESTRITO,
                            Constantes.FirmePlanejado.TOTAL,
                            projectionContext.targetUnitOfMeasure());
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
