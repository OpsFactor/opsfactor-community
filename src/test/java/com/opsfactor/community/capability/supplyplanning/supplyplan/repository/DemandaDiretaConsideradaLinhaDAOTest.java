package com.opsfactor.community.capability.supplyplanning.supplyplan.repository;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

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
    public void communityShouldUsePostgreSqlConflictUpsertInsteadOfMySqlSyntax() {

        DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new DemandaDiretaConsideradaLinhaDAO();
        String sql = demandaDiretaConsideradaLinhaDAO.getSqlUpsertDemandaDiretaConsideradaLinha();

        Assertions.assertTrue(sql.contains("ON CONFLICT"));
        Assertions.assertTrue(sql.contains("excluded.quantidade_plano_demanda_original"));
        Assertions.assertFalse(sql.contains("ON DUPLICATE KEY"));

    }

}
