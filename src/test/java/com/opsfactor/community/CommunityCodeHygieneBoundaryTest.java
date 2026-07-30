package com.opsfactor.community;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guarda de higiene para fontes principais Community.
 *
 * <p>O repo Community sera publicado. Logs operacionais devem passar por logger
 * da aplicacao, erros obrigatorios devem ter mensagem explicita e codigo
 * migrado nao deve carregar prints/stack traces crus do legado.</p>
 */
class CommunityCodeHygieneBoundaryTest {

    private static final List<String> COMMUNITY_MAIN_SOURCE_DIRECTORIES = List.of(
            "src/main/java"
    );

    private static final List<String> COMMUNITY_TEST_SOURCE_DIRECTORIES = List.of(
            "src/test/java"
    );

    private static final List<String> FORBIDDEN_CONSOLE_OUTPUT_TOKENS = List.of(
            "System.out",
            "System.err",
            "printStackTrace("
    );

    private static final List<String> FORBIDDEN_IMPLICIT_GUARD_TOKENS = List.of(
            "Objects.requireNonNull"
    );

    private static final List<String> FORBIDDEN_FATAL_ERROR_TOKENS = List.of(
            "throw new Error",
            "new IllegalAccessError"
    );

    private static final Pattern GENERIC_EXCEPTION_CATCH_PATTERN = Pattern.compile(
            ".*catch\\s*\\(\\s*Exception.*");

    private static final Pattern PRIVATE_SUPPORT_DATA_MAP_HELPER_PATTERN = Pattern.compile(
            ".*private\\s+<[^>]+>\\s+Map\\s*<\\s*String\\s*,.*getMapaPorIdObrigatorio\\s*\\(.*");

    private static final Map<String, Integer> REVIEWED_GENERIC_EXCEPTION_CATCH_COUNTS =
            reviewedGenericExceptionCatchCounts();

