package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.platform.integration.service.IntegrationLoggingContext;
import com.opsfactor.community.platform.integration.service.IntegrationServiceComConfiguracoesInterface;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.web.configuration.UserRoleType;
import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.pivovarit.function.ThrowingSupplier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Framework usado para criação de endpoints de integração de dados por
 * controllers concretos: GET/POST nos formatos FILE/JSON.
 *
 * <p>No Community esta abstracao publica somente os controllers concretos que
 * existem no modulo aberto: master data operacional, producao operacional
 * basica para o heuristico, malha/transportation lane basica, sell-out
 * quantitativo e estoque inicial. Nao existe controller concreto de
 * upload/importacao de planning data; ajustes de Demand/Supply Plan continuam
 * restritos ao Planning Book material/location, e extrações detalhadas são
 * tratadas pelos controllers de planejamento, não por esta camada de carga de
 * dados.</p>
 *
 * @param <ENTITY>
 * @param <DATARECORDDTO>
 * @param <INTEGRATIONSERVICE>
 */
public abstract class IntegrationControllerAbstract<ENTITY, DATARECORDDTO extends IntegrationDataDtoAbstract<DATARECORDDTO, PRIMARYKEYDTO, ENTITY>, PRIMARYKEYDTO extends IntegrationPrimaryKeyDTOAbstract<PRIMARYKEYDTO, ENTITY>, DATAFILTER, INTEGRATIONSERVICE extends IntegrationServiceComConfiguracoesInterface<DATARECORDDTO, PRIMARYKEYDTO, ENTITY, ?, ?, DATAFILTER, OPTIONS>, OPTIONS extends IntegrationOptionsDto> {

    private static final Logger log = LoggerFactory.getLogger(IntegrationControllerAbstract.class);

    /**
     * Mapping Spring usado para registrar dinamicamente endpoints FILE/JSON das
     * subclasses concretas. A injecao explicita e necessaria porque esta classe
     * abstrata publica rotas no `@PostConstruct`.
     */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    /**
     * Executor web Community para comandos sincronizados.
     *
     * <p>No Community toda integracao de dados roda no proprio request. O
     * Enterprise pode oferecer fila/worker em outro overlay, mas este campo nao
     * deve apontar para scheduler async.</p>
     */
    @Autowired
    private WebControllerTaskSchedulingService webControllerTaskSchedulingService;

    /**
     * Service de autenticacao usado porque os endpoints sao registrados
     * dinamicamente e nao passam por anotacoes de seguranca nos metodos reais.
     */
    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Service concreto da subclasse de integracao. Cada controller especializado
     * injeta sua implementacao Community correspondente.
     */
    @Autowired
    private INTEGRATIONSERVICE integrationService;

    /**
     * Request atual usado apenas para logging opcional de integracao.
     */
    @Autowired
    private HttpServletRequest httpServletRequest;

    /**
     * Mapper JSON usado para logging opcional do corpo de requests JSON.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Habilita logs de ciclo de vida das cargas de integracao de dados.
     */
    @Value("${opsfactor.data-integration.logging.lifecycle-enabled:false}")
    protected boolean dataIntegrationLifecycleLoggingEnabled;

    /**
     * Habilita logging controlado do corpo recebido nos endpoints JSON.
     */
    @Value("${opsfactor.data-integration.logging.request-content-enabled:false}")
    protected boolean dataIntegrationRequestContentLoggingEnabled;

    /**
     * Habilita logging de erros ignorados pela camada generica de integracao.
     */
    @Value("${opsfactor.data-integration.logging.ignored-errors-enabled:false}")
    protected boolean dataIntegrationIgnoredErrorsLoggingEnabled;

    /**
     * Limite defensivo de caracteres gravados ao logar conteudo de request.
     */
    @Value("${opsfactor.data-integration.logging.request-content-max-chars:200000}")
    protected int dataIntegrationRequestContentMaxChars;

    /**
     * Paths FILE publicados pela abstracao. A existencia destes prefixos nao
     * implica uma API generica para qualquer dominio: somente subclasses
     * concretas registradas no Spring geram endpoints reais.
     *
     * <p>O runtime Community/Enterprise publica exclusivamente o namespace
     * canônico `api/secured/data/file/`; aliases históricos não podem voltar a
     * ser herdados por controllers concretos.</p>
     */
    public List<String> getRootFilePaths() {

        return List.of("api/secured/data/file/");

    }

