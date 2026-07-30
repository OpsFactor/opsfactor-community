package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import java.util.Map;

/**
 * Dados de apoio para resolver as locations padrao da versao de malha.
 *
 * <p>A carga consulta locations uma unica vez por request e entrega ao mapper
 * um mapa validado. Isso evita lookup por linha durante imports grandes e
 * preserva falha explicita quando o snapshot de apoio vier inconsistente.</p>
 */
public class VersaoMalhaIntegrationSupportData {

    public Map<String, Location> mapaLocationPorId;

}
