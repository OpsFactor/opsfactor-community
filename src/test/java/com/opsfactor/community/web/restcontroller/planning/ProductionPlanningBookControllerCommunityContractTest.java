package com.opsfactor.community.web.restcontroller.planning;

import com.opsfactor.community.capability.supplyplanning.productionplan.facade.ProductionPlanningBookFacade;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningBookDTO;
import com.opsfactor.community.capability.supplyplanning.productionplan.facade.dto.ProductionPlanningBookUpdateDTO;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Keeps the Production Planning Book REST surface inside the Community boundary.
 */
class ProductionPlanningBookControllerCommunityContractTest {

    @Test
    void controllerShouldExposeOnlyCanonicalProductionPlanningBookEndpoints() {

        List<Method> endpointMethods = List.of(ProductionPlanningBookController.class.getDeclaredMethods())
                .stream()
                .filter(method -> method.isAnnotationPresent(GetMapping.class)
                        || method.isAnnotationPresent(PostMapping.class))
                .toList();

        Assertions.assertEquals(2, endpointMethods.size());
        Assertions.assertTrue(endpointMethods.stream().anyMatch(method ->
                method.isAnnotationPresent(GetMapping.class)
                        && List.of(method.getAnnotation(GetMapping.class).value())
                        .contains("api/secured/planning/production/planningbook")));
        Assertions.assertTrue(endpointMethods.stream().anyMatch(method ->
                method.isAnnotationPresent(PostMapping.class)
                        && List.of(method.getAnnotation(PostMapping.class).value())
                        .contains("api/secured/planning/production/planningbook/update")));

        assertRoles(
                endpointMethods,
                "getProductionPlanningBook",
                new String[] {CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE});
        assertRoles(
                endpointMethods,
                "updatePlannedProduction",
                new String[] {CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE});

    }

    @Test
    void controllerShouldDelegateTypedReadAndWorkingPlanUpdateWithoutMapPayload() throws Exception {

        ProductionPlanningBookController productionPlanningBookController =
                new ProductionPlanningBookController();
        ProductionPlanningBookFacade productionPlanningBookFrontService =
                Mockito.mock(ProductionPlanningBookFacade.class);
        ProductionPlanningBookDTO productionPlanningBookDTO = new ProductionPlanningBookDTO(
                10L,
                "LOC",
                List.of(),
                List.of());
        ProductionPlanningBookUpdateDTO productionPlanningBookUpdateDTO =
                new ProductionPlanningBookUpdateDTO(
                        10L,
                        "LOC",
                        "MAT",
                        "RESOURCE",
                        LocalDateTime.of(2026, 7, 31, 23, 59, 59),
                        10.0d);

        injectField(
                productionPlanningBookController,
                "productionPlanningBookFrontService",
                productionPlanningBookFrontService);
        Mockito.when(productionPlanningBookFrontService.getProductionPlanningBook("LOC", 10L))
                .thenReturn(productionPlanningBookDTO);
        Mockito.when(productionPlanningBookFrontService.updatePlannedProduction(productionPlanningBookUpdateDTO))
                .thenReturn(productionPlanningBookDTO);

        ResponseEntity<ProductionPlanningBookDTO> readResponseEntity =
                productionPlanningBookController.getProductionPlanningBook("LOC", 10L);
        ResponseEntity<ProductionPlanningBookDTO> updateResponseEntity =
                productionPlanningBookController.updatePlannedProduction(productionPlanningBookUpdateDTO);

        Assertions.assertSame(productionPlanningBookDTO, readResponseEntity.getBody());
        Assertions.assertSame(productionPlanningBookDTO, updateResponseEntity.getBody());
        Mockito.verify(productionPlanningBookFrontService).getProductionPlanningBook("LOC", 10L);
        Mockito.verify(productionPlanningBookFrontService).updatePlannedProduction(productionPlanningBookUpdateDTO);
        Mockito.verifyNoMoreInteractions(productionPlanningBookFrontService);

    }

    @Test
    void controllerShouldDeclareItsRequiredFacadeDependencyExplicitly() throws Exception {

        Field field = ProductionPlanningBookController.class.getDeclaredField(
                "productionPlanningBookFrontService");

        Assertions.assertNotNull(field.getAnnotation(Autowired.class));
        Assertions.assertTrue(field.getAnnotation(Autowired.class).required());

    }

    private void assertRoles(
            List<Method> endpointMethods,
            String methodName,
            String[] expectedRoles) {

        Method endpointMethod = endpointMethods.stream()
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        Secured secured = endpointMethod.getAnnotation(Secured.class);

        Assertions.assertNotNull(secured);
        Assertions.assertArrayEquals(expectedRoles, secured.value());

    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }
}
