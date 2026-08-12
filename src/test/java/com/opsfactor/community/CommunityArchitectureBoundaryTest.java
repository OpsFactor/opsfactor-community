package com.opsfactor.community;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de fronteira arquitetural da distribuicao Community.
 *
 * <p>O repositorio Community pode documentar como o overlay Enterprise funciona,
 * mas nao pode depender funcionalmente de classes, pacotes, artefatos Maven ou
 * propriedades Enterprise. Esta classe transforma essa regra em uma verificacao
 * automatica simples, executada a partir do build raiz Community.</p>
 *
 * <p>A verificacao evita analisar texto livre de documentacao e comentarios para
 * nao bloquear Javadocs intencionais, como a explicacao do scan que permite ao
 * Enterprise registrar beans {@code @Primary}. O que deve falhar aqui e acoplamento
 * real: imports, packages, artefatos Maven ou configuracao runtime apontando para
 * {@code com.opsfactor.enterprise} ou {@code opsfactor-enterprise}.</p>
 */
class CommunityArchitectureBoundaryTest {

    private static final Pattern ENTERPRISE_IMPORT_OR_PACKAGE_PATTERN = Pattern.compile(
            "^\\s*(import\\s+(static\\s+)?|package\\s+)com\\.opsfactor\\.enterprise(\\.|\\s|;).*");

    private static final Pattern LEGACY_PLANNING_IMPORT_OR_PACKAGE_PATTERN = Pattern.compile(
            "^\\s*(import\\s+(static\\s+)?|package\\s+)com\\.opsfactor\\.planning(\\.|\\s|;).*");

    private static final String ENTERPRISE_FULLY_QUALIFIED_PACKAGE_REFERENCE =
            "com.opsfactor.enterprise.";

    private static final String LEGACY_PLANNING_FULLY_QUALIFIED_PACKAGE_REFERENCE =
            "com.opsfactor.planning.";

    /*
     * A politica lexical do scheduler e o unico ponto Community que pode
     * reconhecer um nome de classe Enterprise. Ela valida texto persistido
     * antes da reflexao, sem importar, carregar ou depender do artefato
     * Enterprise. A excecao e propositalmente textual, por arquivo e por
     * declaracao exata, para que qualquer outro FQCN Enterprise continue
     * bloqueado pela varredura abaixo.
     */
    private static final Map<String, String> COMMUNITY_ALLOWED_ENTERPRISE_FQCN_REFERENCE_BY_SOURCE_PATH = Map.of(
            "src/main/java/com/opsfactor/community/platform/scheduler/services/CanonicalScheduledTaskClassPolicy.java",
            "private static final String ENTERPRISE_PACKAGE_PREFIX = \"com.opsfactor.enterprise.\";");

    private static final Pattern COMMUNITY_ADMIN_SECURED_PATTERN = Pattern.compile(
            "^\\s*@Secured\\(\\s*(\"ROLE_ADMIN\"|CommunitySecurityConstants\\.COMMUNITY_ADMIN_ROLE)\\s*\\)\\s*$");

    /*
     * As tres anotacoes abaixo reproduzem contratos HTTP legados consumidos por
     * clientes existentes. Elas sao metadados de compatibilidade nos controllers
     * compartilhados, nao uma matriz de authorities do Community: o login
     * Community continua materializando exclusivamente ROLE_ADMIN.
     *
     * A allowlist e deliberadamente textual, por arquivo e por anotacao exata.
     * Assim, uma role funcional nao pode vazar para service, security, outro
     * controller ou mesmo outro metodo destes controllers sem uma revisao
     * explicita desta fronteira.
     */
    private static final Map<String, String> COMMUNITY_LEGACY_EXECUTION_SECURED_ANNOTATION_BY_CONTROLLER_PATH = Map.of(
            "src/main/java/com/opsfactor/community/web/restcontroller/planning/DemandPlanningRestController.java",
            "@Secured({\"ROLE_ADMIN\", \"ROLE_DEMAND_PLANNING_EXECUTION\"})",
            "src/main/java/com/opsfactor/community/web/restcontroller/planning/SupplyPlanningController.java",
            "@Secured({\"ROLE_ADMIN\", \"ROLE_SUPPLY_PLANNING_EXECUTION\"})",
            "src/main/java/com/opsfactor/community/web/restcontroller/ProcessStatusController.java",
            "@Secured({\"ROLE_ADMIN\", \"ROLE_DEMAND_PLANNING_EXECUTION\", \"ROLE_SUPPLY_PLANNING_EXECUTION\"})");

    private static final Map<String, String> COMMUNITY_LEGACY_EXECUTION_ENDPOINT_BY_CONTROLLER_PATH = Map.of(
            "src/main/java/com/opsfactor/community/web/restcontroller/planning/DemandPlanningRestController.java",
            "api/secured/planning/demand/generate",
            "src/main/java/com/opsfactor/community/web/restcontroller/planning/SupplyPlanningController.java",
            "api/secured/planning/supply/execute",
            "src/main/java/com/opsfactor/community/web/restcontroller/ProcessStatusController.java",
            "api/secured/scheduler/status");

    private static final List<String> COMMUNITY_GLOBAL_AUTHENTICATED_ENDPOINT_PATH_TOKENS = List.of(
            "api/secured/alerts/uomconversiongaps/snp/",
            "api/secured/alerts/uomconversiongaps/dp/",
            "api/secured/alerts/uomconversiongaps/deployment/",
            "api/secured/bi/planning/supply/materialflows/",
            "api/secured/supplynetwork/dependencies"
    );

    private static final Pattern COMMUNITY_INTEGRATION_ADMIN_GET_ROLE_PATTERN = Pattern.compile(
            "protected\\s+List<UserRoleType>\\s+getUserRoleTypesGet\\s*\\(\\s*\\)\\s*\\{\\s*"
                    + "return\\s+List\\.of\\s*\\(\\s*UserRoleType\\.ROLE_ADMIN\\s*\\)\\s*;\\s*"
                    + "\\}",
            Pattern.DOTALL);

    private static final Pattern COMMUNITY_INTEGRATION_ADMIN_POST_ROLE_PATTERN = Pattern.compile(
            "protected\\s+List<UserRoleType>\\s+getUserRoleTypesPost\\s*\\(\\s*\\)\\s*\\{\\s*"
                    + "return\\s+List\\.of\\s*\\(\\s*UserRoleType\\.ROLE_ADMIN\\s*\\)\\s*;\\s*"
                    + "\\}",
            Pattern.DOTALL);

    private static final Pattern SPRING_SECURITY_ROLE_PATTERN = Pattern.compile("ROLE_[A-Z0-9_]+");

    private static final Pattern COMMUNITY_FORBIDDEN_RUNNER_PATTERN = Pattern.compile(
            ".*(import\\s+org\\.springframework\\.boot\\.(ApplicationRunner|CommandLineRunner)|"
                    + "implements\\s+.*(ApplicationRunner|CommandLineRunner)|"
                    + "\\b(ApplicationRunner|CommandLineRunner)\\s+\\w+\\s*\\().*");

    private static final Pattern COMMUNITY_FORBIDDEN_ENTERPRISE_ENDPOINT_PATTERN = Pattern.compile(
            ".*\"[^\"]*/(pricing|distribution|visibility|agent|gis|map|ai|autofit|auto-fit|"
                    + "finance|warehouse|fleet|vehicle|campaign|event|sellin|sell-in|salesorders|sales-orders|"
                    + "selloutorder|salesordersdeliveries|order|ordersdeliveries|loadingorders|productionorder|"
                    + "budget|presetconstraint|directdemand|productionplan|workingday|icms|releaseddemand|"
                    + "temporalsplitcurve|distributionplan|consolidatedloadingorders|writeoffprojection|greenfield|"
                    + "productionresource/availableshiftsbyproductionresource|"
                    + "productionresource/weekdaysandholidaysbyshift|"
                    + "costtoserve|cost-to-serve|optimizer|optimization|line-scheduling|linescheduling|"
                    + "processchain|process-chain|constraint-tracker|constrainttracker|"
                    + "inventorypolicyoptimization|inventory-policy-optimization|inventoryoptimization|"
                    + "inventory-optimization|demandaccuracy|demand-accuracy|changelog|change-log)(/|\\{|\"|$).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COMMUNITY_FORBIDDEN_PRINT_STACK_TRACE_PATTERN = Pattern.compile(
            ".*\\.printStackTrace\\s*\\(.*");

    private static final Pattern COMMUNITY_FORBIDDEN_STDOUT_STDERR_PATTERN = Pattern.compile(
            ".*System\\.(out|err)\\.print(ln)?\\s*\\(.*");

    private static final Pattern COMMUNITY_FORBIDDEN_EMPTY_RUNTIME_EXCEPTION_PATTERN = Pattern.compile(
            ".*new\\s+(IllegalArgumentException|UnsupportedOperationException)\\s*\\(\\s*\\).*");

    private static final Pattern COMMUNITY_FORBIDDEN_FATAL_ERROR_PATTERN = Pattern.compile(
            ".*throw\\s+new\\s+Error\\s*\\(.*");

    private static final Pattern COMMUNITY_WEB_RESPONSE_ENTITY_WILDCARD_PATTERN = Pattern.compile(
            ".*ResponseEntity\\s*<\\s*\\?\\s*>.*");

    private static final Pattern COMMUNITY_PLANNING_FILE_UPLOAD_IMPORT_PATTERN = Pattern.compile(
            "^\\s*import\\s+org\\.springframework\\.web\\.(multipart\\.MultipartFile|bind\\.annotation\\.RequestPart)\\s*;.*");

    private static final Pattern COMMUNITY_PLANNING_UPLOAD_MAPPING_PATTERN = Pattern.compile(
            ".*@(Get|Post|Put|Delete|Patch|Request)Mapping.*(upload|import).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COMMUNITY_PLANNING_DEMAND_GENERATE_FROM_FILE_MAPPING_PATTERN = Pattern.compile(
            ".*@(Get|Post|Put|Delete|Patch|Request)Mapping.*planning/demand/generate/fromfile.*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COMMUNITY_LEGACY_DEMAND_AGGREGATION_FIELD_PATTERN = Pattern.compile(
            ".*\\b(productAggregationLevel|locationAggregationLevel)\\b.*");

    private static final String COMMUNITY_LEGACY_CLEANSED_HISTORICAL_SALES_FIELD =
            "cleansedHistoricalSales";

    private static final String COMMUNITY_MODEL_DESCRIPTION_METHOD_TOKEN =
            "getDescricaoModeloEstatistico(";

