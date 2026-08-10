package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.MissingDependencyDataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.ValorCaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationSupportData;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper Enterprise da carga de valores de caracteristica por material.
 */
@Component
public class MaterialCharacteristicValueIntegrationMapper implements IntegrationMapperInterface<
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto,
        com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO,
        ValorCaracteristicaProduto,
        MaterialCharacteristicValueIntegrationSupportData> {

    /**
     * Headers do arquivo Enterprise de valores de caracteristica por material.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Material Id",
            "Material Characteristic Id",
            "Characteristic Value");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            ValorCaracteristicaProduto entity) {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.builder()
                .characteristicValue(entity.getAtributo())
                .build();

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            ValorCaracteristicaProduto entity) {

        return new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO(
                entity.getProduto().getId(),
                entity.getCaracteristicaProduto().getId());

    }

    @Override
    public ValorCaracteristicaProduto createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO dto,
            MaterialCharacteristicValueIntegrationSupportData supportData) {

        Produto material = getMaterial(dto, supportData);
        CaracteristicaProduto caracteristicaProduto = getCaracteristicaProduto(dto, supportData);

        ValorCaracteristicaProduto valorCaracteristicaProduto =
                new ValorCaracteristicaProduto();
        valorCaracteristicaProduto.setValorCaracteristicaProdutoCompositeKey(
                new ValorCaracteristicaProduto.ValorCaracteristicaProdutoCompositeKey(
                        material,
                        caracteristicaProduto));
        return valorCaracteristicaProduto;

    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            ValorCaracteristicaProduto entity,
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto dto,
            MaterialCharacteristicValueIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        entity.setAtributo(
                com.opsfactor.community.capability.masterdata.classification.characteristic.integration.CharacteristicValueIntegrationValidation.getNormalizedAttributeValue(
                        dto.characteristicValue,
                        entity.getCaracteristicaProduto()));

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            ValorCaracteristicaProduto entity,
            MaterialCharacteristicValueIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        linhaArquivo.addContent(entity.getProduto().getId());
        linhaArquivo.addContent(entity.getCaracteristicaProduto().getId());
        linhaArquivo.addContent(entity.getAtributo());
        return linhaArquivo;

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            MaterialCharacteristicValueIntegrationSupportData supportData) {

        return com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.builder()
                .characteristicValue(processedFileRow.getColumnValueAsString(2))
                .build();

    }

    @Override
    public com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            MaterialCharacteristicValueIntegrationSupportData supportData) {

        return new com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1));

    }

    private Produto getMaterial(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO dto,
            MaterialCharacteristicValueIntegrationSupportData supportData) {

        validaPrimaryKeyESupportData(dto, supportData);

        Produto material = supportData.materialPorId.get(dto.materialId);
        if (material == null) {
            throw new MissingDependencyDataUploadException(
                    "Material " + dto.materialId + " not found for material characteristic value",
                    dto);
        }
        return material;

    }

    private CaracteristicaProduto getCaracteristicaProduto(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO dto,
            MaterialCharacteristicValueIntegrationSupportData supportData) {

        validaPrimaryKeyESupportData(dto, supportData);

        CaracteristicaProduto caracteristicaProduto =
                supportData.materialCharacteristicById.get(dto.materialCharacteristicId);
        if (caracteristicaProduto == null) {
            throw new MissingDependencyDataUploadException(
                    "Material characteristic "
                            + dto.materialCharacteristicId
                            + " not found for material characteristic value",
                    dto);
        }
        return caracteristicaProduto;

    }

    private void validaPrimaryKeyESupportData(
            com.opsfactor.community.capability.masterdata.classification.characteristic.integration.MaterialCharacteristicValueIntegrationDataDto.MaterialCharacteristicValuePrimaryKeyIntegrationDTO dto,
            MaterialCharacteristicValueIntegrationSupportData supportData) {

        if (dto == null || dto.materialId == null || dto.materialId.isBlank()) {
            throw new IllegalArgumentException("Material id is required for material characteristic value.");
        }
        if (dto.materialCharacteristicId == null || dto.materialCharacteristicId.isBlank()) {
            throw new IllegalArgumentException("Material characteristic id is required for material characteristic value.");
        }
        if (supportData == null
                || supportData.materialPorId == null
                || supportData.materialCharacteristicById == null) {
            throw new IllegalStateException(
                    "Material characteristic value support data requires materials and material characteristics.");
        }

    }

}
