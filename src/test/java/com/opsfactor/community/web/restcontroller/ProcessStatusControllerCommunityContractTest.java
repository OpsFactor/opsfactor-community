package com.opsfactor.community.web.restcontroller;

import com.opsfactor.community.platform.scheduler.facade.dto.TaskSchedulingDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Congela a superficie Community de status de processos.
 *
 * <p>No Community o scheduler e apenas historico tecnico de execucoes
 * sincronas. Endpoints de fila, cancelamento, workers, jobs recorrentes ou
 * execucao assincrona precisam nascer no overlay Enterprise, com beans e
 * contratos proprios.</p>
 */
public class ProcessStatusControllerCommunityContractTest {

    private static final List<ControllerEndpoint> COMMUNITY_PROCESS_STATUS_ENDPOINTS = List.of(
            new ControllerEndpoint("GET", "api/secured/scheduler/status"),
            new ControllerEndpoint("POST", "api/secured/scheduler/delete"));

    @Test
    public void processStatusControllerShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                ProcessStatusController.class,
                "taskSchedulingService",
                "webControllerTaskSchedulingService");

    }

    @Test
    public void processStatusControllerShouldExposeOnlyCommunityHistoryEndpoints() {

        List<ControllerEndpoint> controllerEndpointList = Arrays
                .stream(ProcessStatusController.class.getDeclaredMethods())
                .flatMap(method -> getControllerEndpointList(method).stream())
                .sorted(Comparator.comparing(ControllerEndpoint::httpMethod).thenComparing(ControllerEndpoint::path))
                .toList();

        Assertions.assertEquals(
                COMMUNITY_PROCESS_STATUS_ENDPOINTS,
                controllerEndpointList,
                "Community deve manter apenas consulta e limpeza de historico de execucoes sincronas.");

    }

    @Test
    public void processStatusReadShouldPreserveLegacyExecutionRolesAndDeleteShouldStayAdminOnly() throws Exception {

        Method getScheduledTaskHistory = ProcessStatusController.class
                .getDeclaredMethod("getScheduledTaskHistory");
        Secured getScheduledTaskHistorySecured = getScheduledTaskHistory.getAnnotation(Secured.class);

        Assertions.assertNotNull(
                getScheduledTaskHistorySecured,
                "A consulta de Process Status deve declarar explicitamente os roles legados.");
        Assertions.assertArrayEquals(
                new String[]{
                        "ROLE_ADMIN",
                        "ROLE_DEMAND_PLANNING_EXECUTION",
                        "ROLE_SUPPLY_PLANNING_EXECUTION"},
                getScheduledTaskHistorySecured.value(),
                "Executores de Demand/Supply devem continuar consultando o Process Status legado.");

        Method deleteScheduledTaskHistory = ProcessStatusController.class
                .getDeclaredMethod("deleteScheduledTaskHistory", List.class);
        Secured deleteScheduledTaskHistorySecured = deleteScheduledTaskHistory.getAnnotation(Secured.class);

        Assertions.assertNotNull(
                deleteScheduledTaskHistorySecured,
                "A exclusao de Process Status deve declarar explicitamente o role administrativo.");
        Assertions.assertArrayEquals(
                new String[]{"ROLE_ADMIN"},
                deleteScheduledTaskHistorySecured.value(),
                "A exclusao de Process Status deve permanecer restrita ao admin.");

    }

    @Test
    public void deleteScheduledTaskHistoryShouldRejectInvalidPayloadBeforeScheduler() {

        ProcessStatusController processStatusController = new ProcessStatusController();

        IllegalArgumentException nullListException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> processStatusController.deleteScheduledTaskHistory(null));

        Assertions.assertEquals(
                "Scheduled task history delete payload list is required",
                nullListException.getMessage());

        IllegalArgumentException emptyListException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> processStatusController.deleteScheduledTaskHistory(List.of()));

        Assertions.assertEquals(
                "At least one scheduled task history row must be selected for deletion.",
                emptyListException.getMessage());

        IllegalArgumentException nullItemException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> processStatusController.deleteScheduledTaskHistory(
                        Arrays.asList(TaskSchedulingDTO.builder()
                                .taskId("task-1")
                                .build(), null)));

        Assertions.assertEquals(
                "Scheduled task history delete payload list cannot contain null value at index 1.",
                nullItemException.getMessage());

        IllegalArgumentException missingTaskIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> processStatusController.deleteScheduledTaskHistory(
                        List.of(new TaskSchedulingDTO())));

        Assertions.assertEquals(
                "Scheduled task history delete payload task id is required.",
                missingTaskIdException.getMessage());

    }

    private static void assertAutowiredFields(Class<?> controllerClass, String... fieldNameArray) throws Exception {

        for (String fieldName : fieldNameArray) {
            Field field = controllerClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);
            Assertions.assertNotNull(
                    autowired,
                    controllerClass.getSimpleName() + "." + fieldName + " deve declarar @Autowired explicitamente.");
            Assertions.assertTrue(
                    autowired.required(),
                    controllerClass.getSimpleName() + "." + fieldName + " deve ser bean obrigatorio.");
        }

    }

    private static List<ControllerEndpoint> getControllerEndpointList(Method method) {

        List<ControllerEndpoint> controllerEndpointList = new ArrayList<>();

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            addControllerEndpointList(controllerEndpointList, "GET", getMapping.value(), getMapping.path());
        }

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            addControllerEndpointList(controllerEndpointList, "POST", postMapping.value(), postMapping.path());
        }

        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            addControllerEndpointList(controllerEndpointList, "PUT", putMapping.value(), putMapping.path());
        }

        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            addControllerEndpointList(controllerEndpointList, "PATCH", patchMapping.value(), patchMapping.path());
        }

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            addControllerEndpointList(controllerEndpointList, "DELETE", deleteMapping.value(), deleteMapping.path());
        }

        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            String httpMethod = getRequestMappingHttpMethod(requestMapping);
            addControllerEndpointList(controllerEndpointList, httpMethod, requestMapping.value(), requestMapping.path());
        }

        return controllerEndpointList;

    }

    private static String getRequestMappingHttpMethod(RequestMapping requestMapping) {

        RequestMethod[] requestMethodArray = requestMapping.method();
        if (requestMethodArray.length == 0) {
            return "REQUEST";
        }

        return Arrays
                .stream(requestMethodArray)
                .map(Enum::name)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .get();

    }

    private static void addControllerEndpointList(
            List<ControllerEndpoint> controllerEndpointList,
            String httpMethod,
            String[] mappingValueArray,
            String[] mappingPathArray) {

        List<String> mappingPathList = getMappingPathList(mappingValueArray, mappingPathArray);
        mappingPathList.forEach(path -> controllerEndpointList.add(new ControllerEndpoint(httpMethod, path)));

    }

    private static List<String> getMappingPathList(String[] mappingValueArray, String[] mappingPathArray) {

        if (mappingValueArray.length > 0) {
            return Arrays.asList(mappingValueArray);
        }
        if (mappingPathArray.length > 0) {
            return Arrays.asList(mappingPathArray);
        }
        return List.of("");

    }

    private record ControllerEndpoint(String httpMethod, String path) {

    }

}
