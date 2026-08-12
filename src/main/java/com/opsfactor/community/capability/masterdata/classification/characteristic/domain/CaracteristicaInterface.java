package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;


/**
 * Contrato comum das características públicas de material/location.
 *
 * <p>Ele também é implementado pelas dimensões técnicas `materialId` e
 * `locationId`, preservando os adapters históricos que tratam IDs como
 * características.</p>
 */
public interface CaracteristicaInterface {

    public String toString();
    public String getId();

}
