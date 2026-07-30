package com.opsfactor.community;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Protege a separação entre services funcionais e contratos de consumidor.
 */
class CapabilityFacadeBoundaryTest {

    /**
     * DTOs oferecidos por uma façade ou pela borda web não podem contaminar o
     * contrato dos services internos da capability.
     */
    @Test
    void baseServicesMustNotImportConsumerDtos() throws IOException {

        Path capabilitySourceRoot = Paths.get(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        try (Stream<Path> sourcePathStream = Files.walk(capabilitySourceRoot)) {
            for (Path sourcePath : sourcePathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                String normalizedPath = sourcePath.toString().replace('\\', '/');
                if (!normalizedPath.contains("/service/")
                        || normalizedPath.contains("/integration/")) {
                    continue;
                }

                for (String line : Files.readAllLines(sourcePath, StandardCharsets.UTF_8)) {
                    String normalizedLine = line.trim();
                    if (normalizedLine.startsWith("import ")
                            && (normalizedLine.contains(".facade.dto.")
                            || normalizedLine.contains(".web.dto."))) {
                        violations.add(normalizedPath + "#" + normalizedLine);
                    }
                }
            }
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "Services internos não podem depender de DTOs de façade/web: "
                        + violations);

    }

}
