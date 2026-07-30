package com.opsfactor.community.platform.scheduler.facade;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.scheduler.services.Task;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Garante que o Community nao habilite processamento assíncrono/batch por
 * configuracao externa. Filas, consumers e batch runners devem voltar apenas
 * no overlay Enterprise.
 */
public class WebControllerTaskSchedulingServiceTest {

    @Test
    public void webControllerTaskSchedulingServiceShouldUseExplicitAutowiredBeanFields() throws Exception {

        List<String> autowiredFieldNameList = Arrays
                .stream(WebControllerTaskSchedulingService.class.getDeclaredFields())
                .filter(field -> field.getAnnotation(Autowired.class) != null)
                .map(Field::getName)
                .toList();

        Assertions.assertEquals(
                List.of("taskSchedulingService", "parametrosGlobaisService", "authenticationService"),
                autowiredFieldNameList,
                "A fachada web deve deixar explicitos somente os beans obrigatorios do scheduler Community.");
        assertAutowiredFields(
                WebControllerTaskSchedulingService.class,
                "taskSchedulingService",
                "parametrosGlobaisService",
                "authenticationService");

    }

    @Test
    public void validaModoExecucaoProcessoCommunityShouldAcceptSyncExecution() throws Exception {

        WebControllerTaskSchedulingService webControllerTaskSchedulingService = new WebControllerTaskSchedulingService();

        invokeValidation(
                webControllerTaskSchedulingService,
                Constantes.ModoExecucaoProcesso.SYNC);

    }

    @Test
    public void validaModoExecucaoProcessoCommunityShouldRejectNullExecutionModeAsInvalidPayload() throws Exception {

        WebControllerTaskSchedulingService webControllerTaskSchedulingService = new WebControllerTaskSchedulingService();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidation(
                        webControllerTaskSchedulingService,
                        null));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Modo de execucao do processo nao pode ser nulo",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaModoExecucaoProcessoCommunityShouldRejectAsyncExecution() throws Exception {

        WebControllerTaskSchedulingService webControllerTaskSchedulingService = new WebControllerTaskSchedulingService();

