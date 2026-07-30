package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedDataInterface;

import java.time.LocalDateTime;

/**
 * Interface usada para queries de primeira/ultima data por location.
 * No Community atende vendas sell-out e estoque; pedidos e ordens ficam no Enterprise.
 */
public interface FirstLastByLocation extends AggregatedDataInterface {

    Location getLocation();

    LocalDateTime getFirstDateTime();
    LocalDateTime getLastDateTime();

}
