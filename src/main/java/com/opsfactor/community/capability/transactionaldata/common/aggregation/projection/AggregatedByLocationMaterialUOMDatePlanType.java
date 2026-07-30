package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.time.LocalDate;

/**
 * Agregado transacional por location/material/UOM/data/tipo de plano. No
 * Community atende vendas sell-out e estoque; pedidos e ordens ficam no Enterprise.
 * Agrupamento por material.
 *
 * <p>Na edicao Community o inventory plan agregado por tipo de plano e
 * quantitativo. Valores economicos por plano sao parte do modelo Enterprise
 * de P&L/cost-to-serve.</p>
 */
public interface AggregatedByLocationMaterialUOMDatePlanType {
    
    Double getTotalQuantityUnconstrained();
    Double getTotalQuantityConstrained();
    Double getTotalQuantityWorking();
    UnidadeMedida getUom();

    Location getLocation();
    Produto getMaterial();
    LocalDate getReferenceDate();
        
}
