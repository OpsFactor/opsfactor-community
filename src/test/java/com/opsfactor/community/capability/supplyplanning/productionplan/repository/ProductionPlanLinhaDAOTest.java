package com.opsfactor.community.capability.supplyplanning.productionplan.repository;

import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public void sqliteShouldUseNativeConflictUpsertWithoutJpaMerge() throws Exception {

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("SQLite");
        when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class)))
                .thenAnswer(invocation -> ((org.springframework.jdbc.core.ConnectionCallback<?>) invocation
                        .getArgument(0)).doInConnection(connection));

        ProductionPlanLinhaDAO productionPlanLinhaDAO = new ProductionPlanLinhaDAO();
        setField(productionPlanLinhaDAO, "jdbcTemplate", jdbcTemplate);

        String sql = invokeSqlResolver(productionPlanLinhaDAO);

        Assertions.assertTrue(sql.contains("ON CONFLICT"));
        Assertions.assertTrue(sql.contains("excluded.quantidade_ordem_planejada_producao_irrestrita"));
        Assertions.assertFalse(sql.contains("ON DUPLICATE KEY"));

    }

    @Test
    public void postgreSqlShouldUseNativeConflictUpsertWithoutJpaMerge() throws Exception {

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class)))
                .thenAnswer(invocation -> ((org.springframework.jdbc.core.ConnectionCallback<?>) invocation
                        .getArgument(0)).doInConnection(connection));

        ProductionPlanLinhaDAO productionPlanLinhaDAO = new ProductionPlanLinhaDAO();
        setField(productionPlanLinhaDAO, "jdbcTemplate", jdbcTemplate);

        String sql = invokeSqlResolver(productionPlanLinhaDAO);

        Assertions.assertTrue(sql.contains("ON CONFLICT"));
        Assertions.assertTrue(sql.contains("excluded.quantidade_ordem_planejada_producao_irrestrita"));
        Assertions.assertFalse(sql.contains("ON DUPLICATE KEY"));

    }

    private static String invokeSqlResolver(
            ProductionPlanLinhaDAO productionPlanLinhaDAO) throws Exception {

        var method = ProductionPlanLinhaDAO.class
                .getDeclaredMethod("getSqlUpsertProductionPlanLinha");
        method.setAccessible(true);
        return (String) method.invoke(productionPlanLinhaDAO);

    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }
}
