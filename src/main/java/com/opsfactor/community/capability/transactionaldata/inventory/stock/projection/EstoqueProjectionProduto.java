package com.opsfactor.community.capability.transactionaldata.inventory.stock.projection;

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
 * Projection de estoque agregado por material/UOM, sem dimensao location.
 *
 * <p>Usada por consultas quantitativas em que o estoque inicial precisa ser
 * somado por material antes de entrar em projections mais especificas.</p>
 */
@SuperBuilder
@Getter
public class EstoqueProjectionProduto extends EstoqueProjectionAbstract {
        
    /**
     * Indice material -> agregados de estoque. Esta projection representa um
     * snapshot sem dimensao de data.
     */
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private Map<Produto,Set<AggregatedByMaterialUOM>> mapaEstoquePorMaterialUnidadeMedida = new HashMap<>();
    
    public void addEstoque(AggregatedByMaterialUOM aggregatedByMaterialUOM) {
        
        validaEstoqueAgregadoObrigatorio(
                aggregatedByMaterialUOM,
                "material stock projection");
        validaEstoqueAgregadoMaterialObrigatorio(
                aggregatedByMaterialUOM.getMaterial(),
                "material stock projection");
        
        mapaEstoquePorMaterialUnidadeMedida
                .computeIfAbsent(aggregatedByMaterialUOM.getMaterial(), x -> new HashSet<>())
                .add(aggregatedByMaterialUOM);
        
    }
    
    public Set<AggregatedByMaterialUOM> getEstoques(Produto produto) {
        
        if (mapaEstoquePorMaterialUnidadeMedida == null) mapaEstoquePorMaterialUnidadeMedida = new HashMap<>();
        
        if (!mapaEstoquePorMaterialUnidadeMedida.containsKey(produto)) return new HashSet<>();
        
        return mapaEstoquePorMaterialUnidadeMedida.get(produto);
        
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
    
    public Set<Produto> getMateriaisComEstoque() {
        return mapaEstoquePorMaterialUnidadeMedida.keySet();
    }
}
