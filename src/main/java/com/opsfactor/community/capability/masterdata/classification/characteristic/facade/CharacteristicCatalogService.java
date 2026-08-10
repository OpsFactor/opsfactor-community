package com.opsfactor.community.capability.masterdata.classification.characteristic.facade;

import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaLocationDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaProdutoDTO;

import java.util.List;

/** Edition-neutral catalog contract consumed by the shared REST controller. */
public interface CharacteristicCatalogService {

    List<CaracteristicaProdutoDTO> getCaracteristicaProdutoDTOList();

    List<CaracteristicaLocationDTO> getCaracteristicaLocationDTOList();

}
