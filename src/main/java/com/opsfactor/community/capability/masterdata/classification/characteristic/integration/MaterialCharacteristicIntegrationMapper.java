package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper compartilhado da carga de caracteristicas de material.
 *
 * <p>O arquivo preserva o vocabulario legado em ingles para o tipo da
 * caracteristica (`BINARY`, `NUMERICAL`, `CATEGORICAL`). Internamente a
 * entidade ainda usa os nomes historicos em portugues; a conversao fica aqui,
 * na borda da integracao, para nao contaminar services consumidores.</p>
 */
@Component
public class MaterialCharacteristicIntegrationMapper implements IntegrationMapperInterface<
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO,
        CaracteristicaProduto,
        MaterialCharacteristicIntegrationSupportData> {

    /**
     * Headers do arquivo de caracteristicas de material.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Material Characteristic Id",
            "Description",
            "Type (BINARY / NUMERICAL / CATEGORICAL)");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            CaracteristicaProduto entity) {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto.builder()
                .description(entity.getDescricao())
                .characteristicType(entity.getTipoCaracteristica())
                .build();

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            CaracteristicaProduto entity) {

        return new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO(
                entity.getId());

    }

    @Override
    public CaracteristicaProduto createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO dto,
            MaterialCharacteristicIntegrationSupportData supportData) {

        validaMaterialCharacteristicId(dto, supportData);

        CaracteristicaProduto caracteristicaProduto = new CaracteristicaProduto();
        caracteristicaProduto.setId(dto.materialCharacteristicId);
        return caracteristicaProduto;

    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            CaracteristicaProduto entity,
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto dto,
            MaterialCharacteristicIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        validaMaterialCharacteristicId(dto.primaryKeyDto, supportData);
        validaCamposObrigatorios(dto);

        entity.setDescricao(dto.description);
        entity.setTipoCaracteristica(dto.characteristicType);

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            CaracteristicaProduto entity,
            MaterialCharacteristicIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getId());
        linhaArquivo.addContent(entity.getDescricao());
        linhaArquivo.addContent(getPublicTypeValue(entity.getTipoCaracteristica()));
        return linhaArquivo;

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            MaterialCharacteristicIntegrationSupportData supportData) {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto.builder()
                .description(processedFileRow.getColumnValueAsString(1))
                .characteristicType(getCharacteristicType(processedFileRow.getColumnValueAsString(2)))
                .build();

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            MaterialCharacteristicIntegrationSupportData supportData) {

        return new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0));

    }

    private void validaMaterialCharacteristicId(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto.MaterialCharacteristicPrimaryKeyIntegrationDTO dto,
            MaterialCharacteristicIntegrationSupportData supportData) {

        if (dto == null
                || dto.materialCharacteristicId == null
                || dto.materialCharacteristicId.isBlank()) {
            throw new IllegalArgumentException("Material characteristic id is required.");
        }
        if (supportData == null
                || supportData.locationCharacteristicIdSet == null
                || supportData.materialLocationCharacteristicIdSet == null) {
            throw new IllegalStateException(
                    "Material characteristic support data requires location and material-location characteristic ids.");
        }
        if (supportData.locationCharacteristicIdSet.contains(dto.materialCharacteristicId)) {
            throw new IllegalArgumentException(
                    "There already is a location characteristic with id "
                            + dto.materialCharacteristicId
                            + ".");
        }
        if (supportData.materialLocationCharacteristicIdSet.contains(dto.materialCharacteristicId)) {
            throw new IllegalArgumentException(
                    "There already is a material-location characteristic with id "
                            + dto.materialCharacteristicId
                            + ".");
        }

    }

    private void validaCamposObrigatorios(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicIntegrationDataDto dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Material characteristic DTO is required.");
        }
        if (dto.description == null || dto.description.isBlank()) {
            throw new IllegalArgumentException("Material characteristic description is required.");
        }
        if (dto.characteristicType == null) {
            throw new IllegalArgumentException("Material characteristic type is required.");
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
