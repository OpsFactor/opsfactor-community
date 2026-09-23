package com.opsfactor.community.capability.supplyplanning.productionplan.repository;

import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regressão JDBC da identidade por output: coprodutos devem coexistir e a
 * remoção de uma linha zerada não pode apagar outro output do checkpoint.
 * O H2 executa os bindings reais; o contrato PostgreSQL é verificado separado.
 */
class ProductionPlanLinhaDAOMultiOutputTest {

    private static final Pattern CONFLICT_KEY = Pattern.compile("ON CONFLICT\\s*\\(([^)]+)\\)");

    private EmbeddedDatabase database;
    private JdbcTemplate jdbcTemplate;
    private ProductionPlanLinhaDAO productionPlanLinhaDAO;

    /** Cria um banco efêmero com a chave física de sete dimensões do domínio. */
    @BeforeEach
    void createDatabase() {

        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).build();
        jdbcTemplate = new JdbcTemplate(database);
        jdbcTemplate.execute("""
                CREATE TABLE production_plan_linha (
                    data_referencia TIMESTAMP NOT NULL,
                    quantidade_ordem_firme_producao_irrestrita DOUBLE,
                    quantidade_ordem_firme_producao_restrita DOUBLE,
                    quantidade_ordem_firme_producao_trabalho DOUBLE,
                    quantidade_ordem_planejada_producao_irrestrita DOUBLE,
                    quantidade_ordem_planejada_producao_restrita DOUBLE,
                    quantidade_ordem_planejada_producao_trabalho DOUBLE,
                    quantidade_ordem_producao_baseline DOUBLE,
                    quantidade_ordem_producao_baseline_atendida DOUBLE,
                    quantidade_sugestao_producao_baseline DOUBLE,
                    quantidade_sugestao_producao_baseline_atendida DOUBLE,
                    roteiro_id VARCHAR NOT NULL,
                    location_id VARCHAR NOT NULL,
                    lista_tecnica_id VARCHAR NOT NULL,
                    supply_plan_id BIGINT NOT NULL,
                    versao_producao_id VARCHAR NOT NULL,
                    material_output_id VARCHAR NOT NULL,
                    unidade_medida_id VARCHAR,
                    PRIMARY KEY (data_referencia, lista_tecnica_id, location_id,
                                 roteiro_id, supply_plan_id, versao_producao_id, material_output_id)
                )
                """);
        productionPlanLinhaDAO = new ProductionPlanLinhaDAO() {

            /**
             * H2 não suporta ON CONFLICT DO UPDATE. Adapta só a sintaxe,
             * extraindo a chave do SQL real para não mascarar regressão nela.
             */
            @Override
            protected String getSqlUpsertProductionPlanLinha() {

                String postgresSql = super.getSqlUpsertProductionPlanLinha();
                String insertSql = getSqlInsertProductionPlanLinha();
                return insertSql.replace("INSERT INTO", "MERGE INTO")
                        .replace(") VALUES", ") KEY (" + conflictKey(postgresSql) + ") VALUES");

            }

        };
        ReflectionTestUtils.setField(productionPlanLinhaDAO, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(productionPlanLinhaDAO, "batchSize", 2);

    }

    /** Encerra somente o banco em memória pertencente a este teste. */
    @AfterEach
    void closeDatabase() {

        if (database != null) {
            database.shutdown();
        }

    }

    @Test
    void postgresConflictMustMatchPhysicalPrimaryKeyWithoutUpdatingOutputIdentity() {

        String postgresSql = new ProductionPlanLinhaDAO().getSqlUpsertProductionPlanLinha();
        Set<String> conflictColumns = new HashSet<>(Arrays.asList(conflictKey(postgresSql)
                .replaceAll("\\s", "").toUpperCase().split(",")));
        Set<String> primaryKeyColumns = jdbcTemplate.execute((Connection connection) -> {
            Set<String> columns = new HashSet<>();
            try (var keys = connection.getMetaData().getPrimaryKeys(null, null, "PRODUCTION_PLAN_LINHA")) {
                while (keys.next()) {
                    columns.add(keys.getString("COLUMN_NAME"));
                }
            }
            return columns;
        });

        assertEquals(7, primaryKeyColumns.size());
        assertEquals(primaryKeyColumns, conflictColumns);
        assertFalse(postgresSql.substring(postgresSql.indexOf("DO UPDATE SET"))
                .contains("material_output_id ="));

    }

    @Test
    void batchUpsertMustKeepTwoOutputsAndUpdateOnlyTheSelectedOutput() {

        productionPlanLinhaDAO.saveInBatch(List.of(line("OUTPUT_A", 12.0), line("OUTPUT_B", 5.0)));
        productionPlanLinhaDAO.saveInBatch(List.of(line("OUTPUT_A", 18.0)));

        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM production_plan_linha", Integer.class));
        assertEquals(18.0, plannedQuantity("OUTPUT_A"));
        assertEquals(5.0, plannedQuantity("OUTPUT_B"));

    }

    @Test
    void batchDeleteMustPreserveOtherOutputAtTheSameCheckpoint() {

        productionPlanLinhaDAO.saveInBatch(List.of(line("OUTPUT_A", 12.0), line("OUTPUT_B", 5.0)));
        productionPlanLinhaDAO.deleteInBatch(List.of(line("OUTPUT_A", 0.0)));

        assertEquals(List.of("OUTPUT_B"), jdbcTemplate.queryForList(
                "SELECT material_output_id FROM production_plan_linha", String.class));
        assertEquals(5.0, plannedQuantity("OUTPUT_B"));

    }

    /** Obtém a chave do SQL de produção sem manter outra lista de dimensões. */
    private static String conflictKey(String sql) {

        Matcher matcher = CONFLICT_KEY.matcher(sql);
        assertTrue(matcher.find(), "PostgreSQL upsert must declare its conflict key");
        return matcher.group(1);

    }

    /** Consulta a medida persistida, falhando se a linha não existir. */
    private double plannedQuantity(String materialOutputId) {

        return jdbcTemplate.queryForObject("""
                SELECT quantidade_ordem_planejada_producao_irrestrita
                FROM production_plan_linha WHERE material_output_id = ?
                """, Double.class, materialOutputId);

    }

    /** Referências de domínio sem JPA; toda gravação e exclusão usa JDBC real. */
    private ProductionPlanLinha line(String materialOutputId, double quantity) {

        ProductionPlanLinha line = mock(ProductionPlanLinha.class, RETURNS_DEEP_STUBS);
        when(line.getDataReferencia()).thenReturn(LocalDateTime.of(2027, 1, 31, 23, 59, 59));
        when(line.getSupplyPlan().getId()).thenReturn(1L);
        when(line.getLocation().getId()).thenReturn("PLANT");
        when(line.getRoteiro().getId()).thenReturn("ROUTING");
        when(line.getListaTecnica().getId()).thenReturn("BOM");
        when(line.getVersaoProducao().getId()).thenReturn("VERSION");
        when(line.getMaterialOutput().getId()).thenReturn(materialOutputId);
        when(line.getUnidadeMedidaCadastrado().getId()).thenReturn("TON");
        when(line.getQuantidadeOrdemPlanejadaProducaoIrrestrita()).thenReturn(quantity);
        return line;

    }

}
