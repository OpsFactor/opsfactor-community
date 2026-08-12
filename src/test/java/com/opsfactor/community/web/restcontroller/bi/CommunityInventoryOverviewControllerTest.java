package com.opsfactor.community.web.restcontroller.bi;

import com.opsfactor.community.platform.bi.facade.CommunityInventoryOverviewService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityInventoryOverviewSelectionDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

class CommunityInventoryOverviewControllerTest {

    @Test
    void shouldExposeOnlyTheNewPhysicalInventoryOverviewRoute() throws Exception {

        Assertions.assertTrue(CommunityInventoryOverviewController.class.isAnnotationPresent(RestController.class));
        Method method = CommunityInventoryOverviewController.class.getDeclaredMethod(
                "getInventoryOverview",
                CommunityInventoryOverviewSelectionDTO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        Secured secured = method.getAnnotation(Secured.class);
        Assertions.assertNotNull(postMapping);
        Assertions.assertArrayEquals(new String[]{"api/secured/bi/supply/inventory-overview"}, postMapping.value());
        Assertions.assertNotNull(secured);
        Assertions.assertArrayEquals(new String[]{"ROLE_ADMIN"}, secured.value());

        CommunityInventoryOverviewService service = Mockito.mock(CommunityInventoryOverviewService.class);
        CommunityInventoryOverviewController controller = new CommunityInventoryOverviewController();
        ReflectionTestUtils.setField(controller, "communityInventoryOverviewService", service);
        CommunityInventoryOverviewSelectionDTO selection =
                new CommunityInventoryOverviewSelectionDTO(10L, "PC", List.of(), List.of(), Map.of(), Map.of(), null);
        CommunityInventoryOverviewDTO expected = new CommunityInventoryOverviewDTO("PC", List.of(), List.of(), List.of());
        Mockito.when(service.getInventoryOverview(selection)).thenReturn(expected);

        ResponseEntity<CommunityInventoryOverviewDTO> response = controller.getInventoryOverview(selection);

        Assertions.assertSame(expected, response.getBody());
        Mockito.verify(service).getInventoryOverview(selection);
        Mockito.verifyNoMoreInteractions(service);

    }

}
