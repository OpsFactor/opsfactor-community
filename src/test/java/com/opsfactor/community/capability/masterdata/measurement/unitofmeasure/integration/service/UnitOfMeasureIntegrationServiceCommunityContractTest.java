package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.UnidadeMedidaDataUploadDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contratos do service manual de unidades de medida Community.
 *
 * <p>UOM e cadastro base para vendas, estoque, producao, transporte e
 * conversoes. O service deve falhar cedo quando o envelope de integracao ou o
 * snapshot persistido esta quebrado, antes de qualquer escrita em lote.</p>
 */
class UnitOfMeasureIntegrationServiceCommunityContractTest {

    @Test
    void saveDtoListShouldRejectNullDtoListBeforeRepository() {

        UnidadeMedidaIntegrationService unidadeMedidaIntegrationService =
                new UnidadeMedidaIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> unidadeMedidaIntegrationService.saveDTOList(null));

        Assertions.assertEquals(
                "Unit of Measure DTO list is required.",
                dataUploadException.getMessage());

    }

    @Test
    void saveDtoListShouldRejectNullItemBeforeRepository() {

        UnidadeMedidaIntegrationService unidadeMedidaIntegrationService =
                new UnidadeMedidaIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> unidadeMedidaIntegrationService.saveDTOList(Collections.singletonList(null)));

        Assertions.assertEquals(
                "Unit of Measure DTO is null at line 1",
                dataUploadException.getMessage());

    }

    @Test
    void saveDtoListShouldRejectDuplicatedPayloadIdBeforeRepository() {

        UnidadeMedidaIntegrationService unidadeMedidaIntegrationService =
                new UnidadeMedidaIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> unidadeMedidaIntegrationService.saveDTOList(List.of(
                        UnidadeMedidaDataUploadDTO.builder()
                                .id("KG")
                                .description("Kilogram")
                                .build(),
                        UnidadeMedidaDataUploadDTO.builder()
                                .id("KG")
                                .description("Kilogram duplicate")
                                .build())));

        Assertions.assertEquals(
                "Unit of Measure DTO has duplicated id KG at line 2.",
                dataUploadException.getMessage());

    }

    @Test
    void saveDtoListShouldRejectDuplicatedPersistedSnapshotBeforeSaveOrDelete() {

        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        List.of(new UnidadeMedida("KG"), new UnidadeMedida("KG")));
        UnidadeMedidaIntegrationService unidadeMedidaIntegrationService =
                new UnidadeMedidaIntegrationService();

        ReflectionTestUtils.setField(
                unidadeMedidaIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> unidadeMedidaIntegrationService.saveDTOList(List.of(
                        UnidadeMedidaDataUploadDTO.builder()
                                .id("KG")
                                .description("Kilogram")
                                .build())));

        Assertions.assertEquals(
                "Unit of Measure snapshot returned duplicated id KG.",
                dataUploadException.getMessage());

    }

    @Test
    void saveDtoListShouldPreserveSaveDataAccessCause() {

        DataAccessResourceFailureException dataAccessException =
                new DataAccessResourceFailureException("Simulated UOM save failure");
        Map<String, Object> methodResults = new HashMap<>();
        methodResults.put(
                "findAll",
                List.of());
        methodResults.put(
                "saveAll",
                dataAccessException);
        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(UnidadeMedidaRepository.class, methodResults);
        UnidadeMedidaIntegrationService unidadeMedidaIntegrationService =
                new UnidadeMedidaIntegrationService();

        ReflectionTestUtils.setField(
                unidadeMedidaIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> unidadeMedidaIntegrationService.saveDTOList(List.of(
                        UnidadeMedidaDataUploadDTO.builder()
                                .id("KG")
                                .description("Kilogram")
                                .build())));

        Assertions.assertEquals(
                "Error saving Units of Measure : " + dataAccessException,
                dataUploadException.getMessage());
        Assertions.assertSame(
                dataAccessException,
                dataUploadException.getCause());

    }

    @Test
    void saveDtoListShouldPreserveRemoveDataAccessCause() {

        DataAccessResourceFailureException dataAccessException =
                new DataAccessResourceFailureException("Simulated UOM remove failure");
        Map<String, Object> methodResults = new HashMap<>();
        methodResults.put(
                "findAll",
                List.of());
        methodResults.put(
                "saveAll",
                List.of());
        methodResults.put(
                "flush",
                null);
        methodResults.put(
                "deleteAll",
                dataAccessException);
        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(UnidadeMedidaRepository.class, methodResults);
        UnidadeMedidaIntegrationService unidadeMedidaIntegrationService =
                new UnidadeMedidaIntegrationService();

        ReflectionTestUtils.setField(
                unidadeMedidaIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> unidadeMedidaIntegrationService.saveDTOList(List.of(
                        UnidadeMedidaDataUploadDTO.builder()
                                .id("KG")
                                .description("Kilogram")
                                .delete("D")
                                .build())));

        Assertions.assertEquals(
                "Error removing Units of Measure : " + dataAccessException,
                dataUploadException.getMessage());
        Assertions.assertSame(
                dataAccessException,
                dataUploadException.getCause());

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
        return criaRepositoryProxy(
                repositoryClass,
                methodResults);

    }

    @SuppressWarnings("unchecked")
    private static <T> T criaRepositoryProxy(
            Class<T> repositoryClass,
            Map<String, Object> methodResults) {

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
                        Object methodResult = methodResults.get(method.getName());
                        if (methodResult instanceof RuntimeException runtimeException) {
                            throw runtimeException;
                        }
                        return methodResult;
                    }
                    if ("saveAll".equals(method.getName())
                            || "deleteAll".equals(method.getName())
                            || "flush".equals(method.getName())) {
                        throw new AssertionError(
                                repositoryClass.getSimpleName()
                                        + "."
                                        + method.getName()
                                        + " should not be called before snapshot validation.");
                    }
                    throw new UnsupportedOperationException(
                            repositoryClass.getSimpleName()
                                    + "."
                                    + method.getName()
                                    + " is not implemented in this test proxy.");
                });

    }

}
