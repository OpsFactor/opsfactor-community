package com.opsfactor.community.capability.masterdata.production.routing.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.Map;

/**
 * Dados de apoio para resolver location e material de saida do roteiro
 * durante data upload Community.
 */
public class RoteiroIntegrationSupportData {
    
    public Map<String,Produto> mapaMaterialPorId;
    public Map<String,Location> mapaLocationPorId;
    public Map<String,UnidadeMedida> mapaUnidadeMedidaPorId;
    
}
