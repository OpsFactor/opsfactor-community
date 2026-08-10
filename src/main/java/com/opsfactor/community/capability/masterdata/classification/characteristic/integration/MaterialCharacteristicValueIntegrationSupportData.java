package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;

import java.util.Map;

/**
 * Support data da carga Enterprise de valores de caracteristica por material.
 */
public class MaterialCharacteristicValueIntegrationSupportData {

    /**
     * Materiais Community indexados por id.
     */
    public Map<String, Produto> materialPorId;

    /**
     * Caracteristicas Enterprise de material indexadas por id.
     */
    public Map<String, CaracteristicaProduto> materialCharacteristicById;

}
