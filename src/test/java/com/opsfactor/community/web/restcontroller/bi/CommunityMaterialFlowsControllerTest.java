package com.opsfactor.community.web.restcontroller.bi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.platform.bi.facade.CommunityMaterialFlowsService;
import com.opsfactor.community.platform.bi.facade.dto.CommunityMaterialFlowsDTO;
import com.opsfactor.community.platform.bi.facade.dto.CommunityMaterialFlowsLocationAndColorDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.List;

class CommunityMaterialFlowsControllerTest {

    @Test
    void shouldExposeLegacyMaterialFlowsRouteAndDelegateToCommunityService() throws Exception {

        Assertions.assertTrue(CommunityMaterialFlowsController.class.isAnnotationPresent(RestController.class));
        Method method = CommunityMaterialFlowsController.class.getDeclaredMethod(
                "getMaterialFlows", Long.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        Assertions.assertNotNull(getMapping);
        Assertions.assertArrayEquals(
                new String[]{"api/secured/bi/planning/supply/materialflows/{supplyPlanId}"},
                getMapping.value());

        CommunityMaterialFlowsService service = Mockito.mock(CommunityMaterialFlowsService.class);
        CommunityMaterialFlowsController controller = new CommunityMaterialFlowsController();
        ReflectionTestUtils.setField(controller, "communityMaterialFlowsService", service);
        CommunityMaterialFlowsDTO expected = new CommunityMaterialFlowsDTO();
        Mockito.when(service.getMaterialFlows(51L)).thenReturn(expected);

        ResponseEntity<CommunityMaterialFlowsDTO> response = controller.getMaterialFlows(51L);

        Assertions.assertSame(expected, response.getBody());
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
        Mockito.verify(service).getMaterialFlows(51L);
        Mockito.verifyNoMoreInteractions(service);

    }

    @Test
    void shouldPreserveLegacyJsonPropertyNamesForChartClient() throws Exception {

        CommunityMaterialFlowsDTO dto = new CommunityMaterialFlowsDTO();
        dto.locationAndColorList.add(new CommunityMaterialFlowsLocationAndColorDTO("LOC-1", "#b2182b"));
        dto.flowData.add(List.of(0.0d));

        String json = new ObjectMapper().writeValueAsString(dto);

        Assertions.assertEquals(
                "{\"locationAndColorList\":[{\"location\":\"LOC-1\",\"color\":\"#b2182b\"}],\"flowData\":[[0.0]]}",
                json);

    }
}