    @Test
    void communityMainSourcesShouldNotWriteDirectlyToConsole() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Esta regra evita que codigo migrado do legado traga prints de debug
         * para uma base que sera publicada. Mensagem operacional deve ser
         * emitida pelo logger da propria classe.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (containsForbiddenConsoleOutputToken(sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Fontes principais Community devem usar logger, sem console direto ou stack trace cru:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldUseExplicitArgumentOrStateErrorsInsteadOfImplicitGuardsOrFatalErrors()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Como o Community sera publico, guards obrigatorios devem explicar se
         * o problema e argumento invalido, estado inconsistente ou configuracao
         * quebrada. Isso evita NullPointerException/Error sem contexto e impede
         * que rejeicoes funcionais, como location fora da view, usem erros
         * fatais da JVM.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (containsForbiddenImplicitGuardToken(sourceLine)
                        || containsForbiddenFatalErrorToken(sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Fontes principais Community devem usar erros explicitos, sem Objects.requireNonNull ou Error fatal:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotAddNewGenericExceptionCatchWithoutReview() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Map<String, Integer> genericExceptionCatchCounts = new LinkedHashMap<>();
        List<String> unreviewedViolations = new ArrayList<>();

        /*
         * Nao ha captura generica produtiva aprovada no Community. Task.run()
         * captura explicitamente JsonProcessingException, RuntimeException e
         * Error para formar historico tecnico; controllers, scheduler web e
         * motores de plano tambem estreitaram suas bordas. A guarda congela esse
         * inventario vazio para impedir que novas capturas amplas entrem
         * silenciosamente no codigo Community.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            String relativeSourcePath = normalizeRelativePath(communityWorkspaceDirectory, javaSourcePath);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (isGenericExceptionCatch(sourceLine)) {
                    genericExceptionCatchCounts.merge(relativeSourcePath, 1, Integer::sum);
                    if (!REVIEWED_GENERIC_EXCEPTION_CATCH_COUNTS.containsKey(relativeSourcePath)) {
                        unreviewedViolations.add(formatViolation(
                                communityWorkspaceDirectory,
                                javaSourcePath,
                                lineIndex,
                                sourceLine));
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> reviewedCatchCountEntry
                : REVIEWED_GENERIC_EXCEPTION_CATCH_COUNTS.entrySet()) {
            Integer actualCatchCount = genericExceptionCatchCounts.getOrDefault(
                    reviewedCatchCountEntry.getKey(),
                    0);
            if (!reviewedCatchCountEntry.getValue().equals(actualCatchCount)) {
                unreviewedViolations.add(
                        reviewedCatchCountEntry.getKey()
                                + ": expected "
                                + reviewedCatchCountEntry.getValue()
                                + " reviewed generic catch(es), found "
                                + actualCatchCount
                                + ".");
            }
        }

        assertTrue(
                unreviewedViolations.isEmpty(),
                "Novos catch (Exception...) em fontes Community precisam de revisao funcional explicita:\n"
                        + String.join("\n", unreviewedViolations));

    }

    @Test
    void communityMainSourcePackagesShouldMatchSourceDirectory() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O Community usa um build raiz simples e separa ownership por
         * packages. O package declarado precisa acompanhar o diretorio real
         * para que Maven, IntelliJ, component scan e buscas arquiteturais
         * apontem para o mesmo recorte, inclusive em package-info.java.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String declaredPackage = getDeclaredPackage(javaSourcePath);
            String expectedPackage = getExpectedPackageForSourcePath(
                    communityWorkspaceDirectory,
                    javaSourcePath);
            if (!expectedPackage.equals(declaredPackage)) {
                violations.add(
                        communityWorkspaceDirectory.relativize(javaSourcePath)
                                + ": expected package "
                                + expectedPackage
                                + ", found "
                                + declaredPackage
                                + ".");
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Fontes principais Community devem declarar package alinhado ao diretorio:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityTestSourcePackagesShouldMatchSourceDirectory() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Testes tambem fazem parte do ownership do mega-repo achatado. O
         * package precisa acompanhar a pasta para que fixtures, guards e buscas
         * arquiteturais continuem apontando para o recorte correto.
         */
        for (Path javaSourcePath : findCommunityTestJavaSources(communityWorkspaceDirectory)) {
            String declaredPackage = getDeclaredPackage(javaSourcePath);
            String expectedPackage = getExpectedPackageForSourcePath(
                    communityWorkspaceDirectory,
                    "src/test/java",
                    javaSourcePath);
            if (!expectedPackage.equals(declaredPackage)) {
                violations.add(
                        communityWorkspaceDirectory.relativize(javaSourcePath)
                                + ": expected package "
                                + expectedPackage
                                + ", found "
                                + declaredPackage
                                + ".");
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Testes Community devem declarar package alinhado ao diretorio:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityRepositoriesShouldHaveProductionConsumers() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<Path> communityMainJavaSources = findCommunityMainJavaSources(communityWorkspaceDirectory);
        List<String> violations = new ArrayList<>();

        /*
         * O repositorio Community sera publicado. Repositories sem consumidor
         * produtivo confundem a superficie aberta da plataforma: parecem API
         * persistente ativa, mas nao participam de nenhum service, projection,
         * controller ou task.
         */
        for (Path repositorySourcePath : findCommunityRepositorySources(communityMainJavaSources)) {
            String repositorySimpleName = repositorySourcePath.getFileName()
                    .toString()
                    .replace(".java", "");
            if (!hasReferenceOutsideOwnFile(
                    communityMainJavaSources,
                    repositorySourcePath,
                    repositorySimpleName)) {
                violations.add(communityWorkspaceDirectory.relativize(repositorySourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Repositories Community precisam ter consumidor produtivo fora da propria declaracao:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityIntegrationServicesShouldUseCentralSupportDataValidationHelper() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Support data de uploads Community deve ser indexado pelo helper
         * comum para preservar a mesma semantica em todos os services:
         * colecao vazia e valida; snapshot nulo, item nulo, id vazio ou
         * duplicado falha antes do mapper. Helpers privados duplicados tendem
         * a divergir silenciosamente quando novos uploads sao migrados.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String relativeSourcePath = normalizeRelativePath(communityWorkspaceDirectory, javaSourcePath);
            if (!relativeSourcePath.contains("/services/integration/")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (isPrivateSupportDataMapHelper(sourceLine)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Integration services Community devem usar IntegrationSupportDataValidation.getMapaPorIdObrigatorio, sem helpers privados duplicados:\n"
                        + String.join("\n", violations));

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        if ("community".equals(currentDirectory.getFileName().toString())) {
            return currentDirectory.getParent();
        }

        return currentDirectory;

    }

    private List<Path> findCommunityMainJavaSources(Path communityWorkspaceDirectory) throws IOException {

        List<Path> javaSourcePaths = new ArrayList<>();
        for (String communityMainSourceDirectory : COMMUNITY_MAIN_SOURCE_DIRECTORIES) {
            Path sourceDirectory = communityWorkspaceDirectory.resolve(communityMainSourceDirectory);
            if (!Files.exists(sourceDirectory)) {
                continue;
            }

            try (Stream<Path> pathStream = Files.walk(sourceDirectory)) {
                javaSourcePaths.addAll(pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .toList());
            }
        }
        return javaSourcePaths;

    }

    private List<Path> findCommunityTestJavaSources(Path communityWorkspaceDirectory) throws IOException {

        List<Path> javaSourcePaths = new ArrayList<>();
        for (String communityTestSourceDirectory : COMMUNITY_TEST_SOURCE_DIRECTORIES) {
            Path sourceDirectory = communityWorkspaceDirectory.resolve(communityTestSourceDirectory);
            if (!Files.exists(sourceDirectory)) {
                continue;
            }

            try (Stream<Path> pathStream = Files.walk(sourceDirectory)) {
                javaSourcePaths.addAll(pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .toList());
            }
        }
        return javaSourcePaths;

    }

    private String getExpectedPackageForSourcePath(
            Path communityWorkspaceDirectory,
            Path javaSourcePath) {

        return getExpectedPackageForSourcePath(
                communityWorkspaceDirectory,
                "src/main/java",
                javaSourcePath);

    }

    private String getExpectedPackageForSourcePath(
            Path communityWorkspaceDirectory,
            String relativeSourceRoot,
            Path javaSourcePath) {

        Path javaSourceRootPath = communityWorkspaceDirectory.resolve(relativeSourceRoot);
        Path relativePackagePath = javaSourceRootPath.relativize(javaSourcePath.getParent());
        return relativePackagePath
                .toString()
                .replace('\\', '.')
                .replace('/', '.');

    }

    private String getDeclaredPackage(Path javaSourcePath) throws IOException {

        List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
        for (String sourceLine : sourceLines) {
            String trimmedLine = sourceLine.trim();
            if (trimmedLine.startsWith("package ")
                    && trimmedLine.endsWith(";")) {
                return trimmedLine
                        .substring(
                                "package ".length(),
                                trimmedLine.length() - 1)
                        .trim();
            }
        }
        return "";

    }

    private List<Path> findCommunityRepositorySources(List<Path> communityMainJavaSources) {

        return communityMainJavaSources
                .stream()
                .filter(path -> path.getFileName().toString().endsWith("Repository.java"))
                .toList();

    }

    private boolean hasReferenceOutsideOwnFile(
            List<Path> communityMainJavaSources,
            Path repositorySourcePath,
            String repositorySimpleName) throws IOException {

        for (Path javaSourcePath : communityMainJavaSources) {
            if (javaSourcePath.equals(repositorySourcePath)) {
                continue;
            }
            String sourceContent = Files.readString(javaSourcePath, StandardCharsets.UTF_8);
            if (sourceContent.contains(repositorySimpleName)) {
                return true;
            }
        }
        return false;

    }

    private static boolean containsForbiddenConsoleOutputToken(String sourceLine) {

        return FORBIDDEN_CONSOLE_OUTPUT_TOKENS.stream().anyMatch(sourceLine::contains);

    }

    private static boolean containsForbiddenImplicitGuardToken(String sourceLine) {

        return FORBIDDEN_IMPLICIT_GUARD_TOKENS.stream().anyMatch(sourceLine::contains);

    }

    private static boolean containsForbiddenFatalErrorToken(String sourceLine) {

        return FORBIDDEN_FATAL_ERROR_TOKENS.stream().anyMatch(sourceLine::contains);

    }

    private static boolean isGenericExceptionCatch(String sourceLine) {

        return GENERIC_EXCEPTION_CATCH_PATTERN.matcher(sourceLine).matches();

    }

    private static boolean isPrivateSupportDataMapHelper(String sourceLine) {

        return PRIVATE_SUPPORT_DATA_MAP_HELPER_PATTERN.matcher(sourceLine).matches();

    }

    private static String normalizeRelativePath(Path workspaceDirectory, Path sourcePath) {

        return workspaceDirectory
                .relativize(sourcePath)
                .toString()
                .replace('\\', '/');

    }

    private static String formatViolation(Path workspaceDirectory, Path violationPath, int lineIndex, String line) {

        return workspaceDirectory.relativize(violationPath) + ":" + (lineIndex + 1) + ": " + line.trim();

    }

    private static Map<String, Integer> reviewedGenericExceptionCatchCounts() {

        return Map.of();

    }

}
