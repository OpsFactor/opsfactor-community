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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guarda de legibilidade para construtores de beans Spring no Community.
 *
 * <p>Campos Spring ja sao protegidos por {@link CommunityAutowiredFieldBoundaryTest}.
 * Este teste cobre o outro caminho aceito pelo container: construtores de
 * beans que recebem colaboradores Spring ou classes com varios construtores
 * publicos precisam declarar {@code @Autowired} explicitamente.</p>
 */
class CommunityAutowiredConstructorBoundaryTest {

    private static final List<String> COMMUNITY_MAIN_SOURCE_DIRECTORIES = List.of(
            "src/main/java"
    );

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
            "^\\s*(?:public\\s+)?(?:abstract\\s+|final\\s+)?class\\s+(\\w+).*");

    private static final List<String> SPRING_BEAN_ANNOTATIONS = List.of(
            "@Service",
            "@Component",
            "@RestController",
            "@Controller",
            "@Configuration",
            "@Repository",
            "@SpringBootApplication"
    );

    private static final List<String> LOMBOK_CONSTRUCTOR_INJECTION_ANNOTATIONS = List.of(
            "@RequiredArgsConstructor",
            "@AllArgsConstructor"
    );

    private static final List<String> SPRING_DEPENDENCY_PARAMETER_TOKENS = List.of(
            "Repository",
            "Service",
            "Mapper",
            "Factory",
            "Engine",
            "Processor",
            "Disaggregation",
            "Forecaster",
            "Orchestrator",
            "ApplicationContext",
            "Authentication",
            "PasswordEncoder",
            "EntityManager",
            "ObjectMapper",
            "JdbcTemplate",
            "TaskSchedulingService"
    );

    @Test
    void communityBeanConstructorsWithSpringDependenciesShouldUseExplicitAutowiredAnnotation() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * A regra foca construtores que claramente recebem colaboradores de
         * aplicacao/infraestrutura. Construtores de DTO, entidade ou value
         * object nao entram porque a classe precisa ter anotacao Spring local.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (String beanClassName : findSpringBeanClassNames(sourceLines)) {
                for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                    if (isConstructorLine(sourceLines.get(lineIndex), beanClassName)
                            && hasSpringDependencyParameter(sourceLines, lineIndex)
                            && !hasAutowiredAnnotationBeforeConstructor(sourceLines, lineIndex)) {
                        violations.add(formatViolation(
                                communityWorkspaceDirectory,
                                javaSourcePath,
                                lineIndex,
                                sourceLines.get(lineIndex)));
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Construtores Community que recebem dependencias Spring devem declarar @Autowired explicitamente:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityBeanClassesWithSingleParameterizedConstructorShouldUseExplicitAutowiredAnnotation() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Spring pode usar implicitamente o unico construtor parametrizado de
         * um bean, mesmo quando o parametro e escalar. A migracao exige que
         * essa escolha continue explicita por @Autowired.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (String beanClassName : findSpringBeanClassNames(sourceLines)) {
                List<Integer> constructorLineIndexes = findConstructorLineIndexes(sourceLines, beanClassName);
                if (constructorLineIndexes.size() != 1) {
                    continue;
                }

                int constructorLineIndex = constructorLineIndexes.get(0);
                if (hasConstructorParameter(sourceLines, constructorLineIndex)
                        && !hasAutowiredAnnotationBeforeConstructor(sourceLines, constructorLineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            constructorLineIndex,
                            sourceLines.get(constructorLineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Beans Community com construtor unico parametrizado devem declarar @Autowired explicitamente:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityBeanClassesWithSeveralPublicConstructorsShouldMarkRuntimeConstructor() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Quando um bean tem varios construtores publicos, a selecao feita pelo
         * Spring precisa ficar visivel no source. O caso mais comum e uma API
         * publica de suporte a testes/construcoes controladas junto do
         * construtor de runtime do container.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (String beanClassName : findSpringBeanClassNames(sourceLines)) {
                List<Integer> publicConstructorLineIndexes =
                        findPublicConstructorLineIndexes(sourceLines, beanClassName);
                if (publicConstructorLineIndexes.size() <= 1) {
                    continue;
                }

                boolean hasExplicitRuntimeConstructor = publicConstructorLineIndexes
                        .stream()
                        .anyMatch(lineIndex -> hasAutowiredAnnotationBeforeConstructor(sourceLines, lineIndex));
                if (!hasExplicitRuntimeConstructor) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            publicConstructorLineIndexes.get(0),
                            sourceLines.get(publicConstructorLineIndexes.get(0))));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Beans Community com varios construtores publicos devem marcar o construtor de runtime com @Autowired:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityBeanClassesShouldNotUseLombokConstructorInjectionAnnotations() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Lombok em classe Spring reintroduz o mesmo problema que este guarda
         * evita nos construtores manuais: dependencias passam a entrar por
         * construtor gerado, sem @Autowired visivel no source.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                Matcher classDeclarationMatcher = CLASS_DECLARATION_PATTERN.matcher(sourceLines.get(lineIndex));
                if (!classDeclarationMatcher.matches()
                        || !hasSpringBeanAnnotationBeforeClass(sourceLines, lineIndex)) {
                    continue;
                }

                int lombokAnnotationLineIndex = findLombokConstructorAnnotationBeforeClass(sourceLines, lineIndex);
                if (lombokAnnotationLineIndex >= 0) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lombokAnnotationLineIndex,
                            sourceLines.get(lombokAnnotationLineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Beans Community nao devem usar Lombok para gerar construtor de injecao; declare construtor @Autowired explicitamente:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityAutowiredConstructorsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O construtor @Autowired e a fronteira de wiring de runtime. O Javadoc
         * imediatamente anterior explica por que esse construtor existe e qual
         * colaborador Spring ele fixa no recorte Community/Enterprise.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (String beanClassName : findSpringBeanClassNames(sourceLines)) {
                for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                    if (!isConstructorLine(sourceLines.get(lineIndex), beanClassName)) {
                        continue;
                    }

                    int autowiredAnnotationLineIndex = findAutowiredAnnotationLineBeforeConstructor(
                            sourceLines,
                            lineIndex);
                    if (autowiredAnnotationLineIndex >= 0
                            && !hasJavadocImmediatelyBeforeAnnotation(sourceLines, autowiredAnnotationLineIndex)) {
                        violations.add(formatViolation(
                                communityWorkspaceDirectory,
                                javaSourcePath,
                                autowiredAnnotationLineIndex,
                                sourceLines.get(autowiredAnnotationLineIndex)));
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Construtores Community @Autowired devem ter Javadoc imediatamente anterior:\n"
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

    private static List<String> findSpringBeanClassNames(List<String> sourceLines) {

        Map<String, Integer> beanClassNames = new LinkedHashMap<>();
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            Matcher classDeclarationMatcher = CLASS_DECLARATION_PATTERN.matcher(sourceLines.get(lineIndex));
            if (classDeclarationMatcher.matches()
                    && hasSpringBeanAnnotationBeforeClass(sourceLines, lineIndex)) {
                beanClassNames.put(classDeclarationMatcher.group(1), lineIndex);
            }
        }
        return new ArrayList<>(beanClassNames.keySet());

    }

    private static boolean hasSpringBeanAnnotationBeforeClass(List<String> sourceLines, int classLineIndex) {

        for (int previousLineIndex = classLineIndex - 1; previousLineIndex >= 0; previousLineIndex--) {
            String previousLine = sourceLines.get(previousLineIndex).trim();
            if (SPRING_BEAN_ANNOTATIONS.stream().anyMatch(previousLine::startsWith)) {
                return true;
            }
            if (previousLine.isEmpty()
                    || previousLine.startsWith("@")
                    || previousLine.startsWith("*")
                    || previousLine.startsWith("/*")
                    || previousLine.startsWith("//")) {
                continue;
            }
            return false;
        }
        return false;

    }

    private static int findLombokConstructorAnnotationBeforeClass(List<String> sourceLines, int classLineIndex) {

        for (int previousLineIndex = classLineIndex - 1; previousLineIndex >= 0; previousLineIndex--) {
            String previousLine = sourceLines.get(previousLineIndex).trim();
            if (isLombokConstructorInjectionAnnotation(previousLine)) {
                return previousLineIndex;
            }
            if (previousLine.isEmpty()
                    || previousLine.startsWith("@")
                    || previousLine.startsWith("*")
                    || previousLine.startsWith("/*")
                    || previousLine.startsWith("//")) {
                continue;
            }
            return -1;
        }
        return -1;

    }

    private static boolean isLombokConstructorInjectionAnnotation(String sourceLine) {

        String annotationLine = sourceLine.trim();
        int inlineCommentIndex = annotationLine.indexOf("//");
        if (inlineCommentIndex >= 0) {
            annotationLine = annotationLine.substring(0, inlineCommentIndex).trim();
        }
        if (annotationLine.startsWith("//") || annotationLine.startsWith("*")) {
            return false;
        }

        String normalizedAnnotationLine = annotationLine;
        return LOMBOK_CONSTRUCTOR_INJECTION_ANNOTATIONS.stream().anyMatch(annotationToken ->
                normalizedAnnotationLine.equals(annotationToken)
                        || normalizedAnnotationLine.startsWith(annotationToken + "(")
                        || normalizedAnnotationLine.startsWith(annotationToken + " ")
                        || normalizedAnnotationLine.contains(" " + annotationToken)
                        || normalizedAnnotationLine.contains(" " + annotationToken + "("));

    }

    private static boolean isConstructorLine(String line, String className) {

        String trimmedLine = line.trim();
        return trimmedLine.startsWith("public " + className + "(")
                || trimmedLine.startsWith("protected " + className + "(");

    }

    private static boolean hasSpringDependencyParameter(List<String> sourceLines, int constructorLineIndex) {

        String constructorSignature = collectConstructorSignature(sourceLines, constructorLineIndex);
        int openParenthesisIndex = constructorSignature.indexOf('(');
        int closeParenthesisIndex = constructorSignature.lastIndexOf(')');
        if (openParenthesisIndex < 0 || closeParenthesisIndex <= openParenthesisIndex) {
            return false;
        }

        String parameterDeclaration = constructorSignature
                .substring(openParenthesisIndex + 1, closeParenthesisIndex)
                .trim();
        return !parameterDeclaration.isEmpty()
                && SPRING_DEPENDENCY_PARAMETER_TOKENS.stream().anyMatch(parameterDeclaration::contains);

    }

    private static boolean hasConstructorParameter(List<String> sourceLines, int constructorLineIndex) {

        String constructorSignature = collectConstructorSignature(sourceLines, constructorLineIndex);
        int openParenthesisIndex = constructorSignature.indexOf('(');
        int closeParenthesisIndex = constructorSignature.lastIndexOf(')');
        if (openParenthesisIndex < 0 || closeParenthesisIndex <= openParenthesisIndex) {
            return false;
        }

        return !constructorSignature
                .substring(openParenthesisIndex + 1, closeParenthesisIndex)
                .trim()
                .isEmpty();

    }

    private static String collectConstructorSignature(List<String> sourceLines, int constructorLineIndex) {

        StringBuilder constructorSignature = new StringBuilder();
        for (int lineIndex = constructorLineIndex; lineIndex < sourceLines.size(); lineIndex++) {
            constructorSignature.append(sourceLines.get(lineIndex).trim()).append(' ');
            if (sourceLines.get(lineIndex).contains(")")) {
                break;
            }
        }
        return constructorSignature.toString();

    }

    private static List<Integer> findConstructorLineIndexes(List<String> sourceLines, String className) {

        List<Integer> constructorLineIndexes = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            if (isConstructorLine(sourceLines.get(lineIndex), className)) {
                constructorLineIndexes.add(lineIndex);
            }
        }
        return constructorLineIndexes;

    }

    private static List<Integer> findPublicConstructorLineIndexes(List<String> sourceLines, String className) {

        List<Integer> constructorLineIndexes = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            if (sourceLines.get(lineIndex).trim().startsWith("public " + className + "(")) {
                constructorLineIndexes.add(lineIndex);
            }
        }
        return constructorLineIndexes;

    }

    private static boolean hasAutowiredAnnotationBeforeConstructor(List<String> sourceLines, int constructorLineIndex) {

        return findAutowiredAnnotationLineBeforeConstructor(sourceLines, constructorLineIndex) >= 0;

    }

    private static int findAutowiredAnnotationLineBeforeConstructor(List<String> sourceLines, int constructorLineIndex) {

        for (int previousLineIndex = constructorLineIndex - 1; previousLineIndex >= 0; previousLineIndex--) {
            String previousLine = sourceLines.get(previousLineIndex).trim();
            if (previousLine.startsWith("@Autowired")) {
                return previousLineIndex;
            }
            if (previousLine.isEmpty() || previousLine.startsWith("@")) {
                continue;
            }
            return -1;
        }
        return -1;

    }

    private static boolean hasJavadocImmediatelyBeforeAnnotation(List<String> sourceLines, int annotationLineIndex) {

        int previousLineIndex = annotationLineIndex - 1;
        while (previousLineIndex >= 0 && sourceLines.get(previousLineIndex).trim().isEmpty()) {
            previousLineIndex--;
        }

        return previousLineIndex >= 0 && "*/".equals(sourceLines.get(previousLineIndex).trim());

    }

    private static String formatViolation(
            Path communityWorkspaceDirectory,
            Path javaSourcePath,
            int lineIndex,
            String sourceLine) {

        return communityWorkspaceDirectory.relativize(javaSourcePath).toString().replace('\\', '/')
                + ":"
                + (lineIndex + 1)
                + " -> "
                + sourceLine.trim();

    }

}
