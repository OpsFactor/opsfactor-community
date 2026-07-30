package com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.service;

import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.dto.InventoryPlanIntegrationDataDto;
import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.mapper.InventoryPlanIntegrationMapper;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.service.SupplyPlanPersistedBaselinePreflight;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Contrato do service Community read-only de Inventory Plan.
 *
 * <p>O service deve sempre receber um Supply Plan explicito, buscar somente o
 * envelope necessario com fetches dedicados e bloquear qualquer tentativa de
 * persistencia manual de quantidades calculadas pelo Supply Planning.</p>
 */
public class InventoryPlanIntegrationServiceCommunityContractTest {

    @Test
    public void serviceShouldDeclareServiceAndExplicitAutowiredFields() throws Exception {

        Assertions.assertTrue(InventoryPlanIntegrationService.class.isAnnotationPresent(Service.class));
        assertRequiredAutowiredFields(
                InventoryPlanIntegrationService.class,
                List.of(
                        "supplyPlanRepository",
                        "inventoryPlanLinhaRepository",
                        "supplyPlanPersistedBaselinePreflight",
                        "parametrosGlobaisService",
                        "unidadeMedidaProjectionFactory",
                        "inventoryPlanIntegrationMapper"));

    }

    @Test
    public void serviceShouldExposeReadOnlyMessagesAndBatchSize() {

        InventoryPlanIntegrationService service = new InventoryPlanIntegrationService();

        Assertions.assertEquals("Inventory Plan data exported", service.getSaveSuccessMessage());
        Assertions.assertEquals(1000, service.getBatchSize());
        Assertions.assertNotNull(service.getSupportData());

    }

