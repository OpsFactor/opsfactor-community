package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.Map;

/**
 * Dados de apoio para resolver versao de malha, locations e UOMs na carga
 * Community de linhas de transporte.
 */
public class LinhaTransporteIntegrationSupportData {
    
    public Map<String,VersaoMalha> mapaVersaoMalhaPorId;
    public Map<String,Location> mapaLocationOrigemPorId;
    public Map<String,Location> mapaLocationDestinoPorId;
    public Map<String,UnidadeMedida> mapaUomLoteMinimoMultiploTransportePorId;
    
}
