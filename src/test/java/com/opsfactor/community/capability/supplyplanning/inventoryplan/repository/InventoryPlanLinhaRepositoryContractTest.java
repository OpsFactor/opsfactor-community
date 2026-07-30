package com.opsfactor.community.capability.supplyplanning.inventoryplan.repository;

import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDatePlanType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Contrato estrutural do repository Community de linhas de Inventory Plan.
 *
 * <p>Os agregados quantitativos por calendario precisam expor {@link List}
 * para preservar a cardinalidade retornada pelo banco ate a validacao da
 * projection consumidora. Um {@code Set} nesse boundary poderia esconder
 * duplicidade de snapshot antes do gate funcional de Supply Planning.</p>
 */
class InventoryPlanLinhaRepositoryContractTest {

    @Test
    void inventoryPlanLineAggregatesShouldPreserveSnapshotCardinalityAsList() {

        assertRepositoryAnnotation();
        assertRepositoryGenericTypes();
        assertAllAggregationMethodsReturnList();

    }

    private void assertRepositoryAnnotation() {

        Assertions.assertTrue(InventoryPlanLinhaRepository.class.isAnnotationPresent(Repository.class));

    }

    private void assertRepositoryGenericTypes() {

        ParameterizedType jpaRepositoryType = resolveJpaRepositoryType();

        Assertions.assertEquals(
                InventoryPlanLinha.class,
                jpaRepositoryType.getActualTypeArguments()[0]);
        Assertions.assertEquals(
                InventoryPlanLinha.InventoryPlanLinhaCompositeKey.class,
                jpaRepositoryType.getActualTypeArguments()[1]);

    }

    private void assertAllAggregationMethodsReturnList() {

        int aggregationMethodCount = 0;
        for (Method method : InventoryPlanLinhaRepository.class.getDeclaredMethods()) {
            if (method.getName().startsWith("consolidatedInventoryPlanLinhaBy")) {
                aggregationMethodCount++;
                Assertions.assertEquals(
                        List.class,
                        method.getReturnType(),
                        method.getName() + " must preserve snapshot cardinality as List.");
                assertSupportedAggregationProjection(method);
            }
        }

        Assertions.assertEquals(
                3,
                aggregationMethodCount,
                "InventoryPlanLinhaRepository must expose month, week and day aggregate snapshots.");

    }

    private void assertSupportedAggregationProjection(Method aggregationMethod) {

        ParameterizedType parameterizedReturnType =
                (ParameterizedType) aggregationMethod.getGenericReturnType();
        Type projectionType = parameterizedReturnType.getActualTypeArguments()[0];

        Assertions.assertEquals(
                AggregatedByLocationMaterialUOMDatePlanType.class,
                projectionType,
                aggregationMethod.getName() + " returned an unexpected aggregate projection type.");

    }

    private ParameterizedType resolveJpaRepositoryType() {

        for (Type genericInterface : InventoryPlanLinhaRepository.class.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType parameterizedType
                    && parameterizedType.getRawType().equals(JpaRepository.class)) {
                return parameterizedType;
            }
        }

        throw new AssertionError("Repository must extend JpaRepository with explicit entity and id types");

    }

}
