package com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.mapper;

import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.dto.InventoryPlanIntegrationDataDto;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper Community do export read-only de Inventory Plan simples.
 */
@Component
public class InventoryPlanIntegrationMapper implements IntegrationMapperInterface<
        InventoryPlanIntegrationDataDto,
        InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO,
        InventoryPlanIntegrationDataDto,
        InventoryPlanIntegrationSupportData> {

    /**
     * Headers legados do arquivo de Inventory Plan simples.
     *
     * <p>O arquivo legado e filtrado por {@code supplyPlanId} no path, por isso
     * a coluna de Supply Plan nao aparece no CSV/XLSX. O JSON ainda carrega o
     * id na chave do DTO para manter rastreabilidade da linha.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
            "Location Id",
            "Product Id",
            "Reference Date (represents the end of each period : month/week/day)",
            "UOM - Unit of Measure Id",
            "Safety Stock Quantity",
            "Maximum Stock Quantity",
            "Projected Stock - Working Version",
            "Projected Stock - Unconstrained Version",
            "Projected Stock - Constrained Version",
            "Default SNP UOM",
            "Projected Stock in SNP UOM - Working Version",
            "Projected Stock in SNP UOM - Unconstrained Version",
            "Projected Stock in SNP UOM - Constrained Version");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public InventoryPlanIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            InventoryPlanIntegrationDataDto entity) {

        return InventoryPlanIntegrationDataDto.builder()
                .unitOfMeasureId(entity.unitOfMeasureId)
                .safetyStockQuantity(entity.safetyStockQuantity)
                .maximumStockQuantity(entity.maximumStockQuantity)
                .projectedStockWorkingVersion(entity.projectedStockWorkingVersion)
                .projectedStockUnconstrainedVersion(entity.projectedStockUnconstrainedVersion)
                .projectedStockConstrainedVersion(entity.projectedStockConstrainedVersion)
                .defaultSnpUnitOfMeasureId(entity.defaultSnpUnitOfMeasureId)
                .projectedStockWorkingVersionDefaultSnpUom(entity.projectedStockWorkingVersionDefaultSnpUom)
                .projectedStockUnconstrainedVersionDefaultSnpUom(entity.projectedStockUnconstrainedVersionDefaultSnpUom)
                .projectedStockConstrainedVersionDefaultSnpUom(entity.projectedStockConstrainedVersionDefaultSnpUom)
                .build();

    }

    @Override
    public InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            InventoryPlanIntegrationDataDto entity) {

        return entity.primaryKeyDto;

    }

    @Override
    public InventoryPlanIntegrationDataDto createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO primaryKeyDto,
            InventoryPlanIntegrationSupportData supportData) {

        return InventoryPlanIntegrationDataDto.builder()
                .primaryKeyDto(primaryKeyDto)
                .build();

    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            InventoryPlanIntegrationDataDto entity,
            InventoryPlanIntegrationDataDto dto,
            InventoryPlanIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        throw new UnsupportedOperationException(
                "Inventory Plan is read-only in Community Data Upload; quantities are persisted by Supply Planning execution.");

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            InventoryPlanIntegrationDataDto entity,
            InventoryPlanIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO primaryKeyDto =
                entity.primaryKeyDto;

        linhaArquivo.addContent(primaryKeyDto.locationId);
        linhaArquivo.addContent(primaryKeyDto.materialId);
        linhaArquivo.addContent(primaryKeyDto.referenceDate);
        linhaArquivo.addContent(entity.unitOfMeasureId);
        linhaArquivo.addContent(entity.safetyStockQuantity);
        linhaArquivo.addContent(entity.maximumStockQuantity);
        linhaArquivo.addContent(entity.projectedStockWorkingVersion);
        linhaArquivo.addContent(entity.projectedStockUnconstrainedVersion);
        linhaArquivo.addContent(entity.projectedStockConstrainedVersion);
        linhaArquivo.addContent(entity.defaultSnpUnitOfMeasureId);
        linhaArquivo.addContent(entity.projectedStockWorkingVersionDefaultSnpUom);
        linhaArquivo.addContent(entity.projectedStockUnconstrainedVersionDefaultSnpUom);
        linhaArquivo.addContent(entity.projectedStockConstrainedVersionDefaultSnpUom);
        return linhaArquivo;

    }

    @Override
    public InventoryPlanIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            InventoryPlanIntegrationSupportData supportData) {

        return InventoryPlanIntegrationDataDto.builder()
                .unitOfMeasureId(processedFileRow.getColumnValueAsString(3))
                .safetyStockQuantity(
                        InventoryPlanIntegrationValidation.validaNumeroNaoNegativoOuNulo(
                                processedFileRow.getColumnValueAsDouble(4),
                                "safety stock quantity"))
                .maximumStockQuantity(
                        InventoryPlanIntegrationValidation.validaNumeroNaoNegativoOuNulo(
                                processedFileRow.getColumnValueAsDouble(5),
                                "maximum stock quantity"))
                .projectedStockWorkingVersion(
                        InventoryPlanIntegrationValidation.validaNumeroFinitoOuNulo(
                                processedFileRow.getColumnValueAsDouble(6),
                                "projected stock working version"))
                .projectedStockUnconstrainedVersion(
                        InventoryPlanIntegrationValidation.validaNumeroFinitoOuNulo(
                                processedFileRow.getColumnValueAsDouble(7),
                                "projected stock unconstrained version"))
                .projectedStockConstrainedVersion(
                        InventoryPlanIntegrationValidation.validaNumeroFinitoOuNulo(
                                processedFileRow.getColumnValueAsDouble(8),
                                "projected stock constrained version"))
                .defaultSnpUnitOfMeasureId(processedFileRow.getColumnValueAsString(9))
                .projectedStockWorkingVersionDefaultSnpUom(processedFileRow.getColumnValue(10))
                .projectedStockUnconstrainedVersionDefaultSnpUom(processedFileRow.getColumnValue(11))
                .projectedStockConstrainedVersionDefaultSnpUom(processedFileRow.getColumnValue(12))
                .build();

    }

    @Override
    public InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            InventoryPlanIntegrationSupportData supportData) {

        return new InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO(
                null,
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1),
                processedFileRow.getColumnValueAsLocalDateTime(2));

    }

}
