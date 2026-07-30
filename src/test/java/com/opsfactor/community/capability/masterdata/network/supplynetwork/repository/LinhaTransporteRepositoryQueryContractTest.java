package com.opsfactor.community.capability.masterdata.network.supplynetwork.repository;

import java.lang.reflect.Method;
import java.util.List;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * Contrato das fotografias administrativas de transportation lanes Community.
 */
class LinhaTransporteRepositoryQueryContractTest {

    @Test
    void transportationLaneListSnapshotShouldFetchEveryRelationReadByTheMapper()
            throws Exception {

        Method method = LinhaTransporteRepository.class.getDeclaredMethod(
                "customFindForFrontByVersaoMalha",
                VersaoMalha.class);

        Assertions.assertEquals(List.class, method.getReturnType());

        String query = method.getAnnotation(Query.class).value();
        Assertions.assertTrue(query.contains("SELECT DISTINCT lt FROM LinhaTransporte lt"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.linhaTransporteCompositeKey.versaoMalha vm"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.unidadeMedidaLoteMinimoMultiploTransporte uom"));

    }

    @Test
    void transportationLaneMaterialListSnapshotShouldFetchEveryRelationReadByTheMapper()
            throws Exception {

        Method method = LinhaTransporteProdutoRepository.class.getDeclaredMethod(
                "customFindForFrontByVersaoMalha",
                VersaoMalha.class);

        Assertions.assertEquals(List.class, method.getReturnType());

        String query = method.getAnnotation(Query.class).value();
        Assertions.assertTrue(query.contains("SELECT DISTINCT ltp FROM LinhaTransporteProduto ltp"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.produto p"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ltp.linhaTransporteProdutoCompositeKey.linhaTransporte lt"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.linhaTransporteCompositeKey.versaoMalha vm"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationOrigem lo"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.linhaTransporteCompositeKey.locationDestino ld"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.unidadeMedidaLoteMinimoMultiploTransporte laneUom"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ltp.unidadeMedidaLoteMinimoMultiploTransporte overrideUom"));

    }

}
