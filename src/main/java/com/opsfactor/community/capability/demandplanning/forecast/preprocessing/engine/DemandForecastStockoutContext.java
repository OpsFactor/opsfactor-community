package com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine;

import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionLocationProdutoData;

/**
 * Fotografia imutavel de estoque usada por um tratamento de stockout durante
 * uma rodada de forecast.
 *
 * <p>O Community nunca instancia este contexto porque seu processor permanece
 * neutro. O tipo compartilhado permite que o overlay Enterprise entregue uma
 * unica projection historica pre-carregada ao workflow, sem guardar estado no
 * bean Spring nem consultar estoque uma vez por serie.</p>
 */
public record DemandForecastStockoutContext(
        EstoqueProjectionLocationProdutoData estoqueProjectionLocationProdutoData,
        int diasDohStockout) {

    public DemandForecastStockoutContext {

        if (estoqueProjectionLocationProdutoData == null) {
            throw new IllegalArgumentException(
                    "Historical stock projection is required for Demand Planning stockout treatment.");
        }
        if (diasDohStockout <= 0) {
            throw new IllegalArgumentException(
                    "Demand Planning stockout DOH threshold must be positive.");
        }

    }

}
