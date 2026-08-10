package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper compartilhado da carga de caracteristicas de location.
 */
@Component
public class LocationCharacteristicIntegrationMapper implements IntegrationMapperInterface<
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO,
        CaracteristicaLocation,
        LocationCharacteristicIntegrationSupportData> {

    /**
     * Headers do arquivo Enterprise de caracteristicas de location.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Location Characteristic Id",
            "Description",
            "Type (BINARY / NUMERICAL / CATEGORICAL)");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            CaracteristicaLocation entity) {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto.builder()
                .description(entity.getDescricao())
                .characteristicType(entity.getTipoCaracteristica())
                .build();

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            CaracteristicaLocation entity) {

        return new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO(
                entity.getId());

    }

    @Override
    public CaracteristicaLocation createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO dto,
            LocationCharacteristicIntegrationSupportData supportData) {

        validaLocationCharacteristicId(dto, supportData);

        CaracteristicaLocation caracteristicaLocation = new CaracteristicaLocation();
        caracteristicaLocation.setId(dto.locationCharacteristicId);
        return caracteristicaLocation;

    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            CaracteristicaLocation entity,
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto dto,
            LocationCharacteristicIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        validaLocationCharacteristicId(dto.primaryKeyDto, supportData);
        validaCamposObrigatorios(dto);

        entity.setDescricao(dto.description);
        entity.setTipoCaracteristica(dto.characteristicType);

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            CaracteristicaLocation entity,
            LocationCharacteristicIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getDescricao());
        linhaArquivo.addContent(getPublicTypeValue(entity.getTipoCaracteristica()));
        return linhaArquivo;

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            LocationCharacteristicIntegrationSupportData supportData) {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto.builder()
                .description(processedFileRow.getColumnValueAsString(1))
                .characteristicType(getCharacteristicType(processedFileRow.getColumnValueAsString(2)))
                .build();

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            LocationCharacteristicIntegrationSupportData supportData) {

        return new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));

    }

    private void validaLocationCharacteristicId(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto.LocationCharacteristicPrimaryKeyIntegrationDTO dto,
            LocationCharacteristicIntegrationSupportData supportData) {

        if (dto == null
                || dto.locationCharacteristicId == null
                || dto.locationCharacteristicId.isBlank()) {
            throw new IllegalArgumentException("Location characteristic id is required.");
        }
        if (supportData == null
                || supportData.materialCharacteristicIdSet == null
                || supportData.materialLocationCharacteristicIdSet == null) {
            throw new IllegalStateException(
                    "Location characteristic support data requires material and material-location characteristic ids.");
        }
        if (supportData.materialCharacteristicIdSet.contains(dto.locationCharacteristicId)) {
            throw new IllegalArgumentException(
                    "There already is a material characteristic with id "
                            + dto.locationCharacteristicId
                            + ".");
        }
        if (supportData.materialLocationCharacteristicIdSet.contains(dto.locationCharacteristicId)) {
            throw new IllegalArgumentException(
                    "There already is a material-location characteristic with id "
                            + dto.locationCharacteristicId
                            + ".");
        }

    }

    private void validaCamposObrigatorios(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.LocationCharacteristicIntegrationDataDto dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Location characteristic DTO is required.");
        }
        if (dto.description == null || dto.description.isBlank()) {
            throw new IllegalArgumentException("Location characteristic description is required.");
        }
        if (dto.characteristicType == null) {
            throw new IllegalArgumentException("Location characteristic type is required.");
        }

    }

    /**
     * Converte o tipo publico opcional do arquivo para o enum interno.
     *
     * Valor nulo ou em branco e preservado como `null` para que a validacao
     * obrigatoria do mapper produza o erro funcional no ponto correto.
     */
    @Nullable
    private Caracteristica.TipoCaracteristica getCharacteristicType(
            @Nullable String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toUpperCase()) {
            case "BINARY" -> Caracteristica.TipoCaracteristica.BINARIO;
            case "NUMERICAL" -> Caracteristica.TipoCaracteristica.NUMERICO;
            case "CATEGORICAL" -> Caracteristica.TipoCaracteristica.CATEGORICO;
            default -> MetodosUtilidade.getValorEnumDeJsonProperty(
                    Caracteristica.TipoCaracteristica.class,
                    value);
        };

    }

    /**
     * Converte o enum interno para o vocabulario publico legado do arquivo.
     *
     * Entidade com tipo ausente exporta ausencia literal, sem inventar fallback
     * silencioso para dado mestre incompleto.
     */
    @Nullable
    private String getPublicTypeValue(
            @Nullable Caracteristica.TipoCaracteristica tipoCaracteristica) {

        if (tipoCaracteristica == null) {
            return null;
        }

        return switch (tipoCaracteristica) {
            case BINARIO -> "BINARY";
            case NUMERICO -> "NUMERICAL";
            case CATEGORICO -> "CATEGORICAL";
        };

    }

}
