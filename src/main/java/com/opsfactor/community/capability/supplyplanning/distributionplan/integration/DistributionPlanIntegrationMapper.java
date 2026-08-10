package com.opsfactor.community.capability.supplyplanning.distributionplan.integration;

import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper compartilhado da exportacao read-only de Distribution Plan.
 */
@Component
public class DistributionPlanIntegrationMapper implements IntegrationMapperInterface<
        DistributionPlanIntegrationDataDto,
        DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO,
        DistributionPlanIntegrationDataDto,
        DistributionPlanIntegrationSupportData> {

    /**
     * Headers legados do arquivo `distributionplan`.
     *
     * <p>O arquivo legado e filtrado por {@code supplyPlanId} no path, por isso
     * a coluna de Supply Plan nao aparece no CSV/XLSX. O JSON ainda carrega o
     * id na chave do DTO.</p>
     */
    public static final List<String> processedFileHeaders = List.of(
            "Origin Location Id",
            "Destination Location Id",
            "Product Id",
            "Planned Delivery Date (represents the end of each period : month/week/day)",
            "Suggested Order Emission Date",
            "Unit of Measure Id",
            "Unconstrained Planned Order Quantity",
            "Unconstrained Firm Order Quantity",
            "Constrained Planned Order Quantity",
            "Constrained Firm Order Quantity");

    @Override
    public List<String> getProcessedFileHeaders() {

        return processedFileHeaders;

    }

    @Override
    public DistributionPlanIntegrationDataDto getDtoWithoutPrimaryKeyFromEntity(
            DistributionPlanIntegrationDataDto entity) {

        return DistributionPlanIntegrationDataDto.builder()
                .unitOfMeasureId(entity.unitOfMeasureId)
                .unconstrainedPlannedOrderQuantity(entity.unconstrainedPlannedOrderQuantity)
                .unconstrainedFirmOrderQuantity(entity.unconstrainedFirmOrderQuantity)
                .constrainedPlannedOrderQuantity(entity.constrainedPlannedOrderQuantity)
                .constrainedFirmOrderQuantity(entity.constrainedFirmOrderQuantity)
                .build();

    }

    @Override
    public DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromEntity(
            DistributionPlanIntegrationDataDto entity) {

        return entity.primaryKeyDto;

    }

    @Override
    public DistributionPlanIntegrationDataDto createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
            DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO primaryKeyDto,
            DistributionPlanIntegrationSupportData supportData) {

        return DistributionPlanIntegrationDataDto.builder()
                .primaryKeyDto(primaryKeyDto)
                .build();

    }

    @Override
    public void updateEntityNonPrimaryFieldsFromDTO(
            DistributionPlanIntegrationDataDto entity,
            DistributionPlanIntegrationDataDto dto,
            DistributionPlanIntegrationSupportData supportData,
            @Nullable Map<String, MetodoAtualizacaoCampo> camposASobrecrever) {

        throw new UnsupportedOperationException(
                "Distribution Plan is read-only in Enterprise Data Upload; quantities are persisted by Supply Planning execution.");

    }

    @Override
    public ProcessedFileRow convertEntityToProcessedFileRow(
            DistributionPlanIntegrationDataDto entity,
            DistributionPlanIntegrationSupportData supportData) {

        ProcessedFileRow linhaArquivo = new ProcessedFileRow();
        DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO primaryKeyDto =
                entity.primaryKeyDto;

        linhaArquivo.addContent(primaryKeyDto.originLocationId);
        linhaArquivo.addContent(primaryKeyDto.destinationLocationId);
        linhaArquivo.addContent(primaryKeyDto.materialId);
        linhaArquivo.addContent(primaryKeyDto.plannedDeliveryDate);
        linhaArquivo.addContent(primaryKeyDto.suggestedOrderEmissionDate);
        linhaArquivo.addContent(entity.unitOfMeasureId);
        linhaArquivo.addContent(entity.unconstrainedPlannedOrderQuantity);
        linhaArquivo.addContent(entity.unconstrainedFirmOrderQuantity);
        linhaArquivo.addContent(entity.constrainedPlannedOrderQuantity);
        linhaArquivo.addContent(entity.constrainedFirmOrderQuantity);
        return linhaArquivo;

    }

    @Override
    public DistributionPlanIntegrationDataDto getDtoWithoutPrimaryKeyFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            DistributionPlanIntegrationSupportData supportData) {

        return DistributionPlanIntegrationDataDto.builder()
                .unitOfMeasureId(processedFileRow.getColumnValueAsString(5))
                .unconstrainedPlannedOrderQuantity(
                        DistributionPlanIntegrationValidation.validaNumeroNaoNegativoOuNulo(
                                processedFileRow.getColumnValueAsDouble(6),
                                "unconstrained planned order quantity"))
                .unconstrainedFirmOrderQuantity(
                        DistributionPlanIntegrationValidation.validaNumeroNaoNegativoOuNulo(
                                processedFileRow.getColumnValueAsDouble(7),
                                "unconstrained firm order quantity"))
                .constrainedPlannedOrderQuantity(
                        DistributionPlanIntegrationValidation.validaNumeroNaoNegativoOuNulo(
                                processedFileRow.getColumnValueAsDouble(8),
                                "constrained planned order quantity"))
                .constrainedFirmOrderQuantity(
                        DistributionPlanIntegrationValidation.validaNumeroNaoNegativoOuNulo(
                                processedFileRow.getColumnValueAsDouble(9),
                                "constrained firm order quantity"))
                .build();

    }

    @Override
    public DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO getPrimaryKeyDtoFromProcessedFileRow(
            ProcessedFileRow processedFileRow,
            DistributionPlanIntegrationSupportData supportData) {

        return new DistributionPlanIntegrationDataDto.DistributionPlanPrimaryKeyIntegrationDTO(
                null,
                processedFileRow.getColumnValueAsString(0),
                processedFileRow.getColumnValueAsString(1),
                processedFileRow.getColumnValueAsString(2),
                processedFileRow.getColumnValueAsLocalDateTime(3),
                processedFileRow.getColumnValueAsLocalDateTime(4));

    }

}
