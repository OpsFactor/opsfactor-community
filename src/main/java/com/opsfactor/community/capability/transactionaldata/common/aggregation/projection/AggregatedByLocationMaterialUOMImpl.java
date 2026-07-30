package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Implementacao simples do agregado transacional por location/material/UOM.
 * No Community atende vendas sell-out e estoque; pedidos e ordens ficam no Enterprise.
 * Agrupamento por location, material e unidade de medida
 */
@Builder
@Getter
@Setter
public class AggregatedByLocationMaterialUOMImpl implements AggregatedByLocationMaterialUOM {
    
    private Location location;
    private Produto material;
    private UnidadeMedida uom; 
    
    private Double totalQuantity;
        
}
