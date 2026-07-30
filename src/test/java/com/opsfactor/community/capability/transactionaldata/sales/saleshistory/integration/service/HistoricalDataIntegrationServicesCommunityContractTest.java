package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.integration.service;

import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationFiltroDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationDataDto;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationDataDto;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationFiltroDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.service.EstoqueIntegrationService;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.service.SelloutIntegrationService;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.repository.EstoqueRepository;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.repository.SelloutRepository;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contratos das integracoes transacionais Community.
 *
 * <p>O Community carrega somente estoque inicial/snapshot e historico
 * sell-out. Sell-in, sales orders, estoque por lote produtivo, pedidos,
 * campanhas/eventos e valores economicos permanecem em overlays Enterprise.</p>
 */
class HistoricalDataIntegrationServicesCommunityContractTest {

    @Test
    void historicalDataIntegrationServicesShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                EstoqueIntegrationService.class,
                List.of(
                        "estoqueRepository",
                        "locationRepository",
                        "materialRepository",
                        "unidadeMedidaRepository",
                        "estoqueIntegrationMapper"));

        assertRequiredAutowiredFields(
                SelloutIntegrationService.class,
                List.of(
                        "locationService",
                        "unidadeMedidaRepository",
                        "selloutRepository",
                        "produtoRepository",
                        "selloutIntegrationMapper"));

    }

    @Test
    void historicalDataIntegrationServicesShouldKeepStableBatchSizeAndMessages() {

        Assertions.assertEquals(5000, new EstoqueIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Stock data saved",
                new EstoqueIntegrationService().getSaveSuccessMessage());

        Assertions.assertEquals(5000, new SelloutIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Sell-out data saved",
                new SelloutIntegrationService().getSaveSuccessMessage());

    }

    @Test
    void historicalDataIntegrationServicesShouldRemainSpringBeans() {

        Assertions.assertTrue(EstoqueIntegrationService.class.isAnnotationPresent(Component.class));
        Assertions.assertTrue(SelloutIntegrationService.class.isAnnotationPresent(Service.class));

    }

    @Test
    void historicalDataExtractionAndDeletionShouldRequireDateFilters() {

        EstoqueIntegrationService estoqueIntegrationService = new EstoqueIntegrationService();
        SelloutIntegrationService selloutIntegrationService = new SelloutIntegrationService();

        Assertions.assertThrows(
                DataUploadException.class,
                () -> estoqueIntegrationService.getFilteredPersistedEntities(new EstoqueIntegrationFiltroDto()));
        Assertions.assertThrows(
                DataUploadException.class,
                () -> estoqueIntegrationService.removeFilteredPersistedEntities(new EstoqueIntegrationFiltroDto()));
        Assertions.assertThrows(
                DataUploadException.class,
                () -> selloutIntegrationService.getFilteredPersistedEntities(new SelloutIntegrationFiltroDto()));
        Assertions.assertThrows(
                DataUploadException.class,
                () -> selloutIntegrationService.removeFilteredPersistedEntities(new SelloutIntegrationFiltroDto()));

    }

    @Test
    void directSelloutRangeDeletionShouldRequireCompletePeriod() {

        SelloutIntegrationService selloutIntegrationService = new SelloutIntegrationService();

        Assertions.assertThrows(
                DataUploadException.class,
                () -> selloutIntegrationService.removeSelloutDeRangeDatas(null, null));

        DataUploadException invertedRangeException = Assertions.assertThrows(
                DataUploadException.class,
                () -> selloutIntegrationService.removeSelloutDeRangeDatas(
                        LocalDateTime.of(2026, 2, 1, 0, 0).toLocalDate(),
                        LocalDateTime.of(2026, 1, 1, 0, 0).toLocalDate()));

        Assertions.assertEquals(
                "End date must be after or equal to start date.",
                invertedRangeException.getMessage());

    }

    @Test
    void historicalDataFiltersShouldRejectInvertedDateRangesBeforeRepositoryAccess() {

        LocalDateTime startDateTime = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2026, 1, 1, 0, 0);
        EstoqueIntegrationService estoqueIntegrationService = new EstoqueIntegrationService();
        SelloutIntegrationService selloutIntegrationService = new SelloutIntegrationService();

        DataUploadException stockExtractionException = Assertions.assertThrows(
                DataUploadException.class,
                () -> estoqueIntegrationService.getFilteredPersistedEntities(
                        new EstoqueIntegrationFiltroDto(
                                startDateTime.toLocalDate(),
                                endDateTime.toLocalDate(),
                                null)));
        DataUploadException stockDeletionException = Assertions.assertThrows(
                DataUploadException.class,
                () -> estoqueIntegrationService.removeFilteredPersistedEntities(
                        new EstoqueIntegrationFiltroDto(
                                startDateTime.toLocalDate(),
                                endDateTime.toLocalDate(),
                                null)));

        SelloutIntegrationFiltroDto selloutIntegrationFiltroDto = new SelloutIntegrationFiltroDto();
        selloutIntegrationFiltroDto.startDate = startDateTime.toLocalDate();
        selloutIntegrationFiltroDto.endDate = endDateTime.toLocalDate();
        DataUploadException selloutExtractionException = Assertions.assertThrows(
                DataUploadException.class,
                () -> selloutIntegrationService.getFilteredPersistedEntities(selloutIntegrationFiltroDto));
        DataUploadException selloutDeletionException = Assertions.assertThrows(
                DataUploadException.class,
                () -> selloutIntegrationService.removeFilteredPersistedEntities(selloutIntegrationFiltroDto));

        Assertions.assertEquals(
                "End date must be after or equal to start date.",
                stockExtractionException.getMessage());
        Assertions.assertEquals(
                stockExtractionException.getMessage(),
                stockDeletionException.getMessage());
        Assertions.assertEquals(
                "End date must be after or equal to start date.",
                selloutExtractionException.getMessage());
        Assertions.assertEquals(
                selloutExtractionException.getMessage(),
                selloutDeletionException.getMessage());

    }

    @Test
    void historicalDataSaveShouldRejectPartialRepositorySnapshot() {

        Sellout selloutUm = new Sellout();
        Sellout selloutDois = new Sellout();
        SelloutIntegrationService selloutIntegrationService = new SelloutIntegrationService();
        ReflectionTestUtils.setField(
                selloutIntegrationService,
                "selloutRepository",
                criaRepositoryProxy(
                        SelloutRepository.class,
                        "saveAll",
                        List.of(selloutUm)));

        Estoque estoqueUm = new Estoque();
        Estoque estoqueDois = new Estoque();
        EstoqueIntegrationService estoqueIntegrationService = new EstoqueIntegrationService();
        ReflectionTestUtils.setField(
                estoqueIntegrationService,
                "estoqueRepository",
                criaRepositoryProxy(
                        EstoqueRepository.class,
                        "saveAll",
                        List.of(estoqueUm)));

        DataUploadException selloutException = Assertions.assertThrows(
                DataUploadException.class,
                () -> selloutIntegrationService.saveEntityList(List.of(
                        selloutUm,
                        selloutDois)));
        DataUploadException estoqueException = Assertions.assertThrows(
                DataUploadException.class,
                () -> estoqueIntegrationService.saveEntityList(List.of(
                        estoqueUm,
                        estoqueDois)));

        Assertions.assertEquals(
                "Sell-out saved collection size 1 differs from expected saved batch size 2.",
                selloutException.getMessage());
        Assertions.assertEquals(
                "Inventory saved collection size 1 differs from expected saved batch size 2.",
                estoqueException.getMessage());

    }

    @Test
    void stockUploadBatchEnvelopeShouldRejectIncompletePrimaryKeyBeforeRepository() {

        EstoqueIntegrationService estoqueIntegrationService = new EstoqueIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> estoqueIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO(
                                "LOC-1",
                                "MAT-1",
                                null))));

        Assertions.assertEquals(
                "Stock upload primary key must include location, material and reference date",
                dataUploadException.getMessage());

    }

    @Test
    void stockUploadBatchEnvelopeShouldRejectDuplicatedPrimaryKeyBeforeRepository() {

        EstoqueIntegrationService estoqueIntegrationService = new EstoqueIntegrationService();
        LocalDateTime dataReferencia = LocalDateTime.of(2026, 1, 1, 0, 0);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> estoqueIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO(
                                "LOC-1",
                                "MAT-1",
                                dataReferencia),
                        new EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO(
                                "LOC-1",
                                "MAT-1",
                                dataReferencia))));

        Assertions.assertEquals(
                "Stock upload primary key collection item at index 1 has duplicated key locationId LOC-1 / materialId MAT-1 / referenceDate 2026-01-01T00:00.",
                dataUploadException.getMessage());

    }

    @Test
    void selloutPrimaryKeyLookupShouldRejectDuplicatedDocumentBeforeRepository() {

        SelloutIntegrationService selloutIntegrationService = new SelloutIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> selloutIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO("DOC-001"),
                        new SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO("DOC-001"))));

        Assertions.assertEquals(
                "Sell-out primary key collection item at index 1 has duplicated Sell-out documentId DOC-001.",
                dataUploadException.getMessage());

    }

    @Test
    void stockUploadBatchEnvelopeShouldUseBatchEnvelopeInsteadOfFindAll() {

        Object[][] capturedCustomFindUploadBatchEnvelopeArguments = new Object[1][];
        boolean[] findAllCalled = {false};
        EstoqueRepository estoqueRepository = criaEstoqueRepositoryProxy(
                capturedCustomFindUploadBatchEnvelopeArguments,
                findAllCalled);
        EstoqueIntegrationService estoqueIntegrationService = new EstoqueIntegrationService();
        LocalDateTime primeiraDataReferencia = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime ultimaDataReferencia = LocalDateTime.of(2026, 1, 31, 0, 0);

        ReflectionTestUtils.setField(
                estoqueIntegrationService,
                "estoqueRepository",
                estoqueRepository);

        estoqueIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                new EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO(
                        "LOC-2",
                        "MAT-1",
                        ultimaDataReferencia),
                new EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO(
                        "LOC-1",
                        "MAT-2",
                        primeiraDataReferencia)));

        /*
         * O matching exato continua no fluxo generico pela primary key. O
         * service de estoque deve apenas buscar o envelope minimo do lote para
         * nao ler todo o historico transacional a cada upload Community.
         */
        Assertions.assertNotNull(capturedCustomFindUploadBatchEnvelopeArguments[0]);
        Assertions.assertEquals(primeiraDataReferencia, capturedCustomFindUploadBatchEnvelopeArguments[0][0]);
        Assertions.assertEquals(ultimaDataReferencia, capturedCustomFindUploadBatchEnvelopeArguments[0][1]);
        Assertions.assertEquals(
                Set.of("LOC-1", "LOC-2"),
                Set.copyOf((Collection<String>) capturedCustomFindUploadBatchEnvelopeArguments[0][2]));
        Assertions.assertEquals(
                Set.of("MAT-1", "MAT-2"),
                Set.copyOf((Collection<String>) capturedCustomFindUploadBatchEnvelopeArguments[0][3]));
        Assertions.assertFalse(findAllCalled[0]);

    }

    @Test
    void selloutSupportDataShouldRejectNullLocationSnapshotBeforeIndexing() {

        SelloutIntegrationService selloutIntegrationService = new SelloutIntegrationService();
        ReflectionTestUtils.setField(
                selloutIntegrationService,
                "locationService",
                new TestLocationService(null));

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                selloutIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Location snapshot returned null.",
                dataUploadException.getMessage());

    }

    @Test
    void stockSupportDataShouldRejectNullMaterialSnapshotBeforeIndexing() {

        LocationRepository locationRepository =
                criaRepositoryProxy(
                        LocationRepository.class,
                        "findAll",
                        List.of());
        ProdutoRepository materialRepository =
                criaRepositoryProxy(
                        ProdutoRepository.class,
                        "findAll",
                        null);
        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        List.of());
        EstoqueIntegrationService estoqueIntegrationService = new EstoqueIntegrationService();

        ReflectionTestUtils.setField(
                estoqueIntegrationService,
                "locationRepository",
                locationRepository);
        ReflectionTestUtils.setField(
                estoqueIntegrationService,
                "materialRepository",
                materialRepository);
        ReflectionTestUtils.setField(
                estoqueIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                estoqueIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Material snapshot returned null.",
                dataUploadException.getMessage());

    }

    private EstoqueRepository criaEstoqueRepositoryProxy(
            Object[][] capturedCustomFindUploadBatchEnvelopeArguments,
            boolean[] findAllCalled) {

        InvocationHandler estoqueRepositoryInvocationHandler = (proxy, method, args) -> {
            if ("customFindUploadBatchEnvelope".equals(method.getName())) {
                capturedCustomFindUploadBatchEnvelopeArguments[0] = args;
                return List.of();
            }
            if ("findAll".equals(method.getName()) && method.getParameterCount() == 0) {
                findAllCalled[0] = true;
                return List.of();
            }
            if ("toString".equals(method.getName()) && method.getParameterCount() == 0) {
                return "EstoqueRepository test proxy";
            }
            if ("hashCode".equals(method.getName()) && method.getParameterCount() == 0) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName()) && method.getParameterCount() == 1) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (EstoqueRepository) Proxy.newProxyInstance(
                EstoqueRepository.class.getClassLoader(),
                new Class<?>[]{EstoqueRepository.class},
                estoqueRepositoryInvocationHandler);

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
                    if ("saveAll".equals(method.getName()) || "deleteAll".equals(method.getName())) {
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

    private static void assertRequiredAutowiredFields(
            Class<?> serviceClass,
            List<String> fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = serviceClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    serviceClass.getSimpleName() + "." + fieldName + " must declare @Autowired explicitly");
            Assertions.assertTrue(
                    autowired.required(),
                    serviceClass.getSimpleName() + "." + fieldName + " must be a required Spring bean");
        }

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
