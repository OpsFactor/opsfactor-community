package com.opsfactor.community.capability.transactionaldata.inventory.stock.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.pivovarit.function.ThrowingFunction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Projection material/location/data para consolidacao rapida de estoque.
 *
 * <p>No Community, estoque transacional e snapshot simples por material,
 * location, data, quantidade e UOM. Lote, validade, aging/writeoff e producao
 * em batch seguem Enterprise.</p>
 */
@SuperBuilder
@Getter
public class EstoqueProjectionLocationProdutoData extends EstoqueProjectionAbstract {
        
    /**
     * Indice material -> location -> data -> agregados de estoque.
     */
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private Map<Produto,Map<Location,Map<LocalDate,Set<AggregatedByLocationMaterialUOMDate>>>> mapaEstoquePorMaterialLocationUnidadeMedidaPorData = new HashMap<>();
    
    public void addEstoque(AggregatedByLocationMaterialUOMDate aggregatedByLocationMaterialUOMDate) {

        validaEstoqueAgregadoObrigatorio(
                aggregatedByLocationMaterialUOMDate,
                "location/material/date stock projection");
        validaEstoqueAgregadoMaterialObrigatorio(
                aggregatedByLocationMaterialUOMDate.getMaterial(),
                "location/material/date stock projection");
        validaEstoqueAgregadoLocationObrigatoria(
                aggregatedByLocationMaterialUOMDate.getLocation(),
                "location/material/date stock projection");
        validaEstoqueAgregadoReferenceDateObrigatoria(
                aggregatedByLocationMaterialUOMDate.getReferenceDate(),
                "location/material/date stock projection");

        mapaEstoquePorMaterialLocationUnidadeMedidaPorData
                .computeIfAbsent(aggregatedByLocationMaterialUOMDate.getMaterial(), x -> new HashMap<>())
                .computeIfAbsent(aggregatedByLocationMaterialUOMDate.getLocation(), x -> new HashMap<>())
                .computeIfAbsent(aggregatedByLocationMaterialUOMDate.getReferenceDate(), x -> new HashSet<>())
                .add(aggregatedByLocationMaterialUOMDate);

    }
    
    public Set<AggregatedByLocationMaterialUOMDate> getEstoques(Produto material) {
                
        return mapaEstoquePorMaterialLocationUnidadeMedidaPorData
                .getOrDefault(material, new HashMap<>())
                .values().stream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
        
    }
    
    public Set<AggregatedByLocationMaterialUOMDate> getEstoques() {
        
        return mapaEstoquePorMaterialLocationUnidadeMedidaPorData.values().stream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
        
    }
    
    public Set<AggregatedByLocationMaterialUOMDate> getEstoques(Location location, Produto material) {
        
        return mapaEstoquePorMaterialLocationUnidadeMedidaPorData
                .getOrDefault(material, new HashMap<>())
                .getOrDefault(location, new HashMap<>())
                .values().stream()
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
        
    }
    
    public Set<AggregatedByLocationMaterialUOMDate> getEstoques(Location location, Produto material, LocalDate dataReferencia) {
        
        return mapaEstoquePorMaterialLocationUnidadeMedidaPorData
                .getOrDefault(material, new HashMap<>())
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(dataReferencia, new HashSet<>());
        
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

    public Float getQuantidadeEstoque(Produto material, Location location, LocalDate dataReferencia, UnidadeMedida unidadeMedida) {
        return (float) getEstoques(location, material, dataReferencia).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(material, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }
    
    public Float getQuantidadeEstoque(Produto material, Location location, int posicaoPeriodo, UnidadeMedida unidadeMedida) {
        return (float) getEstoques(location, material, calendario.getUltimaDataPeriodo(posicaoPeriodo)).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(material, x.getUom(), unidadeMedida)))
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
        return mapaEstoquePorMaterialLocationUnidadeMedidaPorData.keySet();
    }
    public Set<Location> getLocationsComEstoque() {
        return mapaEstoquePorMaterialLocationUnidadeMedidaPorData.entrySet().stream()
                .flatMap(x -> x.getValue().keySet().stream())
                .collect(Collectors.toSet());
    }
    public Set<Location> getLocationsAtivasComEstoque() {
        return mapaEstoquePorMaterialLocationUnidadeMedidaPorData.entrySet().stream()
                .flatMap(x -> x.getValue().keySet().stream())
                .filter(x -> x.getAtivo())
                .collect(Collectors.toSet());
    }
    
    public Set<Produto> getMateriaisComEstoqueNaLocation(Location location) {
        return mapaEstoquePorMaterialLocationUnidadeMedidaPorData.keySet().stream()
                .filter(x -> mapaEstoquePorMaterialLocationUnidadeMedidaPorData.get(x).containsKey(location))
                .collect(Collectors.toSet());
    }
}
