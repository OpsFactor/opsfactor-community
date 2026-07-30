package com.opsfactor.community.capability.supplyplanning.inventoryplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.platform.projection.inmemorybi.BIEmMemoria;
import org.javatuples.Pair;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Índice canônico das linhas de estoque da execução heurística. */
public class InventoryPlanLinhaBiProjection {

    private final BIEmMemoria<InventoryPlanLinha> biEmMemoria = new BIEmMemoria<>(InventoryPlanLinha.class);

    public InventoryPlanLinhaBiProjection() {

        biEmMemoria.addObjectAttribute("location", Location.class, InventoryPlanLinha::getLocation, true);
        biEmMemoria.addObjectAttribute("material", Produto.class, InventoryPlanLinha::getProduto, true);
        biEmMemoria.addLocalDateTimeAttribute("dataReferencia", InventoryPlanLinha::getDataReferencia, true);

    }

    /** Adiciona a linha física uma única vez; igualdade segue a chave composta. */
    public void addDadoAoBI(InventoryPlanLinha inventoryPlanLinha) {

        if (!biEmMemoria.contains(inventoryPlanLinha)) {
            biEmMemoria.addElementoNoBI(inventoryPlanLinha);
        }

    }

    public Set<InventoryPlanLinha> getInventoryPlanLinhas(Location location, Collection<Produto> materiais) {

        Set<Produto> materiaisSet = new LinkedHashSet<>(materiais);
        return biEmMemoria.getWhereEquals(Pair.with("location", location)).stream()
                .filter(inventoryPlanLinha -> materiaisSet.contains(inventoryPlanLinha.getProduto()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    }

    public Set<InventoryPlanLinha> getTodosInventoryPlanLinhas() {

        return biEmMemoria.getAllRecords().stream()
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    }

}
