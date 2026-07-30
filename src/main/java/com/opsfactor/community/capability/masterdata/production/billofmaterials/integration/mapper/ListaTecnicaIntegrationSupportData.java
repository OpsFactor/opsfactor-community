package com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.Map;

/**
 * Dados de apoio para resolver location, material de saida e UOM da BOM simples
 * durante data upload Community.
 */
public class ListaTecnicaIntegrationSupportData {
    
    public Map<String,UnidadeMedida> mapaUnidadeMedidaPorId;
    public Map<String,Produto> mapaMaterialPorId;
    public Map<String,Location> mapaLocationPorId;
    
}
