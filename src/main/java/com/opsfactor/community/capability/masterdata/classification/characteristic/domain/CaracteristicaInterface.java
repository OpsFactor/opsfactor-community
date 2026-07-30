package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;


/**
 * Contrato mínimo de pseudo-características Community.
 *
 * <p>Caracteristicas dinamicas reais de material/location pertencem ao
 * Enterprise. No Community, este contrato fica restrito a identificadores
 * tecnicos simples, como materialId e locationId, quando algum fluxo precisa
 * tratar a dimensao como atributo.</p>
 */
public interface CaracteristicaInterface {

    public String toString();
    public String getId();

}
