package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.supplyplanning.distributionplan.projection.DistributionPlanItemBiProjection;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.projection.InventoryPlanLinhaBiProjection;
import com.opsfactor.community.capability.supplyplanning.productionplan.projection.ProductionPlanLinhaBiProjection;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fotografia central, indexada e limitada a uma execução heurística Community.
 *
 * <p>Cada linha persistida é carregada em lote uma única vez. As rotinas que
 * ainda trabalham por location recebem views temporárias das mesmas instâncias,
 * sem repositories nem uma segunda tabela de snapshots.</p>
 */
@Getter
public class SupplyPlanningBiProjection {

    private final SupplyPlan supplyPlan;
    private final PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanConsiderado;
    private final SupplyNetworkProjection supplyNetworkProjection;
    private final PoliticaEstoquesProjection politicaEstoquesProjection;
    private final MaterialProjection materialProjection;
    private final LocationProjection locationProjection;
    private final ClusterEParametrosProjection clusterEParametrosProjection;
    private final UnidadeMedidaProjection conversaoUnidadeMedidaProjection;
    private final Map<Location, Calendario> calendarioPorLocation = new LinkedHashMap<>();
    private final InventoryPlanLinhaBiProjection inventoryPlanLinhaBiProjection;
    private final DistributionPlanItemBiProjection distributionPlanItemBiProjection;
    private final ProductionPlanLinhaBiProjection productionPlanLinhaBiProjection;
    private final DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection;

    public SupplyPlanningBiProjection(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanConsiderado,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection) {

        this.supplyPlan = supplyPlan;
        this.perfilExecucaoSupplyPlanConsiderado = perfilExecucaoSupplyPlanConsiderado;
        this.supplyNetworkProjection = supplyNetworkProjection;
        this.politicaEstoquesProjection = politicaEstoquesProjection;
        this.materialProjection = materialProjection;
        this.locationProjection = locationProjection;
        this.clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        this.conversaoUnidadeMedidaProjection = supplyNetworkProjection.getConversaoUnidadeMedidaProjection();
        this.demandaDiretaConsideradaProjection = demandaDiretaConsideradaProjection;
        locationProjection.getLocationsAtivas().forEach(location -> calendarioPorLocation.put(
                location, supplyPlan.getCalendarioDoSupplyPlanParaLocation(clusterEParametrosProjection, location)));

        Calendario calendarioGlobal = supplyPlan.getCalendarioDoSupplyPlan(
                clusterEParametrosProjection.getParametrosGlobais());
        this.inventoryPlanLinhaBiProjection = new InventoryPlanLinhaBiProjection();
        this.distributionPlanItemBiProjection = new DistributionPlanItemBiProjection(
                calendarioGlobal, conversaoUnidadeMedidaProjection, false);
        this.productionPlanLinhaBiProjection = new ProductionPlanLinhaBiProjection(supplyNetworkProjection);

    }

    public Calendario getCalendario(Location location) {

        Calendario calendario = calendarioPorLocation.get(location);
        if (calendario == null) {
            throw new IllegalArgumentException("Location outside SupplyPlanningBiProjection scope: " + location.getId());
        }
        return calendario;

    }

    public void addInventoryPlanLinha(InventoryPlanLinha inventoryPlanLinha) {

        inventoryPlanLinhaBiProjection.addDadoAoBI(inventoryPlanLinha);

    }

    public void addDistributionPlanItem(DistributionPlanItem distributionPlanItem) {

        distributionPlanItemBiProjection.addDadoAoBI(distributionPlanItem);

    }

    public void addProductionPlanLinha(ProductionPlanLinha productionPlanLinha) {

        productionPlanLinhaBiProjection.addDadoAoBI(productionPlanLinha);

    }

    /** Cria uma view descartável para o recorte LLC/location atual. */
    public SupplyPlanningProjection getSupplyPlanningProjection(Location location, MaterialProjection materialProjectionEscopo) {

        return new SupplyPlanningBiLocationProjection(this, location, materialProjectionEscopo);

    }

    /** Incorpora a demanda direta eventualmente criada pela view local. */
    public void sincroniza(SupplyPlanningProjection supplyPlanningProjection) {

        supplyPlanningProjection.getDemandaDiretaConsideradaProjection().getAllDemandaDiretaConsideradaLinha()
                .forEach(demandaDiretaConsideradaProjection::addDemandPlanRestritoEIrrestritoLinha);

    }

    public Set<InventoryPlanLinha> getTodosInventoryPlanLinhas() {

        return inventoryPlanLinhaBiProjection.getTodosInventoryPlanLinhas();

    }

    public Set<DistributionPlanItem> getTodosDistributionPlanItems() {

        return distributionPlanItemBiProjection.getStreamTodosDistributionPlanItems()
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    }

    public Set<ProductionPlanLinha> getTodosProductionPlanLinhas() {

        return productionPlanLinhaBiProjection.getTodosProductionPlanLinhas();

    }

    public Collection<DemandaDiretaConsideradaLinha> getTodasDemandasDiretasConsideradas() {

        return demandaDiretaConsideradaProjection.getAllDemandaDiretaConsideradaLinha();

    }

}

/** View curta entre a fotografia central e a API de rotina ainda orientada por location. */
final class SupplyPlanningBiLocationProjection extends SupplyPlanningProjection {

    private final SupplyPlanningBiProjection supplyPlanningBiProjection;

