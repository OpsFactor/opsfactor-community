package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaLocationRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.EnterpriseValorCaracteristicaLocationRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service Enterprise de integracao de valores de caracteristica por location.
 */
@Service
public class LocationCharacteristicValueIntegrationService implements IntegrationServiceInterface<
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO,
        ValorCaracteristicaLocation,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationSupportData,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationMapper,
        EmptyIntegrationDataFilter> {

    /**
     * Repository Enterprise dos valores por location/caracteristica.
     */
    @Autowired
    private EnterpriseValorCaracteristicaLocationRepository enterpriseValorCaracteristicaLocationRepository;

    /**
     * Repository Community de locations usadas como chave da carga.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Repository Enterprise do catalogo de caracteristicas de location.
     */
    @Autowired
    private CaracteristicaLocationRepository caracteristicaLocationRepository;

    /**
     * Mapper Enterprise de arquivo/API.
     */
    @Autowired
    private com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationMapper locationCharacteristicValueIntegrationMapper;

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationMapper getMapper() {

        return locationCharacteristicValueIntegrationMapper;

    }

    @Override
    public List<ValorCaracteristicaLocation> saveEntityList(
            Collection<ValorCaracteristicaLocation> entityList) {

        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.validaEntityCollection(
                entityList,
                "Location Characteristic Value entity collection");

        if (entityList.isEmpty()) {
            return new ArrayList<>();
        }

        return validaSavedEntityCollection(
                enterpriseValorCaracteristicaLocationRepository.saveAll(entityList),
                "Location Characteristic Value saved collection",
                entityList.size());

    }

    @Override
    public void removeEntityList(Collection<ValorCaracteristicaLocation> entityList) {

        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.validaEntityCollection(
                entityList,
                "Location Characteristic Value entity collection");

        if (!entityList.isEmpty()) {
            enterpriseValorCaracteristicaLocationRepository.deleteAll(entityList);
        }

    }

    @Override
    public String getSaveSuccessMessage() {

        return "Location Characteristic Value data saved";

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationSupportData getSupportData() {

        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationSupportData supportData =
                new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationSupportData();
        supportData.locationPorId = getLocationPorId();
        supportData.locationCharacteristicById = getLocationCharacteristicById();
        return supportData;

    }

    @Override
    public int getBatchSize() {

        return 1000;

    }

    @Override
    public Collection<ValorCaracteristicaLocation> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO> locationCharacteristicValuePrimaryKeyDtoCollection =
                com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.validaPrimaryKeyDtoCollection(
                        dtoBatchList,
                        "Location Characteristic Value primary key collection",
                        (primaryKeyDto, index) -> {
                            String locationId = com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getRequiredPrimaryKeyField(
                                    primaryKeyDto.locationId,
                                    "locationId",
                                    "Location Characteristic Value primary key collection",
                                    index);
                            String locationCharacteristicId =
                                    com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getRequiredPrimaryKeyField(
                                            primaryKeyDto.locationCharacteristicId,
                                            "locationCharacteristicId",
                                            "Location Characteristic Value primary key collection",
                                            index);
                            return "location "
                                    + locationId
                                    + " / locationCharacteristic "
                                    + locationCharacteristicId;
                        });

        if (locationCharacteristicValuePrimaryKeyDtoCollection.isEmpty()) {
            return List.of();
        }

        Set<com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO> primaryKeys =
                new HashSet<>(locationCharacteristicValuePrimaryKeyDtoCollection);

        return getAllPersistedEntities()
                .stream()
                .filter(valorCaracteristicaLocation -> primaryKeys.contains(
                        locationCharacteristicValueIntegrationMapper.getPrimaryKeyDtoFromEntity(
                                valorCaracteristicaLocation)))
                .toList();

    }

    @Override
    public Collection<ValorCaracteristicaLocation> getAllPersistedEntities() {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.validaPersistedEntityCollection(
                        enterpriseValorCaracteristicaLocationRepository.customFindAllComCaracteristicaELocation(),
                        "Location Characteristic Value persisted collection")
                .stream()
                .sorted(Comparator
                        .comparing((ValorCaracteristicaLocation valorCaracteristicaLocation) ->
                                valorCaracteristicaLocation.getLocation().getId())
                        .thenComparing(valorCaracteristicaLocation ->
                                valorCaracteristicaLocation.getCaracteristicaLocation().getId()))
                .toList();

    }

    private java.util.Map<String, Location> getLocationPorId() {

        List<Location> locations = locationRepository.findAll();
        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getSupportEntityByIdMap(
                locations,
                "Location support collection",
                Location::getId,
                null);

    }

    private java.util.Map<String, CaracteristicaLocation> getLocationCharacteristicById() {

        List<CaracteristicaLocation> caracteristicasLocation =
                caracteristicaLocationRepository.findAll();
        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getSupportEntityByIdMap(
                caracteristicasLocation,
                "Location Characteristic support collection",
                CaracteristicaLocation::getId,
                caracteristicaLocation -> {
                    if (caracteristicaLocation == null
                            || caracteristicaLocation.getTipoCaracteristica() == null) {
                        throw new IllegalStateException(
                                "Location Characteristic support collection returned invalid characteristic.");
                    }
                });

    }

}
