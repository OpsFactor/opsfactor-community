package com.opsfactor.community.capability.supplyplanning.distributionplan.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

class DistributionPlanItemMaterialFlowsRepositoryContractTest {

    @Test
    void shouldFetchOriginAndDestinationInTheSingleMaterialFlowsSnapshotQuery() throws Exception {

        Method method = DistributionPlanItemRepository.class.getDeclaredMethod(
                "customFindBySupplyPlanId", Long.class);
        Query query = method.getAnnotation(Query.class);

        Assertions.assertNotNull(query);
        Assertions.assertTrue(query.value().contains(
                "LEFT JOIN FETCH dpl.key.locationOrigem lo"));
        Assertions.assertTrue(query.value().contains(
                "LEFT JOIN FETCH dpl.key.locationDestino ld"));
        Assertions.assertTrue(query.value().contains("WHERE sp.id = :supplyPlanId"));

    }
}
