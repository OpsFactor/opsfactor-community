package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDate;
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

/**
 * Projection diaria de vendas por material.
 *
 * <p>O mapa interno consolida `Map<Produto, Map<LocalDate, Set<AggregatedByMaterialUOMDate>>>`
 * para leitura rapida de vendas historicas em buckets diarios. Buckets menores que diario
 * devem usar outra projection, sem alterar a semantica desta classe.</p>
 */
@SuperBuilder
@Getter
public class SalesProjectionMaterialData extends SalesProjectionAbstract {

    /**
     * Indice material -> data -> agregados sales para consultas sem location.
     */
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private Map<Produto,Map<LocalDate,Set<AggregatedByMaterialUOMDate>>> mapaVendasAgregadasPorPeriodo = new HashMap<>();
    
    public void addSalesAgregado(AggregatedByMaterialUOMDate aggregatedByMaterialUOMDate) {

        validaSalesAgregadoObrigatorio(
                aggregatedByMaterialUOMDate,
                "material-date sales projection");
        validaSalesAgregadoMaterialObrigatorio(
                aggregatedByMaterialUOMDate.getMaterial(),
                "material-date sales projection");
        validaSalesAgregadoReferenceDateObrigatoria(
                aggregatedByMaterialUOMDate.getReferenceDate(),
                "material-date sales projection");

        mapaVendasAgregadasPorPeriodo
                .computeIfAbsent(aggregatedByMaterialUOMDate.getMaterial(), x -> new HashMap<>())
                .computeIfAbsent(aggregatedByMaterialUOMDate.getReferenceDate(), x -> new HashSet<>())
                .add(aggregatedByMaterialUOMDate);

    }
    
    public Set<AggregatedByMaterialUOMDate> getSetSalesAgregado(Produto produto, LocalDate data) {
        if (!mapaVendasAgregadasPorPeriodo.containsKey(produto)
                || !mapaVendasAgregadasPorPeriodo.get(produto).containsKey(data)) {
            return new HashSet<>();
        }
        return mapaVendasAgregadasPorPeriodo.get(produto).get(data);
    }
    
    // SALES QUANTIDADE POR MATERIAL / PERIODO
    public Float getQuantidadeSales(Produto produto, int posicaoPeriodo, UnidadeMedida unidadeMedida) {
        return getQuantidadeSales(produto, calendario.getUltimaDataPeriodo(posicaoPeriodo), unidadeMedida);
    }
    public Float getQuantidadeSales(Produto produto, LocalDate data, UnidadeMedida unidadeMedida) {
        return (float) getSetSalesAgregado(produto, data).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(produto, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }

}
