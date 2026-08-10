package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import java.util.Set;

/**
 * Support data da carga Enterprise de caracteristicas de location.
 *
 * <p>Location, material e material-location compartilham superficies de filtro
 * e exibicao. A carga valida os outros catalogos para impedir ids ambiguos.</p>
 */
public class LocationCharacteristicIntegrationSupportData {

    /**
     * Ids de caracteristicas de material ja persistidas.
     */
    public Set<String> materialCharacteristicIdSet;

    /**
     * Ids de caracteristicas material-location ja persistidas.
     */
    public Set<String> materialLocationCharacteristicIdSet;

}
