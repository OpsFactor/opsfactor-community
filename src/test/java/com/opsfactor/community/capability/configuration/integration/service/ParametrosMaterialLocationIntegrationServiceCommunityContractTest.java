package com.opsfactor.community.capability.configuration.integration.service;

import com.opsfactor.community.capability.configuration.integration.dto.ParametrosMaterialLocationIntegrationDataDto;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contratos Community da service de integracao material-location.
 */
class ParametrosMaterialLocationIntegrationServiceCommunityContractTest {

    @Test
    void serviceShouldDeclareComponentAndExplicitBeanFields() throws Exception {

        Assertions.assertTrue(ParametrosMaterialLocationIntegrationService.class.isAnnotationPresent(Component.class));

        assertRequiredAutowiredField("locationService");
        assertRequiredAutowiredField("unidadeMedidaRepository");
        assertRequiredAutowiredField("produtoRepository");
        assertRequiredAutowiredField("parametrosProdutoLocationRepository");
        assertRequiredAutowiredField("parametrosMaterialLocationIntegrationMapper");

    }

    @Test
    void serviceShouldExposeCommunitySaveMessageAndBatchSize() {

        ParametrosMaterialLocationIntegrationService parametrosMaterialLocationIntegrationService =
                new ParametrosMaterialLocationIntegrationService();

        /*
         * O contrato publico usa material-location. Os nomes fisicos legados
         * Produto/ProdutoLocation ficam restritos a entities/repositories.
         */
        Assertions.assertEquals(
                "Material - Location parameters saved",
                parametrosMaterialLocationIntegrationService.getSaveSuccessMessage());
        Assertions.assertEquals(
                5000,
                parametrosMaterialLocationIntegrationService.getBatchSize());

    }

    @Test
    void emptyPrimaryKeyBatchShouldNotTriggerFullTableLookup() {

        ParametrosMaterialLocationIntegrationService parametrosMaterialLocationIntegrationService =
                new ParametrosMaterialLocationIntegrationService();

        Assertions.assertTrue(
                parametrosMaterialLocationIntegrationService
                        .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                        .isEmpty());

    }

    @Test
    void primaryKeyBatchShouldRejectNullItemBeforeRepositoryLookup() {

        ParametrosMaterialLocationIntegrationService parametrosMaterialLocationIntegrationService =
                new ParametrosMaterialLocationIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> parametrosMaterialLocationIntegrationService
                        .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                                Collections.singletonList(null)));

        Assertions.assertEquals(
                "Material-location parameters primary key collection item at index 0 is required.",
                dataUploadException.getMessage());

    }

    @Test
    void primaryKeyBatchShouldRejectIncompletePrimaryKeyBeforeRepositoryLookup() {

        ParametrosMaterialLocationIntegrationService parametrosMaterialLocationIntegrationService =
                new ParametrosMaterialLocationIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> parametrosMaterialLocationIntegrationService
                        .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                                new ParametrosMaterialLocationIntegrationDataDto
                                        .ParametrosMaterialLocationPrimaryKeyIntegrationDTO(
                                        "LOC-1",
                                        " "))));

        Assertions.assertEquals(
                "Material-location parameters upload primary key must include location and material",
                dataUploadException.getMessage());

    }

    @Test
    void primaryKeyBatchShouldRejectDuplicatedMaterialLocationBeforeRepositoryLookup() {

        ParametrosMaterialLocationIntegrationService parametrosMaterialLocationIntegrationService =
                new ParametrosMaterialLocationIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> parametrosMaterialLocationIntegrationService
                        .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                                new ParametrosMaterialLocationIntegrationDataDto
                                        .ParametrosMaterialLocationPrimaryKeyIntegrationDTO(
                                        "LOC-1",
                                        "MAT-1"),
                                new ParametrosMaterialLocationIntegrationDataDto
                                        .ParametrosMaterialLocationPrimaryKeyIntegrationDTO(
                                        "LOC-1",
                                        "MAT-1"))));

        Assertions.assertEquals(
                "Material-location parameters primary key collection item at index 1 has duplicated key locationId LOC-1 / materialId MAT-1.",
                dataUploadException.getMessage());

    }

    @Test
    void supportDataShouldRejectNullLocationSnapshotBeforeIndexing() {

        ParametrosMaterialLocationIntegrationService parametrosMaterialLocationIntegrationService =
                new ParametrosMaterialLocationIntegrationService();

        ReflectionTestUtils.setField(
                parametrosMaterialLocationIntegrationService,
                "locationService",
                new TestLocationService(null));

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                parametrosMaterialLocationIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Location snapshot returned null.",
                dataUploadException.getMessage());

    }

    private static void assertRequiredAutowiredField(String fieldName) throws Exception {

        Field field = ParametrosMaterialLocationIntegrationService.class.getDeclaredField(fieldName);
        Autowired autowired = field.getAnnotation(Autowired.class);

        Assertions.assertNotNull(
                autowired,
                "ParametrosMaterialLocationIntegrationService." + fieldName + " deve usar @Autowired explicito");
        Assertions.assertTrue(
                autowired.required(),
                "ParametrosMaterialLocationIntegrationService." + fieldName + " deve ser bean obrigatorio");

    }

    @SuppressWarnings("unchecked")
    private static <T> T criaRepositoryProxy(
            Class<T> repositoryClass,
            String methodName,
            Object methodResult) {

        Map<String, Object> methodResults = new HashMap<>();
        methodResults.put(
                methodName,
                methodResult);

        return (T) Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass().equals(Object.class)) {
                        return switch (method.getName()) {
                            case "toString" -> repositoryClass.getSimpleName() + " test proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(
                                    "Metodo Object nao suportado no teste: " + method.getName());
                        };
                    }
                    if (methodResults.containsKey(method.getName())) {
                        return methodResults.get(method.getName());
                    }
                    if ("findAll".equals(method.getName())) {
                        throw new AssertionError(
                                repositoryClass.getSimpleName()
                                        + ".findAll should not be called in this contract test.");
                    }
                    if ("saveAll".equals(method.getName())) {
                        throw new AssertionError(
                                repositoryClass.getSimpleName()
                                        + ".saveAll should not be called before material-location snapshot validation.");
                    }
                    throw new UnsupportedOperationException(
                            repositoryClass.getSimpleName()
                                    + "."
                                    + method.getName()
                                    + " is not implemented in this test proxy.");
                });

    }

    private static ParametrosProdutoLocation criaParametrosProdutoLocation(
            String locationId,
            String materialId) {

        return new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(
                        new Produto(materialId),
                        new Location(locationId)));

    }

    private static class TestLocationService extends LocationService {

        private final List<Location> locations;

        private TestLocationService(List<Location> locations) {

            this.locations = locations;

        }

        @Override
        public List<Location> findAllWithoutDefault() {

            return locations;

        }

    }

}
