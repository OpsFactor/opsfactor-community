package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contratos das integracoes Community de malha de transporte.
 *
 * <p>Transportation lane e transportation lane/material permanecem no
 * Community porque o Supply Planning heuristico precisa conhecer caminhos
 * operacionais entre locations e materiais. Frota, last mile, custos
 * logisticos, mapa, baricentro e Supply Network Flows continuam Enterprise.</p>
 */
class TransportationLaneIntegrationServicesCommunityContractTest {

    @Test
    void transportationLaneIntegrationServicesShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                LinhaTransporteIntegrationService.class,
                List.of(
                        "linhaTransporteRepository",
                        "versaoMalhaRepository",
                        "unidadeMedidaRepository",
                        "locationRepository",
                        "linhaTransporteIntegrationMapper"));

        assertRequiredAutowiredFields(
                LinhaTransporteProdutoIntegrationService.class,
                List.of(
                        "linhaTransporteProdutoRepository",
                        "produtoRepository",
                        "versaoMalhaRepository",
                        "unidadeMedidaRepository",
                        "locationRepository",
                        "linhaTransporteProdutoIntegrationMapper",
                        "linhaTransporteIntegrationService"));

    }

    @Test
    void transportationLaneIntegrationServicesShouldKeepStableBatchSizeAndMessages() {

        Assertions.assertEquals(1000, new LinhaTransporteIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Transportation Lane data uploaded",
                new LinhaTransporteIntegrationService().getSaveSuccessMessage());

        Assertions.assertEquals(1000, new LinhaTransporteProdutoIntegrationService().getBatchSize());
        Assertions.assertEquals(
                "Transportation Lane - Material data uploaded",
                new LinhaTransporteProdutoIntegrationService().getSaveSuccessMessage());

    }

    @Test
    void transportationLaneIntegrationServicesShouldRemainSpringServices() {

        Assertions.assertTrue(LinhaTransporteIntegrationService.class.isAnnotationPresent(Service.class));
        Assertions.assertTrue(LinhaTransporteProdutoIntegrationService.class.isAnnotationPresent(Service.class));

    }

    @Test
    void transportationLaneMaterialDeactivationShouldRejectMissingFilterBeforeRepository() {

        LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService =
                new LinhaTransporteProdutoIntegrationService();

        /*
         * O endpoint de desativacao lane/material recebe filtro obrigatorio.
         * Chamada direta ao service tambem precisa falhar antes de repository,
         * evitando erro de ponteiro ao acessar os campos do filtro.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> linhaTransporteProdutoIntegrationService.desativaLinhaTransporteProdutoDeFiltro(null));
        Assertions.assertEquals(
                "Transportation lane/material deactivation filter is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void transportationLanePrimaryKeyShouldRejectDuplicateCompositeKeyBeforeRepositoryLookup() {

        LinhaTransporteIntegrationService linhaTransporteIntegrationService =
                new LinhaTransporteIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> linhaTransporteIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(
                                new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                                        "SNV-1",
                                        "LOC-ORIG",
                                        "LOC-DEST"),
                                new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                                        "SNV-1",
                                        "LOC-ORIG",
                                        "LOC-DEST"))));

        Assertions.assertEquals(
                "Transportation Lane primary key collection item at index 1 has duplicated key supplyNetworkVersionId SNV-1 / originLocationId LOC-ORIG / destinationLocationId LOC-DEST.",
                dataUploadException.getMessage());

    }

    @Test
    void transportationLanePrimaryKeyShouldRejectBlankDimensionBeforeRepositoryLookup() {

        LinhaTransporteIntegrationService linhaTransporteIntegrationService =
                new LinhaTransporteIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> linhaTransporteIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(new LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO(
                                "SNV-1",
                                " ",
                                "LOC-DEST"))));

        Assertions.assertEquals(
                "Transportation Lane upload primary key must include supply network version, origin location and destination location",
                dataUploadException.getMessage());

    }

    @Test
    void transportationLaneMaterialPrimaryKeyShouldRejectDuplicateCompositeKeyBeforeRepositoryLookup() {

        LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService =
                new LinhaTransporteProdutoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> linhaTransporteProdutoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(
                                new LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                                        "SNV-1",
                                        "LOC-ORIG",
                                        "LOC-DEST",
                                        "MAT-1"),
                                new LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                                        "SNV-1",
                                        "LOC-ORIG",
                                        "LOC-DEST",
                                        "MAT-1"))));

        Assertions.assertEquals(
                "Transportation Lane Material primary key collection item at index 1 has duplicated key supplyNetworkVersionId SNV-1 / originLocationId LOC-ORIG / destinationLocationId LOC-DEST / materialId MAT-1.",
                dataUploadException.getMessage());

    }

    @Test
    void transportationLaneMaterialPrimaryKeyShouldRejectMissingMaterialBeforeRepositoryLookup() {

        LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService =
                new LinhaTransporteProdutoIntegrationService();

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> linhaTransporteProdutoIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(new LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO(
                                "SNV-1",
                                "LOC-ORIG",
                                "LOC-DEST",
                                null))));

        Assertions.assertEquals(
                "Transportation Lane Material upload primary key must include supply network version, origin location, destination location and material",
                dataUploadException.getMessage());

    }

    @Test
    void transportationLanePrimaryKeyLookupsShouldAcceptEmptyCollectionWithoutRepositoryLookup() {

        Assertions.assertTrue(new LinhaTransporteIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());
        Assertions.assertTrue(new LinhaTransporteProdutoIntegrationService()
                .getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of())
                .isEmpty());

    }

    @Test
    void transportationLaneSupportDataShouldRejectBrokenSupplyNetworkVersionSnapshotBeforeIndexing() {

        VersaoMalhaRepository versaoMalhaRepository =
                criaRepositoryProxy(
                        VersaoMalhaRepository.class,
                        "findAll",
                        List.of(new VersaoMalha()));
        LocationRepository locationRepository =
                criaRepositoryProxy(
                        LocationRepository.class,
                        "findAll",
                        List.of());
        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        List.of());
        LinhaTransporteIntegrationService linhaTransporteIntegrationService =
                criaLinhaTransporteIntegrationService(
                        versaoMalhaRepository,
                        locationRepository,
                        unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                linhaTransporteIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Supply Network Version snapshot returned item without id at index 0.",
                dataUploadException.getMessage());

    }

    @Test
    void transportationLaneMaterialSupportDataShouldRejectNullMaterialSnapshotBeforeIndexing() {

        VersaoMalhaRepository versaoMalhaRepository =
                criaRepositoryProxy(
                        VersaoMalhaRepository.class,
                        "findAll",
                        List.of());
        LocationRepository locationRepository =
                criaRepositoryProxy(
                        LocationRepository.class,
                        "findAll",
                        List.of());
        ProdutoRepository produtoRepository =
                criaRepositoryProxy(
                        ProdutoRepository.class,
                        "findAll",
                        null);
        UnidadeMedidaRepository unidadeMedidaRepository =
                criaRepositoryProxy(
                        UnidadeMedidaRepository.class,
                        "findAll",
                        List.of());
        LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService =
                criaLinhaTransporteProdutoIntegrationService(
                        versaoMalhaRepository,
                        locationRepository,
                        produtoRepository,
                        unidadeMedidaRepository);

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                linhaTransporteProdutoIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Material snapshot returned null.",
                dataUploadException.getMessage());

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

    private static LinhaTransporteProduto criaLinhaTransporteProdutoParaDesativacao(
            String supplyNetworkVersionId,
            String originLocationId,
            String destinationLocationId,
            String materialId) {

        LinhaTransporte linhaTransporte =
                new LinhaTransporte(
                        new LinhaTransporte.LinhaTransporteCompositeKey(
                                new VersaoMalha(supplyNetworkVersionId),
                                new Location(originLocationId),
                                new Location(destinationLocationId)));
        return new LinhaTransporteProduto(
                new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                        linhaTransporte,
                        new Produto(materialId)));

    }

    private static LinhaTransporteIntegrationService criaLinhaTransporteIntegrationService(
            VersaoMalhaRepository versaoMalhaRepository,
            LocationRepository locationRepository,
            UnidadeMedidaRepository unidadeMedidaRepository) {

        LinhaTransporteIntegrationService linhaTransporteIntegrationService =
                new LinhaTransporteIntegrationService();
        ReflectionTestUtils.setField(
                linhaTransporteIntegrationService,
                "versaoMalhaRepository",
                versaoMalhaRepository);
        ReflectionTestUtils.setField(
                linhaTransporteIntegrationService,
                "locationRepository",
                locationRepository);
        ReflectionTestUtils.setField(
                linhaTransporteIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);
        return linhaTransporteIntegrationService;

    }

    private static LinhaTransporteProdutoIntegrationService criaLinhaTransporteProdutoIntegrationService(
            VersaoMalhaRepository versaoMalhaRepository,
            LocationRepository locationRepository,
            ProdutoRepository produtoRepository,
            UnidadeMedidaRepository unidadeMedidaRepository) {

        LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService =
                new LinhaTransporteProdutoIntegrationService();
        ReflectionTestUtils.setField(
                linhaTransporteProdutoIntegrationService,
                "versaoMalhaRepository",
                versaoMalhaRepository);
        ReflectionTestUtils.setField(
                linhaTransporteProdutoIntegrationService,
                "locationRepository",
                locationRepository);
        ReflectionTestUtils.setField(
                linhaTransporteProdutoIntegrationService,
                "produtoRepository",
                produtoRepository);
        ReflectionTestUtils.setField(
                linhaTransporteProdutoIntegrationService,
                "unidadeMedidaRepository",
                unidadeMedidaRepository);
        return linhaTransporteProdutoIntegrationService;

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

}
