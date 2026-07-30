package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.VersaoMalhaIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.VersaoMalhaIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contratos do service Community de versao de malha.
 *
 * <p>A carga e pequena, mas e estrutural para malha e Supply Planning. Os
 * testes garantem batch, lookup incremental por ids e falha antes do repository
 * quando o payload ou o snapshot de support data vier inconsistente.</p>
 */
class VersaoMalhaIntegrationServiceCommunityContractTest {

    @Test
    void supplyNetworkVersionIntegrationServiceShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                VersaoMalhaIntegrationService.class,
                List.of(
                        "versaoMalhaRepository",
                        "locationRepository",
                        "versaoMalhaIntegrationMapper"));

    }

    @Test
    void supplyNetworkVersionIntegrationServiceShouldRemainSpringServiceWithStableContract() {

        VersaoMalhaIntegrationService versaoMalhaIntegrationService =
                new VersaoMalhaIntegrationService();

        Assertions.assertTrue(VersaoMalhaIntegrationService.class.isAnnotationPresent(Service.class));
        Assertions.assertEquals(1000, versaoMalhaIntegrationService.getBatchSize());
        Assertions.assertEquals(
                "Supply Network Version data uploaded",
                versaoMalhaIntegrationService.getSaveSuccessMessage());

    }

    @Test
    void supplyNetworkVersionPrimaryKeyShouldRejectDuplicateKeyBeforeRepositoryLookup() {

        VersaoMalhaIntegrationService versaoMalhaIntegrationService =
                new VersaoMalhaIntegrationService();

        DataUploadException dataUploadException =
                Assertions.assertThrows(
                        DataUploadException.class,
                        () -> versaoMalhaIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                                List.of(
                                        new VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO("SNV-1"),
                                        new VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO("SNV-1"))));

        Assertions.assertEquals(
                "Supply Network Version primary key collection item at index 1 has duplicated key supplyNetworkVersionId SNV-1.",
                dataUploadException.getMessage());

    }

    @Test
    void supplyNetworkVersionPrimaryKeyShouldRejectBlankKeyBeforeRepositoryLookup() {

        VersaoMalhaIntegrationService versaoMalhaIntegrationService =
                new VersaoMalhaIntegrationService();

        DataUploadException dataUploadException =
                Assertions.assertThrows(
                        DataUploadException.class,
                        () -> versaoMalhaIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                                List.of(new VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO(" "))));

        Assertions.assertEquals(
                "Supply Network Version upload primary key must include supply network version id",
                dataUploadException.getMessage());

    }

    @Test
    void supplyNetworkVersionLookupShouldUseBatchIdsInsteadOfFullScan() {

        VersaoMalhaRepository versaoMalhaRepository =
                criaRepositoryProxy(
                        VersaoMalhaRepository.class,
                        Map.of("findAllById", List.of(new VersaoMalha("SNV-1"))));
        VersaoMalhaIntegrationService versaoMalhaIntegrationService =
                criaVersaoMalhaIntegrationService(
                        versaoMalhaRepository,
                        null);

        Collection<VersaoMalha> versoesMalha =
                versaoMalhaIntegrationService.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
                        List.of(new VersaoMalhaIntegrationDataDto.VersaoMalhaPrimaryKeyIntegrationDTO("SNV-1")));

        Assertions.assertEquals(1, versoesMalha.size());

    }

    @Test
    void supplyNetworkVersionSupportDataShouldRejectBrokenLocationSnapshotBeforeIndexing() {

        LocationRepository locationRepository =
                criaRepositoryProxy(
                        LocationRepository.class,
                        Map.of("findAll", List.of(new Location())));
        VersaoMalhaIntegrationService versaoMalhaIntegrationService =
                criaVersaoMalhaIntegrationService(
                        null,
                        locationRepository);

        DataUploadException dataUploadException =
                Assertions.assertThrows(
                        DataUploadException.class,
                        versaoMalhaIntegrationService::getSupportData);

        Assertions.assertEquals(
                "Location snapshot returned item without id at index 0.",
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

    private static VersaoMalhaIntegrationService criaVersaoMalhaIntegrationService(
            VersaoMalhaRepository versaoMalhaRepository,
            LocationRepository locationRepository) {

        VersaoMalhaIntegrationService versaoMalhaIntegrationService =
                new VersaoMalhaIntegrationService();
        ReflectionTestUtils.setField(
                versaoMalhaIntegrationService,
                "versaoMalhaRepository",
                versaoMalhaRepository);
        ReflectionTestUtils.setField(
                versaoMalhaIntegrationService,
                "locationRepository",
                locationRepository);
        ReflectionTestUtils.setField(
                versaoMalhaIntegrationService,
                "versaoMalhaIntegrationMapper",
                new VersaoMalhaIntegrationMapper());
        return versaoMalhaIntegrationService;

    }

    @SuppressWarnings("unchecked")
    private static <T> T criaRepositoryProxy(
            Class<T> repositoryClass,
            Map<String, Object> methodResults) {

        Map<String, Object> mutableMethodResults = new HashMap<>(methodResults);

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
                    if (mutableMethodResults.containsKey(method.getName())) {
                        return mutableMethodResults.get(method.getName());
                    }
                    throw new UnsupportedOperationException(
                            repositoryClass.getSimpleName()
                                    + "."
                                    + method.getName()
                                    + " is not implemented in this test proxy.");
                });

    }

}
