package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedDataInterface;

import java.time.LocalDateTime;

/**
 * Interface usada para queries de primeira/ultima data por material.
 * No Community atende vendas sell-out e estoque; pedidos e ordens ficam no Enterprise.
 */
public interface FirstLastByMaterial extends AggregatedDataInterface {
    
    Produto getMaterial();

    LocalDateTime getFirstDateTime();
    LocalDateTime getLastDateTime();

}
