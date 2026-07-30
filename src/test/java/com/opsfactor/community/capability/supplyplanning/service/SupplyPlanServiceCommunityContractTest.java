package com.opsfactor.community.capability.supplyplanning.service;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoInexistente;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaDAO;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionPorDfu;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoSupplyPlanRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.distributionplan.repository.DistributionPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.repository.InventoryPlanLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.ProductionPlanLinhaRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.masterdata.production.productionversion.service.VersaoProducaoService;
import com.opsfactor.community.platform.exception.SupplyPlanException;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanExecutionServiceSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanFirmProductionOrdersSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanOpenOrdersHeuristicSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanOptimizationServiceSpi;
import com.opsfactor.community.capability.supplyplanning.service.spi.SupplyPlanProcessChainServiceSpi;
import com.opsfactor.community.capability.supplyplanning.service.heuristic.ConstrainedPlanService;
import com.opsfactor.community.capability.supplyplanning.service.heuristic.HeuristicoService;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Valida o mecanismo Community/Enterprise de Supply Planning sem subir Spring.
 *
 * <p>O Community nao possui beans de otimizador nem process chain. Esses campos
 * sao explicitamente opcionais no service e devem falhar com
 * RequiresEnterpriseVersionException quando o perfil solicitar uma feature
 * Enterprise sem que o overlay privado esteja no classpath.</p>
 */
public class SupplyPlanServiceCommunityContractTest {

    @Test
    public void serviceShouldUseExplicitAutowiredBeanFieldsWithOnlyEnterpriseSpisOptional() throws Exception {

        assertAutowiredFields(
                true,
                "demandPlanningService",
                "versaoProducaoService",
                "heuristicoService",
                "capacidadeEfetivaSupplyPlanService",
                "demandPlanRepository",
                "supplyPlanRepository",
                "versaoMalhaRepository",
                "distributionPlanItemRepository",
                "productionPlanLinhaRepository",
                "inventoryPlanLinhaRepository",
                "perfilExecucaoSupplyPlanRepository",
                "estoqueRepository",
                "demandaDiretaConsideradaLinhaRepository",
                "demandaDiretaConsideradaLinhaDAO",
                "supplyPlanProjectionFactory",
                "supplyNetworkProjectionFactory",
                "clusterEParametrosProjectionFactory",
                "conversaoUnidadeMedidaProjectionFactory",
                "estoqueProjectionFactory",
                "splitTemporalProjectionFactory",
                "politicaEstoquesProjectionFactory",
                "biProjectionCapacidadeProdutivaFactory",
                "jdbcTemplate",
                "supplyPlanDemandCatchUpProjectionSpi");

        assertAutowiredFields(
                false,
                "supplyPlanOptimizationService",
                "supplyPlanProcessChainService",
                "supplyPlanPresetConstraintGroupSpi",
                "supplyPlanExecutionProfileLocationScope",
                "supplyPlanExecutionProfileMaterialScope",
                "supplyPlanFirmProductionOrdersSpi");

    }

    @Test
    public void getSupplyPlanListShouldValidateAndSortRepositorySnapshot() throws Exception {

        SupplyPlan supplyPlanDois = new SupplyPlan();
        supplyPlanDois.setId(2L);
        SupplyPlan supplyPlanUm = new SupplyPlan();
        supplyPlanUm.setId(1L);

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComSelector(List.of(supplyPlanDois, supplyPlanUm)));

        /*
         * A lista vinda do repository pode chegar fora de ordem, mas cada plano
         * precisa carregar id funcional. A ordenacao deterministica alimenta a
         * selecao do front sem depender da ordem fisica do banco.
         */
        List<SupplyPlan> supplyPlans = supplyPlanService.getSupplyPlanList();

