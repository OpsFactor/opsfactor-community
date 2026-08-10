package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
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

/** Batch-oriented Community integration for location-characteristic definitions. */
@Service
public class LocationCharacteristicIntegrationService implements IntegrationServiceInterface<
        LocationCharacteristicIntegrationDataDto,
        LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO,
        CaracteristicaLocation,
        LocationCharacteristicIntegrationSupportData,
        LocationCharacteristicIntegrationMapper,
        EmptyIntegrationDataFilter> {

    @Autowired
    private CaracteristicaLocationRepository caracteristicaLocationRepository;

    @Autowired
    private CaracteristicaMaterialRepository caracteristicaMaterialRepository;

    @Autowired
    private LocationCharacteristicIntegrationMapper locationCharacteristicIntegrationMapper;

    @Override
    public LocationCharacteristicIntegrationMapper getMapper() {

        return locationCharacteristicIntegrationMapper;

    }

    @Override
    public List<CaracteristicaLocation> saveEntityList(Collection<CaracteristicaLocation> entityList) {

        CharacteristicIntegrationValidation.validaCharacteristicCollection(entityList, "Location Characteristic entity collection");
        return entityList.isEmpty() ? new ArrayList<>() : caracteristicaLocationRepository.saveAll(entityList);

    }

    @Override
    public void removeEntityList(Collection<CaracteristicaLocation> entityList) {

        CharacteristicIntegrationValidation.validaCharacteristicCollection(entityList, "Location Characteristic entity collection");
        if (!entityList.isEmpty()) {
            caracteristicaLocationRepository.deleteAllInBatch(entityList);
        }

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Location Characteristic data saved";

    }

    @Override
    public LocationCharacteristicIntegrationSupportData getSupportData() {

        LocationCharacteristicIntegrationSupportData supportData = new LocationCharacteristicIntegrationSupportData();
        supportData.materialCharacteristicIdSet = caracteristicaMaterialRepository.findAll().stream()
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
    public Collection<CaracteristicaLocation> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO> primaryKeys =
                CharacteristicIntegrationValidation.validaPrimaryKeyDtoCollection(
                        dtoBatchList,
                        "Location Characteristic primary key collection",
                        (primaryKey, index) -> CharacteristicIntegrationValidation.getRequiredPrimaryKeyField(
                                primaryKey.locationCharacteristicId,
                                "locationCharacteristicId",
                                "Location Characteristic primary key collection",
                                index));
        return caracteristicaLocationRepository.findAllById(primaryKeys.stream()
                .map(primaryKey -> primaryKey.locationCharacteristicId)
                .toList());

    }

    @Override
    public Collection<CaracteristicaLocation> getAllPersistedEntities() {

        return caracteristicaLocationRepository.findAll().stream()
                .sorted(Comparator.comparing(CaracteristicaLocation::getId))
                .toList();

    }

}
