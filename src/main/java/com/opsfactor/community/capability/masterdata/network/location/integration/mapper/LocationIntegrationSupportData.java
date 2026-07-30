package com.opsfactor.community.capability.masterdata.network.location.integration.mapper;

import com.opsfactor.community.capability.masterdata.organization.economicgroup.domain.EconomicGroup;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.util.Map;
import lombok.Builder;

/**
 * Dados de apoio para resolver UOMs referenciadas pelo cadastro Community de
 * locations.
 */
@Builder
public class LocationIntegrationSupportData {
    
    Map<String,UnidadeMedida> unidadeMedidaMap;

    /**
     * Fotografia batch dos grupos econômicos que podem ser referenciados por
     * Location. O mapper Community continua bloqueando a edição desse campo;
     * o mapa existe para o overlay Enterprise resolver a FK sem consulta por
     * linha durante o Data Upload.
     */
    Map<String, EconomicGroup> economicGroupMap;

    /**
     * Fotografia batch das locations disponiveis para resolver a referencia
     * entre locations sem disparar uma consulta por linha de Data Upload.
     */
    Map<String, Location> locationMap;

    /**
     * Resolve uma UOM a partir da fotografia batch preparada pelo integration
     * service. O accessor permite que overlays Enterprise reutilizem a mesma
     * carga sem expor o mapa mutável nem executar nova consulta por linha.
     */
    public UnidadeMedida getUnidadeMedidaById(String unidadeMedidaId) {

        return unidadeMedidaMap == null ? null : unidadeMedidaMap.get(unidadeMedidaId);

    }

    /**
     * Resolve um grupo econômico a partir da fotografia de integração já
     * carregada pelo serviço de locations.
     */
    public EconomicGroup getEconomicGroupById(String economicGroupId) {

        return economicGroupMap == null ? null : economicGroupMap.get(economicGroupId);

    }

    /**
     * Resolve uma location a partir da fotografia batch preparada pelo
     * integration service.
     */
    public Location getLocationById(String locationId) {

        return locationMap == null ? null : locationMap.get(locationId);

    }
    
}
