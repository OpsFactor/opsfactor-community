package com.opsfactor.community.capability.masterdata.production.billofmaterials.repository;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * Contrato da fotografia de componentes de BOM consumida pela listagem Community.
 */
class ListaTecnicaComponenteRepositoryQueryContractTest {

    @Test
    void billOfMaterialsComponentListSnapshotShouldFetchAdministrativeManyToOneAttributes()
            throws Exception {

        Method method = ListaTecnicaComponenteRepository.class.getDeclaredMethod("customFindAll");

        Assertions.assertEquals(List.class, method.getReturnType());

        String query = method.getAnnotation(Query.class).value();
        Assertions.assertTrue(query.contains("SELECT ltc FROM ListaTecnicaComponente ltc"));
        Assertions.assertTrue(query.contains(
                "LEFT JOIN FETCH ltc.listaTecnicaComponenteCompositeKey.listaTecnica lt"));
        Assertions.assertTrue(query.contains(
                "LEFT JOIN FETCH ltc.listaTecnicaComponenteCompositeKey.materialComponente mc"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ltc.unidadeMedidaMaterialComponente"));

    }

}
