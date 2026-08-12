package com.opsfactor.community.web.restcontroller.dataupload;

import com.opsfactor.community.web.restcontroller.dataupload.masterdata.ConversaoUnidadeIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.ConversaoUnidadeProdutoIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.LocationIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.MaterialIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.ParametrosMaterialLocationIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.UnidadeMedidaIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.inventorypolicy.InventoryPolicyDetailIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.inventorypolicy.InventoryPolicyIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.malha.LinhaTransporteIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.malha.LinhaTransporteMaterialIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.malha.VersaoMalhaIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.DisponibilidadeRecursoProdutivoIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.ListaTecnicaComponenteIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.ListaTecnicaIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.OperacaoRoteiroIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.RecursoProdutivoIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.RoteiroIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.VersaoProducaoSimplesIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.planning.supply.FulfilledDemandIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.planning.supply.InventoryPlanIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.transactionaldata.EstoqueIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.transactionaldata.SelloutIntegrationController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Congela a superficie REST de data upload publicada pelo Community.
 *
 * <p>A maioria dos endpoints FILE/JSON e registrada dinamicamente por
 * {@link IntegrationControllerAbstract#configureMappings()} a partir dos
 * {@code subPath}s das subclasses concretas. Por isso este contrato valida duas
 * coisas separadas: os {@code subPath}s permitidos para o registro dinamico e
 * os endpoints declarados manualmente por annotations nos controllers.</p>
 *
 * <p>Qualquer novo controller, rota manual ou {@code subPath} precisa passar
 * por esta lista para evitar que cargas de pedidos, campanhas, custos, precos,
 * frotas, warehouses, lotes, turnos ou planning data entrem no Community por
 * copia acidental do legado.</p>
 */
public class DataUploadControllersCommunityContractTest {

    private static final Map<Class<? extends IntegrationControllerAbstract<?, ?, ?, ?, ?, ?>>, String> COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS =
            createCommunityDynamicControllerSubpaths();

    private static final Map<Class<?>, List<Route>> COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER =
            createCommunityDeclaredEndpointsByController();

    private static final List<String> ENTERPRISE_ONLY_DATA_UPLOAD_SUBPATH_FAMILIES = List.of(
            "campaign",
            "costlist",
            "costmateriallocation",
            "detailedpricelist",
            "pricelist",
            "precoprodutolocation",
            "productionresourcecost",
            "productionroutingcost",
            "productionresource/availableshiftsbyproductionresource",
            "productionresource/shift",
            "productionresource/weekdaysandholidaysbyshift",
            "transportationlanecost",
            "transportationlanematerialcost",
            "locationcost",
            "locationcapacity",
            "locationdatecapacity",
            "maintenanceschedule",
            "allocationpenaltyproductionresourcerouting",
            "routingcluster",
            "parallelproductionversion",
            "supplyplanexecutionprofilelocation",
            "icms",
            "fleet",
            "vehicletype",
            "route",
            "economicgroup",
            "warehouse",
            "productsuccession",
            "locationmirroring",
            "aggregation",
            "workflow/demandplanning",
            "budget",
            "customkeyfiguresdp",
            "releaseddemand",
            "presetconstraint",
            "demandprioritizationmodel",
            "safetystockprioritizationmodel",
            "temporalsplitcurve",
            "workingday",
            "characteristic/materiallocation",
            "productfilter",
            "locationfilter",
            "dfufilter",
            "sellin",
            "selloutorder",
            "order",
            "salesordersdeliveries",
            "loadingorders",
            "productionorder",
            "stockproductionbatch",
            "directdemand",
            "consolidatedloadingorders",
            "inventoryplan/productionlot",
            "inventoryplan/coveragedays",
            "writeoffprojection",
            "greenfield");

    private static final List<String> DATA_UPLOAD_ROUTE_PREFIXES = List.of(
            "api/secured/dataupload/json/",
            "api/secured/dataupload/",
            "api/secured/data/file/",
            "api/secured/data/");

    @Test
    public void migratedDataUploadControllersShouldPublishOnlyCanonicalDynamicRoots() {

        List<IntegrationControllerAbstract<?, ?, ?, ?, ?, ?>> integrationControllerList = List.of(
                new EstoqueIntegrationController(),
                new SelloutIntegrationController(),
                new MaterialIntegrationController(),
                new ParametrosMaterialLocationIntegrationController(),
                new LinhaTransporteMaterialIntegrationController());

        for (IntegrationControllerAbstract<?, ?, ?, ?, ?, ?> integrationController : integrationControllerList) {
            Assertions.assertEquals(
                    List.of("api/secured/data/file/"),
                    integrationController.getRootFilePaths(),
                    integrationController.getClass().getSimpleName()
                            + " must not register a historical FILE dataupload alias.");
            Assertions.assertEquals(
                    List.of("api/secured/data/"),
                    integrationController.getRootJsonPaths(),
                    integrationController.getClass().getSimpleName()
                            + " must not register a historical JSON dataupload alias.");
        }

    }

    @Test
    public void dynamicIntegrationControllerSubPathsShouldStayInApprovedOperationalSet() throws Exception {

        Map<Class<?>, String> actualSubpathsByController = new LinkedHashMap<>();

        /*
         * Estes controllers herdam GET/POST FILE, GET/POST JSON e DELETE JSON
         * de IntegrationControllerAbstract. O valor do subPath e o contrato
         * publico que define quais dominios de carga realmente existem no
         * Community.
         */
        for (Class<? extends IntegrationControllerAbstract<?, ?, ?, ?, ?, ?>> controllerClass : COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS.keySet()) {
            IntegrationControllerAbstract<?, ?, ?, ?, ?, ?> integrationControllerAbstract =
                    controllerClass.getDeclaredConstructor().newInstance();
            Method getSubPathMethod = controllerClass.getDeclaredMethod("getSubPath");
            getSubPathMethod.setAccessible(true);
            actualSubpathsByController.put(
                    controllerClass,
                    (String) getSubPathMethod.invoke(integrationControllerAbstract));
        }

        Assertions.assertEquals(
                COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS,
                actualSubpathsByController);

    }

    @Test
    public void genericCommunityDataUploadControllersShouldStayCoveredByDynamicSubpathAllowlist() throws IOException {

        Path dataUploadControllerRoot = resolveCommunityWebModuleDirectory()
                .resolve("src/main/java/com/opsfactor/community/web/restcontroller/dataupload");

        Set<String> expectedGenericControllerSimpleNameSet;
        try (var controllerPathStream = Files.walk(dataUploadControllerRoot)) {
            expectedGenericControllerSimpleNameSet = controllerPathStream
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .filter(path -> isGenericIntegrationController(path))
                    .map(path -> path.getFileName().toString().replace(".java", ""))
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        Set<String> actualDynamicControllerSimpleNameSet = COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS
                .keySet()
                .stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toCollection(TreeSet::new));

        /*
         * Controllers Community baseados na abstracao generica publicam
         * endpoints FILE/JSON dinamicos no bootstrap. Se uma classe nova ficar
         * apenas na allowlist declarada, o subPath publico deixa de ser testado.
         */
        Assertions.assertEquals(
                expectedGenericControllerSimpleNameSet,
                actualDynamicControllerSimpleNameSet,
                "Todo controller Community baseado em IntegrationControllerAbstract deve estar na allowlist dinamica.");

    }

    @Test
    public void communityDataUploadPackageShouldContainOnlyApprovedControllers() throws IOException {

        Path dataUploadControllerRoot = resolveCommunityWebModuleDirectory()
                .resolve("src/main/java/com/opsfactor/community/web/restcontroller/dataupload");

        Set<String> actualControllerSimpleNameSet;
        try (var controllerPathStream = Files.walk(dataUploadControllerRoot)) {
            actualControllerSimpleNameSet = controllerPathStream
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .map(path -> path.getFileName().toString().replace(".java", ""))
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        Set<String> expectedControllerSimpleNameSet = new TreeSet<>();
        COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS
                .keySet()
                .stream()
                .map(Class::getSimpleName)
                .forEach(expectedControllerSimpleNameSet::add);
        COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER
                .keySet()
                .stream()
                .map(Class::getSimpleName)
                .forEach(expectedControllerSimpleNameSet::add);

        /*
         * A lista de rotas/subPaths abaixo so protege classes conhecidas. Esta
         * guarda do pacote impede que um novo controller de carga Enterprise
         * seja criado no Community sem entrar explicitamente na allowlist.
         */
        Assertions.assertEquals(
                expectedControllerSimpleNameSet,
                actualControllerSimpleNameSet);

    }

    @Test
    public void declaredDataUploadEndpointSurfaceShouldStayApproved() {

        Map<Class<?>, List<Route>> actualDeclaredEndpointsByController = new LinkedHashMap<>();

        /*
         * Endpoints declarados por annotation sao excecoes ao registro dinamico
         * do abstrato: filtros por periodo, deactivate/delete e aliases legados.
         * A lista precisa ser explicita porque estes endpoints nao aparecem a
         * partir do subPath.
         */
        for (Class<?> controllerClass : COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER.keySet()) {
            actualDeclaredEndpointsByController.put(
                    controllerClass,
                    collectDeclaredRoutes(controllerClass));
        }

        Assertions.assertEquals(
                COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER,
                actualDeclaredEndpointsByController);

    }

    @Test
    public void dataUploadSurfaceShouldNotExposeDemandPlanningSupportSeriesSubpaths() {

        List<String> violations = new ArrayList<>();

        /*
         * Support/regression series exigem recorte Enterprise completo de
         * cadastro, projection, factory e engine; por isso nao podem aparecer
         * como endpoint isolado no Community nem na superfície de Data Upload.
         */
        COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS.forEach((controllerClass, subPath) -> {
            if (isSupportSeriesSubPath(subPath)) {
                violations.add(controllerClass.getSimpleName() + " dynamic subpath " + subPath);
            }
        });
        COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER.forEach((controllerClass, routeList) -> routeList.forEach(route -> {
            if (isSupportSeriesSubPath(route.path())) {
                violations.add(controllerClass.getSimpleName()
                        + " declared route "
                        + route.httpMethod()
                        + " "
                        + route.path());
            }
        }));

        Assertions.assertTrue(
                violations.isEmpty(),
                "Data Upload Community nao deve reabrir support series/timeseries isoladamente:\n"
                        + String.join("\n", violations));

    }

    @Test
    public void dataUploadSurfaceShouldNotExposeConfiguredViewSubpathsBeforeDedicatedRecorte() {

        List<String> violations = new ArrayList<>();

        /*
         * Configured Views existem no Community pelo fluxo funcional de
         * configuracao de usuario. A superficie legada de Data Upload, porem,
         * carrega tambem key figures, filtros e caracteristicas que dependem
         * de recorte Enterprise proprio; reabrir so o endpoint criaria uma
         * segunda porta de edicao sem a mesma semantica do front service.
         */
        COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS.forEach((controllerClass, subPath) -> {
            if (isConfiguredViewSubPath(subPath)) {
                violations.add(controllerClass.getSimpleName() + " dynamic subpath " + subPath);
            }
        });
        COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER.forEach((controllerClass, routeList) -> routeList.forEach(route -> {
            if (isConfiguredViewSubPath(route.path())) {
                violations.add(controllerClass.getSimpleName()
                        + " declared route "
                        + route.httpMethod()
                        + " "
                        + route.path());
            }
        }));

        Assertions.assertTrue(
                violations.isEmpty(),
                "Data Upload Community nao deve reabrir configuredview* antes do recorte completo:\n"
                        + String.join("\n", violations));

    }

    @Test
    public void dataUploadSurfaceShouldNotExposeAutoFitSubpathsBeforeDedicatedRecorte() {

        List<String> violations = new ArrayList<>();

        /*
         * AutoFit/regression tree nao e apenas uma carga de modelo/resultado:
         * depende de perfil, projection de parametros, runtime de simulacao e
         * engine conectada ao workflow. O Community bloqueia essa capability e
         * nao deve reabrir subpaths legados de Data Upload isoladamente.
         */
        COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS.forEach((controllerClass, subPath) -> {
            if (isAutoFitSubPath(subPath)) {
                violations.add(controllerClass.getSimpleName() + " dynamic subpath " + subPath);
            }
        });
        COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER.forEach((controllerClass, routeList) -> routeList.forEach(route -> {
            if (isAutoFitSubPath(route.path())) {
                violations.add(controllerClass.getSimpleName()
                        + " declared route "
                        + route.httpMethod()
                        + " "
                        + route.path());
            }
        }));

        Assertions.assertTrue(
                violations.isEmpty(),
                "Data Upload Community nao deve reabrir autofit antes do recorte completo:\n"
                        + String.join("\n", violations));

    }

    @Test
    public void dataUploadSurfaceShouldNotExposeInventoryOptimizationSubpathsBeforeDedicatedRecorte() {

        List<String> violations = new ArrayList<>();

        /*
         * Inventory Optimization combina cadastros, execution workflow,
         * projection propria, resultados simulados e BI. O Community publica
         * apenas politica operacional de estoque; a familia legada de
         * Inventory Optimization precisa de recorte funcional inteiro antes de
         * aparecer em Data Upload.
         */
        COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS.forEach((controllerClass, subPath) -> {
            if (isInventoryOptimizationSubPath(subPath)) {
                violations.add(controllerClass.getSimpleName() + " dynamic subpath " + subPath);
            }
        });
        COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER.forEach((controllerClass, routeList) -> routeList.forEach(route -> {
            if (isInventoryOptimizationSubPath(route.path())) {
                violations.add(controllerClass.getSimpleName()
                        + " declared route "
                        + route.httpMethod()
                        + " "
                        + route.path());
            }
        }));

        Assertions.assertTrue(
                violations.isEmpty(),
                "Data Upload Community nao deve reabrir inventoryoptimization antes do recorte completo:\n"
                        + String.join("\n", violations));

    }

    @Test
    public void dataUploadSurfaceShouldNotExposeRetiredTemplateFillUtilities() {

        List<String> violations = new ArrayList<>();

        /*
         * Utilitarios legados de preenchimento de template foram aposentados e
         * nao pertencem a superficie de Data Upload de nenhuma edicao.
         */
        COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS.forEach((controllerClass, subPath) -> {
            if (isRetiredTemplateFillUtilitySubPath(subPath)) {
                violations.add(controllerClass.getSimpleName() + " dynamic subpath " + subPath);
            }
        });
        COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER.forEach((controllerClass, routeList) -> routeList.forEach(route -> {
            if (isRetiredTemplateFillUtilitySubPath(route.path())) {
                violations.add(controllerClass.getSimpleName()
                        + " declared route "
                        + route.httpMethod()
                        + " "
                        + route.path());
            }
        }));

        Assertions.assertTrue(
                violations.isEmpty(),
                "Data Upload Community nao deve expor utilitarios legados de preenchimento de template:\n"
                        + String.join("\n", violations));

    }

    @Test
    public void dataUploadSurfaceShouldExposeTheBoundedDemandPlanDetailedExport() {

        Assertions.assertEquals(
                routes(
                        route("GET", "api/secured/data/file/demandplan/{demandPlanId}"),
                        route("GET", "api/secured/data/file/demandplan/{demandPlanId}/period/{referenceDate}")),
                COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER.get(DemandPlanDetailedExportController.class));

    }

    @Test
    public void dataUploadSurfaceShouldNotExposeEnterpriseOnlySubpaths() {

        List<String> violations = new ArrayList<>();

        /*
         * O Community pode publicar roots operacionais proprias, como
         * inventoryplan/{supplyPlanId}. As extensoes privadas ja migradas para
         * o overlay Enterprise, porem, nao podem reaparecer como subPath
         * dinamico nem como rota manual por simples copia do legado.
         */
        COMMUNITY_DYNAMIC_CONTROLLER_SUBPATHS.forEach((controllerClass, subPath) -> {
            if (isEnterpriseOnlyDataUploadSubPath(subPath)) {
                violations.add(controllerClass.getSimpleName() + " dynamic subpath " + subPath);
            }
        });
        COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER.forEach((controllerClass, routeList) -> routeList.forEach(route -> {
            if (isEnterpriseOnlyDataUploadSubPath(route.path())) {
                violations.add(controllerClass.getSimpleName()
                        + " declared route "
                        + route.httpMethod()
                        + " "
                        + route.path());
            }
        }));

        Assertions.assertTrue(
                violations.isEmpty(),
                "Data Upload Community nao deve publicar subpaths Enterprise-only:\n"
                        + String.join("\n", violations));

    }

    @Test
    public void declaredDataUploadEndpointsShouldRequireAdminRole() {

        List<String> violations = new ArrayList<>();

        /*
         * As rotas herdadas pelo abstrato fazem autorizacao manual porque sao
         * registradas em runtime. Ja rotas declaradas diretamente nos
         * controllers devem manter @Secured("ROLE_ADMIN") visivel no metodo.
         */
        for (Class<?> controllerClass : COMMUNITY_DECLARED_ENDPOINTS_BY_CONTROLLER.keySet()) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!hasSpringMappingAnnotation(method)) {
                    continue;
                }

                Secured secured = method.getAnnotation(Secured.class);
                if (secured == null || !Arrays.asList(secured.value()).contains("ROLE_ADMIN")) {
                    violations.add(controllerClass.getSimpleName() + "#" + method.getName());
                }
            }
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "Endpoints declarados de data upload Community devem exigir ROLE_ADMIN:\n"
                        + String.join("\n", violations));

    }

    @Test
    public void transactionalDataUploadControllersShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                EstoqueIntegrationController.class,
                List.of("estoqueIntegrationService"));
        assertRequiredAutowiredFields(
                SelloutIntegrationController.class,
                List.of(
                        "selloutIntegrationService",
                        "selloutFrontService"));

    }

    @Test
    public void manualMasterDataUploadControllersShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                LocationIntegrationController.class,
                List.of("locationIntegrationService"));
        assertRequiredAutowiredFields(
                InventoryPolicyIntegrationController.class,
                List.of("inventoryPolicyIntegrationService"));
        assertRequiredAutowiredFields(
                InventoryPolicyDetailIntegrationController.class,
                List.of("inventoryPolicyDetailIntegrationService"));
        assertRequiredAutowiredFields(
                VersaoMalhaIntegrationController.class,
                List.of("versaoMalhaIntegrationService"));
        assertRequiredAutowiredFields(
                LinhaTransporteIntegrationController.class,
                List.of("linhaTransporteIntegrationService"));
        assertRequiredAutowiredFields(
                UnidadeMedidaIntegrationController.class,
                List.of(
                        "unidadeMedidaIntegrationService",
                        "webControllerTaskSchedulingService"));
        assertRequiredAutowiredFields(
                OperacaoRoteiroIntegrationController.class,
                List.of(
                        "operacaoRoteiroIntegrationService",
                        "webControllerTaskSchedulingService"));

    }

    @Test
    public void planningDataUploadControllersShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertRequiredAutowiredFields(
                InventoryPlanIntegrationController.class,
                List.of("inventoryPlanIntegrationService"));

    }

    private static Map<Class<? extends IntegrationControllerAbstract<?, ?, ?, ?, ?, ?>>, String> createCommunityDynamicControllerSubpaths() {

        Map<Class<? extends IntegrationControllerAbstract<?, ?, ?, ?, ?, ?>>, String> communityDynamicControllerSubpaths =
                new LinkedHashMap<>();

        communityDynamicControllerSubpaths.put(ConversaoUnidadeIntegrationController.class, "unitconversion");
        communityDynamicControllerSubpaths.put(ConversaoUnidadeProdutoIntegrationController.class, "unitconversionmaterial");
        communityDynamicControllerSubpaths.put(LocationIntegrationController.class, "location");
        communityDynamicControllerSubpaths.put(MaterialIntegrationController.class, "material");
        communityDynamicControllerSubpaths.put(MaterialCharacteristicIntegrationController.class, "characteristic/material");
        communityDynamicControllerSubpaths.put(MaterialCharacteristicValueIntegrationController.class, "characteristic/material/value");
        communityDynamicControllerSubpaths.put(LocationCharacteristicIntegrationController.class, "characteristic/location");
        communityDynamicControllerSubpaths.put(LocationCharacteristicValueIntegrationController.class, "characteristic/location/value");
        communityDynamicControllerSubpaths.put(ParametrosMaterialLocationIntegrationController.class, "materiallocationparameters");
        communityDynamicControllerSubpaths.put(InventoryPolicyIntegrationController.class, "inventorypolicy");
        communityDynamicControllerSubpaths.put(InventoryPolicyDetailIntegrationController.class, "inventorypolicydetail");
        communityDynamicControllerSubpaths.put(VersaoMalhaIntegrationController.class, "supplynetworkversion");
        communityDynamicControllerSubpaths.put(LinhaTransporteIntegrationController.class, "transportationlane");
        communityDynamicControllerSubpaths.put(LinhaTransporteMaterialIntegrationController.class, "transportationlanematerial");
        communityDynamicControllerSubpaths.put(DisponibilidadeRecursoProdutivoIntegrationController.class, "productionresourceavailability");
        communityDynamicControllerSubpaths.put(ListaTecnicaComponenteIntegrationController.class, "bomcomponents");
        communityDynamicControllerSubpaths.put(ListaTecnicaIntegrationController.class, "bom");
        communityDynamicControllerSubpaths.put(RecursoProdutivoIntegrationController.class, "productionresource");
        communityDynamicControllerSubpaths.put(RoteiroIntegrationController.class, "productionrouting");
        communityDynamicControllerSubpaths.put(VersaoProducaoSimplesIntegrationController.class, "simpleproductionversion");
        communityDynamicControllerSubpaths.put(EstoqueIntegrationController.class, "stock");
        communityDynamicControllerSubpaths.put(SelloutIntegrationController.class, "sellout");
        communityDynamicControllerSubpaths.put(DistributionPlanIntegrationController.class, "distributionplan");
        communityDynamicControllerSubpaths.put(ProductionPlanVolumeIntegrationController.class, "productionplan/volume");
        communityDynamicControllerSubpaths.put(ProductionPlanOccupationIntegrationController.class, "productionplan/occupation");

        return communityDynamicControllerSubpaths;

    }

    private static Map<Class<?>, List<Route>> createCommunityDeclaredEndpointsByController() {

        Map<Class<?>, List<Route>> communityDeclaredEndpointsByController = new LinkedHashMap<>();

        /*
         * Controllers dinamicos sem endpoints manuais. As rotas FILE/JSON sao
         * cobertas pelo contrato dos subPath acima.
         */
        communityDeclaredEndpointsByController.put(ConversaoUnidadeIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(ConversaoUnidadeProdutoIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(DisponibilidadeRecursoProdutivoIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(ListaTecnicaComponenteIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(ListaTecnicaIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(InventoryPolicyIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(InventoryPolicyDetailIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(VersaoMalhaIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(LinhaTransporteIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(RecursoProdutivoIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(RoteiroIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(VersaoProducaoSimplesIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(MaterialCharacteristicIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(MaterialCharacteristicValueIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(LocationCharacteristicIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(LocationCharacteristicValueIntegrationController.class, List.of());
        communityDeclaredEndpointsByController.put(
                DistributionPlanIntegrationController.class,
                routes(
                        route("GET", "api/secured/data/distributionplan/{supplyPlanId}"),
                        route("GET", "api/secured/data/file/distributionplan/{supplyPlanId}")));
        communityDeclaredEndpointsByController.put(
                ProductionPlanVolumeIntegrationController.class,
                routes(
                        route("GET", "api/secured/data/file/productionplan/volume/{supplyPlanId}"),
                        route("GET", "api/secured/data/productionplan/volume/{supplyPlanId}")));
        communityDeclaredEndpointsByController.put(
                ProductionPlanOccupationIntegrationController.class,
                routes(
                        route("GET", "api/secured/data/file/productionplan/occupation/{supplyPlanId}"),
                        route("GET", "api/secured/data/productionplan/occupation/{supplyPlanId}")));

        communityDeclaredEndpointsByController.put(
                DemandPlanDetailedExportController.class,
                routes(
                        route("GET", "api/secured/data/file/demandplan/{demandPlanId}"),
                        route("GET", "api/secured/data/file/demandplan/{demandPlanId}/period/{referenceDate}")));

        communityDeclaredEndpointsByController.put(
                LocationIntegrationController.class,
                routes(
                        route("POST", "api/secured/data/location/deactivate")));

        communityDeclaredEndpointsByController.put(
                MaterialIntegrationController.class,
                routes(
                        route("POST", "api/secured/data/material/deactivate")));

        communityDeclaredEndpointsByController.put(
                ParametrosMaterialLocationIntegrationController.class,
                routes(
                        route("DELETE", "api/secured/data/materiallocationparameters"),
                        route("POST", "api/secured/data/materiallocationparameters/deactivate")));

        communityDeclaredEndpointsByController.put(
                UnidadeMedidaIntegrationController.class,
                routes(
                        route("GET", "api/secured/data/file/unitofmeasure"),
                        route("GET", "api/secured/data/unitofmeasure"),
                        route("POST", "api/secured/data/file/unitofmeasure"),
                        route("POST", "api/secured/data/unitofmeasure")));

        communityDeclaredEndpointsByController.put(
                LinhaTransporteMaterialIntegrationController.class,
                routes(
                        route("POST", "api/secured/data/transportationlanematerial/deactivate")));

        communityDeclaredEndpointsByController.put(
                OperacaoRoteiroIntegrationController.class,
                routes(
                        route("GET", "api/secured/data/file/operationproductionrouting"),
                        route("POST", "api/secured/data/file/operationproductionrouting")));

        communityDeclaredEndpointsByController.put(
                FulfilledDemandIntegrationController.class,
                routes(
                        route("GET", "api/secured/data/file/fulfilleddemand/{supplyPlanId}"),
                        route("GET", "api/secured/data/fulfilleddemand/{supplyPlanId}"),
                        route("GET", "api/secured/data/file/fulfilleddemand/{supplyPlanId}/period/{referenceDate}"),
                        route("GET", "api/secured/data/fulfilleddemand/{supplyPlanId}/period/{referenceDate}")));

        communityDeclaredEndpointsByController.put(
                InventoryPlanIntegrationController.class,
                routes(
                        route("GET", "api/secured/data/file/inventoryplan/{supplyPlanId}"),
                        route("GET", "api/secured/data/inventoryplan/{supplyPlanId}")));

        communityDeclaredEndpointsByController.put(
                EstoqueIntegrationController.class,
                routes(
                        route("DELETE", "api/secured/data/stock/{dataInicial}/{dataFinal}"),
                        route("GET", "api/secured/data/file/stock/{dataInicial}/{dataFinal}"),
                        route("GET", "api/secured/data/stock/{dataInicial}/{dataFinal}")));

        communityDeclaredEndpointsByController.put(
                SelloutIntegrationController.class,
                routes(
                        route("DELETE", "api/secured/data/sellout/{dataInicial}/{dataFinal}"),
                        route("GET", "api/secured/data/file/sellout/{dataInicial}/{dataFinal}"),
                        route("GET", "api/secured/data/sellout/{dataInicial}/{dataFinal}"),
                        route("GET", "api/secured/sellout/firstandlastdate"),
                        /*
                         * Relatorio historico read-only para o consumidor
                         * AgGrid legado. Ele reutiliza a fachada Community de
                         * sell-out e nao reabre cargas, pedidos ou series
                         * Enterprise.
                         */
                        route("POST", "api/secured/historical/sellout")));

        return communityDeclaredEndpointsByController;

    }

    private static List<Route> collectDeclaredRoutes(Class<?> controllerClass) {

        List<Route> routes = new ArrayList<>();

        for (Method method : controllerClass.getDeclaredMethods()) {
            addRoutes(routes, method.getAnnotation(GetMapping.class), "GET");
            addRoutes(routes, method.getAnnotation(PostMapping.class), "POST");
            addRoutes(routes, method.getAnnotation(DeleteMapping.class), "DELETE");
            addRoutes(routes, method.getAnnotation(PutMapping.class), "PUT");
            addRoutes(routes, method.getAnnotation(PatchMapping.class), "PATCH");
            addRequestMappingRoutes(routes, method.getAnnotation(RequestMapping.class));
        }

        return sortRoutes(routes);

    }

    private static void addRoutes(List<Route> routes, GetMapping getMapping, String httpMethod) {

        if (getMapping == null) {
            return;
        }

        addPaths(routes, httpMethod, getMapping.value(), getMapping.path());

    }

    private static void addRoutes(List<Route> routes, PostMapping postMapping, String httpMethod) {

        if (postMapping == null) {
            return;
        }

        addPaths(routes, httpMethod, postMapping.value(), postMapping.path());

    }

    private static void addRoutes(List<Route> routes, DeleteMapping deleteMapping, String httpMethod) {

        if (deleteMapping == null) {
            return;
        }

        addPaths(routes, httpMethod, deleteMapping.value(), deleteMapping.path());

    }

    private static void addRoutes(List<Route> routes, PutMapping putMapping, String httpMethod) {

        if (putMapping == null) {
            return;
        }

        addPaths(routes, httpMethod, putMapping.value(), putMapping.path());

    }

    private static void addRoutes(List<Route> routes, PatchMapping patchMapping, String httpMethod) {

        if (patchMapping == null) {
            return;
        }

        addPaths(routes, httpMethod, patchMapping.value(), patchMapping.path());

    }

    private static void addRequestMappingRoutes(List<Route> routes, RequestMapping requestMapping) {

        if (requestMapping == null) {
            return;
        }

        List<String> paths = collectPaths(requestMapping.value(), requestMapping.path());
        List<RequestMethod> requestMethods = Arrays.asList(requestMapping.method());
        if (requestMethods.isEmpty()) {
            throw new AssertionError("@RequestMapping sem metodo HTTP explicito nao deve ser usado em data upload Community");
        }

        for (RequestMethod requestMethod : requestMethods) {
            for (String path : paths) {
                routes.add(route(requestMethod.name(), path));
            }
        }

    }

    private static void addPaths(
            List<Route> routes,
            String httpMethod,
            String[] annotationValues,
            String[] annotationPaths) {

        for (String path : collectPaths(annotationValues, annotationPaths)) {
            routes.add(route(httpMethod, path));
        }

    }

    private static List<String> collectPaths(String[] annotationValues, String[] annotationPaths) {

        List<String> paths = new ArrayList<>();
        paths.addAll(Arrays.asList(annotationValues));
        paths.addAll(Arrays.asList(annotationPaths));

        return paths;

    }

    private static boolean hasSpringMappingAnnotation(Method method) {

        return method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(RequestMapping.class);

    }

    private static void assertRequiredAutowiredFields(
            Class<?> controllerClass,
            List<String> fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = controllerClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    controllerClass.getSimpleName() + "." + fieldName + " must declare @Autowired explicitly");
            Assertions.assertTrue(
                    autowired.required(),
                    controllerClass.getSimpleName() + "." + fieldName + " must be a required Spring bean");
        }

    }

    private static boolean isGenericIntegrationController(Path controllerSourcePath) {

        try {
            return Files.readString(controllerSourcePath)
                    .contains("extends IntegrationControllerAbstract<");
        } catch (IOException ioException) {
            throw new IllegalStateException(
                    "Nao foi possivel ler controller Community " + controllerSourcePath,
                    ioException);
        }

    }

    private static Route route(String httpMethod, String path) {

        return new Route(
                httpMethod,
                path.startsWith("/") ? path.substring(1) : path);

    }

    private static List<Route> routes(Route... routes) {

        return sortRoutes(new ArrayList<>(Arrays.asList(routes)));

    }

    private static List<Route> sortRoutes(List<Route> routes) {

        routes.sort(
                Comparator.comparing(Route::httpMethod)
                        .thenComparing(Route::path));
        return List.copyOf(routes);

    }

    private static boolean isSupportSeriesSubPath(String subPath) {

        return subPath != null
                && subPath.toLowerCase().contains("timeseries");

    }

    private static boolean isConfiguredViewSubPath(String subPath) {

        return subPath != null
                && subPath.toLowerCase().contains("configuredview");

    }

    private static boolean isAutoFitSubPath(String subPath) {

        return subPath != null
                && subPath.toLowerCase().contains("autofit");

    }

    private static boolean isInventoryOptimizationSubPath(String subPath) {

        if (subPath == null) {
            return false;
        }

        String normalizedSubPath = subPath.toLowerCase();
        return normalizedSubPath.contains("inventoryoptimization")
                || normalizedSubPath.contains("replenishmentanddemandvariation");

    }

    private static boolean isRetiredTemplateFillUtilitySubPath(String subPath) {

        return subPath != null
                && subPath.toLowerCase().contains("fillwithdemandplan");

    }

    private static boolean isDemandPlanDetailedExportSubPath(String subPath) {

        if (subPath == null) {
            return false;
        }

        String normalizedSubPath = subPath.toLowerCase();
        return "demandplan".equals(normalizedSubPath)
                || normalizedSubPath.startsWith("demandplan/")
                || normalizedSubPath.contains("/dataupload/demandplan/")
                || normalizedSubPath.contains("/data/file/demandplan/");

    }

    private static boolean isEnterpriseOnlyDataUploadSubPath(String subPathOrRoute) {

        if (subPathOrRoute == null) {
            return false;
        }

        /*
         * Rotas manuais carregam prefixos FILE/JSON diferentes. Para comparar
         * com a mesma lista usada pelos subPaths dinamicos, reduzimos a string
         * ao trecho funcional depois da raiz de Data Upload.
         */
        String normalizedSubPath = extractDataUploadSubPath(subPathOrRoute.toLowerCase());
        return ENTERPRISE_ONLY_DATA_UPLOAD_SUBPATH_FAMILIES
                .stream()
                .anyMatch(enterpriseOnlyDataUploadSubPathFamily ->
                        normalizedSubPath.startsWith(enterpriseOnlyDataUploadSubPathFamily));

    }

    private static String extractDataUploadSubPath(String normalizedSubPathOrRoute) {

        String normalizedCandidate = normalizedSubPathOrRoute.startsWith("/")
                ? normalizedSubPathOrRoute.substring(1)
                : normalizedSubPathOrRoute;

        for (String dataUploadRoutePrefix : DATA_UPLOAD_ROUTE_PREFIXES) {
            if (normalizedCandidate.startsWith(dataUploadRoutePrefix)) {
                return normalizedCandidate.substring(dataUploadRoutePrefix.length());
            }
        }

        return normalizedCandidate;

    }

    private Path resolveCommunityWebModuleDirectory() {

        Path currentWorkingDirectory = Path.of("").toAbsolutePath();
        while (currentWorkingDirectory != null
                && !"opsfactor-community".equals(currentWorkingDirectory.getFileName().toString())) {
            currentWorkingDirectory = currentWorkingDirectory.getParent();
        }
        if (currentWorkingDirectory == null) {
            throw new IllegalStateException("Could not resolve opsfactor-community workspace directory.");
        }
        return currentWorkingDirectory;

    }

    private record Route(String httpMethod, String path) {
    }

}
