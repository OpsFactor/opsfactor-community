package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Combinação elementar material/location usada como DFU.
 *
 * <p>O atributo fisico continua chamado {@code produto} para acompanhar a
 * entidade JPA {@link Produto}. Novos helpers estaticos, porem, usam a
 * nomenclatura material para evitar que a borda Community propague o termo
 * historico.</p>
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class DFU {
    
    private Produto produto;
    private Location location;
    
    public static Set<Produto> getMateriaisDeDFUs(Set<DFU> dfus) {

        return dfus.stream()
                .map(DFU::getProduto)
                .collect(Collectors.toSet());

    }
    
    public static Set<Location> getLocationsDeDFUs(Set<DFU> dfus) {

        return dfus.stream()
                .map(DFU::getLocation)
                .collect(Collectors.toSet());

    }
        
}
