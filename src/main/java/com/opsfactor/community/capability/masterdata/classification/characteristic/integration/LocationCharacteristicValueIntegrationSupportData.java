package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;

import java.util.Map;

/**
 * Support data da carga Enterprise de valores de caracteristica por location.
 */
public class LocationCharacteristicValueIntegrationSupportData {

    /**
     * Locations Community indexadas por id.
     */
    public Map<String, Location> locationPorId;

    /**
     * Caracteristicas Enterprise de location indexadas por id.
     */
    public Map<String, CaracteristicaLocation> locationCharacteristicById;

}
