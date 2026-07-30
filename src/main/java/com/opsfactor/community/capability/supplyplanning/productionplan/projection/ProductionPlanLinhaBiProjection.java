package com.opsfactor.community.capability.supplyplanning.productionplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.platform.projection.inmemorybi.BIEmMemoria;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import org.javatuples.Pair;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Índice canônico de produção que não duplica a linha física por componente da BOM. */
public class ProductionPlanLinhaBiProjection {

    private final SupplyNetworkProjection supplyNetworkProjection;
    private final BIEmMemoria<ProductionPlanLinha> productionPlanLinhaBi = new BIEmMemoria<>(ProductionPlanLinha.class);
    private final BIEmMemoria<ProductionPlanLinhaInputUsage> productionInputUsageBi = new BIEmMemoria<>(ProductionPlanLinhaInputUsage.class);

    public ProductionPlanLinhaBiProjection(SupplyNetworkProjection supplyNetworkProjection) {

        this.supplyNetworkProjection = supplyNetworkProjection;
        productionPlanLinhaBi.addObjectAttribute("location", Location.class, ProductionPlanLinha::getLocation, true);
        productionPlanLinhaBi.addObjectAttribute("materialOutput", Produto.class, ProductionPlanLinha::getMaterialOutput, true);
        productionPlanLinhaBi.addLocalDateTimeAttribute("dataReferencia", ProductionPlanLinha::getDataReferencia, true);
        productionInputUsageBi.addObjectAttribute("location", Location.class,
                productionPlanLinhaInputUsage -> productionPlanLinhaInputUsage.productionPlanLinha().getLocation(), true);
        productionInputUsageBi.addObjectAttribute("materialInput", Produto.class, ProductionPlanLinhaInputUsage::materialInput, true);

    }

    public void addDadoAoBI(ProductionPlanLinha productionPlanLinha) {

        if (productionPlanLinhaBi.contains(productionPlanLinha)) {
            return;
        }
        productionPlanLinhaBi.addElementoNoBI(productionPlanLinha);
        productionPlanLinha.getMateriaisInput(supplyNetworkProjection).forEach(materialInput ->
                productionInputUsageBi.addElementoNoBI(new ProductionPlanLinhaInputUsage(productionPlanLinha, materialInput)));

    }

    public Set<ProductionPlanLinha> getProductionPlanLinhasOutput(Location location, Collection<Produto> materiaisOutput) {

        Set<Produto> materiaisOutputSet = new LinkedHashSet<>(materiaisOutput);
        return productionPlanLinhaBi.getWhereEquals(Pair.with("location", location)).stream()
                .filter(productionPlanLinha -> materiaisOutputSet.contains(productionPlanLinha.getMaterialOutput()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    }

    public Set<ProductionPlanLinha> getProductionPlanLinhasComInput(Location location, Collection<Produto> materiaisInput) {

        Set<ProductionPlanLinha> productionPlanLinhas = new LinkedHashSet<>();
        for (Produto materialInput : new LinkedHashSet<>(materiaisInput)) {
            productionInputUsageBi.getWhereEquals(Pair.with("location", location), Pair.with("materialInput", materialInput)).stream()
                    .map(ProductionPlanLinhaInputUsage::productionPlanLinha)
                    .forEach(productionPlanLinhas::add);
        }
        return productionPlanLinhas;

    }

    public Set<ProductionPlanLinha> getTodosProductionPlanLinhas() {

        return productionPlanLinhaBi.getAllRecords().stream()
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    }

    private record ProductionPlanLinhaInputUsage(ProductionPlanLinha productionPlanLinha, Produto materialInput) {
    }

}
