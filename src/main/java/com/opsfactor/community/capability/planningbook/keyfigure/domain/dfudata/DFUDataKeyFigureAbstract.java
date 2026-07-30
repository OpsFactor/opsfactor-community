package com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Valor de Key Figure em um ponto material/location/data.
 *
 * <p>Essa estrutura e usada para transportar dados ja normalizados por uma
 * projection. Conversoes de unidade devem acontecer antes da criacao destes
 * objetos; aqui todos os valores de uma serie precisam estar na mesma unidade
 * de medida.</p>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@SuperBuilder
public abstract class DFUDataKeyFigureAbstract {

    /**
     * Material da DFU.
     */
    private Produto produto;

    /**
     * Location da DFU.
     */
    private Location location;

    /**
     * Periodo/data de referencia da Key Figure.
     */
    private LocalDateTime data;

    /**
     * Key Figure representada pelo valor.
     */
    private KeyFigureInterface keyFigure;
            
}
