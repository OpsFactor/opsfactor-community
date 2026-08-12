package com.opsfactor.community.web.restcontroller.bi;

import com.opsfactor.community.platform.bi.facade.CommunityDemandSalesOverviewService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityDemandSalesOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityDemandSalesOverviewSelectionDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

class CommunityDemandSalesOverviewControllerTest {

    @Test
    void shouldExposeReadOnlyCommunityDemandSalesOverviewRoute() throws Exception {

        Assertions.assertTrue(CommunityDemandSalesOverviewController.class.isAnnotationPresent(RestController.class));
        Method method = CommunityDemandSalesOverviewController.class.getDeclaredMethod(
                "getDemandSalesOverview",
                CommunityDemandSalesOverviewSelectionDTO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        Assertions.assertNotNull(postMapping);
        Assertions.assertArrayEquals(new String[]{"api/secured/planning/demand/overview"}, postMapping.value());

        CommunityDemandSalesOverviewService service = Mockito.mock(CommunityDemandSalesOverviewService.class);
        CommunityDemandSalesOverviewController controller = new CommunityDemandSalesOverviewController();
        ReflectionTestUtils.setField(controller, "communityDemandSalesOverviewService", service);
        CommunityDemandSalesOverviewSelectionDTO selection =
                new CommunityDemandSalesOverviewSelectionDTO(
                        10L,
                        null,
                        "PC",
                        2,
                        List.of("MAT-1"),
                        List.of("LOC-1"),
                        Map.of("CATEGORY", List.of("Paper")),
                        Map.of("REGION", List.of("South")));
        CommunityDemandSalesOverviewDTO expected = new CommunityDemandSalesOverviewDTO(List.of(), List.of());
        Mockito.when(service.getDemandSalesOverview(selection)).thenReturn(expected);

        ResponseEntity<CommunityDemandSalesOverviewDTO> response = controller.getDemandSalesOverview(selection);

        Assertions.assertSame(expected, response.getBody());
        Mockito.verify(service).getDemandSalesOverview(selection);
        Mockito.verifyNoMoreInteractions(service);

    }
}
