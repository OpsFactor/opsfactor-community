package com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;

import java.util.Map;

/**
 * Dados de apoio para resolver location, material e UOM na carga Community de
 * estoque inicial.
 */
public class EstoqueIntegrationSupportData {
    
    public Map<String,Location> mapaLocationPorId;
    public Map<String,Produto> mapaProdutoPorId;
    public Map<String,UnidadeMedida> mapaUomPorId;
    
}
