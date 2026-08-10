package com.opsfactor.community.capability.masterdata.classification.characteristic.facade;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaLocationDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaProdutoDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.TipoCaracteristicaDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaLocationRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaMaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Community implementation of the real material and location characteristic catalog. */
@Service
public class CommunityCharacteristicCatalogService implements CharacteristicCatalogService {

    @Autowired
    private CaracteristicaMaterialRepository caracteristicaMaterialRepository;

    @Autowired
    private CaracteristicaLocationRepository caracteristicaLocationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CaracteristicaProdutoDTO> getCaracteristicaProdutoDTOList() {

        return caracteristicaMaterialRepository.findAllWithValues().stream()
                .sorted(Comparator.comparing(CaracteristicaProduto::getId))
                .map(this::toMaterialDto)
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public List<CaracteristicaLocationDTO> getCaracteristicaLocationDTOList() {

        return caracteristicaLocationRepository.findAllWithValues().stream()
                .sorted(Comparator.comparing(CaracteristicaLocation::getId))
                .map(this::toLocationDto)
                .toList();

    }

    private CaracteristicaProdutoDTO toMaterialDto(CaracteristicaProduto materialCharacteristic) {

        CaracteristicaProdutoDTO dto = new CaracteristicaProdutoDTO();
        dto.caracteristicaId = materialCharacteristic.getId();
        dto.descricao = materialCharacteristic.getDescricao();
        dto.tipoCaracteristica = toTypeDto(materialCharacteristic.getTipoCaracteristica());
        dto.listaAtributos = materialCharacteristic.getValoresCaracteristica().stream().distinct().sorted().toList();
        return dto;

    }

    private CaracteristicaLocationDTO toLocationDto(CaracteristicaLocation locationCharacteristic) {

        CaracteristicaLocationDTO dto = new CaracteristicaLocationDTO();
        dto.caracteristicaId = locationCharacteristic.getId();
        dto.descricao = locationCharacteristic.getDescricao();
        dto.tipoCaracteristica = toTypeDto(locationCharacteristic.getTipoCaracteristica());
        dto.listaAtributos = locationCharacteristic.getValoresCaracteristica().stream().distinct().sorted().toList();
        return dto;

    }

    private TipoCaracteristicaDTO toTypeDto(Caracteristica.TipoCaracteristica type) {

        return switch (type) {
            case BINARIO -> TipoCaracteristicaDTO.BINARIO;
            case NUMERICO -> TipoCaracteristicaDTO.NUMERICO;
            case CATEGORICO -> TipoCaracteristicaDTO.CATEGORICO;
        };

    }

}
