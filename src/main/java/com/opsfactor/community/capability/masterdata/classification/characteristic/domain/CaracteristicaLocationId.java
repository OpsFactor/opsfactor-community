package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;

/**
 * Pseudo-caracteristica Community que expõe somente o id tecnico da location.
 *
 * <p>Ela coexiste com o catálogo dinâmico público para manter fluxos capazes de
 * tratar `locationId` como atributo técnico.</p>
 */
public class CaracteristicaLocationId implements CaracteristicaLocationInterface {

    @Override
    public String getValorCaracteristicaDeLocation(Location location) {

        return location.getId();

    }

    @Override
    public String getId() {

        return "locationId";

    }

    @Override
    public String toString() {

        return "locationId";

    }

}
