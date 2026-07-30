package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
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
 * Contratos Community das integracoes de conversao de unidade fisica.
 *
 * <p>Conversoes padrao e por material ficam no Community porque projections e
 * rotinas de Demand/Supply precisam converter quantidades. Precos, custos,
 * peso/volume logistico e qualquer semantica economica permanecem Enterprise.</p>
 */
class UnitOfMeasureConversionIntegrationServicesCommunityContractTest {

    @Test
    void defaultConversionPrimaryKeyShouldRejectDuplicateCompositeKeyBeforeRepositoryLookup() {

        ConversaoUnidadeIntegrationService conversaoUnidadeIntegrationService =
                new ConversaoUnidadeIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> conversaoUnidadeIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(
                                new ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO(
                                        "KG",
                                        "TON"),
                                new ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO(
                                        "KG",
                                        "TON"))));

        Assertions.assertEquals(
                "Default Unit Conversion primary key collection item at index 1 has duplicated key originUomId KG / targetUomId TON.",
                dataUploadException.getMessage());

    }

    @Test
    void defaultConversionPrimaryKeyShouldRejectMissingTargetBeforeRepositoryLookup() {

        ConversaoUnidadeIntegrationService conversaoUnidadeIntegrationService =
                new ConversaoUnidadeIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> conversaoUnidadeIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(new ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO(
                                "KG",
                                null))));

        Assertions.assertEquals(
                "Default Unit Conversion upload primary key must include origin and target UOM",
                dataUploadException.getMessage());

    }

    @Test
    void materialConversionPrimaryKeyShouldRejectDuplicateCompositeKeyBeforeRepositoryLookup() {

        ConversaoUnidadeProdutoIntegrationService conversaoUnidadeProdutoIntegrationService =
                new ConversaoUnidadeProdutoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> conversaoUnidadeProdutoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(
                                new ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO(
                                        "MAT-1",
                                        "KG",
                                        "TON"),
                                new ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO(
                                        "MAT-1",
                                        "KG",
                                        "TON"))));

        Assertions.assertEquals(
                "Material Unit Conversion primary key collection item at index 1 has duplicated key materialId MAT-1 / originUomId KG / targetUomId TON.",
                dataUploadException.getMessage());

    }

    @Test
    void materialConversionPrimaryKeyShouldRejectMissingMaterialBeforeRepositoryLookup() {

        ConversaoUnidadeProdutoIntegrationService conversaoUnidadeProdutoIntegrationService =
                new ConversaoUnidadeProdutoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> conversaoUnidadeProdutoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(new ConversaoUnidadeProdutoIntegrationDataDto.ConversaoUnidadeProdutoPrimaryKeyIntegrationDTO(
                                null,
                                "KG",
                                "TON"))));

        Assertions.assertEquals(
                "Material Unit Conversion upload primary key must include material, origin UOM and target UOM",
                dataUploadException.getMessage());

    }

    @Test
    void conversionPrimaryKeyLookupsShouldAcceptEmptyCollectionWithoutRepositoryLookup() {

        Assertions.assertTrue(new ConversaoUnidadeIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());
        Assertions.assertTrue(new ConversaoUnidadeProdutoIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());

    }

    @Test
    void defaultConversionSupportDataShouldRejectNullUnitOfMeasureSnapshotBeforeIndexing() {

        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        null);
        ConversaoUnidadeIntegrationService conversaoUnidadeIntegrationService =
                new ConversaoUnidadeIntegrationService();

        ReflectionTestUtils.setField(
                conversaoUnidadeIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                conversaoUnidadeIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Unit of Measure snapshot returned null.",
                dataUploadException.getMessage());

    }

    @Test
    void materialConversionSupportDataShouldRejectBrokenMaterialSnapshotBeforeIndexing() {

        ProdutoRepository produtoRepository =
                criaRepositoryProxy(
                        ProdutoRepository.class,
                        "findAll",
                        List.of(new Produto()));
        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        List.of());
        ConversaoUnidadeProdutoIntegrationService conversaoUnidadeProdutoIntegrationService =
                new ConversaoUnidadeProdutoIntegrationService();

        ReflectionTestUtils.setField(
                conversaoUnidadeProdutoIntegrationService,
                "produtoRepository",
                produtoRepository);
        ReflectionTestUtils.setField(
                conversaoUnidadeProdutoIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                conversaoUnidadeProdutoIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Material snapshot returned item without id at index 0.",
                dataUploadException.getMessage());

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
                    throw new UnsupportedOperationException(
                            repositoryClass.getSimpleName()
                                    + "."
                                    + method.getName()
                                    + " is not implemented in this test proxy.");
                });

    }

}
