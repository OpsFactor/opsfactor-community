package com.opsfactor.community;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guarda de legibilidade para injeções Spring no código Community.
 *
 * <p>O Community e o repositorio publico/source-available. Por isso, a
 * fronteira entre atributo de estado e dependencia Spring precisa ficar
 * visivel diretamente no source. Campos que representam beans devem declarar
 * {@code @Autowired} explicitamente, mesmo quando o Spring conseguiria injetar
 * por outro mecanismo.</p>
 */
class CommunityAutowiredFieldBoundaryTest {

    private static final List<String> COMMUNITY_MAIN_SOURCE_DIRECTORIES = List.of(
            "src/main/java"
    );

    private static final List<String> SPRING_LIFECYCLE_METHOD_ANNOTATIONS = List.of(
            "@PostConstruct",
            "@PreDestroy",
            "@Scheduled",
            "@EventListener",
            "@Async"
    );

    private static final List<String> SPRING_BOOTSTRAP_CONFIGURATION_CLASS_ANNOTATIONS = List.of(
            "@SpringBootApplication",
            "@Configuration",
            "@Component",
            "@Controller",
            "@RestController"
    );

    private static final List<String> SPRING_HTTP_HANDLER_METHOD_ANNOTATIONS = List.of(
            "@RequestMapping",
            "@GetMapping",
            "@PostMapping",
            "@PutMapping",
            "@DeleteMapping",
            "@PatchMapping"
    );

    private static final List<String> SPRING_CACHEABLE_METHOD_ANNOTATIONS = List.of(
            "@Cacheable"
    );

    private static final List<String> SPRING_CACHE_EVICT_METHOD_ANNOTATIONS = List.of(
            "@CacheEvict"
    );

    private static final Pattern COMMUNITY_DERIVED_DELETE_REPOSITORY_METHOD_PATTERN = Pattern.compile(
            "^(public\\s+)?(void|long|Long|int|Integer)\\s+(remove|delete)(All)?By\\w*\\s*\\(.*"
    );

    private static final List<String> SPRING_BEAN_FIELD_TYPE_TOKENS = List.of(
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

    private static final List<String> COMMUNITY_ALLOWED_OPTIONAL_AUTOWIRED_FIELD_KEYS = List.of(
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanOptimizationService",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanProcessChainService",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanExecutionProfileLocationScope",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanExecutionProfileMaterialScope",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanFirmProductionOrdersSpi",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanOpenOrdersHeuristicSpi",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanPresetConstraintGroupSpi",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/ConstrainedPlanService.java#supplyPlanExecutionProfileMaterialScope",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/HeuristicoService.java#supplyPlanExecutionProfileLocationPolicySpi",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/supplyplan/facade/SupplyPlanFacade.java#supplyPlanExecutionProfileMaterialScope"
    );

    @Test
    void communityBeanFieldsShouldUseExplicitAutowiredAnnotation() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * A regra e intencionalmente simples: apenas campos privados cujo tipo
         * parece colaborador Spring entram na verificacao. Entidades, DTOs,
         * value objects e constantes ficam fora do escopo por nao terem esses
         * sufixos funcionais.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            if (!isSpringInjectionBoundarySource(sourceLines)) {
                continue;
            }

            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (isCandidateSpringBeanField(sourceLine)
                        && !hasAutowiredAnnotationBeforeField(sourceLines, lineIndex)
                        && !hasExplicitAutowiredConstructorDependency(sourceLines, lineIndex)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Campos Community que parecem beans Spring devem declarar @Autowired explicitamente:\n"
                        + String.join("\n", violations));

    }

    void communityAutowiredBeanFieldsShouldBePrivate() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Dependencias Spring devem permanecer encapsuladas. Se uma classe
         * futura precisar expor um colaborador para extensao Enterprise, o
         * contrato deve ser um metodo protegido documentado, nao um campo
         * visivel.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            if (!isSpringInjectionBoundarySource(sourceLines)) {
                continue;
            }

            int autowiredLineIndex = -1;
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();

                if (trimmedLine.startsWith("@Autowired")) {
                    autowiredLineIndex = lineIndex;
                    continue;
                }

                if (autowiredLineIndex < 0) {
                    continue;
                }

                if (trimmedLine.isEmpty()
                        || trimmedLine.startsWith("//")
                        || trimmedLine.startsWith("@")) {
                    continue;
                }

