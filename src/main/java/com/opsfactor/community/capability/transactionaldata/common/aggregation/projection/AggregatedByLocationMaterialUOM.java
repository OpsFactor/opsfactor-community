package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;

/**
 * Agregado transacional por location/material/UOM. No Community atende vendas
 * sell-out e estoque; pedidos e ordens ficam no Enterprise.
 * Agrupamento por material, unidade de medida e data (que pode representar dias ou fechamento de semanas/meses)
 */
public interface AggregatedByLocationMaterialUOM extends AggregatedDataInterface {
    
    Location getLocation();
    Produto getMaterial();
    
}
