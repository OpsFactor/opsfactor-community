package com.opsfactor.community.capability.masterdata.production.billofmaterials.repository;

import java.lang.reflect.Method;
import java.util.Collection;
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

    @Test
    void supplyNetworkSnapshotsShouldFetchComponentMaterialThroughEmbeddedId()
            throws Exception {

        assertComponentMaterialFetchPath(
                "customFindAllByLocationInAndMaterialOutputInFetchListaTecnicaComponente",
                Collection.class,
                Collection.class);
        assertComponentMaterialFetchPath(
                "customFindAllByLocationInFetchListaTecnicaComponente",
                Collection.class);

    }

    /**
     * Confirma que os snapshots navegam pelo caminho JPA real da chave composta.
     */
    private static void assertComponentMaterialFetchPath(
            String methodName,
            Class<?>... parameterTypes)
            throws Exception {

        Method method = ListaTecnicaRepository.class.getDeclaredMethod(
                methodName,
                parameterTypes);

        Assertions.assertEquals(List.class, method.getReturnType());

        String query = method.getAnnotation(Query.class).value();
        Assertions.assertTrue(query.contains(
                "LEFT JOIN FETCH "
                        + "ltc.listaTecnicaComponenteCompositeKey.materialComponente"));
        Assertions.assertFalse(query.contains("LEFT JOIN FETCH ltc.materialComponente"));

    }

}
