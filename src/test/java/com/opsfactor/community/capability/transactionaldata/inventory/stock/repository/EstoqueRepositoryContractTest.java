package com.opsfactor.community.capability.transactionaldata.inventory.stock.repository;

import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Contrato estrutural do repository Community de estoque transacional.
 *
 * <p>As queries agregadas precisam retornar {@link List} para preservar a
 * cardinalidade exata do snapshot. Duplicidades estruturais devem chegar ate a
 * factory, onde a chave funcional e validada antes de popular os indices de
 * estoque.</p>
 */
class EstoqueRepositoryContractTest {

    @Test
    void estoqueRepositoryShouldExposeJpaContract() throws Exception {

        assertRepositoryAnnotation();
        assertRepositoryGenericTypes();
        assertAllAggregationMethodsReturnList();
        assertEntityLookupMethod("customFindUploadBatchEnvelope",
                LocalDateTime.class,
                LocalDateTime.class,
                Collection.class,
                Collection.class);
        Assertions.assertEquals(
                List.class,
                EstoqueRepository.class.getMethod(
                                "findByEstoqueCompositeKeyDataReferenciaBetween",
                                LocalDateTime.class,
                                LocalDateTime.class)
                        .getReturnType());

    }

    private void assertRepositoryAnnotation() {

        Assertions.assertTrue(EstoqueRepository.class.isAnnotationPresent(Repository.class));

    }

    private void assertRepositoryGenericTypes() {

        ParameterizedType jpaRepositoryType = resolveJpaRepositoryType();

        Assertions.assertEquals(
                Estoque.class,
                jpaRepositoryType.getActualTypeArguments()[0]);
        Assertions.assertEquals(
                Estoque.EstoqueCompositeKey.class,
                jpaRepositoryType.getActualTypeArguments()[1]);

    }

    private void assertAllAggregationMethodsReturnList() {

        int aggregationMethodCount = 0;
        for (Method method : EstoqueRepository.class.getDeclaredMethods()) {
            if (method.getName().startsWith("consolidatedStockQuantityBy")) {
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
                "EstoqueRepository must expose aggregate snapshot methods.");

    }

    private void assertSupportedAggregationProjection(Method aggregationMethod) {

        ParameterizedType parameterizedReturnType =
                (ParameterizedType) aggregationMethod.getGenericReturnType();
        Type projectionType = parameterizedReturnType.getActualTypeArguments()[0];

        Assertions.assertTrue(
                projectionType.equals(AggregatedByLocationMaterialUOM.class)
                        || projectionType.equals(AggregatedByLocationMaterialUOMDate.class)
                        || projectionType.equals(AggregatedByMaterialUOM.class),
                aggregationMethod.getName() + " returned an unexpected aggregate projection type.");

    }

    private void assertEntityLookupMethod(
            String methodName,
            Class<?>... parameterTypes) throws Exception {

        Method entityLookupMethod = EstoqueRepository.class.getMethod(methodName, parameterTypes);

        Assertions.assertEquals(List.class, entityLookupMethod.getReturnType());
        ParameterizedType parameterizedReturnType = (ParameterizedType) entityLookupMethod.getGenericReturnType();
        Assertions.assertEquals(
                Estoque.class,
                parameterizedReturnType.getActualTypeArguments()[0]);

    }

    private ParameterizedType resolveJpaRepositoryType() {

        for (Type genericInterface : EstoqueRepository.class.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType parameterizedType
                    && parameterizedType.getRawType().equals(JpaRepository.class)) {
                return parameterizedType;
            }
        }

        throw new AssertionError("Repository must extend JpaRepository with explicit entity and id types");

    }

}
