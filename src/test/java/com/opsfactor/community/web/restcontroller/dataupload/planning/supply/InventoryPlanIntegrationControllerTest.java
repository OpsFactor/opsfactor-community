package com.opsfactor.community.web.restcontroller.dataupload.planning.supply;

import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.dto.InventoryPlanIntegrationDataDto;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.service.InventoryPlanIntegrationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Teste focado das rotas manuais canonicas do controller Community de Inventory Plan.
 */
public class InventoryPlanIntegrationControllerTest {

    @Test
    public void controllerShouldDeclareRestControllerAndExplicitAutowiredService() throws Exception {

        Assertions.assertTrue(InventoryPlanIntegrationController.class.isAnnotationPresent(RestController.class));

        Field field = InventoryPlanIntegrationController.class.getDeclaredField("inventoryPlanIntegrationService");
        Autowired autowired = field.getAnnotation(Autowired.class);
        Assertions.assertNotNull(autowired);
        Assertions.assertTrue(autowired.required());

    }

    @Test
    public void controllerShouldExposeCanonicalReadOnlyRoutes() throws Exception {

        Assertions.assertEquals(
                List.of("api/secured/data/file/inventoryplan/{supplyPlanId}"),
                getGetMappingValues("getInventoryPlanFile"));
        Assertions.assertEquals(
                List.of("api/secured/data/inventoryplan/{supplyPlanId}"),
                getGetMappingValues("getInventoryPlanJson"));

    }

    @Test
    public void controllerShouldDelegateToFilteredReadOnlyService() {

        InventoryPlanIntegrationService inventoryPlanIntegrationService =
                Mockito.mock(InventoryPlanIntegrationService.class);
        InventoryPlanIntegrationController controller = new InventoryPlanIntegrationController();
        ReflectionTestUtils.setField(
                controller,
                "inventoryPlanIntegrationService",
                inventoryPlanIntegrationService);
        List<List<Object>> fileRows = List.of(List.of("Location Id"));
        List<InventoryPlanIntegrationDataDto> jsonRows = List.of(
                InventoryPlanIntegrationDataDto.builder().build());
        Mockito.when(inventoryPlanIntegrationService.getFile(42L)).thenReturn(fileRows);
        Mockito.when(inventoryPlanIntegrationService.getInventoryPlanDTOList(42L)).thenReturn(jsonRows);

        Assertions.assertSame(fileRows, controller.getInventoryPlanFile(42L));
        Assertions.assertSame(jsonRows, controller.getInventoryPlanJson(42L));

    }

    private static List<String> getGetMappingValues(
            String methodName) throws Exception {

        Method method = Arrays.stream(InventoryPlanIntegrationController.class.getDeclaredMethods())
                .filter(candidateMethod -> candidateMethod.getName().equals(methodName))
                .findFirst()
                .orElseThrow();

        return List.of(method.getAnnotation(GetMapping.class).value());

    }

}
