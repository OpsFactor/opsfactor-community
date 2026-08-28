package com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import java.util.Map;

/**
 * Dados de apoio para resolver as dependências da versão de produção durante
 * data upload.
 */
public class VersaoProducaoIntegrationSupportData {
    
    public Map<String,VersaoProducao> mapaVersaoProducaoPorId;
    public Map<String,Location> mapaLocationPorId;
    public Map<String,Roteiro> mapaRoteiroPorId;
    public Map<String,ListaTecnica> mapaListaTecnicaPorId;
    
}
