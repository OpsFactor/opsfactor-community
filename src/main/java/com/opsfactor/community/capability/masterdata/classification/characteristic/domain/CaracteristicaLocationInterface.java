package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrato Community para características públicas de location.
 *
 * <p>{@link CaracteristicaLocation} representa o catálogo dinâmico e
 * {@link CaracteristicaLocationId} preserva a dimensão técnica do ID.</p>
 */
public interface CaracteristicaLocationInterface extends CaracteristicaInterface {

    public String getValorCaracteristicaDeLocation(Location location);

    public default List<String> getValoresCaracteristicaDeListaLocations(List<Location> locations) {

        return locations.stream()
                .map(l -> getValorCaracteristicaDeLocation(l))
                .distinct()
                .collect(Collectors.toList());

    }

}
