package com.opsfactor.community.web.restcontroller.bi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.platform.bi.facade.CommunityProductionOverviewService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewSelectionDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Map;

/** Contrato de rota, RBAC e desserialização da leitura agregada Community. */
class CommunityProductionOverviewControllerTest {

    private static final String PRODUCTION_OVERVIEW_PATH =
            "api/secured/bi/planning/supply/productionoverview";
    private static final String PRODUCTION_OVERVIEW_ALIAS_PATH =
            "api/secured/bi/planning/supply/volumesandcapacities";
    @Test
    void shouldExposeOnlyCanonicalPostRouteWithoutInventingMethodRbac() throws Exception {

        Assertions.assertTrue(CommunityProductionOverviewController.class
                .isAnnotationPresent(RestController.class));
        Method method = CommunityProductionOverviewController.class.getDeclaredMethod(
                "getProductionOverview", CommunityProductionOverviewSelectionDTO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);

        Assertions.assertNotNull(postMapping);
        Assertions.assertArrayEquals(
                new String[]{PRODUCTION_OVERVIEW_PATH, PRODUCTION_OVERVIEW_ALIAS_PATH},
                postMapping.value());
        Assertions.assertNull(method.getAnnotation(Secured.class));

        CommunityProductionOverviewService service = Mockito.mock(
                CommunityProductionOverviewService.class);
        CommunityProductionOverviewController controller = getController(service);
        CommunityProductionOverviewSelectionDTO selection = new CommunityProductionOverviewSelectionDTO();
        CommunityProductionOverviewDTO expected = new CommunityProductionOverviewDTO();
        Mockito.when(service.getProductionOverview(selection)).thenReturn(expected);

        ResponseEntity<CommunityProductionOverviewDTO> response =
                controller.getProductionOverview(selection);

        Assertions.assertSame(expected, response.getBody());
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        Mockito.verify(service).getProductionOverview(selection);
        Mockito.verifyNoMoreInteractions(service);

    }

    @Test
    void shouldDeserializeCanonicalSelectionWithRequiredQuantityUom() throws Exception {

        CommunityProductionOverviewSelectionDTO selection = new ObjectMapper().readValue(
                "{\"supplyPlanId\":19,\"uomId\":\"EA\",\"locationDTOs\":[],"
                        + "\"valuesByMaterialCharacteristicId\":{\"materialId\":[\"MAT-1\"]}}",
                CommunityProductionOverviewSelectionDTO.class);

        Assertions.assertEquals(19L, selection.supplyPlanId);
        Assertions.assertEquals("EA", selection.uomId);
        Assertions.assertEquals(Map.of("materialId", java.util.List.of("MAT-1")),
                selection.valuesByMaterialCharacteristicId);

    }

    private CommunityProductionOverviewController getController(
            CommunityProductionOverviewService service) {

        CommunityProductionOverviewController controller = new CommunityProductionOverviewController();
        ReflectionTestUtils.setField(controller, "communityProductionOverviewService", service);
        return controller;

    }
}
