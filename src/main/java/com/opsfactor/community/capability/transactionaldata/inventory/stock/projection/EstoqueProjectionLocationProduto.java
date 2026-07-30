package com.opsfactor.community.capability.transactionaldata.inventory.stock.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.pivovarit.function.ThrowingFunction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Projection material/location para consolidacao rapida de estoque inicial.
 *
 * <p>No Community, estoque transacional e snapshot simples por material,
 * location, quantidade e UOM. Lote, validade, aging/writeoff e producao em
 * batch seguem Enterprise.</p>
 */
@SuperBuilder
@Getter
public class EstoqueProjectionLocationProduto extends EstoqueProjectionAbstract {
        
    /**
     * Indice material -> location -> agregados de estoque. Esta projection
     * representa um snapshot sem dimensao de data.
     */
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private Map<Produto,Map<Location,Set<AggregatedByLocationMaterialUOM>>> mapaEstoquePorMaterialLocationUnidadeMedida = new HashMap<>();
    
    public void addEstoque(AggregatedByLocationMaterialUOM aggregatedByLocationMaterialUOM) {

        validaEstoqueAgregadoObrigatorio(
                aggregatedByLocationMaterialUOM,
                "location/material stock projection");
        validaEstoqueAgregadoMaterialObrigatorio(
                aggregatedByLocationMaterialUOM.getMaterial(),
                "location/material stock projection");
        validaEstoqueAgregadoLocationObrigatoria(
                aggregatedByLocationMaterialUOM.getLocation(),
                "location/material stock projection");

        mapaEstoquePorMaterialLocationUnidadeMedida
                .computeIfAbsent(aggregatedByLocationMaterialUOM.getMaterial(), x -> new HashMap<>())
                .computeIfAbsent(aggregatedByLocationMaterialUOM.getLocation(), x -> new HashSet<>())
                .add(aggregatedByLocationMaterialUOM);

    }
    
    public Set<AggregatedByLocationMaterialUOM> getEstoques(Produto produto) {
        
        if (!mapaEstoquePorMaterialLocationUnidadeMedida.containsKey(produto)) return new HashSet<>();
        
        return mapaEstoquePorMaterialLocationUnidadeMedida
                .get(produto)
                .values().stream()
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
        
    }
    
    public Set<AggregatedByLocationMaterialUOM> getEstoques() {
        
        return mapaEstoquePorMaterialLocationUnidadeMedida.values().stream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
        
    }
    
    public Set<AggregatedByLocationMaterialUOM> getEstoques(Location location, Produto produto) {
        if (!mapaEstoquePorMaterialLocationUnidadeMedida.containsKey(produto) ||
                !mapaEstoquePorMaterialLocationUnidadeMedida.get(produto).containsKey(location)) {
            return new HashSet<>();
        } else {
            return mapaEstoquePorMaterialLocationUnidadeMedida.get(produto).get(location);
        }
    }
    
    /**
     * Extrai a quantidade de estoque na unidade target especificada
     * @param produto
     * @param unidadeMedida
     * @return 
     */
    public Float getQuantidadeEstoque(Produto produto, UnidadeMedida unidadeMedida) {
        return (float) getEstoques(produto).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(produto, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }

    /**
     * Extrai a quantidade de estoque na unidade target especificada.
     * @return
     */
    public Float getQuantidadeEstoque(Location location, Produto produto, UnidadeMedida unidadeMedida) {
        return (float) getEstoques(location, produto).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(produto, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }
    
    public Set<Produto> getMateriaisComEstoque() {
        return mapaEstoquePorMaterialLocationUnidadeMedida.keySet();
    }
    public Set<Location> getLocationsComEstoque() {
        return mapaEstoquePorMaterialLocationUnidadeMedida.entrySet().stream()
                .flatMap(x -> x.getValue().keySet().stream())
                .collect(Collectors.toSet());
    }
    public Set<Location> getLocationsAtivasComEstoque() {
        return mapaEstoquePorMaterialLocationUnidadeMedida.entrySet().stream()
                .flatMap(x -> x.getValue().keySet().stream())
                .filter(x -> x.getAtivo())
                .collect(Collectors.toSet());
    }
    
    public Set<Produto> getMateriaisComEstoqueNaLocation(Location location) {
        return mapaEstoquePorMaterialLocationUnidadeMedida.keySet().stream()
                .filter(x -> mapaEstoquePorMaterialLocationUnidadeMedida.get(x).containsKey(location))
                .collect(Collectors.toSet());
    }
}
