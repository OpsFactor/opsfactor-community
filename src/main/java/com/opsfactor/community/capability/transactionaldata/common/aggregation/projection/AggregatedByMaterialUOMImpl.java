package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import lombok.Builder;
import lombok.Getter;

/**
 * Implementacao simples do agregado quantitativo por material/UOM.
 */
@Builder
@Getter
public class AggregatedByMaterialUOMImpl implements AggregatedByMaterialUOM {

    private Produto material;
    private UnidadeMedida uom;    
    
    private Double totalQuantity;
    
}
