package com.opsfactor.community.web.restcontroller.bi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.platform.bi.facade.CommunityProductionOverviewResourceDetailService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewResourceDetailDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewResourceDetailResponseDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityProductionOverviewResourceDetailSelectionDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

class CommunityProductionOverviewResourceDetailControllerTest {

    private static final String PRODUCTION_OVERVIEW_PATH =
            "api/secured/bi/planning/supply/productionoverview/"
                    + "{supplyPlanId}/{productionResourceId}/{periodIndex}/details";

    @Test
    void shouldExposeOnlyCanonicalGetRouteWithoutChangingExistingRbac() throws Exception {

        Assertions.assertTrue(CommunityProductionOverviewResourceDetailController.class
                .isAnnotationPresent(RestController.class));
        Method method = CommunityProductionOverviewResourceDetailController.class.getDeclaredMethod(
                "getResourceDetail",
                Long.class,
                String.class,
                Integer.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        Assertions.assertNotNull(getMapping);
        Assertions.assertArrayEquals(
                new String[]{PRODUCTION_OVERVIEW_PATH},
                getMapping.value());
        Assertions.assertNull(method.getAnnotation(Secured.class),
                "A rota canônica não declara @Secured local; a política global permanece responsável.");

        CommunityProductionOverviewResourceDetailService service = Mockito.mock(
                CommunityProductionOverviewResourceDetailService.class);
        CommunityProductionOverviewResourceDetailController controller =
                getController(service);
        CommunityProductionOverviewResourceDetailResponseDTO expected =
                new CommunityProductionOverviewResourceDetailResponseDTO();
        Mockito.when(service.getResourceDetail(19L, "LINE-A", 4)).thenReturn(expected);

        ResponseEntity<CommunityProductionOverviewResourceDetailResponseDTO> response =
                controller.getResourceDetail(19L, "LINE-A", 4);

        Assertions.assertSame(expected, response.getBody());
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        Mockito.verify(service).getResourceDetail(19L, "LINE-A", 4);
        Mockito.verifyNoMoreInteractions(service);

    }

    @Test
    void shouldForwardCanonicalPostBodyWithoutGivingItsUomAuthority() throws Exception {

        Method method = CommunityProductionOverviewResourceDetailController.class.getDeclaredMethod(
                "getResourceDetail",
                Long.class,
                String.class,
                Integer.class,
                CommunityProductionOverviewResourceDetailSelectionDTO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);

        Assertions.assertNotNull(postMapping);
        Assertions.assertArrayEquals(
                new String[]{PRODUCTION_OVERVIEW_PATH},
                postMapping.value());
        Assertions.assertNull(method.getAnnotation(Secured.class));

        CommunityProductionOverviewResourceDetailSelectionDTO selection = new ObjectMapper().readValue(
                "{\"supplyPlanId\":999,\"uomId\":\"KG\",\"locationDTOs\":[],"
                        + "\"valuesByMaterialCharacteristicId\":{\"materialId\":[\"MAT-1\"]}}",
                CommunityProductionOverviewResourceDetailSelectionDTO.class);
        Assertions.assertEquals("KG", selection.uomId);
        Assertions.assertEquals(Map.of("materialId", List.of("MAT-1")),
                selection.valuesByMaterialCharacteristicId);

        CommunityProductionOverviewResourceDetailService service = Mockito.mock(
                CommunityProductionOverviewResourceDetailService.class);
        CommunityProductionOverviewResourceDetailController controller =
                getController(service);
        CommunityProductionOverviewResourceDetailResponseDTO expected =
                new CommunityProductionOverviewResourceDetailResponseDTO();
        Mockito.when(service.getResourceDetail(19L, "LINE-A", 4, selection)).thenReturn(expected);

        ResponseEntity<CommunityProductionOverviewResourceDetailResponseDTO> response =
                controller.getResourceDetail(19L, "LINE-A", 4, selection);

        Assertions.assertSame(expected, response.getBody());
        Mockito.verify(service).getResourceDetail(19L, "LINE-A", 4, selection);
        Mockito.verifyNoMoreInteractions(service);

    }

    @Test
    void shouldPreserveJsonNamesForExplicitOutputAndCapacityUnits() throws Exception {

        CommunityProductionOverviewResourceDetailDTO row = new CommunityProductionOverviewResourceDetailDTO();
        row.supplyPlanId = 19L;
        row.locationId = "LOC-1";
        row.locationDescription = "Plant";
        row.productionResourceId = "LINE-A";
        row.productionResourceDescription = "Line A";
        row.periodIndex = 4;
        row.plannedDate = LocalDateTime.of(2026, 7, 31, 23, 59, 59);
        row.outputMaterialId = "MAT-1";
        row.outputMaterialDescription = "Finished product";
        row.productionVersionId = "PV-1";
        row.routingId = "R-1";
        row.routingDescription = "Routing";
        row.billOfMaterialsId = "BOM-1";
        row.billOfMaterialsDescription = "Bill";
        row.resourceCapacityUnitOfMeasureId = "Hours";
        row.unitOfMeasureId = "EA";
        row.unconstrainedHours = 5.0d;
        row.constrainedHours = 4.0d;
        row.workPlanHours = 3.0d;
        row.throughputQuantityPerHour = 2.0d;
        row.unconstrainedQuantity = 10.0d;
        row.constrainedQuantity = 8.0d;
        row.workPlanQuantity = 6.0d;

        CommunityProductionOverviewResourceDetailResponseDTO response =
                new CommunityProductionOverviewResourceDetailResponseDTO();
        response.supplyPlanId = 19L;
        response.locationId = "LOC-1";
        response.locationDescription = "Plant";
        response.productionResourceId = "LINE-A";
        response.productionResourceDescription = "Line A";
        response.periodIndex = 4;
        response.plannedDate = LocalDateTime.of(2026, 7, 31, 0, 0);
        response.resourceCapacityUnitOfMeasureId = "Hours";
        response.availableCapacityInHoursOrQuantity = 12.0d;
        response.rows.add(row);

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);

        Assertions.assertTrue(json.contains("\"resourceCapacityUnitOfMeasureId\":\"Hours\""));
        Assertions.assertTrue(json.contains("\"unitOfMeasureId\":\"EA\""));
        Assertions.assertTrue(json.contains("\"unconstrainedQuantity\":10.0"));
        Assertions.assertTrue(json.contains("\"workPlanQuantity\":6.0"));
        Assertions.assertFalse(json.contains("\"uomId\""));

    }

    private CommunityProductionOverviewResourceDetailController getController(
            CommunityProductionOverviewResourceDetailService service) {

        CommunityProductionOverviewResourceDetailController controller =
                new CommunityProductionOverviewResourceDetailController();
        ReflectionTestUtils.setField(
                controller,
                "communityProductionOverviewResourceDetailService",
                service);
        return controller;

    }
}
