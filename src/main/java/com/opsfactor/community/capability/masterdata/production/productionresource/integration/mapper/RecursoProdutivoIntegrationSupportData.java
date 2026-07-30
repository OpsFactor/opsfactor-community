package com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.Map;

/**
 * Dados de apoio para resolver location e UOMs transicionais durante data
 * upload de recursos produtivos Community.
 */
public class RecursoProdutivoIntegrationSupportData {
    
    public Map<String,UnidadeMedida> mapaUnidadeMedidaPorId;
    public Map<String,Location> mapaLocationPorId;
    
}
