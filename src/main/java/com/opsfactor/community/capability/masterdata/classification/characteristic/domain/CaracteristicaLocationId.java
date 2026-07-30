package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;

/**
 * Pseudo-caracteristica Community que expõe somente o id tecnico da location.
 *
 * <p>Ela existe para manter fluxos compartilhados capazes de tratar locationId
 * como atributo tecnico sem depender do cadastro dinamico de caracteristicas
 * Enterprise.</p>
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
