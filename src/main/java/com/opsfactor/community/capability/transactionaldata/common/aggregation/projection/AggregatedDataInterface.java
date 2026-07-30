package com.opsfactor.community.capability.transactionaldata.common.aggregation.projection;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;

/**
 * Interface base para queries de agregacao transacional. No Community ela
 * atende vendas sell-out e estoque; pedidos e ordens ficam no Enterprise.
 * Agrupamento por material.
 *
 * <p>Na edicao Community este contrato e estritamente quantitativo. Valores
 * economicos como gross, net e COGS pertencem ao OpsFactor Enterprise e nao
 * devem ser carregados por projections usadas em calculos do Community.</p>
 */
public interface AggregatedDataInterface {
    
    Double getTotalQuantity();
    UnidadeMedida getUom();
        
}
