package com.opsfactor.community.capability.supplyplanning.productionplan.repository;

import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.Locale;

/**
 * Persiste o snapshot de producao do Supply Plan em batch JDBC.
 *
 * <p>A entidade possui uma chave composta formada por seis dimensoes JPA. O
 * {@code saveAll} trata essas linhas como entidades destacadas e executa
 * {@code merge} individual, carregando todo o grafo associado antes de cada
 * escrita. Alem de multiplicar round-trips, esse select ultrapassa o limite de
 * 64 tabelas por join do SQLite. O batch abaixo grava somente as chaves e
 * medidas materializadas, sem carregar relacionamentos.</p>
 */
@Repository
public class ProductionPlanLinhaDAO {

    /**
     * Tamanho do lote compartilhado com a configuracao JDBC do Hibernate.
     */
    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:1000}")
    private Integer batchSize;

    /**
     * Executor JDBC usado para upsert e delete em lote.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Insere ou atualiza as linhas de producao sem materializar o grafo JPA.
     */
    public void saveInBatch(Collection<ProductionPlanLinha> productionPlanLinhas) {

        validaProductionPlanLinhasParaBatch(productionPlanLinhas);
        if (productionPlanLinhas.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                getSqlUpsertProductionPlanLinha(),
                productionPlanLinhas,
                batchSize,
                new ParameterizedPreparedStatementSetter<>() {
                    @Override
                    public void setValues(
                            PreparedStatement preparedStatement,
                            ProductionPlanLinha productionPlanLinha) throws SQLException {
                        setValoresUpsert(preparedStatement, productionPlanLinha);
                    }
                });

    }

    /**
     * Remove em lote as linhas zeradas calculadas no checkpoint atual.
     */
    public void deleteInBatch(Collection<ProductionPlanLinha> productionPlanLinhas) {

        validaProductionPlanLinhasParaBatch(productionPlanLinhas);
        if (productionPlanLinhas.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                """
                DELETE FROM production_plan_linha
                WHERE data_referencia = ?
                  AND lista_tecnica_id = ?
                  AND location_id = ?
                  AND roteiro_id = ?
                  AND supply_plan_id = ?
                  AND versao_producao_id = ?
                """,
                productionPlanLinhas,
                batchSize,
                new ParameterizedPreparedStatementSetter<>() {
                    @Override
                    public void setValues(
                            PreparedStatement preparedStatement,
                            ProductionPlanLinha productionPlanLinha) throws SQLException {
                        setValoresChave(preparedStatement, productionPlanLinha);
                    }
                });

    }

    /**
     * Valida a chave e as referencias gravadas antes de abrir o batch JDBC.
     */
    private void validaProductionPlanLinhasParaBatch(
            Collection<ProductionPlanLinha> productionPlanLinhas) {

        if (productionPlanLinhas == null) {
            throw new IllegalArgumentException(
                    "Production Plan JDBC batch collection is required.");
        }

        int indiceProductionPlanLinha = 0;
        for (ProductionPlanLinha productionPlanLinha : productionPlanLinhas) {
            if (productionPlanLinha == null) {
                throw new IllegalArgumentException(
                        "Production Plan JDBC batch line at index "
                                + indiceProductionPlanLinha
                                + " is required.");
            }
            if (productionPlanLinha.getProductionPlanLinhaCompositeKey() == null) {
                throw new IllegalArgumentException(
                        "Production Plan JDBC batch line at index "
                                + indiceProductionPlanLinha
                                + " must have a composite key.");
            }
            if (productionPlanLinha.getSupplyPlan() == null
                    || productionPlanLinha.getSupplyPlan().getId() == null
                    || productionPlanLinha.getLocation() == null
                    || productionPlanLinha.getLocation().getId() == null
                    || productionPlanLinha.getVersaoProducao() == null
                    || productionPlanLinha.getVersaoProducao().getId() == null
                    || productionPlanLinha.getRoteiro() == null
                    || productionPlanLinha.getRoteiro().getId() == null
                    || productionPlanLinha.getListaTecnica() == null
                    || productionPlanLinha.getListaTecnica().getId() == null
                    || productionPlanLinha.getDataReferencia() == null
                    || productionPlanLinha.getMaterialOutput() == null
                    || productionPlanLinha.getMaterialOutput().getId() == null) {
                throw new IllegalArgumentException(
                        "Production Plan JDBC batch line at index "
                                + indiceProductionPlanLinha
                                + " has an incomplete key or output material.");
            }
            if (productionPlanLinha.getUnidadeMedidaCadastrado() != null
                    && productionPlanLinha.getUnidadeMedidaCadastrado().getId() == null) {
                throw new IllegalArgumentException(
                        "Production Plan JDBC batch line at index "
                                + indiceProductionPlanLinha
                                + " must have a unit of measure id when explicitly provided.");
            }
            indiceProductionPlanLinha++;
        }

    }

    /**
     * Seleciona a sintaxe de upsert nativa do banco em execucao.
     */
    private String getSqlUpsertProductionPlanLinha() {

        String databaseProductName = jdbcTemplate.execute(
                (ConnectionCallback<String>) connection ->
                        connection.getMetaData().getDatabaseProductName());
        String normalizedDatabaseProductName = databaseProductName == null
                ? ""
                : databaseProductName.toLowerCase(Locale.ROOT);

        if (normalizedDatabaseProductName.contains("h2")) {
            return getSqlMergeProductionPlanLinhaH2();
        }
        if (normalizedDatabaseProductName.contains("sqlite")) {
            return getSqlUpsertProductionPlanLinhaSQLite();
        }
        if (normalizedDatabaseProductName.contains("postgresql")) {
            return getSqlUpsertProductionPlanLinhaPostgreSql();
        }
        throw new IllegalStateException(
                "Community supports PostgreSQL at runtime; unsupported JDBC database: " + databaseProductName);

    }

    private String getSqlMergeProductionPlanLinhaH2() {

        return """
                MERGE INTO production_plan_linha (
                    data_referencia,
                    quantidade_ordem_firme_producao_irrestrita,
                    quantidade_ordem_firme_producao_restrita,
                    quantidade_ordem_firme_producao_trabalho,
                    quantidade_ordem_planejada_producao_irrestrita,
                    quantidade_ordem_planejada_producao_restrita,
                    quantidade_ordem_planejada_producao_trabalho,
                    quantidade_ordem_producao_baseline,
                    quantidade_ordem_producao_baseline_atendida,
                    quantidade_sugestao_producao_baseline,
                    quantidade_sugestao_producao_baseline_atendida,
                    roteiro_id,
                    location_id,
                    lista_tecnica_id,
                    supply_plan_id,
                    versao_producao_id,
                    material_output_id,
                    unidade_medida_id
                ) KEY (
                    data_referencia,
                    lista_tecnica_id,
                    location_id,
                    roteiro_id,
                    supply_plan_id,
                    versao_producao_id
                ) VALUES (?,?,?,?,?,?,?,NULL,NULL,NULL,NULL,?,?,?,?,?,?,?)
                """;

    }

    private String getSqlUpsertProductionPlanLinhaSQLite() {

        return getSqlInsertProductionPlanLinha() + """
                ON CONFLICT (
                    data_referencia,
                    lista_tecnica_id,
                    location_id,
                    roteiro_id,
                    supply_plan_id,
                    versao_producao_id
                ) DO UPDATE SET
                    quantidade_ordem_firme_producao_irrestrita = excluded.quantidade_ordem_firme_producao_irrestrita,
                    quantidade_ordem_firme_producao_restrita = excluded.quantidade_ordem_firme_producao_restrita,
                    quantidade_ordem_firme_producao_trabalho = excluded.quantidade_ordem_firme_producao_trabalho,
                    quantidade_ordem_planejada_producao_irrestrita = excluded.quantidade_ordem_planejada_producao_irrestrita,
                    quantidade_ordem_planejada_producao_restrita = excluded.quantidade_ordem_planejada_producao_restrita,
                    quantidade_ordem_planejada_producao_trabalho = excluded.quantidade_ordem_planejada_producao_trabalho,
                    quantidade_ordem_producao_baseline = NULL,
                    quantidade_ordem_producao_baseline_atendida = NULL,
                    quantidade_sugestao_producao_baseline = NULL,
                    quantidade_sugestao_producao_baseline_atendida = NULL,
                    material_output_id = excluded.material_output_id,
                    unidade_medida_id = excluded.unidade_medida_id
                """;

    }

    /**
     * PostgreSQL compartilha a sintaxe {@code ON CONFLICT} com SQLite.
     */
    private String getSqlUpsertProductionPlanLinhaPostgreSql() {

        return getSqlUpsertProductionPlanLinhaSQLite();

    }

    private String getSqlInsertProductionPlanLinha() {

        return """
                INSERT INTO production_plan_linha (
                    data_referencia,
                    quantidade_ordem_firme_producao_irrestrita,
                    quantidade_ordem_firme_producao_restrita,
                    quantidade_ordem_firme_producao_trabalho,
                    quantidade_ordem_planejada_producao_irrestrita,
                    quantidade_ordem_planejada_producao_restrita,
                    quantidade_ordem_planejada_producao_trabalho,
                    quantidade_ordem_producao_baseline,
                    quantidade_ordem_producao_baseline_atendida,
                    quantidade_sugestao_producao_baseline,
                    quantidade_sugestao_producao_baseline_atendida,
                    roteiro_id,
                    location_id,
                    lista_tecnica_id,
                    supply_plan_id,
                    versao_producao_id,
                    material_output_id,
                    unidade_medida_id
                ) VALUES (?,?,?,?,?,?,?,NULL,NULL,NULL,NULL,?,?,?,?,?,?,?)
                """;

    }

    /**
     * Preenche os valores de medida e, em seguida, as dimensoes da chave.
     */
    private void setValoresUpsert(
            PreparedStatement preparedStatement,
            ProductionPlanLinha productionPlanLinha) throws SQLException {

        preparedStatement.setTimestamp(1, Timestamp.valueOf(productionPlanLinha.getDataReferencia()));
        preparedStatement.setObject(2, productionPlanLinha.getQuantidadeOrdemFirmeProducaoIrrestrita(), Types.DOUBLE);
        preparedStatement.setObject(3, productionPlanLinha.getQuantidadeOrdemFirmeProducaoRestrita(), Types.DOUBLE);
        preparedStatement.setObject(4, productionPlanLinha.getQuantidadeOrdemFirmeProducaoTrabalho(), Types.DOUBLE);
        preparedStatement.setObject(5, productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoIrrestrita(), Types.DOUBLE);
        preparedStatement.setObject(6, productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoRestrita(), Types.DOUBLE);
        preparedStatement.setObject(7, productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoTrabalho(), Types.DOUBLE);
        preparedStatement.setObject(8, productionPlanLinha.getRoteiro().getId(), Types.VARCHAR);
        preparedStatement.setObject(9, productionPlanLinha.getLocation().getId(), Types.VARCHAR);
        preparedStatement.setObject(10, productionPlanLinha.getListaTecnica().getId(), Types.VARCHAR);
        preparedStatement.setObject(11, productionPlanLinha.getSupplyPlan().getId(), Types.BIGINT);
        preparedStatement.setObject(12, productionPlanLinha.getVersaoProducao().getId(), Types.VARCHAR);
        preparedStatement.setObject(13, productionPlanLinha.getMaterialOutput().getId(), Types.VARCHAR);
        preparedStatement.setObject(
                14,
                productionPlanLinha.getUnidadeMedidaCadastrado() == null
                        ? null
                        : productionPlanLinha.getUnidadeMedidaCadastrado().getId(),
                Types.VARCHAR);

    }

    /**
     * Preenche a chave composta na mesma ordem do delete SQL.
     */
    private void setValoresChave(
            PreparedStatement preparedStatement,
            ProductionPlanLinha productionPlanLinha) throws SQLException {

        preparedStatement.setTimestamp(1, Timestamp.valueOf(productionPlanLinha.getDataReferencia()));
        preparedStatement.setObject(2, productionPlanLinha.getListaTecnica().getId(), Types.VARCHAR);
        preparedStatement.setObject(3, productionPlanLinha.getLocation().getId(), Types.VARCHAR);
        preparedStatement.setObject(4, productionPlanLinha.getRoteiro().getId(), Types.VARCHAR);
        preparedStatement.setObject(5, productionPlanLinha.getSupplyPlan().getId(), Types.BIGINT);
        preparedStatement.setObject(6, productionPlanLinha.getVersaoProducao().getId(), Types.VARCHAR);

    }
}
