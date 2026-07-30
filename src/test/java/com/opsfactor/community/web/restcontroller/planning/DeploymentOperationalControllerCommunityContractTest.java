package com.opsfactor.community.web.restcontroller.planning;

import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.DeploymentOperationalFacade;
import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto.DeploymentOperationalInboundUpdateDTO;
import com.opsfactor.community.capability.supplyplanning.distributionplan.facade.dto.DeploymentOperationalLineDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST contract for Community operational deployment only.
 */
class DeploymentOperationalControllerCommunityContractTest {

    @Test
    void controllerShouldExposeOnlyOperationalReadAndInboundUpdateEndpoints() {

        List<Method> endpointMethods = List.of(DeploymentOperationalController.class.getDeclaredMethods())
                .stream()
                .filter(method -> method.isAnnotationPresent(GetMapping.class)
                        || method.isAnnotationPresent(PostMapping.class))
                .toList();

        Assertions.assertEquals(2, endpointMethods.size());
        Assertions.assertTrue(endpointMethods.stream().anyMatch(method ->
                method.isAnnotationPresent(GetMapping.class)
                        && List.of(method.getAnnotation(GetMapping.class).value())
                        .contains("api/secured/planning/supply/deployment")));
        Assertions.assertTrue(endpointMethods.stream().anyMatch(method ->
                method.isAnnotationPresent(PostMapping.class)
                        && List.of(method.getAnnotation(PostMapping.class).value())
                        .contains("api/secured/planning/supply/deployment/update")));
        for (Method endpointMethod : endpointMethods) {
            Secured secured = endpointMethod.getAnnotation(Secured.class);
            Assertions.assertNotNull(secured);
            Assertions.assertArrayEquals(
                    new String[] {CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE},
                    secured.value());
        }

    }

    @Test
    void controllerShouldDelegateTypedOperationalReadAndUpdate() throws Exception {

        DeploymentOperationalController deploymentOperationalController =
                new DeploymentOperationalController();
        DeploymentOperationalFacade deploymentOperationalFrontService =
                Mockito.mock(DeploymentOperationalFacade.class);
        DeploymentOperationalLineDTO deploymentOperationalLineDTO = new DeploymentOperationalLineDTO(
                10L,
                "ORIGIN",
                "DESTINATION",
                "MATERIAL",
                "Material",
                LocalDateTime.of(2026, 7, 31, 23, 59, 59),
                2,
                LocalDate.of(2026, 7, 3),
                "EA",
                12.0d);
        DeploymentOperationalInboundUpdateDTO deploymentOperationalInboundUpdateDTO =
                new DeploymentOperationalInboundUpdateDTO(10L, "ORIGIN", "DESTINATION", "MATERIAL", 15.0d);

        injectField(
                deploymentOperationalController,
                "deploymentOperationalFrontService",
                deploymentOperationalFrontService);
        Mockito.when(deploymentOperationalFrontService.getDeploymentOperationalLine(
                        10L,
                        "ORIGIN",
                        "DESTINATION",
                        "MATERIAL"))
                .thenReturn(deploymentOperationalLineDTO);
        Mockito.when(deploymentOperationalFrontService.updatePlannedInbound(
                        deploymentOperationalInboundUpdateDTO))
                .thenReturn(deploymentOperationalLineDTO);

        ResponseEntity<DeploymentOperationalLineDTO> readResponseEntity =
                deploymentOperationalController.getDeploymentOperationalLine(
                        10L,
                        "ORIGIN",
                        "DESTINATION",
                        "MATERIAL");
        ResponseEntity<DeploymentOperationalLineDTO> updateResponseEntity =
                deploymentOperationalController.updatePlannedInbound(
                        deploymentOperationalInboundUpdateDTO);

        Assertions.assertSame(deploymentOperationalLineDTO, readResponseEntity.getBody());
        Assertions.assertSame(deploymentOperationalLineDTO, updateResponseEntity.getBody());
        Mockito.verify(deploymentOperationalFrontService).getDeploymentOperationalLine(
                10L,
                "ORIGIN",
                "DESTINATION",
                "MATERIAL");
        Mockito.verify(deploymentOperationalFrontService).updatePlannedInbound(
                deploymentOperationalInboundUpdateDTO);
        Mockito.verifyNoMoreInteractions(deploymentOperationalFrontService);

    }

    @Test
    void controllerShouldDeclareRequiredFacadeDependency() throws Exception {

        Field field = DeploymentOperationalController.class.getDeclaredField(
                "deploymentOperationalFrontService");

        Assertions.assertNotNull(field.getAnnotation(Autowired.class));
        Assertions.assertTrue(field.getAnnotation(Autowired.class).required());

    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }
}