        assertRequiresEnterpriseVersionException(
                webControllerTaskSchedulingService,
                Constantes.ModoExecucaoProcesso.ASYNC);

    }

    @Test
    public void validaModoExecucaoProcessoCommunityShouldRejectBatchExecution() throws Exception {

        WebControllerTaskSchedulingService webControllerTaskSchedulingService = new WebControllerTaskSchedulingService();

        assertRequiresEnterpriseVersionException(
                webControllerTaskSchedulingService,
                Constantes.ModoExecucaoProcesso.BATCH);

    }

    @Test
    public void runImediatoShouldRejectAsyncBeforeGlobalParametersAccess() {

        WebControllerTaskSchedulingService webControllerTaskSchedulingService = new WebControllerTaskSchedulingService();

        /*
         * O teste instancia a fachada sem injetar ParametrosGlobaisService de proposito.
         * Se a ordem do metodo voltar a consultar timezone/usuario antes do gate Community,
         * o erro vira falha generica e ASYNC/BATCH deixa de falhar como feature Enterprise.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> webControllerTaskSchedulingService.runImediato(
                        null,
                        null,
                        "TestProcess",
                        "Async blocked before Spring bean access",
                        Constantes.ModoExecucaoProcesso.ASYNC));

    }

    @Test
    public void runImediatoShouldRejectBatchBeforeGlobalParametersAccess() {

        WebControllerTaskSchedulingService webControllerTaskSchedulingService = new WebControllerTaskSchedulingService();

        /*
         * BATCH segue a mesma ordem de ASYNC: Community deve falhar no gate de
         * edicao antes de consultar ParametrosGlobaisService, usuario ou
         * scheduler. O artefato job existe apenas no Enterprise.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> webControllerTaskSchedulingService.runImediato(
                        null,
                        null,
                        "TestProcess",
                        "Batch blocked before Spring bean access",
                        Constantes.ModoExecucaoProcesso.BATCH));

    }

    @Test
    public void runImediatoListShouldStopAtFirstSyncError() {

        TestableWebControllerTaskSchedulingService webControllerTaskSchedulingService =
                new TestableWebControllerTaskSchedulingService();

        ResponseEntity<ResponseDTO> responseEntity = webControllerTaskSchedulingService.runImediato(
                null,
                List.of("ok", "fail", "after-failure"),
                "TestProcess",
                value -> value,
                Constantes.ModoExecucaoProcesso.SYNC);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        Assertions.assertEquals(2, webControllerTaskSchedulingService.executions.get());

    }

    @Test
    public void runImediatoListShouldRejectInvalidPayloadListBeforeAnyExecution() {

        TestableWebControllerTaskSchedulingService webControllerTaskSchedulingService =
                new TestableWebControllerTaskSchedulingService();

        IllegalArgumentException nullListException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> webControllerTaskSchedulingService.runImediato(
                        null,
                        null,
                        "TestProcess",
                        value -> String.valueOf(value),
                        Constantes.ModoExecucaoProcesso.SYNC));

        Assertions.assertEquals(
                "Immediate Community task payload list is required",
                nullListException.getMessage());
        Assertions.assertEquals(0, webControllerTaskSchedulingService.executions.get());

        IllegalArgumentException emptyListException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> webControllerTaskSchedulingService.runImediato(
                        null,
                        List.of(),
                        "TestProcess",
                        value -> String.valueOf(value),
                        Constantes.ModoExecucaoProcesso.SYNC));

        Assertions.assertEquals(
                "Immediate Community task payload list cannot be empty",
                emptyListException.getMessage());
        Assertions.assertEquals(0, webControllerTaskSchedulingService.executions.get());

        IllegalArgumentException nullItemException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> webControllerTaskSchedulingService.runImediato(
                        null,
                        Arrays.asList("ok", null),
                        "TestProcess",
                        value -> String.valueOf(value),
                        Constantes.ModoExecucaoProcesso.SYNC));

        Assertions.assertEquals(
                "Immediate Community task payload list cannot contain null value at index 1",
                nullItemException.getMessage());
        Assertions.assertEquals(0, webControllerTaskSchedulingService.executions.get());

    }

    @Test
    public void respostaExecucaoProcessoShouldPreserveLegacySyncAndBackgroundMessages() {

        WebControllerTaskSchedulingService webControllerTaskSchedulingService =
                new WebControllerTaskSchedulingService();

        ResponseEntity<ResponseDTO> syncResponseEntity =
                webControllerTaskSchedulingService.getRespostaExecucaoProcesso(
                        "Demand Planning",
                        Constantes.ModoExecucaoProcesso.SYNC);
        ResponseEntity<ResponseDTO> asyncResponseEntity =
                webControllerTaskSchedulingService.getRespostaExecucaoProcesso(
                        "Demand Planning",
                        Constantes.ModoExecucaoProcesso.ASYNC);
        ResponseEntity<ResponseDTO> batchResponseEntity =
                webControllerTaskSchedulingService.getRespostaExecucaoProcesso(
                        "Demand Planning",
                        Constantes.ModoExecucaoProcesso.BATCH);

        Assertions.assertEquals(HttpStatus.OK, syncResponseEntity.getStatusCode());
        Assertions.assertEquals(
                "Demand Planning executed successfully",
                syncResponseEntity.getBody().getMessage());
        Assertions.assertEquals(
                "Demand Planning executing in background. The process status can be followed in Processes -> Process Status",
                asyncResponseEntity.getBody().getMessage());
        Assertions.assertEquals(
                asyncResponseEntity.getBody().getMessage(),
                batchResponseEntity.getBody().getMessage());

    }

    @Test
    public void runImediatoSyncShouldUseAuthenticatedUserFromAuthenticationService() {

        WebControllerTaskSchedulingService webControllerTaskSchedulingService =
                new WebControllerTaskSchedulingService();
        TaskSchedulingService taskSchedulingService = Mockito.mock(TaskSchedulingService.class);
        ParametrosGlobaisService parametrosGlobaisService = Mockito.mock(ParametrosGlobaisService.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();

        parametrosGlobais.setTimeZone("America/Sao_Paulo");
        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(parametrosGlobaisService.getParametrosGlobais()).thenReturn(parametrosGlobais);
        Mockito.when(taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoSincronoComSupplier(
                        Mockito.any(),
                        Mockito.eq("SaveStock"),
                        Mockito.eq("admin"),
                        Mockito.isNull(),
                        Mockito.eq("America/Sao_Paulo")))
                .thenReturn("Data saved");
        setCommonFields(
                webControllerTaskSchedulingService,
                taskSchedulingService,
                parametrosGlobaisService,
                authenticationService);

        ResponseEntity<ResponseDTO> responseEntity = webControllerTaskSchedulingService.runImediatoSync(
                () -> "ignored by mocked scheduler",
                "SaveStock");

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Mockito.verify(authenticationService).getAuthenticatedUserId();
        Mockito.verify(taskSchedulingService).criaSalvaEExecutaScheduledTaskImediatoSincronoComSupplier(
                Mockito.any(),
                Mockito.eq("SaveStock"),
                Mockito.eq("admin"),
                Mockito.isNull(),
                Mockito.eq("America/Sao_Paulo"));

    }

    private static void assertRequiresEnterpriseVersionException(
            WebControllerTaskSchedulingService webControllerTaskSchedulingService,
            Constantes.ModoExecucaoProcesso modoExecucaoProcesso) throws Exception {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidation(
                        webControllerTaskSchedulingService,
                        modoExecucaoProcesso));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    private static void invokeValidation(
            WebControllerTaskSchedulingService webControllerTaskSchedulingService,
            Constantes.ModoExecucaoProcesso modoExecucaoProcesso) throws Exception {

        Method validationMethod = WebControllerTaskSchedulingService.class.getDeclaredMethod(
                "validaModoExecucaoProcessoCommunity",
                Constantes.ModoExecucaoProcesso.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                webControllerTaskSchedulingService,
                modoExecucaoProcesso);

    }

    private static void assertAutowiredFields(Class<?> serviceClass, String... fieldNameArray) throws Exception {

        for (String fieldName : fieldNameArray) {
            Field field = serviceClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);
            Assertions.assertNotNull(
                    autowired,
                    serviceClass.getSimpleName() + "." + fieldName + " deve declarar @Autowired explicitamente.");
            Assertions.assertTrue(
                    autowired.required(),
                    serviceClass.getSimpleName() + "." + fieldName + " deve ser bean obrigatorio.");
        }

    }

    private static void setCommonFields(
            WebControllerTaskSchedulingService webControllerTaskSchedulingService,
            TaskSchedulingService taskSchedulingService,
            ParametrosGlobaisService parametrosGlobaisService,
            AuthenticationService authenticationService) {

        ReflectionTestUtils.setField(
                webControllerTaskSchedulingService,
                "taskSchedulingService",
                taskSchedulingService);
        ReflectionTestUtils.setField(
                webControllerTaskSchedulingService,
                "parametrosGlobaisService",
                parametrosGlobaisService);
        ReflectionTestUtils.setField(
                webControllerTaskSchedulingService,
                "authenticationService",
                authenticationService);

    }

    private static class TestableWebControllerTaskSchedulingService extends WebControllerTaskSchedulingService {

        private final AtomicInteger executions = new AtomicInteger();

        @Override
        public <A, S, T extends Task<A, S>> ResponseEntity<ResponseDTO> runImediato(
                Class<T> taskClass,
                A dtoParametros,
                String tipoProcesso,
                String descricaoExecucao,
                Constantes.ModoExecucaoProcesso modoExecucaoProcesso) {

            executions.incrementAndGet();
            if ("fail".equals(dtoParametros)) {
                return ResponseDTO.getResponseEntity("failed", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return ResponseDTO.getResponseEntity("ok", HttpStatus.OK);

        }

    }

}
