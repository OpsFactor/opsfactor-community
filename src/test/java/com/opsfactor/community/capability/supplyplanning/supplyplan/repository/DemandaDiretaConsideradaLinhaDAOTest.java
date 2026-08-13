package com.opsfactor.community.capability.supplyplanning.supplyplan.repository;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
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
 * Testes de contrato do DAO JDBC de demanda direta considerada.
 *
 * <p>O DAO usa batch SQL direto para evitar `merge` entidade a entidade em alto
 * volume. Como ele nao passa pelo repository/JPA, precisa validar a colecao e a
 * chave composta antes de tocar `JdbcTemplate` ou preencher o prepared
 * statement.</p>
 */
public class DemandaDiretaConsideradaLinhaDAOTest {

    @Test
    public void saveInBatchShouldAcceptEmptyCollectionAsNoOp() {

        DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new DemandaDiretaConsideradaLinhaDAO();

        /*
         * Lista vazia representa snapshot sem linhas a persistir. O metodo deve
         * retornar antes de consultar metadata do banco ou chamar batchUpdate.
         */
        Assertions.assertDoesNotThrow(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(List.of()));

    }

    @Test
    public void saveInBatchShouldRejectBrokenCollectionsBeforeJdbcAccess() {

        DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new DemandaDiretaConsideradaLinhaDAO();
        List<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhasComItemNulo =
                new ArrayList<>();
        demandaDiretaConsideradaLinhasComItemNulo.add(null);

        IllegalArgumentException colecaoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(null));
        IllegalArgumentException itemAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(
                        demandaDiretaConsideradaLinhasComItemNulo));
        IllegalArgumentException chaveIncompletaException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(
                        List.of(new DemandaDiretaConsideradaLinha())));

        Assertions.assertEquals(
                "Direct demand considered JDBC batch collection is required.",
                colecaoAusenteException.getMessage());
        Assertions.assertEquals(
                "Direct demand considered JDBC batch line at index 0 is required.",
                itemAusenteException.getMessage());
        Assertions.assertEquals(
                "Direct demand considered JDBC batch line at index 0 must have a composite key.",
                chaveIncompletaException.getMessage());

    }

    @Test
    public void deleteBySupplyPlanIdShouldRejectNullIdBeforeJdbcAccess() {

        DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new DemandaDiretaConsideradaLinhaDAO();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaLinhaDAO.deleteBySupplyPlanId(null));

        Assertions.assertEquals(
                "Supply Plan id is required to delete direct demand considered JDBC batch snapshot.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void sqliteShouldUseNativeConflictUpsertInsteadOfMySqlSyntax() throws Exception {

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("SQLite");
        when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class)))
                .thenAnswer(invocation -> ((org.springframework.jdbc.core.ConnectionCallback<?>) invocation
                        .getArgument(0)).doInConnection(connection));

        DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new DemandaDiretaConsideradaLinhaDAO();
        setField(demandaDiretaConsideradaLinhaDAO, "jdbcTemplate", jdbcTemplate);

        String sql = invokeSqlResolver(demandaDiretaConsideradaLinhaDAO);

        Assertions.assertTrue(sql.contains("ON CONFLICT"));
        Assertions.assertTrue(sql.contains("excluded.quantidade_plano_demanda_original"));
        Assertions.assertFalse(sql.contains("ON DUPLICATE KEY"));

    }

    @Test
    public void postgreSqlShouldUseNativeConflictUpsertInsteadOfMySqlSyntax() throws Exception {

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class)))
                .thenAnswer(invocation -> ((org.springframework.jdbc.core.ConnectionCallback<?>) invocation
                        .getArgument(0)).doInConnection(connection));

        DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new DemandaDiretaConsideradaLinhaDAO();
        setField(demandaDiretaConsideradaLinhaDAO, "jdbcTemplate", jdbcTemplate);

        String sql = invokeSqlResolver(demandaDiretaConsideradaLinhaDAO);

        Assertions.assertTrue(sql.contains("ON CONFLICT"));
        Assertions.assertTrue(sql.contains("excluded.quantidade_plano_demanda_original"));
        Assertions.assertFalse(sql.contains("ON DUPLICATE KEY"));

    }

    private static String invokeSqlResolver(
            DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO) throws Exception {

        var method = DemandaDiretaConsideradaLinhaDAO.class
                .getDeclaredMethod("getSqlUpsertDemandaDiretaConsideradaLinha");
        method.setAccessible(true);
        return (String) method.invoke(demandaDiretaConsideradaLinhaDAO);

    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
