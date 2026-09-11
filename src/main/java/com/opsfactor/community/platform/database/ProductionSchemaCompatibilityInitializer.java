package com.opsfactor.community.platform.database;

import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Migrates the populated PostgreSQL production schema before Hibernate schema update.
 *
 * <p>Hibernate cannot backfill new NOT NULL discriminators, remove obsolete
 * nullability, or transfer historical capacity fields. This compatibility step
 * changes only those known contracts, in one PostgreSQL transaction. It never
 * deletes business rows or chooses a production meaning for ambiguous data.</p>
 */
public final class ProductionSchemaCompatibilityInitializer implements InitializingBean {

    /** The application's own datasource; no separate credentials or database are introduced. */
    private final DataSource dataSource;

    public ProductionSchemaCompatibilityInitializer(DataSource dataSource) {

        this.dataSource = dataSource;

    }

    /** A failure rolls back DDL and data together and prevents EntityManagerFactory initialization. */
    @Override
    public void afterPropertiesSet() {

        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                return;
            }
            connection.setAutoCommit(false);
            try {
                execute(connection, "SELECT pg_advisory_xact_lock(760343091)");
                String schema = connection.getSchema();
                migrateDiscriminator(connection, schema, "roteiro", "tipo_roteiro",
                        "roteiro_multiplo_material", "roteiro_multiplo_id");
                migrateDiscriminator(connection, schema, "lista_tecnica", "tipo_lista_tecnica",
                        "lista_tecnica_multiplo_output", "lista_tecnica_multiplo_id");
                migrateOperationCapacity(connection, schema);
                retainProductionVersionDiscriminator(connection, schema);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw new IllegalStateException(
                        "Production schema compatibility failed; all changes were rolled back. "
                                + "Preserve the database and resolve the reported ambiguity before retrying. "
                                + exception.getMessage(), exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize production schema compatibility", exception);
        }

    }

