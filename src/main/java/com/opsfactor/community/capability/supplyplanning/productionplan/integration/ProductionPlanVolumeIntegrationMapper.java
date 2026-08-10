package com.opsfactor.community.capability.supplyplanning.productionplan.integration;

import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper Enterprise da exportacao read-only de Production Plan Volume.
 */
@Component
public class ProductionPlanVolumeIntegrationMapper implements IntegrationMapperInterface<
        ProductionPlanVolumeIntegrationDataDto,
        ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO,
        ProductionPlanVolumeIntegrationDataDto,
        ProductionPlanVolumeIntegrationSupportData> {

    /**
     * Headers publicos legados do arquivo de Production Plan Volume.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Supply Plan Id",
            "Location Id",
            "Output Material Id",
            "Planned Date",
            "Production Version Id",
            "Routing Id",
            "Bill of Materials",
            "Unit of Measure Id",
            "Unconstrained Planned Order Quantity",
            "Unconstrained Firm Order Quantity",
            "Constrained Planned Order Quantity",
            "Constrained Firm Order Quantity",
            "Default SNP Unit of Measure",
            "Unconstrained Total Planned Order Quantity in default SNP unit",
            "Unconstrained Total Firm Order Quantity in default SNP unit",
            "Constrained Total Planned Order Quantity in default SNP unit",
            "Constrained Total Firm Order Quantity in default SNP unit");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public ProductionPlanVolumeIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            ProductionPlanVolumeIntegrationDataDto entity) {

        return ProductionPlanVolumeIntegrationDataDto.builder()
                .unitOfMeasureId(entity.unitOfMeasureId)
                .unconstrainedPlannedOrderQuantity(entity.unconstrainedPlannedOrderQuantity)
                .unconstrainedFirmOrderQuantity(entity.unconstrainedFirmOrderQuantity)
                .constrainedPlannedOrderQuantity(entity.constrainedPlannedOrderQuantity)
                .constrainedFirmOrderQuantity(entity.constrainedFirmOrderQuantity)
                .defaultSnpUnitOfMeasureId(entity.defaultSnpUnitOfMeasureId)
                .unconstrainedPlannedOrderQuantityDefaultSnpUom(entity.unconstrainedPlannedOrderQuantityDefaultSnpUom)
                .unconstrainedFirmOrderQuantityDefaultSnpUom(entity.unconstrainedFirmOrderQuantityDefaultSnpUom)
                .constrainedPlannedOrderQuantityDefaultSnpUom(entity.constrainedPlannedOrderQuantityDefaultSnpUom)
                .constrainedFirmOrderQuantityDefaultSnpUom(entity.constrainedFirmOrderQuantityDefaultSnpUom)
                .build();

    }

    @Override
    public ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            ProductionPlanVolumeIntegrationDataDto entity) {

        return entity.primaryKeyDto;

    }

    @Override
    public ProductionPlanVolumeIntegrationDataDto createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO primaryKeyDto,
            ProductionPlanVolumeIntegrationSupportData supportData) {

        return ProductionPlanVolumeIntegrationDataDto.builder()
                .primaryKeyDto(primaryKeyDto)
                .build();

    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            ProductionPlanVolumeIntegrationDataDto entity,
            ProductionPlanVolumeIntegrationDataDto dto,
            ProductionPlanVolumeIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        throw new UnsupportedOperationException(
                "Production Plan Volume is read-only in Data Upload; quantities are persisted by Supply Planning execution.");

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            ProductionPlanVolumeIntegrationDataDto entity,
            ProductionPlanVolumeIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO primaryKeyDto =
                entity.primaryKeyDto;

        linhaArquivo.addContent(primaryKeyDto.supplyPlanId);
        linhaArquivo.addContent(primaryKeyDto.locationId);
        linhaArquivo.addContent(primaryKeyDto.outputMaterialId);
        linhaArquivo.addContent(primaryKeyDto.plannedDate);
        linhaArquivo.addContent(primaryKeyDto.productionVersionId);
        linhaArquivo.addContent(primaryKeyDto.routingId);
        linhaArquivo.addContent(primaryKeyDto.billOfMaterialsId);
        linhaArquivo.addContent(entity.unitOfMeasureId);
        linhaArquivo.addContent(entity.unconstrainedPlannedOrderQuantity);
        linhaArquivo.addContent(entity.unconstrainedFirmOrderQuantity);
        linhaArquivo.addContent(entity.constrainedPlannedOrderQuantity);
        linhaArquivo.addContent(entity.constrainedFirmOrderQuantity);
        linhaArquivo.addContent(entity.defaultSnpUnitOfMeasureId);
        linhaArquivo.addContent(entity.unconstrainedPlannedOrderQuantityDefaultSnpUom);
        linhaArquivo.addContent(entity.unconstrainedFirmOrderQuantityDefaultSnpUom);
        linhaArquivo.addContent(entity.constrainedPlannedOrderQuantityDefaultSnpUom);
        linhaArquivo.addContent(entity.constrainedFirmOrderQuantityDefaultSnpUom);
        return linhaArquivo;

    }

    @Override
    public ProductionPlanVolumeIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            ProductionPlanVolumeIntegrationSupportData supportData) {

        return ProductionPlanVolumeIntegrationDataDto.builder()
                .unitOfMeasureId(processedFileRow.getColumnValueAsString(7))
                .unconstrainedPlannedOrderQuantity(
                        ProductionPlanVolumeIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(8),
                                "unconstrained planned order quantity"))
                .unconstrainedFirmOrderQuantity(
                        ProductionPlanVolumeIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(9),
                                "unconstrained firm order quantity"))
                .constrainedPlannedOrderQuantity(
                        ProductionPlanVolumeIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(10),
                                "constrained planned order quantity"))
                .constrainedFirmOrderQuantity(
                        ProductionPlanVolumeIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(11),
                                "constrained firm order quantity"))
                .defaultSnpUnitOfMeasureId(processedFileRow.getColumnValueAsString(12))
                .unconstrainedPlannedOrderQuantityDefaultSnpUom(processedFileRow.getColumnValue(13))
                .unconstrainedFirmOrderQuantityDefaultSnpUom(processedFileRow.getColumnValue(14))
                .constrainedPlannedOrderQuantityDefaultSnpUom(processedFileRow.getColumnValue(15))
                .constrainedFirmOrderQuantityDefaultSnpUom(processedFileRow.getColumnValue(16))
                .build();

    }

    @Override
    public ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            ProductionPlanVolumeIntegrationSupportData supportData) {

        return new ProductionPlanVolumeIntegrationDataDto.ProductionPlanVolumePrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsLong(0),
                processedFileRow.getColumnValueAsString(1),
                processedFileRow.getColumnValueAsString(2),
                processedFileRow.getColumnValueAsLocalDateTime(3),
                processedFileRow.getColumnValueAsString(4),
                processedFileRow.getColumnValueAsString(5),
                processedFileRow.getColumnValueAsString(6));

    }

}
