package com.opsfactor.community.web.restcontroller.masterdata;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Contrato da superficie REST de master data Community.
 *
 * <p>Master data Community cobre material, location, UOM, malha/transporte
 * basico, producao operacional e clustering simples. Warehouses, frotas,
 * last mile, mapa/GIS, caracteristicas dinamicas, filtros/agregadores,
 * custos/precos e visibilidade/distribuicao pertencem ao Enterprise.</p>
 */
public class MasterdataControllersCommunityContractTest {

    @Test
    public void materialControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                MaterialRestController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/material"),
                        new ControllerEndpoint("GET", "api/secured/material/cluster"),
                        new ControllerEndpoint("GET", "api/secured/material/cluster/{clusterId}/materials"),
                        new ControllerEndpoint("GET", "api/secured/material/status"),
                        new ControllerEndpoint("GET", "api/secured/product")));

    }

    @Test
    public void locationControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                LocationRestController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/location"),
                        new ControllerEndpoint("GET", "api/secured/location/cluster"),
                        new ControllerEndpoint("GET", "api/secured/location/cluster/{clusterLocationsId}/locations"),
                        new ControllerEndpoint("GET", "api/secured/location/internal"),
                        new ControllerEndpoint("GET", "api/secured/location/internalandsupplier"),
                        new ControllerEndpoint("GET", "api/secured/location/supplyplanning"),
                        new ControllerEndpoint("GET", "api/secured/location/{id}"),
                        new ControllerEndpoint("POST", "api/secured/location")));

    }

    @Test
    public void unitOfMeasureControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                UnidadeMedidaRestController.class,
                List.of(
                        new ControllerEndpoint(
                                "GET",
                                "api/secured/alerts/uomconversiongaps/deployment/{supplyPlanId}"),
                        new ControllerEndpoint(
                                "GET",
                                "api/secured/alerts/uomconversiongaps/dp/{demandPlanningExecutionProfileId}/{referenceDate}"),
                        new ControllerEndpoint(
                                "GET",
                                "api/secured/alerts/uomconversiongaps/snp/{referenceDate}/{bucketSize}/{supplyNetworkVersionId}/{snpExecutionProfileId}/{demandPlanVersionId}"),
                        new ControllerEndpoint(
                                "GET",
                                "api/secured/unitofmeasure/conversiondetail/{materialId}/{originUomId}/{targetUomId}"),
                        new ControllerEndpoint(
                                "GET",
                                "api/secured/unitofmeasure/conversiondetail/{originUomId}/{targetUomId}"),
                        new ControllerEndpoint("GET", "api/secured/unitofmeasure/findids")));

    }

    @Test
    public void transportationLaneControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                LinhaTransporteController.class,
                List.of(
                        new ControllerEndpoint("DELETE", "api/secured/supplynetwork/transportationline/delete"),
                        new ControllerEndpoint("DELETE", "api/secured/supplynetwork/transportationlinematerial/delete"),
                        new ControllerEndpoint("GET", "api/secured/supplynetwork/transportationline/get/{versaoMalhaId}"),
                        new ControllerEndpoint("GET", "api/secured/supplynetwork/transportationlinematerial/get/{versaoMalhaId}"),
                        new ControllerEndpoint("GET", "api/secured/supplynetwork/version"),
                        new ControllerEndpoint("POST", "api/secured/supplynetwork/transportationline/update"),
                        new ControllerEndpoint("POST", "api/secured/supplynetwork/transportationlinematerial/update"),
                        new ControllerEndpoint("POST", "api/secured/supplynetwork/version")));

    }

    @Test
    public void productionControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                ProductionRestController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/production/billofmaterials"),
                        new ControllerEndpoint("GET", "api/secured/production/billofmaterialscomponents"),
                        new ControllerEndpoint("GET", "api/secured/production/productionresource"),
                        new ControllerEndpoint("GET", "api/secured/production/routing"),
                        new ControllerEndpoint("GET", "api/secured/production/routing/inconsistencies"),
                        new ControllerEndpoint("GET", "api/secured/production/routingoperation"),
                        new ControllerEndpoint("POST", "api/secured/production/productionresource/save")));

    }

    @Test
    public void clusteringControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                ClusteringRestController.class,
                List.of(
                        new ControllerEndpoint("DELETE", "api/secured/clustering/location/criteria"),
                        new ControllerEndpoint("DELETE", "api/secured/clustering/material/criteria"),
                        new ControllerEndpoint("DELETE", "api/secured/locationclustering/delete"),
                        new ControllerEndpoint("DELETE", "api/secured/materialclustering/delete"),
                        new ControllerEndpoint("GET", "api/secured/clustering/location/allocation"),
                        new ControllerEndpoint("GET", "api/secured/clustering/location/criteria"),
                        new ControllerEndpoint("GET", "api/secured/clustering/material/allocation"),
                        new ControllerEndpoint("GET", "api/secured/clustering/material/criteria"),
                        new ControllerEndpoint("GET", "api/secured/locationclustering"),
                        new ControllerEndpoint("GET", "api/secured/locationclustering/{id}"),
                        new ControllerEndpoint("GET", "api/secured/materialclustering"),
                        new ControllerEndpoint("GET", "api/secured/materialclustering/DFU"),
                new ControllerEndpoint("GET", "api/secured/materialclustering/{id}"),
                        new ControllerEndpoint("POST", "api/secured/locationclustering/save"),
                        new ControllerEndpoint("POST", "api/secured/materialclustering/save")));

    }

    @Test
    public void masterdataControllersShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                MaterialRestController.class,
                List.of("materialDtoService"));
        assertRequiredAutowiredFields(
                LocationRestController.class,
                List.of(
                        "clusterLocationDtoService",
                        "locationFrontService"));
        assertRequiredAutowiredFields(
                UnidadeMedidaRestController.class,
                List.of(
                        "unidadeMedidaRepository",
                        "unidadeConversaoFrontService"));
        assertRequiredAutowiredFields(
                ClusteringRestController.class,
                List.of("clusteringFrontService"));
        assertRequiredAutowiredFields(
                LinhaTransporteController.class,
                List.of("linhaTransporteFrontService"));
        assertRequiredAutowiredFields(
                ProductionRestController.class,
                List.of(
                        "roteiroFrontService",
                        "listaTecnicaFrontService",
                        "recursoProdutivoFrontService"));

    }

    @Test
    public void clusteringControllerShouldAcceptOnlyCanonicalMaterialClusterRequestParameter() throws Exception {

        Method getMaterialLocationDfuListMethod = ClusteringRestController.class.getDeclaredMethod(
                "getMaterialLocationDfuList",
                Long.class,
                Long.class,
                LocalDate.class);

        RequestParam materialClusterRequestParam = getMaterialLocationDfuListMethod
                .getParameters()[0]
                .getAnnotation(RequestParam.class);

        Assertions.assertNotNull(materialClusterRequestParam);
        Assertions.assertEquals("materialClusterId", materialClusterRequestParam.value());
        Assertions.assertFalse(
                Arrays.stream(getMaterialLocationDfuListMethod.getParameters())
                        .map(parameter -> parameter.getAnnotation(RequestParam.class))
                        .filter(Objects::nonNull)
                        .map(RequestParam::value)
                        .anyMatch("productClusterId"::equals),
                "Community must not accept the historic productClusterId request parameter.");

    }

    private static void assertControllerEndpoints(
            Class<?> controllerClass,
            List<ControllerEndpoint> expectedControllerEndpointList) {

        List<ControllerEndpoint> controllerEndpointList = Arrays
                .stream(controllerClass.getDeclaredMethods())
                .flatMap(MasterdataControllersCommunityContractTest::getControllerEndpoints)
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
            return Stream.concat(Arrays.stream(valueArray), Arrays.stream(pathArray))
                    .map(MasterdataControllersCommunityContractTest::normalizeEndpointPath)
                    .distinct();
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(
                    "Nao foi possivel ler paths de " + annotationClass.getSimpleName(),
                    reflectiveOperationException);
        }

    }

    private static void assertRequiredAutowiredFields(
            Class<?> controllerClass,
            List<String> fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = controllerClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    controllerClass.getSimpleName() + "." + fieldName + " must declare @Autowired explicitly");
            Assertions.assertTrue(
                    autowired.required(),
                    controllerClass.getSimpleName() + "." + fieldName + " must be a required Spring bean");
        }

    }

    private static String normalizeEndpointPath(String endpointPath) {

        return endpointPath.startsWith("/") ? endpointPath.substring(1) : endpointPath;

    }

    private record ControllerEndpoint(String httpMethod, String path) {

    }

}
