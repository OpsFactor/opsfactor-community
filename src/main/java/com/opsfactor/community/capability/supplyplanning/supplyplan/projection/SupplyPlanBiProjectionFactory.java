package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * Carrega a fotografia única do heurístico em leituras por tipo de linha.
 *
 * <p>A produção usa fetch da lista técnica e dos componentes, garantindo que o
 * índice de inputs seja construído sem lazy load por linha.</p>
 */
@Slf4j
@Component
public class SupplyPlanBiProjectionFactory {

    private final DistributionPlanItemRepository distributionPlanItemRepository;
    private final InventoryPlanLinhaRepository inventoryPlanLinhaRepository;
    private final ProductionPlanLinhaRepository productionPlanLinhaRepository;
    private final DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory;

    /**
     * Fixa o conjunto de leituras em lote que materializa a fotografia central
     * de Supply Planning usada exclusivamente pelo heurístico Community.
     *
     * <p>A injeção explícita mantém visível que a factory não recebe dados de
     * pedidos, custos, preços ou solver privados.</p>
     */
    @Autowired
    public SupplyPlanBiProjectionFactory(
            DistributionPlanItemRepository distributionPlanItemRepository,
            InventoryPlanLinhaRepository inventoryPlanLinhaRepository,
            ProductionPlanLinhaRepository productionPlanLinhaRepository,
            DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory) {

        this.distributionPlanItemRepository = distributionPlanItemRepository;
        this.inventoryPlanLinhaRepository = inventoryPlanLinhaRepository;
        this.productionPlanLinhaRepository = productionPlanLinhaRepository;
        this.demandaDiretaConsideradaProjectionFactory = demandaDiretaConsideradaProjectionFactory;

    }

    public SupplyPlanningBiProjection getSupplyPlanningBiProjectionCompleto(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection) {

        long inicioCarregamentoNanos = System.nanoTime();
        Set<Location> locations = locationProjection.getLocationsAtivas();
        Set<Produto> materiais = materialProjection.getMateriaisAtivos();
        Calendario calendario = supplyPlan.getCalendarioDoSupplyPlan(
                supplyNetworkProjection.getClusterEParametrosProjection().getParametrosGlobais());
        DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection =
                demandaDiretaConsideradaProjectionFactory.getDemandaDiretaConsideradaProjectionCompleto(supplyPlan, calendario);
        SupplyPlanningBiProjection supplyPlanningBiProjection = new SupplyPlanningBiProjection(
                supplyPlan, perfilExecucaoSupplyPlan, supplyNetworkProjection, politicaEstoquesProjection,
                materialProjection, locationProjection, demandaDiretaConsideradaProjection);

        Collection<InventoryPlanLinha> inventoryPlanLinhas =
                inventoryPlanLinhaRepository.customFindBySupplyPlanELocationsDeListaEMateriaisDeLista(supplyPlan, locations, materiais);
        inventoryPlanLinhas.forEach(supplyPlanningBiProjection::addInventoryPlanLinha);
        Collection<DistributionPlanItem> distributionPlanItems =
                distributionPlanItemRepository.customFindBySupplyPlanELocationsOrigemDestinoDeListaEMateriaisDeLista(supplyPlan, locations, materiais);
        distributionPlanItems.forEach(supplyPlanningBiProjection::addDistributionPlanItem);
        Collection<ProductionPlanLinha> productionPlanLinhas = productionPlanLinhaRepository
                .customFindByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocationInIncluindoListaTecnicaEMateriaisInput(supplyPlan, locations);
        productionPlanLinhas.stream()
                .filter(productionPlanLinha -> supplyNetworkProjection.getClusterEParametrosProjection().isDfuAtiva(
                        productionPlanLinha.getMaterialOutput(), productionPlanLinha.getLocation()))
                .forEach(supplyPlanningBiProjection::addProductionPlanLinha);

        long tempoCarregamentoMilissegundos = (System.nanoTime() - inicioCarregamentoNanos) / 1_000_000;
        log.info("Supply Planning central snapshot loaded for plan {} in {} ms: {} inventory, {} distribution, {} production and {} direct-demand lines",
                supplyPlan.getId(), tempoCarregamentoMilissegundos,
                supplyPlanningBiProjection.getTodosInventoryPlanLinhas().size(),
                supplyPlanningBiProjection.getTodosDistributionPlanItems().size(),
                supplyPlanningBiProjection.getTodosProductionPlanLinhas().size(),
                supplyPlanningBiProjection.getTodasDemandasDiretasConsideradas().size());
        return supplyPlanningBiProjection;

    }

}