    SupplyPlanningBiLocationProjection(
            SupplyPlanningBiProjection supplyPlanningBiProjection,
            Location location,
            MaterialProjection materialProjectionEscopo) {

        super(supplyPlanningBiProjection.getSupplyPlan(), supplyPlanningBiProjection.getPerfilExecucaoSupplyPlanConsiderado(),
                supplyPlanningBiProjection.getSupplyNetworkProjection(), supplyPlanningBiProjection.getPoliticaEstoquesProjection(),
                supplyPlanningBiProjection.getCalendario(location), location, materialProjectionEscopo,
                supplyPlanningBiProjection.getLocationProjection());
        this.supplyPlanningBiProjection = supplyPlanningBiProjection;
        populaInventoryPlan(materialProjectionEscopo);
        populaDistributionPlan(location, materialProjectionEscopo);
        populaProductionPlan(location, materialProjectionEscopo);
        populaDemandaDiretaConsiderada(location, materialProjectionEscopo);

    }

    private void populaInventoryPlan(MaterialProjection materialProjectionEscopo) {

        supplyPlanningBiProjection.getInventoryPlanLinhaBiProjection()
                .getInventoryPlanLinhas(getLocation(), materialProjectionEscopo.getMateriaisAtivos())
                .forEach(super::addInventoryPlanLinha);

    }

    private void populaDistributionPlan(Location location, MaterialProjection materialProjectionEscopo) {

        Set<Produto> materiaisLocation = materialProjectionEscopo.getMateriaisAtivosEmLocation(location);
        supplyPlanningBiProjection.getDistributionPlanItemBiProjection()
                .getDistributionPlanItemsPorDestino(getSupplyPlan(), location, materiaisLocation)
                .forEach(super::addDistributionPlanItemInbound);
        supplyPlanningBiProjection.getDistributionPlanItemBiProjection()
                .getDistributionPlanItemsPorOrigem(getSupplyPlan(), location, materiaisLocation)
                .forEach(super::addDistributionPlanItemOutbound);

    }

    private void populaProductionPlan(Location location, MaterialProjection materialProjectionEscopo) {

        ProductionPlanLinhaBiProjection productionPlanLinhaBiProjection = supplyPlanningBiProjection.getProductionPlanLinhaBiProjection();
        Set<Produto> materiaisFiltradosAtivos = materialProjectionEscopo.getMateriaisAtivos();
        Set<ProductionPlanLinha> productionPlanLinhasOutput =
                productionPlanLinhaBiProjection.getProductionPlanLinhasOutput(location, materiaisFiltradosAtivos);
        productionPlanLinhasOutput.forEach(super::addProductionPlanLinhaOutput);

        Set<Produto> materiaisInputConsiderados = new LinkedHashSet<>(materiaisFiltradosAtivos);
        productionPlanLinhasOutput.stream()
                .flatMap(productionPlanLinha -> productionPlanLinha.getMateriaisInput(getSupplyNetworkProjection()).stream())
                .forEach(materiaisInputConsiderados::add);
        productionPlanLinhaBiProjection.getProductionPlanLinhasComInput(location, materiaisInputConsiderados)
                .forEach(super::addProductionPlanLinhaInput);

    }

    private void populaDemandaDiretaConsiderada(Location location, MaterialProjection materialProjectionEscopo) {

        Set<Produto> materiaisEscopo = materialProjectionEscopo.getMateriaisAtivos();
        DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjectionEscopo = new DemandaDiretaConsideradaProjection(
                getSupplyPlan(), getCalendario(), getConversaoUnidadeMedidaProjection());
        supplyPlanningBiProjection.getDemandaDiretaConsideradaProjection().getDemandaDiretaConsideradaLinha(location).stream()
                .filter(demandaDiretaConsideradaLinha -> materiaisEscopo.contains(demandaDiretaConsideradaLinha.getMaterial()))
                .forEach(demandaDiretaConsideradaProjectionEscopo::addDemandPlanRestritoEIrrestritoLinha);
        this.demandaDiretaConsideradaProjection = demandaDiretaConsideradaProjectionEscopo;

    }

    @Override
    public void addInventoryPlanLinha(InventoryPlanLinha inventoryPlanLinha) {

        supplyPlanningBiProjection.addInventoryPlanLinha(inventoryPlanLinha);
        super.addInventoryPlanLinha(inventoryPlanLinha);

    }

    @Override
    public InventoryPlanLinha getOrAddInventoryPlanLinha(int posicaoPeriodo, Produto material) {

        InventoryPlanLinha inventoryPlanLinha = super.getOrAddInventoryPlanLinha(posicaoPeriodo, material);
        supplyPlanningBiProjection.addInventoryPlanLinha(inventoryPlanLinha);
        return inventoryPlanLinha;

    }

    @Override
    public void addDistributionPlanItemInbound(DistributionPlanItem distributionPlanItem) {

        supplyPlanningBiProjection.addDistributionPlanItem(distributionPlanItem);
        super.addDistributionPlanItemInbound(distributionPlanItem);

    }

    @Override
    public void addDistributionPlanItemOutbound(DistributionPlanItem distributionPlanItem) {

        supplyPlanningBiProjection.addDistributionPlanItem(distributionPlanItem);
        super.addDistributionPlanItemOutbound(distributionPlanItem);

    }

    @Override
    public void addProductionPlanLinhaOutput(ProductionPlanLinha productionPlanLinha) {

        supplyPlanningBiProjection.addProductionPlanLinha(productionPlanLinha);
        super.addProductionPlanLinhaOutput(productionPlanLinha);

    }

    @Override
    public void addProductionPlanLinhaInput(ProductionPlanLinha productionPlanLinha) {

        supplyPlanningBiProjection.addProductionPlanLinha(productionPlanLinha);
        super.addProductionPlanLinhaInput(productionPlanLinha);

    }

}
