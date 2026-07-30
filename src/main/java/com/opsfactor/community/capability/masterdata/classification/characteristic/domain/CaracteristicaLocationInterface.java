package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pseudo-caracteristica de location aceita no Community.
 *
 * <p>Implementacoes reais baseadas em cadastro dinamico pertencem ao
 * Enterprise. A implementacao Community viva e {@link CaracteristicaLocationId},
 * usada apenas quando o fluxo precisa acessar o id da location pelo contrato
 * historico de caracteristicas.</p>
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