                if (trimmedLine.endsWith(";")
                        && !trimmedLine.startsWith("private ")
                        && !trimmedLine.contains(" static ")) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLines.get(lineIndex)));
                }
                autowiredLineIndex = -1;
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Campos Community @Autowired devem ser private; exponha metodo protegido documentado quando houver extensao real:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityAutowiredBeanFieldsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * No repositorio aberto, cada dependencia Spring precisa explicar seu
         * papel localmente. A anotacao mostra que e bean; o Javadoc logo acima
         * mostra a fronteira funcional que aquele bean cobre.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            if (!isSpringInjectionBoundarySource(sourceLines)) {
                continue;
            }

            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (trimmedLine.startsWith("@Autowired")
                        && !hasJavadocImmediatelyBeforeAnnotation(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Campos Community @Autowired devem ter Javadoc imediatamente anterior:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityOptionalAutowiredShouldStayOnDocumentedRuntimeExtensionPoints() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Injecao opcional e uma borda de extensao, nao um atalho para esconder
         * dependencia. No Community atual, ela fica restrita aos SPIs de Supply
         * Planning que o Enterprise implementa no classpath privado.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            if (!isSpringInjectionBoundarySource(sourceLines)) {
                continue;
            }

            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (!isOptionalAutowiredAnnotation(sourceLine)) {
                    continue;
                }

                int fieldLineIndex = findFieldLineIndexAfterAutowired(sourceLines, lineIndex);
                if (fieldLineIndex < 0) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                    continue;
                }

                String optionalAutowiredFieldKey = formatAutowiredFieldKey(
                        communityWorkspaceDirectory,
                        javaSourcePath,
                        sourceLines.get(fieldLineIndex));
                if (!COMMUNITY_ALLOWED_OPTIONAL_AUTOWIRED_FIELD_KEYS.contains(optionalAutowiredFieldKey)) {
                    violations.add(optionalAutowiredFieldKey);
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve limitar @Autowired(required = false) aos pontos SPI documentados:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityValueInjectedFieldsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Campos @Value tambem entram pelo container Spring e representam
         * configuracao operacional. O Javadoc local explica o alcance da
         * propriedade sem obrigar o leitor a procurar application.properties.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (trimmedLine.startsWith("@Value")
                        && !hasJavadocImmediatelyBeforeAnnotation(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Campos Community @Value devem ter Javadoc imediatamente anterior:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityBeanMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Metodos @Bean tambem sao fronteiras runtime do Spring. O Javadoc
         * local torna claro qual objeto entra no contexto Community e evita que
         * configuracoes compactas escondam contrato operacional.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (trimmedLine.startsWith("@Bean")
                        && !hasJavadocImmediatelyBeforeAnnotation(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @Bean devem ter Javadoc imediatamente anterior:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityLifecycleMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Metodos lifecycle sao chamados pelo container ou pelo scheduler, nao
         * por fluxo Java explicito. O Javadoc local deixa visivel quando o
         * metodo roda e que contrato runtime ele prepara ou encerra.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringLifecycleMethodAnnotation(trimmedLine)
                        && !hasJavadocImmediatelyBeforeAnnotation(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community lifecycle devem ter Javadoc imediatamente anterior:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityServiceClassesShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Services concentram regras e fronteiras de persistencia/calculo. A
         * classe precisa explicar sua responsabilidade antes da anotacao Spring,
         * inclusive quando houver outras anotacoes no mesmo bloco.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (trimmedLine.startsWith("@Service")
                        && !hasJavadocBeforeClassAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Classes Community @Service devem ter Javadoc de classe antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityBootstrapConfigurationClassesShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Bootstraps, configurations, components e controllers estruturais
         * definem o contexto Spring e a superficie HTTP Community. A classe
         * precisa declarar a responsabilidade do bloco de anotacoes.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringBootstrapConfigurationClassAnnotation(trimmedLine)
                        && !hasJavadocBeforeClassAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Classes Community de bootstrap/configuracao Spring devem ter Javadoc de classe antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityRepositoryClassesShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories Community fazem parte do contrato aberto de persistencia.
         * O Javadoc de classe precisa explicar a entidade/snapshot que a
         * interface governa e, quando aplicavel, o motivo de fetch/cache.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (trimmedLine.startsWith("@Repository")
                        && !hasJavadocBeforeClassAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Repositories Community devem ter Javadoc de classe antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityHttpHandlerMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Handlers HTTP Community sao contratos publicos/source-available de
         * tela, data upload e operacao. O metodo precisa explicar sua
         * responsabilidade antes do bloco de anotacoes que publica a rota.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringHttpHandlerMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos HTTP Community devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityModifyingRepositoryMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Metodos @Modifying executam escrita direta por repository. A
         * responsabilidade da escrita precisa ficar visivel antes do bloco de
         * anotacoes, sem depender apenas do nome legado do metodo ou da JPQL.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (trimmedLine.startsWith("@Modifying")
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @Modifying devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityCacheableMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Metodos @Cacheable materializam fronteiras compartilhadas de leitura,
         * especialmente projections em memoria. O Javadoc local precisa
         * explicar o snapshot cacheado e a responsabilidade de invalida-lo.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheableMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @Cacheable devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityNonRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Fora de repositories, @CacheEvict aparece em services que conectam
         * uma operacao funcional a invalidacao de projection compartilhada. O
         * Javadoc precisa deixar visivel qual snapshot e descartado.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            if (javaSourcePath.getFileName().toString().endsWith("Repository.java")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict fora de repositories devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityUnitConversionRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories de unidade/conversao invalidam a projection de UOM e a
         * malha de suprimentos. Como essa invalidacao afeta conversoes usadas
         * em calculos, o metodo anotado precisa declarar seu snapshot.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedSourcePath = javaSourcePath.toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || !normalizedSourcePath.contains("/masterdata/unidadeconversao/")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories de unidade/conversao devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communitySupplyNetworkRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories de malha invalidam a projection de supply network. Como
         * essa fotografia alimenta rotas, linhas de transporte e versoes de
         * malha, o metodo anotado precisa declarar seu snapshot.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedSourcePath = javaSourcePath.toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || !normalizedSourcePath.contains("/masterdata/malha/")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories de malha devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMasterdataRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories raiz de masterdata material/location invalidam snapshots
         * centrais de parametros/cluster e supply network. Como esses cadastros
         * alimentam multiplas projections, o metodo anotado precisa declarar o
         * descarte de cache no proprio ponto de escrita.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedParentPath = javaSourcePath.getParent().toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || !normalizedParentPath.endsWith("/repository/masterdata")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories raiz de masterdata devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityClusterRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories de cluster material/location invalidam snapshots de
         * parametros/cluster e supply network. Como esses agrupamentos afetam
         * filtros e malha em lote, o metodo anotado precisa declarar seu
         * descarte de cache.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedSourcePath = javaSourcePath.toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || (!normalizedSourcePath.contains("/repository/cluster/location/")
                    && !normalizedSourcePath.contains("/repository/cluster/material/"))) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories de cluster devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityConfigurationRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories raiz de configuracao invalidam snapshots globais de
         * parametros, cluster, supply network e Demand Planning. Como essas
         * configuracoes governam projections centrais, o metodo anotado precisa
         * declarar o descarte no ponto de escrita.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedParentPath = javaSourcePath.getParent().toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || !normalizedParentPath.endsWith("/repository/configuration")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories raiz de configuracao devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityDemandPlanningConfigurationRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories de configuracao Demand Planning invalidam snapshots por
         * perfil de execucao. Como a chave do cache e especifica do perfil, o
         * metodo anotado precisa declarar qual fotografia de parametros descarta.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedSourcePath = javaSourcePath.toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || !normalizedSourcePath.contains("/configuration/planning/demand/")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories de configuracao Demand Planning devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityInventoryPolicyRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories de politica de estoques alteram parametros material-location
         * usados por projections de cluster e malha. O ponto de escrita precisa
         * declarar a invalidacao desses snapshots compartilhados.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedSourcePath = javaSourcePath.toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || !normalizedSourcePath.contains("/configuration/inventorypolicy/")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories de politica de estoques devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityProductClusterConfigurationRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories de configuracao de cluster de produtos alteram regras de
         * alocacao e status que alimentam snapshots de parametros/cluster e
         * malha. Cada escrita cacheada precisa declarar esse descarte.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedSourcePath = javaSourcePath.toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || !normalizedSourcePath.contains("/configuration/cluster/produto/")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories de configuracao de cluster de produtos devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityLocationClusterConfigurationRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories de configuracao de cluster de locations alteram regras e
         * parametros que alimentam snapshots de parametros/cluster e malha. Cada
         * escrita cacheada precisa declarar esse descarte.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedSourcePath = javaSourcePath.toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || !normalizedSourcePath.contains("/configuration/cluster/location/")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories de configuracao de cluster de locations devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityProductionMasterdataRepositoryCacheEvictMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Repositories de dados mestres de producao alimentam estrutura produtiva,
         * roteiros, recursos, listas tecnicas e versoes de producao usadas pela
         * malha. Cada escrita cacheada precisa declarar o descarte da supply
         * network materializada.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            String normalizedSourcePath = javaSourcePath.toString().replace('\\', '/');
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")
                    || !normalizedSourcePath.contains("/masterdata/producao/")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isSpringCacheEvictMethodAnnotation(trimmedLine)
                        && !hasJavadocBeforeAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos Community @CacheEvict de repositories de dados mestres de producao devem ter Javadoc antes do bloco de anotacoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityDerivedDeleteRepositoryMethodsShouldHaveLocalJavadoc() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Metodos derivados delete/removeBy tambem executam escrita direta pelo
         * Spring Data, mesmo sem JPQL explicita. Eles precisam explicar o
         * recorte removido antes da assinatura ou do bloco de anotacoes.
         */
        for (Path javaSourcePath : findCommunityMainJavaSources(communityWorkspaceDirectory)) {
            if (!javaSourcePath.getFileName().toString().endsWith("Repository.java")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String trimmedLine = sourceLines.get(lineIndex).trim();
                if (isCommunityDerivedDeleteRepositoryMethod(trimmedLine)
                        && !hasModifyingAnnotationNearMethod(sourceLines, lineIndex)
                        && !hasJavadocBeforeMethodOrContiguousAnnotationBlock(sourceLines, lineIndex)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLines.get(lineIndex)));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Metodos derivados delete/removeBy Community devem ter Javadoc antes da assinatura ou anotacoes:\n"
                        + String.join("\n", violations));

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

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

    private static boolean isSpringInjectionBoundarySource(List<String> sourceLines) {

        /*
         * Task, converters JPA e value objects podem receber colaboradores por
         * construtor ou criar helpers locais sem serem beans Spring. O teste
         * foca classes que de fato participam da injecao: componentes
         * anotados ou superclasses abstratas que ja usam @Autowired.
         */
        return sourceLines
                .stream()
                .map(String::trim)
                .anyMatch(line -> line.startsWith("@Autowired")
                        || line.startsWith("@Service")
                        || line.startsWith("@Component")
                        || line.startsWith("@RestController")
                        || line.startsWith("@Controller")
                        || line.startsWith("@Configuration")
                        || line.startsWith("@Repository")
                        || line.startsWith("@SpringBootApplication"));

    }

    private static boolean isCandidateSpringBeanField(String line) {

        String trimmedLine = line.trim();
        if (!trimmedLine.startsWith("private ")
                || !trimmedLine.endsWith(";")
                || trimmedLine.contains(" static ")) {
            return false;
        }

        String fieldType = extractPrivateFieldType(trimmedLine);
        return SPRING_BEAN_FIELD_TYPE_TOKENS.stream().anyMatch(fieldType::contains);

    }

    private static String extractPrivateFieldType(String trimmedLine) {

        String fieldDeclaration = trimmedLine
                .substring("private ".length(), trimmedLine.length() - 1)
                .trim();
        int assignmentIndex = fieldDeclaration.indexOf('=');
        if (assignmentIndex >= 0) {
            fieldDeclaration = fieldDeclaration.substring(0, assignmentIndex).trim();
        }

        String[] fieldDeclarationParts = fieldDeclaration.split("\\s+");
        int fieldTypeIndex = 0;
        while (fieldTypeIndex < fieldDeclarationParts.length
                && isFieldModifier(fieldDeclarationParts[fieldTypeIndex])) {
            fieldTypeIndex++;
        }
        return fieldTypeIndex >= fieldDeclarationParts.length ? "" : fieldDeclarationParts[fieldTypeIndex];

    }

    private static boolean isFieldModifier(String fieldDeclarationPart) {

        return "final".equals(fieldDeclarationPart)
                || "transient".equals(fieldDeclarationPart)
                || "volatile".equals(fieldDeclarationPart);

    }

    private static boolean hasAutowiredAnnotationBeforeField(List<String> sourceLines, int fieldLineIndex) {

        /*
         * Campos podem ter multiplas anotacoes, como @Autowired + @Qualifier.
         * Procuramos nas linhas imediatamente acima ate encontrar uma linha que
         * nao seja anotacao nem espaco em branco.
         */
        for (int previousLineIndex = fieldLineIndex - 1; previousLineIndex >= 0; previousLineIndex--) {
            String previousLine = sourceLines.get(previousLineIndex).trim();
            if (previousLine.startsWith("@Autowired")) {
                return true;
            }
            if (previousLine.isEmpty() || previousLine.startsWith("@")) {
                continue;
            }
            return false;
        }
        return false;

    }

    /**
     * Permite a forma equivalente de injeção por construtor quando o campo
     * final é recebido por um construtor anotado explicitamente. Assim a
     * guarda exige wiring visível sem induzir mutabilidade por field injection.
     */
    private static boolean hasExplicitAutowiredConstructorDependency(
            List<String> sourceLines,
            int fieldLineIndex) {

        String fieldType = extractPrivateFieldType(sourceLines.get(fieldLineIndex).trim());
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            if (!sourceLines.get(lineIndex).trim().startsWith("@Autowired")) {
                continue;
            }

            StringBuilder constructorSignature = new StringBuilder();
            for (int constructorLineIndex = lineIndex + 1;
                    constructorLineIndex < sourceLines.size();
                    constructorLineIndex++) {
                String constructorLine = sourceLines.get(constructorLineIndex).trim();
                if (constructorLine.isEmpty()) {
                    continue;
                }

                constructorSignature.append(' ').append(constructorLine);
                if (constructorLine.contains("{")) {
                    break;
                }
            }

            if (constructorSignature.toString().contains("(")
                    && constructorSignature.toString().contains(fieldType)) {
                return true;
            }
        }
        return false;

    }

    private static boolean hasJavadocImmediatelyBeforeAnnotation(
            List<String> sourceLines,
            int annotationLineIndex) {

        for (int previousLineIndex = annotationLineIndex - 1; previousLineIndex >= 0; previousLineIndex--) {
            String previousLine = sourceLines.get(previousLineIndex).trim();
            if (previousLine.isEmpty()) {
                continue;
            }
            return isJavadocClosingLine(previousLine);
        }
        return false;

    }

    private static boolean hasJavadocBeforeClassAnnotationBlock(
            List<String> sourceLines,
            int annotationLineIndex) {

        return hasJavadocBeforeAnnotationBlock(sourceLines, annotationLineIndex);

    }

    private static boolean hasJavadocBeforeAnnotationBlock(
            List<String> sourceLines,
            int annotationLineIndex) {

        for (int previousLineIndex = annotationLineIndex - 1; previousLineIndex >= 0; previousLineIndex--) {
            String previousLine = sourceLines.get(previousLineIndex).trim();
            if (previousLine.isEmpty() || isAnnotationBlockLine(previousLine)) {
                continue;
            }
            return isJavadocClosingLine(previousLine);
        }
        return false;

    }

    private static boolean isAnnotationBlockLine(String trimmedLine) {

        /*
         * Controllers e handlers podem usar blocos como @Tags({ ... }) ou
         * anotacoes adicionais antes da anotacao principal. Ao procurar o
         * Javadoc, o bloco inteiro de anotacoes deve ser ignorado.
         */
        return trimmedLine.startsWith("@")
                || trimmedLine.startsWith("})")
                || trimmedLine.startsWith(")")
                || trimmedLine.startsWith("}");

    }

    /** Aceita Javadoc de uma ou múltiplas linhas antes de elementos Spring. */
    private static boolean isJavadocClosingLine(String trimmedLine) {

        return "*/".equals(trimmedLine)
                || (trimmedLine.startsWith("/**") && trimmedLine.endsWith("*/"));

    }

    private static boolean isSpringLifecycleMethodAnnotation(String trimmedLine) {

        return SPRING_LIFECYCLE_METHOD_ANNOTATIONS.stream()
                .anyMatch(trimmedLine::startsWith);

    }

    private static boolean isSpringBootstrapConfigurationClassAnnotation(String trimmedLine) {

        return SPRING_BOOTSTRAP_CONFIGURATION_CLASS_ANNOTATIONS.stream()
                .anyMatch(trimmedLine::startsWith);

    }

    private static boolean isSpringHttpHandlerMethodAnnotation(String trimmedLine) {

        return SPRING_HTTP_HANDLER_METHOD_ANNOTATIONS.stream()
                .anyMatch(trimmedLine::startsWith);

    }

    private static boolean isSpringCacheableMethodAnnotation(String trimmedLine) {

        return SPRING_CACHEABLE_METHOD_ANNOTATIONS.stream()
                .anyMatch(trimmedLine::startsWith);

    }

    private static boolean isSpringCacheEvictMethodAnnotation(String trimmedLine) {

        return SPRING_CACHE_EVICT_METHOD_ANNOTATIONS.stream()
                .anyMatch(trimmedLine::startsWith);

    }

    private static boolean isCommunityDerivedDeleteRepositoryMethod(String trimmedLine) {

        return COMMUNITY_DERIVED_DELETE_REPOSITORY_METHOD_PATTERN.matcher(trimmedLine).matches();

    }

    private static boolean hasJavadocBeforeMethodOrContiguousAnnotationBlock(
            List<String> sourceLines,
            int methodLineIndex) {

        int candidateLineIndex = methodLineIndex - 1;
        while (candidateLineIndex >= 0
                && isAnnotationBlockLine(sourceLines.get(candidateLineIndex).trim())) {
            candidateLineIndex--;
        }
        return candidateLineIndex >= 0
                && isJavadocClosingLine(sourceLines.get(candidateLineIndex).trim());

    }

    private static boolean hasModifyingAnnotationNearMethod(
            List<String> sourceLines,
            int methodLineIndex) {

        /*
         * Metodos com @Modifying ja sao cobertos por guarda propria. Como
         * @Query pode ter multiplas linhas, a busca fica limitada ao bloco
         * imediatamente anterior onde essas anotacoes aparecem nos repositories.
         */
        int firstCandidateLineIndex = Math.max(0, methodLineIndex - 12);
        for (int lineIndex = methodLineIndex - 1; lineIndex >= firstCandidateLineIndex; lineIndex--) {
            if (sourceLines.get(lineIndex).trim().startsWith("@Modifying")) {
                return true;
            }
        }
        return false;

    }

    private static boolean isOptionalAutowiredAnnotation(String line) {

        String compactLine = line.replace(" ", "");
        return compactLine.contains("@Autowired(required=false)");

    }

    private static int findFieldLineIndexAfterAutowired(List<String> sourceLines, int autowiredLineIndex) {

        for (int lineIndex = autowiredLineIndex + 1; lineIndex < sourceLines.size(); lineIndex++) {
            String trimmedLine = sourceLines.get(lineIndex).trim();
            if (trimmedLine.isEmpty()
                    || trimmedLine.startsWith("//")
                    || trimmedLine.startsWith("@")) {
                continue;
            }
            return trimmedLine.endsWith(";") ? lineIndex : -1;
        }
        return -1;

    }

    private static String formatAutowiredFieldKey(
            Path communityWorkspaceDirectory,
            Path javaSourcePath,
            String fieldLine) {

        return communityWorkspaceDirectory.relativize(javaSourcePath).toString().replace('\\', '/')
                + "#"
                + extractFieldName(fieldLine.trim());

    }

    private static String extractFieldName(String trimmedFieldLine) {

        String fieldDeclaration = trimmedFieldLine.substring(0, trimmedFieldLine.length() - 1).trim();
        int assignmentIndex = fieldDeclaration.indexOf('=');
        if (assignmentIndex >= 0) {
            fieldDeclaration = fieldDeclaration.substring(0, assignmentIndex).trim();
        }

        String[] fieldDeclarationParts = fieldDeclaration.split("\\s+");
        return fieldDeclarationParts.length == 0 ? "" : fieldDeclarationParts[fieldDeclarationParts.length - 1];

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
