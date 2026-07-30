package com.opsfactor.community.capability.masterdata.production.billofmaterials.repository;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * Contrato da fotografia de listas técnicas consumida pela listagem Community.
 */
class ListaTecnicaRepositoryQueryContractTest {

    @Test
    void billOfMaterialsListSnapshotShouldFetchAdministrativeManyToOneAttributes()
            throws Exception {

        Method method = ListaTecnicaRepository.class.getDeclaredMethod(
                "customFindAllWithLocationMaterialOutputAndUnidadeMedidaMaterialOutput");

        Assertions.assertEquals(List.class, method.getReturnType());

        String query = method.getAnnotation(Query.class).value();
        Assertions.assertTrue(query.contains("SELECT DISTINCT lt FROM ListaTecnica lt"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.location"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.materialOutput"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.unidadeMedidaMaterialOutput"));

    }

}
