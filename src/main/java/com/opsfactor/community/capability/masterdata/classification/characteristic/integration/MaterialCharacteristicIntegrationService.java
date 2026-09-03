package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaLocationRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaMaterialRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Batch-oriented Community integration for material-characteristic definitions. */
@Service
public class MaterialCharacteristicIntegrationService implements IntegrationServiceInterface<
        MaterialCharacteristicIntegrationDataDto,
        MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO,
        CaracteristicaProduto,
        MaterialCharacteristicIntegrationSupportData,
        MaterialCharacteristicIntegrationMapper,
        EmptyIntegrationDataFilter> {

    /** Repository usado para persistir caracteristicas de material em lote. */
    @Autowired
    private CaracteristicaMaterialRepository caracteristicaMaterialRepository;

    /** Repository usado para validar ids conflitantes de caracteristicas de location. */
    @Autowired
    private CaracteristicaLocationRepository caracteristicaLocationRepository;

    /** Mapper do contrato publico de integracao de caracteristicas de material. */
    @Autowired
    private MaterialCharacteristicIntegrationMapper materialCharacteristicIntegrationMapper;

    @Override
    public MaterialCharacteristicIntegrationMapper getMapper() {

        return materialCharacteristicIntegrationMapper;

    }

    @Override
    public List<CaracteristicaProduto> saveEntityList(Collection<CaracteristicaProduto> entityList) {

        CharacteristicIntegrationValidation.validaCharacteristicCollection(entityList, "Material Characteristic entity collection");
        return entityList.isEmpty() ? new ArrayList<>() : caracteristicaMaterialRepository.saveAll(entityList);

    }

    @Override
    public void removeEntityList(Collection<CaracteristicaProduto> entityList) {

        CharacteristicIntegrationValidation.validaCharacteristicCollection(entityList, "Material Characteristic entity collection");
        if (!entityList.isEmpty()) {
            caracteristicaMaterialRepository.deleteAllInBatch(entityList);
        }

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Material Characteristic data saved";

    }

    @Override
    public MaterialCharacteristicIntegrationSupportData getSupportData() {

        MaterialCharacteristicIntegrationSupportData supportData = new MaterialCharacteristicIntegrationSupportData();
        supportData.locationCharacteristicIdSet = caracteristicaLocationRepository.findAll().stream()
                .map(characteristic -> characteristic.getId())
                .collect(java.util.stream.Collectors.toSet());
        supportData.materialLocationCharacteristicIdSet = Set.of();
        return supportData;

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    @Override
    public Collection<CaracteristicaProduto> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO> primaryKeys =
                CharacteristicIntegrationValidation.validaPrimaryKeyDtoCollection(
                        dtoBatchList,
                        "Material Characteristic primary key collection",
                        (primaryKey, index) -> CharacteristicIntegrationValidation.getRequiredPrimaryKeyField(
                                primaryKey.materialCharacteristicId,
                                "materialCharacteristicId",
                                "Material Characteristic primary key collection",
                                index));
        return caracteristicaMaterialRepository.findAllById(primaryKeys.stream()
                .map(primaryKey -> primaryKey.materialCharacteristicId)
                .toList());

    }

    @Override
    public Collection<CaracteristicaProduto> getAllPersistedEntities() {

        return caracteristicaMaterialRepository.findAll().stream()
                .sorted(Comparator.comparing(CaracteristicaProduto::getId))
                .toList();

    }

}
