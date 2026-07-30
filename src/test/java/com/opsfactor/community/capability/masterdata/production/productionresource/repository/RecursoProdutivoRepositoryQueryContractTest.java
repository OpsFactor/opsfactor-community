package com.opsfactor.community.capability.masterdata.production.productionresource.repository;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * Contrato da fotografia administrativa de recursos produtivos Community.
 */
class RecursoProdutivoRepositoryQueryContractTest {

    @Test
    void productionResourceListSnapshotShouldFetchLocation() throws Exception {

        Method method = RecursoProdutivoRepository.class.getDeclaredMethod(
                "customFindAllWithLocation");

        Assertions.assertEquals(List.class, method.getReturnType());

        String query = method.getAnnotation(Query.class).value();
        Assertions.assertTrue(query.contains("SELECT DISTINCT rp FROM RecursoProdutivo rp"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH rp.location"));

    }

}
