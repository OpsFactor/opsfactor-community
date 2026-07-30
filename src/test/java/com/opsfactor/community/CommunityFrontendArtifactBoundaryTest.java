package com.opsfactor.community;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Protege a decisao de manter o backend Community sem artefatos estaticos do
 * front-end.
 *
 * <p>O front Community vive no repositorio `opsfactor-community-front` e sera
 * compilado pelo pipeline de release. O instalador/imagem final pode juntar o
 * servidor front ao artefato distribuido, mas o repo backend nao deve versionar
 * `dist`, templates legados, pastas estaticas de SPA ou arquivos legados
 * `.html`, `.js`, `.css` e `.map`.</p>
 */
class CommunityFrontendArtifactBoundaryTest {

    private static final String WORKSPACE_DIRECTORY_NAME = "opsfactor-community";

    private static final List<String> IGNORED_DIRECTORY_NAMES = List.of(
            ".git",
            ".idea",
            "target");

    private static final List<String> FORBIDDEN_FRONTEND_ARTIFACT_DIRECTORY_NAMES = List.of(
            "dist",
            "static",
            "public",
            "templates");

    private static final List<String> FORBIDDEN_FRONTEND_ARTIFACT_FILE_EXTENSIONS = List.of(
            ".html",
            ".js",
            ".css",
            ".map");

    @Test
    void backendShouldNotVersionStaticFrontendArtifacts() throws IOException {

        Path workspaceDirectory = resolveWorkspaceDirectory();
        List<String> violations;

        try (Stream<Path> paths = Files.walk(workspaceDirectory)) {
            violations = paths
                    .filter(path -> !isIgnoredPath(workspaceDirectory, path))
                    .filter(CommunityFrontendArtifactBoundaryTest::isForbiddenFrontendArtifactPath)
                    .map(path -> workspaceDirectory.relativize(path).toString())
                    .sorted()
                    .toList();
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "Backend Community must not version frontend build/static directories or legacy frontend files:\n"
                        + String.join("\n", violations));

    }

    private static Path resolveWorkspaceDirectory() {

        Path currentDirectory = Path.of("").toAbsolutePath();
        while (currentDirectory != null
                && !WORKSPACE_DIRECTORY_NAME.equals(currentDirectory.getFileName().toString())) {
            currentDirectory = currentDirectory.getParent();
        }
        if (currentDirectory == null) {
            throw new IllegalStateException("Could not resolve " + WORKSPACE_DIRECTORY_NAME + " workspace directory.");
        }
        return currentDirectory;

    }

    private static boolean isIgnoredPath(Path workspaceDirectory, Path path) {

        Path relativePath = workspaceDirectory.relativize(path);
        for (Path pathPart : relativePath) {
            if (IGNORED_DIRECTORY_NAMES.contains(pathPart.toString())) {
                return true;
            }
        }
        return false;

    }

    private static boolean isForbiddenFrontendArtifactPath(Path path) {

        if (isForbiddenFrontendArtifactDirectory(path)) {
            return true;
        }

        return Files.isRegularFile(path)
                && hasForbiddenFrontendArtifactFileExtension(path);

    }

    private static boolean isForbiddenFrontendArtifactDirectory(Path path) {

        Path fileName = path.getFileName();
        return fileName != null
                && FORBIDDEN_FRONTEND_ARTIFACT_DIRECTORY_NAMES.contains(fileName.toString());

    }

    private static boolean hasForbiddenFrontendArtifactFileExtension(Path path) {

        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }

        String fileNameText = fileName.toString().toLowerCase(Locale.ROOT);
        return FORBIDDEN_FRONTEND_ARTIFACT_FILE_EXTENSIONS
                .stream()
                .anyMatch(fileNameText::endsWith);

    }

}
