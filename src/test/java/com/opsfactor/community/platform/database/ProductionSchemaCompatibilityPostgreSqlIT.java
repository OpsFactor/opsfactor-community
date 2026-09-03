package com.opsfactor.community.platform.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Explicit release gate against a disposable real PostgreSQL, never the installed database.
 * Run with -Dtest=ProductionSchemaCompatibilityPostgreSqlIT and
 * -Dopsfactor.test.postgresql.bin=/path/to/postgresql/bin. The test owns and stops its cluster.
 */
class ProductionSchemaCompatibilityPostgreSqlIT {

    @TempDir
    static Path temporaryDirectory;

    private static Path binaries;
    private static Path dataDirectory;
    private static String serverUrl;
    private static boolean databaseStarted;
    private DriverManagerDataSource dataSource;
    private String schema;

    @BeforeAll
    static void startIsolatedPostgreSql() throws Exception {

        String configuredBinaries = System.getProperty("opsfactor.test.postgresql.bin");
        if (configuredBinaries == null || configuredBinaries.isBlank()) {
            throw new IllegalStateException("Explicit PostgreSQL integration gate requires -Dopsfactor.test.postgresql.bin");
        }
        binaries = Path.of(configuredBinaries);
        dataDirectory = temporaryDirectory.resolve("data");
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        runTool("initdb", "-D", dataDirectory.toString(), "-U", "postgres", "--auth=trust", "--encoding=UTF8", "--no-locale");
        try {
            runTool("pg_ctl", "-D", dataDirectory.toString(), "-l", temporaryDirectory.resolve("postgres.log").toString(),
                    "-o", "-h 127.0.0.1 -p " + port, "-w", "start");
            databaseStarted = true;
            serverUrl = "jdbc:postgresql://127.0.0.1:" + port + "/postgres";
            System.out.println("Production compatibility IT owns isolated PostgreSQL port " + port);
        } catch (Exception exception) {
            stopIsolatedPostgreSql();
            throw exception;
        }

    }

    @AfterAll
    static void stopIsolatedPostgreSql() throws Exception {

        if (binaries != null && dataDirectory != null && Files.exists(dataDirectory.resolve("postmaster.pid"))) {
            runTool("pg_ctl", "-D", dataDirectory.toString(), "-m", "fast", "-w", "stop");
        }
        databaseStarted = false;
        if (dataDirectory != null) {
            assertThat(dataDirectory.resolve("postmaster.pid")).doesNotExist();
        }

    }

    @BeforeEach
    void createIsolatedSchema() throws Exception {

        assertThat(databaseStarted).isTrue();
        schema = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = new DriverManagerDataSource(serverUrl, "postgres", "").getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        dataSource = new DriverManagerDataSource(serverUrl + "?currentSchema=" + schema, "postgres", "");

    }

