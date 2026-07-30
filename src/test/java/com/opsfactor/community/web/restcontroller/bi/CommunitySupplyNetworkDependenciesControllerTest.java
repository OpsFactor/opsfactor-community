package com.opsfactor.community.web.restcontroller.bi;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.SupplyNetworkDependencyExplorerService;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.MaterialLocationDependencyDTO;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.SupplyNetworkDependencyDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.List;

class CommunitySupplyNetworkDependenciesControllerTest {

    @Test
    void shouldPreserveLegacyDependencyExplorerRouteAndForwardItsFourParameters() throws Exception {

        Assertions.assertTrue(CommunitySupplyNetworkDependenciesController.class.isAnnotationPresent(RestController.class));
        Method method = CommunitySupplyNetworkDependenciesController.class.getDeclaredMethod(
                "getSupplyNetworkDependencies", String.class, String.class, String.class, Integer.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        Assertions.assertNotNull(getMapping);
        Assertions.assertArrayEquals(new String[]{"api/secured/supplynetwork/dependencies"}, getMapping.value());

        SupplyNetworkDependencyExplorerService service = Mockito.mock(SupplyNetworkDependencyExplorerService.class);
        CommunitySupplyNetworkDependenciesController controller =
                new CommunitySupplyNetworkDependenciesController(service);
        MaterialLocationDependencyDTO expectedDependency = new MaterialLocationDependencyDTO();
        expectedDependency.elementType = SupplyNetworkDependencyDTO.ElementType.MATERIAL_LOCATION;
        List<SupplyNetworkDependencyDTO> expected = List.of(expectedDependency);
        Mockito.when(service.getDependencies("NETWORK", "LOCATION", "MATERIAL", 3)).thenReturn(expected);

        ResponseEntity<List<SupplyNetworkDependencyDTO>> response = controller.getSupplyNetworkDependencies(
                "NETWORK", "LOCATION", "MATERIAL", 3);

        Assertions.assertSame(expected, response.getBody());
        Mockito.verify(service).getDependencies("NETWORK", "LOCATION", "MATERIAL", 3);
        Mockito.verifyNoMoreInteractions(service);

    }
}