    /**
     * Paths JSON publicados pela abstracao. Planning data nao possui subclass
     * Community e, portanto, nao e carregavel por estes prefixos.
     *
     * <p>O runtime Community/Enterprise publica exclusivamente o namespace
     * canônico `api/secured/data/`; aliases históricos não podem voltar a ser
     * herdados por controllers concretos.</p>
     */
    public List<String> getRootJsonPaths() {

        return List.of("api/secured/data/");

    }

    /**
     * Community possui seguranca operacional simples: qualquer endpoint de
     * integracao de dados depende apenas de usuario ADMIN. O Enterprise pode
     * sobrescrever estes metodos nos controllers especificos para recuperar a
     * granularidade de permissoes por dominio.
     */
    protected List<UserRoleType> getUserRoleTypesGet() {
        return List.of(UserRoleType.ROLE_ADMIN);
    }

    /**
     * Mesmo contrato de seguranca simples para operacoes mutaveis no
     * Community. Manter o metodo separado do GET preserva o ponto natural de
     * extensao Enterprise sem expor roles extras nesta edicao.
     */
    protected List<UserRoleType> getUserRoleTypesPost() {
        return List.of(UserRoleType.ROLE_ADMIN);
    }

    protected abstract String getSubPath();

    /**
     * Ponto estreito para subclasses Community que precisam publicar endpoints
     * legados especificos, mas ainda devem executar comandos mutaveis pelo
     * wrapper sincrono padrao. O campo real permanece privado para deixar claro
     * que o wiring Spring pertence a esta abstracao.
     */
    protected WebControllerTaskSchedulingService getWebControllerTaskSchedulingService() {

        return webControllerTaskSchedulingService;

    }

    public String getEntityClassName() {

        return resolveEntityClass().getSimpleName();

    }

