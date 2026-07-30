package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.service;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.dto.PoliticaEstoquesMaterialLocationIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper.PoliticaEstoquesIntegrationMapper;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper.PoliticaEstoquesMaterialLocationIntegrationMapper;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper.PoliticaEstoquesMaterialLocationIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesMaterialLocationRepository;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesRepository;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Contrato dos services Community de data upload de politica de estoque.
 *
 * <p>Os testes cobrem a borda que nao aparece no mapper: wiring explicito,
 * lookups em lote, suporte obrigatorio e validacao antes de persistir valores
 * fisicos que alimentam o heuristico de Supply Planning.</p>
 */
public class InventoryPolicyIntegrationServiceCommunityContractTest {

    @Test
    public void servicesShouldDeclareComponentAndExplicitAutowiredFields() throws Exception {

        Assertions.assertTrue(PoliticaEstoquesIntegrationService.class.isAnnotationPresent(Component.class));
        Assertions.assertTrue(PoliticaEstoquesMaterialLocationIntegrationService.class.isAnnotationPresent(Component.class));

        assertRequiredAutowiredFields(
                PoliticaEstoquesIntegrationService.class,
                List.of(
                        "politicaEstoquesRepository",
                        "politicaEstoquesIntegrationMapper"));
        assertRequiredAutowiredFields(
                PoliticaEstoquesMaterialLocationIntegrationService.class,
                List.of(
                        "politicaEstoquesMaterialLocationRepository",
                        "politicaEstoquesRepository",
                        "produtoRepository",
                        "locationRepository",
                        "politicaEstoquesMaterialLocationIntegrationMapper"));

    }

    @Test
    public void inventoryPolicyServiceShouldExposeMessagesAndBatchSize() throws Exception {

        PoliticaEstoquesIntegrationService service = getPoliticaEstoquesIntegrationService(
                Mockito.mock(PoliticaEstoquesRepository.class),
                new PoliticaEstoquesIntegrationMapper());

        Assertions.assertEquals("Inventory policies saved", service.getSaveSuccessMessage());
        Assertions.assertEquals(1000, service.getBatchSize());
        Assertions.assertNotNull(service.getSupportData());

    }

    @Test
    public void inventoryPolicyServiceShouldNotQueryRepositoryForEmptyPrimaryKeys() throws Exception {

        PoliticaEstoquesRepository politicaEstoquesRepository = Mockito.mock(PoliticaEstoquesRepository.class);
        PoliticaEstoquesIntegrationService service =
                getPoliticaEstoquesIntegrationService(
                        politicaEstoquesRepository,
                        new PoliticaEstoquesIntegrationMapper());

        Collection<PoliticaEstoques> persistedEntities =
                service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of());

