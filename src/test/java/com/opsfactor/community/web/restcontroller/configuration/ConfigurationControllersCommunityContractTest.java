package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.configuration.facade.dto.ParametrosMaterialLocationDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Contrato da superficie REST de configuracao Community.
 *
 * <p>A configuracao Community cobre parametros globais permitidos, perfis de
 * execucao DP/SNP recortados, parametros material/location simples, safety
 * stock operacional e views/configs de usuario. Auto-fit, process chain,
 * filtros/agregadores, location level, inventory policy optimization e
 * configuracoes Enterprise devem aparecer em controllers Enterprise.</p>
 */
public class ConfigurationControllersCommunityContractTest {

    @Test
    public void globalParametersControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                ParametrosGlobaisController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/configs/parameters"),
                        new ControllerEndpoint("POST", "api/secured/configs/parameters")));

    }

    @Test
    public void globalParametersControllerShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                ParametrosGlobaisController.class,
                "parametrosGlobaisFrontService",
                "unidadeMedidaService",
                "parametrosGlobaisControllerPolicy");

    }

    @Test
    public void demandExecutionProfileControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                PerfilExecucaoDemandPlanController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/demandplanexecutionprofile"),
                        new ControllerEndpoint("POST", "api/secured/demandplanexecutionprofile")));

    }

    @Test
    public void supplyExecutionProfileControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                PerfilExecucaoSupplyPlanController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/supplyplanexecutionprofile"),
                        new ControllerEndpoint("POST", "api/secured/supplyplanexecutionprofile")));

    }

    @Test
    public void demandExecutionProfileControllerShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                PerfilExecucaoDemandPlanController.class,
                "perfilExecucaoDemandPlanFrontService");

    }

    @Test
    public void supplyExecutionProfileControllerShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                PerfilExecucaoSupplyPlanController.class,
                "perfilExecucaoSupplyPlanFrontService");

    }

    @Test
    public void materialLocationParametersControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                ParametroMaterialLocationController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/configs/parametros/clusterLocation"),
                        new ControllerEndpoint("GET", "api/secured/configs/parametros/locationList"),
                        new ControllerEndpoint("GET", "api/secured/configs/parametros/material"),
                        new ControllerEndpoint("GET", "api/secured/configs/parametros/materialLocation/{location}"),
                        new ControllerEndpoint("POST", "api/secured/configs/parametros/clusterLocation"),
                        new ControllerEndpoint("POST", "api/secured/configs/parametros/material"),
                        new ControllerEndpoint("POST", "api/secured/configs/parametros/materialLocation")));

    }

    @Test
    public void materialLocationParametersControllerShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                ParametroMaterialLocationController.class,
                "parametrosFrontService");

    }

    @Test
    public void materialLocationParametersControllerShouldRejectNegativeFrozenDemandPlanningHorizonAtHttpBoundary() {

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO =
                new ParametrosMaterialLocationDTO();
        parametrosMaterialLocationDTO.setFrozenHorizonDpInDays(-1);

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                new ParametroMaterialLocationController()
                        .pushParametrosMaterialLocation(parametrosMaterialLocationDTO)
                        .getStatusCode());

    }

    @Test
    public void materialLocationParametersControllerShouldRejectInvalidProductionQuantitiesAtHttpBoundary() {

        ParametrosMaterialLocationDTO parametrosComMinimoInvalido =
                new ParametrosMaterialLocationDTO();
        parametrosComMinimoInvalido.setProductionMinimumQuantity(-0.1d);

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                new ParametroMaterialLocationController()
                        .pushParametrosMaterialLocation(parametrosComMinimoInvalido)
                        .getStatusCode());

        ParametrosMaterialLocationDTO parametrosComMultiploInvalido =
                new ParametrosMaterialLocationDTO();
        parametrosComMultiploInvalido.setProductionMultipleQuantity(0.0d);

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                new ParametroMaterialLocationController()
                        .pushParametrosMaterialLocation(parametrosComMultiploInvalido)
                        .getStatusCode());

    }

    @Test
    public void inventoryPolicyControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                PoliticaEstoquesController.class,
                List.of(
                        new ControllerEndpoint("DELETE", "api/secured/configs/inventorypolicy/{inventoryPolicyId}"),
                        new ControllerEndpoint("GET", "api/secured/configs/inventorypolicy"),
                        new ControllerEndpoint("GET", "api/secured/configs/inventorypolicy/{inventoryPolicyId}"),
                        new ControllerEndpoint("POST", "api/secured/configs/inventorypolicy")));

    }

    @Test
    public void inventoryPolicyControllerShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                PoliticaEstoquesController.class,
                "politicaEstoquesFrontService");

    }

    @Test
    public void userConfigurationControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                UserConfigurationController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/configuration/user/userconfigs/{tema}"),
                        new ControllerEndpoint("GET", "api/secured/configuration/user/view/demandplanningbook"),
                        new ControllerEndpoint("GET", "api/secured/configuration/user/view/demandplanningbook/new/{userId}/{viewName}"),
                        new ControllerEndpoint("GET", "api/secured/configuration/user/view/demandplanningbook/{userId}"),
                        new ControllerEndpoint("GET", "api/secured/configuration/user/view/supplyplanningbook"),
                        new ControllerEndpoint("GET", "api/secured/configuration/user/view/supplyplanningbook/new/{userId}/{viewName}"),
                        new ControllerEndpoint("GET", "api/secured/configuration/user/view/supplyplanningbook/{userId}"),
                        new ControllerEndpoint("POST", "api/secured/configuration/user/userconfigs"),
                        new ControllerEndpoint("POST", "api/secured/configuration/user/view"),
                        new ControllerEndpoint("POST", "api/secured/configuration/user/view/delete"),
                        new ControllerEndpoint("POST", "api/secured/configuration/user/view/list"),
                        new ControllerEndpoint("POST", "api/secured/configuration/user/view/new")));

    }

    @Test
    public void userConfigurationControllerShouldNotExposeEnterpriseInterfacePreferences() {

        List<String> endpointPaths = Arrays.stream(UserConfigurationController.class.getDeclaredMethods())
                .flatMap(ConfigurationControllersCommunityContractTest::getControllerEndpoints)
                .map(ControllerEndpoint::path)
                .toList();

        /*
         * Community inicia e permanece no tema claro fixo. A preferência
         * individual de interface é fornecida somente pelo controller
         * Enterprise, portanto esta rota não pode voltar ao artefato aberto.
         */
        Assertions.assertTrue(endpointPaths.stream().noneMatch(path -> path.contains("interface/preferences")));

    }

    @Test
    public void userConfigurationControllerShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                UserConfigurationController.class,
                "configuredViewFrontService",
                "configuracaoUsuarioFrontService",
                "authenticationService");

    }

    private static void assertControllerEndpoints(
            Class<?> controllerClass,
            List<ControllerEndpoint> expectedControllerEndpointList) {

        List<ControllerEndpoint> controllerEndpointList = Arrays
                .stream(controllerClass.getDeclaredMethods())
                .flatMap(ConfigurationControllersCommunityContractTest::getControllerEndpoints)
                .sorted(Comparator.comparing(ControllerEndpoint::httpMethod).thenComparing(ControllerEndpoint::path))
                .toList();

        Assertions.assertEquals(
                expectedControllerEndpointList,
                controllerEndpointList,
                controllerClass.getSimpleName() + " possui endpoint fora do recorte Community aprovado.");

    }

    private static Stream<ControllerEndpoint> getControllerEndpoints(Method method) {

        return Stream.of(
                        getDirectEndpointPaths(method, GetMapping.class).map(path -> new ControllerEndpoint("GET", path)),
                        getDirectEndpointPaths(method, PostMapping.class).map(path -> new ControllerEndpoint("POST", path)),
                        getDirectEndpointPaths(method, PutMapping.class).map(path -> new ControllerEndpoint("PUT", path)),
                        getDirectEndpointPaths(method, DeleteMapping.class).map(path -> new ControllerEndpoint("DELETE", path)),
                        getDirectEndpointPaths(method, PatchMapping.class).map(path -> new ControllerEndpoint("PATCH", path)),
                        getDirectEndpointPaths(method, RequestMapping.class).map(path -> new ControllerEndpoint("REQUEST", path)))
                .flatMap(controllerEndpointStream -> controllerEndpointStream);

    }

    private static <T extends Annotation> Stream<String> getDirectEndpointPaths(
            Method method,
            Class<T> annotationClass) {

        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null) return Stream.empty();

        try {
            String[] valueArray = (String[]) annotationClass.getMethod("value").invoke(annotation);
            String[] pathArray = (String[]) annotationClass.getMethod("path").invoke(annotation);
            return Stream.concat(Arrays.stream(valueArray), Arrays.stream(pathArray)).distinct();
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(
                    "Nao foi possivel ler paths de " + annotationClass.getSimpleName(),
                    reflectiveOperationException);
        }

    }

    private static void assertAutowiredFields(
            Class<?> controllerClass,
            String... fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = controllerClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    controllerClass.getSimpleName() + "." + fieldName + " deve usar @Autowired explicito");
            Assertions.assertTrue(
                    autowired.required(),
                    controllerClass.getSimpleName() + "." + fieldName + " deve ser bean obrigatorio");
        }

    }

    private record ControllerEndpoint(String httpMethod, String path) {

    }

}
