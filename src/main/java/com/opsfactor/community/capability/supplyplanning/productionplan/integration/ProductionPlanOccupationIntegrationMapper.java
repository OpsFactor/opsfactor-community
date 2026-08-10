package com.opsfactor.community.capability.supplyplanning.productionplan.integration;

import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationSupportData;
import com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationValidation;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper Enterprise da exportacao read-only de Production Plan Occupation.
 */
@Component
public class ProductionPlanOccupationIntegrationMapper implements IntegrationMapperInterface<
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto,
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO,
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto,
        ProductionPlanOccupationIntegrationSupportData> {

    /**
     * Headers publicos legados do arquivo de Production Plan Occupation.
     */
    public static final List<String> processedFileHeaders = List.of(
            "Supply Plan Id",
            "Location Id",
            "Output Material Id",
            "Planned Date",
            "Production Version Id",
            "Routing Id",
            "Bill of Materials",
            "Resource Id",
            "Routing Cluster",
            "Resource Capacity Unit of Measure",
            "Resource Capacity Period",
            "Unit of Measure Id",
            "Unconstrained Planned Order Quantity",
            "Unconstrained Firm Order Quantity",
            "Unconstrained Total Resource Consumption",
            "Constrained Planned Order Quantity",
            "Constrained Firm Order Quantity",
            "Constrained Total Resource Consumption",
            "Default SNP Unit of Measure",
            "Unconstrained Planned Order Quantity in default SNP unit",
            "Unconstrained Firm Order Quantity in default SNP unit",
            "Constrained Planned Order Quantity in default SNP unit",
            "Constrained Firm Order Quantity in default SNP unit",
            "Set-up time (hours)",
            "Set-up sequence for current period");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto entity) {

        return com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.builder()
                .routingClusterId(entity.routingClusterId)
                .resourceCapacityUnitOfMeasureId(entity.resourceCapacityUnitOfMeasureId)
                .resourceCapacityPeriod(entity.resourceCapacityPeriod)
                .unitOfMeasureId(entity.unitOfMeasureId)
                .unconstrainedPlannedOrderQuantity(entity.unconstrainedPlannedOrderQuantity)
                .unconstrainedFirmOrderQuantity(entity.unconstrainedFirmOrderQuantity)
                .unconstrainedTotalResourceConsumption(entity.unconstrainedTotalResourceConsumption)
                .constrainedPlannedOrderQuantity(entity.constrainedPlannedOrderQuantity)
                .constrainedFirmOrderQuantity(entity.constrainedFirmOrderQuantity)
                .constrainedTotalResourceConsumption(entity.constrainedTotalResourceConsumption)
                .defaultSnpUnitOfMeasureId(entity.defaultSnpUnitOfMeasureId)
                .unconstrainedPlannedOrderQuantityDefaultSnpUom(entity.unconstrainedPlannedOrderQuantityDefaultSnpUom)
                .unconstrainedFirmOrderQuantityDefaultSnpUom(entity.unconstrainedFirmOrderQuantityDefaultSnpUom)
                .constrainedPlannedOrderQuantityDefaultSnpUom(entity.constrainedPlannedOrderQuantityDefaultSnpUom)
                .constrainedFirmOrderQuantityDefaultSnpUom(entity.constrainedFirmOrderQuantityDefaultSnpUom)
                .setupTimeHours(entity.setupTimeHours)
                .setupSequence(entity.setupSequence)
                .build();

    }

    @Override
    public com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto entity) {

        return entity.primaryKeyDto;

    }

    @Override
    public com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO primaryKeyDto,
            ProductionPlanOccupationIntegrationSupportData supportData) {

        return com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.builder()
                .primaryKeyDto(primaryKeyDto)
                .build();

    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto entity,
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto dto,
            ProductionPlanOccupationIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        throw new UnsupportedOperationException(
                "Production Plan Occupation is read-only in Data Upload; quantities and setup are persisted by Supply Planning execution.");

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto entity,
            ProductionPlanOccupationIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO primaryKeyDto =
                entity.primaryKeyDto;

        linhaArquivo.addContent(primaryKeyDto.supplyPlanId);
        linhaArquivo.addContent(primaryKeyDto.locationId);
        linhaArquivo.addContent(primaryKeyDto.outputMaterialId);
        linhaArquivo.addContent(primaryKeyDto.plannedDate);
        linhaArquivo.addContent(primaryKeyDto.productionVersionId);
        linhaArquivo.addContent(primaryKeyDto.routingId);
        linhaArquivo.addContent(primaryKeyDto.billOfMaterialsId);
        linhaArquivo.addContent(primaryKeyDto.resourceId);
        linhaArquivo.addContent(entity.routingClusterId);
        linhaArquivo.addContent(entity.resourceCapacityUnitOfMeasureId);
        linhaArquivo.addContent(entity.resourceCapacityPeriod);
        linhaArquivo.addContent(entity.unitOfMeasureId);
        linhaArquivo.addContent(entity.unconstrainedPlannedOrderQuantity);
        linhaArquivo.addContent(entity.unconstrainedFirmOrderQuantity);
        linhaArquivo.addContent(entity.unconstrainedTotalResourceConsumption);
        linhaArquivo.addContent(entity.constrainedPlannedOrderQuantity);
        linhaArquivo.addContent(entity.constrainedFirmOrderQuantity);
        linhaArquivo.addContent(entity.constrainedTotalResourceConsumption);
        linhaArquivo.addContent(entity.defaultSnpUnitOfMeasureId);
        linhaArquivo.addContent(entity.unconstrainedPlannedOrderQuantityDefaultSnpUom);
        linhaArquivo.addContent(entity.unconstrainedFirmOrderQuantityDefaultSnpUom);
        linhaArquivo.addContent(entity.constrainedPlannedOrderQuantityDefaultSnpUom);
        linhaArquivo.addContent(entity.constrainedFirmOrderQuantityDefaultSnpUom);
        linhaArquivo.addContent(entity.setupTimeHours);
        linhaArquivo.addContent(entity.setupSequence);
        return linhaArquivo;

    }

    @Override
    public com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            ProductionPlanOccupationIntegrationSupportData supportData) {

        return com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.builder()
                .routingClusterId(processedFileRow.getColumnValueAsString(8))
                .resourceCapacityUnitOfMeasureId(processedFileRow.getColumnValueAsString(9))
                .resourceCapacityPeriod(processedFileRow.getColumnValue(10))
                .unitOfMeasureId(processedFileRow.getColumnValueAsString(11))
                .unconstrainedPlannedOrderQuantity(
                        ProductionPlanOccupationIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(12),
                                "unconstrained planned order quantity"))
                .unconstrainedFirmOrderQuantity(
                        ProductionPlanOccupationIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(13),
                                "unconstrained firm order quantity"))
                .unconstrainedTotalResourceConsumption(
                        ProductionPlanOccupationIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(14),
                                "unconstrained total resource consumption"))
                .constrainedPlannedOrderQuantity(
                        ProductionPlanOccupationIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(15),
                                "constrained planned order quantity"))
                .constrainedFirmOrderQuantity(
                        ProductionPlanOccupationIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(16),
                                "constrained firm order quantity"))
                .constrainedTotalResourceConsumption(
                        ProductionPlanOccupationIntegrationValidation.validaQuantidadeNaoNegativaOuNula(
                                processedFileRow.getColumnValueAsDouble(17),
                                "constrained total resource consumption"))
                .defaultSnpUnitOfMeasureId(processedFileRow.getColumnValueAsString(18))
                .unconstrainedPlannedOrderQuantityDefaultSnpUom(processedFileRow.getColumnValue(19))
                .unconstrainedFirmOrderQuantityDefaultSnpUom(processedFileRow.getColumnValue(20))
                .constrainedPlannedOrderQuantityDefaultSnpUom(processedFileRow.getColumnValue(21))
                .constrainedFirmOrderQuantityDefaultSnpUom(processedFileRow.getColumnValue(22))
                .setupTimeHours(processedFileRow.getColumnValue(23))
                .setupSequence(processedFileRow.getColumnValue(24))
                .build();

    }

    @Override
    public com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            ProductionPlanOccupationIntegrationSupportData supportData) {

        return new com.opsfactor.community.capability.supplyplanning.productionplan.integration.ProductionPlanOccupationIntegrationDataDto.ProductionPlanOccupationPrimaryKeyIntegrationDTO(
                processedFileRow.getColumnValueAsLong(0),
                processedFileRow.getColumnValueAsString(1),
                processedFileRow.getColumnValueAsString(2),
                processedFileRow.getColumnValueAsLocalDateTime(3),
                processedFileRow.getColumnValueAsString(4),
                processedFileRow.getColumnValueAsString(5),
                processedFileRow.getColumnValueAsString(6),
                processedFileRow.getColumnValueAsString(7));

    }

}
