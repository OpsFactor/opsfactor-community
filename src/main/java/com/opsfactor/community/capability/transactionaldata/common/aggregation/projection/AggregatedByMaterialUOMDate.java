package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import java.time.LocalDate;

/**
 * Agregado transacional por material/UOM/data. No Community atende vendas
 * sell-out e estoque; pedidos e ordens ficam no Enterprise.
 * Agrupamento por material, unidade de medida e data (que pode representar dias ou fechamento de semanas/meses)
 */
public interface AggregatedByMaterialUOMDate extends AggregatedDataInterface {
    
    Produto getMaterial();
    LocalDate getReferenceDate();
    
}
