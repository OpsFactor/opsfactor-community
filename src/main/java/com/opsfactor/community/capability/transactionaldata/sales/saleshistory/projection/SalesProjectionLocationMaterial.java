package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

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
 * Projection material/location para leitura rapida de vendas historicas
 * agregadas.
 *
 * <p>A classe nao conhece a fonte documental da venda. No Community, a factory
 * popula apenas sell-out; no Enterprise, um overlay pode compor outras fontes
 * preservando os mesmos metodos neutros de sales.</p>
 */
@SuperBuilder
@Getter
public class SalesProjectionLocationMaterial extends SalesProjectionAbstract {
    
    /**
     * Indice material -> location -> agregados sales. Cada agregado preserva a
     * UOM original para conversao explicita no momento da leitura.
     */
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private Map<Produto,Map<Location,Set<AggregatedByLocationMaterialUOM>>> mapaVendasAgregadasPorMaterialLocation = new HashMap<>();
    
    public void addSalesAgregado(AggregatedByLocationMaterialUOM aggregatedByLocationMaterialUOM) {

        validaSalesAgregadoObrigatorio(
                aggregatedByLocationMaterialUOM,
                "location-material sales projection");
        validaSalesAgregadoMaterialObrigatorio(
                aggregatedByLocationMaterialUOM.getMaterial(),
                "location-material sales projection");
        validaSalesAgregadoLocationObrigatoria(
                aggregatedByLocationMaterialUOM.getLocation(),
                "location-material sales projection");

        mapaVendasAgregadasPorMaterialLocation
                .computeIfAbsent(aggregatedByLocationMaterialUOM.getMaterial(), x -> new HashMap<>())
                .computeIfAbsent(aggregatedByLocationMaterialUOM.getLocation(), x -> new HashSet<>())
                .add(aggregatedByLocationMaterialUOM);

    }
    
    public Set<AggregatedByLocationMaterialUOM> getSetSalesConsolidado() {
        
        return mapaVendasAgregadasPorMaterialLocation.values().stream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
        
    }
    
    public Set<AggregatedByLocationMaterialUOM> getSetSalesConsolidado(Produto produto, Location location) {
        
        return mapaVendasAgregadasPorMaterialLocation
                .getOrDefault(produto, new HashMap<>())
                .getOrDefault(location, new HashSet<>());
        
    }
    
    public Set<AggregatedByLocationMaterialUOM> getSetSalesConsolidado(Produto produto) {
        
        return mapaVendasAgregadasPorMaterialLocation
                .getOrDefault(produto, new HashMap<>()).values().stream() // set de sets de DTOs
                    .flatMap(x -> x.stream())
                    .collect(Collectors.toSet());
        
    }

    public Set<AggregatedByLocationMaterialUOM> getSetSalesConsolidado(Location location) {

        return mapaVendasAgregadasPorMaterialLocation
                .values()
                .stream()
                .flatMap(subMapa -> subMapa.getOrDefault(location, new HashSet<>()).stream()) // set de sets de DTOs
                .collect(Collectors.toSet());

    }

    // EXTRAÇÃO QUANTIDADE SALES PARA MATERIAL
    public double getQuantidadeSales(Produto produto, UnidadeMedida unidadeMedida) {
        
        return getSetSalesConsolidado(produto).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(produto, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
        

    }

    // EXTRAÇÃO QUANTIDADE SALES PARA LOCATION
    public double getQuantidadeSales(Location location, UnidadeMedida unidadeMedida) {

        return getSetSalesConsolidado(location).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(x.getMaterial(), x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();

    }

    // EXTRAÇÃO QUANTIDADE SALES MATERIAL/LOCATION
    public double getQuantidadeSales(Produto produto, Location location, UnidadeMedida unidadeMedida) {
        return  SalesProjectionLocationMaterial.this.getSetSalesConsolidado(produto, location).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(produto, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }

    public Set<Produto> getMateriaisComSalesEmLocation(Location location) {
        return mapaVendasAgregadasPorMaterialLocation.entrySet()
                .stream()
                .filter(entry -> entry.getValue().keySet().contains(location))
                .map(entry -> entry.getKey())
                .collect(Collectors.toSet());
    }

    public Set<Location> getLocationsComSales() {
        return mapaVendasAgregadasPorMaterialLocation.values()
                .stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toSet());
    }

}
