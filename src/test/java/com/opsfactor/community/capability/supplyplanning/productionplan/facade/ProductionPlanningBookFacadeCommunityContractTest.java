package com.opsfactor.community.capability.supplyplanning.productionplan.facade;

import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningBookDTO;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningBookUpdateDTO;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningMaterialDTO;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningResourceDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contract tests for the deliberately narrow Community Production Planning Book.
 */
class ProductionPlanningBookFacadeCommunityContractTest {

    @Test
    void updateCommandShouldRejectNegativePlannedQuantityBeforePersistence() throws Exception {

        ProductionPlanningBookFacade productionPlanningBookFrontService =
                new ProductionPlanningBookFacade();
        ProductionPlanningBookUpdateDTO productionPlanningBookUpdateDTO =
                new ProductionPlanningBookUpdateDTO(
                        10L,
                        "LOC",
                        "MAT",
                        "RESOURCE",
                        LocalDateTime.of(2026, 7, 31, 23, 59, 59),
                        -0.01d);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> invokeValidateUpdateCommand(
                        productionPlanningBookFrontService,
                        productionPlanningBookUpdateDTO));

        Assertions.assertEquals(
                "Production Planning Book planned quantity must be greater than or equal to zero.",
                illegalArgumentException.getMessage());

    }

    @Test
    void updateMethodShouldBeTransactionalAndAcceptOnlyTypedWorkingPlanCommand() throws Exception {

        Method updateMethod = ProductionPlanningBookFacade.class.getDeclaredMethod(
                "updatePlannedProduction",
                ProductionPlanningBookUpdateDTO.class);

        Assertions.assertNotNull(
                updateMethod.getAnnotation(Transactional.class),
                "A escrita do Production Planning Book deve permanecer transacional.");
        Assertions.assertEquals(
                ProductionPlanningBookDTO.class,
                updateMethod.getReturnType());
        Assertions.assertEquals(
                ProductionPlanningBookUpdateDTO.class,
                updateMethod.getParameterTypes()[0]);

    }

    @Test
    void publicDtosShouldNotExposeFirmOrdersOrSchedulingState() {

        Assertions.assertEquals(
                List.of("supplyPlanId", "locationId", "periodEndDates", "resources"),
                List.of(ProductionPlanningBookDTO.class.getRecordComponents())
                        .stream()
                        .map(component -> component.getName())
                        .toList());
        Assertions.assertEquals(
                List.of("productionResourceId", "description", "capacityHoursByPeriod", "materials"),
                List.of(ProductionPlanningResourceDTO.class.getRecordComponents())
                        .stream()
                        .map(component -> component.getName())
                        .toList());
        Assertions.assertEquals(
                List.of("materialId", "description", "unitOfMeasureId", "plannedQuantityByPeriod"),
                List.of(ProductionPlanningMaterialDTO.class.getRecordComponents())
                        .stream()
                        .map(component -> component.getName())
                        .toList());

    }

    @Test
    void updateContextShouldPopulateEveryOperationalSeriesBeforePersistingProduction() throws Exception {

        Path sourcePath = Path.of(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/productionplan/facade/ProductionPlanningBookFacade.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        int inventoryPosition = source.indexOf(
                "populaSupplyPlanningProjectionComInventoryPlan(",
                source.indexOf("createProductionPlanningBookUpdateContext"));
        int inboundPosition = source.indexOf(
                "populaSupplyPlanningProjectionComDistributionPlanInbound(",
                inventoryPosition);
        int outboundPosition = source.indexOf(
                "populaSupplyPlanningProjectionComDistributionPlanOutbound(",
                inboundPosition);
        int productionInputPosition = source.indexOf(
                "populaSupplyPlanningProjectionComProductionPlanInput(",
                outboundPosition);
        int productionOutputPosition = source.indexOf(
                "populaSupplyPlanningProjectionComProductionPlanOutput(",
                productionInputPosition);

        Assertions.assertTrue(inventoryPosition >= 0);
        Assertions.assertTrue(inventoryPosition < inboundPosition);
        Assertions.assertTrue(inboundPosition < outboundPosition);
        Assertions.assertTrue(outboundPosition < productionInputPosition);
        Assertions.assertTrue(productionInputPosition < productionOutputPosition);
        Assertions.assertTrue(
                source.contains("validateMaterialProducedByResource(\n                supplyNetworkProjection,"),
                "O comando deve validar a combinacao material/recurso antes de montar e persistir o ajuste.");
        Assertions.assertTrue(
                source.contains("getMaterialSetProduzidoEmRecursoProdutivo(productionResource)"),
                "A validacao deve usar a malha/projection carregada, sem consulta por celula.");

    }

    private void invokeValidateUpdateCommand(
            ProductionPlanningBookFacade productionPlanningBookFrontService,
            ProductionPlanningBookUpdateDTO productionPlanningBookUpdateDTO) throws Exception {

        Method validateMethod = ProductionPlanningBookFacade.class.getDeclaredMethod(
                "validateUpdateCommand",
                ProductionPlanningBookUpdateDTO.class);
        validateMethod.setAccessible(true);
        try {
            validateMethod.invoke(productionPlanningBookFrontService, productionPlanningBookUpdateDTO);
        } catch (InvocationTargetException invocationTargetException) {
            if (invocationTargetException.getCause() instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw invocationTargetException;
        }

    }
}
