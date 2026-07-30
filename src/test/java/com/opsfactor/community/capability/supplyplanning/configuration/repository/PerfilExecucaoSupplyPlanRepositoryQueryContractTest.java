package com.opsfactor.community.capability.supplyplanning.configuration.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Contrato das consultas usadas pela listagem e pelo detalhe de perfis de
 * execucao de Supply Planning no Community.
 */
class PerfilExecucaoSupplyPlanRepositoryQueryContractTest {

    @Test
    void supplyProfileSnapshotsShouldFetchInventoryPoliciesThroughTheCompositeKey()
            throws Exception {

        Method listMethod = PerfilExecucaoSupplyPlanRepository.class.getDeclaredMethod(
                "customFindAll");
        Method detailMethod = PerfilExecucaoSupplyPlanRepository.class.getDeclaredMethod(
                "customFindById",
                String.class);

        assertInventoryPolicyFetchSnapshot(listMethod, List.class);
        assertInventoryPolicyFetchSnapshot(detailMethod, Optional.class);
        Assertions.assertTrue(
                detailMethod.getAnnotation(Query.class).value()
                        .contains("WHERE pesp.id = :perfilExecucaoSupplyPlanId"));

    }

    /**
     * A politica e associada pela chave composta do filho. O fetch explicito
     * evita uma consulta lazy adicional para cada politica exibida pelo mapper.
     */
    private static void assertInventoryPolicyFetchSnapshot(
            Method method,
            Class<?> expectedReturnType) {

        Assertions.assertEquals(expectedReturnType, method.getReturnType());

        String query = method.getAnnotation(Query.class).value();
        Assertions.assertTrue(
                query.contains("SELECT DISTINCT pesp FROM PerfilExecucaoSupplyPlan pesp"));
        Assertions.assertTrue(
                query.contains("LEFT JOIN FETCH pesp.setPerfilExecucaoPoliticaEstoques pepe"));
        Assertions.assertTrue(
                query.contains(
                        "LEFT JOIN FETCH pepe.perfilExecucaoPoliticaEstoquesCompositeKey.politicaEstoques"));

    }

}