    @AfterEach
    void removeOnlyTestSchema() throws Exception {

        if (schema != null) {
            try (Connection connection = new DriverManagerDataSource(serverUrl, "postgres", "").getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }

    }

    @Test
    void freshDatabaseRemainsEmptyForHibernate() throws Exception {

        migrate();
        migrate();
        assertThat(scalar("SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema()"))
                .isEqualTo("0");

    }

    @Test
    void emptyHistoricalTablesAcceptDiscriminators() throws Exception {

        legacySchema();
        migrate();
        assertThat(columnExists("roteiro", "tipo_roteiro")).isTrue();
        assertThat(columnExists("lista_tecnica", "tipo_lista_tecnica")).isTrue();
        assertThat(scalar("SELECT count(*) FROM roteiro")).isEqualTo("0");

    }

    @Test
    void populatedLegacySchemaPreservesCapacityDemandIdsAndRelationshipsAndIsIdempotent() throws Exception {

        populatedLegacySchema();
        migrate();
        assertThat(scalar("SELECT tipo_roteiro FROM roteiro WHERE id='R1'")).isEqualTo("simples");
        assertThat(scalar("SELECT tipo_lista_tecnica FROM lista_tecnica WHERE id='B1'")).isEqualTo("simples");
        assertThat(scalar("SELECT quantidade_base || ':' || unidade_medida_quantidade_base_id FROM roteiro"))
                .isEqualTo("1:TON");
        assertThat(rows("SELECT posicao || ':' || tempo_por_quantidade_base || ':' || unidade_tempo_operacao FROM operacao_roteiro ORDER BY posicao"))
                .containsExactly("10:1.25:H", "20:0.800000011920929:H");
        List<String> beforeSecondRun = snapshot();
        migrate();
        assertThat(snapshot()).containsExactlyElementsOf(beforeSecondRun);
        assertThat(scalar("SELECT count(*) FROM versao_producao WHERE roteiro_id='R1' AND lista_tecnica_id='B1'"))
                .isEqualTo("1");
        assertThat(scalar("SELECT value FROM demand_preservation WHERE id='DP_MONTHLY_TUTORIAL'"))
                .isEqualTo("123.45");
        assertThat(scalar("SELECT tipo_versao_producao FROM versao_producao WHERE id='DEFAULT_PRODUCTION_VERSION'"))
                .isEqualTo("inexistente");
        sql("INSERT INTO versao_producao (id,roteiro_id,lista_tecnica_id) VALUES ('NEW_VERSION','R1','B1')");
        assertThat(scalar("SELECT tipo_versao_producao FROM versao_producao WHERE id='NEW_VERSION'"))
                .isEqualTo("simples");

    }

    @Test
    void mixedSchemaClassifiesOnlyUnambiguousMissingLabelsAndPreservesExistingMultiple() throws Exception {

        legacySchema();
        sql("ALTER TABLE roteiro ALTER COLUMN material_output_id DROP NOT NULL",
                "ALTER TABLE lista_tecnica ALTER COLUMN material_output_id DROP NOT NULL",
                "ALTER TABLE roteiro ADD COLUMN tipo_roteiro varchar(31)",
                "ALTER TABLE lista_tecnica ADD COLUMN tipo_lista_tecnica varchar(31)",
                "CREATE TABLE roteiro_multiplo_material (roteiro_multiplo_id varchar(50), material_id varchar(50))",
                "CREATE TABLE lista_tecnica_multiplo_output (lista_tecnica_multiplo_id varchar(50), material_output_id varchar(50))",
                "INSERT INTO roteiro(id,material_output_id,tipo_roteiro) VALUES ('S','M',NULL),('M',NULL,NULL),('KNOWN',NULL,'multiplo')",
                "INSERT INTO roteiro_multiplo_material VALUES ('M','M1'),('M','M2'),('KNOWN','M1')",
                "INSERT INTO lista_tecnica(id,material_output_id,tipo_lista_tecnica) VALUES ('S','M',NULL),('M',NULL,NULL)",
                "INSERT INTO lista_tecnica_multiplo_output VALUES ('M','M1'),('M','M2')");
        migrate();
        assertThat(rows("SELECT id || ':' || tipo_roteiro FROM roteiro ORDER BY id"))
                .containsExactly("KNOWN:multiplo", "M:multiplo", "S:simples");
        assertThat(rows("SELECT id || ':' || tipo_lista_tecnica FROM lista_tecnica ORDER BY id"))
                .containsExactly("M:multiplo", "S:simples");
        migrate();
        assertThat(scalar("SELECT count(*) FROM roteiro_multiplo_material")).isEqualTo("3");

    }

    @Test
    void missingDiscriminatorWithSingleChildRollsBackAllPreviousDdlAndRows() throws Exception {

        populatedLegacySchema();
        sql("ALTER TABLE lista_tecnica ALTER COLUMN material_output_id DROP NOT NULL",
                "UPDATE lista_tecnica SET material_output_id=NULL",
                "CREATE TABLE lista_tecnica_multiplo_output (lista_tecnica_multiplo_id varchar(50), material_output_id varchar(50))",
                "INSERT INTO lista_tecnica_multiplo_output VALUES ('B1','M1')");
        assertThatThrownBy(this::migrate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rolled back").hasMessageContaining("B1").hasMessageContaining("child outputs=1");
        assertThat(columnExists("roteiro", "tipo_roteiro")).isFalse();
        assertThat(columnExists("lista_tecnica", "tipo_lista_tecnica")).isFalse();
        assertThat(scalar("SELECT is_nullable FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='roteiro' AND column_name='material_output_id'"))
                .isEqualTo("NO");
        assertThat(scalar("SELECT count(*) FROM versao_producao")).isEqualTo("2");

    }

    @Test
    void outputInBothHeaderAndChildrenIsAmbiguous() throws Exception {

        populatedLegacySchema();
        sql("CREATE TABLE roteiro_multiplo_material (roteiro_multiplo_id varchar(50), material_id varchar(50))",
                "INSERT INTO roteiro_multiplo_material VALUES ('R1','M1'),('R1','M2')");
        assertThatThrownBy(this::migrate).hasMessageContaining("Ambiguous").hasMessageContaining("R1");
        assertThat(columnExists("roteiro", "tipo_roteiro")).isFalse();

    }

    @Test
    void divergentLegacyBasesAbortWithoutChangingCapacityOrDiscriminators() throws Exception {

        populatedLegacySchema();
        sql("UPDATE operacao_roteiro SET quantidade_base=2 WHERE posicao=20");
        assertThatThrownBy(this::migrate).hasMessageContaining("bases/units disagree").hasMessageContaining("R1");
        assertThat(columnExists("roteiro", "tipo_roteiro")).isFalse();
        assertThat(columnExists("operacao_roteiro", "tempo_por_quantidade_base")).isFalse();
        assertThat(rows("SELECT quantidade_base::text FROM operacao_roteiro ORDER BY posicao")).containsExactly("1", "2");

    }

    @Test
    void alreadyPopulatedNewCapacityFieldsAreNeverOverwrittenByStaleLegacyColumns() throws Exception {

        populatedLegacySchema();
        migrate();
        sql("UPDATE roteiro SET quantidade_base=99,unidade_medida_quantidade_base_id='KG'",
                "UPDATE operacao_roteiro SET tempo_por_quantidade_base=7,unidade_tempo_operacao='M'");
        migrate();
        assertThat(scalar("SELECT quantidade_base || ':' || unidade_medida_quantidade_base_id FROM roteiro"))
                .isEqualTo("99:KG");
        assertThat(scalar("SELECT count(*) FROM operacao_roteiro WHERE tempo_por_quantidade_base=7 AND unidade_tempo_operacao='M'"))
                .isEqualTo("2");

    }

    @Test
    void populatedDurationWithoutUnitAlwaysRollsBackWithoutInferringHours() throws Exception {

        populatedLegacySchema();
        sql("ALTER TABLE operacao_roteiro ADD COLUMN tempo_por_quantidade_base double precision",
                "UPDATE operacao_roteiro SET tempo_por_quantidade_base=7 WHERE posicao=20");
        assertThatThrownBy(this::migrate).hasMessageContaining("without an explicit time unit").hasMessageContaining("R1/20");
        assertThat(columnExists("roteiro", "tipo_roteiro")).isFalse();
        assertThat(columnExists("operacao_roteiro", "unidade_tempo_operacao")).isFalse();
        assertThat(scalar("SELECT tempo_por_quantidade_base::text FROM operacao_roteiro WHERE posicao=20")).isEqualTo("7");

    }

    @Test
    void missingDurationWithMinutesIsAmbiguousAndRollsBack() throws Exception {

        populatedLegacySchema();
        sql("ALTER TABLE operacao_roteiro ADD COLUMN unidade_tempo_operacao varchar(1)",
                "UPDATE operacao_roteiro SET unidade_tempo_operacao='M' WHERE posicao=20");
        assertThatThrownBy(this::migrate).hasMessageContaining("partially configured duration/unit");
        assertThat(columnExists("roteiro", "tipo_roteiro")).isFalse();
        assertThat(columnExists("operacao_roteiro", "tempo_por_quantidade_base")).isFalse();
        assertThat(scalar("SELECT unidade_tempo_operacao FROM operacao_roteiro WHERE posicao=20")).isEqualTo("M");

    }

    @Test
    void missingLegacyHoursDoesNotBecomeADefaultCapacity() throws Exception {

        populatedLegacySchema();
        sql("UPDATE operacao_roteiro SET horas_por_quantidade_base=NULL WHERE posicao=20");
        assertThatThrownBy(this::migrate).hasMessageContaining("legacy hours cannot be copied");
        assertThat(columnExists("roteiro", "tipo_roteiro")).isFalse();
        assertThat(columnExists("operacao_roteiro", "tempo_por_quantidade_base")).isFalse();
        assertThat(scalar("SELECT count(*) FROM operacao_roteiro WHERE horas_por_quantidade_base IS NULL")).isEqualTo("1");

    }

    @Test
    void divergentLegacyUnitsDoNotSelectAnArbitraryRoutingUnit() throws Exception {

        populatedLegacySchema();
        sql("UPDATE operacao_roteiro SET unidade_medida_id='KG' WHERE posicao=20");
        assertThatThrownBy(this::migrate).hasMessageContaining("bases/units disagree");
        assertThat(columnExists("roteiro", "tipo_roteiro")).isFalse();
        assertThat(columnExists("roteiro", "unidade_medida_quantidade_base_id")).isFalse();
        assertThat(rows("SELECT unidade_medida_id FROM operacao_roteiro ORDER BY posicao")).containsExactly("TON", "KG");

    }

    @Test
    void explicitHoursWithMissingDurationMigratesOnlyTheDuration() throws Exception {

        populatedLegacySchema();
        sql("ALTER TABLE operacao_roteiro ADD COLUMN unidade_tempo_operacao varchar(1)",
                "UPDATE operacao_roteiro SET unidade_tempo_operacao='H'");
        migrate();
        assertThat(scalar("SELECT tempo_por_quantidade_base::text FROM operacao_roteiro WHERE posicao=10")).isEqualTo("1.25");
        assertThat(scalar("SELECT count(*) FROM operacao_roteiro WHERE unidade_tempo_operacao='H'")).isEqualTo("2");

    }

    @Test
    void unknownExistingDiscriminatorFailsWithoutReplacingIt() throws Exception {

        populatedLegacySchema();
        sql("ALTER TABLE roteiro ADD COLUMN tipo_roteiro varchar(31)", "UPDATE roteiro SET tipo_roteiro='unknown'");
        assertThatThrownBy(this::migrate).hasMessageContaining("unknown discriminator");
        assertThat(scalar("SELECT tipo_roteiro FROM roteiro")).isEqualTo("unknown");

    }

    private void migrate() {

        new ProductionSchemaCompatibilityInitializer(dataSource).afterPropertiesSet();

    }

    private void legacySchema() throws Exception {

        sql("CREATE TABLE roteiro (id varchar(50) PRIMARY KEY,material_output_id varchar(50) NOT NULL)",
                "CREATE TABLE lista_tecnica (id varchar(50) PRIMARY KEY,material_output_id varchar(50) NOT NULL)",
                "CREATE TABLE operacao_roteiro (roteiro_id varchar(50) REFERENCES roteiro(id),posicao int,quantidade_base real,unidade_medida_id varchar(50),horas_por_quantidade_base real,PRIMARY KEY(roteiro_id,posicao))",
                "CREATE TABLE versao_producao (id varchar(50) PRIMARY KEY,tipo_versao_producao varchar(31) NOT NULL,roteiro_id varchar(50) REFERENCES roteiro(id),lista_tecnica_id varchar(50) REFERENCES lista_tecnica(id))",
                "CREATE TABLE demand_preservation (id varchar(50) PRIMARY KEY,value numeric)");

    }

    private void populatedLegacySchema() throws Exception {

        legacySchema();
        sql("INSERT INTO roteiro VALUES ('R1','M1')", "INSERT INTO lista_tecnica VALUES ('B1','M1')",
                "INSERT INTO operacao_roteiro VALUES ('R1',10,1,'TON',1.25),('R1',20,1,'TON',0.8)",
                "INSERT INTO versao_producao VALUES ('DEFAULT_PRODUCTION_VERSION','inexistente',NULL,NULL),('V1','simples','R1','B1')",
                "INSERT INTO demand_preservation VALUES ('DP_MONTHLY_TUTORIAL',123.45)");

    }

    private List<String> snapshot() throws Exception {

        return rows("SELECT row_to_json(r)::text FROM roteiro r UNION ALL SELECT row_to_json(o)::text FROM operacao_roteiro o "
                + "UNION ALL SELECT row_to_json(b)::text FROM lista_tecnica b UNION ALL SELECT row_to_json(v)::text FROM versao_producao v "
                + "UNION ALL SELECT row_to_json(d)::text FROM demand_preservation d ORDER BY 1");

    }

    private boolean columnExists(String table, String column) throws Exception {

        return !scalar("SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='"
                + table + "' AND column_name='" + column + "'").equals("0");

    }

    private String scalar(String query) throws Exception {

        return rows(query).getFirst();

    }

    private List<String> rows(String query) throws Exception {

        List<String> values = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(query)) {
            while (result.next()) {
                values.add(result.getString(1));
            }
        }
        return values;

    }

    private void sql(String... statements) throws Exception {

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }

    }

    private static void runTool(String name, String... arguments) throws Exception {

        Path executable = binaries.resolve(name + (System.getProperty("os.name").startsWith("Windows") ? ".exe" : ""));
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(List.of(arguments));
        Path log = temporaryDirectory.resolve(name + "-" + UUID.randomUUID() + ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if (!process.waitFor(Duration.ofSeconds(45).toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(name + " timed out: " + log);
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException(name + " failed: " + Files.readString(log));
            }
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor();
            }
        }

    }
}
