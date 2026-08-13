package com.opsfactor.community;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Contratos de propriedades publicadas pela distribuicao Community.
 *
 * <p>O Community deve ser facil de executar por quem baixar o backend aberto,
 * mas o runtime local/produtivo nao deve recriar schema automaticamente. Neste
 * recorte seguimos com {@code ddl-auto=update}; {@code create-drop} fica
 * permitido apenas em recursos de teste automatizado.</p>
 */
class CommunityBootstrapPropertiesTest {

    private static final String WORKSPACE_DIRECTORY_NAME = "opsfactor-community";

    private static final String COMMUNITY_CONFIG_IMPORT =
            "classpath:application-community-defaults.properties";

    @Test
    void communityWebShouldImportCommunityDefaultsAndUsePostgreSqlProfile() throws IOException {

        Properties webApplicationProperties = loadProperties(
                "src/main/resources/application.properties");

        Assertions.assertEquals(
                COMMUNITY_CONFIG_IMPORT,
                webApplicationProperties.getProperty("spring.config.import"));
        Assertions.assertEquals(
                "prd,database-postgresql",
                webApplicationProperties.getProperty("spring.profiles.active"));

    }

    @Test
    void communityDefaultsShouldDisableOpenApiAndSwagger() throws IOException {

        Properties communityDefaults = loadProperties(
                "src/main/resources/application-community-defaults.properties");

        Assertions.assertEquals("false", communityDefaults.getProperty("springdoc.api-docs.enabled"));
        Assertions.assertEquals("false", communityDefaults.getProperty("springdoc.swagger-ui.enabled"));
        Assertions.assertNull(communityDefaults.getProperty("opsfactor.openapi.enabled"));

    }

    @Test
    void communityPostgreSqlProfileShouldUseOpenDriverAndDialect() throws IOException {

        Properties postgreSqlApplicationProperties = loadProperties(
                "src/main/resources/application-database-postgresql.properties");

        Assertions.assertEquals(
                "org.postgresql.Driver",
                postgreSqlApplicationProperties.getProperty("spring.datasource.driver-class-name"));
        Assertions.assertEquals(
                "com.opsfactor.community.platform.database.hibernate.dialect.PlanningPostgreSQLDialect",
                postgreSqlApplicationProperties.getProperty("spring.jpa.properties.hibernate.dialect"));
        Assertions.assertEquals(
                "true",
                postgreSqlApplicationProperties.getProperty("spring.jpa.properties.hibernate.globally_quoted_identifiers"));
        Assertions.assertEquals(
                "true",
                postgreSqlApplicationProperties.getProperty(
                        "spring.jpa.properties.hibernate.globally_quoted_identifiers_skip_column_definitions"));
        Assertions.assertEquals(
                "${OPSFACTOR_DATASOURCE_USERNAME:opsfactor}",
                postgreSqlApplicationProperties.getProperty("spring.datasource.username"));
        Assertions.assertEquals(
                "${OPSFACTOR_DATASOURCE_PASSWORD:}",
                postgreSqlApplicationProperties.getProperty("spring.datasource.password"));

    }

    @Test
    void communityRuntimeDatabaseProfilesShouldUseDdlAutoUpdate() throws IOException {

        assertDdlAutoUpdate("src/main/resources/application-dev.properties");
        assertDdlAutoUpdate("src/main/resources/application-database-postgresql.properties");

    }

    @Test
    void communityApplicationPropertiesShouldLiveOnlyInExecutableWebModule() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<Path> allowedApplicationProperties = List.of(
                communityWorkspaceDirectory.resolve("src/main/resources/application.properties").normalize());
        List<String> violations = new ArrayList<>();

        /*
         * O Community possui um unico executavel Spring Boot. Defaults e
         * profiles especificos podem existir ao lado dele, mas nao deve haver
         * outro application.properties carregado automaticamente como camada
         * silenciosa de configuracao.
         */
        try (Stream<Path> pathStream = Files.walk(communityWorkspaceDirectory)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isTargetFile(path))
                    .filter(CommunityBootstrapPropertiesTest::isMainResourcesFile)
                    .filter(path -> "application.properties".equals(path.getFileName().toString()))
                    .filter(path -> !allowedApplicationProperties.contains(path.normalize()))
                    .map(communityWorkspaceDirectory::relativize)
                    .map(Path::toString)
                    .forEach(violations::add);
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "Community deve publicar application.properties somente no executavel web:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainResourcesShouldNotUseCreateDropSchemaStrategy() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Test resources podem usar create-drop como fixture isolada. Runtime
         * publicado em src/main/resources deve continuar em update para evitar
         * destruir base local ou de cliente em execucoes normais.
         */
        for (Path mainResourceFile : findMainResourceConfigurationFiles(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(mainResourceFile, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (sourceLine.contains("ddl-auto=create-drop")) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            mainResourceFile,
                            lineIndex,
                            sourceLine));
                }
            }
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "Community runtime resources must not use ddl-auto=create-drop:\n"
                        + String.join("\n", violations));

    }

    private void assertDdlAutoUpdate(String relativePath) throws IOException {

        Properties properties = loadProperties(relativePath);

        Assertions.assertEquals(
                "update",
                properties.getProperty("spring.jpa.hibernate.ddl-auto"),
                relativePath + " must keep ddl-auto=update for runtime profiles.");

    }

    private Properties loadProperties(String relativePath) throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path propertiesPath = communityWorkspaceDirectory.resolve(relativePath);

        Properties properties = new Properties();
        try (Reader propertiesReader = Files.newBufferedReader(propertiesPath, StandardCharsets.UTF_8)) {
            properties.load(propertiesReader);
        }
        return properties;

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        while (currentDirectory != null
                && !WORKSPACE_DIRECTORY_NAME.equals(currentDirectory.getFileName().toString())) {
            currentDirectory = currentDirectory.getParent();
        }
        if (currentDirectory == null) {
            throw new IllegalStateException("Could not resolve " + WORKSPACE_DIRECTORY_NAME + " workspace directory.");
        }
        return currentDirectory;

    }

    private List<Path> findMainResourceConfigurationFiles(Path communityWorkspaceDirectory) throws IOException {

        try (Stream<Path> pathStream = Files.walk(communityWorkspaceDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isTargetFile(path))
                    .filter(path -> path.toString().contains("src\\main\\resources")
                            || path.toString().contains("src/main/resources"))
                    .filter(CommunityBootstrapPropertiesTest::isConfigurationFile)
                    .toList();
        }

    }

    private static boolean isConfigurationFile(Path path) {

        String fileName = path.getFileName().toString();
        return fileName.endsWith(".properties")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml");

    }

    private static boolean isMainResourcesFile(Path path) {

        String normalizedPath = path.toString().replace('\\', '/');
        return normalizedPath.contains("/src/main/resources/");

    }

    private static boolean isTargetFile(Path path) {

        return path.toString().contains("\\target\\")
                || path.toString().contains("/target/");

    }

    private static String formatViolation(
            Path workspaceDirectory,
            Path sourcePath,
            int zeroBasedLineIndex,
            String sourceLine) {

        return workspaceDirectory.relativize(sourcePath)
                + ":"
                + (zeroBasedLineIndex + 1)
                + " "
                + sourceLine.trim();

    }

}
