package com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.Map;

/**
 * Dados de apoio para resolver material, location e UOM na carga Community de
 * sell-out.
 */
public class SelloutIntegrationSupportData {
    
    public Map<String,Produto> mapaMaterialPorId;
    public Map<String,Location> mapaLocationPorId;
    public Map<String,UnidadeMedida> mapaUnidadeMedidaPorId;
    
}
