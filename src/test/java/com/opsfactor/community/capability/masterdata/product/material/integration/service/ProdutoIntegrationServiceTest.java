package com.opsfactor.community.capability.masterdata.product.material.integration.service;

import com.opsfactor.community.capability.masterdata.network.location.integration.dto.LocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.product.material.integration.dto.ProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.network.location.integration.service.LocationIntegrationService;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contratos publicos pequenos da integracao de materiais.
 *
 * <p>As entidades fisicas ainda usam `Produto`, mas mensagens e bordas
 * Community devem falar em material para manter a nomenclatura escolhida para
 * o repositorio novo.</p>
 */
class ProdutoIntegrationServiceTest {

    @Test
    void saveSuccessMessageShouldUseMaterialNaming() {

        ProdutoIntegrationService produtoIntegrationService = new ProdutoIntegrationService();

        Assertions.assertEquals(
                "Material data saved",
                produtoIntegrationService.getSaveSuccessMessage());

    }

    @Test
    void materialPrimaryKeyLookupShouldRejectDuplicatedIdBeforeRepository() {

        ProdutoIntegrationService produtoIntegrationService = new ProdutoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> produtoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO("MAT-1"),
                        new ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO("MAT-1"))));

        Assertions.assertEquals(
                "Material primary key collection item at index 1 has duplicated materialId MAT-1.",
                dataUploadException.getMessage());

    }

    @Test
    void materialPrimaryKeyLookupShouldRejectBlankIdBeforeRepository() {

        ProdutoIntegrationService produtoIntegrationService = new ProdutoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> produtoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO(" "))));

        Assertions.assertEquals(
                "materialId is required.",
                dataUploadException.getMessage());

    }

    @Test
    void locationPrimaryKeyLookupShouldRejectDuplicatedIdBeforeRepository() {

        LocationIntegrationService locationIntegrationService = new LocationIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> locationIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO("LOC-1"),
                        new LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO("LOC-1"))));

        Assertions.assertEquals(
                "Location primary key collection item at index 1 has duplicated locationId LOC-1.",
                dataUploadException.getMessage());

    }

    @Test
    void locationPrimaryKeyLookupShouldRejectBlankIdBeforeRepository() {

        LocationIntegrationService locationIntegrationService = new LocationIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> locationIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new LocationIntegrationDataDto.LocationPrimaryKeyIntegrationDTO(" "))));

        Assertions.assertEquals(
                "locationId is required.",
                dataUploadException.getMessage());

    }

    @Test
    void locationSupportDataShouldRejectNullUnitOfMeasureSnapshotBeforeIndexing() {

        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        null);
        LocationIntegrationService locationIntegrationService =
                new LocationIntegrationService();

        ReflectionTestUtils.setField(
                locationIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                locationIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Unit of Measure snapshot returned null.",
                dataUploadException.getMessage());

    }

    @Test
    void materialSupportDataShouldRejectBrokenUnitOfMeasureSnapshotBeforeIndexing() {

        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        List.of(new UnidadeMedida()));
        ProdutoIntegrationService produtoIntegrationService =
                new ProdutoIntegrationService();

        ReflectionTestUtils.setField(
                produtoIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                produtoIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Unit of Measure snapshot returned item without id at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void materialFullExportShouldUseTheUnitOfMeasureFetchSnapshot() {

        Produto material = new Produto("MAT-1");
        ProdutoRepository produtoRepository = criaRepositoryProxy(
                ProdutoRepository.class,
                "findAllWithUnitOfMeasures",
                List.of(material));
        ProdutoIntegrationService produtoIntegrationService = new ProdutoIntegrationService();
        ReflectionTestUtils.setField(
                produtoIntegrationService,
                "produtoRepository",
                produtoRepository);

        Assertions.assertEquals(
                List.of(material),
                produtoIntegrationService.getAllPersistedEntities());

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
                    if ("saveAll".equals(method.getName())) {
                        throw new AssertionError(
                                repositoryClass.getSimpleName()
                                        + ".saveAll should not be called before deactivation snapshot validation.");
                    }
                    throw new UnsupportedOperationException(
                            repositoryClass.getSimpleName()
                                    + "."
                                    + method.getName()
                                    + " is not implemented in this test proxy.");
                });

    }

}