    @Test
    public void serviceShouldBlockMutationAndFullExport() {

        InventoryPlanIntegrationService service = new InventoryPlanIntegrationService();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> service.saveEntityList(List.of()));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> service.removeEntityList(List.of()));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> service.saveFile(null));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> service.saveDTOList((IntegrationDto<
                        InventoryPlanIntegrationDataDto,
                        InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO,
                        EmptyIntegrationDataFilter,
                        IntegrationOptionsDto>) null));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                service::getAllPersistedEntities);

    }

    @Test
    public void filteredExportShouldValidateSupplyPlanAndBuildConvertedDtos() {

        SupplyPlan supplyPlan = getSupplyPlan(42L);
        UnidadeMedida unidadeMedidaInventoryPlan = new UnidadeMedida("EA");
        UnidadeMedida unidadeMedidaPadraoSnp = new UnidadeMedida("CS");
        InventoryPlanLinha inventoryPlanLinha =
                getInventoryPlanLinha(
                        supplyPlan,
                        "LOC_01",
                        "MAT_01",
                        LocalDateTime.of(2026, 1, 31, 0, 0),
                        unidadeMedidaInventoryPlan);

        SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        ParametrosGlobaisService parametrosGlobaisService = Mockito.mock(ParametrosGlobaisService.class);
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                Mockito.mock(UnidadeMedidaProjectionFactory.class);
        UnidadeMedidaProjection unidadeMedidaProjection = Mockito.mock(UnidadeMedidaProjection.class);
        Mockito.when(supplyPlanRepository.findById(42L)).thenReturn(Optional.of(supplyPlan));
        Mockito.when(inventoryPlanLinhaRepository.customFindBySupplyPlan(supplyPlan))
                .thenReturn(List.of(inventoryPlanLinha));
        Mockito.when(parametrosGlobaisService.getParametrosGlobais())
                .thenReturn(getParametrosGlobais(unidadeMedidaPadraoSnp));
        Mockito.when(unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes())
                .thenReturn(unidadeMedidaProjection);
        Mockito.when(unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                        inventoryPlanLinha.getProduto(),
                        unidadeMedidaInventoryPlan,
                        unidadeMedidaPadraoSnp))
                .thenReturn(2.0d);
        InventoryPlanIntegrationService service =
                getService(
                        supplyPlanRepository,
                        inventoryPlanLinhaRepository,
                        parametrosGlobaisService,
                        unidadeMedidaProjectionFactory,
                        new InventoryPlanIntegrationMapper());

        List<InventoryPlanIntegrationDataDto> dtoList = service.getInventoryPlanDTOList(42L);

        Assertions.assertEquals(1, dtoList.size());
        InventoryPlanIntegrationDataDto dto = dtoList.get(0);
        Assertions.assertEquals(42L, dto.primaryKeyDto.supplyPlanId);
        Assertions.assertEquals("LOC_01", dto.primaryKeyDto.locationId);
        Assertions.assertEquals("MAT_01", dto.primaryKeyDto.materialId);
        Assertions.assertEquals("EA", dto.unitOfMeasureId);
        Assertions.assertEquals(2.0d, dto.safetyStockQuantity);
        Assertions.assertEquals(10.0d, dto.maximumStockQuantity);
        Assertions.assertEquals(-3.0d, dto.projectedStockWorkingVersion);
        Assertions.assertEquals(4.0d, dto.projectedStockUnconstrainedVersion);
        Assertions.assertEquals(5.0d, dto.projectedStockConstrainedVersion);
        Assertions.assertEquals("CS", dto.defaultSnpUnitOfMeasureId);
        Assertions.assertEquals(-6.0d, dto.projectedStockWorkingVersionDefaultSnpUom);
        Assertions.assertEquals(8.0d, dto.projectedStockUnconstrainedVersionDefaultSnpUom);
        Assertions.assertEquals(10.0d, dto.projectedStockConstrainedVersionDefaultSnpUom);
        Mockito.verify(supplyPlanRepository).findById(42L);
        Mockito.verify(inventoryPlanLinhaRepository).customFindBySupplyPlan(supplyPlan);

    }

    @Test
    public void filteredExportShouldFailMissingSupplyPlanBeforeInventoryRepository() {

        SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        Mockito.when(supplyPlanRepository.findById(42L)).thenReturn(Optional.empty());
        InventoryPlanIntegrationService service =
                getService(
                        supplyPlanRepository,
                        inventoryPlanLinhaRepository,
                        Mockito.mock(ParametrosGlobaisService.class),
                        Mockito.mock(UnidadeMedidaProjectionFactory.class),
                        new InventoryPlanIntegrationMapper());

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> service.getInventoryPlanDTOList(42L));

        Assertions.assertEquals("Supply Plan Id not found", dataUploadException.getMessage());
        Mockito.verify(inventoryPlanLinhaRepository, Mockito.never()).customFindBySupplyPlan(Mockito.any());

    }

    @Test
    public void primaryKeyEnvelopeShouldUseSupplyPlanBatchRepository() {

        SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        ParametrosGlobaisService parametrosGlobaisService = Mockito.mock(ParametrosGlobaisService.class);
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                Mockito.mock(UnidadeMedidaProjectionFactory.class);
        Mockito.when(inventoryPlanLinhaRepository.customFindBySupplyPlanIdInForInventoryPlanExport(Set.of(42L, 43L)))
                .thenReturn(List.of());
        Mockito.when(parametrosGlobaisService.getParametrosGlobais())
                .thenReturn(getParametrosGlobais(new UnidadeMedida("CS")));
        Mockito.when(unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes())
                .thenReturn(Mockito.mock(UnidadeMedidaProjection.class));
        InventoryPlanIntegrationService service =
                getService(
                        supplyPlanRepository,
                        inventoryPlanLinhaRepository,
                        parametrosGlobaisService,
                        unidadeMedidaProjectionFactory,
                        new InventoryPlanIntegrationMapper());

        Collection<InventoryPlanIntegrationDataDto> dtoCollection =
                service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO(
                                42L,
                                "LOC_01",
                                "MAT_01",
                                LocalDateTime.of(2026, 1, 31, 0, 0)),
                        new InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO(
                                43L,
                                "LOC_02",
                                "MAT_02",
                                LocalDateTime.of(2026, 2, 28, 0, 0))));

        Assertions.assertTrue(dtoCollection.isEmpty());
        Mockito.verify(inventoryPlanLinhaRepository)
                .customFindBySupplyPlanIdInForInventoryPlanExport(Set.of(42L, 43L));
        Mockito.verify(inventoryPlanLinhaRepository, Mockito.never()).findAll();

    }

    @Test
    public void primaryKeyEnvelopeShouldRejectDuplicatesBeforeRepository() {

        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        InventoryPlanIntegrationService service =
                getService(
                        Mockito.mock(SupplyPlanRepository.class),
                        inventoryPlanLinhaRepository,
                        Mockito.mock(ParametrosGlobaisService.class),
                        Mockito.mock(UnidadeMedidaProjectionFactory.class),
                        new InventoryPlanIntegrationMapper());
        LocalDateTime referenceDate = LocalDateTime.of(2026, 1, 31, 0, 0);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.getPersistedEntityCollectionFromPrimaryKeyDtoCollection(List.of(
                        new InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO(
                                42L,
                                "LOC_01",
                                "MAT_01",
                                referenceDate),
                        new InventoryPlanIntegrationDataDto.InventoryPlanPrimaryKeyIntegrationDTO(
                                42L,
                                "LOC_01",
                                "MAT_01",
                                referenceDate))));
        Mockito.verify(inventoryPlanLinhaRepository, Mockito.never())
                .customFindBySupplyPlanIdInForInventoryPlanExport(Mockito.any());

    }

    @Test
    public void filteredExportShouldRejectInvalidDefaultSnpQuantityBeforePublication() {

        SupplyPlan supplyPlan = getSupplyPlan(42L);
        UnidadeMedida unidadeMedidaInventoryPlan = new UnidadeMedida("EA");
        UnidadeMedida unidadeMedidaPadraoSnp = new UnidadeMedida("CS");
        InventoryPlanLinha inventoryPlanLinha =
                getInventoryPlanLinha(
                        supplyPlan,
                        "LOC_01",
                        "MAT_01",
                        LocalDateTime.of(2026, 1, 31, 0, 0),
                        unidadeMedidaInventoryPlan);
        SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        ParametrosGlobaisService parametrosGlobaisService = Mockito.mock(ParametrosGlobaisService.class);
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                Mockito.mock(UnidadeMedidaProjectionFactory.class);
        UnidadeMedidaProjection unidadeMedidaProjection = Mockito.mock(UnidadeMedidaProjection.class);
        Mockito.when(supplyPlanRepository.findById(42L)).thenReturn(Optional.of(supplyPlan));
        Mockito.when(inventoryPlanLinhaRepository.customFindBySupplyPlan(supplyPlan))
                .thenReturn(List.of(inventoryPlanLinha));
        Mockito.when(parametrosGlobaisService.getParametrosGlobais())
                .thenReturn(getParametrosGlobais(unidadeMedidaPadraoSnp));
        Mockito.when(unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes())
                .thenReturn(unidadeMedidaProjection);
        Mockito.when(unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                        inventoryPlanLinha.getProduto(),
                        unidadeMedidaInventoryPlan,
                        unidadeMedidaPadraoSnp))
                .thenReturn(Double.NaN);
        InventoryPlanIntegrationService service =
                getService(
                        supplyPlanRepository,
                        inventoryPlanLinhaRepository,
                        parametrosGlobaisService,
                        unidadeMedidaProjectionFactory,
                        new InventoryPlanIntegrationMapper());

        IllegalArgumentException illegalArgumentException =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> service.getInventoryPlanDTOList(42L));

        Assertions.assertEquals(
                "Inventory Plan DTO at index 0 is invalid: Inventory Plan projected stock working version in default SNP unit must be finite: NaN.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void filteredExportShouldRejectDuplicatedDtoBeforePublication() {

        SupplyPlan supplyPlan = getSupplyPlan(42L);
        UnidadeMedida unidadeMedidaInventoryPlan = new UnidadeMedida("EA");
        UnidadeMedida unidadeMedidaPadraoSnp = new UnidadeMedida("CS");
        InventoryPlanLinha inventoryPlanLinha =
                getInventoryPlanLinha(
                        supplyPlan,
                        "LOC_01",
                        "MAT_01",
                        LocalDateTime.of(2026, 1, 31, 0, 0),
                        unidadeMedidaInventoryPlan);
        SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        ParametrosGlobaisService parametrosGlobaisService = Mockito.mock(ParametrosGlobaisService.class);
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                Mockito.mock(UnidadeMedidaProjectionFactory.class);
        UnidadeMedidaProjection unidadeMedidaProjection = Mockito.mock(UnidadeMedidaProjection.class);
        Mockito.when(supplyPlanRepository.findById(42L)).thenReturn(Optional.of(supplyPlan));
        Mockito.when(inventoryPlanLinhaRepository.customFindBySupplyPlan(supplyPlan))
                .thenReturn(List.of(
                        inventoryPlanLinha,
                        inventoryPlanLinha));
        Mockito.when(parametrosGlobaisService.getParametrosGlobais())
                .thenReturn(getParametrosGlobais(unidadeMedidaPadraoSnp));
        Mockito.when(unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes())
                .thenReturn(unidadeMedidaProjection);
        Mockito.when(unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                        inventoryPlanLinha.getProduto(),
                        unidadeMedidaInventoryPlan,
                        unidadeMedidaPadraoSnp))
                .thenReturn(2.0d);
        InventoryPlanIntegrationService service =
                getService(
                        supplyPlanRepository,
                        inventoryPlanLinhaRepository,
                        parametrosGlobaisService,
                        unidadeMedidaProjectionFactory,
                        new InventoryPlanIntegrationMapper());

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        () -> service.getInventoryPlanDTOList(42L));

        Assertions.assertEquals(
                "Inventory Plan persisted collection contains duplicated key supply plan 42 / location LOC_01 / material MAT_01 / reference date 2026-01-31T00:00 at index 1.",
                illegalStateException.getMessage());

    }

    @Test
    public void missingConversionShouldPopulateAllDefaultSnpColumnsWithMessage() {

        SupplyPlan supplyPlan = getSupplyPlan(42L);
        UnidadeMedida unidadeMedidaInventoryPlan = new UnidadeMedida("EA");
        UnidadeMedida unidadeMedidaPadraoSnp = new UnidadeMedida("CS");
        InventoryPlanLinha inventoryPlanLinha =
                getInventoryPlanLinha(
                        supplyPlan,
                        "LOC_01",
                        "MAT_01",
                        LocalDateTime.of(2026, 1, 31, 0, 0),
                        unidadeMedidaInventoryPlan);
        SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        ParametrosGlobaisService parametrosGlobaisService = Mockito.mock(ParametrosGlobaisService.class);
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                Mockito.mock(UnidadeMedidaProjectionFactory.class);
        UnidadeMedidaProjection unidadeMedidaProjection = Mockito.mock(UnidadeMedidaProjection.class);
        Mockito.when(supplyPlanRepository.findById(42L)).thenReturn(Optional.of(supplyPlan));
        Mockito.when(inventoryPlanLinhaRepository.customFindBySupplyPlan(supplyPlan))
                .thenReturn(List.of(inventoryPlanLinha));
        Mockito.when(parametrosGlobaisService.getParametrosGlobais())
                .thenReturn(getParametrosGlobais(unidadeMedidaPadraoSnp));
        Mockito.when(unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes())
                .thenReturn(unidadeMedidaProjection);
        Mockito.when(unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                        inventoryPlanLinha.getProduto(),
                        unidadeMedidaInventoryPlan,
                        unidadeMedidaPadraoSnp))
                .thenThrow(new UnitOfMeasureConversionException("No conversion available"));
        InventoryPlanIntegrationService service =
                getService(
                        supplyPlanRepository,
                        inventoryPlanLinhaRepository,
                        parametrosGlobaisService,
                        unidadeMedidaProjectionFactory,
                        new InventoryPlanIntegrationMapper());

        InventoryPlanIntegrationDataDto dto = service.getInventoryPlanDTOList(42L).get(0);

        Assertions.assertEquals(
                "No conversion from EA to CS",
                dto.projectedStockWorkingVersionDefaultSnpUom);
        Assertions.assertEquals(
                "No conversion from EA to CS",
                dto.projectedStockUnconstrainedVersionDefaultSnpUom);
        Assertions.assertEquals(
                "No conversion from EA to CS",
                dto.projectedStockConstrainedVersionDefaultSnpUom);

    }

    private static InventoryPlanIntegrationService getService(
            SupplyPlanRepository supplyPlanRepository,
            InventoryPlanLinhaRepository inventoryPlanLinhaRepository,
            ParametrosGlobaisService parametrosGlobaisService,
            UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory,
            InventoryPlanIntegrationMapper inventoryPlanIntegrationMapper) {

        InventoryPlanIntegrationService service = new InventoryPlanIntegrationService();
        ReflectionTestUtils.setField(service, "supplyPlanRepository", supplyPlanRepository);
        ReflectionTestUtils.setField(service, "inventoryPlanLinhaRepository", inventoryPlanLinhaRepository);
        ReflectionTestUtils.setField(service, "supplyPlanPersistedBaselinePreflight",
                Mockito.mock(SupplyPlanPersistedBaselinePreflight.class));
        ReflectionTestUtils.setField(service, "parametrosGlobaisService", parametrosGlobaisService);
        ReflectionTestUtils.setField(service, "unidadeMedidaProjectionFactory", unidadeMedidaProjectionFactory);
        ReflectionTestUtils.setField(service, "inventoryPlanIntegrationMapper", inventoryPlanIntegrationMapper);
        return service;

    }

    private static SupplyPlan getSupplyPlan(
            Long supplyPlanId) {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(supplyPlanId);
        return supplyPlan;

    }

    private static InventoryPlanLinha getInventoryPlanLinha(
            SupplyPlan supplyPlan,
            String locationId,
            String materialId,
            LocalDateTime referenceDate,
            UnidadeMedida unidadeMedida) {

        InventoryPlanLinha inventoryPlanLinha = new InventoryPlanLinha(
                new InventoryPlanLinha.InventoryPlanLinhaCompositeKey(
                        supplyPlan,
                        new Location(locationId),
                        new Produto(materialId),
                        referenceDate));
        inventoryPlanLinha.setUnidadeMedida(unidadeMedida);
        inventoryPlanLinha.setQuantidadeEstoqueSegurancaIrrestrito(2.0d);
        inventoryPlanLinha.setQuantidadeEstoqueMaximoIrrestrito(10.0d);
        inventoryPlanLinha.setQuantidadeEstoqueProjetadoTrabalho(-3.0d);
        inventoryPlanLinha.setQuantidadeEstoqueProjetadoIrrestrito(4.0d);
        inventoryPlanLinha.setQuantidadeEstoqueProjetadoRestrito(5.0d);
        return inventoryPlanLinha;

    }

    private static ParametrosGlobais getParametrosGlobais(
            UnidadeMedida unidadeMedidaPadraoSnp) {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setUnidadeMedidaPadraoSNP(unidadeMedidaPadraoSnp);
        return parametrosGlobais;

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

}
