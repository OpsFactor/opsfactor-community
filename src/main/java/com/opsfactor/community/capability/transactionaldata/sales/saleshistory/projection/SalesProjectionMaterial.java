package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import com.pivovarit.function.ThrowingFunction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Projection material para leitura rapida de vendas historicas agregadas.
 *
 * <p>A classe usa API neutra de sales. A origem sell-out do Community fica na
 * factory, que consulta `SelloutRepository`; a projection apenas indexa os
 * agregados ja materializados.</p>
 */
@SuperBuilder
@Getter
public class SalesProjectionMaterial extends SalesProjectionAbstract {
    
    /**
     * Indice material -> agregados sales, usado quando a location nao faz
     * parte da consulta.
     */
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private Map<Produto,Set<AggregatedByMaterialUOM>> mapaVendasAgregadasPorMaterial = new HashMap<>();
    
    public void addSalesAgregado(AggregatedByMaterialUOM aggregatedByMaterialUOM) {

        validaSalesAgregadoObrigatorio(
                aggregatedByMaterialUOM,
                "material sales projection");
        validaSalesAgregadoMaterialObrigatorio(
                aggregatedByMaterialUOM.getMaterial(),
                "material sales projection");

        mapaVendasAgregadasPorMaterial
                .computeIfAbsent(aggregatedByMaterialUOM.getMaterial(), x -> new HashSet<>())
                .add(aggregatedByMaterialUOM);

    }
    
    public Set<AggregatedByMaterialUOM> getSetSalesAgregado(Produto produto) {
        if (!mapaVendasAgregadasPorMaterial.containsKey(produto)) {
            return new HashSet<>();
        } else {
            return mapaVendasAgregadasPorMaterial.get(produto);
        }
    }
    
    public Float getQuantidadeSales(Produto produto, UnidadeMedida unidadeMedida) {
        return (float) getSetSalesAgregado(produto).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(produto, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }

    public Set<Produto> getMateriaisComSales() {
        return mapaVendasAgregadasPorMaterial.keySet();
    }

    
}
