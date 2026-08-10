package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import java.util.Set;

/**
 * Support data da carga Enterprise de caracteristicas de material.
 *
 * <p>A carga valida que um id de caracteristica de material nao esteja sendo
 * usado tambem como caracteristica de location ou material-location. Essa
 * restricao vem do legado e evita ambiguidades no front-end e em estruturas que
 * exibem caracteristicas de naturezas diferentes na mesma configuracao.</p>
 */
public class MaterialCharacteristicIntegrationSupportData {

    /**
     * Ids de caracteristicas de location ja persistidas.
     */
    public Set<String> locationCharacteristicIdSet;

    /**
     * Ids de caracteristicas material-location ja persistidas.
     */
    public Set<String> materialLocationCharacteristicIdSet;

}
