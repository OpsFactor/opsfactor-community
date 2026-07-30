package com.opsfactor.community.web.restcontroller.dataupload.transactionaldata;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.dto.SelloutReportParametrosDTO;
import com.opsfactor.community.web.dto.template.AgGridDTO;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.SelloutFacade;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.annotation.Secured;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

/**
 * Contrato do adaptador Community da exportacao historica de sell-out.
 */
class SelloutIntegrationControllerTest {

    @Test
    void historicalSelloutShouldDelegateToTheExistingCommunityFrontService() {

        SelloutIntegrationController controller = new SelloutIntegrationController();
        SelloutFacade selloutFrontService = Mockito.mock(SelloutFacade.class);
        SelloutReportParametrosDTO parameters = new SelloutReportParametrosDTO();
        AgGridDTO expectedResult = AgGridDTO.builder().build();
        Mockito.when(selloutFrontService.getSelloutParaExportacaoAgGrid(parameters))
                .thenReturn(expectedResult);
        ReflectionTestUtils.setField(controller, "selloutFrontService", selloutFrontService);

        Assertions.assertSame(expectedResult, controller.getHistoricalSellout(parameters));
        Mockito.verify(selloutFrontService).getSelloutParaExportacaoAgGrid(parameters);
        Mockito.verifyNoMoreInteractions(selloutFrontService);

    }

    @Test
    void historicalSelloutShouldPreserveSemanticReportPathAndAdminRole() throws Exception {

        Method method = SelloutIntegrationController.class.getDeclaredMethod(
                "getHistoricalSellout",
                SelloutReportParametrosDTO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        Secured secured = method.getAnnotation(Secured.class);

        Assertions.assertArrayEquals(
                new String[]{"api/secured/historical/sellout"},
                postMapping.value());
        Assertions.assertArrayEquals(
                new String[]{CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE},
                secured.value());

    }

}