    /**
     * Resolve a entidade principal declarada no primeiro parametro generico do
     * controller concreto.
     *
     * <p>O nome da entidade aparece em logs/mensagens genericas de integracao.
     * Como esta abstracao registra endpoints dinamicamente no bootstrap,
     * subclasses precisam declarar diretamente
     * {@code IntegrationControllerAbstract<Entity, ...>}. Falhar com mensagem
     * contextual aqui deixa claro qual controller saiu do contrato, em vez de
     * vazar um {@link ClassCastException} reflexivo.</p>
     */
    protected Class<?> resolveEntityClass() {

        Type integrationControllerGenericSuperclass = getClass().getGenericSuperclass();

        if (!(integrationControllerGenericSuperclass instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException(
                    "Data integration controller "
                            + getClass().getName()
                            + " must declare IntegrationControllerAbstract<Entity, ...> directly.");
        }

        Type entityClassType = parameterizedType.getActualTypeArguments()[0];
        if (!(entityClassType instanceof Class<?> entityClass)) {
            throw new IllegalStateException(
                    "Data integration controller "
                            + getClass().getName()
                            + " must declare a concrete entity class as the first generic parameter.");
        }

        return entityClass;

    }

    /**
     * Registra os endpoints dinamicos de arquivo, JSON e delete apos a criacao do bean Spring.
     */
    @PostConstruct
    public void configureMappings() {

        Method metodoGetFile = getMethod("getFile");
        Method metodoSaveFile = getMethod("saveFile", MultipartFile.class);
        Method metodoGetDTOList = getMethod("getDataRecordDtoList");
        Method metodoSaveDTOList = getMethod("saveIntegrationDto", IntegrationDto.class);
        Method metodoDeleteComDtosOuFiltro = getMethod("deleteDtoOuFiltro", IntegrationDto.class);

        for (String rootFilePath : getRootFilePaths()) {
            // Get File
            handlerMapping.registerMapping(
                    RequestMappingInfo.paths(rootFilePath + getSubPath()).methods(RequestMethod.GET).build(),
                    this, metodoGetFile);
            // Save File
            handlerMapping.registerMapping(
                    RequestMappingInfo.paths(rootFilePath + getSubPath()).methods(RequestMethod.POST).build(),
                    this, metodoSaveFile);
        }

        for (String rootJsonPath : getRootJsonPaths()) {
            // Get Json
            handlerMapping.registerMapping(
                    RequestMappingInfo.paths(rootJsonPath + getSubPath()).methods(RequestMethod.GET).build(),
                    this, metodoGetDTOList);
            // Save Json
            handlerMapping.registerMapping(
                    RequestMappingInfo.paths(rootJsonPath + getSubPath()).methods(RequestMethod.POST).build(),
                    this, metodoSaveDTOList);
            // Delete
            handlerMapping.registerMapping(
                    RequestMappingInfo.paths(rootJsonPath + getSubPath() + "/delete").methods(RequestMethod.POST, RequestMethod.DELETE).build(),
                    this, metodoDeleteComDtosOuFiltro);

        }

    }

    /**
     * Resolve o metodo que sera registrado dinamicamente no
     * {@link RequestMappingHandlerMapping}.
     *
     * <p>Como os controllers de integracao herdam uma superficie REST comum,
     * erro aqui significa erro de configuracao da propria classe concreta e deve
     * falhar no bootstrap/registro com nome do metodo ausente.</p>
     */
    protected Method getMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return this.getClass().getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Could not configure data integration request mapping for method " + methodName
                            + " in controller " + this.getClass().getName(),
                    e);
        }
    }

    public List<List<Object>> getFile() {
        validaAutorizacaoGetCommunity();
        return integrationService.getFullFileContents();
    }

    public List<List<Object>> getFileComFiltro(IntegrationDto<DATARECORDDTO,PRIMARYKEYDTO,DATAFILTER,OPTIONS> integrationDto) {
        validaIntegrationDtoPresenteCommunity(integrationDto);
        if (integrationDto.dataFilter == null) return getFile();
        validaAutorizacaoGetCommunity();
        return integrationService.getFilteredFileContents(integrationDto.dataFilter);
    }

    public List<DATARECORDDTO> getDataRecordDtoList() {
        validaAutorizacaoGetCommunity();
        return integrationService.getFullDTOList();
    }

    public List<DATARECORDDTO> getDataRecordDtoListComFiltro(IntegrationDto<DATARECORDDTO,PRIMARYKEYDTO,DATAFILTER,OPTIONS> integrationDto) {
        validaIntegrationDtoPresenteCommunity(integrationDto);
        if (integrationDto.dataFilter == null) return getDataRecordDtoList();
        validaAutorizacaoGetCommunity();
        return integrationService.getFilteredDTOList(integrationDto.dataFilter);
    }

    /**
     * Os GETs sao registrados dinamicamente a partir desta classe abstrata, por
     * isso a autorizacao precisa ser manual. No Community, qualquer falha deve
     * virar HTTP 401 explicito; retornar null mascararia falta de permissao como
     * resposta vazia.
     */
    private void validaAutorizacaoGetCommunity() {

        if (!authenticationService.currentUserHasAnyRole(getUserRoleTypeNamesCommunity(
                this.getUserRoleTypesGet(),
                "GET"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authorized to read data integration contents");
        }

    }

    public ResponseEntity<ResponseDTO> saveFile(@RequestParam("file") MultipartFile multipartFile) {
        // autorização feita manualmente pois @PreAuthorize só funciona quando não se trabalha com interfaces ou classes abstratas
        if (!authenticationService.currentUserHasAnyRole(getUserRoleTypeNamesCommunity(
                this.getUserRoleTypesPost(),
                "POST"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return webControllerTaskSchedulingService.runImediatoSync(
                buildLoggedSupplier(
                        ThrowingSupplier.unchecked(() -> integrationService.saveFile(multipartFile)),
                        "SAVE_FILE",
                        null,
                        getMultipartRequestContent(multipartFile)),
                "Save" + getEntityClassName() + "File");
    }

    public ResponseEntity<ResponseDTO> saveIntegrationDto(
            @RequestBody IntegrationDto<DATARECORDDTO, PRIMARYKEYDTO, DATAFILTER, OPTIONS> integrationDto) {
        // autorização feita manualmente pois @PreAuthorize só funciona quando não se trabalha com interfaces ou classes abstratas
        if (!authenticationService.currentUserHasAnyRole(getUserRoleTypeNamesCommunity(
                this.getUserRoleTypesPost(),
                "POST"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        validaThreadSyncCommunity(integrationDto);

        Supplier<String> loggedSupplier = buildLoggedSupplier(
                () -> integrationService.saveDTOList(integrationDto),
                "SAVE_JSON",
                integrationDto.data == null ? null : integrationDto.data.size(),
                getJsonRequestContent(integrationDto));

        return webControllerTaskSchedulingService.runImediatoSync(
                loggedSupplier,
                "Save" + getEntityClassName() + "Json");
    }

    public ResponseEntity<ResponseDTO> deleteDtoOuFiltro(
            @RequestBody IntegrationDto<DATARECORDDTO, PRIMARYKEYDTO, DATAFILTER, OPTIONS> integrationDto) {
        if (!authenticationService.currentUserHasAnyRole(getUserRoleTypeNamesCommunity(
                this.getUserRoleTypesPost(),
                "POST"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        validaThreadSyncCommunity(integrationDto);

        if (integrationDto.data != null && !integrationDto.data.isEmpty()) {
            Supplier<String> loggedSupplier = buildLoggedSupplier(
                    () -> {
                        integrationService.removeDtoList(
                            integrationDto.data
                                    .stream()
                                    .map(x -> x.primaryKeyDto)
                                    .collect(Collectors.toSet()));
                        return "Data successfully deleted";
                    },
                    "DELETE_RECORDS",
                    integrationDto.data.size(),
                    getJsonRequestContent(integrationDto));

            return webControllerTaskSchedulingService.runImediatoSync(
                    loggedSupplier,
                    "Delete" + getEntityClassName() + "Records");
        } else if (integrationDto.dataFilter != null) {
            Supplier<String> loggedSupplier = buildLoggedSupplier(
                    () -> {
                        integrationService.removeFilteredPersistedEntities(integrationDto.dataFilter);
                        return "Data successfully deleted";
                    },
                    "DELETE_FILTER",
                    null,
                    getJsonRequestContent(integrationDto));

            return webControllerTaskSchedulingService.runImediatoSync(
                    loggedSupplier,
                    "Delete" + getEntityClassName() + "Filter");
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Community nao tem fila/background worker para integracoes de dados.
     * O campo continua no DTO para compatibilidade do front compartilhado, mas
     * qualquer valor diferente de SYNC deve falhar antes de executar o service.
     */
    private void validaThreadSyncCommunity(IntegrationDto<DATARECORDDTO, PRIMARYKEYDTO, DATAFILTER, OPTIONS> integrationDto) {

        validaIntegrationDtoPresenteCommunity(integrationDto);
        if (integrationDto.threadSync != null && !integrationDto.threadSync.equals(IntegrationDto.ThreadSync.SYNC)) {
            throw new RequiresEnterpriseVersionException("Asynchronous data integration");
        }

    }

    /**
     * Valida a presenca do envelope generico de integracao.
     *
     * <p>Lista de dados vazia, filtro vazio e opcoes nulas mantem a semantica
     * historica de cada service concreto. O envelope inteiro nulo, entretanto,
     * nao tem significado funcional e deve falhar antes de `threadSync`,
     * logging de request ou acesso ao service de integracao.</p>
     */
    private void validaIntegrationDtoPresenteCommunity(
            IntegrationDto<DATARECORDDTO, PRIMARYKEYDTO, DATAFILTER, OPTIONS> integrationDto) {

        if (integrationDto == null) {
            throw new IllegalArgumentException("Data integration payload is required.");
        }

    }

    /**
     * Executa comandos especificos de uma subclasse, como APIs de desativacao.
     * O retorno e sempre o envelope padronizado `ResponseDTO`, pois o comando
     * mutavel Community deve concluir sincronicamente no proprio request.
     */
    public ResponseEntity<ResponseDTO> executaComandoPersonalizado(
            Consumer<INTEGRATIONSERVICE> consumerPersonalizadoIntegrationService,
            String descricaoAcao,
            String retornoQuandoSucesso) {
        // autorização feita manualmente pois @PreAuthorize só funciona quando não se trabalha com interfaces ou classes abstratas
        if (!authenticationService.currentUserHasAnyRole(getUserRoleTypeNamesCommunity(
                this.getUserRoleTypesPost(),
                "POST"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return webControllerTaskSchedulingService.runImediatoSync(
                () -> {
                    consumerPersonalizadoIntegrationService.accept(integrationService);
                    return retornoQuandoSucesso;
                },
                descricaoAcao);
    }

    /**
     * Converte as roles declaradas pela subclasse em nomes Spring Security.
     *
     * <p>Lista vazia e permitida e significa que nenhuma authority autoriza a
     * operacao. Lista nula ou item nulo indicam erro de contrato do controller
     * concreto/overlay e devem falhar antes de chamar o service de autenticacao,
     * para nao virar NPE durante registro dinamico de endpoints.</p>
     */
    private List<String> getUserRoleTypeNamesCommunity(
            List<UserRoleType> userRoleTypes,
            String operationName) {

        if (userRoleTypes == null) {
            throw new IllegalStateException(
                    "Data integration " + operationName + " role list is required.");
        }

        for (int indiceRole = 0; indiceRole < userRoleTypes.size(); indiceRole++) {
            if (userRoleTypes.get(indiceRole) == null) {
                throw new IllegalStateException(
                        "Data integration "
                                + operationName
                                + " role at index "
                                + indiceRole
                                + " is required.");
            }
        }

        return userRoleTypes.stream()
                .map(UserRoleType::name)
                .collect(Collectors.toList());

    }

    private Supplier<String> buildLoggedSupplier(
            Supplier<String> supplierExecucaoIntegracao,
            String operationName,
            Integer totalRecords,
            @Nullable String requestContent) {

        if (!dataIntegrationLifecycleLoggingEnabled
                && !dataIntegrationRequestContentLoggingEnabled
                && !dataIntegrationIgnoredErrorsLoggingEnabled) {
            return supplierExecucaoIntegracao;
        }

        IntegrationLoggingContext integrationLoggingContext = new IntegrationLoggingContext(
                dataIntegrationLifecycleLoggingEnabled,
                dataIntegrationIgnoredErrorsLoggingEnabled,
                getRequestPath(),
                httpServletRequest.getMethod(),
                getCurrentUsername(),
                getClientIpAddress(),
                getEntityClassName(),
                operationName,
                totalRecords);

        return () -> {
            IntegrationLoggingContext.setCurrent(integrationLoggingContext);
            integrationLoggingContext.markStarted();
            logIntegrationStart(integrationLoggingContext);
            logRequestContent(integrationLoggingContext, requestContent);

            try {
                String result = supplierExecucaoIntegracao.get();
                integrationLoggingContext.markSuccess();
                return result;
            } catch (RuntimeException | Error throwable) {
                /*
                 * O scheduler que executa Supplier tambem captura
                 * RuntimeException e Error para salvar Process Status. O
                 * logging de integracao precisa marcar ambos como FAILED antes
                 * de repassar a falha, senao erros graves apareceriam no log de
                 * ciclo de vida como RUNNING.
                 */
                integrationLoggingContext.markFailure(throwable);
                throw throwable;
            } finally {
                integrationLoggingContext.markFinished();
                logIntegrationEnd(integrationLoggingContext);
                IntegrationLoggingContext.clearCurrent();
            }
        };

    }

    private void logIntegrationStart(IntegrationLoggingContext integrationLoggingContext) {

        if (!integrationLoggingContext.isLifecycleLoggingEnabled()) {
            return;
        }

        log.info(
                "DATA_INTEGRATION_START timestamp={} api={} method={} user={} ip={} entity={} operation={} totalRecords={}",
                Instant.now(),
                integrationLoggingContext.getApiPath(),
                integrationLoggingContext.getHttpMethod(),
                integrationLoggingContext.getUsername(),
                integrationLoggingContext.getIpAddress(),
                integrationLoggingContext.getEntityClassName(),
                integrationLoggingContext.getOperationName(),
                integrationLoggingContext.getTotalRecords());

    }

    private void logIntegrationEnd(IntegrationLoggingContext integrationLoggingContext) {

        if (!integrationLoggingContext.isLifecycleLoggingEnabled()) {
            return;
        }

        log.info(
                "DATA_INTEGRATION_END timestamp={} api={} method={} user={} ip={} entity={} operation={} totalRecords={} savedRecords={} removedRecords={} ignoredRecords={} durationMs={} status={} failure={}",
                Instant.now(),
                integrationLoggingContext.getApiPath(),
                integrationLoggingContext.getHttpMethod(),
                integrationLoggingContext.getUsername(),
                integrationLoggingContext.getIpAddress(),
                integrationLoggingContext.getEntityClassName(),
                integrationLoggingContext.getOperationName(),
                integrationLoggingContext.getTotalRecords(),
                integrationLoggingContext.getSavedRecords(),
                integrationLoggingContext.getRemovedRecords(),
                integrationLoggingContext.getIgnoredRecords(),
                integrationLoggingContext.getDurationMs(),
                integrationLoggingContext.getStatus(),
                integrationLoggingContext.getFailureMessage());

    }

    private void logRequestContent(IntegrationLoggingContext integrationLoggingContext, @Nullable String requestContent) {

        if (!dataIntegrationRequestContentLoggingEnabled) {
            return;
        }

        log.info(
                "DATA_INTEGRATION_REQUEST_CONTENT timestamp={} api={} method={} user={} ip={} entity={} operation={} content={}",
                Instant.now(),
                integrationLoggingContext.getApiPath(),
                integrationLoggingContext.getHttpMethod(),
                integrationLoggingContext.getUsername(),
                integrationLoggingContext.getIpAddress(),
                integrationLoggingContext.getEntityClassName(),
                integrationLoggingContext.getOperationName(),
                requestContent);

    }

    /**
     * Serializa o payload JSON apenas quando o log de conteudo esta ligado.
     * Retorna {@code null} como contrato explicito de no-op para evitar montar
     * strings grandes quando a instalacao Community nao quer esse nivel de log.
     */
    @Nullable
    private String getJsonRequestContent(@Nullable Object requestBody) {

        if (!dataIntegrationRequestContentLoggingEnabled) {
            return null;
        }

        try {
            return limitRequestContent(objectMapper.writeValueAsString(requestBody));
        } catch (JsonProcessingException | RuntimeException exception) {
            return limitRequestContent(String.valueOf(requestBody));
        }

    }

    /**
     * Serializa metadados e conteudo do multipart somente quando configurado.
     * O retorno {@code null} indica que o wrapper deve registrar apenas o
     * ciclo de vida da integracao, sem copiar o arquivo para o log.
     */
    @Nullable
    private String getMultipartRequestContent(MultipartFile multipartFile) {

        if (!dataIntegrationRequestContentLoggingEnabled) {
            return null;
        }

        try {
            byte[] bytes = multipartFile.getBytes();
            String contentType = multipartFile.getContentType();
            String originalFilename = multipartFile.getOriginalFilename();
            boolean textFile = isTextFile(contentType, originalFilename);
            String content = textFile
                    ? new String(bytes, StandardCharsets.UTF_8)
                    : Base64.getEncoder().encodeToString(bytes);
            return limitRequestContent("filename=" + originalFilename
                    + "; contentType=" + contentType
                    + "; size=" + multipartFile.getSize()
                    + "; encoding=" + (textFile ? "UTF-8" : "BASE64")
                    + "; content=" + content);
        } catch (IOException | RuntimeException exception) {
            return "filename=" + multipartFile.getOriginalFilename()
                    + "; contentType=" + multipartFile.getContentType()
                    + "; size=" + multipartFile.getSize()
                    + "; content=<unavailable: " + exception.getMessage() + ">";
        }

    }

    private boolean isTextFile(String contentType, String originalFilename) {

        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase();
        String normalizedFilename = originalFilename == null ? "" : originalFilename.toLowerCase();
        return normalizedContentType.startsWith("text/")
                || normalizedContentType.contains("json")
                || normalizedContentType.contains("csv")
                || normalizedFilename.endsWith(".csv")
                || normalizedFilename.endsWith(".txt")
                || normalizedFilename.endsWith(".json");

    }

    @Nullable
    private String limitRequestContent(@Nullable String requestContent) {

        if (requestContent == null) {
            return null;
        }

        if (dataIntegrationRequestContentMaxChars <= 0 || requestContent.length() <= dataIntegrationRequestContentMaxChars) {
            return requestContent;
        }

        return requestContent.substring(0, dataIntegrationRequestContentMaxChars)
                + "...<truncated originalLength=" + requestContent.length() + ">";

    }

    private String getRequestPath() {

        String queryString = httpServletRequest.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return httpServletRequest.getRequestURI();
        }
        return httpServletRequest.getRequestURI() + "?" + queryString;

    }

    private String getClientIpAddress() {

        String forwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = httpServletRequest.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return httpServletRequest.getRemoteAddr();

    }

    private String getCurrentUsername() {

        try {
            Authentication authentication = authenticationService.getAuthentication();
            return authentication == null ? "anonymous" : authentication.getName();
        } catch (RuntimeException exception) {
            return "unknown";
        }

    }

}