    /**
     * Classifies only rows lacking a discriminator. Existing simple/multiple labels are retained.
     * A missing label is simple only with one header output and no child outputs;
     * multiple requires a child collection of at least two, as enforced by both domain subtypes.
     */
    private static void migrateDiscriminator(Connection connection, String schema, String table,
            String discriminator, String childTable, String childOwner) throws SQLException {

        if (!tableExists(connection, schema, table)) {
            return;
        }
        ensureColumn(connection, schema, table, discriminator, "varchar(31)");
        boolean childrenExist = tableExists(connection, schema, childTable);
        String childCount = childrenExist ? "COALESCE(c.output_count, 0)" : "0";
        String childJoin = childrenExist
                ? " LEFT JOIN (SELECT " + quote(childOwner) + " owner_id, COUNT(*) output_count FROM "
                        + qualified(schema, childTable) + " GROUP BY " + quote(childOwner)
                        + ") c ON c.owner_id = p.id" : "";
        String query = "SELECT p.id, p." + quote(discriminator) + ", p.material_output_id, "
                + childCount + " FROM " + qualified(schema, table) + " p" + childJoin;
        List<DiscriminatorAssignment> assignments = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) {
                String id = rows.getString(1);
                String existing = rows.getString(2);
                if (existing != null) {
                    if (!existing.equals("simples") && !existing.equals("multiplo")) {
                        throw ambiguous(table, id, "unknown discriminator '" + existing + "'");
                    }
                    continue;
                }
                boolean headerOutput = rows.getString(3) != null;
                long outputs = rows.getLong(4);
                if (headerOutput && outputs == 0) {
                    assignments.add(new DiscriminatorAssignment(id, "simples"));
                } else if (!headerOutput && outputs >= 2) {
                    assignments.add(new DiscriminatorAssignment(id, "multiplo"));
                } else {
                    throw ambiguous(table, id, "missing discriminator with header output="
                            + headerOutput + " and child outputs=" + outputs);
                }
            }
        }
        // Batch writes avoid an update round-trip per legacy business row.
        try (PreparedStatement update = connection.prepareStatement("UPDATE " + qualified(schema, table)
                + " SET " + quote(discriminator) + " = ? WHERE id = ? AND " + quote(discriminator) + " IS NULL")) {
            for (DiscriminatorAssignment assignment : assignments) {
                update.setString(1, assignment.value());
                update.setString(2, assignment.id());
                update.addBatch();
            }
            if (!assignments.isEmpty()) {
                update.executeBatch();
            }
        }
        execute(connection, "ALTER TABLE " + qualified(schema, table) + " ALTER COLUMN "
                + quote(discriminator) + " SET NOT NULL");
        // A multiple package stores outputs in child rows, not in the singular header.
        execute(connection, "ALTER TABLE " + qualified(schema, table)
                + " ALTER COLUMN material_output_id DROP NOT NULL");

    }

    /** Transfers old operation-level bases and hours without overwriting already migrated values. */
    private static void migrateOperationCapacity(Connection connection, String schema) throws SQLException {

        if (!tableExists(connection, schema, "roteiro") || !tableExists(connection, schema, "operacao_roteiro")
                || !columnExists(connection, schema, "operacao_roteiro", "horas_por_quantidade_base")
                || !columnExists(connection, schema, "operacao_roteiro", "quantidade_base")
                || !columnExists(connection, schema, "operacao_roteiro", "unidade_medida_id")) {
            return;
        }
        ensureColumn(connection, schema, "roteiro", "quantidade_base", "double precision");
        ensureColumn(connection, schema, "roteiro", "unidade_medida_quantidade_base_id", "varchar(255)");
        ensureColumn(connection, schema, "operacao_roteiro", "tempo_por_quantidade_base", "double precision");
        ensureColumn(connection, schema, "operacao_roteiro", "unidade_tempo_operacao", "varchar(1)");
        String query = "SELECT r.id, r.quantidade_base, r.unidade_medida_quantidade_base_id, o.posicao, "
                + "o.quantidade_base, o.unidade_medida_id, o.horas_por_quantidade_base, "
                + "o.tempo_por_quantidade_base, o.unidade_tempo_operacao FROM " + qualified(schema, "roteiro")
                + " r JOIN " + qualified(schema, "operacao_roteiro") + " o ON o.roteiro_id = r.id "
                + "WHERE o.quantidade_base IS NOT NULL OR o.unidade_medida_id IS NOT NULL "
                + "OR o.horas_por_quantidade_base IS NOT NULL ORDER BY r.id, o.posicao";
        Map<String, List<LegacyOperation>> operationsByRouting = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) {
                LegacyOperation operation = new LegacyOperation(rows.getString(1), number(rows, 2), rows.getString(3),
                        rows.getInt(4), number(rows, 5), rows.getString(6), number(rows, 7),
                        number(rows, 8), rows.getString(9));
                operationsByRouting.computeIfAbsent(operation.routingId(), ignored -> new ArrayList<>()).add(operation);
            }
        }
        String routingSql = "UPDATE " + qualified(schema, "roteiro")
                + " SET quantidade_base = COALESCE(quantidade_base, ?), "
                + "unidade_medida_quantidade_base_id = COALESCE(unidade_medida_quantidade_base_id, ?) WHERE id = ?";
        String operationSql = "UPDATE " + qualified(schema, "operacao_roteiro")
                + " SET tempo_por_quantidade_base = COALESCE(tempo_por_quantidade_base, ?), "
                + "unidade_tempo_operacao = COALESCE(unidade_tempo_operacao, 'H') WHERE roteiro_id = ? AND posicao = ?";
        try (PreparedStatement routingUpdate = connection.prepareStatement(routingSql);
             PreparedStatement operationUpdate = connection.prepareStatement(operationSql)) {
            for (List<LegacyOperation> operations : operationsByRouting.values()) {
                if (operations.stream().allMatch(LegacyOperation::alreadyMigrated)) {
                    continue;
                }
                LegacyOperation first = operations.getFirst();
                validateCommonBase(operations);
                routingUpdate.setDouble(1, first.oldBase());
                routingUpdate.setString(2, first.oldUnit());
                routingUpdate.setString(3, first.routingId());
                routingUpdate.addBatch();
                for (LegacyOperation operation : operations) {
                    if (operation.duration() != null && operation.timeUnit() != null) {
                        continue;
                    }
                    if (operation.duration() != null && operation.timeUnit() == null) {
                        throw ambiguous("operacao_roteiro", operation.routingId() + "/" + operation.position(),
                                "populated duration without an explicit time unit cannot be inferred from legacy hours");
                    }
                    if (operation.duration() == null && (operation.oldHours() == null
                            || !Double.isFinite(operation.oldHours()) || operation.oldHours() < 0
                            || (operation.timeUnit() != null && !operation.timeUnit().equals("H")))) {
                        throw ambiguous("operacao_roteiro", operation.routingId() + "/" + operation.position(),
                                "legacy hours cannot be copied into the partially configured duration/unit");
                    }
                    operationUpdate.setDouble(1, operation.duration() != null ? operation.duration() : operation.oldHours());
                    operationUpdate.setString(2, operation.routingId());
                    operationUpdate.setInt(3, operation.position());
                    operationUpdate.addBatch();
                }
            }
            routingUpdate.executeBatch();
            operationUpdate.executeBatch();
        }

    }

    /** Rejects differing legacy bases/units rather than silently changing capacity consumption. */
    private static void validateCommonBase(List<LegacyOperation> operations) {

        LegacyOperation first = operations.getFirst();
        if (first.oldBase() == null || !Double.isFinite(first.oldBase()) || first.oldBase() <= 0
                || first.oldUnit() == null || first.oldUnit().isBlank()) {
            throw ambiguous("roteiro", first.routingId(), "legacy operation base quantity/unit is missing or invalid");
        }
        for (LegacyOperation operation : operations) {
            if (!Objects.equals(first.oldBase(), operation.oldBase()) || !Objects.equals(first.oldUnit(), operation.oldUnit())
                    || (operation.routingBase() != null && !Objects.equals(first.oldBase(), operation.routingBase()))
                    || (operation.routingUnit() != null && !Objects.equals(first.oldUnit(), operation.routingUnit()))) {
                throw ambiguous("roteiro", first.routingId(), "legacy operation bases/units disagree with each other or the populated routing fields");
            }
        }

    }

    /** Keeps obsolete labels (including the reserved inexistente row) while allowing concrete-entity inserts. */
    private static void retainProductionVersionDiscriminator(Connection connection, String schema) throws SQLException {

        if (!columnExists(connection, schema, "versao_producao", "tipo_versao_producao")) {
            return;
        }
        try (PreparedStatement query = connection.prepareStatement("SELECT column_default FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = 'versao_producao' AND column_name = 'tipo_versao_producao'")) {
            query.setString(1, schema);
            try (ResultSet rows = query.executeQuery()) {
                if (rows.next() && rows.getString(1) == null) {
                    execute(connection, "ALTER TABLE " + qualified(schema, "versao_producao")
                            + " ALTER COLUMN tipo_versao_producao SET DEFAULT 'simples'");
                }
            }
        }

    }

    /** Adds only a known new column; Hibernate remains responsible for fresh schema creation. */
    private static void ensureColumn(Connection connection, String schema, String table, String column, String type) throws SQLException {

        if (!columnExists(connection, schema, table, column)) {
            execute(connection, "ALTER TABLE " + qualified(schema, table) + " ADD COLUMN " + quote(column) + " " + type);
        }

    }

    private static boolean tableExists(Connection connection, String schema, String table) throws SQLException {

        try (ResultSet tables = connection.getMetaData().getTables(null, schema, table, new String[]{"TABLE"})) {
            return tables.next();
        }

    }

    private static boolean columnExists(Connection connection, String schema, String table, String column) throws SQLException {

        try (ResultSet columns = connection.getMetaData().getColumns(null, schema, table, column)) {
            return columns.next();
        }

    }

    private static Double number(ResultSet rows, int index) throws SQLException {

        Number value = (Number) rows.getObject(index);
        return value == null ? null : value.doubleValue();

    }

    private static void execute(Connection connection, String sql) throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }

    }

    private static String qualified(String schema, String table) {

        return quote(schema) + "." + quote(table);

    }

    private static String quote(String identifier) {

        return "\"" + identifier.replace("\"", "\"\"") + "\"";

    }

    private static IllegalStateException ambiguous(String table, String id, String reason) {

        return new IllegalStateException("Ambiguous production upgrade in " + table + " [" + id + "]: " + reason);

    }

    private record DiscriminatorAssignment(String id, String value) { }

    /** One flat JDBC row, loaded in a single query rather than traversing JPA relationships. */
    private record LegacyOperation(String routingId, Double routingBase, String routingUnit, int position,
            Double oldBase, String oldUnit, Double oldHours, Double duration, String timeUnit) {

        boolean alreadyMigrated() {

            return routingBase != null && routingUnit != null && duration != null && timeUnit != null;

        }

    }
}
