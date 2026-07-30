package com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.mapper;

import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.dto.InventoryPlanIntegrationDataDto;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato Community do mapper read-only de Inventory Plan simples.
 *
 * <p>O arquivo continua filtrado por Supply Plan no path, portanto a coluna de
 * Supply Plan nao aparece no XLSX/CSV. A chave JSON, entretanto, conserva o
 * id do plano para rastreabilidade e reconciliacao generica.</p>
 */
public class InventoryPlanIntegrationMapperCommunityTest {

    @Test
    public void inventoryPlanHeadersShouldStayStable() {

        InventoryPlanIntegrationMapper mapper = new InventoryPlanIntegrationMapper();

        List<String> processedFileHeaders = mapper.getProcessedFileHeaders();

        Assertions.assertEquals(
                List.of(
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
                        "Projected Stock in SNP UOM - Constrained Version"),
                processedFileHeaders);
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Enterprise Column"));

    }

    @Test
    public void inventoryPlanShouldExportFileRowWithoutSupplyPlanColumn() {

        InventoryPlanIntegrationMapper mapper = new InventoryPlanIntegrationMapper();
        LocalDateTime referenceDate = LocalDateTime.of(2026, 1, 31, 0, 0);
        InventoryPlanIntegrationDataDto dto = InventoryPlanIntegrationDataDto.builder()
                .primaryKeyDto(new InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO(
                        42L,
                        "LOC_01",
                        "MAT_01",
                        referenceDate))
                .unitOfMeasureId("EA")
                .safetyStockQuantity(2.0d)
                .maximumStockQuantity(10.0d)
                .projectedStockWorkingVersion(-3.0d)
                .projectedStockUnconstrainedVersion(4.0d)
                .projectedStockConstrainedVersion(5.0d)
                .defaultSnpUnitOfMeasureId("CS")
                .projectedStockWorkingVersionDefaultSnpUom(-6.0d)
                .projectedStockUnconstrainedVersionDefaultSnpUom(8.0d)
                .projectedStockConstrainedVersionDefaultSnpUom(10.0d)
                .build();

        ProcessedFileRow processedFileRow =
                mapper.convertEntityToProcessedFileRow(
                        dto,
                        new InventoryPlanIntegrationSupportData());

        Assertions.assertEquals(13, processedFileRow.getRowSize());
        Assertions.assertEquals("LOC_01", processedFileRow.getColumnValue(0));
        Assertions.assertEquals("MAT_01", processedFileRow.getColumnValue(1));
        Assertions.assertEquals(referenceDate.toString(), processedFileRow.getColumnValue(2));
        Assertions.assertFalse(processedFileRow.getRowAsObjectList().contains(42L));

    }

    @Test
    public void inventoryPlanShouldParseFileRowWithPathScopedSupplyPlan() {

        InventoryPlanIntegrationMapper mapper = new InventoryPlanIntegrationMapper();
        LocalDateTime referenceDate = LocalDateTime.of(2026, 2, 28, 0, 0);
        ProcessedFileRow processedFileRow = new ProcessedFileRow(List.of(
                "LOC_01",
                "MAT_01",
                referenceDate,
                "EA",
                2.0d,
                10.0d,
                -3.0d,
                -4.0d,
                -5.0d,
                "CS",
                "No conversion from EA to CS",
                "No conversion from EA to CS",
                "No conversion from EA to CS"));

        InventoryPlanIntegrationDataDto dto =
                mapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                        processedFileRow,
                        new InventoryPlanIntegrationSupportData());
        InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO primaryKeyDto =
                mapper.getPrimaryKeyDtoFromProcessedFileRow(
                        processedFileRow,
                        new InventoryPlanIntegrationSupportData());

        Assertions.assertNull(primaryKeyDto.supplyPlanId);
        Assertions.assertEquals("LOC_01", primaryKeyDto.locationId);
        Assertions.assertEquals("MAT_01", primaryKeyDto.materialId);
        Assertions.assertEquals(referenceDate, primaryKeyDto.referenceDate);
        Assertions.assertEquals(-3.0d, dto.projectedStockWorkingVersion);
        Assertions.assertEquals(-4.0d, dto.projectedStockUnconstrainedVersion);
        Assertions.assertEquals(-5.0d, dto.projectedStockConstrainedVersion);

    }

    @Test
    public void inventoryPlanShouldRemainReadOnlyAndValidatePhysicalQuantities() {

        InventoryPlanIntegrationMapper mapper = new InventoryPlanIntegrationMapper();
        ProcessedFileRow processedFileRowWithNegativeSafetyStock = new ProcessedFileRow(List.of(
                "LOC_01",
                "MAT_01",
                LocalDateTime.of(2026, 3, 31, 0, 0),
                "EA",
                -1.0d,
                10.0d,
                -3.0d,
                -4.0d,
                -5.0d));

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> mapper.updateEntityNonPrimaryFieldsFromDTO(
                        InventoryPlanIntegrationDataDto.builder().build(),
                        InventoryPlanIntegrationDataDto.builder().build(),
                        new InventoryPlanIntegrationSupportData(),
                        null));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> mapper.getDtoWithoutPrimaryKeyFromProcessedFileRow(
                        processedFileRowWithNegativeSafetyStock,
                        new InventoryPlanIntegrationSupportData()));

    }

}
