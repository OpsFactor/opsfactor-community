package com.opsfactor.community.capability.supplyplanning.productionplan.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Contrato das queries de Production Plan Linha usadas por exportacoes de
 * planning data.
 */
class ProductionPlanLinhaRepositoryQueryContractTest {

    @Test
    void productionPlanProjectionSnapshotsShouldReturnListAndUseDistinctWithBomFetchJoin() throws Exception {

        Method locationSnapshotMethod =
                ProductionPlanLinhaRepository.class.getDeclaredMethod(
                        "customFindByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocationIncluindoListaTecnicaEMateriaisInput",
                        SupplyPlan.class,
                        Location.class);
        Method supplyPlanSnapshotMethod =
                ProductionPlanLinhaRepository.class.getDeclaredMethod(
                        "customFindByProductionPlanLinhaCompositeKeySupplyPlan",
                        SupplyPlan.class);

        assertProductionPlanSnapshotMethodPreservesCardinalityWithDistinct(locationSnapshotMethod);
        assertProductionPlanSnapshotMethodPreservesCardinalityWithDistinct(supplyPlanSnapshotMethod);

    }

    @Test
    void productionPlanVolumeExportQueriesShouldFetchHeaderDimensionsAndUseSupplyPlanEnvelope() throws Exception {

        Method singlePlanMethod =
                ProductionPlanLinhaRepository.class.getDeclaredMethod(
                        "customFindBySupplyPlanIdForProductionPlanVolumeExport",
                        Long.class);
        Method envelopeMethod =
                ProductionPlanLinhaRepository.class.getDeclaredMethod(
                        "customFindBySupplyPlanIdInForProductionPlanVolumeExport",
                        Collection.class);

        String singlePlanQuery =
                singlePlanMethod.getAnnotation(Query.class).value();
        String envelopeQuery =
                envelopeMethod.getAnnotation(Query.class).value();

        assertProductionPlanVolumeFetchJoins(singlePlanQuery);
        assertProductionPlanVolumeFetchJoins(envelopeQuery);
        Assertions.assertTrue(
                singlePlanQuery.contains("WHERE sp.id = :supplyPlanId"));
        Assertions.assertTrue(
                envelopeQuery.contains("WHERE sp.id IN :supplyPlanIds"));

    }

    @Test
    void productionPlanOccupationExportQueriesShouldFetchResourceDimensionsAndUseSupplyPlanEnvelope() throws Exception {

        Method singlePlanMethod =
                ProductionPlanLinhaRepository.class.getDeclaredMethod(
                        "customFindBySupplyPlanIdForProductionPlanOccupationExport",
                        Long.class);
        Method envelopeMethod =
                ProductionPlanLinhaRepository.class.getDeclaredMethod(
                        "customFindBySupplyPlanIdInForProductionPlanOccupationExport",
                        Collection.class);

        String singlePlanQuery =
                singlePlanMethod.getAnnotation(Query.class).value();
        String envelopeQuery =
                envelopeMethod.getAnnotation(Query.class).value();

        assertProductionPlanOccupationFetchJoins(singlePlanQuery);
        assertProductionPlanOccupationFetchJoins(envelopeQuery);
        Assertions.assertTrue(
                singlePlanQuery.contains("WHERE sp.id = :supplyPlanId"));
        Assertions.assertTrue(
                envelopeQuery.contains("WHERE sp.id IN :supplyPlanIds"));

    }

    private static void assertProductionPlanVolumeFetchJoins(
            String query) {

        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.supplyPlan sp"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.location loc"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.versaoProducao vp"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.roteiro rot"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH rot.location"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH rot.materialOutput"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ppl.productionPlanLinhaCompositeKey.listaTecnica lt"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.location"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH lt.materialOutput"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ppl.materialOutput mat"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH ppl.unidadeMedida uom"));

    }

    private static void assertProductionPlanSnapshotMethodPreservesCardinalityWithDistinct(
            Method method) {

        Assertions.assertEquals(
                List.class,
                method.getReturnType(),
                method.getName() + " deve retornar List para preservar cardinalidade ate a validation.");

        String query = method.getAnnotation(Query.class).value();
        Assertions.assertTrue(
                query.contains("SELECT DISTINCT ppl FROM ProductionPlanLinha ppl"),
                method.getName() + " deve usar SELECT DISTINCT no fetch join de BOM/componentes.");

    }

    private static void assertProductionPlanOccupationFetchJoins(
            String query) {

        assertProductionPlanVolumeFetchJoins(query);
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH rot.operacaoRoteiroSet opr"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH opr.recursoProdutivo rp"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH rp.location"));
        Assertions.assertTrue(query.contains("LEFT JOIN FETCH rp.unidadeMedidaCapacidadeEmUom"));

    }

}
