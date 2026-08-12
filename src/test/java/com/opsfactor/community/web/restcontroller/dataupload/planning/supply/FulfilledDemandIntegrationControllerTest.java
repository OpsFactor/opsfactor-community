package com.opsfactor.community.web.restcontroller.dataupload.planning.supply;

import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.dto.FulfilledDemandIntegrationDataDto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.service.FulfilledDemandIntegrationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Contrato das duas rotas read-only de Fulfilled Demand Community.
 */
public class FulfilledDemandIntegrationControllerTest {

    @Test
    public void controllerShouldExposeCanonicalReadOnlyRoutesAndDelegate() throws Exception {

        Assertions.assertTrue(
                FulfilledDemandIntegrationController.class.isAnnotationPresent(RestController.class));
        Assertions.assertEquals(
                List.of("api/secured/data/file/fulfilleddemand/{supplyPlanId}"),
                getGetMappingValues("getFulfilledDemandFile"));
        Assertions.assertEquals(
                List.of("api/secured/data/fulfilleddemand/{supplyPlanId}"),
                getGetMappingValues("getFulfilledDemandJson"));
        Assertions.assertEquals(
                List.of("api/secured/data/file/fulfilleddemand/{supplyPlanId}/period/{referenceDate}"),
                getGetMappingValues("getFulfilledDemandPeriodFile"));
        Assertions.assertEquals(
                List.of("api/secured/data/fulfilleddemand/{supplyPlanId}/period/{referenceDate}"),
                getGetMappingValues("getFulfilledDemandPeriodJson"));

        FulfilledDemandIntegrationService service =
                Mockito.mock(FulfilledDemandIntegrationService.class);
        FulfilledDemandIntegrationController controller =
                new FulfilledDemandIntegrationController();
        ReflectionTestUtils.setField(controller, "fulfilledDemandIntegrationService", service);
        List<List<Object>> fileRows = List.of(List.of("Fulfilled Demand"));
        List<FulfilledDemandIntegrationDataDto> jsonRows = List.of(
                FulfilledDemandIntegrationDataDto.builder().supplyPlanId(2L).build());
        LocalDate referenceDate = LocalDate.of(2027, 2, 1);
        Mockito.when(service.getFile(2L)).thenReturn(fileRows);
        Mockito.when(service.getFulfilledDemandDtoList(2L)).thenReturn(jsonRows);
        Mockito.when(service.getFileByPeriod(2L, referenceDate)).thenReturn(fileRows);
        Mockito.when(service.getFulfilledDemandDtoListByPeriod(2L, referenceDate)).thenReturn(jsonRows);

        Assertions.assertSame(fileRows, controller.getFulfilledDemandFile(2L));
        Assertions.assertSame(jsonRows, controller.getFulfilledDemandJson(2L));
        Assertions.assertSame(fileRows, controller.getFulfilledDemandPeriodFile(2L, referenceDate));
        Assertions.assertSame(jsonRows, controller.getFulfilledDemandPeriodJson(2L, referenceDate));

    }

    private static List<String> getGetMappingValues(
            String methodName) throws Exception {

        Method method = Arrays.stream(FulfilledDemandIntegrationController.class.getDeclaredMethods())
                .filter(candidateMethod -> candidateMethod.getName().equals(methodName))
                .findFirst()
                .orElseThrow();

        return List.of(method.getAnnotation(GetMapping.class).value());

    }

}
