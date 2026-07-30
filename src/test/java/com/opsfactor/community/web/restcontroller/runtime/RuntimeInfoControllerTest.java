package com.opsfactor.community.web.restcontroller.runtime;

import com.opsfactor.community.platform.runtime.facade.CommunityRuntimeInfoService;
import com.opsfactor.community.platform.runtime.facade.dto.RuntimeInfoDTO;
import com.opsfactor.community.platform.runtime.facade.RuntimeInfoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Contratos do endpoint aberto de runtime info usado pelo front-end
 * Community/Enterprise.
 *
 * <p>O controller pertence ao Community e deve permanecer estavel. A diferenca
 * entre as edicoes acontece no bean {@link RuntimeInfoService}: o Community
 * usa a implementacao padrao e o Enterprise registra uma implementacao
 * {@code @Primary} no classpath privado.</p>
 */
class RuntimeInfoControllerTest {

    @Test
    void runtimeInfoControllerShouldExposeOpenRuntimeInfoEndpoint() throws NoSuchMethodException {

        Method getRuntimeInfoMethod = RuntimeInfoController.class.getMethod("getRuntimeInfo");
        GetMapping getMapping = getRuntimeInfoMethod.getAnnotation(GetMapping.class);

        Assertions.assertTrue(RuntimeInfoController.class.isAnnotationPresent(RestController.class));
        Assertions.assertNotNull(getMapping);
        Assertions.assertArrayEquals(
                new String[]{"api/open/runtime-info"},
                getMapping.value());

    }

    @Test
    void runtimeInfoControllerShouldUseExplicitRuntimeInfoServiceInjection() throws Exception {

        Field runtimeInfoServiceField = RuntimeInfoController.class.getDeclaredField("runtimeInfoService");
        Autowired autowired = runtimeInfoServiceField.getAnnotation(Autowired.class);

        Assertions.assertNotNull(autowired);
        Assertions.assertTrue(autowired.required());

    }

    @Test
    void getRuntimeInfoShouldDelegateToRuntimeInfoService() {

        /*
         * O objetivo deste teste e a delegacao do controller. O DTO usado pelo
         * mock deve vir da implementacao Community real para nao duplicar o
         * catalogo visual `...Options` neste teste de camada web.
         */
        RuntimeInfoDTO expectedRuntimeInfoDTO = new CommunityRuntimeInfoService().getRuntimeInfo();

        RuntimeInfoService runtimeInfoService = Mockito.mock(RuntimeInfoService.class);
        Mockito.when(runtimeInfoService.getRuntimeInfo()).thenReturn(expectedRuntimeInfoDTO);

        RuntimeInfoController runtimeInfoController = new RuntimeInfoController();
        ReflectionTestUtils.setField(
                runtimeInfoController,
                "runtimeInfoService",
                runtimeInfoService);

        RuntimeInfoDTO actualRuntimeInfoDTO = runtimeInfoController.getRuntimeInfo();

        Assertions.assertSame(expectedRuntimeInfoDTO, actualRuntimeInfoDTO);
        Mockito.verify(runtimeInfoService).getRuntimeInfo();
        Mockito.verifyNoMoreInteractions(runtimeInfoService);

    }
}
