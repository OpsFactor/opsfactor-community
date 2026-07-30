package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import java.time.LocalDate;

/**
 * Agregado transacional por location/material/UOM/data. No Community atende
 * vendas sell-out e estoque; pedidos e ordens ficam no Enterprise.
 * Agrupamento por material, unidade de medida e data (que pode representar dias ou fechamento de semanas/meses)
 */
public interface AggregatedByLocationMaterialUOMDate extends AggregatedDataInterface {
    
    Location getLocation();
    Produto getMaterial();
    LocalDate getReferenceDate();

}