        Assertions.assertTrue(persistedEntities.isEmpty());
        Mockito.verify(politicaEstoquesRepository, Mockito.never()).findAllById(Mockito.any());
        Mockito.verify(politicaEstoquesRepository, Mockito.never()).findAll();

    }

    @Test
    public void inventoryPolicyServiceShouldRejectDuplicatedPrimaryKeysBeforeRepository() throws Exception {

        PoliticaEstoquesRepository politicaEstoquesRepository = Mockito.mock(PoliticaEstoquesRepository.class);
        PoliticaEstoquesIntegrationService service =
                getPoliticaEstoquesIntegrationService(
                        politicaEstoquesRepository,
                        new PoliticaEstoquesIntegrationMapper());

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO("POL_01"),
                        new PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO("POL_01"))));

        Assertions.assertEquals(
                "Inventory policy primary key collection item at index 1 has duplicated id POL_01.",
                dataUploadException.getMessage());
        Mockito.verifyNoInteractions(politicaEstoquesRepository);

    }

    @Test
    public void inventoryPolicyServiceShouldQueryOnlyRequestedIds() throws Exception {

        PoliticaEstoques politicaEstoques = getPoliticaEstoques("POL_01");
        PoliticaEstoquesRepository politicaEstoquesRepository = Mockito.mock(PoliticaEstoquesRepository.class);
        Mockito.when(politicaEstoquesRepository.findAllById(Set.of("POL_01")))
                .thenReturn(List.of(politicaEstoques));
        PoliticaEstoquesIntegrationService service =
                getPoliticaEstoquesIntegrationService(
                        politicaEstoquesRepository,
                        new PoliticaEstoquesIntegrationMapper());

        Collection<PoliticaEstoques> persistedEntities =
                service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO("POL_01")));

        Assertions.assertEquals(List.of(politicaEstoques), persistedEntities);
        Mockito.verify(politicaEstoquesRepository).findAllById(Set.of("POL_01"));
        Mockito.verify(politicaEstoquesRepository, Mockito.never()).findAll();

    }

    @Test
    public void inventoryPolicyServiceShouldRejectInvalidPrimaryKeys() throws Exception {

        PoliticaEstoquesIntegrationService service = getPoliticaEstoquesIntegrationService(
                Mockito.mock(PoliticaEstoquesRepository.class),
                new PoliticaEstoquesIntegrationMapper());

        Assertions.assertThrows(
                DataUploadException.class,
                () -> service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(null));
        Assertions.assertThrows(
                DataUploadException.class,
                () -> service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO("POL_01"),
                        new PoliticaEstoquesIntegrationDataDto.PoliticaEstoquesPrimaryKeyIntegrationDTO("POL_01"))));

    }

    @Test
    public void inventoryPolicyServiceShouldValidateSaveAllReturn() throws Exception {

        PoliticaEstoques politicaEstoques = getPoliticaEstoques("POL_01");
        PoliticaEstoquesRepository politicaEstoquesRepository = Mockito.mock(PoliticaEstoquesRepository.class);
        Mockito.when(politicaEstoquesRepository.saveAll(List.of(politicaEstoques)))
                .thenReturn(List.of(politicaEstoques));
        PoliticaEstoquesIntegrationService service =
                getPoliticaEstoquesIntegrationService(
                        politicaEstoquesRepository,
                        new PoliticaEstoquesIntegrationMapper());

        List<PoliticaEstoques> savedEntities = service.saveEntityList(List.of(politicaEstoques));

        Assertions.assertEquals(List.of(politicaEstoques), savedEntities);

    }

    @Test
    public void inventoryPolicyDetailServiceShouldExposeMessagesAndBatchSize() throws Exception {

        PoliticaEstoquesMaterialLocationIntegrationService service =
                getPoliticaEstoquesMaterialLocationIntegrationService(
                        Mockito.mock(PoliticaEstoquesMaterialLocationRepository.class),
                        Mockito.mock(PoliticaEstoquesRepository.class),
                        Mockito.mock(ProdutoRepository.class),
                        Mockito.mock(LocationRepository.class),
                        new PoliticaEstoquesMaterialLocationIntegrationMapper());

        Assertions.assertEquals("Inventory policy details saved", service.getSaveSuccessMessage());
        Assertions.assertEquals(1000, service.getBatchSize());

    }

    @Test
    public void inventoryPolicyDetailServiceShouldLoadValidatedSupportData() throws Exception {

        PoliticaEstoques politicaEstoques = getPoliticaEstoques("POL_01");
        Produto material = new Produto("MAT_01");
        Location location = new Location("LOC_01");
        PoliticaEstoquesRepository politicaEstoquesRepository = Mockito.mock(PoliticaEstoquesRepository.class);
        ProdutoRepository produtoRepository = Mockito.mock(ProdutoRepository.class);
        LocationRepository locationRepository = Mockito.mock(LocationRepository.class);
        Mockito.when(politicaEstoquesRepository.findAll()).thenReturn(List.of(politicaEstoques));
        Mockito.when(produtoRepository.findAll()).thenReturn(List.of(material));
        Mockito.when(locationRepository.findAll()).thenReturn(List.of(location));
        PoliticaEstoquesMaterialLocationIntegrationService service =
                getPoliticaEstoquesMaterialLocationIntegrationService(
                        Mockito.mock(PoliticaEstoquesMaterialLocationRepository.class),
                        politicaEstoquesRepository,
                        produtoRepository,
                        locationRepository,
                        new PoliticaEstoquesMaterialLocationIntegrationMapper());

        PoliticaEstoquesMaterialLocationIntegrationSupportData supportData = service.getSupportData();

        Assertions.assertSame(politicaEstoques, supportData.mapaPoliticaEstoquesPorId.get("POL_01"));
        Assertions.assertSame(material, supportData.mapaMaterialPorId.get("MAT_01"));
        Assertions.assertSame(location, supportData.mapaLocationPorId.get("LOC_01"));

    }

    @Test
    public void inventoryPolicyDetailServiceShouldNotQueryRepositoryForEmptyPrimaryKeys() throws Exception {

        PoliticaEstoquesMaterialLocationRepository repository =
                Mockito.mock(PoliticaEstoquesMaterialLocationRepository.class);
        PoliticaEstoquesMaterialLocationIntegrationService service =
                getPoliticaEstoquesMaterialLocationIntegrationService(
                        repository,
                        Mockito.mock(PoliticaEstoquesRepository.class),
                        Mockito.mock(ProdutoRepository.class),
                        Mockito.mock(LocationRepository.class),
                        new PoliticaEstoquesMaterialLocationIntegrationMapper());

        Collection<PoliticaEstoquesMaterialLocation> persistedEntities =
                service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of());

        Assertions.assertTrue(persistedEntities.isEmpty());
        Mockito.verify(repository, Mockito.never()).customFindByPoliticaEstoquesIdIn(Mockito.any());
        Mockito.verify(repository, Mockito.never()).customFindAll();

    }

    @Test
    public void inventoryPolicyDetailServiceShouldRejectDuplicatedPrimaryKeysBeforeRepository() throws Exception {

        PoliticaEstoquesMaterialLocationRepository repository =
                Mockito.mock(PoliticaEstoquesMaterialLocationRepository.class);
        PoliticaEstoquesMaterialLocationIntegrationService service =
                getPoliticaEstoquesMaterialLocationIntegrationService(
                        repository,
                        Mockito.mock(PoliticaEstoquesRepository.class),
                        Mockito.mock(ProdutoRepository.class),
                        Mockito.mock(LocationRepository.class),
                        new PoliticaEstoquesMaterialLocationIntegrationMapper());

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO(
                                "POL_01",
                                "MAT_01",
                                "LOC_01"),
                        new PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO(
                                "POL_01",
                                "MAT_01",
                                "LOC_01"))));

        Assertions.assertEquals(
                "Inventory policy detail primary key collection item at index 1 has duplicated key inventoryPolicyId POL_01 / materialId MAT_01 / locationId LOC_01.",
                dataUploadException.getMessage());
        Mockito.verifyNoInteractions(repository);

    }

    @Test
    public void inventoryPolicyDetailServiceShouldQueryByInventoryPolicyEnvelope() throws Exception {

        PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation =
                getPoliticaEstoquesMaterialLocation("POL_01", "MAT_01", "LOC_01");
        PoliticaEstoquesMaterialLocationRepository repository =
                Mockito.mock(PoliticaEstoquesMaterialLocationRepository.class);
        Mockito.when(repository.customFindByPoliticaEstoquesIdIn(Set.of("POL_01")))
                .thenReturn(List.of(politicaEstoquesMaterialLocation));
        PoliticaEstoquesMaterialLocationIntegrationService service =
                getPoliticaEstoquesMaterialLocationIntegrationService(
                        repository,
                        Mockito.mock(PoliticaEstoquesRepository.class),
                        Mockito.mock(ProdutoRepository.class),
                        Mockito.mock(LocationRepository.class),
                        new PoliticaEstoquesMaterialLocationIntegrationMapper());

        Collection<PoliticaEstoquesMaterialLocation> persistedEntities =
                service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO(
                                "POL_01",
                                "MAT_01",
                                "LOC_01")));

        Assertions.assertEquals(List.of(politicaEstoquesMaterialLocation), persistedEntities);
        Mockito.verify(repository).customFindByPoliticaEstoquesIdIn(Set.of("POL_01"));

    }

    @Test
    public void inventoryPolicyDetailServiceShouldRejectInvalidPrimaryKeys() throws Exception {

        PoliticaEstoquesMaterialLocationIntegrationService service =
                getPoliticaEstoquesMaterialLocationIntegrationService(
                        Mockito.mock(PoliticaEstoquesMaterialLocationRepository.class),
                        Mockito.mock(PoliticaEstoquesRepository.class),
                        Mockito.mock(ProdutoRepository.class),
                        Mockito.mock(LocationRepository.class),
                        new PoliticaEstoquesMaterialLocationIntegrationMapper());

        Assertions.assertThrows(
                DataUploadException.class,
                () -> service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(null));
        Assertions.assertThrows(
                DataUploadException.class,
                () -> service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO(
                                "POL_01",
                                "MAT_01",
                                "LOC_01"),
                        new PoliticaEstoquesMaterialLocationIntegrationDataDto.PoliticaEstoquesMaterialLocationPrimaryKeyIntegrationDTO(
                                "POL_01",
                                "MAT_01",
                                "LOC_01"))));

    }

    @Test
    public void inventoryPolicyDetailServiceShouldRejectInvalidOperationalValuesBeforeSaving() throws Exception {

        PoliticaEstoquesMaterialLocationRepository repository =
                Mockito.mock(PoliticaEstoquesMaterialLocationRepository.class);
        PoliticaEstoquesMaterialLocationIntegrationService service =
                getPoliticaEstoquesMaterialLocationIntegrationService(
                        repository,
                        Mockito.mock(PoliticaEstoquesRepository.class),
                        Mockito.mock(ProdutoRepository.class),
                        Mockito.mock(LocationRepository.class),
                        new PoliticaEstoquesMaterialLocationIntegrationMapper());
        PoliticaEstoquesMaterialLocation negativeSafetyStock =
                getPoliticaEstoquesMaterialLocation("POL_01", "MAT_01", "LOC_01");
        negativeSafetyStock.setEstoqueSegurancaDrpOuTargetKanban(-1.0d);
        PoliticaEstoquesMaterialLocation nonFiniteMaximumStock =
                getPoliticaEstoquesMaterialLocation("POL_01", "MAT_01", "LOC_01");
        nonFiniteMaximumStock.setEstoqueMaximoDrp(Double.POSITIVE_INFINITY);
        PoliticaEstoquesMaterialLocation enterpriseFrequency =
                getPoliticaEstoquesMaterialLocation("POL_01", "MAT_01", "LOC_01");
        enterpriseFrequency.setFrequenciaReabastecimentoDias(7.0d);

        Assertions.assertThrows(
                DataUploadException.class,
                () -> service.saveEntityList(List.of(negativeSafetyStock)));
        Assertions.assertThrows(
                DataUploadException.class,
                () -> service.saveEntityList(List.of(nonFiniteMaximumStock)));
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> service.saveEntityList(List.of(enterpriseFrequency)));
        Mockito.verify(repository, Mockito.never()).saveAll(Mockito.any());

    }

    @Test
    public void inventoryPolicyDetailServiceShouldValidateSaveAllReturn() throws Exception {

        PoliticaEstoquesMaterialLocation entity =
                getPoliticaEstoquesMaterialLocation("POL_01", "MAT_01", "LOC_01");
        PoliticaEstoquesMaterialLocationRepository repository =
                Mockito.mock(PoliticaEstoquesMaterialLocationRepository.class);
        Mockito.when(repository.saveAll(List.of(entity))).thenReturn(List.of(entity));
        PoliticaEstoquesMaterialLocationIntegrationService service =
                getPoliticaEstoquesMaterialLocationIntegrationService(
                        repository,
                        Mockito.mock(PoliticaEstoquesRepository.class),
                        Mockito.mock(ProdutoRepository.class),
                        Mockito.mock(LocationRepository.class),
                        new PoliticaEstoquesMaterialLocationIntegrationMapper());

        List<PoliticaEstoquesMaterialLocation> savedEntities = service.saveEntityList(List.of(entity));

        Assertions.assertEquals(List.of(entity), savedEntities);

    }

    private static PoliticaEstoquesIntegrationService getPoliticaEstoquesIntegrationService(
            PoliticaEstoquesRepository politicaEstoquesRepository,
            PoliticaEstoquesIntegrationMapper politicaEstoquesIntegrationMapper) throws Exception {

        PoliticaEstoquesIntegrationService service = new PoliticaEstoquesIntegrationService();
        setField(service, "politicaEstoquesRepository", politicaEstoquesRepository);
        setField(service, "politicaEstoquesIntegrationMapper", politicaEstoquesIntegrationMapper);
        return service;

    }

    private static PoliticaEstoquesMaterialLocationIntegrationService getPoliticaEstoquesMaterialLocationIntegrationService(
            PoliticaEstoquesMaterialLocationRepository politicaEstoquesMaterialLocationRepository,
            PoliticaEstoquesRepository politicaEstoquesRepository,
            ProdutoRepository produtoRepository,
            LocationRepository locationRepository,
            PoliticaEstoquesMaterialLocationIntegrationMapper mapper) throws Exception {

        PoliticaEstoquesMaterialLocationIntegrationService service =
                new PoliticaEstoquesMaterialLocationIntegrationService();
        setField(service, "politicaEstoquesMaterialLocationRepository", politicaEstoquesMaterialLocationRepository);
        setField(service, "politicaEstoquesRepository", politicaEstoquesRepository);
        setField(service, "produtoRepository", produtoRepository);
        setField(service, "locationRepository", locationRepository);
        setField(service, "politicaEstoquesMaterialLocationIntegrationMapper", mapper);
        return service;

    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

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

    private static PoliticaEstoques getPoliticaEstoques(String id) {

        PoliticaEstoques politicaEstoques = new PoliticaEstoques();
        politicaEstoques.setId(id);
        return politicaEstoques;

    }

    private static PoliticaEstoquesMaterialLocation getPoliticaEstoquesMaterialLocation(
            String inventoryPolicyId,
            String materialId,
            String locationId) {

        return new PoliticaEstoquesMaterialLocation(
                new PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey(
                        getPoliticaEstoques(inventoryPolicyId),
                        new Produto(materialId),
                        new Location(locationId)));

    }

}
