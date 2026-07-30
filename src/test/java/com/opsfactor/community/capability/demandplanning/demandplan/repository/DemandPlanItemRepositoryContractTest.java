package com.opsfactor.community.capability.demandplanning.demandplan.repository;

import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Contrato estrutural do repository Community de linhas de Demand Plan.
 *
 * <p>Os agregados quantitativos por calendario precisam expor {@link List}
 * para preservar a cardinalidade retornada pelo banco ate a validacao da
 * projection consumidora. Um {@code Set} nesse boundary poderia mascarar
 * duplicidade estrutural antes de uma mensagem de erro rastreavel.</p>
 */
class DemandPlanItemRepositoryContractTest {

    @Test
    void demandPlanItemAggregatesShouldPreserveSnapshotCardinalityAsList() {

        assertRepositoryAnnotation();
        assertRepositoryGenericTypes();
        assertAllAggregationMethodsReturnList();

    }

    @Test
    void locationScopedSnapshotShouldFetchTheUnitOfMeasureInTheSameBatch() throws Exception {

        Method locationScopedSnapshotMethod = DemandPlanItemRepository.class.getDeclaredMethod(
                "customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInLocations",
                Long.class,
                java.util.Collection.class);
        Query query = locationScopedSnapshotMethod.getAnnotation(Query.class);

        Assertions.assertNotNull(query);
        Assertions.assertTrue(
                query.value().replaceAll("\\s+", " ").toLowerCase()
                        .contains("left join fetch dpl.unidademedida"),
                "The location-scoped Demand Plan snapshot must fetch the UOM used by batch post-processors.");

    }

    private void assertRepositoryAnnotation() {

        Assertions.assertTrue(DemandPlanItemRepository.class.isAnnotationPresent(Repository.class));

    }

    private void assertRepositoryGenericTypes() {

        ParameterizedType jpaRepositoryType = resolveJpaRepositoryType();

        Assertions.assertEquals(
                DemandPlanItem.class,
                jpaRepositoryType.getActualTypeArguments()[0]);
        Assertions.assertEquals(
                DemandPlanItem.DemandPlanItemKey.class,
                jpaRepositoryType.getActualTypeArguments()[1]);

    }

    private void assertAllAggregationMethodsReturnList() {

        int aggregationMethodCount = 0;
        for (Method method : DemandPlanItemRepository.class.getDeclaredMethods()) {
            if (method.getName().startsWith("consolidatedDemandPlanItemBy")) {
                aggregationMethodCount++;
                Assertions.assertEquals(
                        List.class,
                        method.getReturnType(),
                        method.getName() + " must preserve snapshot cardinality as List.");
                assertSupportedAggregationProjection(method);
            }
        }

        Assertions.assertTrue(
                aggregationMethodCount > 0,
                "DemandPlanItemRepository must expose aggregate snapshot methods.");

    }

    private void assertSupportedAggregationProjection(Method aggregationMethod) {

        ParameterizedType parameterizedReturnType =
                (ParameterizedType) aggregationMethod.getGenericReturnType();
        Type projectionType = parameterizedReturnType.getActualTypeArguments()[0];

        Assertions.assertEquals(
                AggregatedByLocationMaterialUOMDate.class,
                projectionType,
                aggregationMethod.getName() + " returned an unexpected aggregate projection type.");

    }

    private ParameterizedType resolveJpaRepositoryType() {

        for (Type genericInterface : DemandPlanItemRepository.class.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType parameterizedType
                    && parameterizedType.getRawType().equals(JpaRepository.class)) {
                return parameterizedType;
            }
        }

        throw new AssertionError("Repository must extend JpaRepository with explicit entity and id types");

    }

}
