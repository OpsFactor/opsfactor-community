package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationSupportData;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper Enterprise da carga de valores de caracteristica por location.
 */
@Component
public class LocationCharacteristicValueIntegrationMapper implements IntegrationMapperInterface<
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO,
        ValorCaracteristicaLocation,
        LocationCharacteristicValueIntegrationSupportData> {

    /**
     * Headers do arquivo Enterprise de valores de caracteristica por location.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Location Id",
            "Location Characteristic Id",
            "Characteristic Value");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            ValorCaracteristicaLocation entity) {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.builder()
                .characteristicValue(entity.getAtributo())
                .build();

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            ValorCaracteristicaLocation entity) {

        return new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO(
                entity.getLocation().getId(),
                entity.getCaracteristicaLocation().getId());

    }

    @Override
    public ValorCaracteristicaLocation createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO dto,
            LocationCharacteristicValueIntegrationSupportData supportData) {

        Location location = getLocation(dto, supportData);
        CaracteristicaLocation caracteristicaLocation = getCaracteristicaLocation(dto, supportData);

        ValorCaracteristicaLocation valorCaracteristicaLocation =
                new ValorCaracteristicaLocation();
        valorCaracteristicaLocation.setValorCaracteristicaLocationCompositeKey(
                new ValorCaracteristicaLocation.ValorCaracteristicaLocationCompositeKey(
                        location,
                        caracteristicaLocation));
        return valorCaracteristicaLocation;

    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            ValorCaracteristicaLocation entity,
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto dto,
            LocationCharacteristicValueIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        entity.setAtributo(
                com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getNormalizedAttributeValue(
                        dto.characteristicValue,
                        entity.getCaracteristicaLocation()));

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            ValorCaracteristicaLocation entity,
            LocationCharacteristicValueIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getLocation().getId());
        linhaArquivo.addContent(entity.getCaracteristicaLocation().getId());
        linhaArquivo.addContent(entity.getAtributo());
        return linhaArquivo;

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            LocationCharacteristicValueIntegrationSupportData supportData) {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.builder()
                .characteristicValue(processedFileRow.getColumnValueAsString(2))
                .build();

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            LocationCharacteristicValueIntegrationSupportData supportData) {

        return new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1));

    }

    private Location getLocation(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO dto,
            LocationCharacteristicValueIntegrationSupportData supportData) {

        validaPrimaryKeyESupportData(dto, supportData);

        Location location = supportData.locationPorId.get(dto.locationId);
        if (location == null) {
            throw new MissingDependencyDataUploadException(
                    "Location " + dto.locationId + " not found for location characteristic value",
                    dto);
        }
        return location;

    }

    private CaracteristicaLocation getCaracteristicaLocation(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO dto,
            LocationCharacteristicValueIntegrationSupportData supportData) {

        validaPrimaryKeyESupportData(dto, supportData);

        CaracteristicaLocation caracteristicaLocation =
                supportData.locationCharacteristicById.get(dto.locationCharacteristicId);
        if (caracteristicaLocation == null) {
            throw new MissingDependencyDataUploadException(
                    "Location characteristic "
                            + dto.locationCharacteristicId
                            + " not found for location characteristic value",
                    dto);
        }
        return caracteristicaLocation;

    }

    private void validaPrimaryKeyESupportData(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicValueIntegrationDataDto.LocationCharacteristicValuePrimaryKeyIntegrationDTO dto,
            LocationCharacteristicValueIntegrationSupportData supportData) {

        if (dto == null || dto.locationId == null || dto.locationId.isBlank()) {
            throw new IllegalArgumentException("Location id is required for location characteristic value.");
        }
        if (dto.locationCharacteristicId == null || dto.locationCharacteristicId.isBlank()) {
            throw new IllegalArgumentException("Location characteristic id is required for location characteristic value.");
        }
        if (supportData == null
                || supportData.locationPorId == null
                || supportData.locationCharacteristicById == null) {
            throw new IllegalStateException(
                    "Location characteristic value support data requires locations and location characteristics.");
        }

    }

}
