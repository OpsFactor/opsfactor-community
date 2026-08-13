package com.opsfactor.community.capability.supplyplanning.productionplan.repository;

import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

/**
 * Testes de contrato do batch JDBC de Production Plan.
 */
public class ProductionPlanLinhaDAOTest {

    @Test
    public void emptyCollectionsShouldBeNoOpWithoutJdbcAccess() {

        ProductionPlanLinhaDAO productionPlanLinhaDAO = new ProductionPlanLinhaDAO();

        Assertions.assertDoesNotThrow(
                () -> productionPlanLinhaDAO.saveInBatch(List.of()));
        Assertions.assertDoesNotThrow(
                () -> productionPlanLinhaDAO.deleteInBatch(List.of()));

    }

    @Test
    public void batchShouldRejectBrokenCollectionsBeforeJdbcAccess() {

        ProductionPlanLinhaDAO productionPlanLinhaDAO = new ProductionPlanLinhaDAO();
        List<ProductionPlanLinha> linhasComItemNulo = new ArrayList<>();
        linhasComItemNulo.add(null);

        IllegalArgumentException colecaoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> productionPlanLinhaDAO.saveInBatch(null));
        IllegalArgumentException itemAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> productionPlanLinhaDAO.saveInBatch(linhasComItemNulo));
        IllegalArgumentException chaveAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> productionPlanLinhaDAO.saveInBatch(
                        List.of(new ProductionPlanLinha())));

        Assertions.assertEquals(
                "Production Plan JDBC batch collection is required.",
                colecaoAusenteException.getMessage());
        Assertions.assertEquals(
                "Production Plan JDBC batch line at index 0 is required.",
                itemAusenteException.getMessage());
        Assertions.assertEquals(
                "Production Plan JDBC batch line at index 0 must have a composite key.",
                chaveAusenteException.getMessage());

    }

    @Test
    public void communityShouldUsePostgreSqlConflictUpsertWithoutJpaMerge() {

        ProductionPlanLinhaDAO productionPlanLinhaDAO = new ProductionPlanLinhaDAO();
        String sql = productionPlanLinhaDAO.getSqlUpsertProductionPlanLinha();

        Assertions.assertTrue(sql.contains("ON CONFLICT"));
        Assertions.assertTrue(sql.contains("excluded.quantidade_ordem_planejada_producao_irrestrita"));
        Assertions.assertFalse(sql.contains("ON DUPLICATE KEY"));

    }

}
