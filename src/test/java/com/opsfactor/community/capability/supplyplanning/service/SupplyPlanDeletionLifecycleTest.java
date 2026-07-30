package com.opsfactor.community.capability.supplyplanning.service;

import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaDAO;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.service.CapacidadeEfetivaSupplyPlanService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Contrato do lifecycle Community de exclusão de Supply Plan.
 */
class SupplyPlanDeletionLifecycleTest {

    @Test
    void deleteShouldClearReferencesBeforeChildrenAndHeader() {

        LifecycleFixture fixture = new LifecycleFixture();

        fixture.service.deleteSupplyPlan(10L);

        InOrder deleteOrder = Mockito.inOrder(
                fixture.supplyPlanRepository,
                fixture.distributionPlanItemRepository,
                fixture.productionPlanLinhaRepository,
                fixture.inventoryPlanLinhaRepository,
                fixture.capacidadeEfetivaSupplyPlanService,
                fixture.demandaDiretaConsideradaLinhaDAO);
        deleteOrder.verify(fixture.supplyPlanRepository)
                .clearSupplyPlanReferenceByReferencedSupplyPlanId(10L);
        deleteOrder.verify(fixture.distributionPlanItemRepository)
                .removeByKeySupplyPlanId(10L);
        deleteOrder.verify(fixture.productionPlanLinhaRepository)
                .removeByProductionPlanLinhaCompositeKeySupplyPlanId(10L);
        deleteOrder.verify(fixture.inventoryPlanLinhaRepository)
                .removeByInventoryPlanLinhaCompositeKeySupplyPlanId(10L);
        deleteOrder.verify(fixture.capacidadeEfetivaSupplyPlanService).removeBySupplyPlanId(10L);
        deleteOrder.verify(fixture.demandaDiretaConsideradaLinhaDAO).deleteBySupplyPlanId(10L);
        deleteOrder.verify(fixture.supplyPlanRepository).deleteById(10L);

    }

    @Test
    void referenceCleanupFailureShouldPreventChildrenAndHeaderDeletion() {

        LifecycleFixture fixture = new LifecycleFixture();
        IllegalStateException cleanupFailure = new IllegalStateException("reference cleanup failed");
        Mockito.doThrow(cleanupFailure)
                .when(fixture.supplyPlanRepository)
                .clearSupplyPlanReferenceByReferencedSupplyPlanId(10L);

        Assertions.assertSame(
                cleanupFailure,
                Assertions.assertThrows(
                        IllegalStateException.class,
                        () -> fixture.service.deleteSupplyPlan(10L)));
        Mockito.verifyNoInteractions(
                fixture.distributionPlanItemRepository,
                fixture.productionPlanLinhaRepository,
                fixture.inventoryPlanLinhaRepository,
                fixture.capacidadeEfetivaSupplyPlanService,
                fixture.demandaDiretaConsideradaLinhaDAO);
        Mockito.verify(fixture.supplyPlanRepository, Mockito.never()).deleteById(Mockito.anyLong());

    }

    @Test
    void repositoryShouldUseOneExplicitBulkUpdateAndServiceShouldBeTransactional() throws Exception {

        Method clearReferenceMethod = SupplyPlanRepository.class.getMethod(
                "clearSupplyPlanReferenceByReferencedSupplyPlanId",
                Long.class);
        Modifying modifying = clearReferenceMethod.getAnnotation(Modifying.class);
        Query query = clearReferenceMethod.getAnnotation(Query.class);
        Method deleteMethod = SupplyPlanService.class.getMethod("deleteSupplyPlan", Long.class);

        Assertions.assertEquals(int.class, clearReferenceMethod.getReturnType());
        Assertions.assertNotNull(clearReferenceMethod.getAnnotation(Transactional.class));
        Assertions.assertNotNull(modifying);
        Assertions.assertTrue(modifying.flushAutomatically());
        Assertions.assertFalse(modifying.clearAutomatically());
        Assertions.assertTrue(query.value().startsWith("UPDATE SupplyPlan"));
        Assertions.assertTrue(query.value().contains("SET referencingSupplyPlan.supplyPlanReferencia = null"));
        Assertions.assertTrue(query.value().contains("supplyPlanReferencia.id = :referencedSupplyPlanId"));
        Assertions.assertEquals(
                "referencedSupplyPlanId",
                clearReferenceMethod.getParameters()[0].getAnnotation(Param.class).value());
        Assertions.assertNotNull(deleteMethod.getAnnotation(Transactional.class));

    }

    private static final class LifecycleFixture {

        private final SupplyPlanService service = new SupplyPlanService();
        private final SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        private final DistributionPlanItemRepository distributionPlanItemRepository =
                Mockito.mock(DistributionPlanItemRepository.class);
        private final ProductionPlanLinhaRepository productionPlanLinhaRepository =
                Mockito.mock(ProductionPlanLinhaRepository.class);
        private final InventoryPlanLinhaRepository inventoryPlanLinhaRepository =
                Mockito.mock(InventoryPlanLinhaRepository.class);
        private final CapacidadeEfetivaSupplyPlanService capacidadeEfetivaSupplyPlanService =
                Mockito.mock(CapacidadeEfetivaSupplyPlanService.class);
        private final DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                Mockito.mock(DemandaDiretaConsideradaLinhaDAO.class);

        private LifecycleFixture() {

            setField(service, "supplyPlanRepository", supplyPlanRepository);
            setField(service, "distributionPlanItemRepository", distributionPlanItemRepository);
            setField(service, "productionPlanLinhaRepository", productionPlanLinhaRepository);
            setField(service, "inventoryPlanLinhaRepository", inventoryPlanLinhaRepository);
            setField(service, "capacidadeEfetivaSupplyPlanService", capacidadeEfetivaSupplyPlanService);
            setField(service, "demandaDiretaConsideradaLinhaDAO", demandaDiretaConsideradaLinhaDAO);

        }

        private void verifyNoInteractions() {

            Mockito.verifyNoInteractions(
                    supplyPlanRepository,
                    distributionPlanItemRepository,
                    productionPlanLinhaRepository,
                    inventoryPlanLinhaRepository,
                    capacidadeEfetivaSupplyPlanService,
                    demandaDiretaConsideradaLinhaDAO);

        }

    }

    private static void setField(Object target, String fieldName, Object value) {

        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException reflectionFailure) {
            throw new IllegalStateException(reflectionFailure);
        }

    }

}
