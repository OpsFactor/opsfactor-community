package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;

/**
 * Agregado transacional por material/UOM. No Community atende vendas sell-out
 * e estoque; pedidos e ordens ficam no Enterprise.
 * Agrupamento por material
 */
public interface AggregatedByMaterialUOM extends AggregatedDataInterface {
    
    Produto getMaterial();
    
}