    private static final List<String> COMMUNITY_FORBIDDEN_DEMAND_PLAN_LINE_ENTERPRISE_TOKENS = List.of(
            "quantidadeCarteira",
            "quantidadeCarteiraRestrita",
            "getQuantidadeCarteiraAtendida",
            "atualizaDemandaComRebalanceamentoPlano",
            "getQuantidadeDeKeyFigure",
            "setQuantidadeDeKeyFigure"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_GENERIC_IMPLEMENTATION_MARKERS = List.of(
            "TODO",
            "FIXME",
            "Unsupported enum value",
            "No implementation for",
            "Not implemented",
            "IMPLEMENTACAO ATUAL",
            "IMPLEMENTACAO FUTURA",
            "IMPLEMENTAÇÃO ATUAL",
            "IMPLEMENTAÇÃO FUTURA"
    );

    /**
     * O Community nao deve ter {@code catch (Exception)} produtivo. A borda
     * final de {@link Task#run()} captura explicitamente desserializacao JSON,
     * runtime funcional e {@link Error}; controllers, Demand/Supply Planning,
     * scheduler web, clustering e logs auxiliares de data upload tambem
     * capturam tipos especificos. A lista fica vazia de proposito para impedir
     * regressao para capturas amplas.
     */
    private static final Map<String, Integer> COMMUNITY_ALLOWED_GENERIC_EXCEPTION_CATCH_COUNTS = Map.of();

    private static final List<String> COMMUNITY_FORBIDDEN_LEGACY_SELLOUT_PROJECTION_API_TOKENS = List.of(
            "getDFUsComSellout",
            "getUltimoPeriodoComSellout",
            "getPrimeiroPeriodoComSellout",
            "getMateriaisComSellout",
            "getLocationsComSellout",
            "addSelloutAgregado",
            "getSetSellout",
            "getQuantidadeSellout"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_PRODUCT_NAMED_SALES_PROJECTION_TOKENS = List.of(
            "SalesProjectionProduto",
            "SalesProjectionLocationProduto",
            "getFirstLastSalesProjectionLocationProduto"
    );

    private static final String COMMUNITY_LEGACY_SELLOUT_PROJECTION_PACKAGE_TOKEN =
            "historicaldata.selloutprojection";

    private static final String COMMUNITY_PARALLEL_ROUTING_RUNTIME_SWITCH_TOKEN =
            "modificaTodosOutputsVersaoProducaoParalela";

    private static final String COMMUNITY_CONFIG_IMPORT = "classpath:application-community-defaults.properties";

    private static final List<String> ENTERPRISE_MAVEN_OR_PROPERTY_TOKENS = List.of(
            "com.opsfactor.enterprise",
            "opsfactor-enterprise"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_BUILD_OR_PROPERTY_TOKENS = List.of(
            "mysql",
            "oracle",
            "or-tools",
            "ortools",
            "optaplanner",
            "pricing",
            "finance",
            "langchain",
            "openai",
            "stl-decomp",
            "stl4j",
            "catch22",
            "dhtmlx",
            "amazon",
            "amazonaws",
            "azure",
            "aws",
            "sqs",
            "sns",
            "servicebus",
            "queue-util",
            "oauth",
            "openid",
            "oidc",
            "saml",
            "keycloak",
            "okta",
            "jjwt",
            "jwt"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_PRIVATE_RUNTIME_DEPENDENCY_COORDINATES = List.of(
            "com.azure:",
            "com.azure.spring:",
            "com.microsoft.azure:",
            "com.google.ortools:",
            "com.github.servicenow:stl-decomp-4j",
            "org.python:jython",
            "com.opsfactor.enterprise:"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_LEGACY_EDITION_NAMING_TOKENS = List.of(
            "pre" + "mium",
            "open " + "core",
            "open-" + "core",
            "open " + "source",
            "open-" + "source",
            "open" + "source",
            "opsfactor-" + "pro",
            "opsfactor-" + "core",
            "requires" + "pro" + "versionexception",
            "pro " + "edition",
            "core " + "edition",
            "pro/" + "pre" + "mium",
            "core/" + "pro",
            "planning" + "edition",
            "planning" + "capability",
            "pro" + "version"
    );

    private static final Pattern COMMUNITY_FORBIDDEN_COMMERCIAL_EDITION_TERM_PATTERN = Pattern.compile(
            ".*\\b(pa" + "g(a|o|as|os)|pa" + "id)\\b.*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COMMUNITY_FORBIDDEN_ACTIVE_SECRET_PLACEHOLDER_PATTERN = Pattern.compile(
            "^\\s*([a-z0-9._-]+\\.)?(password|secret|token|client[-_]?secret)\\s*=\\s*"
                    + "(password|sa|admin|root|change-me)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> COMMUNITY_FORBIDDEN_CUSTOMER_OR_PRIVATE_HOST_TOKENS = List.of(
            "samsonite",
            "whirlpool"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_DATA_SQL_ENTERPRISE_TOKENS = List.of(
            "sellin",
            "pedido",
            "sales_order",
            "salesorder",
            "campanha",
            "campaign",
            "custo",
            "preco",
            "pricing",
            "finance",
            "fleet",
            "vehicle",
            "warehouse",
            "optimizer",
            "optimization",
            "gis",
            "baricentro",
            "latitude",
            "longitude",
            "costtoserve",
            "cost_to_serve"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_ROOT_MODULE_DIRECTORIES = List.of(
            "ai",
            "gis",
            "inventory-optimization",
            "planning-batch",
            "planning-finance",
            "planning-optimizer",
            "pricing",
            "queue-util"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_GIS_MAP_CLASS_NAME_TOKENS = List.of(
            "GIS",
            "Gis",
            "Geo",
            "Mapa",
            "Baricentro",
            "GraphHopper"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_DEMAND_ENTERPRISE_CLASS_TOKENS = List.of(
            "AgrupamentoCaracteristicasECampanha",
            "CampanhaDTO",
            "CampanhaEmLocationProjection",
            "CampanhaFrontService",
            "CampanhaIntegrationService",
            "CampanhaLinha",
            "CampanhaProjection",
            "CampanhaProjectionFactory",
            "CampanhaTipo",
            "ChronosPythonForecaster",
            "DemandPlanAutofitModel",
            "DemandPlanAutofitModelProjection",
            "DemandPlanAutofitModelProjectionFactory",
            "DemandPlanAutoFitModelFrontService",
            "DemandPlanningAutofit",
            "DemandPlanningAutofitService",
            "DemandPlanningAutofitTask",
            "DemandPlanningChronos",
            "ListaPrecosDemandPlan",
            "ListaPrecosDemandPlanService",
            "ParametrosModeloEstatisticoClusterAutofit",
            "ParametrosModeloEstatisticoNodeArvoreBinariaAutofit",
            "OrderItemRepository",
            "ResultadoAutofit",
            "ResultadoClusterAutofit",
            "ResultadoModeloAutofit",
            "ResultadoNodeArvoreBinariaAutofit",
            "SalesProjectionAgrupamentoCaracteristicasECampanhaData",
            "SucessaoLocationIntegrationService",
            "SucessaoProduto",
            "SucessaoProdutoIntegrationService",
            "SucessaoProdutoProjection",
            "SucessaoProdutoProjectionFactory"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_DEMAND_SOURCE_TOKENS = List.of(
            "lagMapeDias",
            "getLagMapeDias",
            "DP_PADRAO_DIAS_LAG_MAPE",
            "HISTORICAL_SALESPACKAGE"
    );

    private static final List<String> COMMUNITY_ALLOWED_DATA_UPLOAD_CONTROLLER_FILES = List.of(
            "ConversaoUnidadeIntegrationController.java",
            "ConversaoUnidadeProdutoIntegrationController.java",
            "DisponibilidadeRecursoProdutivoIntegrationController.java",
            "EstoqueIntegrationController.java",
            "FulfilledDemandIntegrationController.java",
            "IntegrationControllerAbstract.java",
            "InventoryPolicyDetailIntegrationController.java",
            "InventoryPolicyIntegrationController.java",
            "InventoryPlanIntegrationController.java",
            "LinhaTransporteIntegrationController.java",
            "LinhaTransporteMaterialIntegrationController.java",
            "ListaTecnicaComponenteIntegrationController.java",
            "ListaTecnicaIntegrationController.java",
            "LocationIntegrationController.java",
            "MaterialIntegrationController.java",
            "OperacaoRoteiroIntegrationController.java",
            "ParametrosMaterialLocationIntegrationController.java",
            "RecursoProdutivoIntegrationController.java",
            "RoteiroIntegrationController.java",
            "SelloutIntegrationController.java",
            "UnidadeMedidaIntegrationController.java",
            "VersaoMalhaIntegrationController.java",
            "VersaoProducaoSimplesIntegrationController.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_MASTERDATA_CONTROLLER_FILES = List.of(
            "ClusteringRestController.java",
            "LinhaTransporteController.java",
            "LocationRestController.java",
            "MaterialRestController.java",
            "ProductionRestController.java",
            "UnidadeMedidaRestController.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_CONFIGURATION_CONTROLLER_FILES = List.of(
            "ParametroMaterialLocationController.java",
            "ParametrosGlobaisController.java",
            "PerfilExecucaoDemandPlanController.java",
            "PerfilExecucaoSupplyPlanController.java",
            "PoliticaEstoquesController.java",
            "UserConfigurationController.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_PLANNING_CONTROLLER_FILES = List.of(
            "ConstrainedPlanController.java",
            "DemandAnalysisRestController.java",
            "DemandPlanningRestController.java",
            "DeploymentOperationalController.java",
            "ProductionPlanningBookController.java",
            "SupplyPlanningController.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_ADMIN_CONTROLLER_FILES = List.of(
            "AdminRestController.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_RUNTIME_CONTROLLER_FILES = List.of(
            "RuntimeInfoController.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_ROOT_REST_CONTROLLER_FILES = List.of(
            "ProcessStatusController.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_OPEN_ENDPOINT_PATH_TOKENS = List.of(
            "api/open/createdefaultuser",
            "api/open/applicationappearance",
            "api/open/runtime-info"
    );

    private static final List<String> COMMUNITY_ALLOWED_INTEGRATION_SERVICE_FILES = List.of(
            "ConversaoUnidadeIntegrationService.java",
            "ConversaoUnidadeProdutoIntegrationService.java",
            "DisponibilidadeRecursoProdutivoIntegrationService.java",
            "EstoqueIntegrationService.java",
            "FulfilledDemandIntegrationService.java",
            "InventoryPlanIntegrationService.java",
            "LinhaTransporteIntegrationService.java",
            "LinhaTransporteProdutoIntegrationService.java",
            "ListaTecnicaComponenteIntegrationService.java",
            "ListaTecnicaIntegrationService.java",
            "LocationIntegrationService.java",
            "OperacaoRoteiroIntegrationService.java",
            "ParametrosMaterialLocationIntegrationService.java",
            "PoliticaEstoquesIntegrationService.java",
            "PoliticaEstoquesMaterialLocationIntegrationService.java",
            "ProdutoIntegrationService.java",
            "RecursoProdutivoIntegrationService.java",
            "RoteiroIntegrationService.java",
            "SelloutIntegrationService.java",
            "UnidadeMedidaIntegrationService.java",
            "VersaoMalhaIntegrationService.java",
            "VersaoProducaoSimplesIntegrationService.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_INTEGRATION_MAPPER_FILES = List.of(
            "ConversaoUnidadeIntegrationMapper.java",
            "ConversaoUnidadeProdutoIntegrationMapper.java",
            "DisponibilidadeRecursoProdutivoIntegrationMapper.java",
            "EstoqueIntegrationMapper.java",
            "InventoryPlanIntegrationMapper.java",
            "LinhaTransporteIntegrationMapper.java",
            "LinhaTransporteProdutoIntegrationMapper.java",
            "ListaTecnicaComponenteIntegrationMapper.java",
            "ListaTecnicaIntegrationMapper.java",
            "LocationIntegrationMapper.java",
            "ParametrosMaterialLocationIntegrationMapper.java",
            "PoliticaEstoquesIntegrationMapper.java",
            "PoliticaEstoquesMaterialLocationIntegrationMapper.java",
            "ProdutoIntegrationMapper.java",
            "RecursoProdutivoIntegrationMapper.java",
            "RoteiroIntegrationMapper.java",
            "SelloutIntegrationMapper.java",
            "VersaoMalhaIntegrationMapper.java",
            "VersaoProducaoSimplesIntegrationMapper.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_INTEGRATION_DTO_FILES = List.of(
            "ConversaoUnidadeIntegrationDataDto.java",
            "ConversaoUnidadeProdutoIntegrationDataDto.java",
            "DisponibilidadeRecursoProdutivoIntegrationDataDto.java",
            "EstoqueIntegrationDataDto.java",
            "EstoqueIntegrationFiltroDto.java",
            "FulfilledDemandIntegrationDataDto.java",
            "IntegrationDataDtoAbstract.java",
            "IntegrationDto.java",
            "IntegrationOptionsDto.java",
            "IntegrationPrimaryKeyDTOAbstract.java",
            "IntegrationTextNormalization.java",
            "InventoryPlanIntegrationDataDto.java",
            "LinhaTransporteIntegrationDataDto.java",
            "LinhaTransporteProdutoIntegrationDataDto.java",
            "LinhaTransporteProdutoIntegrationOptionsDto.java",
            "ListaTecnicaComponenteIntegrationDataDto.java",
            "ListaTecnicaIntegrationDataDto.java",
            "LocationIntegrationDataDto.java",
            "LocationIntegrationFiltroDto.java",
            "ParametrosMaterialLocationIntegrationDataDto.java",
            "PoliticaEstoquesIntegrationDataDto.java",
            "PoliticaEstoquesMaterialLocationIntegrationDataDto.java",
            "ProdutoIntegrationDataDto.java",
            "RecursoProdutivoIntegrationDataDto.java",
            "RoteiroIntegrationDataDto.java",
            "SelloutIntegrationDataDto.java",
            "SelloutIntegrationFiltroDto.java",
            "UnidadeMedidaDataUploadDTO.java",
            "VersaoMalhaIntegrationDataDto.java",
            "VersaoProducaoSimplesIntegrationDataDto.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_MODEL_PROJECTION_FILES = List.of(
            "configuration/parametros/ClusterEParametrosProjection.java",
            "configuration/parametros/ClusterEParametrosProjectionFactory.java",
            "configuration/planning/demand/aggregation/ParametrosDemandPlanNivelClusterProjection.java",
            "configuration/planning/demand/aggregation/ParametrosDemandPlanNivelClusterProjectionSimples.java",
            "configuration/planning/demand/factory/ParametrosDemandPlanningProjectionFactory.java",
            "configuration/planning/demand/forecast/ParametrosAgregacaoForecast.java",
            "configuration/planning/demand/forecast/ParametrosArima.java",
            "configuration/planning/demand/forecast/ParametrosChronos.java",
            "configuration/planning/demand/forecast/ParametrosExponentialSmoothing.java",
            "configuration/planning/demand/forecast/ForecastInternalRegressorParameters.java",
            "configuration/planning/demand/forecast/ParametrosHoltWinters.java",
            "configuration/planning/demand/forecast/ParametrosLimpezaHistoricoForecast.java",
            "configuration/planning/demand/forecast/ParametrosMediaMovel.java",
            "configuration/planning/demand/forecast/ParametrosProphet.java",
            "configuration/planning/demand/ParametrosDemandPlanProjection.java",
            "configuration/planning/demand/ParametrosForecastProjection.java",
            "configuration/planning/demand/ParametrosGeraisDemandPlanningProjection.java",
            "configuration/user/ConfiguredViewProjection.java",
            "configuration/user/ConfiguredViewProjectionFactory.java",
            "dfu/dfu/DFU.java",
            "dfu/dfu/FiltroDFUProjection.java",
            "dfu/location/LocationProjection.java",
            "dfu/location/LocationProjectionCompleto.java",
            "dfu/location/LocationProjectionFactory.java",
            "dfu/material/MaterialProjection.java",
            "dfu/material/MaterialProjectionCompleto.java",
            "dfu/material/MaterialProjectionFactory.java",
            "historicaldata/aggregatedtransactionaldata/AggregatedByLocationMaterialUOM.java",
            "historicaldata/aggregatedtransactionaldata/AggregatedByLocationMaterialUOMDate.java",
            "historicaldata/aggregatedtransactionaldata/AggregatedByLocationMaterialUOMDatePlanType.java",
            "historicaldata/aggregatedtransactionaldata/AggregatedByMaterialUOM.java",
            "historicaldata/aggregatedtransactionaldata/AggregatedByMaterialUOMDate.java",
            "historicaldata/aggregatedtransactionaldata/AggregatedDataInterface.java",
            "historicaldata/aggregatedtransactionaldata/impl/AggregatedByLocationMaterialUOMDateImpl.java",
            "historicaldata/aggregatedtransactionaldata/impl/AggregatedByLocationMaterialUOMImpl.java",
            "historicaldata/aggregatedtransactionaldata/impl/AggregatedByMaterialUOMDateImpl.java",
            "historicaldata/aggregatedtransactionaldata/impl/AggregatedByMaterialUOMImpl.java",
            "historicaldata/estoqueprojection/EstoqueProjectionAbstract.java",
            "historicaldata/estoqueprojection/EstoqueProjectionLocationProduto.java",
            "historicaldata/estoqueprojection/EstoqueProjectionLocationProdutoData.java",
            "historicaldata/estoqueprojection/EstoqueProjectionProduto.java",
            "historicaldata/factory/EstoqueProjectionFactory.java",
            "historicaldata/factory/SalesProjectionFactory.java",
            "historicaldata/firstlasttransactionaldata/FirstLastByLocation.java",
            "historicaldata/firstlasttransactionaldata/FirstLastByMaterial.java",
            "historicaldata/firstlasttransactionaldata/FirstLastByMaterialLocation.java",
            "historicaldata/salesprojection/FirstLastSalesProjection.java",
            "historicaldata/salesprojection/LocationMaterialCorrelationScope.java",
            "historicaldata/salesprojection/SalesProjectionAbstract.java",
            "historicaldata/salesprojection/SalesProjectionLocationMaterial.java",
            "historicaldata/salesprojection/SalesProjectionLocationMaterialData.java",
            "historicaldata/salesprojection/SalesProjectionMaterial.java",
            "historicaldata/salesprojection/SalesProjectionMaterialData.java",
            "inventorypolicy/PoliticaEstoquesProjection.java",
            "inventorypolicy/PoliticaEstoquesProjectionFactory.java",
            "masterdata/malha/SupplyNetworkProjection.java",
            "masterdata/malha/SupplyNetworkProjectionFactory.java",
            "masterdata/production/capacity/BIProjectionCapacidadeProdutiva.java",
            "masterdata/production/capacity/BIProjectionCapacidadeProdutivaFactory.java",
            "masterdata/unidadeconversao/UnidadeMedidaProjection.java",
            "masterdata/unidadeconversao/UnidadeMedidaProjectionFactory.java",
            "planningdata/AjusteCelulaPlanningBook.java",
            "planningdata/ChavePlanningProjection.java",
            "planningdata/demand/DemandPlanningProjection.java",
            "planningdata/demand/forecast/DemandPlanForecastProjection.java",
            "planningdata/demand/forecast/DemandPlanForecastProjectionAgregado.java",
            "planningdata/demand/forecast/DemandPlanForecastProjectionFactory.java",
            "planningdata/demand/forecast/DemandPlanForecastProjectionMaterialLocation.java",
            "planningdata/factory/DemandPlanProjectionFactory.java",
            "planningdata/keyfigure/dfudata/DFUDataKeyFigureAbstract.java",
            "planningdata/keyfigure/dfudata/DFUDataKeyFigureCoberturaEstoque.java",
            "planningdata/keyfigure/dfudata/DFUDataKeyFigurePadrao.java",
            "planningdata/keyfigure/dfudata/DFUDataKeyFigureRelacaoEntreValores.java",
            "planningdata/keyfigure/EditMode.java",
            "planningdata/keyfigure/KeyFigureInterface.java",
            "planningdata/keyfigure/KeyFigureStandard.java",
            "planningdata/keyfigure/KeyFigureStandardEnum.java",
            "planningdata/keyfigure/KeyFigureStandardMonetariaDemandPlanning.java",
            "planningdata/keyfigure/KeyFigureStandardSupplyPlanning.java",
            "planningdata/PlanningBookDfuScope.java",
            "planningdata/supply/DemandaDiretaConsideradaProjection.java",
            "planningdata/supply/DemandaDiretaConsideradaProjectionFactory.java",
            "planningdata/supply/DistributionPlanItemBiProjection.java",
            "planningdata/supply/InventoryPlanLinhaBiProjection.java",
            "planningdata/supply/ProductionPlanLinhaBiProjection.java",
            "planningdata/supply/SafetyStockMultiplasLocationsProjection.java",
            "planningdata/supply/SafetyStockProjection.java",
            "planningdata/supply/SupplyPlanBiProjectionFactory.java",
            "planningdata/supply/SupplyPlanningMultiplasLocationsProjection.java",
            "planningdata/supply/SupplyPlanningBiProjection.java",
            "planningdata/supply/SupplyPlanningProjection.java",
            "planningdata/supply/SupplyPlanProjectionFactory.java",
            "planningdata/temporalsplit/SplitTemporalProjection.java",
            "planningdata/temporalsplit/SplitTemporalProjectionCurva.java",
            "planningdata/temporalsplit/SplitTemporalProjectionCurvaFlat.java",
            "planningdata/temporalsplit/SplitTemporalProjectionFactory.java",
            "planningdata/temporalsplit/SplitTemporalProjectionPorDfu.java",
            "historicaldata/projection/factory/HistoricalSalesSource.java",
            "historicaldata/projection/factory/SelloutHistoricalSalesSource.java",
            "planningbook/keyfigure/projection/KeyFigureProjection.java",
            "planningbook/keyfigure/projection/KeyFigureProjectionFactory.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_MODEL_REPOSITORY_FILES = List.of(
            "cluster/location/ClusterLocationsRepository.java",
            "cluster/material/ClusterProdutosDemandPlanningRepository.java",
            "cluster/material/ClusterProdutosRepository.java",
            "configuration/cluster/location/ParametrosClusterLocationsRepository.java",
            "configuration/cluster/location/RegraAlocacaoClusterLocationsPaisEstadoRepository.java",
            "configuration/cluster/location/RegraAlocacaoClusterLocationsRepository.java",
            "configuration/cluster/location/RegraAlocacaoClusterLocationsTipoLocationRepository.java",
            "configuration/cluster/produto/RegraAlocacaoClusterProdutosRepository.java",
            "configuration/cluster/produto/RegraAlocacaoClusterProdutosStatusRepository.java",
            "configuration/inventorypolicy/PoliticaEstoquesMaterialLocationRepository.java",
            "configuration/inventorypolicy/PoliticaEstoquesRepository.java",
            "configuration/ParametrosGlobaisRepository.java",
            "configuration/ParametrosProdutoLocationRepository.java",
            "configuration/planning/demand/ParametrosDemandPlanNivelClusterRepository.java",
            "configuration/planning/demand/PerfilExecucaoDemandPlanRepository.java",
            "configuration/planning/supply/PerfilExecucaoPoliticaEstoquesRepository.java",
            "configuration/planning/supply/PerfilExecucaoSupplyPlanRepository.java",
            "configuration/user/ConfiguracaoUsuarioRepository.java",
            "configuration/user/ConfiguredViewRepository.java",
            "configuration/user/ConfiguredViewKeyFigureRepository.java",
            "historicaldata/EstoqueRepository.java",
            "historicaldata/SelloutRepository.java",
            "masterdata/LocationRepository.java",
            "masterdata/economicgroup/EconomicGroupRepository.java",
            "masterdata/malha/LinhaTransporteProdutoRepository.java",
            "masterdata/malha/LinhaTransporteRepository.java",
            "masterdata/malha/VersaoMalhaRepository.java",
            "masterdata/producao/DisponibilidadeRecursoProdutivoRepository.java",
            "masterdata/producao/ListaTecnicaComponenteRepository.java",
            "masterdata/producao/ListaTecnicaRepository.java",
            "masterdata/producao/OperacaoRoteiroRepository.java",
            "masterdata/producao/RecursoProdutivoRepository.java",
            "masterdata/producao/RoteiroRepository.java",
            "masterdata/producao/VersaoProducaoInexistenteRepository.java",
            "masterdata/producao/VersaoProducaoSimplesRepository.java",
            "masterdata/ProdutoRepository.java",
            "masterdata/unidadeconversao/ConversaoUnidadeProdutoRepository.java",
            "masterdata/unidadeconversao/ConversaoUnidadeRepository.java",
            "masterdata/unidadeconversao/UnidadeMedidaRepository.java",
            "planningdata/demandplanning/DemandPlanItemRepository.java",
            "planningdata/demandplanning/DemandPlanRepository.java",
            "planningdata/demandplanning/HistoricoDemandPlanItemRepository.java",
            "planningdata/supplyplanning/CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository.java",
            "supplyplanning/repository/DemandaDiretaConsideradaLinhaDAO.java",
            "planningdata/supplyplanning/DemandaDiretaConsideradaLinhaRepository.java",
            "planningdata/supplyplanning/DistributionPlanLegacyBaselineRequirement.java",
            "planningdata/supplyplanning/DistributionPlanItemRepository.java",
            "planningdata/supplyplanning/InventoryPlanLegacyBaselineRequirement.java",
            "planningdata/supplyplanning/InventoryPlanLinhaRepository.java",
            "planningdata/supplyplanning/ProductionPlanLegacyBaselineRequirement.java",
            "planningdata/supplyplanning/ProductionPlanLinhaRepository.java",
            "planningdata/supplyplanning/SupplyPlanRepository.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_MODEL_DOMAIN_FILES = List.of(
            "cluster/location/ClusterLocations.java",
            "cluster/location/RegraAlocacaoClusterLocations.java",
            "cluster/location/RegraAlocacaoClusterLocationsPaisEstado.java",
            "cluster/location/RegraAlocacaoClusterLocationsTipoLocation.java",
            "cluster/produto/ClusterProdutos.java",
            "cluster/produto/ClusterProdutosDemandPlanning.java",
            "cluster/produto/RegraAlocacaoClusterProdutos.java",
            "cluster/produto/RegraAlocacaoClusterProdutosStatus.java",
            "configuration/cluster/location/ParametrosClusterLocations.java",
            "configuration/ParametrosGlobais.java",
            "configuration/ParametrosProdutoLocation.java",
            "configuration/planning/demand/ParametrosDemandPlanNivelCluster.java",
            "configuration/planning/demand/ParametrosModeloEstatisticoAbstract.java",
            "configuration/planning/demand/PerfilExecucaoDemandPlan.java",
            "configuration/planning/supply/PerfilExecucaoPoliticaEstoques.java",
            "configuration/planning/supply/PerfilExecucaoSupplyPlan.java",
            "configuration/planning/supply/optimizer/presetconstraint/RestricaoPredefinidaGrupo.java",
            "configuration/user/ConfiguracaoUsuario.java",
            "configuration/user/ConfiguredView.java",
            "configuration/user/ConfiguredViewKeyFigure.java",
            "historicaldata/Estoque.java",
            "historicaldata/Sellout.java",
            "inventorypolicy/PoliticaEstoques.java",
            "inventorypolicy/PoliticaEstoquesMaterialLocation.java",
            "masterdata/caracteristica/CaracteristicaInterface.java",
            "masterdata/caracteristica/location/CaracteristicaLocationId.java",
            "masterdata/caracteristica/location/CaracteristicaLocationInterface.java",
            "masterdata/caracteristica/material/CaracteristicaProdutoId.java",
            "masterdata/caracteristica/material/CaracteristicaProdutoInterface.java",
            "masterdata/economicgroup/EconomicGroup.java",
            "masterdata/location/Location.java",
            "masterdata/location/LocationAbstract.java",
            "masterdata/malha/LinhaTransporte.java",
            "masterdata/malha/LinhaTransporteProduto.java",
            "masterdata/malha/VersaoMalha.java",
            "masterdata/production/DisponibilidadeRecursoProdutivo.java",
            "masterdata/production/ListaTecnica.java",
            "masterdata/production/ListaTecnicaComponente.java",
            "masterdata/production/OperacaoAbstract.java",
            "masterdata/production/OperacaoRoteiro.java",
            "masterdata/production/RecursoProdutivo.java",
            "masterdata/production/Roteiro.java",
            "masterdata/production/VersaoProducao.java",
            "masterdata/production/VersaoProducaoInexistente.java",
            "masterdata/production/VersaoProducaoParalela.java",
            "masterdata/production/VersaoProducaoParalelaComponente.java",
            "masterdata/production/VersaoProducaoSimples.java",
            "masterdata/produto/Produto.java",
            "masterdata/unidadeconversao/ConversaoUnidade.java",
            "masterdata/unidadeconversao/ConversaoUnidadeProduto.java",
            "masterdata/unidadeconversao/UnitOfMeasureConversionLegacyRatioState.java",
            "masterdata/unidadeconversao/UnidadeMedida.java",
            "planningdata/demandplanning/DemandPlan.java",
            "planningdata/demandplanning/DemandPlanItem.java",
            "planningdata/demandplanning/HistoricoDemandPlanItem.java",
            "planningdata/supplyplanning/CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan.java",
            "planningdata/supplyplanning/DemandaDiretaConsideradaLinha.java",
            "planningdata/supplyplanning/DistributionPlanItem.java",
            "planningdata/supplyplanning/InventoryPlanLinha.java",
            "planningdata/supplyplanning/ProductionPlanLinha.java",
            "planningdata/supplyplanning/SupplyPlan.java",
            "demandplanning/planningbook/domain/DemandPlanningPlanningBookCatalog.java",
            "planningbook/domain/AjusteCelulaPlanningBook.java",
            "planningbook/domain/PlanningBookDfuScope.java",
            "planningbook/keyfigure/domain/dfudata/DFUDataKeyFigureAbstract.java",
            "planningbook/keyfigure/domain/dfudata/DFUDataKeyFigureCoberturaEstoque.java",
            "planningbook/keyfigure/domain/dfudata/DFUDataKeyFigurePadrao.java",
            "planningbook/keyfigure/domain/dfudata/DFUDataKeyFigureRelacaoEntreValores.java",
            "planningbook/keyfigure/domain/EditMode.java",
            "planningbook/keyfigure/domain/KeyFigureInterface.java",
            "planningbook/keyfigure/domain/KeyFigureStandard.java",
            "planningbook/keyfigure/domain/KeyFigureStandardEnum.java",
            "planningbook/keyfigure/domain/KeyFigureStandardMonetariaDemandPlanning.java",
            "planningbook/keyfigure/domain/KeyFigureStandardSupplyPlanning.java",
            "supplyplanning/domain/SupplyPlanningDataContract.java",
            "supplyplanning/planningbook/domain/SupplyPlanningPlanningBookCatalog.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_SERVICE_OR_TASK_FILES = List.of(
            "bi/CommunityDemandSalesOverviewService.java",
            "bi/CommunityInventoryOverviewService.java",
            "bi/CommunityMaterialFlowsService.java",
            "bi/CommunityProductionOverviewResourceDetailService.java",
            "bi/CommunityProductionOverviewService.java",
            "cluster/front/ClusteringFrontService.java",
            "configuration/ConfiguredViewService.java",
            "configuration/front/ConfiguracaoUsuarioFrontService.java",
            "configuration/front/ConfiguredViewFrontService.java",
            "configuration/front/ApplicationAppearanceFrontService.java",
            "configuration/front/ParametrosGlobaisFrontService.java",
            "configuration/front/PerfilExecucaoDemandPlanFrontService.java",
            "configuration/front/PerfilExecucaoSupplyPlanFrontService.java",
            "configuration/front/PoliticaEstoquesFrontService.java",
            "historicaldata/EstoqueService.java",
            "historicaldata/front/SelloutFrontService.java",
            "historicaldata/SalesService.java",
            "integration/configuration/ParametrosMaterialLocationIntegrationService.java",
            "integration/historicaldata/EstoqueIntegrationService.java",
            "integration/historicaldata/SelloutIntegrationService.java",
            "integration/inventorypolicy/PoliticaEstoquesIntegrationService.java",
            "integration/inventorypolicy/PoliticaEstoquesMaterialLocationIntegrationService.java",
            "integration/planning/supply/InventoryPlanIntegrationService.java",
            "integration/planning/supply/FulfilledDemandIntegrationService.java",
            "integration/masterdata/LocationIntegrationService.java",
            "integration/masterdata/malha/LinhaTransporteIntegrationService.java",
            "integration/masterdata/malha/LinhaTransporteProdutoIntegrationService.java",
            "integration/masterdata/malha/VersaoMalhaIntegrationService.java",
            "integration/masterdata/producao/DisponibilidadeRecursoProdutivoIntegrationService.java",
            "integration/masterdata/producao/ListaTecnicaComponenteIntegrationService.java",
            "integration/masterdata/producao/ListaTecnicaIntegrationService.java",
            "integration/masterdata/producao/OperacaoRoteiroIntegrationService.java",
            "integration/masterdata/producao/RecursoProdutivoIntegrationService.java",
            "integration/masterdata/producao/RoteiroIntegrationService.java",
            "integration/masterdata/producao/VersaoProducaoSimplesIntegrationService.java",
            "integration/masterdata/ProdutoIntegrationService.java",
            "integration/masterdata/unidademedida/ConversaoUnidadeIntegrationService.java",
            "integration/masterdata/unidademedida/ConversaoUnidadeProdutoIntegrationService.java",
            "integration/masterdata/unidademedida/UnidadeMedidaIntegrationService.java",
            "masterdata/front/malha/LinhaTransporteFrontService.java",
            "masterdata/front/producao/ListaTecnicaFrontService.java",
            "masterdata/front/producao/RecursoProdutivoFrontService.java",
            "masterdata/front/producao/RoteiroFrontService.java",
            "masterdata/front/UnidadeMedidaFrontService.java",
            "planning/demand/DemandPlanningService.java",
            "planning/demand/forecast/DemandForecastWorkflowService.java",
            "planning/front/demandplanning/DemandPlanningFrontService.java",
            "planning/front/demandplanning/DemandSimulationFrontService.java",
            "planning/front/ParametrosFrontService.java",
            "planning/front/planningbook/PlanningBookExcelExportService.java",
            "planning/front/planningbook/PlanningBookService.java",
            "planning/front/supplyplanning/LowLevelCodeFrontService.java",
            "planning/front/supplyplanning/DeploymentOperationalFrontService.java",
            "planning/front/supplyplanning/ProductionPlanningBookFrontService.java",
            "planning/front/supplyplanning/SupplyPlanFrontService.java",
            "planning/supply/heuristic/ConstrainedPlanService.java",
            "planning/supply/heuristic/HeuristicoService.java",
            "planning/supply/heuristic/NivelamentoCapacidadePlanoIrrestritoHeuristicoService.java",
            "planning/supply/persistence/CapacidadeEfetivaSupplyPlanService.java",
            "planning/supply/persistence/SupplyPlanningModificacoesService.java",
            "planning/supply/SupplyPlanService.java",
            "runtime/CommunityRuntimeInfoService.java",
            "runtime/RuntimeInfoService.java",
            "supplynetwork/SupplyNetworkDependencyExplorerService.java",
            "task/planning/DeleteDemandPlanTask.java",
            "task/planning/DeleteSupplyPlanTask.java",
            "task/planning/DemandPlanningTask.java",
            "task/planning/SupplyPlanningTask.java",
            "cluster/service/ClusteringService.java",
            "configuration/service/ParametrosGlobaisService.java",
            "masterdata/service/ClusterLocationDtoService.java",
            "masterdata/service/ClusterLocationService.java",
            "masterdata/service/LocationDtoService.java",
            "masterdata/service/LocationService.java",
            "masterdata/service/MaterialDtoService.java",
            "masterdata/service/MaterialService.java",
            "masterdata/service/UnidadeMedidaService.java",
            "masterdata/service/VersaoProducaoService.java",
            "planningbook/keyfigure/service/KeyFigureService.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_SERVICE_DTO_FILES = List.of(
            "bi/dto/CommunityDemandSalesOverviewDTO.java",
            "bi/dto/CommunityDemandSalesOverviewPeriodDTO.java",
            "bi/dto/CommunityDemandSalesOverviewSelectionDTO.java",
            "bi/dto/CommunityDemandPlanSalesReportRowDTO.java",
            "bi/dto/CommunityInventoryOverviewDTO.java",
            "bi/dto/CommunityInventoryOverviewMaterialLocationDetailDTO.java",
            "bi/dto/CommunityInventoryOverviewPeriodDTO.java",
            "bi/dto/CommunityInventoryOverviewSelectionDTO.java",
            "bi/dto/CommunityMaterialCharacteristicGroupingDTO.java",
            "bi/dto/CommunityMaterialFlowsDTO.java",
            "bi/dto/CommunityMaterialFlowsLocationAndColorDTO.java",
            "bi/dto/CommunityProductionOverviewDTO.java",
            "bi/dto/CommunityProductionOverviewResourceDetailDTO.java",
            "bi/dto/CommunityProductionOverviewResourceDetailResponseDTO.java",
            "bi/dto/CommunityProductionOverviewResourceDetailSelectionDTO.java",
            "bi/dto/CommunityProductionOverviewSelectionDTO.java",
            "configuration/front/dto/ConfiguredViewCaracteristicaDTO.java",
            "configuration/front/dto/ConfiguredViewDTO.java",
            "configuration/front/dto/ConfiguredViewKeyFigureDTO.java",
            "configuration/front/dto/ConfiguredViewSelectionDTO.java",
            "configuration/front/dto/PoliticaEstoquesDTO.java",
            "dto/configuration/ParametroClusterLocationDTO.java",
            "dto/configuration/ParametrosMaterialDTO.java",
            "dto/configuration/ParametrosMaterialLocationDTO.java",
            "dto/EstruturaHierarquicaDTO.java",
            "dto/login/UserDTO.java",
            "dto/masterdata/ClusterRuleDTO.java",
            "dto/masterdata/EstoqueDTO.java",
            "dto/masterdata/malha/VersaoMalhaDTO.java",
            "dto/planning/demandanalysis/SimulatedDemandPlanDTO.java",
            "dto/planning/demandanalysis/SimulatedDemandPlanMaterialLocationDTO.java",
            "masterdata/front/dto/producao/ListaTecnicaComponenteDTO.java",
            "masterdata/front/dto/producao/ListaTecnicaDTO.java",
            "masterdata/front/dto/producao/OperacaoRoteiroDTO.java",
            "masterdata/front/dto/producao/RoteiroDTO.java",
            "masterdata/front/dto/UnidadeConversaoFaltanteDTO.java",
            "masterdata/facade/dto/FiltroMaterialLocationDeCombinacaoCaracteristicasDTO.java",
            "masterdata/front/producao/DTO/InconsistenciaReceitaProducaoDTO.java",
            "masterdata/front/producao/DTO/RecursoProdutivoDTO.java",
            "planning/front/demandplanning/dto/DemandPlanDTO.java",
            "planning/front/demandplanning/dto/DemandPlanItemDTO.java",
            "planning/front/demandplanning/dto/DemandPlanPeriodDTO.java",
            "planning/front/demandplanning/dto/DemandPlanSelectDTO.java",
            "planning/front/demandplanning/dto/VersaoDemandPlanDTO.java",
            "planning/front/dto/DFUDTO.java",
            "planning/front/supplyplanning/dto/SupplyPlanDTO.java",
            "planning/front/supplyplanning/dto/SupplyPlanPeriodDTO.java",
            "planning/front/supplyplanning/dto/SupplyPlanSelectDTO.java",
            "planning/front/supplyplanning/dto/VersaoSupplyPlanDTO.java",
            "planning/front/supplyplanning/dto/deployment/DeploymentOperationalInboundUpdateDTO.java",
            "planning/front/supplyplanning/dto/deployment/DeploymentOperationalLineDTO.java",
            "planning/front/supplyplanning/dto/productionplanning/ProductionPlanningBookDTO.java",
            "planning/front/supplyplanning/dto/productionplanning/ProductionPlanningBookUpdateDTO.java",
            "planning/front/supplyplanning/dto/productionplanning/ProductionPlanningMaterialDTO.java",
            "planning/front/supplyplanning/dto/productionplanning/ProductionPlanningResourceDTO.java",
            "runtime/RuntimeInfoDTO.java",
            "supplynetwork/dto/BillOfMaterialsDependencyDTO.java",
            "supplynetwork/dto/MaterialLocationDependencyDTO.java",
            "supplynetwork/dto/ProductionResourceDependencyDTO.java",
            "supplynetwork/dto/ProductionVersionDependencyDTO.java",
            "supplynetwork/dto/RoutingBomCombinationDependencyDTO.java",
            "supplynetwork/dto/RoutingDependencyDTO.java",
            "supplynetwork/dto/SupplyNetworkDependencyDTO.java",
            "supplynetwork/dto/TransportationLineDependencyDTO.java",
            "cluster/web/dto/allocation/AlocacaoClusterLocationDTO.java",
            "cluster/web/dto/allocation/AlocacaoClusterMaterialDTO.java",
            "cluster/web/dto/ClusterLocationsDTO.java",
            "cluster/web/dto/ClusterProdutosDTO.java",
            "cluster/web/dto/RegraAlocaoClusterLocationsCaracteristicaDTO.java",
            "cluster/web/dto/RegraAlocaoClusterLocationsDTO.java",
            "cluster/web/dto/RegraAlocaoClusterLocationsPaisEstadoDTO.java",
            "cluster/web/dto/RegraAlocaoClusterLocationsTipoLocationDTO.java",
            "cluster/web/dto/RegraAlocaoClusterProdutosDTO.java",
            "configuration/user/web/dto/ConfiguracaoUsuarioDTO.java",
            "configuration/user/web/dto/UserInterfacePreferencesDTO.java",
            "configuration/web/dto/application/ApplicationAppearanceDTO.java",
            "demandplanning/configuration/web/dto/DemandPlanningClusterLevelConfigurationDTO.java",
            "demandplanning/configuration/web/dto/DemandPlanningForecastParametersDTO.java",
            "demandplanning/configuration/web/dto/DemandPlanningGeneralParametersDTO.java",
            "demandplanning/configuration/web/dto/DemandPlanningPreviaForecastRequestDTO.java",
            "demandplanning/configuration/web/dto/PerfilExecucaoDemandPlanDTO.java",
            "historicaldata/bi/dto/SelloutReportDTO.java",
            "historicaldata/bi/dto/SelloutReportParametrosDTO.java",
            "historicaldata/web/dto/FirstAndLastDateDTO.java",
            "lowlevelcode/web/dto/DFUMalhaCircularDTO.java",
            "lowlevelcode/web/dto/LowLevelCodeDTO.java",
            "lowlevelcode/web/dto/LowLevelCodeEdgeDTO.java",
            "lowlevelcode/web/dto/LowLevelCodeNodeDTO.java",
            "masterdata/web/dto/characteristic/CaracteristicaLocationDTO.java",
            "masterdata/web/dto/characteristic/CaracteristicaProdutoDTO.java",
            "masterdata/web/dto/characteristic/CaracteristicaProdutoLocationDTO.java",
            "masterdata/web/dto/characteristic/TipoCaracteristicaDTO.java",
            "masterdata/web/dto/LocationDTO.java",
            "masterdata/web/dto/ProdutoDTO.java",
            "masterdata/web/dto/unidademedida/ConversaoUnidadeMedidaDTO.java",
            "planningbook/web/dto/CellDetailsDTO.java",
            "planningbook/web/dto/ColumnDefDTO.java",
            "planningbook/web/dto/GroupDTO.java",
            "planningbook/web/dto/PlanningBookDTO.java",
            "planningbook/web/dto/PlanningBookParentSelectionDTO.java",
            "planningbook/web/dto/SelectedPlanningBookCellDTO.java",
            "supplyplanning/configuration/web/dto/PerfilExecucaoSupplyPlanDTO.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_SERVICE_MAPPER_FILES = List.of(
            "mapper/configuration/ConfiguredViewAutoMapper.java",
            "mapper/masterdata/malha/VersaoMalhaAutoMapper.java",
            "mapper/masterdata/producao/ListaTecnicaAutoMapper.java",
            "mapper/masterdata/producao/ListaTecnicaComponenteAutoMapper.java",
            "mapper/masterdata/producao/OperacaoRoteiroAutoMapper.java",
            "mapper/masterdata/producao/RecursoProdutivoAutoMapper.java",
            "mapper/masterdata/producao/RoteiroAutoMapper.java",
            "mapper/planning/DemandAnalysisMapper.java",
            "mapper/planning/DemandPlanAutoMapper.java",
            "mapper/planning/DemandPlanItemAutoMapper.java",
            "mapper/planning/DFUAutoMapper.java",
            "mapper/planning/SupplyPlanAutoMapper.java",
            "cluster/web/mapper/ClusterLocationsMapper.java",
            "cluster/web/mapper/ClusterProdutosMapper.java",
            "configuration/user/web/mapper/ConfiguracaoUsuarioAutoMapper.java",
            "demandplanning/configuration/web/mapper/DemandPlanningConfigurationMapper.java",
            "demandplanning/configuration/web/mapper/PerfilExecucaoDemandPlanAutoMapper.java",
            "historicaldata/web/mapper/SelloutReportMapper.java",
            "masterdata/integration/mapper/unidademedida/UnidadeMedidaIntegrationAutoMapper.java",
            "masterdata/web/mapper/LocationMapper.java",
            "masterdata/web/mapper/MaterialMapper.java",
            "supplyplanning/configuration/web/mapper/PerfilExecucaoSupplyPlanAutoMapper.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_ROUTINES_FILES = List.of(
            "com/opsfactor/community/planning/routines/RoutinesApplication.java",
            "com/opsfactor/community/planning/routines/bi/AgregacaoDFU.java",
            "com/opsfactor/community/planning/routines/dto/lowlevelcode/DFUMalhaCircularDTO.java",
            "com/opsfactor/community/planning/routines/dto/lowlevelcode/LowLevelCodeDTO.java",
            "com/opsfactor/community/planning/routines/dto/lowlevelcode/LowLevelCodeEdgeDTO.java",
            "com/opsfactor/community/planning/routines/dto/lowlevelcode/LowLevelCodeNodeDTO.java",
            "com/opsfactor/community/planning/routines/exceptions/CircularNetworkException.java",
            "com/opsfactor/community/planning/routines/planning/demand/DemandPlanning.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/disaggregation/DemandForecastDisaggregationSpi.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/disaggregation/HistoricalSalesForecastDisaggregation.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/ArimaForecastEngine.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/DemandForecastEngineSpi.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/DemandForecastFoundationModelEngineSpi.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/DemandForecastStatisticalEngineSpi.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/DemandForecastStatisticalResultSupport.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/ExponentialSmoothingForecastEngine.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/HoltWintersForecastEngine.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/MovingAverageForecastEngine.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/MovingAverageForecastEngineValidation.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/RStatisticalForecastEngineValidation.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/engine/RollingMovingAverageForecastEngine.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/processor/DemandForecastHistoryCleaningProcessor.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/processor/DemandForecastStockoutContext.java",
            "com/opsfactor/community/planning/routines/planning/demand/forecast/processor/DemandForecastStockoutTreatmentProcessor.java",
            "com/opsfactor/community/planning/routines/planning/supply/LowLevelCode.java",
            "com/opsfactor/community/planning/routines/planning/supply/ProductionPlanning.java",
            "com/opsfactor/community/planning/routines/planning/supply/SupplyPlanning.java",
            "com/opsfactor/community/planning/routines/planning/supply/constrained/ConstrainedPlanningHeuristicoRotinas.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_R_INSTANCE_FILES = List.of(
            "com/opsfactor/community/rinstance/InstanciaRCaller.java",
            "com/opsfactor/community/rinstance/model/ResultadoForecastEstatistico.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_SUPPORT_FILES = List.of(
            "com/opsfactor/community/planning/support/calendar/Calendario.java",
            "com/opsfactor/community/planning/support/demandplanning/DemandPlanningModelCatalog.java",
            "com/opsfactor/community/planning/support/demandplanning/DemandPlanningPlanningBookCatalog.java",
            "com/opsfactor/community/planning/support/demandplanning/package-info.java",
            "com/opsfactor/community/planning/support/supplyplanning/SupplyPlanningDataContract.java",
            "com/opsfactor/community/planning/support/supplyplanning/SupplyPlanningPlanningBookCatalog.java",
            "com/opsfactor/community/planning/support/enums/EnumsCalendario.java",
            "com/opsfactor/community/planning/support/exceptions/DataUploadException.java",
            "com/opsfactor/community/planning/support/exceptions/IncompatibleCalendarException.java",
            "com/opsfactor/community/planning/support/exceptions/MissingDependencyDataUploadException.java",
            "com/opsfactor/community/planning/support/exceptions/RequiresEnterpriseVersionException.java",
            "com/opsfactor/community/planning/support/exceptions/SchedulingException.java",
            "com/opsfactor/community/planning/support/list/UtilList.java",
            "com/opsfactor/community/planning/support/logging/JvmMemoryLogging.java",
            "com/opsfactor/community/planning/support/utility/Constantes.java",
            "com/opsfactor/community/planning/support/utility/Encoder.java",
            "com/opsfactor/community/planning/support/utility/FuncoesCollections.java",
            "com/opsfactor/community/planning/support/utility/FuncoesMap.java",
            "com/opsfactor/community/planning/support/utility/Logger/LoggerImplementation.java",
            "com/opsfactor/community/planning/support/utility/MetodosUtilidade.java",
            "com/opsfactor/community/planning/support/utility/fileprocessing/FileProcessing.java",
            "com/opsfactor/community/planning/support/utility/fileprocessing/LibraryLoader.java",
            "com/opsfactor/community/planning/support/utility/fileprocessing/ProcessedFile.java",
            "com/opsfactor/community/planning/support/utility/fileprocessing/ProcessedFileRow.java",
            "com/opsfactor/community/planning/support/utility/statistical/NormalDistributionCustom.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_SCHEDULER_FILES = List.of(
            "com/opsfactor/community/scheduler/SchedulerApplication.java",
            "com/opsfactor/community/scheduler/domain/ScheduledTaskAbstract.java",
            "com/opsfactor/community/scheduler/domain/ScheduledTaskExecution.java",
            "com/opsfactor/community/scheduler/domain/ScheduledTaskImediato.java",
            "com/opsfactor/community/scheduler/dto/TaskSchedulingDTO.java",
            "com/opsfactor/community/scheduler/exception/TaskSchedulingException.java",
            "com/opsfactor/community/scheduler/repository/ScheduledTaskAbstractRepository.java",
            "com/opsfactor/community/scheduler/repository/ScheduledTaskExecutionRepository.java",
            "com/opsfactor/community/scheduler/repository/ScheduledTaskImediatoRepository.java",
            "com/opsfactor/community/scheduler/repository/dto/ScheduledTaskPayloadPreflightSnapshot.java",
            "com/opsfactor/community/scheduler/services/CanonicalScheduledTaskClassPolicy.java",
            "com/opsfactor/community/scheduler/services/Task.java",
            "com/opsfactor/community/scheduler/services/TaskSchedulingService.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_SECURITY_FILES = List.of(
            "com/opsfactor/community/security/SecurityApplication.java",
            "com/opsfactor/community/security/config/CustomHttpSecurityConfig.java",
            "com/opsfactor/community/security/login/AuthenticationService.java",
            "com/opsfactor/community/security/login/CommunitySecurityConstants.java",
            "com/opsfactor/community/security/login/dto/UserDTO.java",
            "com/opsfactor/community/security/login/model/User.java",
            "com/opsfactor/community/security/login/model/UserRole.java",
            "com/opsfactor/community/security/login/repository/UserRepository.java",
            "com/opsfactor/community/security/login/service/CustomUserDetailsService.java",
            "com/opsfactor/community/security/login/service/front/UserFrontService.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_WEB_SOURCE_FILES = List.of(
            "com/opsfactor/community/planning/web/WebApplication.java",
            "com/opsfactor/community/planning/web/configuration/IntegrationOpenApiConfiguration.java",
            "com/opsfactor/community/planning/web/configuration/UserRoleType.java",
            "com/opsfactor/community/planning/web/restcontroller/ProcessStatusController.java",
            "com/opsfactor/community/planning/web/restcontroller/WebControllerTaskSchedulingService.java",
            "com/opsfactor/community/planning/web/restcontroller/bi/CommunityInventoryOverviewController.java",
            "com/opsfactor/community/planning/web/restcontroller/admin/AdminRestController.java",
            "com/opsfactor/community/planning/web/restcontroller/configuration/ParametroMaterialLocationController.java",
            "com/opsfactor/community/planning/web/restcontroller/configuration/ParametrosGlobaisController.java",
            "com/opsfactor/community/planning/web/restcontroller/configuration/ParametrosGlobaisControllerPolicy.java",
            "com/opsfactor/community/planning/web/restcontroller/configuration/PerfilExecucaoDemandPlanController.java",
            "com/opsfactor/community/planning/web/restcontroller/configuration/PerfilExecucaoSupplyPlanController.java",
            "com/opsfactor/community/planning/web/restcontroller/configuration/PoliticaEstoquesController.java",
            "com/opsfactor/community/planning/web/restcontroller/configuration/UserConfigurationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/IntegrationControllerAbstract.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/ConversaoUnidadeIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/ConversaoUnidadeProdutoIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/LocationIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/MaterialIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/ParametrosMaterialLocationIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/UnidadeMedidaIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/inventorypolicy/InventoryPolicyDetailIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/inventorypolicy/InventoryPolicyIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/malha/LinhaTransporteIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/malha/LinhaTransporteMaterialIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/malha/VersaoMalhaIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/production/DisponibilidadeRecursoProdutivoIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/production/ListaTecnicaComponenteIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/production/ListaTecnicaIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/production/OperacaoRoteiroIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/production/RecursoProdutivoIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/production/RoteiroIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/masterdata/production/VersaoProducaoSimplesIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/planning/supply/InventoryPlanIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/transactionaldata/EstoqueIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/dataupload/transactionaldata/SelloutIntegrationController.java",
            "com/opsfactor/community/planning/web/restcontroller/masterdata/ClusteringRestController.java",
            "com/opsfactor/community/planning/web/restcontroller/masterdata/LinhaTransporteController.java",
            "com/opsfactor/community/planning/web/restcontroller/masterdata/LocationRestController.java",
            "com/opsfactor/community/planning/web/restcontroller/masterdata/MaterialRestController.java",
            "com/opsfactor/community/planning/web/restcontroller/masterdata/ProductionRestController.java",
            "com/opsfactor/community/planning/web/restcontroller/masterdata/UnidadeMedidaRestController.java",
            "com/opsfactor/community/planning/web/restcontroller/planning/ConstrainedPlanController.java",
            "com/opsfactor/community/planning/web/restcontroller/planning/DemandAnalysisRestController.java",
            "com/opsfactor/community/planning/web/restcontroller/planning/DemandPlanningRestController.java",
            "com/opsfactor/community/planning/web/restcontroller/bi/CommunityDemandSalesOverviewController.java",
            "com/opsfactor/community/planning/web/restcontroller/bi/CommunityMaterialFlowsController.java",
            "com/opsfactor/community/planning/web/restcontroller/bi/CommunityProductionOverviewController.java",
            "com/opsfactor/community/planning/web/restcontroller/bi/CommunityProductionOverviewResourceDetailController.java",
            "com/opsfactor/community/planning/web/restcontroller/bi/CommunitySupplyNetworkDependenciesController.java",
            "com/opsfactor/community/planning/web/restcontroller/planning/DeploymentOperationalController.java",
            "com/opsfactor/community/planning/web/restcontroller/planning/ProductionPlanningBookController.java",
            "com/opsfactor/community/planning/web/restcontroller/planning/SupplyPlanningController.java",
            "com/opsfactor/community/planning/web/restcontroller/runtime/RuntimeInfoController.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_WEB_RESOURCE_FILES = List.of(
            "application-community-defaults.properties",
            "application-database-mariadb.properties",
            "application-dev.properties",
            "application-prd.properties",
            "application.properties",
            "banner.txt",
            "data-community-dev.sql",
            "data.sql",
            "META-INF/services/org.hibernate.boot.model.FunctionContributor"
    );

    private static final List<String> COMMUNITY_ALLOWED_PLANNING_DTO_SOURCE_FILES = List.of(
            "com/opsfactor/community/planning/dto/DtoApplication.java",
            "com/opsfactor/community/planning/dto/dto/bi/historicaldata/SelloutReportDTO.java",
            "com/opsfactor/community/planning/dto/dto/bi/historicaldata/SelloutReportParametrosDTO.java",
            "com/opsfactor/community/planning/dto/dto/cluster/ClusterLocationsDTO.java",
            "com/opsfactor/community/planning/dto/dto/cluster/ClusterProdutosDTO.java",
            "com/opsfactor/community/planning/dto/dto/cluster/RegraAlocaoClusterLocationsCaracteristicaDTO.java",
            "com/opsfactor/community/planning/dto/dto/cluster/RegraAlocaoClusterLocationsDTO.java",
            "com/opsfactor/community/planning/dto/dto/cluster/RegraAlocaoClusterLocationsPaisEstadoDTO.java",
            "com/opsfactor/community/planning/dto/dto/cluster/RegraAlocaoClusterLocationsTipoLocationDTO.java",
            "com/opsfactor/community/planning/dto/dto/cluster/RegraAlocaoClusterProdutosDTO.java",
            "com/opsfactor/community/planning/dto/dto/cluster/allocation/AlocacaoClusterLocationDTO.java",
            "com/opsfactor/community/planning/dto/dto/cluster/allocation/AlocacaoClusterMaterialDTO.java",
            "com/opsfactor/community/planning/dto/dto/configuration/application/ApplicationAppearanceDTO.java",
            "com/opsfactor/community/planning/dto/dto/configuration/planning/demand/DemandPlanningClusterLevelConfigurationDTO.java",
            "com/opsfactor/community/planning/dto/dto/configuration/planning/demand/DemandPlanningForecastParametersDTO.java",
            "com/opsfactor/community/planning/dto/dto/configuration/planning/demand/DemandPlanningGeneralParametersDTO.java",
            "com/opsfactor/community/planning/dto/dto/configuration/planning/demand/DemandPlanningPreviaForecastRequestDTO.java",
            "com/opsfactor/community/planning/dto/dto/configuration/planning/demand/PerfilExecucaoDemandPlanDTO.java",
            "com/opsfactor/community/planning/dto/dto/configuration/planning/supply/PerfilExecucaoSupplyPlanDTO.java",
            "com/opsfactor/community/planning/dto/dto/configuration/user/ConfiguracaoUsuarioDTO.java",
            "com/opsfactor/community/planning/dto/dto/configuration/user/UserInterfacePreferencesDTO.java",
            "com/opsfactor/community/planning/dto/dto/controller/ResponseDTO.java",
            "com/opsfactor/community/planning/dto/dto/filter/FiltroMaterialLocationDeCombinacaoCaracteristicasDTO.java",
            "com/opsfactor/community/planning/dto/dto/historical/FirstAndLastDateDTO.java",
            "com/opsfactor/community/planning/dto/dto/integration/IntegrationDataDtoAbstract.java",
            "com/opsfactor/community/planning/dto/dto/integration/IntegrationDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/IntegrationOptionsDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/IntegrationPrimaryKeyDTOAbstract.java",
            "com/opsfactor/community/planning/dto/dto/integration/IntegrationTextNormalization.java",
            "com/opsfactor/community/planning/dto/dto/integration/configuration/ParametrosMaterialLocationIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/historicaldata/EstoqueIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/historicaldata/EstoqueIntegrationFiltroDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/historicaldata/SelloutIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/historicaldata/SelloutIntegrationFiltroDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/inventorypolicy/PoliticaEstoquesIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/inventorypolicy/PoliticaEstoquesMaterialLocationIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/LocationIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/LocationIntegrationFiltroDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/ProdutoIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/malha/LinhaTransporteIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/malha/LinhaTransporteProdutoIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/malha/LinhaTransporteProdutoIntegrationOptionsDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/malha/VersaoMalhaIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/production/DisponibilidadeRecursoProdutivoIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/production/ListaTecnicaComponenteIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/production/ListaTecnicaIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/production/RecursoProdutivoIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/production/RoteiroIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/production/VersaoProducaoSimplesIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/unidademedida/ConversaoUnidadeIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/unidademedida/ConversaoUnidadeProdutoIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/integration/masterdata/unidademedida/UnidadeMedidaDataUploadDTO.java",
            "com/opsfactor/community/planning/dto/dto/integration/planning/supply/InventoryPlanIntegrationDataDto.java",
            "com/opsfactor/community/planning/dto/dto/masterdata/LocationDTO.java",
            "com/opsfactor/community/planning/dto/dto/masterdata/ProdutoDTO.java",
            "com/opsfactor/community/planning/dto/dto/masterdata/characteristic/CaracteristicaLocationDTO.java",
            "com/opsfactor/community/planning/dto/dto/masterdata/characteristic/CaracteristicaProdutoLocationDTO.java",
            "com/opsfactor/community/planning/dto/dto/masterdata/characteristic/CaracteristicaProdutoDTO.java",
            "com/opsfactor/community/planning/dto/dto/masterdata/characteristic/TipoCaracteristicaDTO.java",
            "com/opsfactor/community/planning/dto/dto/masterdata/unidademedida/ConversaoUnidadeMedidaDTO.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/CellDetailsDTO.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/ColumnDefDTO.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/GroupDTO.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/KeyFigureDTOAbstract.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/PlanningBookDTO.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/PlanningBookParentSelectionDTO.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/SelectedPlanningBookCellDTO.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/specializedkeyfigure/KeyFigureDTOCoberturaEstoque.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/specializedkeyfigure/KeyFigureDTOPadrao.java",
            "com/opsfactor/community/planning/dto/dto/planning/planningbook/specializedkeyfigure/KeyFigureDTORelacaoEntreValores.java",
            "com/opsfactor/community/planning/dto/dto/template/AgGridColumnDefDTO.java",
            "com/opsfactor/community/planning/dto/dto/template/AgGridDTO.java",
            "com/opsfactor/community/planning/dto/dto/template/DTO.java",
            "com/opsfactor/community/planning/dto/mapper/cluster/ClusterLocationsMapper.java",
            "com/opsfactor/community/planning/dto/mapper/cluster/ClusterProdutosMapper.java",
            "com/opsfactor/community/planning/dto/mapper/configuration/planning/demand/DemandPlanningConfigurationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/configuration/planning/demand/PerfilExecucaoDemandPlanAutoMapper.java",
            "com/opsfactor/community/planning/dto/mapper/configuration/planning/supply/PerfilExecucaoSupplyPlanAutoMapper.java",
            "com/opsfactor/community/planning/dto/mapper/configuration/user/ConfiguracaoUsuarioAutoMapper.java",
            "com/opsfactor/community/planning/dto/mapper/historicaldata/SelloutReportMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/IntegrationMapperInterface.java",
            "com/opsfactor/community/planning/dto/mapper/integration/configuration/ParametrosMaterialLocationIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/configuration/ParametrosMaterialLocationIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/historicaldata/EstoqueIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/historicaldata/EstoqueIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/historicaldata/SelloutIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/historicaldata/SelloutIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/inventorypolicy/PoliticaEstoquesIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/inventorypolicy/PoliticaEstoquesIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/inventorypolicy/PoliticaEstoquesMaterialLocationIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/inventorypolicy/PoliticaEstoquesMaterialLocationIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/LocationIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/LocationIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/ProdutoIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/ProdutoIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/malha/LinhaTransporteIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/malha/LinhaTransporteIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/malha/LinhaTransporteProdutoIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/malha/LinhaTransporteProdutoIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/malha/VersaoMalhaIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/malha/VersaoMalhaIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/DisponibilidadeRecursoProdutivoIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/DisponibilidadeRecursoProdutivoIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/ListaTecnicaComponenteIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/ListaTecnicaComponenteIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/ListaTecnicaIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/ListaTecnicaIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/RecursoProdutivoIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/RecursoProdutivoIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/RoteiroIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/RoteiroIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/VersaoProducaoSimplesIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/production/VersaoProducaoSimplesIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/unidademedida/ConversaoUnidadeIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/unidademedida/ConversaoUnidadeIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/unidademedida/ConversaoUnidadeProdutoIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/unidademedida/ConversaoUnidadeProdutoIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/masterdata/unidademedida/UnidadeMedidaIntegrationAutoMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/planning/supply/InventoryPlanIntegrationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/integration/planning/supply/InventoryPlanIntegrationSupportData.java",
            "com/opsfactor/community/planning/dto/mapper/integration/planning/supply/InventoryPlanIntegrationValidation.java",
            "com/opsfactor/community/planning/dto/mapper/masterdata/LocationMapper.java",
            "com/opsfactor/community/planning/dto/mapper/masterdata/MaterialMapper.java",
            "com/opsfactor/community/planning/dto/service/masterdata/ClusterLocationDtoService.java",
            "com/opsfactor/community/planning/dto/service/masterdata/LocationDtoService.java",
            "com/opsfactor/community/planning/dto/service/masterdata/MaterialDtoService.java"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_TRANSACTIONAL_ENTITY_OR_REPOSITORY_FILES = List.of(
            "Pedido.java",
            "OrderItem.java",
            "PedidoRepository.java",
            "OrderItemRepository.java",
            "Remessa.java",
            "RemessaLinha.java",
            "RemessaRepository.java",
            "RemessaLinhaRepository.java",
            "OrdemCarga.java",
            "OrdemCargaDropoff.java",
            "OrdemCargaRepository.java",
            "OrdemCargaDropoffRepository.java"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_ECONOMIC_DEMAND_PLANNING_FILES = List.of(
            "PricePlan.java",
            "PricePlanRepository.java",
            "ListaPrecosDemandPlanService.java"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_ECONOMIC_SUPPLY_PLANNING_FILES = List.of(
            "CustosProjection.java",
            "CustosProjectionFactory.java",
            "LogisticsCost.java",
            "LogisticsCostRepository.java",
            "CurvaCustoLogistico.java",
            "CurvaCustoLogisticoRepository.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_TEMPORAL_SPLIT_CURVE_PROJECTION_FILES = List.of(
            "SplitTemporalProjectionCurva.java",
            "SplitTemporalProjectionCurvaFlat.java"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_CONFIGURABLE_TEMPORAL_SPLIT_FILES = List.of(
            "CurvaSplitTemporal.java",
            "CurvaSplitTemporalRepository.java",
            "FiltroDfus.java",
            "PerfilExecucaoSupplyPlanCurvaSplitDemanda.java",
            "SplitDiaMes.java",
            "SplitDiaSemana.java",
            "SplitMes.java",
            "SplitSemana.java"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_CONFIGURABLE_CALENDAR_FILES = List.of(
            "DiaUtil.java",
            "DiaUtilRepository.java",
            "Feriado.java",
            "FeriadoRepository.java",
            "FeriadoEstadual.java",
            "FeriadoEstadualRepository.java",
            "FeriadoMunicipal.java",
            "FeriadoMunicipalRepository.java",
            "FeriadoNacional.java",
            "FeriadoNacionalRepository.java",
            "Weekday.java",
            "WeekdayRepository.java",
            "WorkingDayProjection.java",
            "WorkingDayProjectionFactory.java",
            "WorkingDayCalendar.java",
            "WorkingDayCalendarRepository.java"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_ADVANCED_SECURITY_MODEL_FILES = List.of(
            "UserLocation.java",
            "UserLocationRepository.java",
            "Tenant.java",
            "TenantRepository.java",
            "BlockedIp.java",
            "BlockedIpRepository.java",
            "LoginAttempt.java",
            "LoginAttemptRepository.java"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_ADVANCED_SECURITY_IMPORT_TOKENS = List.of(
            "oauth",
            "oauth2",
            "oidc",
            "saml",
            "saml2",
            "keycloak",
            "jsonwebtoken",
            "jwt"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_CLOUD_MESSAGING_IMPORT_TOKENS = List.of(
            "com.azure.messaging",
            "com.azure.identity",
            "servicebus",
            "software.amazon.awssdk",
            "com.amazonaws",
            "amazonaws",
            "sqs",
            "sns",
            "jakarta.jms",
            "javax.jms",
            "org.springframework.jms",
            "rabbitmq",
            "kafka"
    );

    private static final List<String> COMMUNITY_SPRING_BEAN_FIELD_TYPE_TOKENS = List.of(
            "Repository",
            "Service",
            "Mapper",
            "Factory",
            "ApplicationContext",
            "PasswordEncoder",
            "Authentication"
    );

    private static final List<String> COMMUNITY_ALLOWED_NON_AUTOWIRED_BEAN_FIELD_FILES = List.of(
            "DoubleArrayToStringConverter.java",
            "FloatArrayToStringConverter.java",
            "Task.java"
    );

    private static final List<String> COMMUNITY_ALLOWED_OPTIONAL_AUTOWIRED_FIELD_KEYS = List.of(
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanOptimizationService",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanProcessChainService",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanPresetConstraintGroupSpi",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanExecutionProfileLocationScope",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanExecutionProfileMaterialScope",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanFirmProductionOrdersSpi",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java#supplyPlanOpenOrdersHeuristicSpi",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/ConstrainedPlanService.java#supplyPlanExecutionProfileMaterialScope",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/HeuristicoService.java#supplyPlanExecutionProfileLocationPolicySpi",
            "src/main/java/com/opsfactor/community/capability/supplyplanning/supplyplan/facade/SupplyPlanFacade.java#supplyPlanExecutionProfileMaterialScope"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_CONFIGURED_VIEW_CHARACTERISTIC_REPOSITORY_FILES = List.of(
            "ConfiguredViewCaracteristicaProdutoRepository.java",
            "ConfiguredViewCaracteristicaProdutoFiltroRepository.java",
            "ConfiguredViewCaracteristicaLocationRepository.java",
            "ConfiguredViewCaracteristicaLocationFiltroRepository.java",
            "ConfiguredViewCaracteristicaProduto.java",
            "ConfiguredViewCaracteristicaProdutoFiltro.java",
            "ConfiguredViewCaracteristicaLocation.java",
            "ConfiguredViewCaracteristicaLocationFiltro.java"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_REMOVED_ORPHAN_FILES = List.of(
            "DataUploadLogArquivo.java",
            "DataUploadLogArquivoRepository.java",
            "DataUploadLogLinha.java",
            "DataUploadLogLinhaRepository.java",
            "ParametrosNivelServicoEstoque.java",
            "ParametrosNivelServicoEstoqueRepository.java",
            "PossibilidadeAbastecimentoMalhaService.java",
            "Caracteristica.java",
            "CaracteristicaLocation.java",
            "CaracteristicaLocationRepository.java",
            "CaracteristicaProduto.java",
            "CaracteristicaProdutoRepository.java",
            "LogModificacaoDemandPlanItem.java",
            "LogModificacaoDemandPlanItemRepository.java",
            "ValorCaracteristicaLocation.java",
            "ValorCaracteristicaProduto.java",
            "ValorCaracteristicaLocationRepository.java",
            "ValorCaracteristicaProdutoRepository.java",
            "GrupoProdutos.java",
            "SimulatedDemandPlanAgrupamentoDTO.java",
            "AcompanhamentoPlanoDTO.java",
            "AggregatedByLocationMaterialUOMDatePlanTypeImpl.java",
            "AggregatedByLocationMaterialUOMDateTime.java",
            "AggregatedByLocationMaterialUOMDateTimeImpl.java",
            "AgrupamentoCaracteristicas.java",
            "AgrupamentoCaracteristicasLocation.java",
            "AgrupamentoCaracteristicasMaterial.java",
            "AmostragemPonderadaComMediaEDesvio.java",
            "ArrayUtility.java",
            "AtributoDTO.java",
            "BIProjectionAgrupamentoCaracteristicasPeriodo.java",
            "BIProjectionAgrupamentoCaracteristicasPeriodoTest.java",
            "BIProjectionLocation.java",
            "BIProjectionMaterial.java",
            "BiLogDTO.java",
            "ClassScope.java",
            "ConsolidacaoDados.java",
            "ConversaoEstatisticaEntreTamanhosBucket.java",
            "EmptyIntegrationSupportData.java",
            "FinderDTO.java",
            "GenericDTO.java",
            "ImageUtilitys.java",
            "InconsistentProductionRoutingException.java",
            "LocationProjectionPerfilExecucaoSupplyPlan.java",
            "LowLevelCodeSomenteDependenciasProducao.java",
            "LogTaskDTO.java",
            "PeriodoDTO.java",
            "ScheduleDTO.java",
            "ScatterPlotDTO.java",
            "SneakyThrowUtil.java",
            "ThrowingDoubleFunction.java",
            "ThrowingToDoubleFunction.java",
            "VersaoProducaoAutoMapper.java",
            "VersaoProducaoDTO.java",
            "VersaoProducaoFrontService.java",
            "VersaoProducaoIntegrationDTO.java",
            "VersaoProducaoIntegrationMapper.java",
            "VersaoProducaoIntegrationMapperCommunityTest.java",
            "VersaoProducaoIntegrationService.java",
            "VendaRepository.java",
            "VersaoProducaoRepository.java"
    );

    private static final List<String> COMMUNITY_FORBIDDEN_REMOVED_ORPHAN_PATHS = List.of();

    private static final List<String> LEGACY_FRONTEND_RESOURCE_DIRECTORIES = List.of(
            "templates",
            "public",
            "static"
    );

    private static final String FRONTEND_DISTRIBUTION_DIRECTORY_NAME = "dist";

    private static final List<String> LEGACY_FRONTEND_FILE_SUFFIXES = List.of(
            ".html",
            ".js",
            ".css"
    );

    private static final List<String> LEGACY_FRONTEND_WEB_CODE_TOKENS = List.of(
            "@Controller",
            "ModelAndView",
            "ViewControllerRegistry",
            "addViewControllers",
            "Thymeleaf",
            "thymeleaf",
            "org.springframework.stereotype.Controller",
            "org.springframework.web.servlet.ModelAndView"
    );

    @Test
    void communityJavaSourcesShouldNotImportOrDeclarePrivateOrLegacyPackages() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Comentarios podem explicar o overlay Enterprise e a origem historica
         * da migracao. Imports/packages, porem, devem contar a historia real
         * do novo repositorio publico: Community depende apenas de
         * com.opsfactor.community e nao volta a acoplar nem o Enterprise privado
         * nem o namespace legado com.opsfactor.planning.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (ENTERPRISE_IMPORT_OR_PACKAGE_PATTERN.matcher(sourceLine).matches()
                        || LEGACY_PLANNING_IMPORT_OR_PACKAGE_PATTERN.matcher(sourceLine).matches()) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao pode importar nem declarar packages Enterprise ou legados:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainJavaCodeShouldNotReferenceEnterprisePackagesByFullyQualifiedName() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path mainJavaDirectory = communityWorkspaceDirectory.resolve("src/main/java");
        List<String> violations = new ArrayList<>();

        /*
         * A guarda de imports/packages cobre o acoplamento normal. Esta segunda
         * varredura impede que codigo produtivo burle a fronteira usando FQCN
         * Enterprise diretamente em chamadas, annotations ou strings
         * funcionais. Comentarios e Javadocs continuam livres para documentar
         * o overlay privado.
         */
        for (Path javaSourcePath : findWorkspaceFiles(mainJavaDirectory, ".java")) {
            boolean insideBlockComment = false;
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                JavaCodeSnippet javaCodeSnippet =
                        removeJavaCommentsFromLine(
                                sourceLines.get(lineIndex),
                                insideBlockComment);
                insideBlockComment = javaCodeSnippet.insideBlockComment();
                if (javaCodeSnippet.code()
                        .contains(ENTERPRISE_FULLY_QUALIFIED_PACKAGE_REFERENCE)
                        && !isApprovedEnterpriseFqcnReference(
                        communityWorkspaceDirectory,
                        javaSourcePath,
                        javaCodeSnippet.code())) {
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
                "Community main nao pode referenciar package Enterprise por FQCN em codigo produtivo:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainJavaCodeShouldNotReferenceLegacyPlanningPackagesByFullyQualifiedName() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path mainJavaDirectory = communityWorkspaceDirectory.resolve("src/main/java");
        List<String> violations = new ArrayList<>();

        /*
         * Imports normais ja sao bloqueados acima. Esta varredura cobre o
         * desvio mais sutil por Class.forName, reflection, annotations ou
         * strings funcionais com FQCN legado. Nao ha excecao: uma migracao
         * persistida deve ser tratada por executor/preflight, nunca voltando a
         * carregar o namespace planning-* no runtime Community.
         */
        for (Path javaSourcePath : findWorkspaceFiles(mainJavaDirectory, ".java")) {
            boolean insideBlockComment = false;
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                JavaCodeSnippet javaCodeSnippet =
                        removeJavaCommentsFromLine(sourceLines.get(lineIndex), insideBlockComment);
                insideBlockComment = javaCodeSnippet.insideBlockComment();
                if (javaCodeSnippet.code().contains(LEGACY_PLANNING_FULLY_QUALIFIED_PACKAGE_REFERENCE)) {
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
                "Community main nao pode referenciar package legado por FQCN em codigo produtivo:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityBuildAndPropertiesShouldNotReferenceEnterpriseArtifacts() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        // POMs e properties fazem parte da fronteira de build/runtime. Qualquer token
        // Enterprise nesses arquivos indica dependencia privada ou bootstrap incorreto.
        for (Path configurationPath : findWorkspaceConfigurationFiles(communityWorkspaceDirectory)) {
            List<String> configurationLines = Files.readAllLines(configurationPath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < configurationLines.size(); lineIndex++) {
                String configurationLine = configurationLines.get(lineIndex);
                if (containsEnterpriseToken(configurationLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, configurationPath, lineIndex, configurationLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao pode declarar artefatos Maven nem properties Enterprise:\n" + String.join("\n", violations));

    }

    @Test
    void communityWorkspacePomShouldStaySingleModuleRootBuild() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        String communityWorkspacePom = Files.readString(
                communityWorkspaceDirectory.resolve("pom.xml"),
                StandardCharsets.UTF_8);

        /*
         * O Community deve compilar sozinho por um unico build Maven simples.
         * A fronteira publica agora fica em packages e guardas de import, nao
         * em submodulos Maven que imitam a divisao historica do monolito.
         */
        Assertions.assertTrue(communityWorkspacePom.contains("<artifactId>opsfactor-community</artifactId>"));
        Assertions.assertTrue(communityWorkspacePom.contains("<packaging>jar</packaging>"));
        Assertions.assertFalse(communityWorkspacePom.contains("<modules>"));
        Assertions.assertFalse(communityWorkspacePom.contains("<module>"));
        Assertions.assertFalse(communityWorkspacePom.contains("enterprise"));
        Assertions.assertFalse(communityWorkspacePom.contains("planning-batch"));
        Assertions.assertFalse(communityWorkspacePom.contains("planning-optimizer"));

    }

    @Test
    void communityWorkspaceShouldNotContainNestedMavenPomFiles() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O achatamento Maven precisa continuar fisico, nao apenas sem tag
         * <modules> no POM raiz. Um pom.xml aninhado reintroduziria a ideia de
         * submodulo Maven mesmo que ainda nao estivesse listado no build raiz.
         */
        for (Path pomPath : findWorkspaceFiles(communityWorkspaceDirectory, "pom.xml")) {
            String relativePomPath = communityWorkspaceDirectory
                    .relativize(pomPath)
                    .toString()
                    .replace('\\', '/');

            if (!"pom.xml".equals(relativePomPath)) {
                violations.add(relativePomPath);
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve manter somente o POM Maven raiz:\n" + String.join("\n", violations));

    }

    @Test
    void communityPomFilesShouldNotReferenceLegacyPlanningArtifacts() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O snapshot publico deve ser um workspace Maven proprio. Qualquer
         * referencia `planning-*` em POM aponta de volta para os modulos legados
         * que estamos substituindo por artefatos `opsfactor-community-*`.
         */
        for (Path pomPath : findWorkspaceFiles(communityWorkspaceDirectory, "pom.xml")) {
            List<String> pomLines = Files.readAllLines(pomPath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < pomLines.size(); lineIndex++) {
                String pomLine = pomLines.get(lineIndex);
                if (pomLine.contains("planning-")) {
                    violations.add(formatViolation(communityWorkspaceDirectory, pomPath, lineIndex, pomLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "POMs Community nao devem referenciar artefatos ou modulos Maven legados planning-*:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMavenDependenciesShouldNotUsePrivateEnterpriseRuntimeStacks() throws Exception {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * A edicao aberta pode depender de bibliotecas publicas usadas pelo seu
         * recorte funcional, mas nao deve puxar stacks runtime do overlay
         * privado: Azure Service Bus, OR-Tools, bridge Python ou STL privado.
         * A verificacao usa coordenadas Maven para nao depender de comentarios
         * ou texto livre no POM.
         */
        for (Path pomPath : findWorkspaceFiles(communityWorkspaceDirectory, "pom.xml")) {
            Element pomDocumentElement = readPomDocumentElement(pomPath);
            NodeList dependencyNodeList = pomDocumentElement.getElementsByTagNameNS("*", "dependency");

            for (int dependencyIndex = 0; dependencyIndex < dependencyNodeList.getLength(); dependencyIndex++) {
                Element dependencyElement = (Element) dependencyNodeList.item(dependencyIndex);
                String dependencyCoordinate =
                        getFirstChildTextContent(dependencyElement, "groupId")
                                + ":"
                                + getFirstChildTextContent(dependencyElement, "artifactId");
                if (isForbiddenPrivateRuntimeDependencyCoordinate(dependencyCoordinate)) {
                    violations.add(communityWorkspaceDirectory.relativize(pomPath)
                            + " -> "
                            + dependencyCoordinate);
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "POMs Community nao devem declarar dependencias runtime privadas/comerciais do Enterprise:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityWebApplicationShouldUseExplicitCommunityOnlyComponentScan() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        String communityWebApplicationSource = Files.readString(
                communityWorkspaceDirectory.resolve(
                        "src/main/java/com/opsfactor/community/web/WebApplication.java"),
                StandardCharsets.UTF_8);

        /*
         * Depois do achatamento Maven, a fronteira Community/Enterprise passa a
         * ser package. O executavel Community deve escanear apenas packages
         * abertos; o Enterprise possui bootstrap proprio para adicionar o
         * overlay privado.
         */
        Assertions.assertTrue(communityWebApplicationSource.contains("@ComponentScan(basePackages = {"));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.platform.model\""));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.platform.scheduler\""));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.platform.dto\""));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.platform.routine\""));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.platform.service\""));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.platform.bi\""));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.platform.cache\""));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.platform.runtime\""));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.web\""));
        Assertions.assertTrue(communityWebApplicationSource.contains("\"com.opsfactor.community.platform.security\""));
        Assertions.assertFalse(
                communityWebApplicationSource.contains("@ComponentScan(basePackages = {\"com.opsfactor\"})"));
        Assertions.assertFalse(communityWebApplicationSource.contains("\"com.opsfactor\""));
        Assertions.assertFalse(communityWebApplicationSource.contains("com.opsfactor.enterprise"));

    }

    @Test
    void communityExecutablePackagingShouldStayOnRootPom() throws Exception {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();
        boolean rootExecutablePluginFound = false;

        /*
         * O Community possui um unico artefato executavel no POM raiz. Classes
         * auxiliares @SpringBootApplication continuam servindo como fontes de
         * configuracao, mas nao existem mais POMs internos com repackage proprio.
         */
        for (Path pomPath : findWorkspaceFiles(communityWorkspaceDirectory, "pom.xml")) {
            boolean pomDeclaresSpringBootMavenPlugin =
                    pomDeclaresActiveSpringBootMavenPlugin(pomPath);
            String relativePomPath = communityWorkspaceDirectory
                    .relativize(pomPath)
                    .toString()
                    .replace('\\', '/');

            if ("pom.xml".equals(relativePomPath)) {
                rootExecutablePluginFound = pomDeclaresSpringBootMavenPlugin;
                continue;
            }

            if (pomDeclaresSpringBootMavenPlugin) {
                violations.add(relativePomPath);
            }
        }

        Assertions.assertTrue(
                rootExecutablePluginFound,
                "POM raiz deve continuar sendo o unico fat jar executavel do Community.");
        assertTrue(
                violations.isEmpty(),
                "Somente o POM raiz pode declarar spring-boot-maven-plugin ativo:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMavenRepositoriesShouldNotDeclareSpringSnapshotsOrMilestones() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O repositorio Community deve compilar de forma reprodutivel usando
         * releases estaveis em Maven Central. Repositorios Spring de
         * snapshot/milestone nao devem ficar declarados em POM publicado.
         */
        for (Path configurationPath : findWorkspaceConfigurationFiles(communityWorkspaceDirectory)) {
            if (!configurationPath.getFileName().toString().equals("pom.xml")) {
                continue;
            }

            List<String> configurationLines = Files.readAllLines(configurationPath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < configurationLines.size(); lineIndex++) {
                String configurationLine = configurationLines.get(lineIndex);
                if (configurationLine.contains("repo.spring.io")
                        || configurationLine.contains("spring-snapshots")
                        || configurationLine.contains("spring-milestones")) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            configurationPath,
                            lineIndex,
                            configurationLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve declarar spring-snapshots/spring-milestones em POM publicado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityActiveMaterialRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path produtoRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/product/material/repository/ProdutoRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * MaterialDtoService e ClusterEParametrosProjectionFactory validam id
         * ausente e duplicado antes de indexar materiais ativos. O repository
         * precisa retornar List para que ids duplicados vindos do snapshot nao
         * sejam deduplicados antes dessas validations.
         */
        List<String> sourceLines = Files.readAllLines(produtoRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<Produto>")
                    && sourceLine.contains("customFindProdutosAtivos")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        produtoRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        assertTrue(
                violations.isEmpty(),
                "ProdutoRepository.customFindProdutosAtivos() deve preservar cardinalidade em List "
                        + "ate as validations de material ativo:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communitySelloutFirstLastRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path selloutRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/transactionaldata/sales/sellout/repository/SelloutRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * SalesProjectionFactory valida duplicidade de first/last por DFU,
         * location e material antes de popular o indice mutavel. O repository
         * precisa retornar List para que linhas agregadas duplicadas nao sejam
         * descartadas antes desse gate.
         */
        List<String> sourceLines = Files.readAllLines(selloutRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<FirstLastBy")
                    && sourceLine.contains("findFirstLastSelloutPor")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        selloutRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        assertTrue(
                violations.isEmpty(),
                "SelloutRepository first/last deve preservar cardinalidade em List "
                        + "ate as validations de SalesProjectionFactory:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityClusterLocationRepositoryShouldPreserveSnapshotCardinalityUntilValidation() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path clusterLocationsRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/cluster/repository/location/ClusterLocationsRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * ClusterEParametrosProjectionFactory valida ids duplicados de clusters
         * de location antes de montar os mapas centrais. O repository nao pode
         * voltar a retornar Set nesse snapshot amplo, pois isso deduplicaria a
         * cardinalidade estrutural antes da borda funcional validar o dado.
         */
        List<String> sourceLines = Files.readAllLines(clusterLocationsRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<ClusterLocations>")
                    && sourceLine.contains("customFindAll")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        clusterLocationsRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        assertTrue(
                violations.isEmpty(),
                "ClusterLocationsRepository.customFindAll() deve preservar cardinalidade em List ate a validation:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communitySupplyExecutionProfileRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path perfilExecucaoSupplyPlanRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/configuration/repository/PerfilExecucaoSupplyPlanRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * PerfilExecucaoSupplyPlanFrontService valida ids duplicados antes de
         * devolver a listagem para o front compartilhado. O repository nao pode
         * voltar a retornar Set nesse snapshot amplo, pois isso deduplicaria a
         * cardinalidade estrutural antes da borda Community acusar cadastro ou
         * overlay quebrado.
         */
        List<String> sourceLines = Files.readAllLines(perfilExecucaoSupplyPlanRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<PerfilExecucaoSupplyPlan>")
                    && sourceLine.contains("customFindAll")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        perfilExecucaoSupplyPlanRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        assertTrue(
                violations.isEmpty(),
                "PerfilExecucaoSupplyPlanRepository.customFindAll() deve preservar cardinalidade em List ate a validation:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityDemandPlanningClusterParameterRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path parametrosDemandPlanNivelClusterRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/demandplanning/configuration/repository/ParametrosDemandPlanNivelClusterRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * ParametrosDemandPlanningProjectionFactory valida chave duplicada de
         * cluster location/material antes de montar o mapa efetivo de
         * parametros. O repository deve preservar cardinalidade em List para
         * que essa validation receba a fotografia real do snapshot.
         */
        List<String> sourceLines =
                Files.readAllLines(parametrosDemandPlanNivelClusterRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<ParametrosDemandPlanNivelCluster>")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        parametrosDemandPlanNivelClusterRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        assertTrue(
                violations.isEmpty(),
                "ParametrosDemandPlanNivelClusterRepository deve preservar cardinalidade em List "
                        + "ate a validation da ParametrosDemandPlanningProjectionFactory:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityHistoricalDemandPlanItemRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path historicoDemandPlanItemRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/demandplanning/demandplan/repository/HistoricoDemandPlanItemRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * DemandPlanProjectionFactory valida linhas historicas duplicadas pela
         * chave composta antes de popular os mapas paralelos da projection. O
         * repository deve preservar cardinalidade em List, mas manter DISTINCT
         * porque o fetch join de location/material pode multiplicar a entidade
         * raiz retornada pelo JPA.
         */
        List<String> sourceLines = Files.readAllLines(historicoDemandPlanItemRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<HistoricoDemandPlanItem>")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        historicoDemandPlanItemRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        String repositorySource = Files.readString(historicoDemandPlanItemRepositoryPath, StandardCharsets.UTF_8);
        if (!repositorySource.contains("SELECT DISTINCT hdpl FROM HistoricoDemandPlanItem hdpl")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    historicoDemandPlanItemRepositoryPath,
                    0,
                    "HistoricoDemandPlanItemRepository deve manter SELECT DISTINCT no snapshot com fetch join."));
        }

        assertTrue(
                violations.isEmpty(),
                "HistoricoDemandPlanItemRepository deve preservar cardinalidade em List "
                        + "ate a validation da DemandPlanProjectionFactory e manter DISTINCT no fetch join:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityProductionResourceRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path recursoProdutivoRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/production/productionresource/repository/RecursoProdutivoRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * SupplyNetworkProjectionFactory valida ids duplicados dos recursos
         * produtivos antes de materializar os mapas da projection. O repository
         * deve preservar cardinalidade em List, mas manter DISTINCT na JPQL
         * porque o fetch join de disponibilidades pode multiplicar linhas da
         * mesma entidade raiz.
         */
        List<String> sourceLines = Files.readAllLines(recursoProdutivoRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<RecursoProdutivo>")
                    && sourceLine.contains("customFindByLocationIn")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        recursoProdutivoRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        String repositorySource = Files.readString(recursoProdutivoRepositoryPath, StandardCharsets.UTF_8);
        if (!repositorySource.contains("SELECT DISTINCT rp FROM RecursoProdutivo rp")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    recursoProdutivoRepositoryPath,
                    0,
                    "RecursoProdutivoRepository.customFindByLocationIn(...) deve usar SELECT DISTINCT no fetch join."));
        }

        assertTrue(
                violations.isEmpty(),
                "RecursoProdutivoRepository.customFindByLocationIn(...) deve preservar cardinalidade em List "
                        + "e evitar duplicidade artificial do fetch join:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityBomAndRoutingRepositoriesShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path listaTecnicaRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/production/billofmaterials/repository/ListaTecnicaRepository.java");
        Path listaTecnicaComponenteRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/production/billofmaterials/repository/ListaTecnicaComponenteRepository.java");
        Path roteiroRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/production/routing/repository/RoteiroRepository.java");
        Path operacaoRoteiroRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/production/operation/repository/OperacaoRoteiroRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * SupplyNetworkProjectionFactory valida ids duplicados de BOM e roteiro antes
         * de montar mapas por id/location/material. Os repositories raiz devem
         * preservar cardinalidade em List, mas manter DISTINCT nas JPQLs porque os
         * fetch joins de componentes e operacoes multiplicam linhas da mesma entidade
         * raiz. Os repositories filhos tambem nao podem voltar a Set, pois isso
         * deduplicaria o snapshot antes das validations de Data Upload/front.
         */
        List<String> listaTecnicaSourceLines = Files.readAllLines(listaTecnicaRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < listaTecnicaSourceLines.size(); lineIndex++) {
            String sourceLine = listaTecnicaSourceLines.get(lineIndex);
            if (sourceLine.contains("Set<ListaTecnica>")
                    && sourceLine.contains("customFindAllByLocationInAndMaterialOutputInFetchListaTecnicaComponente")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        listaTecnicaRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        String listaTecnicaRepositorySource =
                Files.readString(listaTecnicaRepositoryPath, StandardCharsets.UTF_8);
        if (!listaTecnicaRepositorySource.contains("SELECT DISTINCT lt FROM ListaTecnica lt")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    listaTecnicaRepositoryPath,
                    0,
                    "ListaTecnicaRepository deve usar SELECT DISTINCT no fetch join de componentes."));
        }

        List<String> listaTecnicaComponenteSourceLines =
                Files.readAllLines(listaTecnicaComponenteRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < listaTecnicaComponenteSourceLines.size(); lineIndex++) {
            String sourceLine = listaTecnicaComponenteSourceLines.get(lineIndex);
            if (sourceLine.contains("Set<ListaTecnicaComponente>")
                    && (sourceLine.contains("findAllByListaTecnicaComponenteCompositeKeyListaTecnicaLocationInAndListaTecnicaComponenteCompositeKeyListaTecnicaMaterialOutputIn")
                    || sourceLine.contains("findAllByListaTecnicaComponenteCompositeKeyListaTecnicaIdIn"))) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        listaTecnicaComponenteRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        List<String> roteiroSourceLines = Files.readAllLines(roteiroRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < roteiroSourceLines.size(); lineIndex++) {
            String sourceLine = roteiroSourceLines.get(lineIndex);
            if (sourceLine.contains("Set<Roteiro>")
                    && (sourceLine.contains("customFindAllByLocationInAndMaterialOutputInFetchOperacaoRoteiroSet")
                    || sourceLine.contains("findAllByLocationIn"))) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        roteiroRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        String roteiroRepositorySource = Files.readString(roteiroRepositoryPath, StandardCharsets.UTF_8);
        if (!roteiroRepositorySource.contains("SELECT DISTINCT r FROM Roteiro r")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    roteiroRepositoryPath,
                    0,
                    "RoteiroRepository deve usar SELECT DISTINCT no fetch join de operacoes."));
        }
        if (!roteiroRepositorySource.contains("List<Roteiro> customFindAllForFront()")
                || !roteiroRepositorySource.contains("LEFT JOIN FETCH r.location")
                || !roteiroRepositorySource.contains("LEFT JOIN FETCH r.materialOutput")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    roteiroRepositoryPath,
                    0,
                    "RoteiroRepository.customFindAllForFront() deve carregar location e material output em fetch join."));
        }
        int consistencyDiagnosticMethodIndex = roteiroRepositorySource.indexOf(
                "List<Roteiro> customFindAllForConsistencyDiagnostic()");
        if (consistencyDiagnosticMethodIndex < 0
                || roteiroRepositorySource.lastIndexOf(
                        "LEFT JOIN FETCH r.materialOutput", consistencyDiagnosticMethodIndex) < 0
                || roteiroRepositorySource.lastIndexOf(
                        "LEFT JOIN FETCH r.operacaoRoteiroSet", consistencyDiagnosticMethodIndex) < 0) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    roteiroRepositoryPath,
                    0,
                    "RoteiroRepository.customFindAllForConsistencyDiagnostic() deve carregar material output e operacoes em fetch join."));
        }

        List<String> operacaoRoteiroSourceLines =
                Files.readAllLines(operacaoRoteiroRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < operacaoRoteiroSourceLines.size(); lineIndex++) {
            String sourceLine = operacaoRoteiroSourceLines.get(lineIndex);
            if (sourceLine.contains("Set<OperacaoRoteiro>")
                    && sourceLine.contains("findAllByOperacaoRoteiroCompositeKeyRoteiroLocationInAndOperacaoRoteiroCompositeKeyRoteiroMaterialOutputIn")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        operacaoRoteiroRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Repositories de BOM, componentes, roteiro e operacoes devem preservar cardinalidade em List "
                        + "e evitar deduplicacao antes das validations:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityTransportationLaneRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path linhaTransporteRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/network/supplynetwork/repository/LinhaTransporteRepository.java");
        Path linhaTransporteProdutoRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/network/supplynetwork/repository/LinhaTransporteProdutoRepository.java");
        Path supplyNetworkProjectionFactoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/network/supplynetwork/projection/SupplyNetworkProjectionFactory.java");
        List<String> violations = new ArrayList<>();

        /*
         * SupplyNetworkProjectionFactory valida a chave composta da lane antes
         * de montar mapas de inbound/outbound. O repository deve preservar
         * cardinalidade em List e a factory nao pode reduzir o snapshot para
         * Set antes dessa validation.
         */
        List<String> repositorySourceLines =
                Files.readAllLines(linhaTransporteRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < repositorySourceLines.size(); lineIndex++) {
            String sourceLine = repositorySourceLines.get(lineIndex);
            if (sourceLine.contains("Set<LinhaTransporte>")
                    && (sourceLine.contains("findByLinhaTransporteCompositeKeyVersaoAndLinhaTransporteCompositeKeyLocationOrigemInAndLinhaTransporteCompositeKeyLocationDestinoIn")
                    || sourceLine.contains("findByLinhaTransporteCompositeKeyVersaoIdInAndLinhaTransporteCompositeKeyLocationOrigemIdInAndLinhaTransporteCompositeKeyLocationDestinoIdIn"))) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        linhaTransporteRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        List<String> linhaTransporteProdutoRepositorySourceLines =
                Files.readAllLines(linhaTransporteProdutoRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < linhaTransporteProdutoRepositorySourceLines.size(); lineIndex++) {
            String sourceLine = linhaTransporteProdutoRepositorySourceLines.get(lineIndex);
            if (sourceLine.contains("Set<LinhaTransporteProduto>")
                    && (sourceLine.contains("findByLinhaTransporteProdutoCompositeKeyLinhaTransporteCompositeKeyLocationOrigemInOrLinhaTransporteProdutoCompositeKeyLinhaTransporteCompositeKeyLocationDestinoInWhereProdutoIn")
                    || sourceLine.contains("findByLinhaTransporteProdutoCompositeKeyVersaoIdInAndLinhaTransporteProdutoCompositeKeyProdutoIdInAndLinhaTransporteProdutoCompositeKeyLocationOrigemIdInAndLinhaTransporteProdutoCompositeKeyLocationDestinoIdIn"))) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        linhaTransporteProdutoRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        List<String> factorySourceLines =
                Files.readAllLines(supplyNetworkProjectionFactoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < factorySourceLines.size(); lineIndex++) {
            String sourceLine = factorySourceLines.get(lineIndex);
            if (sourceLine.contains("Set<LinhaTransporte> linhaTransporteSet")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        supplyNetworkProjectionFactoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Repositories de transportation lane e SupplyNetworkProjectionFactory devem preservar "
                        + "cardinalidade das lanes em List ate as validations de chave:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communitySimpleProductionVersionRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path versaoProducaoSimplesRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/production/productionversion/repository/VersaoProducaoSimplesRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * SupplyNetworkProjectionFactory valida ids de versoes simples antes de
         * misturar snapshots persistidos com versoes temporarias. O repository
         * deve preservar cardinalidade em List, manter DISTINCT nos fetch joins
         * e nao reintroduzir lookup residual sem consumidor produtivo.
         */
        List<String> sourceLines = Files.readAllLines(versaoProducaoSimplesRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<VersaoProducaoSimples>")
                    && sourceLine.contains(
                            "findByVersaoProducaoCompositeKeyRoteiroLocationInAndVersaoProducaoCompositeKeyRoteiroMaterialOutputIn")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        versaoProducaoSimplesRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
            if (sourceLine.contains("findByRoteiroLocationIdInAndRoteiroMaterialOutputIdIn")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        versaoProducaoSimplesRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        String repositorySource =
                Files.readString(versaoProducaoSimplesRepositoryPath, StandardCharsets.UTF_8);
        if (!repositorySource.contains("SELECT DISTINCT vp FROM VersaoProducaoSimples vp")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    versaoProducaoSimplesRepositoryPath,
                    0,
                    "VersaoProducaoSimplesRepository deve usar SELECT DISTINCT nos fetch joins."));
        }
        if (!repositorySource.contains("customFindAllForIntegrationExport()")
                || !repositorySource.contains("LEFT JOIN FETCH vp.location")
                || !repositorySource.contains("LEFT JOIN FETCH vp.materialOutput")
                || !repositorySource.contains("LEFT JOIN FETCH vp.roteiro")
                || !repositorySource.contains("LEFT JOIN FETCH vp.listaTecnica")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    versaoProducaoSimplesRepositoryPath,
                    0,
                    "Export de VersaoProducaoSimples deve buscar suas referencias em uma unica consulta."));
        }

        assertTrue(
                violations.isEmpty(),
                "VersaoProducaoSimplesRepository deve preservar cardinalidade em List, "
                        + "evitar duplicidade artificial dos fetch joins e nao reintroduzir lookup residual:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityInventoryPolicyRepositoryShouldUseDistinctOnMaterialLocationFetchJoin()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path politicaEstoquesRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/inventory/inventorypolicy/repository/PoliticaEstoquesRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * A leitura individual de politica carrega a lista material/location para
         * montar o DTO completo da tela. DISTINCT evita que o fetch join de
         * colecao replique a entidade raiz, mantendo a consistencia com o
         * snapshot amplo customFindAllWithMaterialLocation().
         */
        String repositorySource = Files.readString(politicaEstoquesRepositoryPath, StandardCharsets.UTF_8);
        if (!repositorySource.contains("SELECT DISTINCT pe FROM PoliticaEstoques pe")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    politicaEstoquesRepositoryPath,
                    0,
                    "PoliticaEstoquesRepository.customFindById(...) deve usar SELECT DISTINCT no fetch join."));
        }
        if (repositorySource.contains("SELECT pe FROM PoliticaEstoques pe")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    politicaEstoquesRepositoryPath,
                    0,
                    "PoliticaEstoquesRepository.customFindById(...) nao deve voltar para SELECT sem DISTINCT."));
        }

        assertTrue(
                violations.isEmpty(),
                "PoliticaEstoquesRepository deve evitar duplicidade artificial no fetch join "
                        + "de material/location:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityInventoryPolicyProfileLinkRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path perfilExecucaoPoliticaEstoquesRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/configuration/repository/PerfilExecucaoPoliticaEstoquesRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * PoliticaEstoquesProjectionFactory valida ids duplicados de politicas
         * vinculadas ao perfil antes de ordenar por prioridade e popular o
         * mapa vigente. O repository deve preservar cardinalidade em List, mas
         * manter DISTINCT porque o fetch join das regras material/location
         * multiplica linhas da mesma entidade raiz.
         */
        List<String> sourceLines =
                Files.readAllLines(perfilExecucaoPoliticaEstoquesRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<PerfilExecucaoPoliticaEstoques>")
                    && sourceLine.contains("customFindByPerfilExecucaoSupplyPlan")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        perfilExecucaoPoliticaEstoquesRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        String repositorySource =
                Files.readString(perfilExecucaoPoliticaEstoquesRepositoryPath, StandardCharsets.UTF_8);
        if (!repositorySource.contains("SELECT DISTINCT pepe FROM PerfilExecucaoPoliticaEstoques pepe")) {
            violations.add(formatViolation(
                    communityWorkspaceDirectory,
                    perfilExecucaoPoliticaEstoquesRepositoryPath,
                    0,
                    "PerfilExecucaoPoliticaEstoquesRepository.customFindByPerfilExecucaoSupplyPlan(...) deve usar SELECT DISTINCT no fetch join."));
        }

        assertTrue(
                violations.isEmpty(),
                "PerfilExecucaoPoliticaEstoquesRepository.customFindByPerfilExecucaoSupplyPlan(...) "
                        + "deve preservar cardinalidade em List e evitar duplicidade artificial do fetch join:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityProductionPlanLineRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path productionPlanLinhaRepositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/productionplan/repository/ProductionPlanLinhaRepository.java");
        Path supplyPlanProjectionFactoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/supplyplan/projection/SupplyPlanProjectionFactory.java");
        List<String> violations = new ArrayList<>();

        /*
         * SupplyPlanProjectionFactory valida chave composta duplicada das
         * linhas de Production Plan antes de popular outputs e inputs
         * produtivos. O repository deve preservar cardinalidade em List, mas
         * manter DISTINCT porque o fetch join de componentes de BOM pode
         * multiplicar a entidade raiz retornada pelo JPA.
         */
        List<String> sourceLines = Files.readAllLines(productionPlanLinhaRepositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<ProductionPlanLinha>")
                    && (sourceLine.contains("customFindByProductionPlanLinhaCompositeKeySupplyPlan")
                    || sourceLine.contains("customFindByProductionPlanLinhaCompositeKeySupplyPlanAndProductionPlanLinhaCompositeKeyLocationIncluindoListaTecnicaEMateriaisInput"))) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        productionPlanLinhaRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
            if (sourceLine.contains("SELECT ppl FROM ProductionPlanLinha ppl")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        productionPlanLinhaRepositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        List<String> factorySourceLines = Files.readAllLines(supplyPlanProjectionFactoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < factorySourceLines.size(); lineIndex++) {
            String sourceLine = factorySourceLines.get(lineIndex);
            if (sourceLine.contains("Set<ProductionPlanLinha> productionPlanLinhas")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        supplyPlanProjectionFactoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        assertTrue(
                violations.isEmpty(),
                "ProductionPlanLinhaRepository deve preservar cardinalidade em List "
                        + "ate a validation da SupplyPlanProjectionFactory, que tambem nao pode "
                        + "deduplicar ProductionPlanLinha em Set antes da validation, e manter DISTINCT "
                        + "nos fetch joins:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communitySchedulerHistoryRepositoryShouldPreserveSnapshotCardinalityUntilValidation()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path repositoryPath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/platform/scheduler/repository/ScheduledTaskAbstractRepository.java");
        List<String> violations = new ArrayList<>();

        /*
         * Process Status monta DTOs a partir de tasks e execucoes carregadas em lote.
         * O repository deve retornar List para nao deduplicar a fotografia antes da
         * validation do service, mas manter DISTINCT porque o fetch join da colecao
         * de execucoes multiplica a entidade raiz no resultado JPA.
         */
        List<String> sourceLines = Files.readAllLines(repositoryPath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.contains("Set<ScheduledTaskAbstract>")
                    && sourceLine.contains("customFindAllComDetalhes")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        repositoryPath,
                        lineIndex,
                        sourceLine));
            }
            if (sourceLine.contains("SELECT st FROM ScheduledTaskAbstract st")) {
                violations.add(formatViolation(
                        communityWorkspaceDirectory,
                        repositoryPath,
                        lineIndex,
                        sourceLine));
            }
        }

        assertTrue(
                violations.isEmpty(),
                "ScheduledTaskAbstractRepository.customFindAllComDetalhes() deve retornar List "
                        + "e usar SELECT DISTINCT no fetch join de execucoes:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityBuildAndPropertiesShouldNotReferenceForbiddenPrivateCapabilities() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O Community precisa continuar compilavel/publicavel sem conectores e
         * bibliotecas privadas Enterprise. A busca fica restrita a POM/properties
         * porque codigo e testes podem mencionar esses dominios em comentarios,
         * validacoes defensivas ou nomes de enums compartilhados com o front.
         */
        for (Path configurationPath : findWorkspaceConfigurationFiles(communityWorkspaceDirectory)) {
            List<String> configurationLines = Files.readAllLines(configurationPath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < configurationLines.size(); lineIndex++) {
                String configurationLine = configurationLines.get(lineIndex);
                if (containsForbiddenBuildOrPropertyToken(configurationLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, configurationPath, lineIndex, configurationLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao pode declarar dependencias/properties de capacidades privadas:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityCodeAndConfigurationShouldUseCommunityEnterpriseNaming() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * A nomenclatura publica dos repositorios novos e Community/Enterprise.
         * Termos antigos de recorte ou licenciamento nao devem voltar para Java,
         * Maven, properties ou README como nomes operacionais de edicao,
         * exception ou capability.
         */
        List<Path> inspectedPaths = new ArrayList<>(findWorkspaceFiles(communityWorkspaceDirectory, ".java"));
        inspectedPaths.addAll(findWorkspaceConfigurationFiles(communityWorkspaceDirectory));
        inspectedPaths.addAll(findWorkspaceDocumentationFiles(communityWorkspaceDirectory));

        for (Path sourcePath : inspectedPaths) {
            List<String> sourceLines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if ((containsLegacyEditionNamingToken(sourceLine)
                        && !isApprovedSourceAvailableLicenseStatement(sourceLine))
                        || containsForbiddenCommercialEditionTerm(sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, sourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve usar Community/Enterprise como nomenclatura de edicao:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityWorkspaceShouldNotContainEnterpriseOnlyRootModules() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Alguns nomes podem aparecer em entidades transicionais ou comentarios,
         * mas os modulos privados inteiros nao devem ser publicados dentro do
         * repositorio Community. Esse teste olha apenas diretorios raiz para nao
         * confundir DistributionPlanItem interno do heuristico com o modulo Enterprise
         * de Distribution.
         */
        try (Stream<Path> pathStream = Files.list(communityWorkspaceDirectory)) {
            pathStream
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(COMMUNITY_FORBIDDEN_ROOT_MODULE_DIRECTORIES::contains)
                    .forEach(violations::add);
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve conter modulos raiz 100% Enterprise:\n" + String.join("\n", violations));

    }

    @Test
    void communityBackendModulesShouldNotPackageLegacyFrontendResources() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O novo front Community/Enterprise roda em servidor separado. O backend
         * Community deve publicar APIs e arquivos de configuracao, nao templates
         * Thymeleaf, scripts ou assets do front legado em nenhum modulo.
         */
        try (Stream<Path> pathStream = Files.walk(communityWorkspaceDirectory)) {
            pathStream
                    .filter(path -> !isTargetFile(path))
                    .filter(CommunityArchitectureBoundaryTest::isMainResourcesPath)
                    .filter(CommunityArchitectureBoundaryTest::isLegacyFrontendResourcePath)
                    .map(communityWorkspaceDirectory::relativize)
                    .map(Path::toString)
                    .forEach(violations::add);
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve empacotar front legado em modulos backend:\n" + String.join("\n", violations));

    }

    @Test
    void communityBackendModulesShouldNotVersionFrontendDistributionArtifacts() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O dist do front Community sera produzido pelo pipeline do front e
         * empacotado fora do repositorio backend. Versionar esse diretorio aqui
         * misturaria codigo-fonte Java com artefato compilado de SPA e recriaria
         * a confusao que a separacao de servidores tentou eliminar.
         */
        try (Stream<Path> pathStream = Files.walk(communityWorkspaceDirectory)) {
            pathStream
                    .filter(path -> !isTargetFile(path))
                    .filter(Files::isDirectory)
                    .filter(path -> FRONTEND_DISTRIBUTION_DIRECTORY_NAME.equalsIgnoreCase(
                            path.getFileName().toString()))
                    .map(communityWorkspaceDirectory::relativize)
                    .map(Path::toString)
                    .forEach(violations::add);
        }

        assertTrue(
                violations.isEmpty(),
                "Backend Community nao deve versionar diretorios dist do front:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityWebModuleShouldNotPublishLegacyViewControllers() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningWebJavaDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/web/restcontroller");
        List<String> violations = new ArrayList<>();

        /*
         * RestControllers seguem permitidos: o backend Community publica APIs.
         * O que fica bloqueado aqui e a volta de controllers de view,
         * ModelAndView ou configuracao Thymeleaf do front legado.
         */
        for (Path javaSourcePath : findWorkspaceFiles(planningWebJavaDirectory, ".java")) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (containsLegacyFrontendWebCodeToken(sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve publicar controllers de view/front legado:\n" + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotUseGenericImplementationMarkersOrEmptyRuntimeExceptions() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Codigo aberto precisa falhar com mensagens de contrato que expliquem
         * se o caso e payload invalido, dado operacional ausente ou capability
         * Enterprise bloqueada. Marcadores genericos e excecoes sem mensagem
         * fazem a fronteira Community parecer incompleta e dificultam suporte
         * de usuarios externos.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_FORBIDDEN_EMPTY_RUNTIME_EXCEPTION_PATTERN.matcher(sourceLine).matches()
                        || COMMUNITY_FORBIDDEN_FATAL_ERROR_PATTERN.matcher(sourceLine).matches()
                        || containsForbiddenGenericImplementationMarker(javaSourcePath, sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve substituir marcadores genericos por mensagens de contrato explicitas:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldKeepOnlyDocumentedGenericExceptionCatches() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Map<String, Integer> genericExceptionCatchCountByPath = new TreeMap<>();

        /*
         * Este teste nao proibe as bordas historicas que ainda declaram checked
         * exceptions, mas torna a excecao explicita: qualquer catch generico novo
         * precisa estreitar o tipo capturado ou atualizar este contrato com a
         * justificativa do recorte.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            int genericExceptionCatchCount = 0;
            for (String sourceLine : sourceLines) {
                if (sourceLine.contains("catch (Exception")) {
                    genericExceptionCatchCount++;
                }
            }

            if (genericExceptionCatchCount > 0) {
                String relativeSourcePath = communityWorkspaceDirectory
                        .relativize(javaSourcePath)
                        .toString()
                        .replace('\\', '/');
                genericExceptionCatchCountByPath.put(relativeSourcePath, genericExceptionCatchCount);
            }
        }

        Assertions.assertEquals(
                COMMUNITY_ALLOWED_GENERIC_EXCEPTION_CATCH_COUNTS,
                genericExceptionCatchCountByPath,
                "Community deve manter apenas os catch (Exception) produtivos ja documentados");

    }

    @Test
    void communityWebControllersShouldNotUseWildcardResponseEntity() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningWebJavaDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/web/restcontroller");
        List<String> violations = new ArrayList<>();

        /*
         * O repositorio aberto precisa publicar contratos REST legiveis. Quando
         * o payload e uma mensagem/status, usar ResponseDTO; quando e dado de
         * dominio, declarar o DTO/lista concreto no ResponseEntity.
         */
        for (Path javaSourcePath : findWorkspaceFiles(planningWebJavaDirectory, ".java")) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_WEB_RESPONSE_ENTITY_WILDCARD_PATTERN.matcher(sourceLine).matches()) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community web controllers devem declarar ResponseEntity com payload concreto:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityWebSecuredAnnotationsShouldUseOnlyAdminRole() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningWebJavaDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/web");
        List<String> violations = new ArrayList<>();

        // A edicao Community possui login simples e apenas ROLE_ADMIN. As tres
        // anotacoes legadas de execucao sao excecoes estritamente compatíveis e
        // nao materializam roles no login Community.
        for (Path javaSourcePath : findWorkspaceFiles(planningWebJavaDirectory, ".java")) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (sourceLine.trim().startsWith("@Secured")
                        && !COMMUNITY_ADMIN_SECURED_PATTERN.matcher(sourceLine).matches()
                        && !isApprovedLegacyExecutionControllerAnnotation(
                        communityWorkspaceDirectory, javaSourcePath, sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve publicar endpoints com ROLE_ADMIN, exceto as tres anotacoes legadas de execucao aprovadas:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityWebSecuredEndpointMappingsShouldDeclareAdminSecurityAnnotation() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path restControllerJavaDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/web/restcontroller");
        List<String> violations = new ArrayList<>();

        /*
         * Endpoints abertos devem ficar sob `api/open` e ser revisados caso a
         * caso. Qualquer rota `api/secured` em controller Community precisa
         * declarar explicitamente ROLE_ADMIN no metodo, porque nao existe
         * matriz granular de permissoes nesta edicao.
         */
        for (Path javaSourcePath : findWorkspaceFiles(restControllerJavaDirectory, ".java")) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (!containsSpringMappingAnnotation(sourceLine)) {
                    continue;
                }

                String methodAnnotationBlock = collectMethodAnnotationBlock(sourceLines, lineIndex);
                if (methodAnnotationBlock.contains("api/secured")
                        && !isGlobalAuthenticatedLegacyEndpoint(methodAnnotationBlock)
                        && !(methodAnnotationBlock.contains("@Secured(\"ROLE_ADMIN\")")
                        || methodAnnotationBlock.contains(
                        "@Secured(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)")
                        || isApprovedLegacyExecutionControllerEndpoint(
                        communityWorkspaceDirectory, javaSourcePath, methodAnnotationBlock))) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Endpoints api/secured Community devem declarar ROLE_ADMIN no metodo, salvo as tres anotacoes legadas aprovadas:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityDataUploadDynamicMappingsShouldRemainAdminOnly() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path dataUploadControllerJavaDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/web/restcontroller/dataupload");
        Path integrationControllerAbstractPath = dataUploadControllerJavaDirectory.resolve(
                "IntegrationControllerAbstract.java");
        List<String> violations = new ArrayList<>();

        /*
         * Data upload e o unico ponto Community que registra endpoints dinamicos
         * via RequestMappingHandlerMapping, portanto a regra de seguranca nao
         * aparece como @Secured no metodo publicado. A abstracao deve continuar
         * exigindo ROLE_ADMIN para GET/POST e as subclasses Community nao devem
         * reabrir a matriz granular de permissoes que pertence ao Enterprise.
         */
        String integrationControllerAbstractSource = Files.readString(
                integrationControllerAbstractPath,
                StandardCharsets.UTF_8);
        if (!COMMUNITY_INTEGRATION_ADMIN_GET_ROLE_PATTERN.matcher(integrationControllerAbstractSource).find()) {
            violations.add(communityWorkspaceDirectory.relativize(integrationControllerAbstractPath)
                    + ": getUserRoleTypesGet deve retornar somente UserRoleType.ROLE_ADMIN");
        }
        if (!COMMUNITY_INTEGRATION_ADMIN_POST_ROLE_PATTERN.matcher(integrationControllerAbstractSource).find()) {
            violations.add(communityWorkspaceDirectory.relativize(integrationControllerAbstractPath)
                    + ": getUserRoleTypesPost deve retornar somente UserRoleType.ROLE_ADMIN");
        }

        for (Path javaSourcePath : findWorkspaceFiles(dataUploadControllerJavaDirectory, ".java")) {
            if ("IntegrationControllerAbstract.java".equals(javaSourcePath.getFileName().toString())) {
                continue;
            }

            String source = Files.readString(javaSourcePath, StandardCharsets.UTF_8);
            if (source.contains("extends IntegrationControllerAbstract")
                    && (source.contains("getUserRoleTypesGet")
                    || source.contains("getUserRoleTypesPost"))) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Endpoints dinamicos de data upload Community devem permanecer admin-only:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityWebOpenEndpointMappingsShouldStayInApprovedSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path restControllerJavaDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/web/restcontroller");
        List<String> violations = new ArrayList<>();

        /*
         * Rotas abertas Community devem ser excecoes pequenas e nomeadas. Hoje
         * existem apenas o bootstrap do primeiro usuario e a runtime-info usada
         * pelo front compartilhado para identificar a edicao em execucao.
         */
        for (Path javaSourcePath : findWorkspaceFiles(restControllerJavaDirectory, ".java")) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (!containsSpringMappingAnnotation(sourceLine)) {
                    continue;
                }

                String methodAnnotationBlock = collectMethodAnnotationBlock(sourceLines, lineIndex);
                if (methodAnnotationBlock.contains("api/open")
                        && !containsAllowedOpenEndpointPath(methodAnnotationBlock)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Endpoints api/open Community devem ficar no conjunto aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotDeclareNonAdminRoles() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Testes podem usar roles Enterprise como entradas invalidas. Codigo
         * principal Community nao deve declarar novas authorities ROLE_* fora
         * das tres anotacoes legadas de compatibilidade em controllers.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            boolean insideBlockComment = false;
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                JavaCodeSnippet javaCodeSnippet = removeJavaCommentsFromLine(
                        sourceLines.get(lineIndex),
                        insideBlockComment);
                insideBlockComment = javaCodeSnippet.insideBlockComment();
                Matcher springSecurityRoleMatcher = SPRING_SECURITY_ROLE_PATTERN.matcher(javaCodeSnippet.code());
                while (springSecurityRoleMatcher.find()) {
                    String springSecurityRole = springSecurityRoleMatcher.group();
                    if (!"ROLE_ADMIN".equals(springSecurityRole)
                            && !isApprovedLegacyExecutionControllerAnnotation(
                            communityWorkspaceDirectory, javaSourcePath, javaCodeSnippet.code())) {
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
                "Community nao deve declarar roles funcionais alem das tres anotacoes legadas aprovadas:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldUseSecurityConstantForFunctionalAdminRoleChecks() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * @Secured aceita constante de compilacao; o valor Community central e
         * a origem preferida. O literal continua aceito para os controllers
         * anteriores ja cobertos, mas qualquer outra logica funcional deve usar
         * a constante para evitar drift entre services/controllers.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)
                    || "CommunitySecurityConstants.java".equals(javaSourcePath.getFileName().toString())) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (sourceLine.contains("\"ROLE_ADMIN\"")
                        && !COMMUNITY_ADMIN_SECURED_PATTERN.matcher(sourceLine).matches()
                        && !isApprovedLegacyExecutionControllerAnnotation(
                        communityWorkspaceDirectory, javaSourcePath, sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Checks funcionais Community devem usar CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE, salvo as tres anotacoes legadas aprovadas:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityWebBootstrapShouldUseOnlyCommunityDefaultsAndMariaDbProfile() throws IOException {

        Properties applicationProperties = loadWorkspaceProperties("src/main/resources/application.properties");

        Assertions.assertEquals(
                COMMUNITY_CONFIG_IMPORT,
                applicationProperties.getProperty("spring.config.import"));
        Assertions.assertEquals(
                "prd,database-mariadb",
                applicationProperties.getProperty("spring.profiles.active"));

    }

    @Test
    void communityPropertiesShouldNotUseEditionSwitch() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * A edicao Community e o default natural quando apenas os artefatos
         * abertos estao no classpath. Nao deve existir property de edicao para
         * alternar comportamento em runtime.
         */
        for (Path configurationPath : findWorkspaceConfigurationFiles(communityWorkspaceDirectory)) {
            List<String> configurationLines = Files.readAllLines(configurationPath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < configurationLines.size(); lineIndex++) {
                String configurationLine = configurationLines.get(lineIndex);
                if (configurationLine.contains("opsfactor.edition")) {
                    violations.add(formatViolation(communityWorkspaceDirectory, configurationPath, lineIndex, configurationLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve usar property de edicao para alternar runtime:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityPublishedConfigurationShouldNotContainEmbeddedCredentialsOrCustomerHosts() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O repositorio Community sera publicado. Properties e READMEs podem
         * declarar chaves de configuracao, mas valores de senha fake ativos e
         * nomes/domínios de cliente nao devem ficar versionados nem comentados.
         */
        List<Path> publishedConfigurationOrDocumentationFiles = new ArrayList<>();
        publishedConfigurationOrDocumentationFiles.addAll(findWorkspaceConfigurationFiles(communityWorkspaceDirectory));
        publishedConfigurationOrDocumentationFiles.addAll(findWorkspaceDocumentationFiles(communityWorkspaceDirectory));

        for (Path sourcePath : publishedConfigurationOrDocumentationFiles) {
            List<String> sourceLines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (containsForbiddenCustomerOrPrivateHostToken(sourceLine)
                        || isForbiddenActiveSecretPlaceholder(sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, sourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve publicar credenciais placeholder ativas nem nomes/hosts de cliente:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMariaDbProfileShouldUseCommunityDialectAndMariaDbDriver() throws IOException {

        Properties mariaDbApplicationProperties = loadWorkspaceProperties(
                "src/main/resources/application-database-mariadb.properties");

        Assertions.assertEquals(
                "org.mariadb.jdbc.Driver",
                mariaDbApplicationProperties.getProperty("spring.datasource.driver-class-name"));
        Assertions.assertEquals(
                "com.opsfactor.community.platform.database.hibernate.dialect.PlanningMariaDBDialect",
                mariaDbApplicationProperties.getProperty("spring.jpa.properties.hibernate.dialect"));
        Assertions.assertEquals(
                "${OPSFACTOR_DATASOURCE_USERNAME:opsfactor}",
                mariaDbApplicationProperties.getProperty("spring.datasource.username"));
        Assertions.assertEquals(
                "${OPSFACTOR_DATASOURCE_PASSWORD:}",
                mariaDbApplicationProperties.getProperty("spring.datasource.password"));

    }

    @Test
    void communityMainSourcesShouldNotDeclareBootStartupRunners() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * A distribuicao Community publica o servidor web e executa tasks apenas
         * quando chamadas explicitamente pelo fluxo sincronizado. Beans
         * ApplicationRunner/CommandLineRunner recriariam comportamento batch/job
         * automatico dentro do bootstrap web.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_FORBIDDEN_RUNNER_PATTERN.matcher(sourceLine).matches()) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve registrar runners automaticos de startup/batch:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityShouldNotUseProcessExecutionModeProperties() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<Path> inspectedPaths = new ArrayList<>();
        inspectedPaths.addAll(findWorkspaceFiles(communityWorkspaceDirectory, ".java"));
        inspectedPaths.addAll(findWorkspaceFiles(communityWorkspaceDirectory, ".properties"));
        inspectedPaths.addAll(findWorkspaceFiles(communityWorkspaceDirectory, ".md"));
        List<String> violations = new ArrayList<>();

        /*
         * Community roda Demand/Supply Planning e integracoes de dados apenas
         * em modo sincronizado no request. O mesmo controller compartilhado
         * pode documentar que o overlay Enterprise consulta a property para
         * ASYNC/BATCH, mas nenhum codigo ou configuracao Community pode le-la
         * ou associa-la ao runtime local.
         */
        for (Path sourcePath : inspectedPaths) {
            if (isTestSource(sourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
            boolean insideBlockComment = false;
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                String executableOrConfiguredLine = sourceLine;
                if (sourcePath.toString().endsWith(".java")) {
                    JavaCodeSnippet javaCodeSnippet = removeJavaCommentsFromLine(
                            sourceLine,
                            insideBlockComment);
                    insideBlockComment = javaCodeSnippet.insideBlockComment();
                    executableOrConfiguredLine = javaCodeSnippet.code();
                } else if (sourceLine.trim().startsWith("#") || sourceLine.trim().startsWith("!")) {
                    continue;
                }

                if (executableOrConfiguredLine.contains("opsfactor.execution_mode.planning_processes")
                        || executableOrConfiguredLine.contains("opsfactor.execution_mode.data_integration")) {
                    violations.add(formatViolation(communityWorkspaceDirectory, sourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve ler property para habilitar execucao ASYNC/BATCH; essa configuracao pertence somente ao overlay Enterprise:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityWebModuleShouldNotPublishEnterpriseModuleEndpointPaths() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningWebJavaDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/web");
        List<String> violations = new ArrayList<>();

        /*
         * Modulos 100% Enterprise nao devem voltar como paths REST no backend
         * Community. Supplynetwork e permitido porque neste repositorio significa
         * apenas cadastro operacional de malha/transportation lanes para o
         * heuristico, nao Supply Network Flows visual/mapa.
         */
        for (Path javaSourcePath : findWorkspaceFiles(planningWebJavaDirectory, ".java")) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_FORBIDDEN_ENTERPRISE_ENDPOINT_PATTERN.matcher(sourceLine).matches()) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve publicar endpoints de modulos 100% Enterprise:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotContainGisMapOrBarycenterImplementations() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Location e transportation lane podem preservar campos transicionais
         * como latitude, longitude e distancia, sempre bloqueados ou
         * neutralizados nos mappers Community. Implementacoes funcionais de
         * GIS, mapa, geovisualizacao, baricentro ou GraphHopper pertencem ao
         * Enterprise e nao devem aparecer como classes do source principal.
         *
         * A regra evita o token generico "Map" para nao confundir MapStruct,
         * Mapper e java.util.Map com a capacidade visual Enterprise.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            String fileName = javaSourcePath.getFileName().toString();
            if (containsForbiddenGisMapClassNameToken(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve conter implementacoes funcionais de GIS/mapa/baricentro:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityDataUploadControllersShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path dataUploadControllerDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/web/restcontroller/dataupload");
        List<String> violations = new ArrayList<>();

        /*
         * Data upload Community e intencionalmente pequeno: master data
         * operacional, producao operacional do heuristico, malha material,
         * sell-out quantitativo e estoque inicial. Uploads de pedidos,
         * campanhas, custos/precos, frotas, warehouses, lotes, turnos e
         * planning data pertencem ao Enterprise.
         */
        for (Path javaSourcePath : findWorkspaceFiles(dataUploadControllerDirectory, ".java")) {
            String fileName = javaSourcePath.getFileName().toString();
            if (!COMMUNITY_ALLOWED_DATA_UPLOAD_CONTROLLER_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui controller de data upload fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMasterdataControllersShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();

        /*
         * Master data REST Community fica limitada aos cadastros/listagens
         * operacionais usados por Demand/Supply Planning: material, location,
         * UOM, clusters, malha e producao basica. Controllers de warehouses,
         * frotas, rotas last mile, filtros/agregadores, caracteristicas
         * dinamicas, custos/precos ou dominios de visibilidade/distribuicao
         * pertencem ao Enterprise.
         */
        assertApprovedControllerSet(
                communityWorkspaceDirectory,
                "src/main/java/com/opsfactor/community/web/restcontroller/masterdata",
                COMMUNITY_ALLOWED_MASTERDATA_CONTROLLER_FILES,
                "master data");

    }

    @Test
    void communityConfigurationControllersShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();

        /*
         * Configuration Community publica somente parametros material/location,
         * perfis DP/Supply basicos, safety stock operacional e configuracao de
         * usuario. Auto-fit, process chain, politicas otimizadas de estoque,
         * pricing, seguranca avancada e demais configuracoes Enterprise devem
         * nascer no repositorio Enterprise.
         */
        assertApprovedControllerSet(
                communityWorkspaceDirectory,
                "src/main/java/com/opsfactor/community/web/restcontroller/configuration",
                COMMUNITY_ALLOWED_CONFIGURATION_CONTROLLER_FILES,
                "configuration");

    }

    @Test
    void communityPlanningControllersShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();

        /*
         * Planning Community fica restrito a Demand Planning, Demand Analysis,
         * Supply Planning heuristico e constrained plan. Distribution,
         * visibility, pricing, agent, line scheduling, optimizer/process chain,
         * constraint tracker e demais analiticos Enterprise nao devem aparecer
         * como controllers neste modulo.
         */
        assertApprovedControllerSet(
                communityWorkspaceDirectory,
                "src/main/java/com/opsfactor/community/web/restcontroller/planning",
                COMMUNITY_ALLOWED_PLANNING_CONTROLLER_FILES,
                "planning");

    }

    @Test
    void communityAdminRuntimeAndRootControllersShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();

        /*
         * Admin/runtime Community sao superficies pequenas: admin simples,
         * runtime info e process status. SSO, lockout/unlock, tenants, async
         * queues, observabilidade privada ou endpoints operacionais de nuvem
         * pertencem ao Enterprise.
         */
        assertApprovedControllerSet(
                communityWorkspaceDirectory,
                "src/main/java/com/opsfactor/community/web/restcontroller/admin",
                COMMUNITY_ALLOWED_ADMIN_CONTROLLER_FILES,
                "admin");
        assertApprovedControllerSet(
                communityWorkspaceDirectory,
                "src/main/java/com/opsfactor/community/web/restcontroller/runtime",
                COMMUNITY_ALLOWED_RUNTIME_CONTROLLER_FILES,
                "runtime");
        assertApprovedControllerSet(
                communityWorkspaceDirectory,
                "src/main/java/com/opsfactor/community/web/restcontroller",
                COMMUNITY_ALLOWED_ROOT_REST_CONTROLLER_FILES,
                "root restcontroller");

    }

    @Test
    void communityIntegrationServicesShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path integrationServiceDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        /*
         * Services de integracao Community ficam limitados aos cadastros
         * necessarios para Demand/Supply Planning Community. Produção basica
         * permanece aqui porque o heuristico depende de roteiros, BOM,
         * recursos e versao simples; line scheduling, custos, turnos,
         * manutencao, versoes paralelas, pedidos, eventos e valores devem ser
         * implementados no Enterprise. `OperacaoRoteiroIntegrationService`
         * nao possui mapper proprio porque a operacao e materializada pelo
         * mapper de roteiro.
         */
        for (Path javaSourcePath : findWorkspaceFiles(integrationServiceDirectory, "IntegrationService.java")) {
            String fileName = javaSourcePath.getFileName().toString();
            if (!COMMUNITY_ALLOWED_INTEGRATION_SERVICE_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui service de integracao fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityIntegrationMappersShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path integrationMapperDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        /*
         * Mappers sao a fronteira onde um payload Enterprise poderia ser aceito
         * silenciosamente. A lista abaixo congela apenas os mappers Community;
         * campos transicionais compartilhados devem rejeitar valores
         * Enterprise explicitamente nos testes especificos de cada mapper.
         */
        for (Path javaSourcePath : findWorkspaceFiles(integrationMapperDirectory, "IntegrationMapper.java")) {
            String fileName = javaSourcePath.getFileName().toString();
            if (!COMMUNITY_ALLOWED_INTEGRATION_MAPPER_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui mapper de integracao fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityIntegrationDtosShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path integrationDtoDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        /*
         * DTOs de integracao sao contrato publico de data upload/API JSON. Esta
         * guarda complementa as allowlists de controllers, services e mappers:
         * novos contratos de pedidos, campanhas, custos/precos, frotas,
         * warehouses, lotes, turnos, planning data ou demais dominios
         * Enterprise nao devem entrar no Community por simples copia do legado.
         * Campos transicionais dentro de DTOs aprovados continuam sendo
         * tratados pelos testes especificos dos respectivos mappers.
         */
        for (Path javaSourcePath : findWorkspaceFiles(integrationDtoDirectory, ".java")) {
            String normalizedSourcePath = javaSourcePath.toString().replace('\\', '/');
            if (!normalizedSourcePath.contains("/integration/dto/")) {
                continue;
            }
            String fileName = javaSourcePath.getFileName().toString();
            if (!COMMUNITY_ALLOWED_INTEGRATION_DTO_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui DTO de integracao fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityModelProjectionsShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path modelProjectionDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        /*
         * Projections sao a fronteira de dados em memoria usada pelos calculos.
         * Uma projection Enterprise copiada para o Community pode reabrir acesso
         * a custos, pedidos, campanhas, filtros/agregadores, otimizador ou
         * caracteristicas antes de qualquer controller perceber. Por isso a
         * lista fica positiva: nova projection Community exige decisao explicita.
         */
        for (Path javaSourcePath : findWorkspaceFiles(modelProjectionDirectory, ".java")) {
            String projectionRelativePath = modelProjectionDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!projectionRelativePath.contains("/projection/")) {
                continue;
            }
            if (!containsAllowedFileName(
                    COMMUNITY_ALLOWED_MODEL_PROJECTION_FILES,
                    javaSourcePath.getFileName().toString())) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui projection fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityModelRepositoriesShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path modelRepositoryDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        /*
         * Repositories sao a fronteira fisica de acesso a tabelas. Um repository
         * Enterprise copiado para o Community pode reabrir entidades privadas
         * mesmo que controllers/services ainda estejam ausentes.
         */
        for (Path javaSourcePath : findWorkspaceFiles(modelRepositoryDirectory, ".java")) {
            String repositoryRelativePath = modelRepositoryDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!repositoryRelativePath.contains("/repository/")) {
                continue;
            }
            if (!containsAllowedFileName(
                    COMMUNITY_ALLOWED_MODEL_REPOSITORY_FILES,
                    javaSourcePath.getFileName().toString())) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui repository fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityModelDomainShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path modelDomainDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        /*
         * Entidades sao a fronteira mais sensivel do repo aberto: elas revelam
         * schema e modelagem. Algumas classes permanecem por transicao de
         * schema, como VersaoProducaoParalela e colunas antigas de planning
         * data, mas a lista precisa ser deliberada e documentada.
         */
        for (Path javaSourcePath : findWorkspaceFiles(modelDomainDirectory, ".java")) {
            String domainRelativePath = modelDomainDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!domainRelativePath.contains("/domain/")) {
                continue;
            }
            if (!containsAllowedFileName(
                    COMMUNITY_ALLOWED_MODEL_DOMAIN_FILES,
                    javaSourcePath.getFileName().toString())) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui domain/entity fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityServiceAndTaskBeansShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningServicesDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        /*
         * Services e tasks sao pontos de comportamento. DTOs e mappers podem ser
         * compartilhados para compatibilidade, mas um novo Service/Task costuma
         * representar uma capacidade funcional nova e deve passar pelo recorte.
         */
        for (Path javaSourcePath : findWorkspaceFiles(planningServicesDirectory, ".java")) {
            String fileName = javaSourcePath.getFileName().toString();
            if (!fileName.endsWith("Service.java") && !fileName.endsWith("Task.java")) {
                continue;
            }

            if (!containsAllowedFileName(COMMUNITY_ALLOWED_SERVICE_OR_TASK_FILES, fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui service/task fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityServiceDtosShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningServicesDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        /*
         * Estes DTOs sao contratos REST/front internos ao modulo services. DTOs
         * compartilhados podem carregar campos Enterprise bloqueaveis, mas o
         * conjunto de contratos publicados pelo Community deve permanecer
         * rastreavel.
         */
        for (Path javaSourcePath : findWorkspaceFiles(planningServicesDirectory, ".java")) {
            String fileName = javaSourcePath.getFileName().toString();
            if (!fileName.endsWith("DTO.java") && !fileName.endsWith("Dto.java")) {
                continue;
            }

            if (!containsAllowedFileName(COMMUNITY_ALLOWED_SERVICE_DTO_FILES, fileName)
                    && !COMMUNITY_ALLOWED_INTEGRATION_DTO_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui DTO de services fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityServiceMappersShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningServicesDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability");
        List<String> violations = new ArrayList<>();

        /*
         * Mappers de services fazem a ponte entre entidades/projections e DTOs
         * internos consumidos pelos controllers. Como eles determinam quais
         * campos chegam ao front, qualquer mapper novo precisa ser uma decisao
         * explicita do recorte Community.
         */
        for (Path javaSourcePath : findWorkspaceFiles(planningServicesDirectory, ".java")) {
            String fileName = javaSourcePath.getFileName().toString();
            if (!fileName.endsWith("Mapper.java")) {
                continue;
            }

            if (!containsAllowedFileName(COMMUNITY_ALLOWED_SERVICE_MAPPER_FILES, fileName)
                    && !COMMUNITY_ALLOWED_INTEGRATION_MAPPER_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui mapper de services fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityRoutinesShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path routinesSourceDirectory = communityWorkspaceDirectory.resolve("src/main/java");
        List<String> violations = new ArrayList<>();

        /*
         * Rotinas concentram calculos de Demand/Supply. O Community deve manter
         * apenas estatistico operacional, heuristico, low level code e
         * agregacao DFU; optimizer, finance, pricing e modelos Enterprise nao
         * podem retornar por copia de pacote.
         */
        for (Path javaSourcePath : findWorkspaceFiles(routinesSourceDirectory, ".java")) {
            String routineRelativePath = routinesSourceDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!routineRelativePath.startsWith("com/opsfactor/community/planning/routines/")) {
                continue;
            }
            if (!COMMUNITY_ALLOWED_ROUTINES_FILES.contains(routineRelativePath)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui rotina fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityRInstanceShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path rInstanceSourceDirectory = communityWorkspaceDirectory.resolve("src/main/java");
        List<String> violations = new ArrayList<>();

        /*
         * A integracao R Community deve expor somente o caller estatistico
         * restrito e seu resultado. Implementacoes de STL/Chronos/Prophet,
         * regressoras externas, auto-fit ou foundation models pertencem ao
         * Enterprise.
         */
        for (Path javaSourcePath : findWorkspaceFiles(rInstanceSourceDirectory, ".java")) {
            String rInstanceRelativePath = rInstanceSourceDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!rInstanceRelativePath.startsWith("com/opsfactor/community/rinstance/")) {
                continue;
            }
            if (!COMMUNITY_ALLOWED_R_INSTANCE_FILES.contains(rInstanceRelativePath)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui integracao R fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communitySupportShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path supportSourceDirectory = communityWorkspaceDirectory.resolve("src/main/java");
        List<String> violations = new ArrayList<>();

        /*
         * Support e compartilhado por todos os modulos. A allowlist evita que
         * helpers de integracao privada, licencas, clientes, clouds ou
         * algoritmos Enterprise voltem ao repositorio Community sem decisao
         * explicita.
         */
        for (Path javaSourcePath : findWorkspaceFiles(supportSourceDirectory, ".java")) {
            String supportRelativePath = supportSourceDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!supportRelativePath.startsWith("com/opsfactor/community/planning/support/")) {
                continue;
            }
            if (!COMMUNITY_ALLOWED_SUPPORT_FILES.contains(supportRelativePath)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui classe de suporte fora do conjunto aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communitySchedulerShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path schedulerSourceDirectory = communityWorkspaceDirectory.resolve("src/main/java");
        List<String> violations = new ArrayList<>();

        /*
         * Scheduler Community existe somente para execucoes imediatas e
         * sincronas. Fila, workers, recorrencia real, consumidores assincronos e
         * provedores cloud pertencem ao Enterprise.
         */
        for (Path javaSourcePath : findWorkspaceFiles(schedulerSourceDirectory, ".java")) {
            String schedulerRelativePath = schedulerSourceDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!schedulerRelativePath.startsWith("com/opsfactor/community/scheduler/")) {
                continue;
            }
            if (!COMMUNITY_ALLOWED_SCHEDULER_FILES.contains(schedulerRelativePath)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui classe de scheduler fora do conjunto aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communitySecurityShouldStayInApprovedOperationalSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path securitySourceDirectory = communityWorkspaceDirectory.resolve("src/main/java");
        List<String> violations = new ArrayList<>();

        /*
         * Security Community deve continuar simples: usuario/senha local e
         * apenas ROLE_ADMIN. SSO, OIDC/SAML, unlock de IP, RBAC avancado e
         * hardening Enterprise ficam fora deste modulo.
         */
        for (Path javaSourcePath : findWorkspaceFiles(securitySourceDirectory, ".java")) {
            String securityRelativePath = securitySourceDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!securityRelativePath.startsWith("com/opsfactor/community/security/")) {
                continue;
            }
            if (!COMMUNITY_ALLOWED_SECURITY_FILES.contains(securityRelativePath)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui classe de security fora do conjunto aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityWebModuleShouldStayInApprovedApiAndResourceSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path webSourceDirectory = communityWorkspaceDirectory.resolve("src/main/java");
        Path webResourcesDirectory = communityWorkspaceDirectory.resolve("src/main/resources");
        List<String> violations = new ArrayList<>();

        /*
         * O package web Community publica somente APIs e configuracoes de
         * bootstrap. O novo front roda em servidor apartado; logo qualquer
         * arquivo Java/resource novo no backend web precisa ser aprovado no
         * recorte antes de aparecer no repositorio aberto.
         */
        for (Path javaSourcePath : findWorkspaceFiles(webSourceDirectory, ".java")) {
            String webSourceRelativePath = webSourceDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!webSourceRelativePath.startsWith("com/opsfactor/community/planning/web/")) {
                continue;
            }
            if (!COMMUNITY_ALLOWED_WEB_SOURCE_FILES.contains(webSourceRelativePath)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        for (Path resourcePath : findWorkspaceFiles(webResourcesDirectory, "")) {
            String webResourceRelativePath = webResourcesDirectory
                    .relativize(resourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!COMMUNITY_ALLOWED_WEB_RESOURCE_FILES.contains(webResourceRelativePath)) {
                violations.add(communityWorkspaceDirectory.relativize(resourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui arquivo web/resource fora do conjunto aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityApplicationPropertiesShouldLiveOnlyInExecutableWebModule() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path webApplicationProperties = communityWorkspaceDirectory.resolve(
                "src/main/resources/application.properties");
        List<String> violations = new ArrayList<>();

        /*
         * Modulos biblioteca Community nao devem publicar `application.properties`
         * proprio. Spring Boot carrega arquivos com esse nome de jars no
         * classpath; manter o bootstrap somente no executavel web evita uma
         * segunda fonte silenciosa de defaults quando o Enterprise importa o
         * Community como dependencia.
         */
        try (Stream<Path> pathStream = Files.walk(communityWorkspaceDirectory)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isTargetFile(path))
                    .filter(CommunityArchitectureBoundaryTest::isMainResourcesPath)
                    .filter(path -> "application.properties".equals(path.getFileName().toString()))
                    .filter(path -> !path.normalize().equals(webApplicationProperties.normalize()))
                    .map(communityWorkspaceDirectory::relativize)
                    .map(Path::toString)
                    .forEach(violations::add);
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve publicar application.properties somente no executavel raiz:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityPlanningDtoModuleShouldStayInApprovedContractSet() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningDtoSourceDirectory = communityWorkspaceDirectory.resolve("src/main/java");
        List<String> violations = new ArrayList<>();

        /*
         * O package DTO e contrato publico para API/front e tambem carrega
         * mappers de data upload. DTOs compartilhados podem manter campos
         * bloqueaveis por compatibilidade, mas qualquer novo arquivo de
         * contrato precisa passar pelo recorte Community antes de ser publicado.
         */
        for (Path javaSourcePath : findWorkspaceFiles(planningDtoSourceDirectory, ".java")) {
            String planningDtoRelativePath = planningDtoSourceDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (!planningDtoRelativePath.startsWith("com/opsfactor/community/planning/dto/")) {
                continue;
            }
            if (!COMMUNITY_ALLOWED_PLANNING_DTO_SOURCE_FILES.contains(planningDtoRelativePath)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui arquivo DTO fora do conjunto aprovado:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityPlanningControllersShouldNotPublishPlanningBookFileUploadEndpoints() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningControllerDirectory = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/web/restcontroller/planning");
        List<String> violations = new ArrayList<>();

        /*
         * Planning Book Community permite ajuste por celula/tela e export XLSX
         * somente leitura. Upload/importacao de ajustes por arquivo e geracao
         * de Demand Plan a partir de arquivo pertencem a recortes proprios e
         * nao devem voltar como endpoint incidental nos controllers de
         * planning.
         */
        for (Path javaSourcePath : findWorkspaceFiles(planningControllerDirectory, ".java")) {
            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_PLANNING_FILE_UPLOAD_IMPORT_PATTERN.matcher(sourceLine).matches()
                        || COMMUNITY_PLANNING_UPLOAD_MAPPING_PATTERN.matcher(sourceLine).matches()
                        || COMMUNITY_PLANNING_DEMAND_GENERATE_FROM_FILE_MAPPING_PATTERN.matcher(sourceLine).matches()) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve publicar upload/importacao de Planning Book ou generate/fromfile por arquivo:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotContainEnterpriseTransactionalOrderEntities() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Pedidos, remessas e ordens de carga sao dados transacionais
         * Enterprise. O Community pode manter campos fisicos transicionais em
         * planning data para compatibilidade de schema, mas nao deve publicar
         * as entidades/repositories que materializam esses dominios.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            String fileName = javaSourcePath.getFileName().toString();
            if (COMMUNITY_FORBIDDEN_TRANSACTIONAL_ENTITY_OR_REPOSITORY_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve conter entidades/repositories transacionais de pedidos/remessas:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotContainEnterprisePricingDemandPlanningFiles() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Listas de preco, price plan e demais efeitos economicos de demanda
         * pertencem ao Enterprise. Comentarios podem explicar a fronteira, mas
         * arquivos funcionais com esses nomes nao devem voltar ao Community.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            String fileName = javaSourcePath.getFileName().toString();
            if (COMMUNITY_FORBIDDEN_ECONOMIC_DEMAND_PLANNING_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve conter arquivos funcionais de pricing/listas de preco do Demand Planning:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityDemandPlanItemShouldNotReintroduceEnterpriseCustomerOrderOrGenericKeyFigureState()
            throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * DemandPlanItem Community representa somente Baseline e Ajuste de
         * Demanda na colaboracao via Planning Book. Carteira/customer orders,
         * rebalanceamento legado e APIs genericas por Key Figure pertencem ao
         * desenho Enterprise ou ao codigo legado removido; se voltarem por copia
         * mecanica, a neutralizacao Community pode deixar de ser explicita.
         */
        List<Path> demandPlanItemSourcePaths = List.of(
                communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/demandplanning/demandplan/domain/DemandPlanItem.java"),
                communityWorkspaceDirectory.resolve(
                        "src/main/java/com/opsfactor/community/capability/demandplanning/demandplan/domain/HistoricoDemandPlanItem.java"));

        for (Path demandPlanItemSourcePath : demandPlanItemSourcePaths) {
            List<String> sourceLines = Files.readAllLines(demandPlanItemSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_FORBIDDEN_DEMAND_PLAN_LINE_ENTERPRISE_TOKENS.stream().anyMatch(sourceLine::contains)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            demandPlanItemSourcePath,
                            lineIndex,
                            sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "DemandPlanItem Community nao deve reintroduzir carteira, rebalanceamento legado ou API generica de Key Figure:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotContainEnterpriseEconomicSupplyPlanningFiles() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Custos, frete, curvas logisticas, COGS, P&L e cost-to-serve sao
         * Enterprise. O Community pode manter DTOs que rejeitam flags/campos
         * economicos recebidos do front compartilhado, mas nao pode materializar
         * projections ou repositories economicos.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            String fileName = javaSourcePath.getFileName().toString();
            if (COMMUNITY_FORBIDDEN_ECONOMIC_SUPPLY_PLANNING_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve conter arquivos funcionais economicos de Supply Planning:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotContainConfigurableTemporalSplitCurves() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O Community precisa da projection temporal flat para converter valores
         * entre calendarios DP/SNP. O cadastro de curvas por DFU, filtros e
         * qualquer nova implementacao de curva temporal configuravel pertencem
         * ao Enterprise e nao devem voltar ao source principal Community.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            String fileName = javaSourcePath.getFileName().toString();
            if (COMMUNITY_FORBIDDEN_CONFIGURABLE_TEMPORAL_SPLIT_FILES.contains(fileName)
                    || isForbiddenTemporalSplitCurveProjectionImplementation(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve conter curvas temporais configuraveis ou implementacoes nao flat:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotContainConfigurableCalendarEntities() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Weekdays, holidays e projections derivadas de dias uteis pertencem ao
         * Enterprise. O Community nao calcula `Direct Demand per Working Day`
         * nem deve manter uma projection tecnica orfa para essa finalidade.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            String fileName = javaSourcePath.getFileName().toString();
            if (COMMUNITY_FORBIDDEN_CONFIGURABLE_CALENDAR_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve conter entidades, repositories ou projections de calendario/dias uteis Enterprise:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotContainAdvancedSecurityModels() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Seguranca Community e login/senha simples com ROLE_ADMIN. Modelos de
         * tenant, lockout, IP bloqueado, tentativa de login e escopo granular
         * por location pertencem ao Enterprise e nao devem voltar como entidade
         * ou repository no source principal Community.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            String fileName = javaSourcePath.getFileName().toString();
            if (COMMUNITY_FORBIDDEN_ADVANCED_SECURITY_MODEL_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve conter modelos de seguranca avancada/granular:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotImportAdvancedSecurityStacks() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Comentarios podem explicar que SSO/OAuth/SAML/JWT pertencem ao
         * Enterprise. Imports reais desses stacks, porem, indicam que a seguranca
         * Community deixou de ser usuario/senha simples.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (isForbiddenAdvancedSecurityImport(sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve importar stacks de seguranca avancada:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotImportCloudMessagingStacks() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O Community pode usar filas Java em memoria dentro de algoritmos, mas
         * nao deve importar SDKs de mensageria/cloud. Azure Service Bus, AWS e
         * outros providers entram somente no Enterprise.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (isForbiddenCloudMessagingImport(sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve importar SDKs de mensageria/cloud:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityBeanFieldsShouldUseExplicitAutowiredAnnotation() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Beans em services/controllers/components devem ser explicitamente
         * anotados com @Autowired. A regra olha o tipo declarado do campo para
         * evitar falso positivo com nomes de inicializadores, por exemplo
         * LoggerFactory em campos que nao sao beans Spring.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)
                    || COMMUNITY_ALLOWED_NON_AUTOWIRED_BEAN_FIELD_FILES.contains(javaSourcePath.getFileName().toString())) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
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

    @Test
    void communityAutowiredBeanFieldsShouldBePrivate() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * A anotacao @Autowired deixa claro que o atributo e um bean Spring,
         * mas o campo em si nao deve ficar package-private/protected sem uma
         * razao explicita. Pontos de extensao para subclasses devem ser metodos
         * protegidos documentados, mantendo o wiring encapsulado na classe dona.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            int autowiredLineIndex = -1;
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                String trimmedLine = sourceLine.trim();

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
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
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
    void communityOptionalAutowiredShouldStayOnDocumentedEnterpriseExtensionPoints() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Injeção opcional é um mecanismo de recorte, não uma convenção geral.
         * No Community ela deve aparecer somente nas SPIs documentadas em que o
         * Enterprise fornece o bean real pelo classpath privado. Qualquer novo
         * uso precisa ser decidido e documentado explicitamente.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
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
                "Community deve limitar @Autowired(required = false) aos pontos de extensao Enterprise documentados:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotContainConfiguredViewCharacteristicRepositories() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * As entidades de caracteristica em ConfiguredView ainda existem para
         * permitir limpeza de registros legados por orphanRemoval. Repositories
         * diretos, porem, reabrem uma superficie de configuracao Enterprise que
         * o Community nao publica nem usa funcionalmente.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            String fileName = javaSourcePath.getFileName().toString();
            if (COMMUNITY_FORBIDDEN_CONFIGURED_VIEW_CHARACTERISTIC_REPOSITORY_FILES.contains(fileName)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve conter repositories diretos de caracteristicas de User View:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotContainRemovedOrphanModelOrServiceFiles() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Estes arquivos foram removidos porque nao tinham callers funcionais no
         * Community ou representavam logs/modelos antigos sem superficie publica.
         * Se alguma capacidade real precisar voltar, ela deve nascer com service,
         * contrato REST e documentacao explicitos, nao como repository/entity solto.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            String fileName = javaSourcePath.getFileName().toString();
            String relativePath = communityWorkspaceDirectory
                    .relativize(javaSourcePath)
                    .toString()
                    .replace('\\', '/');
            if (COMMUNITY_FORBIDDEN_REMOVED_ORPHAN_FILES.contains(fileName)
                    || COMMUNITY_FORBIDDEN_REMOVED_ORPHAN_PATHS.contains(relativePath)) {
                violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve reintroduzir entities/repositories/services orfaos removidos:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotCallPrintStackTrace() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O Community aberto precisa registrar erros de forma auditavel. Chamadas
         * diretas a printStackTrace vazam para stdout/stderr sem contexto de
         * logger, dificultando suporte e operacao em container.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_FORBIDDEN_PRINT_STACK_TRACE_PATTERN.matcher(sourceLine).matches()) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve usar logger ou resposta controlada em vez de printStackTrace:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotWriteDirectlyToStdoutOrStderr() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Em container, stdout/stderr deve receber logs estruturados pelo
         * framework de logging. Prints diretos perdem contexto de classe,
         * request e severidade, o que torna suporte do Community mais dificil.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_FORBIDDEN_STDOUT_STDERR_PATTERN.matcher(sourceLine).matches()) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve usar logger em vez de System.out/System.err:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotUseLegacySelloutProjectionPackage() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * As projections de historico de vendas sao neutras no Community:
         * sell-out e apenas a fonte documental permitida nesta edicao. O package
         * legado selloutprojection induz leitura errada do contrato e nao deve
         * voltar depois do rename para salesprojection.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (sourceLine.contains(COMMUNITY_LEGACY_SELLOUT_PROJECTION_PACKAGE_TOKEN)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve usar historicaldata.salesprojection em vez do package legado selloutprojection:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotExposeLegacySelloutProjectionApiNames() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Sell-out continua sendo a fonte concreta carregada pela factory
         * Community, mas as projections em memoria devem expor API neutra de
         * sales. Isso evita que consumidores de forecast, Demand Analysis e
         * Planning Book fiquem acoplados ao documento historico Community e
         * facilita o overlay Enterprise por @Primary na factory.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_FORBIDDEN_LEGACY_SELLOUT_PROJECTION_API_TOKENS.stream().anyMatch(sourceLine::contains)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve expor APIs neutras de sales nas projections, nao nomes legados *Sellout*:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldUseMaterialNamedSalesProjectionClasses() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O domínio JPA ainda se chama Produto, mas a API pública de projections
         * de vendas deve usar material. Isso mantém o contrato alinhado ao front
         * novo e evita que novas classes de Demand Planning reintroduzam a
         * nomenclatura antiga em tipos compartilhados.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_FORBIDDEN_PRODUCT_NAMED_SALES_PROJECTION_TOKENS.stream().anyMatch(sourceLine::contains)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve usar SalesProjectionMaterial* nas projections de sales, nao nomes antigos *Produto*:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotExposeParallelProductionRuntimeSwitch() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * VersaoProducaoParalela ainda existe como entidade transicional de
         * schema, mas o Community nao deve expor um booleano runtime para
         * espalhar ajustes por multiplos outputs. O fluxo Community ajusta
         * sempre o output material/location selecionado; parallel routing/output
         * volta no Enterprise por overlay proprio.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (sourceLine.contains(COMMUNITY_PARALLEL_ROUTING_RUNTIME_SWITCH_TOKEN)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve expor switch runtime para outputs de versao de producao paralela:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotUseLegacyDemandAggregationFieldNames() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * O contrato publico novo usa materialAggregationType/locationAggregationType.
         * locationAggregationLevelId e outro conceito funcional e nao deve ser
         * confundido com esta migracao de enum TOP_DOWN/BOTTOM_UP.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (COMMUNITY_LEGACY_DEMAND_AGGREGATION_FIELD_PATTERN.matcher(sourceLine).matches()) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve usar materialAggregationType/locationAggregationType para agregacao de forecast:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotUseLegacyCleansedHistoricalSalesFieldName() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * A simulacao de forecast expõe duas etapas historicas explicitas:
         * stockout treatment e outlier/event cleaning. O nome antigo
         * cleansedHistoricalSales escondia qual etapa estava sendo mostrada.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (sourceLine.contains(COMMUNITY_LEGACY_CLEANSED_HISTORICAL_SALES_FIELD)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve expor historicalSalesAfterStockoutTreatment/historicalSalesAfterOutlierTreatment:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotExposeDemandModelDescriptionsFromEntity() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * ParametrosModeloEstatisticoAbstract conhece labels de modelos
         * Enterprise por compatibilidade interna do enum compartilhado. A
         * superficie Community deve publicar modelos via DemandPlanningModelCatalog,
         * que filtra apenas o subconjunto aberto para Runtime Info e OpenAPI.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)
                    || javaSourcePath.getFileName().toString().equals("ParametrosModeloEstatisticoAbstract.java")) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (sourceLine.contains(COMMUNITY_MODEL_DESCRIPTION_METHOD_TOKEN)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community deve publicar modelos de Demand Planning via DemandPlanningModelCatalog, nao por descricao da entidade:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotReferenceEnterpriseDemandPlanningImplementations() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Auto-fit, campanhas/eventos, sucessao de materiais, Chronos/foundation
         * model, listas de preco e pedidos como fonte historica pertencem ao
         * Enterprise. DTOs Community podem manter campos bloqueaveis, mas nao
         * devem depender das implementacoes/classes privadas listadas abaixo.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (containsEnterpriseDemandPlanningImplementationToken(sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve referenciar implementacoes Enterprise de Demand Planning:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityMainSourcesShouldNotReferenceForbiddenDemandPlanningSourceTokens() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<String> violations = new ArrayList<>();

        /*
         * Residuos pequenos de Demand Planning tambem devem ficar fora do
         * Community mesmo quando nao formam uma classe propria. Esta guarda
         * evita que campos/constantes removidos ou nomenclaturas obsoletas
         * voltem por copia do legado.
         */
        for (Path javaSourcePath : findWorkspaceFiles(communityWorkspaceDirectory, ".java")) {
            if (isTestSource(javaSourcePath)) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (containsForbiddenDemandPlanningSourceToken(sourceLine)) {
                    violations.add(formatViolation(communityWorkspaceDirectory, javaSourcePath, lineIndex, sourceLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "Community nao deve referenciar tokens residuais ou obsoletos de Demand Planning:\n"
                        + String.join("\n", violations));

    }

    @Test
    void communityDemoDataSqlShouldNotSeedEnterpriseData() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path dataSqlPath = communityWorkspaceDirectory.resolve("src/main/resources/data.sql");
        List<String> violations = new ArrayList<>();

        /*
         * O data.sql Community e um artefato publico de bootstrap/demo. Ele pode
         * carregar dados mestres e transacionais do recorte aberto, como
         * sell-out, estoque, roteiros simples e linhas de transporte, mas nao
         * deve carregar seeds de funcionalidades Enterprise. Isso evita que o
         * repositorio aberto publique exemplos de tabelas ou conceitos privados
         * apenas porque eles existiam no demo legado.
         */
        if (Files.exists(dataSqlPath)) {
            List<String> dataSqlLines = Files.readAllLines(dataSqlPath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < dataSqlLines.size(); lineIndex++) {
                String dataSqlLine = dataSqlLines.get(lineIndex);
                String dataSqlLineLowercase = dataSqlLine.toLowerCase(Locale.ROOT);
                if (COMMUNITY_FORBIDDEN_DATA_SQL_ENTERPRISE_TOKENS.stream()
                        .anyMatch(dataSqlLineLowercase::contains)) {
                    violations.add(formatViolation(
                            communityWorkspaceDirectory,
                            dataSqlPath,
                            lineIndex,
                            dataSqlLine));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                "data.sql Community nao deve carregar seeds de funcionalidades Enterprise:\n"
                        + String.join("\n", violations));

    }

    /**
     * Resolve a raiz do workspace Community independentemente de o teste ser
     * iniciado pelo build raiz atual ou por um subdiretorio legado
     * {@code community} ainda usado em checkpoints antigos da migracao.
     */
    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        if ("community".equals(currentDirectory.getFileName().toString())) {
            return currentDirectory.getParent();
        }

        return currentDirectory;

    }

    private List<Path> findWorkspaceFiles(Path communityWorkspaceDirectory, String fileSuffix) throws IOException {

        try (Stream<Path> pathStream = Files.walk(communityWorkspaceDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isTargetFile(path))
                    .filter(path -> path.getFileName().toString().endsWith(fileSuffix))
                    .toList();
        }

    }

    private void assertApprovedControllerSet(
            Path communityWorkspaceDirectory,
            String controllerDirectoryRelativePath,
            List<String> allowedControllerFiles,
            String controllerGroupName) throws IOException {

        Path controllerDirectory = communityWorkspaceDirectory.resolve(controllerDirectoryRelativePath);
        List<String> violations = new ArrayList<>();

        try (Stream<Path> pathStream = Files.list(controllerDirectory)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .forEach(javaSourcePath -> {
                        String fileName = javaSourcePath.getFileName().toString();
                        if (!allowedControllerFiles.contains(fileName)) {
                            violations.add(communityWorkspaceDirectory.relativize(javaSourcePath).toString());
                        }
                    });
        }

        assertTrue(
                violations.isEmpty(),
                "Community possui controller REST de " + controllerGroupName
                        + " fora do conjunto operacional aprovado:\n"
                        + String.join("\n", violations));

    }

    private List<Path> findWorkspaceConfigurationFiles(Path communityWorkspaceDirectory) throws IOException {

        try (Stream<Path> pathStream = Files.walk(communityWorkspaceDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isTargetFile(path))
                    .filter(CommunityArchitectureBoundaryTest::isMavenOrPropertiesFile)
                    .toList();
        }

    }

    private boolean pomDeclaresActiveSpringBootMavenPlugin(Path pomPath) throws Exception {

        Element pomDocumentElement = readPomDocumentElement(pomPath);
        NodeList pluginNodeList = pomDocumentElement.getElementsByTagNameNS("*", "plugin");

        for (int pluginIndex = 0; pluginIndex < pluginNodeList.getLength(); pluginIndex++) {
            Element pluginElement = (Element) pluginNodeList.item(pluginIndex);
            String pluginArtifactId = getFirstChildTextContent(pluginElement, "artifactId");
            if ("spring-boot-maven-plugin".equals(pluginArtifactId)) {
                return true;
            }
        }

        return false;

    }

    private Element readPomDocumentElement(Path pomPath) throws Exception {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        return documentBuilderFactory
                .newDocumentBuilder()
                .parse(pomPath.toFile())
                .getDocumentElement();

    }

    private String getFirstChildTextContent(Element parentElement, String childName) {

        NodeList childNodeList = parentElement.getElementsByTagNameNS("*", childName);
        if (childNodeList.getLength() == 0) {
            return "";
        }

        return childNodeList.item(0).getTextContent().trim();

    }

    private List<Path> findWorkspaceDocumentationFiles(Path communityWorkspaceDirectory) throws IOException {

        try (Stream<Path> pathStream = Files.walk(communityWorkspaceDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isTargetFile(path))
                    .filter(path -> path.getFileName().toString().equals("README.md"))
                    .toList();
        }

    }

    private static boolean isTargetFile(Path path) {

        return StreamSupportPath.segments(path).contains("target");

    }

    private static boolean isMainResourcesPath(Path path) {

        List<String> pathSegments = StreamSupportPath.segments(path);
        for (int pathSegmentIndex = 0; pathSegmentIndex < pathSegments.size() - 2; pathSegmentIndex++) {
            if ("src".equals(pathSegments.get(pathSegmentIndex))
                    && "main".equals(pathSegments.get(pathSegmentIndex + 1))
                    && "resources".equals(pathSegments.get(pathSegmentIndex + 2))) {
                return true;
            }
        }
        return false;

    }

    private static boolean isLegacyFrontendResourcePath(Path path) {

        String fileName = path.getFileName().toString().toLowerCase();
        if (Files.isDirectory(path)) {
            return LEGACY_FRONTEND_RESOURCE_DIRECTORIES.contains(fileName);
        }

        return Files.isRegularFile(path)
                && LEGACY_FRONTEND_FILE_SUFFIXES.stream().anyMatch(fileName::endsWith);

    }

    private static boolean isMavenOrPropertiesFile(Path path) {

        String fileName = path.getFileName().toString();
        return "pom.xml".equals(fileName) || fileName.endsWith(".properties");

    }

    private static boolean isTestSource(Path path) {

        return path.toString().contains("src\\test") || path.toString().contains("src/test");

    }

    private Properties loadWorkspaceProperties(String relativePath) throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path propertiesPath = communityWorkspaceDirectory.resolve(relativePath);

        Properties properties = new Properties();
        try (Reader propertiesReader = Files.newBufferedReader(propertiesPath, StandardCharsets.UTF_8)) {
            properties.load(propertiesReader);
        }
        return properties;

    }

    private static boolean containsEnterpriseToken(String line) {

        return ENTERPRISE_MAVEN_OR_PROPERTY_TOKENS.stream().anyMatch(line::contains);

    }

    private static boolean containsForbiddenBuildOrPropertyToken(String line) {

        String lowercaseLine = line.toLowerCase();
        return COMMUNITY_FORBIDDEN_BUILD_OR_PROPERTY_TOKENS.stream().anyMatch(lowercaseLine::contains);

    }

    private static boolean isForbiddenPrivateRuntimeDependencyCoordinate(String dependencyCoordinate) {

        String lowercaseDependencyCoordinate = dependencyCoordinate.toLowerCase(Locale.ROOT);
        return COMMUNITY_FORBIDDEN_PRIVATE_RUNTIME_DEPENDENCY_COORDINATES.stream()
                .anyMatch(lowercaseDependencyCoordinate::startsWith);

    }

    private static boolean containsLegacyEditionNamingToken(String line) {

        String lowercaseLine = line.toLowerCase();
        return COMMUNITY_FORBIDDEN_LEGACY_EDITION_NAMING_TOKENS.stream().anyMatch(lowercaseLine::contains);

    }

    /**
     * A licença aprovada para a distribuição Community é source-available e a
     * negativa explícita da aprovação OSI é documentação jurídica, não
     * nomenclatura operacional de edição. Sem esta exceção a própria regra de
     * fronteira reprova o README aprovado.
     */
    private static boolean isApprovedSourceAvailableLicenseStatement(String line) {

        String lowercaseLine = line.toLowerCase(Locale.ROOT);
        String approvedOsiStatement = "not an osi-approved open" + "-source license";
        return lowercaseLine.contains("source-available")
                && lowercaseLine.contains(approvedOsiStatement);

    }

    /**
     * Mantem a unica excecao lexical do scheduler restrita a sua declaracao
     * de prefixo. A regra nao permite imports, artefatos Maven ou qualquer
     * outra referencia Community ao runtime Enterprise.
     */
    private static boolean isApprovedEnterpriseFqcnReference(
            Path communityWorkspaceDirectory,
            Path javaSourcePath,
            String sourceCodeLine) {

        String relativeJavaSourcePath = communityWorkspaceDirectory
                .relativize(javaSourcePath)
                .toString()
                .replace('\\', '/');
        String approvedSourceCodeLine =
                COMMUNITY_ALLOWED_ENTERPRISE_FQCN_REFERENCE_BY_SOURCE_PATH.get(relativeJavaSourcePath);

        return approvedSourceCodeLine != null
                && approvedSourceCodeLine.equals(sourceCodeLine.trim());

    }

    private static boolean containsForbiddenGenericImplementationMarker(Path sourcePath, String line) {

        return COMMUNITY_FORBIDDEN_GENERIC_IMPLEMENTATION_MARKERS.stream().anyMatch(line::contains);

    }

    private static boolean containsForbiddenCommercialEditionTerm(String line) {

        return COMMUNITY_FORBIDDEN_COMMERCIAL_EDITION_TERM_PATTERN.matcher(line).matches();

    }

    private static boolean containsForbiddenCustomerOrPrivateHostToken(String line) {

        String lowercaseLine = line.toLowerCase(Locale.ROOT);
        return COMMUNITY_FORBIDDEN_CUSTOMER_OR_PRIVATE_HOST_TOKENS.stream().anyMatch(lowercaseLine::contains);

    }

    private static boolean isForbiddenActiveSecretPlaceholder(String line) {

        String trimmedLine = line.trim();
        if (trimmedLine.startsWith("#") || trimmedLine.startsWith("!")) {
            return false;
        }

        return COMMUNITY_FORBIDDEN_ACTIVE_SECRET_PLACEHOLDER_PATTERN.matcher(trimmedLine).matches();

    }

    private static boolean containsLegacyFrontendWebCodeToken(String line) {

        return LEGACY_FRONTEND_WEB_CODE_TOKENS.stream().anyMatch(line::contains);

    }

    private static boolean containsEnterpriseDemandPlanningImplementationToken(String line) {

        return COMMUNITY_FORBIDDEN_DEMAND_ENTERPRISE_CLASS_TOKENS.stream().anyMatch(line::contains);

    }

    private static boolean containsForbiddenDemandPlanningSourceToken(String line) {

        return COMMUNITY_FORBIDDEN_DEMAND_SOURCE_TOKENS.stream().anyMatch(line::contains);

    }

    private static boolean containsForbiddenGisMapClassNameToken(String fileName) {

        return COMMUNITY_FORBIDDEN_GIS_MAP_CLASS_NAME_TOKENS.stream().anyMatch(fileName::contains);

    }

    private static boolean isForbiddenAdvancedSecurityImport(String line) {

        String trimmedLine = line.trim();
        String lowercaseLine = trimmedLine.toLowerCase();
        return trimmedLine.startsWith("import ")
                && COMMUNITY_FORBIDDEN_ADVANCED_SECURITY_IMPORT_TOKENS.stream().anyMatch(lowercaseLine::contains);

    }

    private static boolean containsSpringMappingAnnotation(String line) {

        return line.contains("@GetMapping")
                || line.contains("@PostMapping")
                || line.contains("@PutMapping")
                || line.contains("@DeleteMapping")
                || line.contains("@PatchMapping")
                || line.contains("@RequestMapping");

    }

    private static String collectMethodAnnotationBlock(List<String> sourceLines, int mappingLineIndex) {

        StringBuilder methodAnnotationBlock = new StringBuilder();
        int firstRelevantLineIndex = Math.max(0, mappingLineIndex - 4);
        int lastRelevantLineIndex = Math.min(sourceLines.size() - 1, mappingLineIndex + 12);

        for (int lineIndex = firstRelevantLineIndex; lineIndex <= lastRelevantLineIndex; lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex).trim();
            methodAnnotationBlock.append(sourceLine);
            methodAnnotationBlock.append('\n');

            if (lineIndex > mappingLineIndex
                    && (sourceLine.startsWith("public ")
                    || sourceLine.startsWith("protected ")
                    || sourceLine.startsWith("private "))) {
                break;
            }
        }

        return methodAnnotationBlock.toString();

    }

    private static boolean containsAllowedOpenEndpointPath(String methodAnnotationBlock) {

        return COMMUNITY_ALLOWED_OPEN_ENDPOINT_PATH_TOKENS
                .stream()
                .anyMatch(methodAnnotationBlock::contains);

    }

    /**
     * Compara pelo nome da classe quando a taxonomia recursiva distribui o
     * mesmo papel canonico entre varias capabilities.
     */
    private static boolean containsAllowedFileName(List<String> allowedPaths, String fileName) {

        return allowedPaths.stream()
                .map(Path::of)
                .map(Path::getFileName)
                .map(Path::toString)
                .anyMatch(fileName::equals);

    }

    private static boolean isForbiddenCloudMessagingImport(String line) {

        String trimmedLine = line.trim();
        String lowercaseLine = trimmedLine.toLowerCase();
        return trimmedLine.startsWith("import ")
                && COMMUNITY_FORBIDDEN_CLOUD_MESSAGING_IMPORT_TOKENS.stream().anyMatch(lowercaseLine::contains);

    }

    private static boolean isCandidateSpringBeanField(String line) {

        String trimmedLine = line.trim();
        if (!trimmedLine.startsWith("private ")
                || !trimmedLine.endsWith(";")
                || trimmedLine.contains(" static ")) {
            return false;
        }

        String fieldType = extractPrivateFieldType(trimmedLine);
        return COMMUNITY_SPRING_BEAN_FIELD_TYPE_TOKENS.stream().anyMatch(fieldType::contains);

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
     * Aceita o campo final satisfeito por construtor anotado explicitamente,
     * que é uma forma de wiring tão visível quanto a injeção por campo.
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

    private static boolean isOptionalAutowiredAnnotation(String line) {

        String normalizedLine = line.replace(" ", "");
        return normalizedLine.startsWith("@Autowired(")
                && normalizedLine.contains("required=false");

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
            Path workspaceDirectory,
            Path javaSourcePath,
            String fieldLine) {

        String relativePath = workspaceDirectory
                .relativize(javaSourcePath)
                .toString()
                .replace('\\', '/');
        return relativePath + "#" + extractFieldName(fieldLine.trim());

    }

    private static String extractFieldName(String trimmedLine) {

        String fieldDeclaration = trimmedLine.substring(0, trimmedLine.length() - 1).trim();
        int assignmentIndex = fieldDeclaration.indexOf('=');
        if (assignmentIndex >= 0) {
            fieldDeclaration = fieldDeclaration.substring(0, assignmentIndex).trim();
        }

        String[] fieldDeclarationParts = fieldDeclaration.split("\\s+");
        return fieldDeclarationParts.length == 0
                ? ""
                : fieldDeclarationParts[fieldDeclarationParts.length - 1];

    }

    private static boolean isForbiddenTemporalSplitCurveProjectionImplementation(String fileName) {

        return fileName.startsWith("SplitTemporalProjectionCurva")
                && !COMMUNITY_ALLOWED_TEMPORAL_SPLIT_CURVE_PROJECTION_FILES.contains(fileName);

    }

    /**
     * Mantém explícitas as poucas rotas históricas que delegam a autenticação
     * à cadeia global, sem tornar a exceção uma dispensa genérica de RBAC.
     */
    private static boolean isGlobalAuthenticatedLegacyEndpoint(String methodAnnotationBlock) {

        return COMMUNITY_GLOBAL_AUTHENTICATED_ENDPOINT_PATH_TOKENS.stream()
                .anyMatch(methodAnnotationBlock::contains);

    }

    /**
     * Reconhece somente a anotacao integral dos tres contratos legados de
     * execucao. A comparacao por caminho relativo impede que a mesma role seja
     * reutilizada em services, seguranca ou novos endpoints Community.
     */
    private static boolean isApprovedLegacyExecutionControllerAnnotation(
            Path communityWorkspaceDirectory,
            Path javaSourcePath,
            String sourceLine) {

        String relativeJavaSourcePath = communityWorkspaceDirectory
                .relativize(javaSourcePath)
                .toString()
                .replace('\\', '/');
        String approvedSecuredAnnotation =
                COMMUNITY_LEGACY_EXECUTION_SECURED_ANNOTATION_BY_CONTROLLER_PATH.get(relativeJavaSourcePath);

        return approvedSecuredAnnotation != null
                && approvedSecuredAnnotation.equals(sourceLine.trim());

    }

    /**
     * Garante que a anotacao compativel seja usada no metodo da rota legada
     * correspondente, e nao apenas em qualquer metodo do mesmo controller.
     */
    private static boolean isApprovedLegacyExecutionControllerEndpoint(
            Path communityWorkspaceDirectory,
            Path javaSourcePath,
            String methodAnnotationBlock) {

        String relativeJavaSourcePath = communityWorkspaceDirectory
                .relativize(javaSourcePath)
                .toString()
                .replace('\\', '/');
        String approvedSecuredAnnotation =
                COMMUNITY_LEGACY_EXECUTION_SECURED_ANNOTATION_BY_CONTROLLER_PATH.get(relativeJavaSourcePath);
        String approvedEndpoint =
                COMMUNITY_LEGACY_EXECUTION_ENDPOINT_BY_CONTROLLER_PATH.get(relativeJavaSourcePath);

        return approvedSecuredAnnotation != null
                && approvedEndpoint != null
                && methodAnnotationBlock.contains(approvedSecuredAnnotation)
                && methodAnnotationBlock.contains(approvedEndpoint);

    }

    private static String formatViolation(Path workspaceDirectory, Path violationPath, int lineIndex, String line) {

        return workspaceDirectory.relativize(violationPath) + ":" + (lineIndex + 1) + ": " + line.trim();

    }

    private static JavaCodeSnippet removeJavaCommentsFromLine(
            String sourceLine,
            boolean insideBlockComment) {

        StringBuilder codeBuilder = new StringBuilder();
        boolean currentInsideBlockComment = insideBlockComment;
        int currentIndex = 0;

        while (currentIndex < sourceLine.length()) {
            if (currentInsideBlockComment) {
                int blockCommentEndIndex = sourceLine.indexOf("*/", currentIndex);
                if (blockCommentEndIndex < 0) {
                    return new JavaCodeSnippet(
                            codeBuilder.toString(),
                            true);
                }
                currentIndex = blockCommentEndIndex + 2;
                currentInsideBlockComment = false;
                continue;
            }

            int lineCommentIndex = sourceLine.indexOf("//", currentIndex);
            int blockCommentStartIndex = sourceLine.indexOf("/*", currentIndex);
            if (lineCommentIndex >= 0
                    && (blockCommentStartIndex < 0 || lineCommentIndex < blockCommentStartIndex)) {
                codeBuilder.append(
                        sourceLine,
                        currentIndex,
                        lineCommentIndex);
                break;
            }
            if (blockCommentStartIndex >= 0) {
                codeBuilder.append(
                        sourceLine,
                        currentIndex,
                        blockCommentStartIndex);
                currentIndex = blockCommentStartIndex + 2;
                currentInsideBlockComment = true;
                continue;
            }

            codeBuilder.append(sourceLine.substring(currentIndex));
            break;
        }

        return new JavaCodeSnippet(
                codeBuilder.toString(),
                currentInsideBlockComment);

    }

    private record JavaCodeSnippet(
            String code,
            boolean insideBlockComment) {
    }

    /**
     * Utilitario minimo para inspecionar segmentos de caminho sem depender de
     * separadores especificos de sistema operacional.
     */
    private static final class StreamSupportPath {

        private StreamSupportPath() {

        }

        private static List<String> segments(Path path) {

            List<String> segments = new ArrayList<>();
            for (Path pathSegment : path) {
                segments.add(pathSegment.toString());
            }
            return segments;

        }

    }

}
