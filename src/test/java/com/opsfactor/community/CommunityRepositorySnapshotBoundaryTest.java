package com.opsfactor.community;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guarda de cardinalidade para snapshots de repositories Community.
 *
 * <p>Snapshots agregados ou consolidados precisam preservar a cardinalidade
 * retornada pelo banco ate a validation/factory consumidora. Por isso, o
 * boundary de repository nao deve voltar a expor {@code Set} para retornos
 * quantitativos agregados.</p>
 */
class CommunityRepositorySnapshotBoundaryTest {

    private static final List<String> COMMUNITY_MAIN_SOURCE_DIRECTORIES = List.of(
            "src/main/java"
    );

    @Test
    void communityAggregatedRepositorySnapshotsShouldNotReturnSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Parametros Set continuam permitidos para filtros `IN`. A regra olha
         * somente declaracoes de repositories cuja assinatura de retorno ou
         * nome de metodo indique snapshot agregado/consolidado.
         */
        for (Path repositorySourcePath : findCommunityRepositorySources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(repositorySourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (!sourceLine.contains("Set<")) {
                    continue;
                }

                String methodSignature = collectMethodSignature(sourceLines, lineIndex);
                if (isAggregatedOrConsolidatedSetReturningSignature(methodSignature)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            repositorySourcePath,
                            lineIndex,
                            methodSignature));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Snapshots agregados/consolidados de repositories Community devem retornar List/Collection, nao Set:\n"
                        + String.join("\n", violations));

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();
        if (Files.exists(currentDirectory.resolve("src/main/java"))) {
            return currentDirectory;
        }

        Path communityWorkspaceDirectory = currentDirectory.resolve("opsfactor-community");
        if (Files.exists(communityWorkspaceDirectory.resolve("src/main/java"))) {
            return communityWorkspaceDirectory;
        }

        return currentDirectory;

    }

    private List<Path> findCommunityRepositorySources(Path communityWorkspaceDirectory) throws IOException {

        List<Path> repositorySourcePaths = new ArrayList<>();
        for (String communityMainSourceDirectory : COMMUNITY_MAIN_SOURCE_DIRECTORIES) {
            Path sourceDirectory = communityWorkspaceDirectory.resolve(communityMainSourceDirectory);
            if (!Files.exists(sourceDirectory)) {
                continue;
            }

            try (Stream<Path> pathStream = Files.walk(sourceDirectory)) {
                repositorySourcePaths.addAll(pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith("Repository.java"))
                        .toList());
            }
        }
        return repositorySourcePaths;

    }

    private static String collectMethodSignature(List<String> sourceLines, int firstLineIndex) {

        StringBuilder methodSignature = new StringBuilder();
        for (int lineIndex = firstLineIndex; lineIndex < sourceLines.size(); lineIndex++) {
            methodSignature.append(sourceLines.get(lineIndex).trim()).append(' ');
            if (sourceLines.get(lineIndex).contains(";")) {
                break;
            }
            if (lineIndex - firstLineIndex >= 8) {
                break;
            }
        }
        return methodSignature.toString().trim();

    }

    private static boolean isAggregatedOrConsolidatedSetReturningSignature(String methodSignature) {

        String normalizedSignature = methodSignature.replace(" ", "");
        return (normalizedSignature.startsWith("publicSet<")
                || normalizedSignature.startsWith("Set<"))
                && (normalizedSignature.contains("Aggregated")
                || normalizedSignature.contains("Consolid")
                || normalizedSignature.contains("consolidated"));

    }

    private static String formatViolation(
            Path communityWorkspaceDirectory,
            Path repositorySourcePath,
            int lineIndex,
            String methodSignature) {

        return communityWorkspaceDirectory.relativize(repositorySourcePath).toString().replace('\\', '/')
                + ":"
                + (lineIndex + 1)
                + " -> "
                + methodSignature;

    }

}