        Assertions.assertEquals(
                List.of(1L, 2L),
                supplyPlans.stream()
                        .map(SupplyPlan::getId)
                        .toList());

    }

    @Test
    public void getSupplyPlanListShouldRejectBrokenRepositorySnapshotBeforeSorting() throws Exception {

        SupplyPlanService supplyPlanServiceComListaNula = new SupplyPlanService();
        setField(
                supplyPlanServiceComListaNula,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComSelector(null));

        IllegalStateException listaNulaException = Assertions.assertThrows(
                IllegalStateException.class,
                supplyPlanServiceComListaNula::getSupplyPlanList);

        Assertions.assertEquals(
                "Supply Plan repository returned null list for Supply Plan listing.",
                listaNulaException.getMessage());

        List<SupplyPlan> supplyPlansComItemNulo = new ArrayList<>();
        supplyPlansComItemNulo.add(null);
        SupplyPlanService supplyPlanServiceComItemNulo = new SupplyPlanService();
        setField(
                supplyPlanServiceComItemNulo,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComSelector(supplyPlansComItemNulo));

        IllegalStateException itemNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                supplyPlanServiceComItemNulo::getSupplyPlanList);

        Assertions.assertEquals(
                "Supply Plan repository returned null item at index 0 for Supply Plan listing.",
                itemNuloException.getMessage());

        SupplyPlan supplyPlanSemId = new SupplyPlan();
        SupplyPlanService supplyPlanServiceComPlanoSemId = new SupplyPlanService();
        setField(
                supplyPlanServiceComPlanoSemId,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComSelector(List.of(supplyPlanSemId)));

        IllegalStateException planoSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                supplyPlanServiceComPlanoSemId::getSupplyPlanList);

        Assertions.assertEquals(
                "Supply Plan repository returned item without id at index 0 for Supply Plan listing.",
                planoSemIdException.getMessage());

    }

    @Test
    public void reexecutionWithoutDirectDemandSnapshotShouldRebuildIt() throws Exception {

        List<Long> consultedSupplyPlanIds = new ArrayList<>();
        SupplyPlanService supplyPlanService = new SupplyPlanService();
        setField(
                supplyPlanService,
                "demandaDiretaConsideradaLinhaRepository",
                getDemandaDiretaConsideradaLinhaRepositoryComExistencia(false, consultedSupplyPlanIds));

        boolean deveGerarFotografia = supplyPlanService.deveGerarDemandaDiretaConsiderada(false, 42L);

        Assertions.assertTrue(deveGerarFotografia);
        Assertions.assertEquals(List.of(42L), consultedSupplyPlanIds);

    }

    @Test
    public void reexecutionWithDirectDemandSnapshotShouldNotRebuildIt() throws Exception {

        List<Long> consultedSupplyPlanIds = new ArrayList<>();
        SupplyPlanService supplyPlanService = new SupplyPlanService();
        setField(
                supplyPlanService,
                "demandaDiretaConsideradaLinhaRepository",
                getDemandaDiretaConsideradaLinhaRepositoryComExistencia(true, consultedSupplyPlanIds));

        boolean deveGerarFotografia = supplyPlanService.deveGerarDemandaDiretaConsiderada(false, 42L);

        Assertions.assertFalse(deveGerarFotografia);
        Assertions.assertEquals(List.of(42L), consultedSupplyPlanIds);

    }

    @Test
    public void constrainedPlanServiceShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                ConstrainedPlanService.class,
                true,
                "supplyPlanRepository",
                "productionPlanLinhaRepository",
                "distributionPlanItemRepository",
                "supplyPlanService",
                "supplyPlanProjectionFactory",
                "parametrosProjectionFactory",
                "supplyNetworkProjectionFactory",
                "inventoryPlanLinhaRepository",
                "demandaDiretaConsideradaLinhaRepository",
                "politicaEstoquesProjectionFactory",
                "biProjectionCapacidadeProdutivaFactory");

        assertAutowiredFields(
                ConstrainedPlanService.class,
                false,
                "supplyPlanExecutionProfileMaterialScope");

    }

    @Test
    public void heuristicServiceShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                HeuristicoService.class,
                true,
                "supplyPlanService",
                "constrainedPlanService",
                "supplyPlanBiProjectionFactory",
                "nivelamentoCapacidadePlanoIrrestritoHeuristicoService");

    }

    @Test
    public void supplyHeuristicSourcesShouldNotKeepLegacyDemandProjectionPopulationCalls() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<Path> supplyHeuristicSourcePaths = List.of(
                communityWorkspaceDirectory.resolve(
                        "src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/HeuristicoService.java"),
                communityWorkspaceDirectory.resolve(
                        "src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/ConstrainedPlanService.java"));
        List<String> violations = new ArrayList<>();

        /*
         * A DemandPlanningProjection e populada no orquestrador principal antes
         * de materializar a demanda direta considerada. Os motores heuristico e
         * restrito trabalham apenas com SupplyPlanningProjection; manter
         * chamadas antigas comentadas aqui sugere um acoplamento que o recorte
         * Community ja eliminou.
         */
        for (Path supplyHeuristicSourcePath : supplyHeuristicSourcePaths) {
            List<String> sourceLines = Files.readAllLines(
                    supplyHeuristicSourcePath,
                    StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (sourceLine.contains("planningProjectionFactory.populaDemandPlanningProjectionComDemandPlan")) {
                    violations.add(
                            communityWorkspaceDirectory.relativize(supplyHeuristicSourcePath)
                                    + ":"
                                    + (lineIndex + 1)
                                    + ": "
                                    + sourceLine.trim());
                }
            }
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "Fluxos heuristico/restrito de Supply Planning Community nao devem manter chamada legada comentada para DemandPlanningProjection:\n"
                        + String.join("\n", violations));

    }

    @Test
    public void supplyPlanServiceShouldNotKeepCommentedInventoryFlushAlternatives() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path supplyPlanServiceSourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java");
        List<String> sourceLines = Files.readAllLines(
                supplyPlanServiceSourcePath,
                StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        /*
         * Persistencia Community deve declarar flush apenas quando ele faz
         * parte do contrato ativo do metodo. Um flush comentado apos saveAll
         * parece alternativa operacional pendente e enfraquece a leitura do
         * fluxo batch.
         */
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.trim().startsWith("//")
                    && sourceLine.contains("inventoryPlanLinhaRepository.flush()")) {
                violations.add(
                        communityWorkspaceDirectory.relativize(supplyPlanServiceSourcePath)
                                + ":"
                                + (lineIndex + 1)
                                + ": "
                                + sourceLine.trim());
            }
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "SupplyPlanService nao deve manter flush de inventory plan comentado como alternativa de persistencia:\n"
                        + String.join("\n", violations));

    }

    @Test
    public void supplyExecutionSpiShouldDocumentPersistedSupplyPlanHeaderContract() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path supplyPlanExecutionServiceSpiSourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/service/spi/SupplyPlanExecutionServiceSpi.java");
        String supplyPlanExecutionServiceSpiSource = Files.readString(
                supplyPlanExecutionServiceSpiSourcePath,
                StandardCharsets.UTF_8);

        /*
         * Process chain Enterprise reentra no fluxo principal antes das
         * projections compartilhadas, mas as etapas atomicas persistem artefatos
         * sobre um unico Supply Plan fisico. Esse contrato precisa estar
         * documentado na SPI Community para evitar overlays que tentem executar
         * sobre um header ainda sem id.
         */
        Assertions.assertTrue(
                supplyPlanExecutionServiceSpiSource.contains("o header do Supply Plan ja")
                        && supplyPlanExecutionServiceSpiSource.contains("id funcional")
                        && supplyPlanExecutionServiceSpiSource.contains("mesmo plano fisico"),
                "SupplyPlanExecutionServiceSpi deve documentar que process chains exigem header persistido com id.");

    }

    @Test
    public void heuristicExecutionShouldRejectMissingSupplyPlanBeforeLowLevelCode() {

        HeuristicoService heuristicoService = new HeuristicoService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> heuristicoService.executaSupplyPlanHeuristico(
                        null,
                        perfilExecucaoSupplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null));

        /*
         * O motor heuristico persiste linhas por Supply Plan. Plano ausente
         * deve falhar antes de montar LowLevelCode ou tocar qualquer factory.
         */
        Assertions.assertEquals(
                "Supply Plan is required for heuristic Supply Planning execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void heuristicExecutionShouldRejectMissingSupplyPlanIdBeforeLowLevelCode() {

        HeuristicoService heuristicoService = new HeuristicoService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> heuristicoService.executaSupplyPlanHeuristico(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null));

        Assertions.assertEquals(
                "Supply Plan id is required for heuristic Supply Planning execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void heuristicExecutionShouldRejectEnterpriseModeBeforeLowLevelCode() {

        HeuristicoService heuristicoService = new HeuristicoService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> heuristicoService.executaSupplyPlanHeuristico(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null));

        /*
         * Optimizer e process chain nao devem cair no motor heuristico mesmo
         * se alguem chamar o service diretamente fora do orquestrador.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning execution engine requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void heuristicExecutionShouldRejectMissingSupplyNetworkBeforeLowLevelCode() {

        HeuristicoService heuristicoService = new HeuristicoService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> heuristicoService.executaSupplyPlanHeuristico(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null));

        Assertions.assertEquals(
                "Supply Network projection is required for heuristic Supply Planning execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void heuristicExecutionShouldRejectMissingClusterBeforeLowLevelCode() {

        HeuristicoService heuristicoService = new HeuristicoService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> heuristicoService.executaSupplyPlanHeuristico(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        supplyNetworkProjection,
                        null,
                        null,
                        null,
                        null));

        /*
         * LowLevelCode depende da fotografia de cluster/material/location
         * carregada na malha. A validação deve ocorrer antes de instanciar a
         * rotina compartilhada.
         */
        Assertions.assertEquals(
                "Cluster and parameters projection is required for heuristic Supply Planning execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void heuristicExecutionShouldRejectMissingGlobalParametersBeforeLowLevelCode() throws Exception {

        HeuristicoService heuristicoService = new HeuristicoService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);
        setField(
                supplyNetworkProjection,
                "clusterEParametrosProjection",
                new ClusterEParametrosProjection());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> heuristicoService.executaSupplyPlanHeuristico(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        supplyNetworkProjection,
                        null,
                        null,
                        null,
                        null));

        /*
         * ParametrosGlobais definem o calendario do plano e fazem parte da
         * fotografia estrutural de Supply Planning. Falhar aqui evita que uma
         * malha parcialmente montada vire NPE dentro do LowLevelCode ou no
         * encadeamento do plano restrito.
         */
        Assertions.assertEquals(
                "Global parameters are required for heuristic Supply Planning execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void constrainedPlanShouldRejectLogisticConstraintsAtRuntime() throws Exception {

        ConstrainedPlanService constrainedPlanService = new ConstrainedPlanService();

        Assertions.assertDoesNotThrow(
                () -> invokeValidaRestricoesLogisticasCommunity(
                        constrainedPlanService,
                        new PerfilExecucaoSupplyPlan()));

        PerfilExecucaoSupplyPlan perfilComRestricaoOutbound = new PerfilExecucaoSupplyPlan() {

            @Override
            public boolean getConsideraRestricaoOutbound() {
                return true;
            }

        };

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaRestricoesLogisticasCommunity(
                        constrainedPlanService,
                        perfilComRestricaoOutbound));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    @Test
    public void constrainedPlanByIdShouldRejectNullRepositoryOptionalBeforeProfileUse() throws Exception {

        ConstrainedPlanService constrainedPlanService = new ConstrainedPlanService();
        setField(
                constrainedPlanService,
                "supplyPlanRepository",
                getRepositoryProxyRetornandoOptionalNulo(
                        SupplyPlanRepository.class,
                        "findById"));

        /*
         * A entrada publica por id recarrega o Supply Plan antes de validar o
         * perfil heuristico. Um Optional nulo do repository e falha estrutural
         * e nao deve virar NPE tardio nem se misturar ao caso funcional de
         * Supply Plan inexistente.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> constrainedPlanService.restringePlanoComPerfilHeuristico(42L));

        Assertions.assertEquals(
                "Supply Plan repository returned null Optional for constrained heuristic execution id 42.",
                illegalStateException.getMessage());

    }

    @Test
    public void constrainedPlanByIdShouldRejectBrokenSupplyPlanSnapshotBeforeProfileUse() throws Exception {

        ConstrainedPlanService constrainedPlanServiceComPlanoSemId = new ConstrainedPlanService();
        setField(
                constrainedPlanServiceComPlanoSemId,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComSupplyPlan(new SupplyPlan()));

        /*
         * O repository encontrou uma entidade, mas a identidade funcional nao
         * veio materializada. A restricao por id deve falhar antes de perfil,
         * factories de projection e updates massivos.
         */
        IllegalStateException planoSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> constrainedPlanServiceComPlanoSemId.restringePlanoComPerfilHeuristico(42L));
        Assertions.assertEquals(
                "Supply Plan snapshot id is required for constrained heuristic execution id 42.",
                planoSemIdException.getMessage());

        SupplyPlan supplyPlanDivergente = new SupplyPlan();
        supplyPlanDivergente.setId(43L);
        ConstrainedPlanService constrainedPlanServiceComPlanoDivergente = new ConstrainedPlanService();
        setField(
                constrainedPlanServiceComPlanoDivergente,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComSupplyPlan(supplyPlanDivergente));

        IllegalStateException planoDivergenteException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> constrainedPlanServiceComPlanoDivergente.restringePlanoComPerfilHeuristico(42L));
        Assertions.assertEquals(
                "Supply Plan snapshot id must match constrained heuristic execution id 42.",
                planoDivergenteException.getMessage());

    }

    @Test
    public void constrainedPlanDirectExecutionShouldRejectMissingProfileBeforeProjectionUse() {

        ConstrainedPlanService constrainedPlanService = new ConstrainedPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(42L);

        /*
         * A sobrecarga projection-aware tambem e ponto de entrada interno.
         * Perfil nulo deve falhar como contrato do caller antes de qualquer
         * acesso a projections, que neste teste permanecem nulas de proposito.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> constrainedPlanService.restringePlano(
                        supplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        Assertions.assertEquals(
                "Constrained Supply Planning execution profile is null.",
                illegalStateException.getMessage());

    }

    @Test
    public void constrainedPlanDirectExecutionShouldRejectEnterpriseModeBeforeProjectionUse() {

        ConstrainedPlanService constrainedPlanService = new ConstrainedPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);

        /*
         * O plano restrito direto e parte do heuristico Community. Optimizer e
         * process chain devem ser resolvidos por seus services Enterprise, nao
         * cair neste metodo com projections ja materializadas.
         */
        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> constrainedPlanService.restringePlano(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null));

        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning execution engine requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void constrainedPlanDirectExecutionShouldRejectMissingSupplyPlanBeforeProjectionUse() {

        ConstrainedPlanService constrainedPlanService = new ConstrainedPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> constrainedPlanService.restringePlano(
                        null,
                        perfilExecucaoSupplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null));

        /*
         * A sobrecarga projection-aware e uma entrada interna compartilhada.
         * Plano ausente deve falhar antes do log, updates massivos ou acesso a
         * snapshots que tambem estao nulos neste teste.
         */
        Assertions.assertEquals(
                "Supply Plan is required for constrained heuristic execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void constrainedPlanDirectExecutionShouldRejectMissingCalendarBeforeProjectionUse() {

        ConstrainedPlanService constrainedPlanService = new ConstrainedPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> constrainedPlanService.restringePlano(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null));

        /*
         * Com perfil heuristico valido, a proxima borda e o calendario ja
         * materializado pelo orquestrador. A rotina nao deve chegar aos
         * repositories de reset sem essa fotografia temporal.
         */
        Assertions.assertEquals(
                "Calendar is required for constrained heuristic execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void constrainedPlanDirectExecutionShouldRejectMissingSupplyNetworkProjectionBeforeProjectionUse() {

        ConstrainedPlanService constrainedPlanService = new ConstrainedPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> constrainedPlanService.restringePlano(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        criaCalendarioConstrainedPlanningTeste(),
                        null,
                        null,
                        null,
                        null));

        /*
         * A malha em memoria contem cluster/material/location e estrutura
         * produtiva. Ela deve ser obrigatoria antes de qualquer projection de
         * location/material ou reset de linhas calculadas.
         */
        Assertions.assertEquals(
                "Supply Network projection is required for constrained heuristic execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void constrainedPlanDirectExecutionShouldRejectMissingClusterProjectionBeforeLowLevelCodeUse() {

        ConstrainedPlanService constrainedPlanService = new ConstrainedPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        SupplyNetworkProjection supplyNetworkProjection = new SupplyNetworkProjection();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> constrainedPlanService.restringePlano(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        criaCalendarioConstrainedPlanningTeste(),
                        supplyNetworkProjection,
                        null,
                        null,
                        null));

        /*
         * SupplyNetworkProjection vazia nao e uma malha valida para o plano
         * restrito: sem cluster/parametros nao ha como construir projections
         * material/location de cada low level code.
         */
        Assertions.assertEquals(
                "Cluster and parameters projection is required for constrained heuristic execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void constrainedPlanShouldRejectEnterpriseExecutionModeBeforeProjectionFactories() throws Exception {

        ConstrainedPlanService constrainedPlanService = new ConstrainedPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        supplyPlan.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan);
        setField(
                constrainedPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComSupplyPlan(supplyPlan));

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> constrainedPlanService.restringePlanoComPerfilHeuristico(42L));

        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning execution engine requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void validaModoExecucaoCommunityShouldAcceptHeuristicWithoutEnterpriseBeans() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);

        Assertions.assertDoesNotThrow(
                () -> invokeValidaModoExecucaoCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

    }

    @Test
    public void validaModoExecucaoCommunityShouldRejectAllNonHeuristicModesWithoutEnterpriseBeans() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();

        /*
         * Community publica apenas o motor heuristico. Os demais valores do
         * enum continuam no contrato para bloquear visualmente no front e para
         * permitir o overlay Enterprise quando o bean real existir.
         */
        for (PerfilExecucaoSupplyPlan.ModoExecucao modoExecucao : PerfilExecucaoSupplyPlan.ModoExecucao.values()) {
            if (PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO.equals(modoExecucao)) {
                continue;
            }

            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
            perfilExecucaoSupplyPlan.setModoExecucao(modoExecucao);

            InvocationTargetException invocationTargetException = Assertions.assertThrows(
                    InvocationTargetException.class,
                    () -> invokeValidaModoExecucaoCommunity(
                            supplyPlanService,
                            perfilExecucaoSupplyPlan));
            Assertions.assertInstanceOf(
                    RequiresEnterpriseVersionException.class,
                    invocationTargetException.getCause());
        }

    }

    @Test
    public void validaModoExecucaoCommunityShouldRejectMissingExecutionProfileExplicitly() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaModoExecucaoCommunity(
                        supplyPlanService,
                        null));

        Assertions.assertInstanceOf(
                IllegalStateException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Supply Planning execution profile is null.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void getSupplyPlanEnterpriseExecutionServiceShouldRejectMissingExecutionModeExplicitly() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetSupplyPlanEnterpriseExecutionService(
                        supplyPlanService,
                        null));

        Assertions.assertInstanceOf(
                IllegalStateException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Supply Planning execution mode is required.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldRejectTransactionalOrdersForHeuristicEvenWithEnterpriseOptimizerBean() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);
        perfilExecucaoSupplyPlan.setConsideraOrdensSelloutBacklog(true);
        setField(
                supplyPlanService,
                "supplyPlanOptimizationService",
                new StubSupplyPlanOptimizationService());

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaPedidosTransacionaisCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

        /*
         * O bean Enterprise pode existir no runtime privado, mas a etapa
         * heuristica continua sendo Community e nao consome pedidos
         * transacionais abertos.
         */
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning transactional orders requires OpsFactor Enterprise.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldAllowTransactionalOrdersForHeuristicWhenOpenOrdersExtensionIsAvailable()
            throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);
        perfilExecucaoSupplyPlan.setConsideraOrdensSelloutBacklog(true);
        perfilExecucaoSupplyPlan.setConsideraOrdensSellinFuturas(true);
        perfilExecucaoSupplyPlan.setConsideraOrdensTransferenciaBacklog(true);
        perfilExecucaoSupplyPlan.setConsideraOrdensCompraFuturas(true);
        setField(
                supplyPlanService,
                "supplyPlanOpenOrdersHeuristicSpi",
                getSupplyPlanOpenOrdersHeuristicSpiTeste());

        /*
         * A simples presença do optimizer não libera o heurístico. A liberação
         * depende do contrato que realmente materializa os quatro grupos de
         * ordens na projection comum antes da alocação.
         */
        Assertions.assertDoesNotThrow(
                () -> invokeValidaPedidosTransacionaisCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldRejectTransactionalOrdersForOptimizerWithoutEnterpriseBean() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        perfilExecucaoSupplyPlan.setConsideraOrdensCompraFuturas(true);

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaPedidosTransacionaisCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

        /*
         * No Community puro nao existe optimizer capaz de materializar
         * PedidosAbertosProjection. O payload deve continuar falhando na borda
         * funcional, mesmo que o enum do modo consiga ser desserializado.
         */
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning transactional orders requires OpsFactor Enterprise.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldAllowTransactionalOrdersForOptimizerWhenEnterpriseBeanIsAvailable() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        perfilExecucaoSupplyPlan.setConsideraOrdensSelloutBacklog(true);
        perfilExecucaoSupplyPlan.setConsideraOrdensSellinFuturas(true);
        perfilExecucaoSupplyPlan.setConsideraOrdensTransferenciaBacklog(true);
        perfilExecucaoSupplyPlan.setConsideraOrdensCompraFuturas(true);
        setField(
                supplyPlanService,
                "supplyPlanOptimizationService",
                new StubSupplyPlanOptimizationService());

        /*
         * O optimizer Enterprise e a unica implementacao que reabre pedidos
         * transacionais abertos neste recorte. O teste exercita os quatro
         * grupos consumidos por PedidosAbertosProjection sem montar factories
         * pesadas do fluxo completo.
         */
        Assertions.assertDoesNotThrow(
                () -> invokeValidaPedidosTransacionaisCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldRejectBacklogCarryOverForHeuristicEvenWithEnterpriseOptimizerBean() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);
        perfilExecucaoSupplyPlan.setAllowBacklogCarryOver(true);
        setField(
                supplyPlanService,
                "supplyPlanOptimizationService",
                new StubSupplyPlanOptimizationService());

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaPedidosTransacionaisCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning backlog carry-over requires OpsFactor Enterprise.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldAllowBacklogCarryOverForOptimizerWhenEnterpriseBeanIsAvailable() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        perfilExecucaoSupplyPlan.setAllowBacklogCarryOver(true);
        setField(
                supplyPlanService,
                "supplyPlanOptimizationService",
                new StubSupplyPlanOptimizationService());

        Assertions.assertDoesNotThrow(
                () -> invokeValidaPedidosTransacionaisCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldRejectForceMakeToOrderForHeuristicEvenWithEnterpriseOptimizerBean() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);
        perfilExecucaoSupplyPlan.setForceMakeToOrderModel(true);
        setField(
                supplyPlanService,
                "supplyPlanOptimizationService",
                new StubSupplyPlanOptimizationService());

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaPedidosTransacionaisCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning fully make-to-order requires OpsFactor Enterprise.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaPedidosTransacionaisCommunityShouldAllowForceMakeToOrderForOptimizerWhenEnterpriseBeanIsAvailable() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        perfilExecucaoSupplyPlan.setForceMakeToOrderModel(true);
        setField(
                supplyPlanService,
                "supplyPlanOptimizationService",
                new StubSupplyPlanOptimizationService());

        Assertions.assertDoesNotThrow(
                () -> invokeValidaPedidosTransacionaisCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

    }

    @Test
    public void validaOtimizadorInteligenciaArtificialCommunityShouldRejectAiOptimizerForHeuristicEvenWithEnterpriseOptimizerBean() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);
        perfilExecucaoSupplyPlan.setOtimizadorInteligenciaArtificial(
                PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial.SNP);
        setField(
                supplyPlanService,
                "supplyPlanOptimizationService",
                new StubSupplyPlanOptimizationService());

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaOtimizadorInteligenciaArtificialCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

        /*
         * O bean Enterprise pode existir no runtime privado, mas AI optimizer
         * nao altera o contrato do motor heuristico Community.
         */
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: AI optimizer requires OpsFactor Enterprise.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaOtimizadorInteligenciaArtificialCommunityShouldRejectAiOptimizerForOptimizerWithoutEnterpriseBean() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        perfilExecucaoSupplyPlan.setOtimizadorInteligenciaArtificial(
                PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial.GREENFIELD);

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaOtimizadorInteligenciaArtificialCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

        /*
         * No Community puro nao existe overlay capaz de interpretar os modos de
         * AI optimizer. O payload deve falhar antes de montar projections.
         */
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: AI optimizer requires OpsFactor Enterprise.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaOtimizadorInteligenciaArtificialCommunityShouldAllowAiOptimizerForOptimizerWhenEnterpriseBeanIsAvailable() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        perfilExecucaoSupplyPlan.setOtimizadorInteligenciaArtificial(
                PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial.DETALHADO);
        setField(
                supplyPlanService,
                "supplyPlanOptimizationService",
                new StubSupplyPlanOptimizationService());

        /*
         * O optimizer Enterprise e o unico runtime que conhece os modos de AI
         * optimizer. O Community service apenas deixa o parametro atravessar
         * quando a SPI real esta disponivel.
         */
        Assertions.assertDoesNotThrow(
                () -> invokeValidaOtimizadorInteligenciaArtificialCommunity(
                        supplyPlanService,
                        perfilExecucaoSupplyPlan));

    }

    @Test
    public void executeSupplyPlanInternalShouldRejectOptimizerBeforeProjectionFactories() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        supplyPlan.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan);

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        supplyPlan,
                        null,
                        perfilExecucaoSupplyPlan,
                        false,
                        false,
                        false));

        /*
         * O metodo interno e usado pela process chain Enterprise. No runtime
         * Community puro ele precisa falhar na borda de motor, antes de tocar
         * factories de projections que nao foram injetadas neste teste e que
         * representariam custo desnecessario em producao.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning Optimizer requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void executeSupplyPlanInternalShouldRejectAiOptimizerBeforeProjectionFactories() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);
        perfilExecucaoSupplyPlan.setOtimizadorInteligenciaArtificial(
                PerfilExecucaoSupplyPlan.OtimizadorInteligenciaArtificial.SNP);
        supplyPlan.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan);

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        supplyPlan,
                        null,
                        perfilExecucaoSupplyPlan,
                        false,
                        false,
                        false));

        /*
         * AI optimizer e opcao Enterprise. Mesmo quando o motor atomico e
         * HEURISTICO, o Community deve bloquear antes de montar projections de
         * UOM, cluster ou malha, porque o heuristico nao consome esse parametro.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: AI optimizer requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void executeSupplyPlanInternalShouldRejectTransactionalOrdersBeforeProjectionFactories() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO);
        perfilExecucaoSupplyPlan.setConsideraOrdensSellinFuturas(true);
        supplyPlan.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan);

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        supplyPlan,
                        null,
                        perfilExecucaoSupplyPlan,
                        false,
                        false,
                        false));

        /*
         * Pedidos transacionais sao materializados somente pelo optimizer
         * Enterprise. Perfil heuristico Community deve falhar antes de qualquer
         * projection compartilhada ou repository pesado.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning transactional orders requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void executeSupplyPlanInternalShouldDelegateProcessChainBeforeProjectionFactories() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        StubSupplyPlanProcessChainService supplyPlanProcessChainService =
                new StubSupplyPlanProcessChainService();
        SupplyPlan supplyPlan = new SupplyPlan();
        SupplyPlan supplyPlanParaProjecaoEstoqueInicial = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN);
        setField(
                supplyPlanService,
                "supplyPlanProcessChainService",
                supplyPlanProcessChainService);

        supplyPlanService.executeSupplyPlan(
                supplyPlan,
                supplyPlanParaProjecaoEstoqueInicial,
                perfilExecucaoSupplyPlan,
                true,
                true,
                false);

        /*
         * PROCESS_CHAIN e um orquestrador Enterprise. O metodo interno nao deve
         * montar projections Community antes de expandir a cadeia; cada etapa
         * chamara o mesmo metodo com HEURISTICO ou OTIMIZADOR. O teste deixa
         * todas as factories nulas de proposito: qualquer regressao para o
         * caminho pesado volta a falhar por NullPointerException antes da SPI.
         */
        Assertions.assertSame(
                supplyPlan,
                supplyPlanProcessChainService.supplyPlanRecebido);
        Assertions.assertSame(
                supplyPlanParaProjecaoEstoqueInicial,
                supplyPlanProcessChainService.supplyPlanParaProjecaoEstoqueInicialRecebido);
        Assertions.assertSame(
                perfilExecucaoSupplyPlan,
                supplyPlanProcessChainService.perfilExecucaoSupplyPlanRecebido);
        Assertions.assertTrue(
                supplyPlanProcessChainService.novoSupplyPlanRecebido);
        Assertions.assertTrue(
                supplyPlanProcessChainService.consideraRequisicoesEtapaAnteriorRecebido);
        Assertions.assertFalse(
                supplyPlanProcessChainService.consideraOrdensProducaoPlanejadasEtapaAnteriorRecebido);

    }

    @Test
    public void executeNewSupplyPlanShouldPersistHeaderBeforeDelegatingToProcessChain() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        StubSupplyPlanProcessChainService supplyPlanProcessChainService =
                new StubSupplyPlanProcessChainService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setId("PERFIL_PROCESS_CHAIN");
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN);
        perfilExecucaoSupplyPlan.setHorizontePlanoDias(7);
        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setId(42L);
        VersaoMalha versaoMalha = new VersaoMalha();
        versaoMalha.setId("MALHA_PADRAO");
        ClusterEParametrosProjection clusterEParametrosProjection = new ClusterEParametrosProjection();
        setField(
                clusterEParametrosProjection,
                "parametrosGlobais",
                new ParametrosGlobais());
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryParaPlanoNovoProcessChain());
        setField(
                supplyPlanService,
                "perfilExecucaoSupplyPlanRepository",
                getPerfilExecucaoSupplyPlanRepositoryComPerfil(perfilExecucaoSupplyPlan));
        setField(
                supplyPlanService,
                "demandPlanRepository",
                getDemandPlanRepositoryComDemandPlan(demandPlan));
        setField(
                supplyPlanService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryComVersaoMalha(versaoMalha));
        setField(
                supplyPlanService,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory(clusterEParametrosProjection));
        setField(
                supplyPlanService,
                "supplyPlanProcessChainService",
                supplyPlanProcessChainService);

        supplyPlanService.executeSupplyPlan(
                demandPlan.getId(),
                null,
                null,
                perfilExecucaoSupplyPlan.getId(),
                versaoMalha.getId(),
                null,
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "Process chain Enterprise",
                "System");

        /*
         * O process chain Enterprise executa antes das projections pesadas,
         * mas ele orquestra varias etapas sobre o mesmo Supply Plan fisico.
         * Assim, plano novo precisa chegar a SPI ja com id; o flag
         * `novoSupplyPlan` continua verdadeiro para que a primeira etapa
         * atomica prepare estoque inicial, demanda direta e snapshots base.
         */
        Assertions.assertNotNull(
                supplyPlanProcessChainService.supplyPlanRecebido);
        Assertions.assertEquals(
                1000L,
                supplyPlanProcessChainService.supplyPlanRecebido.getId());
        Assertions.assertSame(
                demandPlan,
                supplyPlanProcessChainService.supplyPlanRecebido.getDemandPlan());
        Assertions.assertSame(
                versaoMalha,
                supplyPlanProcessChainService.supplyPlanRecebido.getVersaoMalha());
        Assertions.assertSame(
                perfilExecucaoSupplyPlan,
                supplyPlanProcessChainService.perfilExecucaoSupplyPlanRecebido);
        Assertions.assertTrue(
                supplyPlanProcessChainService.novoSupplyPlanRecebido);
        Assertions.assertNull(
                supplyPlanProcessChainService.supplyPlanParaProjecaoEstoqueInicialRecebido);

    }

    @Test
    public void executeNewSupplyPlanShouldRejectEnterpriseEngineBeforeSharedProjectionFactories() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryVazio());
        setField(
                supplyPlanService,
                "perfilExecucaoSupplyPlanRepository",
                getPerfilExecucaoSupplyPlanRepositoryComPerfil(perfilExecucaoSupplyPlan));

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        42L,
                        null,
                        null,
                        "PERFIL_OTIMIZADOR",
                        "MALHA_PADRAO",
                        null,
                        Constantes.TamanhoBucket.MENSAL,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        "Plano otimizado Community bloqueado",
                        "System"));

        /*
         * O teste injeta apenas os repositories indispensaveis para descobrir
         * que nao ha Supply Plan existente e que o perfil novo solicita
         * otimizador. Se a implementacao voltar a montar DemandPlan, malha ou
         * ClusterEParametrosProjection antes da validacao de edicao, a chamada
         * passa a quebrar por NullPointerException ou proxy nao esperado.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning Optimizer requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void executeNewSupplyPlanShouldRejectEnterpriseEngineBeforeInitialStockProjectionLookup() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComUmaConsultaCustomFindById(Optional.empty()));
        setField(
                supplyPlanService,
                "perfilExecucaoSupplyPlanRepository",
                getPerfilExecucaoSupplyPlanRepositoryComPerfil(perfilExecucaoSupplyPlan));

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        42L,
                        null,
                        999L,
                        "PERFIL_OTIMIZADOR",
                        "MALHA_PADRAO",
                        null,
                        Constantes.TamanhoBucket.MENSAL,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        "Plano otimizado Community bloqueado",
                        "System"));

        /*
         * O id de projection de estoque inicial nao pode ser resolvido antes da
         * validacao do motor. Em Community puro, o erro principal precisa ser
         * a ausencia do overlay Enterprise, nao a existencia ou nao do snapshot
         * auxiliar informado no payload.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning Optimizer requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void executeNewSupplyPlanShouldRejectProcessChainBeforeInitialStockProjectionLookup() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN);
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComUmaConsultaCustomFindById(Optional.empty()));
        setField(
                supplyPlanService,
                "perfilExecucaoSupplyPlanRepository",
                getPerfilExecucaoSupplyPlanRepositoryComPerfil(perfilExecucaoSupplyPlan));

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        42L,
                        null,
                        999L,
                        "PERFIL_PROCESS_CHAIN",
                        "MALHA_PADRAO",
                        null,
                        Constantes.TamanhoBucket.MENSAL,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        "Process chain Community bloqueada",
                        "System"));

        /*
         * Process chain tambem e capability Enterprise. Em plano novo, o
         * Community precisa rejeitar o motor antes de tentar carregar o Supply
         * Plan auxiliar para estoque inicial ou qualquer projection pesada.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning Process Chain requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void executeSupplyPlanShouldRejectNullExistingSupplyPlanOptionalBeforeExecutionBranch() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComUmaConsultaCustomFindById(null));

        /*
         * Id nulo significa plano novo e vira sentinela -1. O repository deve
         * devolver `Optional.empty()` para esse caso. Se devolver `null`, a
         * falha precisa acontecer antes de perfil, Demand Plan, malha ou
         * qualquer projection, deixando claro que a fronteira de persistencia
         * quebrou.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        42L,
                        null,
                        null,
                        "PERFIL_HEURISTICO",
                        "MALHA_PADRAO",
                        null,
                        Constantes.TamanhoBucket.MENSAL,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        "Plano Community",
                        "System"));

        Assertions.assertEquals(
                "Supply Plan repository returned null Optional while checking existing Supply Plan id -1.",
                illegalStateException.getMessage());

    }

    @Test
    public void validaSupplyPlanSalvoInicialCommunityShouldRejectBrokenSavedSnapshots() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlanValido = criaSupplyPlanSalvoInicialParaTeste();

        Assertions.assertSame(
                supplyPlanValido,
                invokeValidaSupplyPlanSalvoInicialCommunity(
                        supplyPlanService,
                        supplyPlanValido));

        assertValidaSupplyPlanSalvoInicialCommunityMessage(
                supplyPlanService,
                null,
                "Saved Supply Plan snapshot is required before Community Supply Planning execution.");

        SupplyPlan supplyPlanSemId = criaSupplyPlanSalvoInicialParaTeste();
        supplyPlanSemId.setId(null);
        assertValidaSupplyPlanSalvoInicialCommunityMessage(
                supplyPlanService,
                supplyPlanSemId,
                "Saved Supply Plan snapshot has no id before Community Supply Planning execution.");

        SupplyPlan supplyPlanSemDemandPlan = criaSupplyPlanSalvoInicialParaTeste();
        supplyPlanSemDemandPlan.setDemandPlan(null);
        assertValidaSupplyPlanSalvoInicialCommunityMessage(
                supplyPlanService,
                supplyPlanSemDemandPlan,
                "Saved Supply Plan snapshot has no Demand Plan before Community Supply Planning execution.");

        SupplyPlan supplyPlanSemVersaoMalha = criaSupplyPlanSalvoInicialParaTeste();
        supplyPlanSemVersaoMalha.setVersaoMalha(null);
        assertValidaSupplyPlanSalvoInicialCommunityMessage(
                supplyPlanService,
                supplyPlanSemVersaoMalha,
                "Saved Supply Plan snapshot has no Supply Network version before Community Supply Planning execution.");

        SupplyPlan supplyPlanSemPerfilExecucao = criaSupplyPlanSalvoInicialParaTeste();
        supplyPlanSemPerfilExecucao.setPerfilExecucaoSupplyPlan(null);
        assertValidaSupplyPlanSalvoInicialCommunityMessage(
                supplyPlanService,
                supplyPlanSemPerfilExecucao,
                "Saved Supply Plan snapshot has no execution profile before Community Supply Planning execution.");

    }

    @Test
    public void executeExistingSupplyPlanShouldRejectEnterpriseEngineBeforeRestartArtifacts() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlanExistente = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        supplyPlanExistente.setId(42L);
        supplyPlanExistente.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan);
        supplyPlanExistente.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComCustomFindById(supplyPlanExistente));

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        null,
                        42L,
                        null,
                        null,
                        null,
                        null,
                        Constantes.TamanhoBucket.MENSAL,
                        null,
                        "Reexecucao otimizada Community bloqueada",
                        "System"));

        /*
         * Planos existentes podem carregar historico de perfil Enterprise no
         * banco. O Community deve falhar no motor antes de tentar limpar
         * artefatos do heuristico, otimizar ou montar projections sem overlay.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning Optimizer requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void executeExistingSupplyPlanShouldRejectEnterpriseEngineBeforeInitialStockProjectionLookup() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlanExistente = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        supplyPlanExistente.setId(42L);
        supplyPlanExistente.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan);
        supplyPlanExistente.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComUmaConsultaCustomFindById(Optional.of(supplyPlanExistente)));

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        null,
                        42L,
                        999L,
                        null,
                        null,
                        null,
                        Constantes.TamanhoBucket.MENSAL,
                        null,
                        "Reexecucao otimizada Community bloqueada",
                        "System"));

        /*
         * Em reexecucao, a validacao de motor tambem precede snapshots
         * auxiliares. A limpeza de artefatos e a consulta do plano-base de
         * estoque inicial so podem acontecer depois que o modo for permitido.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning Optimizer requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void executeExistingSupplyPlanShouldRejectProcessChainBeforeInitialStockProjectionLookup() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlanExistente = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN);
        supplyPlanExistente.setId(42L);
        supplyPlanExistente.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan);
        supplyPlanExistente.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComUmaConsultaCustomFindById(Optional.of(supplyPlanExistente)));

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.executeSupplyPlan(
                        null,
                        42L,
                        999L,
                        null,
                        null,
                        null,
                        Constantes.TamanhoBucket.MENSAL,
                        null,
                        "Reexecucao process chain Community bloqueada",
                        "System"));

        /*
         * Em reexecucao, a ordem e a mesma do optimizer: se o plano salvo aponta
         * para process chain e nao existe overlay Enterprise, a falha e de
         * edicao antes da consulta do plano-base de estoque inicial.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning Process Chain requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void getSupplyPlanEnterpriseExecutionServiceShouldRejectOptimizerWithoutEnterpriseBean() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();

        assertRequiresEnterpriseVersionException(
                supplyPlanService,
                PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);

    }

    @Test
    public void getSupplyPlanEnterpriseExecutionServiceShouldRejectProcessChainWithoutEnterpriseBean() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();

        assertRequiresEnterpriseVersionException(
                supplyPlanService,
                PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN);

    }

    @Test
    public void getSupplyPlanEnterpriseExecutionServiceShouldReturnOptimizerBeanWhenAvailable() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlanOptimizationServiceSpi supplyPlanOptimizationService = new StubSupplyPlanOptimizationService();
        setField(
                supplyPlanService,
                "supplyPlanOptimizationService",
                supplyPlanOptimizationService);

        SupplyPlanExecutionServiceSpi supplyPlanExecutionService = invokeGetSupplyPlanEnterpriseExecutionService(
                supplyPlanService,
                PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);

        Assertions.assertSame(
                supplyPlanOptimizationService,
                supplyPlanExecutionService);

    }

    @Test
    public void getSupplyPlanEnterpriseExecutionServiceShouldReturnProcessChainBeanWhenAvailable() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlanProcessChainServiceSpi supplyPlanProcessChainService = new StubSupplyPlanProcessChainService();
        setField(
                supplyPlanService,
                "supplyPlanProcessChainService",
                supplyPlanProcessChainService);

        SupplyPlanExecutionServiceSpi supplyPlanExecutionService = invokeGetSupplyPlanEnterpriseExecutionService(
                supplyPlanService,
                PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN);

        Assertions.assertSame(
                supplyPlanProcessChainService,
                supplyPlanExecutionService);

    }

    @Test
    public void saveProductionPlanLinhaCollectionShouldNeutralizeFirmOrdersCommunity() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        ProductionPlanLinha productionPlanLinha = new ProductionPlanLinha();
        productionPlanLinha.setQuantidadeOrdemFirmeProducaoIrrestrita(12.0);
        productionPlanLinha.setQuantidadeOrdemFirmeProducaoRestrita(8.0);
        productionPlanLinha.setQuantidadeOrdemFirmeProducaoTrabalho(5.0);

        supplyPlanService.saveProductionPlanLinhaCollection(
                List.of(productionPlanLinha),
                false);

        /*
         * A linha tinha apenas ordens firmes Enterprise. O Community deve
         * zerar esses campos antes do filtro; como nao sobra quantidade
         * planejada, o metodo retorna sem acionar repository/saveAll.
         */
        Assertions.assertEquals(
                0.0,
                productionPlanLinha.getQuantidadeOrdemFirmeProducaoIrrestrita(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                productionPlanLinha.getQuantidadeOrdemFirmeProducaoRestrita(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                productionPlanLinha.getQuantidadeOrdemFirmeProducaoTrabalho(),
                0.000001);

    }

    @Test
    public void enterpriseFirmProductionCapabilityShouldPreserveFirmOrdersDuringSaveAndZeroCleanup()
            throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        List<ProductionPlanLinha> productionPlanLinhasSalvas = new ArrayList<>();
        ProductionPlanLinha productionPlanLinha =
                criaProductionPlanLinhaParaTeste("MAT-FIRM-ENTERPRISE");
        productionPlanLinha.setQuantidadeOrdemFirmeProducaoIrrestrita(12.0);
        productionPlanLinha.setQuantidadeOrdemFirmeProducaoRestrita(8.0);
        productionPlanLinha.setQuantidadeOrdemFirmeProducaoTrabalho(5.0);
        setField(
                supplyPlanService,
                "supplyPlanFirmProductionOrdersSpi",
                getSupplyPlanFirmProductionOrdersSpiTeste());
        setField(
                supplyPlanService,
                "versaoProducaoService",
                getVersaoProducaoServiceTeste());
        setField(
                supplyPlanService,
                "productionPlanLinhaRepository",
                getProductionPlanLinhaRepositoryCapturandoSaveAll(productionPlanLinhasSalvas));

        supplyPlanService.saveProductionPlanLinhaCollection(
                List.of(productionPlanLinha),
                false);
        supplyPlanService.removeProductionPlanLinhaZeradosCollection(
                List.of(productionPlanLinha));

        /*
         * O SPI Enterprise e o contrato que inseriu a linha firme na
         * projection. Ela precisa sobreviver tanto ao save inicial quanto a
         * uma limpeza entre checkpoints, para que o nivelamento reserve sua
         * capacidade e o plano restrito a reencontre no banco.
         */
        Assertions.assertEquals(
                12.0,
                productionPlanLinha.getQuantidadeOrdemFirmeProducaoIrrestrita(),
                0.000001);
        Assertions.assertEquals(
                8.0,
                productionPlanLinha.getQuantidadeOrdemFirmeProducaoRestrita(),
                0.000001);
        Assertions.assertEquals(
                5.0,
                productionPlanLinha.getQuantidadeOrdemFirmeProducaoTrabalho(),
                0.000001);
        Assertions.assertEquals(List.of(productionPlanLinha), productionPlanLinhasSalvas);

    }

    @Test
    public void saveProductionPlanLinhaCollectionShouldRejectBrokenCollectionsCommunity() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        List<ProductionPlanLinha> productionPlanLinhaCollectionComItemNulo = new ArrayList<>();
        productionPlanLinhaCollectionComItemNulo.add(new ProductionPlanLinha());
        productionPlanLinhaCollectionComItemNulo.add(null);

        IllegalArgumentException colecaoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveProductionPlanLinhaCollection(
                        null,
                        false));
        IllegalArgumentException itemAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveProductionPlanLinhaCollection(
                        productionPlanLinhaCollectionComItemNulo,
                        false));

        /*
         * Snapshot quebrado de linhas de producao deve falhar antes da
         * neutralizacao de ordens firmes e antes do filtro de linhas nao-zero.
         */
        Assertions.assertEquals(
                "Production Plan line collection is required for Community production planning persistence.",
                colecaoAusenteException.getMessage());
        Assertions.assertEquals(
                "Production Plan line at index 1 is required for Community production planning persistence.",
                itemAusenteException.getMessage());

    }

    @Test
    public void saveProductionPlanLinhaCollectionShouldPersistRestrictedAndWorkQuantitiesCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        List<ProductionPlanLinha> productionPlanLinhasSalvas = new ArrayList<>();
        ProductionPlanLinha productionPlanLinhaRestrita =
                criaProductionPlanLinhaParaTeste("MAT-RESTRICTED");
        ProductionPlanLinha productionPlanLinhaTrabalho =
                criaProductionPlanLinhaParaTeste("MAT-WORK");
        productionPlanLinhaRestrita.setQuantidadeOrdemPlanejadaProducaoRestrita(9.0);
        productionPlanLinhaTrabalho.setQuantidadeOrdemPlanejadaProducaoTrabalho(7.0);
        setField(
                supplyPlanService,
                "versaoProducaoService",
                getVersaoProducaoServiceTeste());
        setField(
                supplyPlanService,
                "productionPlanLinhaRepository",
                getProductionPlanLinhaRepositoryCapturandoSaveAll(productionPlanLinhasSalvas));

        supplyPlanService.saveProductionPlanLinhaCollection(
                List.of(
                        productionPlanLinhaRestrita,
                        productionPlanLinhaTrabalho),
                false);

        /*
         * Production Plan possui tres superficies fisicas: irrestrita, restrita
         * e trabalho. A decisao de persistir precisa considerar todas elas; se
         * olhasse apenas a quantidade irrestrita, uma rodada restrita ou ajuste
         * manual sem volume irrestrito seria descartado antes do batch save.
         */
        Assertions.assertEquals(
                2,
                productionPlanLinhasSalvas.size());
        Assertions.assertTrue(
                productionPlanLinhasSalvas.contains(productionPlanLinhaRestrita));
        Assertions.assertTrue(
                productionPlanLinhasSalvas.contains(productionPlanLinhaTrabalho));

    }

    @Test
    public void saveSupplyPlanLinhaCollectionsShouldRejectNullRepositorySnapshotsCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        ProductionPlanLinha productionPlanLinha =
                criaProductionPlanLinhaParaTeste("MAT-PROD-SAVED-NULL");
        DistributionPlanItem distributionPlanItem =
                criaDistributionPlanItemParaTeste("MAT-DIST-SAVED-NULL");
        InventoryPlanLinha inventoryPlanLinha =
                criaInventoryPlanLinhaParaTeste("MAT-INV-SAVED-NULL");
        productionPlanLinha.setQuantidadeOrdemPlanejadaProducaoRestrita(1.0);
        distributionPlanItem.setQuantidadeOrdemPlanejadaRestrita(1.0);
        inventoryPlanLinha.setQuantidadeEstoqueProjetadoRestrito(1.0);

        setField(
                supplyPlanService,
                "versaoProducaoService",
                getVersaoProducaoServiceTeste());
        setField(
                supplyPlanService,
                "productionPlanLinhaRepository",
                getProductionPlanLinhaRepositoryRetornandoSaveAll(null));
        setField(
                supplyPlanService,
                "distributionPlanItemRepository",
                getDistributionPlanItemRepositoryRetornandoSaveAll(null));
        setField(
                supplyPlanService,
                "inventoryPlanLinhaRepository",
                getInventoryPlanLinhaRepositoryRetornandoSaveAll(null));

        /*
         * O repository e a ultima borda antes de assumirmos que o plano foi
         * materializado. Retorno nulo de saveAll deve falhar de forma explicita
         * para cada familia de linha, em vez de seguir como sucesso silencioso.
         */
        IllegalStateException productionPlanException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanService.saveProductionPlanLinhaCollection(
                        List.of(productionPlanLinha),
                        false));
        IllegalStateException distributionPlanException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanService.saveDistributionPlanItemCollection(
                        List.of(distributionPlanItem),
                        false));
        IllegalStateException inventoryPlanException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanService.saveInventoryPlanLinhaCollection(
                        List.of(inventoryPlanLinha),
                        false));

        Assertions.assertEquals(
                "Saved Production Plan line snapshot is required after Community production planning persistence.",
                productionPlanException.getMessage());
        Assertions.assertEquals(
                "Saved Distribution Plan line snapshot is required after Community distribution planning persistence.",
                distributionPlanException.getMessage());
        Assertions.assertEquals(
                "Saved Inventory Plan line snapshot is required after Community inventory planning persistence.",
                inventoryPlanException.getMessage());

    }

    @Test
    public void saveSupplyPlanLinhaCollectionsShouldRejectPartialRepositorySnapshotsCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        ProductionPlanLinha productionPlanLinhaUm =
                criaProductionPlanLinhaParaTeste("MAT-PROD-PARTIAL-1");
        ProductionPlanLinha productionPlanLinhaDois =
                criaProductionPlanLinhaParaTeste("MAT-PROD-PARTIAL-2");
        DistributionPlanItem distributionPlanItemUm =
                criaDistributionPlanItemParaTeste("MAT-DIST-PARTIAL-1");
        DistributionPlanItem distributionPlanItemDois =
                criaDistributionPlanItemParaTeste("MAT-DIST-PARTIAL-2");
        InventoryPlanLinha inventoryPlanLinhaUm =
                criaInventoryPlanLinhaParaTeste("MAT-INV-PARTIAL-1");
        InventoryPlanLinha inventoryPlanLinhaDois =
                criaInventoryPlanLinhaParaTeste("MAT-INV-PARTIAL-2");
        productionPlanLinhaUm.setQuantidadeOrdemPlanejadaProducaoRestrita(1.0);
        productionPlanLinhaDois.setQuantidadeOrdemPlanejadaProducaoRestrita(1.0);
        distributionPlanItemUm.setQuantidadeOrdemPlanejadaRestrita(1.0);
        distributionPlanItemDois.setQuantidadeOrdemPlanejadaRestrita(1.0);
        inventoryPlanLinhaUm.setQuantidadeEstoqueProjetadoRestrito(1.0);
        inventoryPlanLinhaDois.setQuantidadeEstoqueProjetadoRestrito(1.0);

        setField(
                supplyPlanService,
                "versaoProducaoService",
                getVersaoProducaoServiceTeste());
        setField(
                supplyPlanService,
                "productionPlanLinhaRepository",
                getProductionPlanLinhaRepositoryRetornandoSaveAll(List.of(productionPlanLinhaUm)));
        setField(
                supplyPlanService,
                "distributionPlanItemRepository",
                getDistributionPlanItemRepositoryRetornandoSaveAll(List.of(distributionPlanItemUm)));
        setField(
                supplyPlanService,
                "inventoryPlanLinhaRepository",
                getInventoryPlanLinhaRepositoryRetornandoSaveAll(List.of(inventoryPlanLinhaUm)));

        /*
         * O save heuristico e tratado como snapshot completo por familia de
         * linha. Se o repository devolver apenas parte do lote, o Community
         * deve falhar antes de assumir que Planning Book/plano restrito estao
         * materializados.
         */
        IllegalStateException productionPlanException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanService.saveProductionPlanLinhaCollection(
                        List.of(
                                productionPlanLinhaUm,
                                productionPlanLinhaDois),
                        false));
        IllegalStateException distributionPlanException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanService.saveDistributionPlanItemCollection(
                        List.of(
                                distributionPlanItemUm,
                                distributionPlanItemDois),
                        false));
        IllegalStateException inventoryPlanException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanService.saveInventoryPlanLinhaCollection(
                        List.of(
                                inventoryPlanLinhaUm,
                                inventoryPlanLinhaDois),
                        false));

        Assertions.assertEquals(
                "Saved Production Plan line snapshot size 1 differs from expected Community production planning persistence size 2.",
                productionPlanException.getMessage());
        Assertions.assertEquals(
                "Saved Distribution Plan line snapshot size 1 differs from expected Community distribution planning persistence size 2.",
                distributionPlanException.getMessage());
        Assertions.assertEquals(
                "Saved Inventory Plan line snapshot size 1 differs from expected Community inventory planning persistence size 2.",
                inventoryPlanException.getMessage());

    }

    @Test
    public void inventoryPlanLinhaPersistenceShouldRejectDuplicatedFilteredKeysBeforeRepositoryCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        InventoryPlanLinha inventoryPlanLinha =
                criaInventoryPlanLinhaParaTeste("MAT-INV-DUP-PERSIST");
        InventoryPlanLinha inventoryPlanLinhaTransito =
                criaInventoryPlanLinhaParaTeste("MAT-INV-DUP-TRANSIT");
        inventoryPlanLinha.setQuantidadeEstoqueProjetadoRestrito(1.0);
        inventoryPlanLinhaTransito.setQuantidadeEstoqueTransitoInbound(1.0);

        setField(
                supplyPlanService,
                "inventoryPlanLinhaRepository",
                getInventoryPlanLinhaRepositoryFalhandoEmMutacao());

        IllegalArgumentException saveException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveInventoryPlanLinhaCollection(
                        List.of(
                                inventoryPlanLinha,
                                inventoryPlanLinha),
                        false));
        IllegalArgumentException transitException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveEstoqueEmTransitoDeInventoryPlanLinhaCollection(
                        List.of(
                                inventoryPlanLinhaTransito,
                                inventoryPlanLinhaTransito)));

        Assertions.assertTrue(
                saveException.getMessage()
                        .contains("Inventory Plan line at index 1 has duplicated Community inventory planning key for persistence"));
        Assertions.assertTrue(
                transitException.getMessage()
                        .contains("Inventory Plan line at index 1 has duplicated Community inventory planning key for persistence"));

    }

    @Test
    public void supplyPlanLinhaPersistenceShouldRejectDuplicatedFilteredKeysBeforeRepositoryCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        ProductionPlanLinha productionPlanLinha =
                criaProductionPlanLinhaParaTeste("MAT-PROD-DUP-PERSIST");
        DistributionPlanItem distributionPlanItem =
                criaDistributionPlanItemParaTeste("MAT-DIST-DUP-PERSIST");
        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha =
                criaDemandaDiretaConsideradaLinhaParaTeste("MAT-DIRECT-DUP-PERSIST");
        productionPlanLinha.setQuantidadeOrdemPlanejadaProducaoRestrita(1.0);
        distributionPlanItem.setQuantidadeOrdemPlanejadaRestrita(1.0);
        demandaDiretaConsideradaLinha.setQuantidadePlanoDemandaOriginal(1.0);

        setField(
                supplyPlanService,
                "versaoProducaoService",
                getVersaoProducaoServiceTeste());
        setField(
                supplyPlanService,
                "productionPlanLinhaRepository",
                getProductionPlanLinhaRepositoryFalhandoEmMutacao());
        setField(
                supplyPlanService,
                "distributionPlanItemRepository",
                getDistributionPlanItemRepositoryFalhandoEmMutacao());
        setField(
                supplyPlanService,
                "demandaDiretaConsideradaLinhaDAO",
                new TestDemandaDiretaConsideradaLinhaDAO(true));

        IllegalArgumentException productionPlanException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveProductionPlanLinhaCollection(
                        List.of(
                                productionPlanLinha,
                                productionPlanLinha),
                        false));
        IllegalArgumentException distributionPlanException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveDistributionPlanItemCollection(
                        List.of(
                                distributionPlanItem,
                                distributionPlanItem),
                        false));
        IllegalArgumentException demandaDiretaException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveDemandaDiretaConsideradaLinhaCollection(
                        List.of(
                                demandaDiretaConsideradaLinha,
                                demandaDiretaConsideradaLinha),
                        false));

        Assertions.assertTrue(
                productionPlanException.getMessage()
                        .contains("Production Plan line at index 1 has duplicated Community production planning key for persistence"));
        Assertions.assertTrue(
                distributionPlanException.getMessage()
                        .contains("Distribution Plan line at index 1 has duplicated Community distribution planning key for persistence"));
        Assertions.assertTrue(
                demandaDiretaException.getMessage()
                        .contains("Direct demand considered line at index 1 has duplicated Community Supply Planning key"));

    }

    @Test
    public void savedSupplyPlanLinhaSnapshotsShouldRejectBrokenItemsAndKeysCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        List<ProductionPlanLinha> productionPlanLinhasComItemNulo = new ArrayList<>();
        List<DistributionPlanItem> distributionPlanItemsComItemNulo = new ArrayList<>();
        List<InventoryPlanLinha> inventoryPlanLinhasComItemNulo = new ArrayList<>();
        productionPlanLinhasComItemNulo.add(null);
        distributionPlanItemsComItemNulo.add(null);
        inventoryPlanLinhasComItemNulo.add(null);

        assertIllegalStateMessage(
                () -> invokeValidaProductionPlanLinhasSalvasCommunity(
                        supplyPlanService,
                        productionPlanLinhasComItemNulo),
                "Saved Production Plan line at index 0 is required after Community production planning persistence.");
        assertIllegalStateMessage(
                () -> invokeValidaDistributionPlanItemsSalvasCommunity(
                        supplyPlanService,
                        distributionPlanItemsComItemNulo),
                "Saved Distribution Plan line at index 0 is required after Community distribution planning persistence.");
        assertIllegalStateMessage(
                () -> invokeValidaInventoryPlanLinhasSalvasCommunity(
                        supplyPlanService,
                        inventoryPlanLinhasComItemNulo),
                "Saved Inventory Plan line at index 0 is required after Community inventory planning persistence.");

        /*
         * A chave incompleta simula mapper/repository devolvendo entidade
         * parcialmente materializada. Isso deve falhar antes de qualquer caller
         * usar a linha salva para Planning Book ou recalculo restrito.
         */
        assertIllegalStateMessage(
                () -> invokeValidaProductionPlanLinhasSalvasCommunity(
                        supplyPlanService,
                        List.of(new ProductionPlanLinha())),
                "Saved Production Plan line at index 0 has an incomplete Community production planning key.");
        assertIllegalStateMessage(
                () -> invokeValidaDistributionPlanItemsSalvasCommunity(
                        supplyPlanService,
                        List.of(new DistributionPlanItem())),
                "Saved Distribution Plan line at index 0 has an incomplete Community distribution planning key.");
        assertIllegalStateMessage(
                () -> invokeValidaInventoryPlanLinhasSalvasCommunity(
                        supplyPlanService,
                        List.of(new InventoryPlanLinha())),
                "Saved Inventory Plan line at index 0 has an incomplete Community inventory planning key.");

    }

    @Test
    public void removeProductionPlanLinhaZeradosCollectionShouldKeepRestrictedAndWorkQuantitiesCommunity() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        ProductionPlanLinha productionPlanLinhaRestrita =
                criaProductionPlanLinhaParaTeste("MAT-RESTRICTED");
        ProductionPlanLinha productionPlanLinhaTrabalho =
                criaProductionPlanLinhaParaTeste("MAT-WORK");
        productionPlanLinhaRestrita.setQuantidadeOrdemPlanejadaProducaoRestrita(9.0);
        productionPlanLinhaTrabalho.setQuantidadeOrdemPlanejadaProducaoTrabalho(7.0);

        /*
         * Sem repository injetado de proposito. Caso o metodo volte a enxergar
         * apenas quantidade irrestrita, tentara deletar estas linhas validas e
         * o teste falhara por NullPointerException antes de chegar ao banco.
         */
        Assertions.assertDoesNotThrow(
                () -> supplyPlanService.removeProductionPlanLinhaZeradosCollection(
                        List.of(
                                productionPlanLinhaRestrita,
                                productionPlanLinhaTrabalho)));

    }

    @Test
    public void removeSupplyPlanLinhaZeradosCollectionsShouldRejectIncompleteKeysBeforeRepositoryCommunity() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();

        IllegalArgumentException productionPlanException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.removeProductionPlanLinhaZeradosCollection(
                        List.of(new ProductionPlanLinha())));
        IllegalArgumentException distributionPlanException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.removeDistributionPlanItemZeradosCollection(
                        List.of(new DistributionPlanItem())));
        IllegalArgumentException inventoryPlanException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.removeInventoryPlanLinhaZeradosCollection(
                        List.of(new InventoryPlanLinha())));

        /*
         * Linhas zeradas sao candidatas reais a delete. A chave composta deve
         * ser validada antes do repository para evitar deleteAll/NPE sem
         * contexto ou tentativa de remover snapshot indefinido.
         */
        Assertions.assertEquals(
                "Production Plan line at index 0 has an incomplete Community production planning key for delete.",
                productionPlanException.getMessage());
        Assertions.assertEquals(
                "Distribution Plan line at index 0 has an incomplete Community distribution planning key for delete.",
                distributionPlanException.getMessage());
        Assertions.assertEquals(
                "Inventory Plan line at index 0 has an incomplete Community inventory planning key for delete.",
                inventoryPlanException.getMessage());

    }

    @Test
    public void removeInventoryPlanLinhaZeradosCollectionShouldRejectDuplicatedKeysBeforeRepositoryCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        InventoryPlanLinha inventoryPlanLinha =
                criaInventoryPlanLinhaParaTeste("MAT-INV-DUP-DELETE");

        setField(
                supplyPlanService,
                "inventoryPlanLinhaRepository",
                getInventoryPlanLinhaRepositoryFalhandoEmMutacao());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.removeInventoryPlanLinhaZeradosCollection(
                        List.of(
                                inventoryPlanLinha,
                                inventoryPlanLinha)));

        Assertions.assertTrue(
                illegalArgumentException.getMessage()
                        .contains("Inventory Plan line at index 1 has duplicated Community inventory planning key for delete"));

    }

    @Test
    public void removeSupplyPlanLinhaZeradosCollectionShouldRejectDuplicatedKeysBeforeRepositoryCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        ProductionPlanLinha productionPlanLinha =
                criaProductionPlanLinhaParaTeste("MAT-PROD-DUP-DELETE");
        DistributionPlanItem distributionPlanItem =
                criaDistributionPlanItemParaTeste("MAT-DIST-DUP-DELETE");

        setField(
                supplyPlanService,
                "productionPlanLinhaRepository",
                getProductionPlanLinhaRepositoryFalhandoEmMutacao());
        setField(
                supplyPlanService,
                "distributionPlanItemRepository",
                getDistributionPlanItemRepositoryFalhandoEmMutacao());

        IllegalArgumentException productionPlanException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.removeProductionPlanLinhaZeradosCollection(
                        List.of(
                                productionPlanLinha,
                                productionPlanLinha)));
        IllegalArgumentException distributionPlanException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.removeDistributionPlanItemZeradosCollection(
                        List.of(
                                distributionPlanItem,
                                distributionPlanItem)));

        Assertions.assertTrue(
                productionPlanException.getMessage()
                        .contains("Production Plan line at index 1 has duplicated Community production planning key for delete"));
        Assertions.assertTrue(
                distributionPlanException.getMessage()
                        .contains("Distribution Plan line at index 1 has duplicated Community distribution planning key for delete"));

    }

    @Test
    public void removeDistributionPlanItemZeradosCollectionShouldRejectBrokenCollectionsCommunity() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        List<DistributionPlanItem> distributionPlanItemCollectionComItemNulo = new ArrayList<>();
        distributionPlanItemCollectionComItemNulo.add(new DistributionPlanItem());
        distributionPlanItemCollectionComItemNulo.add(null);

        IllegalArgumentException colecaoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.removeDistributionPlanItemZeradosCollection(null));
        IllegalArgumentException itemAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.removeDistributionPlanItemZeradosCollection(
                        distributionPlanItemCollectionComItemNulo));

        /*
         * A limpeza de linhas zeradas percorre a mesma borda de persistencia do
         * save. Item nulo nao deve chegar ao filtro, ao deleteAll ou a um NPE
         * sem contexto.
         */
        Assertions.assertEquals(
                "Distribution Plan line collection is required for Community distribution planning persistence.",
                colecaoAusenteException.getMessage());
        Assertions.assertEquals(
                "Distribution Plan line at index 1 is required for Community distribution planning persistence.",
                itemAusenteException.getMessage());

    }

    @Test
    public void saveDistributionPlanItemCollectionShouldNeutralizeFirmOrdersCommunity() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        DistributionPlanItem distributionPlanItem = new DistributionPlanItem();
        distributionPlanItem.setQuantidadeOrdemFirmeIrrestrita(12.0);
        distributionPlanItem.setQuantidadeOrdemFirmeRestrita(8.0);
        distributionPlanItem.setQuantidadeOrdemFirmeTrabalho(5.0);
        distributionPlanItem.setParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta(4.0);
        distributionPlanItem.setParcelaOrdemFirmeRestritaAtendimentoDemandaDireta(2.0);

        supplyPlanService.saveDistributionPlanItemCollection(
                List.of(distributionPlanItem),
                false);

        Assertions.assertEquals(
                0.0,
                distributionPlanItem.getQuantidadeOrdemFirmeIrrestrita(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                distributionPlanItem.getQuantidadeOrdemFirmeRestrita(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                distributionPlanItem.getQuantidadeOrdemFirmeTrabalho(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                distributionPlanItem.getParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                distributionPlanItem.getParcelaOrdemFirmeRestritaAtendimentoDemandaDireta(),
                0.000001);

    }

    @Test
    public void enterpriseOpenOrdersCapabilityShouldPreserveFirmDistributionOrdersDuringSave() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        DistributionPlanItem distributionPlanItem =
                criaDistributionPlanItemParaTeste("MAT-DIST-FIRM-ENTERPRISE");
        distributionPlanItem.setQuantidadeOrdemFirmeIrrestrita(12.0);
        setField(
                supplyPlanService,
                "supplyPlanOpenOrdersHeuristicSpi",
                getSupplyPlanOpenOrdersHeuristicSpiTeste());
        setField(
                supplyPlanService,
                "distributionPlanItemRepository",
                getDistributionPlanItemRepositoryRetornandoSaveAll(List.of(distributionPlanItem)));

        supplyPlanService.saveDistributionPlanItemCollection(
                List.of(distributionPlanItem),
                false);

        Assertions.assertEquals(
                12.0,
                distributionPlanItem.getQuantidadeOrdemFirmeIrrestrita(),
                0.000001);

    }

    @Test
    public void saveInventoryPlanLinhaCollectionShouldRejectBrokenCollectionsCommunity() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        List<InventoryPlanLinha> inventoryPlanLinhaCollectionComItemNulo = new ArrayList<>();
        inventoryPlanLinhaCollectionComItemNulo.add(new InventoryPlanLinha());
        inventoryPlanLinhaCollectionComItemNulo.add(null);

        IllegalArgumentException colecaoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveInventoryPlanLinhaCollection(
                        null,
                        false));
        IllegalArgumentException itemAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveEstoqueEmTransitoDeInventoryPlanLinhaCollection(
                        inventoryPlanLinhaCollectionComItemNulo));

        /*
         * Inventory Plan e usado pelo estoque operacional Community e por
         * campos transicionais de estoque em transito. Snapshot quebrado deve
         * falhar antes do stream e antes do repository.
         */
        Assertions.assertEquals(
                "Inventory Plan line collection is required for Community inventory planning persistence.",
                colecaoAusenteException.getMessage());
        Assertions.assertEquals(
                "Inventory Plan line at index 1 is required for Community inventory planning persistence.",
                itemAusenteException.getMessage());

    }

    @Test
    public void getQuantidadeDemandPlanCommunityNoBucketSupplyShouldIgnoreEnterpriseDemandComponents() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        TestDemandPlanningProjection demandPlanningProjection = new TestDemandPlanningProjection();
        SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu = null;
        Location location = new Location("LOCATION");
        Produto material = new Produto("MATERIAL");
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");

        double quantidadeDemandPlanCommunityNoBucketSupply = invokeGetQuantidadeDemandPlanCommunityNoBucketSupply(
                supplyPlanService,
                demandPlanningProjection,
                splitTemporalProjectionPorDfu,
                3,
                location,
                material,
                unidadeMedida);

        Assertions.assertEquals(
                12.0,
                quantidadeDemandPlanCommunityNoBucketSupply,
                0.000001);
        Assertions.assertEquals(
                List.of(Constantes.TipoDemanda.BASELINE, Constantes.TipoDemanda.AJUSTE_DEMANDA),
                demandPlanningProjection.tipoDemandasConsultados);

    }

    @Test
    public void saveDemandaDiretaConsideradaLinhaCollectionShouldNeutralizeWalletAndValuesCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        TestDemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new TestDemandaDiretaConsideradaLinhaDAO();
        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha =
                criaDemandaDiretaConsideradaLinhaParaTeste("MAT-DIRECT-DEMAND");
        demandaDiretaConsideradaLinha.setQuantidadePlanoDemandaOriginal(10.0);
        demandaDiretaConsideradaLinha.setQuantidadePlanoDemandaOriginalPropagadaLocationInterna(4.0);
        demandaDiretaConsideradaLinha.setQuantidadeCarteiraOriginal(3.0);
        demandaDiretaConsideradaLinha.setQuantidadeCarteiraOriginalPropagadaLocationInterna(2.0);
        demandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaPlanoDemandaIrrestrita(8.0);
        demandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaPlanoDemandaRestrita(6.0);
        demandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaCarteiraIrrestrita(7.0);
        demandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaCarteiraRestrita(5.0);
        demandaDiretaConsideradaLinha.setGrossSalesPlanoDemandaOriginal(100.0);
        demandaDiretaConsideradaLinha.setCustoImpostosDemandaDiretaPlanoDemandaRestrita(11.0);
        demandaDiretaConsideradaLinha.setCustoFreteDemandaDiretaCarteiraIrrestrita(9.0);
        setField(
                supplyPlanService,
                "demandaDiretaConsideradaLinhaDAO",
                demandaDiretaConsideradaLinhaDAO);

        supplyPlanService.saveDemandaDiretaConsideradaLinhaCollection(
                List.of(demandaDiretaConsideradaLinha),
                false);

        /*
         * Community persiste apenas quantidades vindas do Demand Plan e safety
         * stock. Carteira e valores economicos podem existir no schema, mas
         * devem ser zerados antes do batch aberto.
         */
        Assertions.assertEquals(
                List.of(demandaDiretaConsideradaLinha),
                demandaDiretaConsideradaLinhaDAO.demandaDiretaConsideradaLinhasSalvas);
        Assertions.assertEquals(
                10.0,
                demandaDiretaConsideradaLinha.getQuantidadePlanoDemandaOriginal(),
                0.000001);
        Assertions.assertEquals(
                8.0,
                demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaPlanoDemandaIrrestrita(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                demandaDiretaConsideradaLinha.getQuantidadeCarteiraOriginal(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaCarteiraIrrestrita(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                demandaDiretaConsideradaLinha.getGrossSalesPlanoDemandaOriginal(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                demandaDiretaConsideradaLinha.getCustoImpostosDemandaDiretaPlanoDemandaRestrita(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                demandaDiretaConsideradaLinha.getCustoFreteDemandaDiretaCarteiraIrrestrita(),
                0.000001);

    }

    @Test
    public void saveDemandaDiretaConsideradaLinhaCollectionShouldIgnoreWalletOnlyLinesCommunity() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        TestDemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new TestDemandaDiretaConsideradaLinhaDAO();
        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha =
                new DemandaDiretaConsideradaLinha();
        demandaDiretaConsideradaLinha.setQuantidadeCarteiraOriginal(3.0);
        demandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaCarteiraIrrestrita(7.0);
        demandaDiretaConsideradaLinha.setGrossSalesCarteiraOriginal(100.0);
        setField(
                supplyPlanService,
                "demandaDiretaConsideradaLinhaDAO",
                demandaDiretaConsideradaLinhaDAO);

        supplyPlanService.saveDemandaDiretaConsideradaLinhaCollection(
                List.of(demandaDiretaConsideradaLinha),
                false);

        /*
         * A linha tinha apenas componentes Enterprise. Depois da neutralizacao
         * nao sobra quantidade Community relevante e o DAO nao deve receber
         * batch algum.
         */
        Assertions.assertTrue(
                demandaDiretaConsideradaLinhaDAO.demandaDiretaConsideradaLinhasSalvas.isEmpty());
        Assertions.assertEquals(
                0.0,
                demandaDiretaConsideradaLinha.getQuantidadeCarteiraOriginal(),
                0.000001);
        Assertions.assertEquals(
                0.0,
                demandaDiretaConsideradaLinha.getGrossSalesCarteiraOriginal(),
                0.000001);

    }

    @Test
    public void saveDemandaDiretaConsideradaLinhaCollectionShouldRejectBrokenCollectionsCommunity() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        List<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhasComItemNulo =
                new ArrayList<>();
        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinhaSemChave =
                new DemandaDiretaConsideradaLinha();
        demandaDiretaConsideradaLinhasComItemNulo.add(new DemandaDiretaConsideradaLinha());
        demandaDiretaConsideradaLinhasComItemNulo.add(null);
        demandaDiretaConsideradaLinhaSemChave.setQuantidadePlanoDemandaOriginal(1.0);

        IllegalArgumentException colecaoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveDemandaDiretaConsideradaLinhaCollection(
                        null,
                        false));
        IllegalArgumentException itemAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveDemandaDiretaConsideradaLinhaCollection(
                        demandaDiretaConsideradaLinhasComItemNulo,
                        false));
        IllegalArgumentException chaveIncompletaException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.saveDemandaDiretaConsideradaLinhaCollection(
                        List.of(demandaDiretaConsideradaLinhaSemChave),
                        false));

        Assertions.assertEquals(
                "Direct demand considered line collection is required for Community Supply Planning persistence.",
                colecaoAusenteException.getMessage());
        Assertions.assertEquals(
                "Direct demand considered line at index 1 is required for Community Supply Planning persistence.",
                itemAusenteException.getMessage());
        Assertions.assertEquals(
                "Direct demand considered line at index 0 has an incomplete Community Supply Planning key.",
                chaveIncompletaException.getMessage());

    }

    @Test
    public void atualizaDemandaDiretaConsideradaShouldRejectLogisticCostCurvesBeforeProjectionUse() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        supplyPlan.setId(42L);
        perfilExecucaoSupplyPlan.setAplicaCurvasCustoFrete(true);

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanService.atualizaSupplyPlanComDemandaDiretaConsiderada(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null));

        /*
         * Curvas de custo logistico pertencem ao Enterprise e devem falhar na
         * borda de edicao antes de montar projections de Demand/Supply.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Logistics cost curves requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void atualizaDemandaDiretaConsideradaShouldRejectMissingDemandProjectionBeforeSupplySnapshots() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        supplyPlan.setId(42L);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.atualizaSupplyPlanComDemandaDiretaConsiderada(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        null,
                        null,
                        null,
                        null,
                        null));

        Assertions.assertEquals(
                "Demand Planning projection is required for Community direct demand considered update.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void atualizaDemandaDiretaConsideradaShouldRejectMissingTemporalSplitBeforeSupplySnapshots() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        supplyPlan.setId(42L);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.atualizaSupplyPlanComDemandaDiretaConsiderada(
                        supplyPlan,
                        perfilExecucaoSupplyPlan,
                        new TestDemandPlanningProjection(),
                        null,
                        null,
                        null,
                        null));

        Assertions.assertEquals(
                "Temporal split projection is required for Community direct demand considered update.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getDescricaoSupplyPlanOuDefaultShouldCompareEmptyDescriptionByValue() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        String descricaoVaziaNaoCanonica = new String(new char[0]);

        String descricaoDefaultParaStringVazia = invokeGetDescricaoSupplyPlanOuDefault(
                supplyPlanService,
                descricaoVaziaNaoCanonica,
                Constantes.TamanhoBucket.MENSAL);
        String descricaoDefaultParaStringEmBranco = invokeGetDescricaoSupplyPlanOuDefault(
                supplyPlanService,
                "   ",
                Constantes.TamanhoBucket.MENSAL);
        String descricaoInformada = invokeGetDescricaoSupplyPlanOuDefault(
                supplyPlanService,
                "Execucao Heuristica",
                Constantes.TamanhoBucket.MENSAL);

        /*
         * O payload REST pode construir uma instancia nova de String vazia. O
         * contrato correto e comparar conteudo, gerar descricao default para
         * ausencia real e preservar textos informados pelo usuario.
         */
        Assertions.assertTrue(descricaoDefaultParaStringVazia.startsWith("Supply Plan "));
        Assertions.assertTrue(descricaoDefaultParaStringEmBranco.startsWith("Supply Plan "));
        Assertions.assertEquals(
                "Execucao Heuristica",
                descricaoInformada);

    }

    @Test
    public void requiredSupplyPlanningInputsShouldFailExplicitlyWhenMissing() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        setField(
                supplyPlanService,
                "demandPlanRepository",
                getDemandPlanRepositoryVazio());
        setField(
                supplyPlanService,
                "versaoMalhaRepository",
                getVersaoMalhaRepositoryVazio());
        setField(
                supplyPlanService,
                "perfilExecucaoSupplyPlanRepository",
                getPerfilExecucaoSupplyPlanRepositoryVazio());
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryVazio());

        assertSupplyPlanExceptionMessage(
                () -> invokeGetDemandPlanObrigatorio(
                        supplyPlanService,
                        42L),
                "Demand Plan 42 not found for Supply Planning execution.");
        assertSupplyPlanExceptionMessage(
                () -> invokeGetVersaoMalhaObrigatoria(
                        supplyPlanService,
                        "MALHA_INEXISTENTE"),
                "Supply Network Version MALHA_INEXISTENTE not found for Supply Planning execution.");
        assertSupplyPlanExceptionMessage(
                () -> invokeGetPerfilExecucaoSupplyPlanObrigatorio(
                        supplyPlanService,
                        "PERFIL_INEXISTENTE"),
                "Supply Planning Execution Profile PERFIL_INEXISTENTE not found for Supply Planning execution.");
        assertSupplyPlanExceptionMessage(
                () -> invokeGetSupplyPlanParaProjecaoEstoqueInicialObrigatorio(
                        supplyPlanService,
                        88L),
                "Supply Plan 88 not found for initial stock projection.");

    }

    @Test
    public void requiredSupplyPlanningInputsShouldFailExplicitlyWhenRepositoryReturnsNullOptional() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        setField(
                supplyPlanService,
                "demandPlanRepository",
                getRepositoryProxyRetornandoOptionalNulo(
                        DemandPlanRepository.class,
                        "customFindByIdComPerfilExecucao"));
        setField(
                supplyPlanService,
                "versaoMalhaRepository",
                getRepositoryProxyRetornandoOptionalNulo(
                        VersaoMalhaRepository.class,
                        "findById"));
        setField(
                supplyPlanService,
                "perfilExecucaoSupplyPlanRepository",
                getRepositoryProxyRetornandoOptionalNulo(
                        PerfilExecucaoSupplyPlanRepository.class,
                        "customFindById"));
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getRepositoryProxyRetornandoOptionalNulo(
                        SupplyPlanRepository.class,
                        "customFindById"));

        /*
         * `Optional.empty()` continua representando cadastro nao encontrado,
         * que os helpers traduzem para SupplyPlanException funcional. `null`
         * no lugar do Optional e quebra do contrato de repository e deve
         * falhar antes de qualquer projection/calculo/limpeza de artefatos.
         */
        assertIllegalStateMessage(
                () -> invokeGetDemandPlanObrigatorio(
                        supplyPlanService,
                        42L),
                "Demand Plan repository returned null Optional for Supply Planning execution id 42.");
        assertIllegalStateMessage(
                () -> invokeGetVersaoMalhaObrigatoria(
                        supplyPlanService,
                        "MALHA_01"),
                "Supply Network Version repository returned null Optional for Supply Planning execution id MALHA_01.");
        assertIllegalStateMessage(
                () -> invokeGetPerfilExecucaoSupplyPlanObrigatorio(
                        supplyPlanService,
                        "PERFIL_01"),
                "Supply Planning Execution Profile repository returned null Optional for Supply Planning execution id PERFIL_01.");
        assertIllegalStateMessage(
                () -> invokeGetSupplyPlanParaProjecaoEstoqueInicialObrigatorio(
                        supplyPlanService,
                        88L),
                "Supply Plan repository returned null Optional for initial stock projection id 88.");

    }

    @Test
    public void getSupplyPlanDeIdShouldRejectNullRepositoryOptionalWithContractMessage() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        setField(
                supplyPlanService,
                "supplyPlanRepository",
                getRepositoryProxyRetornandoOptionalNulo(
                        SupplyPlanRepository.class,
                        "customFindById"));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> supplyPlanService.getSupplyPlanDeId(77L));

        Assertions.assertEquals(
                "Supply Plan repository returned null Optional while loading Supply Plan id 77.",
                illegalStateException.getMessage());

    }

    @Test
    public void getTamanhoBucketConsideradoParaProjecaoEstoqueInicialShouldRejectNullBucketWithContractMessage() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetTamanhoBucketConsideradoParaProjecaoEstoqueInicialAPartirPreEstoque(
                        supplyPlanService,
                        null));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertTrue(
                invocationTargetException.getCause().getMessage().contains(
                        "SupplyPlanService cannot derive an initial-stock projection bucket for Supply Plan bucket null"));

    }

    @Test
    public void reiniciaArtefatosSupplyPlanExistenteShouldResetOnlyMaterializedRecalculatedPlans() throws Exception {

        SupplyPlanService supplyPlanService = new SupplyPlanService();
        TestJdbcTemplate jdbcTemplate = new TestJdbcTemplate();
        SupplyPlan supplyPlan = new SupplyPlan();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();

        supplyPlan.setId(77L);
        perfilExecucaoSupplyPlan.setSalvaInventoryPlan(false);
        supplyPlan.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan);
        setField(
                supplyPlanService,
                "jdbcTemplate",
                jdbcTemplate);

        supplyPlanService.reiniciaArtefatosSupplyPlanExistente(supplyPlan);

        /*
         * Distribuicao e producao sempre existem no plano heuristico Community.
         * Inventory Plan so e materializado quando o perfil solicita salvar
         * inventario, portanto nao deve ser zerado para perfis sem esse output.
         */
        Assertions.assertEquals(
                List.of(77L, 77L),
                jdbcTemplate.supplyPlanIdsAtualizados);

    }

    @Test
    public void reiniciaArtefatosSupplyPlanExistenteShouldRejectMissingSupplyPlan() {

        SupplyPlanService supplyPlanService = new SupplyPlanService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanService.reiniciaArtefatosSupplyPlanExistente(null));

        Assertions.assertEquals(
                "Supply Plan is required.",
                illegalArgumentException.getMessage());

    }

    private static void assertAutowiredFields(
            boolean required,
            String... fieldNames) throws Exception {

        assertAutowiredFields(
                SupplyPlanService.class,
                required,
                fieldNames);

    }

    private static void assertAutowiredFields(
            Class<?> serviceClass,
            boolean required,
            String... fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = serviceClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    serviceClass.getSimpleName() + "." + fieldName + " deve usar @Autowired explicito");
            Assertions.assertEquals(
                    required,
                    autowired.required(),
                    serviceClass.getSimpleName() + "." + fieldName + " possui required inesperado");
        }

    }

    private static void assertRequiresEnterpriseVersionException(
            SupplyPlanService supplyPlanService,
            PerfilExecucaoSupplyPlan.ModoExecucao modoExecucao) {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetSupplyPlanEnterpriseExecutionService(
                        supplyPlanService,
                        modoExecucao));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    private static void assertValidaSupplyPlanSalvoInicialCommunityMessage(
            SupplyPlanService supplyPlanService,
            SupplyPlan supplyPlan,
            String mensagemEsperada) {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSupplyPlanSalvoInicialCommunity(
                        supplyPlanService,
                        supplyPlan));

        Assertions.assertInstanceOf(
                IllegalStateException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                mensagemEsperada,
                invocationTargetException.getCause().getMessage());

    }

    private static SupplyPlanExecutionServiceSpi invokeGetSupplyPlanEnterpriseExecutionService(
            SupplyPlanService supplyPlanService,
            PerfilExecucaoSupplyPlan.ModoExecucao modoExecucao) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getSupplyPlanEnterpriseExecutionService",
                PerfilExecucaoSupplyPlan.ModoExecucao.class);
        method.setAccessible(true);
        return (SupplyPlanExecutionServiceSpi) method.invoke(
                supplyPlanService,
                modoExecucao);

    }

    private static void invokeValidaModoExecucaoCommunity(
            SupplyPlanService supplyPlanService,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "validaModoExecucaoCommunity",
                PerfilExecucaoSupplyPlan.class);
        method.setAccessible(true);
        method.invoke(
                supplyPlanService,
                perfilExecucaoSupplyPlan);

    }

    private static SupplyPlan invokeValidaSupplyPlanSalvoInicialCommunity(
            SupplyPlanService supplyPlanService,
            SupplyPlan supplyPlanSalvo) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "validaSupplyPlanSalvoInicialCommunity",
                SupplyPlan.class);
        method.setAccessible(true);
        return (SupplyPlan) method.invoke(
                supplyPlanService,
                supplyPlanSalvo);

    }

    private static void invokeValidaPedidosTransacionaisCommunity(
            SupplyPlanService supplyPlanService,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "validaPedidosTransacionaisCommunity",
                PerfilExecucaoSupplyPlan.class);
        method.setAccessible(true);
        method.invoke(
                supplyPlanService,
                perfilExecucaoSupplyPlan);

    }

    private static void invokeValidaOtimizadorInteligenciaArtificialCommunity(
            SupplyPlanService supplyPlanService,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "validaOtimizadorInteligenciaArtificialCommunity",
                PerfilExecucaoSupplyPlan.class);
        method.setAccessible(true);
        method.invoke(
                supplyPlanService,
                perfilExecucaoSupplyPlan);

    }

    private static void invokeValidaRestricoesLogisticasCommunity(
            ConstrainedPlanService constrainedPlanService,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) throws Exception {

        Method method = ConstrainedPlanService.class.getDeclaredMethod(
                "validaRestricoesLogisticasCommunity",
                PerfilExecucaoSupplyPlan.class);
        method.setAccessible(true);
        method.invoke(
                constrainedPlanService,
                perfilExecucaoSupplyPlan);

    }

    private static ClusterEParametrosProjection invokeGetClusterEParametrosProjectionObrigatoriaConstrained(
            ConstrainedPlanService constrainedPlanService,
            ClusterEParametrosProjection clusterEParametrosProjection,
            String contexto) throws Exception {

        Method method = ConstrainedPlanService.class.getDeclaredMethod(
                "getClusterEParametrosProjectionObrigatoria",
                ClusterEParametrosProjection.class,
                String.class);
        method.setAccessible(true);
        return (ClusterEParametrosProjection) method.invoke(
                constrainedPlanService,
                clusterEParametrosProjection,
                contexto);

    }

    private static ParametrosGlobais invokeGetParametrosGlobaisObrigatoriosConstrained(
            ConstrainedPlanService constrainedPlanService,
            ClusterEParametrosProjection clusterEParametrosProjection,
            String contexto) throws Exception {

        Method method = ConstrainedPlanService.class.getDeclaredMethod(
                "getParametrosGlobaisObrigatorios",
                ClusterEParametrosProjection.class,
                String.class);
        method.setAccessible(true);
        return (ParametrosGlobais) method.invoke(
                constrainedPlanService,
                clusterEParametrosProjection,
                contexto);

    }

    private static PoliticaEstoquesProjection invokeGetPoliticaEstoquesProjectionObrigatoriaConstrained(
            ConstrainedPlanService constrainedPlanService,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            String contexto) throws Exception {

        Method method = ConstrainedPlanService.class.getDeclaredMethod(
                "getPoliticaEstoquesProjectionObrigatoria",
                PoliticaEstoquesProjection.class,
                String.class);
        method.setAccessible(true);
        return (PoliticaEstoquesProjection) method.invoke(
                constrainedPlanService,
                politicaEstoquesProjection,
                contexto);

    }

    private static MaterialProjection invokeGetMaterialProjectionObrigatoriaConstrained(
            ConstrainedPlanService constrainedPlanService,
            MaterialProjection materialProjection,
            String contexto) throws Exception {

        Method method = ConstrainedPlanService.class.getDeclaredMethod(
                "getMaterialProjectionObrigatoria",
                MaterialProjection.class,
                String.class);
        method.setAccessible(true);
        return (MaterialProjection) method.invoke(
                constrainedPlanService,
                materialProjection,
                contexto);

    }

    private static LocationProjection invokeGetLocationProjectionObrigatoriaConstrained(
            ConstrainedPlanService constrainedPlanService,
            LocationProjection locationProjection,
            String contexto) throws Exception {

        Method method = ConstrainedPlanService.class.getDeclaredMethod(
                "getLocationProjectionObrigatoria",
                LocationProjection.class,
                String.class);
        method.setAccessible(true);
        return (LocationProjection) method.invoke(
                constrainedPlanService,
                locationProjection,
                contexto);

    }

    private static SupplyNetworkProjection invokeGetSupplyNetworkProjectionObrigatoriaConstrained(
            ConstrainedPlanService constrainedPlanService,
            SupplyNetworkProjection supplyNetworkProjection,
            String contexto) throws Exception {

        Method method = ConstrainedPlanService.class.getDeclaredMethod(
                "getSupplyNetworkProjectionObrigatoria",
                SupplyNetworkProjection.class,
                String.class);
        method.setAccessible(true);
        return (SupplyNetworkProjection) method.invoke(
                constrainedPlanService,
                supplyNetworkProjection,
                contexto);

    }

    private static BIProjectionCapacidadeProdutiva invokeGetBIProjectionCapacidadeProdutivaObrigatoriaConstrained(
            ConstrainedPlanService constrainedPlanService,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            String contexto) throws Exception {

        Method method = ConstrainedPlanService.class.getDeclaredMethod(
                "getBIProjectionCapacidadeProdutivaObrigatoria",
                BIProjectionCapacidadeProdutiva.class,
                String.class);
        method.setAccessible(true);
        return (BIProjectionCapacidadeProdutiva) method.invoke(
                constrainedPlanService,
                biProjectionCapacidadeProdutiva,
                contexto);

    }

    private static Calendario criaCalendarioConstrainedPlanningTeste() {

        return Calendario.criaCalendarioDeDatas(
                Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0));

    }

    private static SplitTemporalProjectionPorDfu criaSplitTemporalProjectionPorDfuTeste() {

        Calendario calendario = criaCalendarioConstrainedPlanningTeste();

        /*
         * Estes testes exercitam apenas a validacao de entrada da ponte
         * Demand -> Supply. Usar o mesmo calendario como origem e destino
         * evita introduzir variacao temporal irrelevante para o contrato.
         */
        return new SplitTemporalProjectionPorDfu(
                calendario,
                calendario);

    }

    private static double invokeGetQuantidadeDemandPlanCommunityNoBucketSupply(
            SupplyPlanService supplyPlanService,
            DemandPlanningProjection demandPlanningProjection,
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu,
            int posicaoPeriodoCalendarioSupply,
            Location location,
            Produto material,
            UnidadeMedida unidadeMedida) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getQuantidadeDemandPlanCommunityNoBucketSupply",
                DemandPlanningProjection.class,
                SplitTemporalProjectionPorDfu.class,
                int.class,
                Location.class,
                Produto.class,
                UnidadeMedida.class);
        method.setAccessible(true);
        return (double) method.invoke(
                supplyPlanService,
                demandPlanningProjection,
                splitTemporalProjectionPorDfu,
                posicaoPeriodoCalendarioSupply,
                location,
                material,
                unidadeMedida);

    }

    private static ProductionPlanLinha criaProductionPlanLinhaParaTeste(String materialId) {

        SupplyPlan supplyPlan = new SupplyPlan();
        Location location = new Location("LOCATION-" + materialId);
        Produto material = new Produto(materialId);
        VersaoProducaoInexistente versaoProducaoInexistente = new VersaoProducaoInexistente();
        Roteiro roteiro = new Roteiro();
        ListaTecnica listaTecnica = new ListaTecnica();
        ProductionPlanLinha productionPlanLinha = new ProductionPlanLinha();
        ProductionPlanLinha.ProductionPlanLinhaCompositeKey productionPlanLinhaCompositeKey =
                new ProductionPlanLinha.ProductionPlanLinhaCompositeKey();

        roteiro.setId("ROUTING-" + materialId);
        roteiro.setLocation(location);
        roteiro.setMaterialOutput(material);
        listaTecnica.setId("BOM-" + materialId);
        listaTecnica.setLocation(location);
        listaTecnica.setMaterialOutput(material);

        productionPlanLinhaCompositeKey.setSupplyPlan(supplyPlan);
        productionPlanLinhaCompositeKey.setLocation(location);
        productionPlanLinhaCompositeKey.setVersaoProducao(versaoProducaoInexistente);
        productionPlanLinhaCompositeKey.setRoteiro(roteiro);
        productionPlanLinhaCompositeKey.setListaTecnica(listaTecnica);
        productionPlanLinhaCompositeKey.setDataReferencia(LocalDateTime.of(2026, 1, 1, 0, 0));

        productionPlanLinha.setProductionPlanLinhaCompositeKey(productionPlanLinhaCompositeKey);
        productionPlanLinha.setMaterialOutput(material);
        return productionPlanLinha;

    }

    private static DistributionPlanItem criaDistributionPlanItemParaTeste(String materialId) {

        SupplyPlan supplyPlan = new SupplyPlan();
        Location locationOrigem = new Location("ORIGIN-" + materialId);
        Location locationDestino = new Location("DESTINATION-" + materialId);
        Produto material = new Produto(materialId);
        DistributionPlanItem distributionPlanItem = new DistributionPlanItem();
        DistributionPlanItem.DistributionPlanItemKey key =
                new DistributionPlanItem.DistributionPlanItemKey();

        key.setSupplyPlan(supplyPlan);
        key.setLocationOrigem(locationOrigem);
        key.setLocationDestino(locationDestino);
        key.setProduto(material);
        key.setDataExpedicao(LocalDateTime.of(2026, 1, 1, 0, 0));
        key.setDataRecebimento(LocalDateTime.of(2026, 1, 2, 0, 0));

        distributionPlanItem.setKey(key);
        return distributionPlanItem;

    }

    private static InventoryPlanLinha criaInventoryPlanLinhaParaTeste(String materialId) {

        SupplyPlan supplyPlan = new SupplyPlan();
        Location location = new Location("LOCATION-" + materialId);
        Produto material = new Produto(materialId);
        InventoryPlanLinha inventoryPlanLinha = new InventoryPlanLinha();
        InventoryPlanLinha.InventoryPlanLinhaCompositeKey inventoryPlanLinhaCompositeKey =
                new InventoryPlanLinha.InventoryPlanLinhaCompositeKey();

        inventoryPlanLinhaCompositeKey.setSupplyPlan(supplyPlan);
        inventoryPlanLinhaCompositeKey.setLocation(location);
        inventoryPlanLinhaCompositeKey.setProduto(material);
        inventoryPlanLinhaCompositeKey.setDataReferencia(LocalDateTime.of(2026, 1, 1, 0, 0));

        inventoryPlanLinha.setInventoryPlanLinhaCompositeKey(inventoryPlanLinhaCompositeKey);
        return inventoryPlanLinha;

    }

    private static DemandaDiretaConsideradaLinha criaDemandaDiretaConsideradaLinhaParaTeste(String materialId) {

        SupplyPlan supplyPlan = new SupplyPlan();
        Location location = new Location("LOCATION-" + materialId);
        Produto material = new Produto(materialId);
        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha =
                new DemandaDiretaConsideradaLinha();
        DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey demandaDiretaConsideradaLinhaCompositeKey =
                new DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey();

        supplyPlan.setId(42L);
        demandaDiretaConsideradaLinhaCompositeKey.setSupplyPlan(supplyPlan);
        demandaDiretaConsideradaLinhaCompositeKey.setLocation(location);
        demandaDiretaConsideradaLinhaCompositeKey.setMaterial(material);
        demandaDiretaConsideradaLinhaCompositeKey.setDataReferencia(LocalDateTime.of(2026, 1, 1, 0, 0));

        demandaDiretaConsideradaLinha.setDemandaDiretaConsideradaLinhaCompositeKey(
                demandaDiretaConsideradaLinhaCompositeKey);
        return demandaDiretaConsideradaLinha;

    }

    private static SupplyPlan criaSupplyPlanSalvoInicialParaTeste() {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(42L);
        supplyPlan.setDemandPlan(new DemandPlan());
        supplyPlan.setVersaoMalha(new VersaoMalha());
        supplyPlan.setPerfilExecucaoSupplyPlan(new PerfilExecucaoSupplyPlan());
        return supplyPlan;

    }

    private static VersaoProducaoService getVersaoProducaoServiceTeste() {

        return new VersaoProducaoService() {

            @Override
            public VersaoProducaoInexistente getOuPersisteVersaoProducaoInexistente() {
                return new VersaoProducaoInexistente();
            }

        };

    }

    /**
     * Marca o runtime de teste como Enterprise sem introduzir tipo privado no
     * contrato do service Community. A implementacao real materializa ordens
     * firmes antes de os checkpoints compartilhados serem persistidos.
     */
    private static SupplyPlanFirmProductionOrdersSpi getSupplyPlanFirmProductionOrdersSpiTeste() {

        return supplyPlanningMultiplasLocationsProjection -> {
            /* O comportamento de materializacao e coberto no modulo Enterprise. */
        };

    }

    /** Marca o runtime de teste como Enterprise com leitura de ordens abertas. */
    private static SupplyPlanOpenOrdersHeuristicSpi getSupplyPlanOpenOrdersHeuristicSpiTeste() {

        return new SupplyPlanOpenOrdersHeuristicSpi() {

            @Override
            public void materializaEntradasECarteiraParaNovoPlanoHeuristico(
                    SupplyPlan supplyPlan,
                    PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
                    SupplyNetworkProjection supplyNetworkProjection,
                    PoliticaEstoquesProjection politicaEstoquesProjection,
                    MaterialProjection materialProjection,
                    LocationProjection locationProjection) {

                /* Materialização detalhada é coberta no módulo Enterprise. */
            }

            @Override
            public void materializaCarteiraParaDemandaDiretaHeuristica(
                    SupplyPlan supplyPlan,
                    PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
                    SupplyNetworkProjection supplyNetworkProjection,
                    PoliticaEstoquesProjection politicaEstoquesProjection,
                    MaterialProjection materialProjection,
                    LocationProjection locationProjection) {

                /* Recuperação detalhada é coberta no módulo Enterprise. */
            }
        };

    }

    private static ProductionPlanLinhaRepository getProductionPlanLinhaRepositoryRetornandoSaveAll(
            Object retornoSaveAll) {

        return getRepositoryRetornandoSaveAll(
                ProductionPlanLinhaRepository.class,
                retornoSaveAll,
                "ProductionPlanLinhaRepository retornando saveAll customizado para teste Community");

    }

    private static ProductionPlanLinhaRepository getProductionPlanLinhaRepositoryCapturandoSaveAll(
            List<ProductionPlanLinha> productionPlanLinhasSalvas) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("saveAll".equals(method.getName())) {
                Iterable<?> iterableProductionPlanLinhas = (Iterable<?>) args[0];
                for (Object productionPlanLinha : iterableProductionPlanLinhas) {
                    productionPlanLinhasSalvas.add((ProductionPlanLinha) productionPlanLinha);
                }
                return productionPlanLinhasSalvas;
            }
            if ("toString".equals(method.getName())) {
                return "ProductionPlanLinhaRepository capturando saveAll para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return ProductionPlanLinhaRepository.class.cast(Proxy.newProxyInstance(
                ProductionPlanLinhaRepository.class.getClassLoader(),
                new Class<?>[]{ProductionPlanLinhaRepository.class},
                invocationHandler));

    }

    private static ProductionPlanLinhaRepository getProductionPlanLinhaRepositoryFalhandoEmMutacao() {

        return getRepositoryFalhandoEmMutacao(
                ProductionPlanLinhaRepository.class,
                "ProductionPlanLinhaRepository");

    }

    private static DistributionPlanItemRepository getDistributionPlanItemRepositoryRetornandoSaveAll(
            Object retornoSaveAll) {

        return getRepositoryRetornandoSaveAll(
                DistributionPlanItemRepository.class,
                retornoSaveAll,
                "DistributionPlanItemRepository retornando saveAll customizado para teste Community");

    }

    private static DistributionPlanItemRepository getDistributionPlanItemRepositoryFalhandoEmMutacao() {

        return getRepositoryFalhandoEmMutacao(
                DistributionPlanItemRepository.class,
                "DistributionPlanItemRepository");

    }

    private static InventoryPlanLinhaRepository getInventoryPlanLinhaRepositoryRetornandoSaveAll(
            Object retornoSaveAll) {

        return getRepositoryRetornandoSaveAll(
                InventoryPlanLinhaRepository.class,
                retornoSaveAll,
                "InventoryPlanLinhaRepository retornando saveAll customizado para teste Community");

    }

    private static InventoryPlanLinhaRepository getInventoryPlanLinhaRepositoryFalhandoEmMutacao() {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("saveAll".equals(method.getName())
                    || "deleteAll".equals(method.getName())
                    || "flush".equals(method.getName())) {
                throw new AssertionError(
                        "InventoryPlanLinhaRepository nao deveria ser chamado antes da validacao Community.");
            }
            if ("toString".equals(method.getName())) {
                return "InventoryPlanLinhaRepository bloqueando mutacoes para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return InventoryPlanLinhaRepository.class.cast(Proxy.newProxyInstance(
                InventoryPlanLinhaRepository.class.getClassLoader(),
                new Class<?>[]{InventoryPlanLinhaRepository.class},
                invocationHandler));

    }

    private static <T> T getRepositoryFalhandoEmMutacao(
            Class<T> repositoryClass,
            String repositoryDescription) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("saveAll".equals(method.getName())
                    || "deleteAll".equals(method.getName())
                    || "flush".equals(method.getName())) {
                throw new AssertionError(
                        repositoryDescription
                                + " nao deveria ser chamado antes da validacao Community.");
            }
            if ("toString".equals(method.getName())) {
                return repositoryDescription + " bloqueando mutacoes para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return repositoryClass.cast(Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                invocationHandler));

    }

    private static <T> T getRepositoryRetornandoSaveAll(
            Class<T> repositoryClass,
            Object retornoSaveAll,
            String descricaoToString) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("saveAll".equals(method.getName())) {
                return retornoSaveAll;
            }
            if ("flush".equals(method.getName())) {
                return null;
            }
            if ("toString".equals(method.getName())) {
                return descricaoToString;
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return repositoryClass.cast(Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                invocationHandler));

    }

    private static DemandPlanRepository getDemandPlanRepositoryVazio() {

        return getRepositoryProxy(
                DemandPlanRepository.class,
                "customFindByIdComPerfilExecucao");

    }

    private static VersaoMalhaRepository getVersaoMalhaRepositoryVazio() {

        return getRepositoryProxy(
                VersaoMalhaRepository.class,
                "findById");

    }

    private static VersaoMalhaRepository getVersaoMalhaRepositoryComVersaoMalha(
            VersaoMalha versaoMalha) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(versaoMalha);
            }
            if ("toString".equals(method.getName())) {
                return "VersaoMalhaRepository com versao em memoria para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return VersaoMalhaRepository.class.cast(Proxy.newProxyInstance(
                VersaoMalhaRepository.class.getClassLoader(),
                new Class<?>[]{VersaoMalhaRepository.class},
                invocationHandler));

    }

    private static DemandPlanRepository getDemandPlanRepositoryComDemandPlan(
            DemandPlan demandPlan) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindByIdComPerfilExecucao".equals(method.getName())) {
                return Optional.of(demandPlan);
            }
            if ("toString".equals(method.getName())) {
                return "DemandPlanRepository com Demand Plan em memoria para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return DemandPlanRepository.class.cast(Proxy.newProxyInstance(
                DemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{DemandPlanRepository.class},
                invocationHandler));

    }

    private static PerfilExecucaoSupplyPlanRepository getPerfilExecucaoSupplyPlanRepositoryVazio() {

        return getRepositoryProxy(
                PerfilExecucaoSupplyPlanRepository.class,
                "customFindById");

    }

    private static PerfilExecucaoSupplyPlanRepository getPerfilExecucaoSupplyPlanRepositoryComPerfil(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindById".equals(method.getName())) {
                return Optional.of(perfilExecucaoSupplyPlan);
            }
            if ("toString".equals(method.getName())) {
                return "PerfilExecucaoSupplyPlanRepository com perfil em memoria para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return PerfilExecucaoSupplyPlanRepository.class.cast(Proxy.newProxyInstance(
                PerfilExecucaoSupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{PerfilExecucaoSupplyPlanRepository.class},
                invocationHandler));

    }

    private static SupplyPlanRepository getSupplyPlanRepositoryVazio() {

        return getRepositoryProxy(
                SupplyPlanRepository.class,
                "customFindById");

    }

    /**
     * Cria repository que aceita exclusivamente o selector com os fetches da
     * lista administrativa. Assim, os testes de contrato falham se o service
     * regredir para {@code findAll()} e reintroduzir N+1 no front.
     */
    private static SupplyPlanRepository getSupplyPlanRepositoryComSelector(
            List<SupplyPlan> supplyPlans) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindAllForSelector".equals(method.getName())) {
                return supplyPlans;
            }
            if ("toString".equals(method.getName())) {
                return "SupplyPlanRepository com selector em memoria para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return SupplyPlanRepository.class.cast(Proxy.newProxyInstance(
                SupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{SupplyPlanRepository.class},
                invocationHandler));

    }

    private static SupplyPlanRepository getSupplyPlanRepositoryComSupplyPlan(
            SupplyPlan supplyPlan) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(supplyPlan);
            }
            if ("toString".equals(method.getName())) {
                return "SupplyPlanRepository com plano em memoria para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return SupplyPlanRepository.class.cast(Proxy.newProxyInstance(
                SupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{SupplyPlanRepository.class},
                invocationHandler));

    }

    private static SupplyPlanRepository getSupplyPlanRepositoryComCustomFindById(
            SupplyPlan supplyPlan) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindById".equals(method.getName())) {
                return Optional.of(supplyPlan);
            }
            if ("toString".equals(method.getName())) {
                return "SupplyPlanRepository com customFindById em memoria para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return SupplyPlanRepository.class.cast(Proxy.newProxyInstance(
                SupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{SupplyPlanRepository.class},
                invocationHandler));

    }

    private static SupplyPlanRepository getSupplyPlanRepositoryComUmaConsultaCustomFindById(
            Optional<SupplyPlan> optionalSupplyPlan) {

        int[] quantidadeConsultasCustomFindById = new int[]{0};
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindById".equals(method.getName())) {
                quantidadeConsultasCustomFindById[0]++;
                if (quantidadeConsultasCustomFindById[0] > 1) {
                    throw new AssertionError(
                            "customFindById foi chamado para snapshot auxiliar antes da validacao de edicao.");
                }
                return optionalSupplyPlan;
            }
            if ("toString".equals(method.getName())) {
                return "SupplyPlanRepository com customFindById unico para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return SupplyPlanRepository.class.cast(Proxy.newProxyInstance(
                SupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{SupplyPlanRepository.class},
                invocationHandler));

    }

    private static SupplyPlanRepository getSupplyPlanRepositoryParaPlanoNovoProcessChain() {

        int[] quantidadeConsultasCustomFindById = new int[]{0};
        int[] quantidadeChamadasSaveAndFlush = new int[]{0};
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindById".equals(method.getName())) {
                quantidadeConsultasCustomFindById[0]++;
                if (quantidadeConsultasCustomFindById[0] > 1) {
                    throw new AssertionError(
                            "customFindById nao deve ser chamado novamente antes do desvio para process chain.");
                }
                return Optional.empty();
            }
            if ("saveAndFlush".equals(method.getName())) {
                quantidadeChamadasSaveAndFlush[0]++;
                if (quantidadeChamadasSaveAndFlush[0] > 1) {
                    throw new AssertionError(
                            "Plano novo em process chain deve salvar somente o header antes da SPI.");
                }
                SupplyPlan supplyPlanSalvo = (SupplyPlan) args[0];
                supplyPlanSalvo.setId(1000L);
                return supplyPlanSalvo;
            }
            if ("toString".equals(method.getName())) {
                return "SupplyPlanRepository com persistencia de header para process chain em teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return SupplyPlanRepository.class.cast(Proxy.newProxyInstance(
                SupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{SupplyPlanRepository.class},
                invocationHandler));

    }

    private static <T> T getRepositoryProxy(
            Class<T> repositoryClass,
            String optionalMethodName) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if (optionalMethodName.equals(method.getName())) {
                return Optional.empty();
            }
            if ("toString".equals(method.getName())) {
                return repositoryClass.getSimpleName() + " vazio para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return repositoryClass.cast(Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                invocationHandler));

    }

    /**
     * Cria um repository estrito para testar a decisao de preservar ou
     * reconstruir a fotografia de demanda direta sem subir o contexto Spring.
     */
    private static DemandaDiretaConsideradaLinhaRepository
    getDemandaDiretaConsideradaLinhaRepositoryComExistencia(
            boolean existeFotografia,
            List<Long> consultedSupplyPlanIds) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("existsByDemandaDiretaConsideradaLinhaCompositeKeySupplyPlanId".equals(method.getName())) {
                consultedSupplyPlanIds.add((Long) args[0]);
                return existeFotografia;
            }
            if ("toString".equals(method.getName())) {
                return "DemandaDiretaConsideradaLinhaRepository para teste de reexecucao";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de demanda direta: " + method.getName());
        };

        return DemandaDiretaConsideradaLinhaRepository.class.cast(Proxy.newProxyInstance(
                DemandaDiretaConsideradaLinhaRepository.class.getClassLoader(),
                new Class<?>[]{DemandaDiretaConsideradaLinhaRepository.class},
                invocationHandler));

    }

    private static <T> T getRepositoryProxyRetornandoOptionalNulo(
            Class<T> repositoryClass,
            String optionalMethodName) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if (optionalMethodName.equals(method.getName())) {
                return null;
            }
            if ("toString".equals(method.getName())) {
                return repositoryClass.getSimpleName() + " retornando Optional nulo para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return repositoryClass.cast(Proxy.newProxyInstance(
                repositoryClass.getClassLoader(),
                new Class<?>[]{repositoryClass},
                invocationHandler));

    }

    private static void assertSupplyPlanExceptionMessage(
            ThrowingReflectionCall throwingReflectionCall,
            String expectedMessage) {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                throwingReflectionCall::execute);

        Assertions.assertInstanceOf(
                SupplyPlanException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                expectedMessage,
                invocationTargetException.getCause().getMessage());

    }

    private static void assertIllegalArgumentMessage(
            ThrowingReflectionCall throwingReflectionCall,
            String expectedMessage) {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                throwingReflectionCall::execute);

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                expectedMessage,
                invocationTargetException.getCause().getMessage());

    }

    private static void assertIllegalStateMessage(
            ThrowingReflectionCall throwingReflectionCall,
            String expectedMessage) {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                throwingReflectionCall::execute);

        Assertions.assertInstanceOf(
                IllegalStateException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                expectedMessage,
                invocationTargetException.getCause().getMessage());

    }

    private static DemandPlan invokeGetDemandPlanObrigatorio(
            SupplyPlanService supplyPlanService,
            Long demandPlanId) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getDemandPlanObrigatorio",
                Long.class);
        method.setAccessible(true);
        return (DemandPlan) method.invoke(
                supplyPlanService,
                demandPlanId);

    }

    private static VersaoMalha invokeGetVersaoMalhaObrigatoria(
            SupplyPlanService supplyPlanService,
            String versaoMalhaId) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getVersaoMalhaObrigatoria",
                String.class);
        method.setAccessible(true);
        return (VersaoMalha) method.invoke(
                supplyPlanService,
                versaoMalhaId);

    }

    private static PerfilExecucaoSupplyPlan invokeGetPerfilExecucaoSupplyPlanObrigatorio(
            SupplyPlanService supplyPlanService,
            String perfilExecucaoSupplyPlanId) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getPerfilExecucaoSupplyPlanObrigatorio",
                String.class);
        method.setAccessible(true);
        return (PerfilExecucaoSupplyPlan) method.invoke(
                supplyPlanService,
                perfilExecucaoSupplyPlanId);

    }

    private static SupplyPlan invokeGetSupplyPlanParaProjecaoEstoqueInicialObrigatorio(
            SupplyPlanService supplyPlanService,
            Long supplyPlanIdParaProjecaoEstoqueInicial) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getSupplyPlanParaProjecaoEstoqueInicialObrigatorio",
                Long.class);
        method.setAccessible(true);
        return (SupplyPlan) method.invoke(
                supplyPlanService,
                supplyPlanIdParaProjecaoEstoqueInicial);

    }

    private static ClusterEParametrosProjection invokeGetClusterEParametrosProjectionObrigatoria(
            SupplyPlanService supplyPlanService,
            ClusterEParametrosProjection clusterEParametrosProjection,
            String contexto) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getClusterEParametrosProjectionObrigatoria",
                ClusterEParametrosProjection.class,
                String.class);
        method.setAccessible(true);
        return (ClusterEParametrosProjection) method.invoke(
                supplyPlanService,
                clusterEParametrosProjection,
                contexto);

    }

    private static void invokeValidaUnidadeMedidaProjectionObrigatoria(
            SupplyPlanService supplyPlanService,
            UnidadeMedidaProjection unidadeMedidaProjection,
            String contexto) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "validaUnidadeMedidaProjectionObrigatoria",
                UnidadeMedidaProjection.class,
                String.class);
        method.setAccessible(true);
        method.invoke(
                supplyPlanService,
                unidadeMedidaProjection,
                contexto);

    }

    private static void invokeValidaProductionPlanLinhasSalvasCommunity(
            SupplyPlanService supplyPlanService,
            Collection<ProductionPlanLinha> productionPlanLinhasSalvas) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "validaProductionPlanLinhasSalvasCommunity",
                Collection.class,
                int.class);
        method.setAccessible(true);
        method.invoke(
                supplyPlanService,
                productionPlanLinhasSalvas,
                productionPlanLinhasSalvas.size());

    }

    private static void invokeValidaDistributionPlanItemsSalvasCommunity(
            SupplyPlanService supplyPlanService,
            Collection<DistributionPlanItem> distributionPlanItemsSalvas) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "validaDistributionPlanItemsSalvasCommunity",
                Collection.class,
                int.class);
        method.setAccessible(true);
        method.invoke(
                supplyPlanService,
                distributionPlanItemsSalvas,
                distributionPlanItemsSalvas.size());

    }

    private static void invokeValidaInventoryPlanLinhasSalvasCommunity(
            SupplyPlanService supplyPlanService,
            Collection<InventoryPlanLinha> inventoryPlanLinhasSalvas) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "validaInventoryPlanLinhasSalvasCommunity",
                Collection.class,
                int.class);
        method.setAccessible(true);
        method.invoke(
                supplyPlanService,
                inventoryPlanLinhasSalvas,
                inventoryPlanLinhasSalvas.size());

    }

    private static SupplyNetworkProjection invokeGetSupplyNetworkProjectionObrigatoria(
            SupplyPlanService supplyPlanService,
            SupplyNetworkProjection supplyNetworkProjection,
            String contexto) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getSupplyNetworkProjectionObrigatoria",
                SupplyNetworkProjection.class,
                String.class);
        method.setAccessible(true);
        return (SupplyNetworkProjection) method.invoke(
                supplyPlanService,
                supplyNetworkProjection,
                contexto);

    }

    private static PoliticaEstoquesProjection invokeGetPoliticaEstoquesProjectionObrigatoria(
            SupplyPlanService supplyPlanService,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            String contexto) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getPoliticaEstoquesProjectionObrigatoria",
                PoliticaEstoquesProjection.class,
                String.class);
        method.setAccessible(true);
        return (PoliticaEstoquesProjection) method.invoke(
                supplyPlanService,
                politicaEstoquesProjection,
                contexto);

    }

    private static LocationProjection invokeGetLocationProjectionObrigatoria(
            SupplyPlanService supplyPlanService,
            LocationProjection locationProjection,
            String contexto) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getLocationProjectionObrigatoria",
                LocationProjection.class,
                String.class);
        method.setAccessible(true);
        return (LocationProjection) method.invoke(
                supplyPlanService,
                locationProjection,
                contexto);

    }

    private static MaterialProjection invokeGetMaterialProjectionObrigatoria(
            SupplyPlanService supplyPlanService,
            MaterialProjection materialProjection,
            String contexto) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getMaterialProjectionObrigatoria",
                MaterialProjection.class,
                String.class);
        method.setAccessible(true);
        return (MaterialProjection) method.invoke(
                supplyPlanService,
                materialProjection,
                contexto);

    }

    private static String invokeGetDescricaoSupplyPlanOuDefault(
            SupplyPlanService supplyPlanService,
            String descricao,
            Constantes.TamanhoBucket tamanhoBucket) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getDescricaoSupplyPlanOuDefault",
                String.class,
                Constantes.TamanhoBucket.class);
        method.setAccessible(true);
        return (String) method.invoke(
                supplyPlanService,
                descricao,
                tamanhoBucket);

    }

    private static Constantes.TamanhoBucket invokeGetTamanhoBucketConsideradoParaProjecaoEstoqueInicialAPartirPreEstoque(
            SupplyPlanService supplyPlanService,
            Constantes.TamanhoBucket tamanhoBucketSupplyPlan) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getTamanhoBucketConsideradoParaProjecaoEstoqueInicialAPartirPreEstoque",
                Constantes.TamanhoBucket.class);
        method.setAccessible(true);
        return (Constantes.TamanhoBucket) method.invoke(
                supplyPlanService,
                tamanhoBucketSupplyPlan);

    }

    private static Integer invokeGetOffsetPeriodosLeadTimeDemandaPropagadaObrigatorio(
            SupplyPlanService supplyPlanService,
            Optional<Integer> optionalOffsetPeriodosLeadTime,
            SupplyPlan supplyPlan,
            Location locationConsiderada,
            Location locationComPlanoDemanda,
            Produto material) throws Exception {

        Method method = SupplyPlanService.class.getDeclaredMethod(
                "getOffsetPeriodosLeadTimeDemandaPropagadaObrigatorio",
                Optional.class,
                SupplyPlan.class,
                Location.class,
                Location.class,
                Produto.class);
        method.setAccessible(true);
        return (Integer) method.invoke(
                supplyPlanService,
                optionalOffsetPeriodosLeadTime,
                supplyPlan,
                locationConsiderada,
                locationComPlanoDemanda,
                material);

    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        while (currentDirectory != null
                && !"opsfactor-community".equals(currentDirectory.getFileName().toString())) {
            currentDirectory = currentDirectory.getParent();
        }
        if (currentDirectory == null) {
            throw new IllegalStateException("Could not resolve opsfactor-community workspace directory.");
        }
        return currentDirectory;

    }

    @FunctionalInterface
    private interface ThrowingReflectionCall {

        void execute() throws Exception;

    }

    private static class StubSupplyPlanOptimizationService implements SupplyPlanOptimizationServiceSpi {

        @Override
        public void reiniciaSupplyPlanExistente(SupplyPlan supplyPlan) {

        }

        @Override
        public void executaSupplyPlan(
                SupplyPlan supplyPlan,
                SupplyPlan supplyPlanParaProjecaoEstoqueInicial,
                PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
                boolean novoSupplyPlan,
                boolean consideraRequisicoesEtapaAnterior,
                boolean consideraOrdensProducaoPlanejadasEtapaAnterior) {

        }

        @Override
        public void executaSupplyPlan(
                SupplyPlan supplyPlan,
                SupplyPlan supplyPlanParaProjecaoEstoqueInicial,
                PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
                boolean novoSupplyPlan,
                boolean consideraRequisicoesEtapaAnterior,
                boolean consideraOrdensProducaoPlanejadasEtapaAnterior,
                SupplyNetworkProjection supplyNetworkProjection,
                BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
                PoliticaEstoquesProjection politicaEstoquesProjection,
                MaterialProjection materialProjection,
                LocationProjection locationProjection) {

        }

    }

    private static class StubSupplyPlanProcessChainService implements SupplyPlanProcessChainServiceSpi {

        private SupplyPlan supplyPlanRecebido;

        private SupplyPlan supplyPlanParaProjecaoEstoqueInicialRecebido;

        private PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanRecebido;

        private boolean novoSupplyPlanRecebido;

        private boolean consideraRequisicoesEtapaAnteriorRecebido;

        private boolean consideraOrdensProducaoPlanejadasEtapaAnteriorRecebido;

        @Override
        public void reiniciaSupplyPlanExistente(SupplyPlan supplyPlan) {

        }

        @Override
        public void executaSupplyPlan(
                SupplyPlan supplyPlan,
                SupplyPlan supplyPlanParaProjecaoEstoqueInicial,
                PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
                boolean novoSupplyPlan,
                boolean consideraRequisicoesEtapaAnterior,
                boolean consideraOrdensProducaoPlanejadasEtapaAnterior) {

            this.supplyPlanRecebido = supplyPlan;
            this.supplyPlanParaProjecaoEstoqueInicialRecebido = supplyPlanParaProjecaoEstoqueInicial;
            this.perfilExecucaoSupplyPlanRecebido = perfilExecucaoSupplyPlan;
            this.novoSupplyPlanRecebido = novoSupplyPlan;
            this.consideraRequisicoesEtapaAnteriorRecebido = consideraRequisicoesEtapaAnterior;
            this.consideraOrdensProducaoPlanejadasEtapaAnteriorRecebido =
                    consideraOrdensProducaoPlanejadasEtapaAnterior;

        }

    }

    private static class TestClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        private TestClusterEParametrosProjectionFactory(
                ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            /*
             * O teste da rota publica de process chain precisa apenas do
             * snapshot de parametros globais para normalizar o calendario do
             * header. Qualquer acesso ao factory real abriria caminho para
             * repositories de master data e transformaria o contrato em teste
             * de integracao acidental.
             */
            return clusterEParametrosProjection;

        }

    }

    private static class TestJdbcTemplate extends JdbcTemplate {

        private final List<Object> supplyPlanIdsAtualizados = new ArrayList<>();

        @Override
        public int update(String sql, Object... args) {

            /*
             * O teste nao valida SQL literal; ele valida o contrato de chamada
             * do reset compartilhado. Cada update deve receber o id do Supply
             * Plan reaproveitado como unico parametro.
             */
            Assertions.assertEquals(1, args.length);
            supplyPlanIdsAtualizados.add(args[0]);
            return 1;

        }

    }

    private static class TestDemandaDiretaConsideradaLinhaDAO extends DemandaDiretaConsideradaLinhaDAO {

        private final List<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhasSalvas =
                new ArrayList<>();

        private final boolean falhaEmSaveInBatch;

        private TestDemandaDiretaConsideradaLinhaDAO() {

            this(false);

        }

        private TestDemandaDiretaConsideradaLinhaDAO(
                boolean falhaEmSaveInBatch) {

            this.falhaEmSaveInBatch = falhaEmSaveInBatch;

        }

        @Override
        public void saveInBatch(
                Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhas) {

            if (falhaEmSaveInBatch) {
                throw new AssertionError(
                        "DemandaDiretaConsideradaLinhaDAO nao deveria ser chamado antes da validacao Community.");
            }

            /*
             * O teste valida o contrato do service antes do DAO JDBC real:
             * quais linhas chegam ao batch e quais componentes Enterprise
             * foram neutralizados em memoria.
             */
            demandaDiretaConsideradaLinhasSalvas.addAll(demandaDiretaConsideradaLinhas);

        }

    }

    private static class TestDemandPlanningProjection extends DemandPlanningProjection {

        private final List<Constantes.TipoDemanda> tipoDemandasConsultados = new ArrayList<>();

        private TestDemandPlanningProjection() {

            super(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    null);

        }

        @Override
        public double getValorDemandPlanItemNoCalendarioTargetSplitTemporal(
                SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfu,
                int posicaoPeriodoCalendarioTarget,
                Location location,
                Produto material,
                Constantes.TipoDemanda tipoDemanda,
                Constantes.TipoPlano tipoPlano,
                UnidadeMedida unidadeMedida) {

            tipoDemandasConsultados.add(tipoDemanda);
            return switch (tipoDemanda) {
                case BASELINE -> 10.0;
                case AJUSTE_DEMANDA -> 2.0;
                case TOTAL -> 999.0;
                default -> 0.0;
            };

        }

    }

}
