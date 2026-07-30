package com.opsfactor.community.capability.transactionaldata.sales.sellout.repository;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.FirstLastByLocation;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.FirstLastByMaterial;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.FirstLastByMaterialLocation;
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
 * Contrato estrutural do repository Community de sell-out.
 *
 * <p>As queries agregadas precisam retornar {@link List} para preservar a
 * cardinalidade exata do snapshot. Duplicidades estruturais devem chegar ate a
 * factory, onde a chave funcional e validada com mensagem rastreavel.</p>
 */
class SelloutRepositoryContractTest {

    @Test
    void selloutRepositoryShouldExposeJpaContract() throws Exception {

        assertRepositoryAnnotation();
        assertRepositoryGenericTypes();
        assertAllAggregationMethodsReturnList();
        assertEntityLookupMethod("customFindBySelloutIdIn", Collection.class);
        assertEntityLookupMethod("customFindByDataVendaBetween", LocalDateTime.class, LocalDateTime.class);
        assertEntityLookupMethod(
                "customFindByDataVendaBetweenAndLocationDestinoTypeIn",
                LocalDateTime.class,
                LocalDateTime.class,
                Collection.class);
        assertEntityLookupMethod(
                "customFindByDataVendaBetweenMaterialInAndLocationIn",
                LocalDateTime.class,
                LocalDateTime.class,
                Collection.class,
                Collection.class);
        assertEntityLookupMethod("customFindAll");
        assertFirstLastMethod(
                "findFirstLastSelloutPorMaterialLocation",
                FirstLastByMaterialLocation.class);
        assertFirstLastMethod(
                "findFirstLastSelloutPorLocation",
                FirstLastByLocation.class);
        assertFirstLastMethod(
                "findFirstLastSelloutPorMaterial",
                FirstLastByMaterial.class);
        Assertions.assertEquals(
                LocalDateTime.class,
                SelloutRepository.class.getMethod("customFindPrimeiroSellout").getReturnType());
        Assertions.assertEquals(
                LocalDateTime.class,
                SelloutRepository.class.getMethod("customFindUltimoSellout").getReturnType());

    }

    private void assertRepositoryAnnotation() {

        Assertions.assertTrue(SelloutRepository.class.isAnnotationPresent(Repository.class));

    }

    private void assertRepositoryGenericTypes() {

        ParameterizedType jpaRepositoryType = resolveJpaRepositoryType();

        Assertions.assertEquals(
                Sellout.class,
                jpaRepositoryType.getActualTypeArguments()[0]);
        Assertions.assertEquals(
                String.class,
                jpaRepositoryType.getActualTypeArguments()[1]);

    }

    private void assertAllAggregationMethodsReturnList() {

        int aggregationMethodCount = 0;
        for (Method method : SelloutRepository.class.getDeclaredMethods()) {
            if (method.getName().startsWith("consolidatedSelloutBy")) {
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
                "SelloutRepository must expose aggregate snapshot methods.");

    }

    private void assertSupportedAggregationProjection(Method aggregationMethod) {

        ParameterizedType parameterizedReturnType =
                (ParameterizedType) aggregationMethod.getGenericReturnType();
        Type projectionType = parameterizedReturnType.getActualTypeArguments()[0];

        Assertions.assertTrue(
                projectionType.equals(AggregatedByMaterialUOM.class)
                        || projectionType.equals(AggregatedByLocationMaterialUOM.class)
                        || projectionType.equals(AggregatedByMaterialUOMDate.class)
                        || projectionType.equals(AggregatedByLocationMaterialUOMDate.class),
                aggregationMethod.getName() + " returned an unexpected aggregate projection type.");

    }

    private void assertFirstLastMethod(
            String methodName,
            Class<?> expectedProjectionType) throws Exception {

        Method firstLastMethod = SelloutRepository.class.getMethod(methodName);

        Assertions.assertEquals(List.class, firstLastMethod.getReturnType());
        ParameterizedType parameterizedReturnType = (ParameterizedType) firstLastMethod.getGenericReturnType();
        Assertions.assertEquals(
                expectedProjectionType,
                parameterizedReturnType.getActualTypeArguments()[0]);

    }

    private void assertEntityLookupMethod(
            String methodName,
            Class<?>... parameterTypes) throws Exception {

        Method entityLookupMethod = SelloutRepository.class.getMethod(methodName, parameterTypes);

        Assertions.assertEquals(List.class, entityLookupMethod.getReturnType());
        ParameterizedType parameterizedReturnType = (ParameterizedType) entityLookupMethod.getGenericReturnType();
        Assertions.assertEquals(
                Sellout.class,
                parameterizedReturnType.getActualTypeArguments()[0]);

    }

    private ParameterizedType resolveJpaRepositoryType() {

        for (Type genericInterface : SelloutRepository.class.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType parameterizedType
                    && parameterizedType.getRawType().equals(JpaRepository.class)) {
                return parameterizedType;
            }
        }

        throw new AssertionError("Repository must extend JpaRepository with explicit entity and id types");

    }

}
