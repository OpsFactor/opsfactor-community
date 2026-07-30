package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Implementacao simples do agregado transacional por location/material/UOM/data.
 * No Community atende vendas sell-out e estoque; pedidos e ordens ficam no Enterprise.
 * Agrupamento por material, unidade de medida e data (que pode representar dias ou fechamento de semanas/meses)
 */
@Builder
@Getter
@Setter
public class AggregatedByLocationMaterialUOMDateImpl implements AggregatedByLocationMaterialUOMDate {
    
    private Location location;
    private Produto material;
    private UnidadeMedida uom; 
    
    private LocalDate referenceDate;
    
    private Double totalQuantity;
        
}
