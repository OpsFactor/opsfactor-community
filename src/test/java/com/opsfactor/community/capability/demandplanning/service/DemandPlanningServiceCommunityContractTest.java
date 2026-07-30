package com.opsfactor.community.capability.demandplanning.service;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastStockoutContext;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosGeraisDemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjectionSimples;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosMediaMovel;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanItemRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.HistoricoDemandPlanItemRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.demandplanning.engine.DemandPlanning;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastHistoryCleaningProcessor;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastStockoutTreatmentProcessor;
import com.opsfactor.community.capability.demandplanning.forecast.service.DemandForecastWorkflowService;
import com.opsfactor.community.capability.demandplanning.service.spi.CommunityDemandPlanReferenceCopySpi;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Valida barreiras Community do service de Demand Planning sem subir Spring nem
 * banco. O foco e proteger rotas transicionais que ainda existem no service,
 * mesmo quando a UI Community ja bloqueia a feature correspondente.
 */
public class DemandPlanningServiceCommunityContractTest {

    @Test
    public void serviceShouldExposeProtectedForecastHooksWithoutChangingCommunityPublicContract()
            throws Exception {

        Method resolvedParametersHook = DemandPlanningService.class.getDeclaredMethod(
                "geraDemandPlanForecastProjectionsExecucaoComParametrosResolvidos",
                Calendario.class,
                ParametrosDemandPlanNivelClusterProjection.class,
                MaterialProjection.class,
                LocationProjection.class,
                SalesProjectionLocationMaterialData.class,
                ParametrosGeraisDemandPlanningProjection.class,
                ParametrosForecastProjection.class,
                ClusterEParametrosProjection.class,
                boolean.class);
        Method stockoutContextHook = DemandPlanningService.class.getDeclaredMethod(
                "geraDemandPlanForecastProjectionsExecucaoComForecastEContextoStockout",
                List.class,
                Calendario.class,
                MaterialProjection.class,
                LocationProjection.class,
                ParametrosGeraisDemandPlanningProjection.class,
                ParametrosForecastProjection.class,
                ClusterEParametrosProjection.class,
                boolean.class,
                DemandForecastStockoutContext.class);

        Assertions.assertTrue(java.lang.reflect.Modifier.isProtected(
                resolvedParametersHook.getModifiers()));
        Assertions.assertTrue(java.lang.reflect.Modifier.isProtected(
                stockoutContextHook.getModifiers()));

    }

    @Test
    public void serviceShouldUseExplicitAutowiredBeanFieldsWithoutDeadClusteringService() throws Exception {

        assertAutowiredFields(
                "clusterLocationService",
                "demandForecastWorkflowService",
                "demandPlanItemRepository",
                "historicoDemandPlanItemRepository",
                "clusterMateriaisDemandPlanningRepository",
                "demandPlanRepository",
                "supplyPlanRepository",
                "perfilExecucaoDemandPlanRepository",
                "salesProjectionFactory",
                "demandPlanProjectionFactory",
                "clusterEParametrosProjectionFactory",
                "unidadeMedidaProjectionFactory",
                "parametrosDemandPlanningProjectionFactory",
                "jdbcTemplate");

        /*
         * O service principal nao deve manter beans sem uso: eles confundem a
         * fronteira Community/Enterprise e sugerem dependencia que nao existe.
         */
        Assertions.assertThrows(
                NoSuchFieldException.class,
                () -> DemandPlanningService.class.getDeclaredField("clusteringService"));

    }

    @Test
    public void validaParametrosForecastCommunityShouldRejectEnterpriseStatisticalModel() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        ParametrosForecastProjection parametrosForecastProjection = getCommunityParametrosForecastProjection();
        parametrosForecastProjection.setDpModeloEstatistico(Constantes.DPModeloEstatistico.STL);

        assertRequiresEnterpriseVersionException(
                demandPlanningService,
                "validaParametrosForecastCommunity",
                parametrosForecastProjection);

    }

    @Test
    public void validaParametrosForecastCommunityShouldRejectMissingForecastParameters() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();

        /*
         * O bloco de forecast inteiro e obrigatorio para a execucao. Se ele
         * vier ausente, a falha deve ser explicita de payload/configuracao
         * incompleta, antes de acessar modelo estatistico, split ou projections.
         */
        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaParametrosForecastCommunity(
                        demandPlanningService,
                        null));
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Demand Planning forecast parameters are required",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaParametrosForecastCommunityShouldRejectMissingStatisticalModelAsBrokenConfiguration() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        ParametrosForecastProjection parametrosForecastProjection =
                getCommunityParametrosForecastProjection();
        parametrosForecastProjection.setDpModeloEstatistico(null);

        /*
         * Modelo nulo nao e capability Enterprise bloqueada: e configuracao
         * incompleta. O service precisa falhar antes de construir projections,
         * carregar historico de vendas ou classificar a edicao.
         */
        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaParametrosForecastCommunity(
                        demandPlanningService,
                        parametrosForecastProjection));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Demand Planning statistical forecast model is required",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaParametrosForecastCommunityShouldRejectMissingSplitModelAsBrokenConfiguration() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        ParametrosForecastProjection parametrosForecastProjection =
                getCommunityParametrosForecastProjection();
        parametrosForecastProjection.setDpModeloSplit(null);

        /*
         * Split nulo tambem representa payload/configuracao quebrada. A falha
         * deve ser IllegalArgumentException para nao confundir esse caso com
         * HTS/Forecast Proportion bloqueados no Community.
         */
        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaParametrosForecastCommunity(
                        demandPlanningService,
                        parametrosForecastProjection));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Demand Planning forecast split model is required",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void executionClusterSnapshotsShouldRejectBrokenCollectionsBeforeParallelLoop() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        List<ClusterLocations> clusterLocationListComItemNulo = new ArrayList<>();
        ClusterLocations clusterLocationsValido = new ClusterLocations("LOCATION_CLUSTER", false, 1);
        clusterLocationsValido.setId(1L);
        clusterLocationListComItemNulo.add(clusterLocationsValido);
        clusterLocationListComItemNulo.add(null);
        List<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningListComItemNulo = new ArrayList<>();
        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanningValido =
                new ClusterProdutosDemandPlanning("MATERIAL_CLUSTER", false, 1);
        clusterMateriaisDemandPlanningValido.setId(1L);
        clusterMateriaisDemandPlanningListComItemNulo.add(clusterMateriaisDemandPlanningValido);
        clusterMateriaisDemandPlanningListComItemNulo.add(null);
        List<ClusterLocations> clusterLocationListComItemSemId = List.of(
                new ClusterLocations("LOCATION_CLUSTER_WITHOUT_ID", false, 1));
        List<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningListComItemSemId = List.of(
                new ClusterProdutosDemandPlanning("MATERIAL_CLUSTER_WITHOUT_ID", false, 1));

        InvocationTargetException clusterLocationCollectionAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetClusterLocationsDemandPlanningParaExecucaoCommunity(
                        demandPlanningService,
                        null));
        InvocationTargetException clusterLocationItemAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetClusterLocationsDemandPlanningParaExecucaoCommunity(
                        demandPlanningService,
                        clusterLocationListComItemNulo));
        InvocationTargetException clusterMateriaisCollectionAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetClusterMateriaisDemandPlanningParaExecucaoCommunity(
                        demandPlanningService,
                        null));
        InvocationTargetException clusterMateriaisItemAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetClusterMateriaisDemandPlanningParaExecucaoCommunity(
                        demandPlanningService,
                        clusterMateriaisDemandPlanningListComItemNulo));
        InvocationTargetException clusterLocationItemSemIdException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetClusterLocationsDemandPlanningParaExecucaoCommunity(
                        demandPlanningService,
                        clusterLocationListComItemSemId));
        InvocationTargetException clusterMateriaisItemSemIdException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetClusterMateriaisDemandPlanningParaExecucaoCommunity(
                        demandPlanningService,
                        clusterMateriaisDemandPlanningListComItemSemId));

        /*
         * Snapshot vazio continua valido e apenas nao entra no loop. Snapshot
         * quebrado precisa falhar antes da execucao paralela, onde a origem do
         * NPE fica muito menos legivel. O id tambem e obrigatorio porque as
         * projections de configuracao e DFU usam igualdade por entidade/id.
         */
        Assertions.assertEquals(
                "Demand Planning location cluster collection is required for Demand Planning execution.",
                clusterLocationCollectionAusenteException.getCause().getMessage());
        Assertions.assertEquals(
                "Demand Planning location cluster at index 1 is required for Demand Planning execution.",
                clusterLocationItemAusenteException.getCause().getMessage());
        Assertions.assertEquals(
                "Demand Planning material cluster collection is required for Demand Planning execution.",
                clusterMateriaisCollectionAusenteException.getCause().getMessage());
        Assertions.assertEquals(
                "Demand Planning material cluster at index 1 is required for Demand Planning execution.",
                clusterMateriaisItemAusenteException.getCause().getMessage());
        Assertions.assertEquals(
                "Demand Planning location cluster at index 0 must have an id for Demand Planning execution.",
                clusterLocationItemSemIdException.getCause().getMessage());
        Assertions.assertEquals(
                "Demand Planning material cluster at index 0 must have an id for Demand Planning execution.",
                clusterMateriaisItemSemIdException.getCause().getMessage());
        Assertions.assertDoesNotThrow(
                () -> invokeGetClusterLocationsDemandPlanningParaExecucaoCommunity(
                        demandPlanningService,
                        List.of()));
        Assertions.assertDoesNotThrow(
                () -> invokeGetClusterMateriaisDemandPlanningParaExecucaoCommunity(
                        demandPlanningService,
                        List.of()));

    }

    @Test
    public void validaParametrosForecastCommunityShouldAcceptAllCommunityStatisticalModels() {

        DemandPlanningService demandPlanningService = new DemandPlanningService();

        /*
         * Estes sao os modelos estatisticos efetivamente implementaveis no
         * Community. O enum pode expor modelos Enterprise para compatibilidade
         * de payload/front, mas a validacao do service precisa aceitar todos os
         * modelos Community conhecidos sem exigir classe Enterprise.
         */
        for (Constantes.DPModeloEstatistico dpModeloEstatistico : getDpModelosEstatisticosCommunity()) {
            ParametrosForecastProjection parametrosForecastProjection = getCommunityParametrosForecastProjection();
            parametrosForecastProjection.setDpModeloEstatistico(dpModeloEstatistico);

            Assertions.assertDoesNotThrow(
                    () -> invokeValidation(
                            demandPlanningService,
                            "validaParametrosForecastCommunity",
                            parametrosForecastProjection),
                    "Modelo Community deveria passar na validacao: " + dpModeloEstatistico);
        }

    }

    @Test
    public void geraForecastAgregadoShouldRejectAllEnterpriseStatisticalModels() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);

        /*
         * A validacao do service bloqueia estes modelos antes da rotina, mas a
         * rotina estatistica Community tambem precisa falhar explicitamente se
         * algum caller transicional chamar o motor direto.
         */
        for (Constantes.DPModeloEstatistico dpModeloEstatistico : Constantes.DPModeloEstatistico.values()) {
            if (getDpModelosEstatisticosCommunity().contains(dpModeloEstatistico)) {
                continue;
            }

            ParametrosForecastProjection parametrosForecastProjection = getCommunityParametrosForecastProjection();
            parametrosForecastProjection.setDpModeloEstatistico(dpModeloEstatistico);

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> DemandPlanning.geraForecastAgregadoNoDemandPlanForecastProjection(
                            calendario,
                            parametrosForecastProjection,
                            demandPlanForecastProjectionMaterialLocation),
                    "Modelo Enterprise deveria falhar no motor Community: " + dpModeloEstatistico);
        }

    }

    @Test
    public void validaParametrosForecastCommunityShouldRejectEnterpriseSplitModel() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        ParametrosForecastProjection parametrosForecastProjection = getCommunityParametrosForecastProjection();
        parametrosForecastProjection.setDpModeloSplit(Constantes.DPModeloSplit.HTS);

        assertRequiresEnterpriseVersionException(
                demandPlanningService,
                "validaParametrosForecastCommunity",
                parametrosForecastProjection);

    }

    @Test
    public void geraDemandPlanForecastAgregadoPorClusterShouldRejectEnterpriseForecastBeforeSalesProjection() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        ClusterEParametrosProjectionStub clusterEParametrosProjectionStub =
                new ClusterEParametrosProjectionStub(parametrosGlobais);
        UnidadeMedidaProjectionFactoryStub unidadeMedidaProjectionFactoryStub =
                new UnidadeMedidaProjectionFactoryStub();
        SalesProjectionFactoryFailingStub salesProjectionFactoryFailingStub =
                new SalesProjectionFactoryFailingStub();

        setField(
                demandPlanningService,
                "clusterEParametrosProjectionFactory",
                new ClusterEParametrosProjectionFactoryStub(clusterEParametrosProjectionStub));
        setField(
                demandPlanningService,
                "unidadeMedidaProjectionFactory",
                unidadeMedidaProjectionFactoryStub);
        setField(
                demandPlanningService,
                "salesProjectionFactory",
                salesProjectionFactoryFailingStub);

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("PROFILE");
        perfilExecucaoDemandPlan.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        ParametrosForecastProjection parametrosForecastProjection = getCommunityParametrosForecastProjection();
        parametrosForecastProjection.setDpModeloEstatistico(Constantes.DPModeloEstatistico.STL);

        ParametrosDemandPlanNivelClusterProjectionSimples parametrosDemandPlanNivelClusterProjection =
                new ParametrosDemandPlanNivelClusterProjectionSimples(
                        perfilExecucaoDemandPlan,
                        new ClusterLocations("LOCATION_CLUSTER", true, 1),
                        new ClusterProdutosDemandPlanning("MATERIAL_CLUSTER", true, 1),
                        new ParametrosGeraisDemandPlanningProjection(
                                true,
                                new ParametrosAgregacaoForecast(
                                        Constantes.DPNivelAgregacao.TOP_DOWN,
                                        Constantes.DPNivelAgregacao.TOP_DOWN),
                                3,
                                false,
                                false,
                                0,
                                new UnidadeMedida("UN"),
                                false,
                                parametrosGlobais),
                        parametrosForecastProjection);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        Calendario.criaCalendarioDeOffsetsDias(
                                Constantes.TamanhoBucket.DIARIO,
                                LocalDateTime.of(2026, 1, 10, 0, 0),
                                0,
                                3,
                                2,
                                0),
                        parametrosDemandPlanNivelClusterProjection,
                        false));
        Assertions.assertFalse(
                salesProjectionFactoryFailingStub.salesProjectionRequested,
                "Configuracao Enterprise deve falhar antes de consultar historico de vendas.");
        Assertions.assertFalse(
                unidadeMedidaProjectionFactoryStub.unidadeMedidaProjectionRequested,
                "Configuracao Enterprise deve falhar antes de carregar projection de unidade de medida.");

    }

    @Test
    public void validaParametrosForecastCommunityShouldRejectAllNonHistoricalSalesSplitModels() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();

        /*
         * Historical Sales e o unico split material/location disponivel no
         * Community. HTS e Forecast Proportion permanecem no enum compartilhado
         * apenas para o front bloquear visualmente e para o backend devolver
         * RequiresEnterpriseVersionException se receber payload manual.
         */
        for (Constantes.DPModeloSplit dpModeloSplit : Constantes.DPModeloSplit.values()) {
            if (Constantes.DPModeloSplit.HISTORICAL_SALES.equals(dpModeloSplit)) {
                continue;
            }

            ParametrosForecastProjection parametrosForecastProjection = getCommunityParametrosForecastProjection();
            parametrosForecastProjection.setDpModeloSplit(dpModeloSplit);

            assertRequiresEnterpriseVersionException(
                    demandPlanningService,
                    "validaParametrosForecastCommunity",
                    parametrosForecastProjection);
        }

    }

    @Test
    public void validaPerfilExecucaoDemandPlanDisponivelShouldRejectEnterpriseHistoricalDocuments() {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);

        /*
         * O hook fica protegido para o Enterprise trocar a allowlist via
         * `@Primary`, mas a implementacao Community continua sell-out only.
         * Assim um perfil legado com sell-in ou sales orders falha antes de
         * extrair historico ou iniciar a rodada.
         */
        for (Constantes.TipoDocumentoVenda tipoDocumentoVenda : List.of(
                Constantes.TipoDocumentoVenda.SELLIN,
                Constantes.TipoDocumentoVenda.PEDIDO)) {
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                    new PerfilExecucaoDemandPlan("PROFILE");
            perfilExecucaoDemandPlan.setTipoDocumentoVenda(tipoDocumentoVenda);

            RequiresEnterpriseVersionException requiresEnterpriseVersionException =
                    Assertions.assertThrows(
                            RequiresEnterpriseVersionException.class,
                            () -> invokeValidaPerfilExecucaoDemandPlanDisponivel(
                                    demandPlanningService,
                                    perfilExecucaoDemandPlan,
                                    parametrosGlobais));
            Assertions.assertTrue(
                    requiresEnterpriseVersionException.getMessage().contains(
                            "Sell-in and sales orders as historical sales source"));
        }

    }

    @Test
    public void validaPerfilExecucaoDemandPlanDisponivelShouldRejectMissingProfileAndGlobalParameters() {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                new PerfilExecucaoDemandPlan("PROFILE");

        IllegalArgumentException perfilAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> invokeValidaPerfilExecucaoDemandPlanDisponivel(
                        demandPlanningService,
                        null,
                        parametrosGlobais));
        IllegalArgumentException parametrosGlobaisAusentesException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> invokeValidaPerfilExecucaoDemandPlanDisponivel(
                        demandPlanningService,
                        perfilExecucaoDemandPlan,
                        null));

        Assertions.assertEquals(
                "Demand Planning execution profile is required",
                perfilAusenteException.getMessage());
        Assertions.assertEquals(
                "Demand Planning global parameters are required",
                parametrosGlobaisAusentesException.getMessage());

    }

    @Test
    public void geraForecastProjectionAwareShouldRejectMissingGeneralParametersBeforeWorkflow() {

        DemandPlanningService demandPlanningService = new DemandPlanningService();

        /*
         * A sobrecarga usada por simulacao/fluxos com projections ja
         * materializadas tambem deve validar o contrato de parametros gerais
         * antes de chamar processor, engine ou desagregacao.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        Calendario.criaCalendarioDeOffsetsDias(
                                Constantes.TamanhoBucket.DIARIO,
                                LocalDateTime.of(2026, 1, 10, 0, 0),
                                0,
                                3,
                                2,
                                0),
                        null,
                        null,
                        null,
                        null,
                        getCommunityParametrosForecastProjection(),
                        null,
                        false));

        Assertions.assertEquals(
                "Demand Planning general parameters are required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void geraForecastProjectionAwareShouldRejectMissingProjectionInputsBeforeStaticRoutine() {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        Calendario calendario = getCalendarioForecastTeste();
        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                getParametrosGeraisDemandPlanningProjectionBottomUp();
        ClusterEParametrosProjection clusterEParametrosProjection =
                new ClusterEParametrosProjectionStub(new ParametrosGlobais());
        MaterialProjection materialProjection = new MaterialProjection();
        LocationProjection locationProjection = new LocationProjection();
        SalesProjectionLocationMaterialData salesProjection =
                SalesProjectionLocationMaterialData.builder().build();

        /*
         * Esta entrada e usada por simulacao e execucao quando as projections ja
         * foram carregadas fora do metodo. Se algum snapshot estrutural vier
         * ausente, o service deve falhar aqui, antes das rotinas estaticas de
         * forecast/agregacao acessarem mapas internos e gerarem NPE sem
         * contexto funcional.
         */
        IllegalArgumentException calendarioAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        null,
                        materialProjection,
                        locationProjection,
                        salesProjection,
                        parametrosGeraisDemandPlanningProjection,
                        getCommunityParametrosForecastProjection(),
                        clusterEParametrosProjection,
                        false));
        IllegalArgumentException materialProjectionAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        calendario,
                        null,
                        locationProjection,
                        salesProjection,
                        parametrosGeraisDemandPlanningProjection,
                        getCommunityParametrosForecastProjection(),
                        clusterEParametrosProjection,
                        false));
        IllegalArgumentException locationProjectionAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        calendario,
                        materialProjection,
                        null,
                        salesProjection,
                        parametrosGeraisDemandPlanningProjection,
                        getCommunityParametrosForecastProjection(),
                        clusterEParametrosProjection,
                        false));
        IllegalArgumentException salesProjectionAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        calendario,
                        materialProjection,
                        locationProjection,
                        null,
                        parametrosGeraisDemandPlanningProjection,
                        getCommunityParametrosForecastProjection(),
                        clusterEParametrosProjection,
                        false));

        Assertions.assertEquals(
                "Demand Planning calendar is required for Demand Planning forecast projection generation.",
                calendarioAusenteException.getMessage());
        Assertions.assertEquals(
                "Demand Planning material projection is required for Demand Planning forecast projection generation.",
                materialProjectionAusenteException.getMessage());
        Assertions.assertEquals(
                "Demand Planning location projection is required for Demand Planning forecast projection generation.",
                locationProjectionAusenteException.getMessage());
        Assertions.assertEquals(
                "Demand Planning sales projection is required for Demand Planning forecast projection generation.",
                salesProjectionAusenteException.getMessage());

    }

    @Test
    public void geraForecastProjectionAwareShouldRejectMissingAggregationAndUomBeforeStaticRoutine() {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        Calendario calendario = getCalendarioForecastTeste();
        ClusterEParametrosProjection clusterEParametrosProjection =
                new ClusterEParametrosProjectionStub(new ParametrosGlobais());
        MaterialProjection materialProjection = new MaterialProjection();
        LocationProjection locationProjection = new LocationProjection();
        SalesProjectionLocationMaterialData salesProjection =
                SalesProjectionLocationMaterialData.builder().build();
        ParametrosGeraisDemandPlanningProjection parametrosGeraisSemAgregacao =
                getParametrosGeraisDemandPlanningProjectionBottomUp();
        ParametrosGeraisDemandPlanningProjection parametrosGeraisSemUnidadeMedida =
                getParametrosGeraisDemandPlanningProjectionBottomUp();

        parametrosGeraisSemAgregacao.setParametrosAgregacaoForecast(null);
        parametrosGeraisSemUnidadeMedida.setUnidadeMedidaDP(null);

        /*
         * A agregacao material/location e a UOM default sao parte do contrato
         * minimo para construir as series de forecast. Mesmo que um cluster
         * venha vazio, esses parametros precisam estar presentes para evitar
         * uma rodada aparentemente valida com configuracao quebrada.
         */
        IllegalArgumentException parametrosAgregacaoAusentesException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        calendario,
                        materialProjection,
                        locationProjection,
                        salesProjection,
                        parametrosGeraisSemAgregacao,
                        getCommunityParametrosForecastProjection(),
                        clusterEParametrosProjection,
                        false));
        IllegalArgumentException unidadeMedidaAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        calendario,
                        materialProjection,
                        locationProjection,
                        salesProjection,
                        parametrosGeraisSemUnidadeMedida,
                        getCommunityParametrosForecastProjection(),
                        clusterEParametrosProjection,
                        false));

        Assertions.assertEquals(
                "Demand Planning forecast aggregation parameters are required for Demand Planning forecast projection generation.",
                parametrosAgregacaoAusentesException.getMessage());
        Assertions.assertEquals(
                "Demand Planning default UOM is required for Demand Planning forecast projection generation.",
                unidadeMedidaAusenteException.getMessage());

    }

    @Test
    public void executaDemandPlanningShouldRejectReferencePlanBeforeLoadingProfile() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        setField(
                demandPlanningService,
                "demandPlanReferenceCopySpi",
                new CommunityDemandPlanReferenceCopySpi());

        /*
         * Reference Plan e copia de horizonte congelado sao Enterprise. O
         * bloqueio precisa acontecer antes de carregar perfil, salvar plano ou
         * consultar projections, permitindo que um payload manual falhe cedo.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningService.executaDemandPlanning(
                        "PROFILE",
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        "Demand Plan",
                        101L,
                        true,
                        "user"));

    }

    @Test
    public void executaDemandPlanningShouldRejectFrozenHorizonCopyBeforeLoadingProfile() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        setField(
                demandPlanningService,
                "demandPlanReferenceCopySpi",
                new CommunityDemandPlanReferenceCopySpi());

        /*
         * A flag de copia no horizonte congelado tambem pertence ao Reference
         * Plan Enterprise. Mesmo sem id de plano de referencia, o Community
         * deve falhar antes de carregar perfil/repository.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningService.executaDemandPlanning(
                        "PROFILE",
                        "2026.01",
                        "Demand Plan",
                        null,
                        true,
                        "user"));

    }

    @Test
    public void deleteDemandPlanShouldRejectBrokenAssociatedSupplyPlansBeforePartialDeletion() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        List<SupplyPlan> supplyPlansAssociadosAoDemandPlan = new ArrayList<>();
        supplyPlansAssociadosAoDemandPlan.add(new SupplyPlan());
        supplyPlansAssociadosAoDemandPlan.add(null);
        setField(
                demandPlanningService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComAssociadosParaExclusao(
                        supplyPlansAssociadosAoDemandPlan));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.deleteDemandPlan(42L));

        /*
         * A exclusao deve parar antes de remover historico/linhas quando a
         * fotografia dos Supply Plans associados vem quebrada do repository.
         */
        Assertions.assertEquals(
                "Associated Supply Plan at index 1 is required for Community Demand Plan deletion.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void deleteDemandPlanShouldRejectMissingAssociatedSupplyPlanCollectionBeforePartialDeletion() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        setField(
                demandPlanningService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComAssociadosParaExclusao(null));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.deleteDemandPlan(42L));

        Assertions.assertEquals(
                "Associated Supply Plan collection is required for Community Demand Plan deletion.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void deleteDemandPlanShouldRejectBrokenAssociatedDemandPlansBeforeDeletingVersion() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        List<DemandPlan> demandPlansAssociadosAoDemandPlan = new ArrayList<>();
        demandPlansAssociadosAoDemandPlan.add(new DemandPlan());
        demandPlansAssociadosAoDemandPlan.add(null);
        setField(
                demandPlanningService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComAssociadosParaExclusao(List.of()));
        setField(
                demandPlanningService,
                "historicoDemandPlanItemRepository",
                getHistoricoDemandPlanItemRepositoryParaExclusao());
        setField(
                demandPlanningService,
                "demandPlanItemRepository",
                getDemandPlanItemRepositoryParaExclusao());
        setField(
                demandPlanningService,
                "demandPlanRepository",
                getDemandPlanRepositoryComAssociadosParaExclusao(
                        demandPlansAssociadosAoDemandPlan));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.deleteDemandPlan(42L));

        /*
         * A remocao da versao so pode acontecer depois de limpar com seguranca
         * as referencias de horizonte congelado. Item nulo deve falhar antes de
         * `saveAll`/`deleteById`.
         */
        Assertions.assertEquals(
                "Associated Demand Plan at index 1 is required for Community Demand Plan deletion.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void deleteDemandPlanShouldRejectMissingAssociatedDemandPlanCollectionBeforeDeletingVersion() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        setField(
                demandPlanningService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComAssociadosParaExclusao(List.of()));
        setField(
                demandPlanningService,
                "historicoDemandPlanItemRepository",
                getHistoricoDemandPlanItemRepositoryParaExclusao());
        setField(
                demandPlanningService,
                "demandPlanItemRepository",
                getDemandPlanItemRepositoryParaExclusao());
        setField(
                demandPlanningService,
                "demandPlanRepository",
                getDemandPlanRepositoryComAssociadosParaExclusao(null));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.deleteDemandPlan(42L));

        Assertions.assertEquals(
                "Associated Demand Plan collection is required for Community Demand Plan deletion.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void deleteDemandPlanSavedAssociationSnapshotsShouldRejectBrokenCollections() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();

        InvocationTargetException supplyPlanCollectionException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSupplyPlansAssociadosSalvosParaExclusaoCommunity(
                        demandPlanningService,
                        null));
        InvocationTargetException supplyPlanItemException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSupplyPlansAssociadosSalvosParaExclusaoCommunity(
                        demandPlanningService,
                        Arrays.asList((SupplyPlan) null)));
        InvocationTargetException supplyPlanPartialSaveException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSupplyPlansAssociadosSalvosParaExclusaoCommunity(
                        demandPlanningService,
                        List.of(new SupplyPlan()),
                        2));
        InvocationTargetException demandPlanCollectionException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDemandPlansAssociadosSalvosParaExclusaoCommunity(
                        demandPlanningService,
                        null));
        InvocationTargetException demandPlanItemException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDemandPlansAssociadosSalvosParaExclusaoCommunity(
                        demandPlanningService,
                        Arrays.asList((DemandPlan) null)));
        InvocationTargetException demandPlanPartialSaveException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDemandPlansAssociadosSalvosParaExclusaoCommunity(
                        demandPlanningService,
                        List.of(new DemandPlan()),
                        2));

        Assertions.assertEquals(
                "Saved associated Supply Plan collection is required for Community Demand Plan deletion.",
                supplyPlanCollectionException.getCause().getMessage());
        Assertions.assertEquals(
                "Saved associated Supply Plan at index 0 is required for Community Demand Plan deletion.",
                supplyPlanItemException.getCause().getMessage());
        Assertions.assertEquals(
                "Saved associated Supply Plan collection size 1 does not match expected size 2 for Community Demand Plan deletion.",
                supplyPlanPartialSaveException.getCause().getMessage());
        Assertions.assertEquals(
                "Saved associated Demand Plan collection is required for Community Demand Plan deletion.",
                demandPlanCollectionException.getCause().getMessage());
        Assertions.assertEquals(
                "Saved associated Demand Plan at index 0 is required for Community Demand Plan deletion.",
                demandPlanItemException.getCause().getMessage());
        Assertions.assertEquals(
                "Saved associated Demand Plan collection size 1 does not match expected size 2 for Community Demand Plan deletion.",
                demandPlanPartialSaveException.getCause().getMessage());

    }

    @Test
    public void savedDemandPlanItemSnapshotShouldRejectBrokenCollections() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();

        InvocationTargetException collectionException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDemandPlanItemsSalvasCommunity(
                        demandPlanningService,
                        null));
        InvocationTargetException itemException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDemandPlanItemsSalvasCommunity(
                        demandPlanningService,
                        Arrays.asList((DemandPlanItem) null)));
        InvocationTargetException partialSaveException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDemandPlanItemsSalvasCommunity(
                        demandPlanningService,
                        List.of(criaDemandPlanItemSalvaParaTeste()),
                        2));

        Assertions.assertEquals(
                "Saved Demand Plan line collection is required for Community Demand Planning persistence.",
                collectionException.getCause().getMessage());
        Assertions.assertEquals(
                "Saved Demand Plan line at index 0 is required for Community Demand Planning persistence.",
                itemException.getCause().getMessage());
        Assertions.assertEquals(
                "Saved Demand Plan line collection size 1 does not match expected size 2 for Community Demand Planning persistence.",
                partialSaveException.getCause().getMessage());

    }

    @Test
    public void validaDemandPlanSalvoInicialCommunityShouldRejectBrokenSavedSnapshots() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        DemandPlan demandPlanValido = criaDemandPlanSalvoInicialParaTeste();

        Assertions.assertSame(
                demandPlanValido,
                invokeValidaDemandPlanSalvoInicialCommunity(
                        demandPlanningService,
                        demandPlanValido));

        assertIllegalStateExceptionMessage(
                () -> invokeValidaDemandPlanSalvoInicialCommunity(
                        demandPlanningService,
                        null),
                "Saved Demand Plan snapshot is required before Community Demand Planning execution.");

        DemandPlan demandPlanSemId = criaDemandPlanSalvoInicialParaTeste();
        demandPlanSemId.setId(null);
        assertIllegalStateExceptionMessage(
                () -> invokeValidaDemandPlanSalvoInicialCommunity(
                        demandPlanningService,
                        demandPlanSemId),
                "Saved Demand Plan snapshot has no id before Community Demand Planning execution.");

        DemandPlan demandPlanSemPerfilExecucao = criaDemandPlanSalvoInicialParaTeste();
        demandPlanSemPerfilExecucao.setPerfilExecucaoDemandPlan(null);
        assertIllegalStateExceptionMessage(
                () -> invokeValidaDemandPlanSalvoInicialCommunity(
                        demandPlanningService,
                        demandPlanSemPerfilExecucao),
                "Saved Demand Plan snapshot has no execution profile before Community Demand Planning execution.");

        DemandPlan demandPlanSemHorarioGeracao = criaDemandPlanSalvoInicialParaTeste();
        demandPlanSemHorarioGeracao.setHorarioGeracao(null);
        assertIllegalStateExceptionMessage(
                () -> invokeValidaDemandPlanSalvoInicialCommunity(
                        demandPlanningService,
                        demandPlanSemHorarioGeracao),
                "Saved Demand Plan snapshot has no generation timestamp before Community Demand Planning execution.");

        DemandPlan demandPlanSemDataInicioPlano = criaDemandPlanSalvoInicialParaTeste();
        demandPlanSemDataInicioPlano.setDataInicioPlano(null);
        assertIllegalStateExceptionMessage(
                () -> invokeValidaDemandPlanSalvoInicialCommunity(
                        demandPlanningService,
                        demandPlanSemDataInicioPlano),
                "Saved Demand Plan snapshot has no plan start date before Community Demand Planning execution.");

        DemandPlan demandPlanSemDataFimPlano = criaDemandPlanSalvoInicialParaTeste();
        demandPlanSemDataFimPlano.setDataFimPlano(null);
        assertIllegalStateExceptionMessage(
                () -> invokeValidaDemandPlanSalvoInicialCommunity(
                        demandPlanningService,
                        demandPlanSemDataFimPlano),
                "Saved Demand Plan snapshot has no plan end date before Community Demand Planning execution.");

        /*
         * DemandPlan#getTamanhoBucket aplica fallback mensal para compatibilidade
         * historica. A validacao do snapshot salvo precisa olhar o valor bruto
         * para nao gerar linhas em bucket default quando o save perdeu o campo.
         */
        DemandPlan demandPlanSemTamanhoBucket = criaDemandPlanSalvoInicialParaTeste();
        demandPlanSemTamanhoBucket.setTamanhoBucket(null);
        assertIllegalStateExceptionMessage(
                () -> invokeValidaDemandPlanSalvoInicialCommunity(
                        demandPlanningService,
                        demandPlanSemTamanhoBucket),
                "Saved Demand Plan snapshot has no bucket size before Community Demand Planning execution.");

    }

    @Test
    public void aplicaJanelaEdicaoPlanningBookCommunityShouldPersistStartAndEndDates() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("PROFILE");
        perfilExecucaoDemandPlan.setTamanhoBucket(Constantes.TamanhoBucket.DIARIO);
        perfilExecucaoDemandPlan.setRestringePeriodosEdicaoPlano(true);
        perfilExecucaoDemandPlan.setPeriodoInicialEdicaoPlano(2);
        perfilExecucaoDemandPlan.setPeriodoFinalEdicaoPlano(4);

        invokeAplicaJanelaEdicaoPlanningBookCommunity(
                demandPlanningService,
                demandPlan,
                perfilExecucaoDemandPlan);

        Assertions.assertEquals(
                LocalDate.of(2026, 1, 2),
                demandPlan.getDataInicioEdicao());
        Assertions.assertEquals(
                LocalDate.of(2026, 1, 4),
                demandPlan.getDataFimEdicao());

    }

    @Test
    public void resetPlanoRestritoShouldNeutralizeEnterpriseKeyFigures() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        CapturingJdbcTemplate capturingJdbcTemplate = new CapturingJdbcTemplate();

        setField(demandPlanningService, "jdbcTemplate", capturingJdbcTemplate);

        demandPlanningService.resetPlanoRestrito(202L);

        Assertions.assertEquals(1, capturingJdbcTemplate.sqlStatements.size());

        String resetSql = capturingJdbcTemplate.sqlStatements.get(0);
        Assertions.assertTrue(resetSql.contains("quantidade_baseline_atendida = quantidade_baseline"));
        Assertions.assertTrue(resetSql.contains("quantidade_itens_novos = 0"));
        Assertions.assertTrue(resetSql.contains("quantidade_uplift = 0"));
        Assertions.assertTrue(resetSql.contains("quantidade_itens_novos_atendida = 0"));
        Assertions.assertTrue(resetSql.contains("quantidade_uplift_atendida = 0"));
        Assertions.assertFalse(resetSql.contains("quantidade_itens_novos_atendida = quantidade_itens_novos"));
        Assertions.assertFalse(resetSql.contains("quantidade_uplift_atendida = quantidade_uplift"));

    }

    @Test
    public void saveDemandPlanDePlanningProjectionShouldNeutralizeEnterpriseKeyFiguresBeforeSaveAll() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        CapturingDemandPlanItemRepositoryInvocationHandler capturingDemandPlanItemRepositoryInvocationHandler =
                new CapturingDemandPlanItemRepositoryInvocationHandler();
        setField(
                demandPlanningService,
                "demandPlanItemRepository",
                capturingDemandPlanItemRepositoryInvocationHandler.getProxy());

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                0,
                1,
                0);
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setTamanhoBucket(Constantes.TamanhoBucket.DIARIO);
        demandPlan.setDataInicioPlano(calendario.getPrimeiraDataHorarioPeriodo(0));

        DemandPlanningProjection demandPlanningProjection = new DemandPlanningProjection(
                demandPlan,
                new UnidadeMedidaProjection(),
                null,
                null,
                calendario,
                null,
                false,
                null,
                null);

        DemandPlanItem demandPlanItem = new DemandPlanItem(
                new DemandPlanItem.DemandPlanItemKey(
                        demandPlan,
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        calendario.getUltimoSegundoPeriodo(0)));
        demandPlanItem.setUnidadeMedida(unidadeMedida);
        demandPlanItem.setQuantidadeBaseline(10.0);
        demandPlanItem.setQuantidadeAjusteDemanda(2.0);
        demandPlanItem.setQuantidadeItensNovos(7.0);
        demandPlanItem.setQuantidadeItensNovosAtendida(3.0);
        demandPlanItem.setQuantidadeUplift(5.0);
        demandPlanItem.setQuantidadeUpliftAtendida(4.0);
        demandPlanningProjection.addDemandPlanItem(demandPlanItem);

        demandPlanningService.saveDemandPlanDePlanningProjection(demandPlanningProjection);

        Assertions.assertEquals(1, capturingDemandPlanItemRepositoryInvocationHandler.savedDemandPlanItems.size());
        DemandPlanItem demandPlanItemSalva = capturingDemandPlanItemRepositoryInvocationHandler.savedDemandPlanItems.get(0);
        Assertions.assertEquals(10.0, demandPlanItemSalva.getQuantidadeBaseline(), 0.0001d);
        Assertions.assertEquals(2.0, demandPlanItemSalva.getQuantidadeAjusteDemanda(), 0.0001d);
        Assertions.assertEquals(0.0, demandPlanItemSalva.getQuantidadeItensNovos(), 0.0001d);
        Assertions.assertEquals(0.0, demandPlanItemSalva.getQuantidadeItensNovosAtendida(), 0.0001d);
        Assertions.assertEquals(0.0, demandPlanItemSalva.getQuantidadeUplift(), 0.0001d);
        Assertions.assertEquals(0.0, demandPlanItemSalva.getQuantidadeUpliftAtendida(), 0.0001d);

    }

    @Test
    public void saveDemandPlanDePlanningProjectionShouldRejectMissingProjectionBeforeRepository() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        CapturingDemandPlanItemRepositoryInvocationHandler capturingDemandPlanItemRepositoryInvocationHandler =
                new CapturingDemandPlanItemRepositoryInvocationHandler();
        setField(
                demandPlanningService,
                "demandPlanItemRepository",
                capturingDemandPlanItemRepositoryInvocationHandler.getProxy());

        /*
         * A projection e o snapshot calculado que sera persistido. Ausencia da
         * projection e erro de contrato do caller e deve falhar antes de tentar
         * salvar uma lista vazia ou acessar repository.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.saveDemandPlanDePlanningProjection(null));

        Assertions.assertEquals(
                "Demand Planning projection is required to save Demand Plan lines.",
                illegalArgumentException.getMessage());
        Assertions.assertTrue(
                capturingDemandPlanItemRepositoryInvocationHandler.savedDemandPlanItems.isEmpty());

    }

    @Test
    public void saveDemandPlanDePlanningProjectionShouldRejectBrokenLineCollectionBeforeRepository() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        CapturingDemandPlanItemRepositoryInvocationHandler capturingDemandPlanItemRepositoryInvocationHandler =
                new CapturingDemandPlanItemRepositoryInvocationHandler();
        setField(
                demandPlanningService,
                "demandPlanItemRepository",
                capturingDemandPlanItemRepositoryInvocationHandler.getProxy());

        IllegalArgumentException colecaoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.saveDemandPlanDePlanningProjection(
                        new BrokenDemandPlanningProjection(null)));

        Set<DemandPlanItem> demandPlanItemsComItemNulo = new LinkedHashSet<>();
        demandPlanItemsComItemNulo.add(new DemandPlanItem());
        demandPlanItemsComItemNulo.add(null);

        IllegalArgumentException itemAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.saveDemandPlanDePlanningProjection(
                        new BrokenDemandPlanningProjection(demandPlanItemsComItemNulo)));

        /*
         * A borda de persistencia deve diferenciar snapshot vazio de snapshot
         * quebrado. Nulo indica projection inconsistente e nao pode cair em NPE
         * durante neutralizacao de Uplift/New Materials.
         */
        Assertions.assertEquals(
                "Demand Plan line collection is required to save Demand Plan lines.",
                colecaoAusenteException.getMessage());
        Assertions.assertEquals(
                "Demand Plan line at index 1 is required to save Demand Plan lines.",
                itemAusenteException.getMessage());
        Assertions.assertTrue(
                capturingDemandPlanItemRepositoryInvocationHandler.savedDemandPlanItems.isEmpty());

    }

    @Test
    public void saveDemandPlanDePlanningProjectionShouldRejectDuplicatedFilteredKeysBeforeRepository() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        CapturingDemandPlanItemRepositoryInvocationHandler capturingDemandPlanItemRepositoryInvocationHandler =
                new CapturingDemandPlanItemRepositoryInvocationHandler();
        setField(
                demandPlanningService,
                "demandPlanItemRepository",
                capturingDemandPlanItemRepositoryInvocationHandler.getProxy());

        DemandPlanItem demandPlanItem = criaDemandPlanItemSalvaParaTeste();
        demandPlanItem.setQuantidadeBaseline(10.0d);
        BrokenDemandPlanningProjection demandPlanningProjection =
                new BrokenDemandPlanningProjection(
                        new DuplicatedDemandPlanItemSet(demandPlanItem));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.saveDemandPlanDePlanningProjection(
                        demandPlanningProjection));

        /*
         * A projection real usa mapas e ja rejeita duplicidade, mas a borda de
         * persistencia ainda precisa se proteger de snapshots transicionais ou
         * mocks que exponham cardinalidade quebrada. A falha deve acontecer
         * antes do repository, nao por dedupe implicito em `Set`.
         */
        Assertions.assertTrue(
                illegalArgumentException.getMessage()
                        .contains("Demand Plan line at index 1 has duplicated Community Demand Planning key for persistence"));
        Assertions.assertTrue(
                capturingDemandPlanItemRepositoryInvocationHandler.savedDemandPlanItems.isEmpty());

        DemandPlanningService demandPlanningServicePorPeriodo = new DemandPlanningService();
        CapturingDemandPlanItemRepositoryInvocationHandler capturingRepositoryPorPeriodo =
                new CapturingDemandPlanItemRepositoryInvocationHandler();
        setField(
                demandPlanningServicePorPeriodo,
                "demandPlanItemRepository",
                capturingRepositoryPorPeriodo.getProxy());

        IllegalArgumentException illegalArgumentExceptionPorPeriodo = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningServicePorPeriodo.saveDemandPlanDePlanningProjection(
                        demandPlanningProjection,
                        0));

        Assertions.assertTrue(
                illegalArgumentExceptionPorPeriodo.getMessage()
                        .contains("Demand Plan line at index 1 has duplicated Community Demand Planning key for persistence"));
        Assertions.assertTrue(
                capturingRepositoryPorPeriodo.savedDemandPlanItems.isEmpty());

    }

    @Test
    public void saveDemandPlanDePlanningProjectionForSinglePeriodShouldRejectMissingProjectionBeforeRepository() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        CapturingDemandPlanItemRepositoryInvocationHandler capturingDemandPlanItemRepositoryInvocationHandler =
                new CapturingDemandPlanItemRepositoryInvocationHandler();
        setField(
                demandPlanningService,
                "demandPlanItemRepository",
                capturingDemandPlanItemRepositoryInvocationHandler.getProxy());

        /*
         * A sobrecarga por periodo e usada por fluxos incrementais, mas o
         * contrato e o mesmo: sem projection nao ha snapshot material/location
         * para neutralizar KFs Enterprise nem persistir.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.saveDemandPlanDePlanningProjection(null, 0));

        Assertions.assertEquals(
                "Demand Planning projection is required to save Demand Plan lines.",
                illegalArgumentException.getMessage());
        Assertions.assertTrue(
                capturingDemandPlanItemRepositoryInvocationHandler.savedDemandPlanItems.isEmpty());

    }

    @Test
    public void saveDemandPlanDePlanningProjectionForSinglePeriodShouldRejectBrokenLineCollectionBeforeRepository() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        CapturingDemandPlanItemRepositoryInvocationHandler capturingDemandPlanItemRepositoryInvocationHandler =
                new CapturingDemandPlanItemRepositoryInvocationHandler();
        setField(
                demandPlanningService,
                "demandPlanItemRepository",
                capturingDemandPlanItemRepositoryInvocationHandler.getProxy());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.saveDemandPlanDePlanningProjection(
                        new BrokenDemandPlanningProjection(null),
                        2));

        Assertions.assertEquals(
                "Demand Plan line collection is required to save Demand Plan lines.",
                illegalArgumentException.getMessage());
        Assertions.assertTrue(
                capturingDemandPlanItemRepositoryInvocationHandler.savedDemandPlanItems.isEmpty());

    }

    @Test
    public void generatedDemandPlanItemsShouldRejectBrokenCollectionsBeforeInitialSave() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        List<DemandPlanItem> demandPlanItemsGeradasComItemNulo = new ArrayList<>();
        demandPlanItemsGeradasComItemNulo.add(new DemandPlanItem());
        demandPlanItemsGeradasComItemNulo.add(null);

        InvocationTargetException colecaoAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetDemandPlanItemsGeradasParaPersistenciaCommunity(
                        demandPlanningService,
                        null));
        InvocationTargetException itemAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeGetDemandPlanItemsGeradasParaPersistenciaCommunity(
                        demandPlanningService,
                        demandPlanItemsGeradasComItemNulo));

        /*
         * A geracao inicial do Demand Plan tambem deve falhar com mensagem
         * funcional se a rotina estatistica devolver uma fotografia quebrada.
         */
        Assertions.assertEquals(
                "Generated Demand Plan line collection is required for Community Demand Planning persistence.",
                colecaoAusenteException.getCause().getMessage());
        Assertions.assertEquals(
                "Generated Demand Plan line at index 1 is required for Community Demand Planning persistence.",
                itemAusenteException.getCause().getMessage());

    }

    @Test
    public void geraDemandPlanForecastProjectionsExecucaoComForecastShouldAcceptBottomUpMaterialLocationExecution() {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        Assertions.assertDoesNotThrow(
                () -> setField(
                        demandPlanningService,
                        "demandForecastWorkflowService",
                        criaDemandForecastWorkflowService()));
        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);

        for (int periodo = calendario.getPosicaoPeriodoInicialPassado();
             periodo <= calendario.getPosicaoPeriodoFinalPassado();
            periodo++) {
            demandPlanForecastProjectionMaterialLocation.demanda[periodo] = 10.0d;
            demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts[periodo] = 10.0d;
            demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[periodo] = 10.0d;
        }

        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                new ParametrosGeraisDemandPlanningProjection(
                        true,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.BOTTOM_UP,
                                Constantes.DPNivelAgregacao.BOTTOM_UP),
                        3,
                        false,
                        false,
                        0,
                        unidadeMedida,
                        false,
                        null);
        ParametrosForecastProjection parametrosForecastProjection = new ParametrosForecastProjection(
                Constantes.DPModeloEstatistico.MM,
                new ParametrosMediaMovel(1),
                null,
                null,
                null,
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                1);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionList =
                demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        calendario,
                        null,
                        null,
                        parametrosGeraisDemandPlanningProjection,
                        parametrosForecastProjection,
                        null,
                        false);

        Assertions.assertEquals(1, demandPlanForecastProjectionList.size());
        Assertions.assertSame(
                demandPlanForecastProjectionMaterialLocation,
                demandPlanForecastProjectionList.get(0));
        Assertions.assertEquals(
                10.0d,
                demandPlanForecastProjectionMaterialLocation.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);

    }

    @Test
    public void geraForecastFromMaterialLocationListShouldRejectBrokenCollectionBeforeAggregation() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        Assertions.assertDoesNotThrow(
                () -> setField(
                        demandPlanningService,
                        "demandForecastWorkflowService",
                        criaDemandForecastWorkflowService()));
        Calendario calendario = getCalendarioForecastTeste();
        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                getParametrosGeraisDemandPlanningProjectionBottomUp();
        List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationListComItemNulo =
                new ArrayList<>();
        demandPlanForecastProjectionMaterialLocationListComItemNulo.add(
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false));
        demandPlanForecastProjectionMaterialLocationListComItemNulo.add(null);

        /*
         * Lista vazia e valida, mas uma factory/caller que devolve lista nula ou
         * item nulo deve falhar antes da rotina de agregacao. Isso preserva a
         * rodada paralela por cluster sem deixar um snapshot quebrado aparecer
         * como cluster sem DFUs.
         */
        IllegalArgumentException colecaoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        null,
                        calendario,
                        null,
                        null,
                        parametrosGeraisDemandPlanningProjection,
                        getCommunityParametrosForecastProjection(),
                        null,
                        false));
        IllegalArgumentException itemAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        demandPlanForecastProjectionMaterialLocationListComItemNulo,
                        calendario,
                        null,
                        null,
                        parametrosGeraisDemandPlanningProjection,
                        getCommunityParametrosForecastProjection(),
                        null,
                        false));

        Assertions.assertEquals(
                "Demand Planning material/location forecast projection collection is required for forecast projection generation.",
                colecaoAusenteException.getMessage());
        Assertions.assertEquals(
                "Demand Planning material/location forecast projection at index 1 is required for forecast projection generation.",
                itemAusenteException.getMessage());

    }

    @Test
    public void geraForecastFromMaterialLocationListShouldRejectMissingAggregationBeforeAggregationRoutine() throws Exception {

        DemandPlanningService demandPlanningService = new DemandPlanningService();
        Assertions.assertDoesNotThrow(
                () -> setField(
                        demandPlanningService,
                        "demandForecastWorkflowService",
                        criaDemandForecastWorkflowService()));
        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                getParametrosGeraisDemandPlanningProjectionBottomUp();
        parametrosGeraisDemandPlanningProjection.setParametrosAgregacaoForecast(null);

        /*
         * Mesmo com lista vazia, os parametros de agregacao sao obrigatorios:
         * eles definem se a unidade de execucao e material/location ou agregada.
         * Sem eles, retornar lista vazia silenciosamente esconderia configuracao
         * quebrada.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningService.geraDemandPlanForecastProjectionsExecucaoComForecast(
                        List.of(),
                        getCalendarioForecastTeste(),
                        null,
                        null,
                        parametrosGeraisDemandPlanningProjection,
                        getCommunityParametrosForecastProjection(),
                        null,
                        false));

        Assertions.assertEquals(
                "Demand Planning forecast aggregation parameters are required for Demand Planning forecast projection generation.",
                illegalArgumentException.getMessage());

    }

    private static PerfilExecucaoDemandPlanRepository getPerfilExecucaoDemandPlanRepositoryVazio() {

        return (PerfilExecucaoDemandPlanRepository) Proxy.newProxyInstance(
                PerfilExecucaoDemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{PerfilExecucaoDemandPlanRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("toString".equals(method.getName())) {
                        return "PerfilExecucaoDemandPlanRepository vazio para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static PerfilExecucaoDemandPlanRepository getPerfilExecucaoDemandPlanRepositoryRetornandoOptionalNulo() {

        return (PerfilExecucaoDemandPlanRepository) Proxy.newProxyInstance(
                PerfilExecucaoDemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{PerfilExecucaoDemandPlanRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "PerfilExecucaoDemandPlanRepository retornando Optional nulo para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static DemandPlanRepository getDemandPlanRepositoryVazio() {

        return (DemandPlanRepository) Proxy.newProxyInstance(
                DemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{DemandPlanRepository.class},
                (proxy, method, args) -> {
                    if ("customFindByIdComPerfilExecucao".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("toString".equals(method.getName())) {
                        return "DemandPlanRepository vazio para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static DemandPlanRepository getDemandPlanRepositoryRetornandoOptionalNulo() {

        return (DemandPlanRepository) Proxy.newProxyInstance(
                DemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{DemandPlanRepository.class},
                (proxy, method, args) -> {
                    if ("customFindByIdComPerfilExecucao".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DemandPlanRepository retornando Optional nulo para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static SupplyPlanRepository getSupplyPlanRepositoryComAssociadosParaExclusao(
            List<SupplyPlan> supplyPlansAssociadosAoDemandPlan) {

        return (SupplyPlanRepository) Proxy.newProxyInstance(
                SupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{SupplyPlanRepository.class},
                (proxy, method, args) -> {
                    if ("findByDemandPlanId".equals(method.getName())) {
                        return supplyPlansAssociadosAoDemandPlan;
                    }
                    if ("saveAll".equals(method.getName())) {
                        return args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "SupplyPlanRepository para teste de exclusao Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static HistoricoDemandPlanItemRepository getHistoricoDemandPlanItemRepositoryParaExclusao() {

        return (HistoricoDemandPlanItemRepository) Proxy.newProxyInstance(
                HistoricoDemandPlanItemRepository.class.getClassLoader(),
                new Class<?>[]{HistoricoDemandPlanItemRepository.class},
                (proxy, method, args) -> {
                    if ("deleteByKeyDemandPlanId".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "HistoricoDemandPlanItemRepository para teste de exclusao Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static DemandPlanItemRepository getDemandPlanItemRepositoryParaExclusao() {

        return (DemandPlanItemRepository) Proxy.newProxyInstance(
                DemandPlanItemRepository.class.getClassLoader(),
                new Class<?>[]{DemandPlanItemRepository.class},
                (proxy, method, args) -> {
                    if ("removeByKeyDemandPlanId".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DemandPlanItemRepository para teste de exclusao Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static DemandPlanRepository getDemandPlanRepositoryComAssociadosParaExclusao(
            List<DemandPlan> demandPlansAssociadosAoDemandPlan) {

        return (DemandPlanRepository) Proxy.newProxyInstance(
                DemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{DemandPlanRepository.class},
                (proxy, method, args) -> {
                    if ("findByDemandPlanCopiadoNoHorizonteCongeladoId".equals(method.getName())) {
                        return demandPlansAssociadosAoDemandPlan;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DemandPlanRepository para teste de exclusao Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static PerfilExecucaoDemandPlan invokeGetPerfilExecucaoDemandPlanObrigatorio(
            DemandPlanningService demandPlanningService,
            String perfilExecucaoDemandPlanId) throws Exception {

        Method method = DemandPlanningService.class.getDeclaredMethod(
                "getPerfilExecucaoDemandPlanObrigatorio",
                String.class);
        method.setAccessible(true);
        return (PerfilExecucaoDemandPlan) method.invoke(
                demandPlanningService,
                perfilExecucaoDemandPlanId);

    }

    private static DemandPlan invokeGetDemandPlanGeradoComPerfilExecucaoObrigatorio(
            DemandPlanningService demandPlanningService,
            Long demandPlanId) throws Exception {

        Method method = DemandPlanningService.class.getDeclaredMethod(
                "getDemandPlanGeradoComPerfilExecucaoObrigatorio",
                Long.class);
        method.setAccessible(true);
        return (DemandPlan) method.invoke(
                demandPlanningService,
                demandPlanId);

    }

    private static DemandPlan invokeValidaDemandPlanSalvoInicialCommunity(
            DemandPlanningService demandPlanningService,
            DemandPlan demandPlanSalvo) throws Exception {

        Method method = DemandPlanningService.class.getDeclaredMethod(
                "validaDemandPlanSalvoInicialCommunity",
                DemandPlan.class);
        method.setAccessible(true);
        return (DemandPlan) method.invoke(
                demandPlanningService,
                demandPlanSalvo);

    }

    private static void invokeValidaSupplyPlansAssociadosSalvosParaExclusaoCommunity(
            DemandPlanningService demandPlanningService,
            List<SupplyPlan> supplyPlansAssociadosAoDemandPlanSalvos) throws Exception {

        Method method = DemandPlanningService.class.getDeclaredMethod(
                "validaSupplyPlansAssociadosSalvosParaExclusaoCommunity",
                List.class);
        method.setAccessible(true);
        method.invoke(
                demandPlanningService,
                supplyPlansAssociadosAoDemandPlanSalvos);

    }

    private static void invokeValidaSupplyPlansAssociadosSalvosParaExclusaoCommunity(
            DemandPlanningService demandPlanningService,
            List<SupplyPlan> supplyPlansAssociadosAoDemandPlanSalvos,
            Integer numeroSupplyPlansAssociadosAoDemandPlanEsperado) throws Exception {

        Method method = DemandPlanningService.class.getDeclaredMethod(
                "validaSupplyPlansAssociadosSalvosParaExclusaoCommunity",
                List.class,
                Integer.class);
        method.setAccessible(true);
        method.invoke(
                demandPlanningService,
                supplyPlansAssociadosAoDemandPlanSalvos,
                numeroSupplyPlansAssociadosAoDemandPlanEsperado);

    }

    private static void invokeValidaDemandPlansAssociadosSalvosParaExclusaoCommunity(
            DemandPlanningService demandPlanningService,
            List<DemandPlan> demandPlansAssociadosAoDemandPlanSalvos) throws Exception {

        Method method = DemandPlanningService.class.getDeclaredMethod(
                "validaDemandPlansAssociadosSalvosParaExclusaoCommunity",
                List.class);
        method.setAccessible(true);
        method.invoke(
                demandPlanningService,
                demandPlansAssociadosAoDemandPlanSalvos);

    }

    private static void invokeValidaDemandPlansAssociadosSalvosParaExclusaoCommunity(
            DemandPlanningService demandPlanningService,
            List<DemandPlan> demandPlansAssociadosAoDemandPlanSalvos,
            Integer numeroDemandPlansAssociadosAoDemandPlanEsperado) throws Exception {

        Method method = DemandPlanningService.class.getDeclaredMethod(
                "validaDemandPlansAssociadosSalvosParaExclusaoCommunity",
                List.class,
                Integer.class);
        method.setAccessible(true);
        method.invoke(
                demandPlanningService,
                demandPlansAssociadosAoDemandPlanSalvos,
                numeroDemandPlansAssociadosAoDemandPlanEsperado);

    }

    private static void invokeValidaDemandPlanItemsSalvasCommunity(
            DemandPlanningService demandPlanningService,
            List<DemandPlanItem> demandPlanItemsSalvas) throws Exception {

        Method method = DemandPlanningService.class.getDeclaredMethod(
                "validaDemandPlanItemsSalvasCommunity",
                java.util.Collection.class);
        method.setAccessible(true);
        method.invoke(
                demandPlanningService,
                demandPlanItemsSalvas);

    }

    private static void invokeValidaDemandPlanItemsSalvasCommunity(
            DemandPlanningService demandPlanningService,
            List<DemandPlanItem> demandPlanItemsSalvas,
            Integer numeroDemandPlanItemsEsperado) throws Exception {

        Method method = DemandPlanningService.class.getDeclaredMethod(
                "validaDemandPlanItemsSalvasCommunity",
                java.util.Collection.class,
                Integer.class);
        method.setAccessible(true);
        method.invoke(
                demandPlanningService,
                demandPlanItemsSalvas,
                numeroDemandPlanItemsEsperado);

    }

    private static void assertAutowiredFields(String... fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = DemandPlanningService.class.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    "DemandPlanningService." + fieldName + " deve usar @Autowired explicito");
            Assertions.assertTrue(
                    autowired.required(),
                    "DemandPlanningService." + fieldName + " deve ser bean obrigatorio");
        }

    }

    private static void assertNoResultExceptionMessage(
            ThrowingReflectionCall throwingReflectionCall,
            String expectedMessage) {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                throwingReflectionCall::execute);

        Assertions.assertInstanceOf(
                NoResultException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                expectedMessage,
                invocationTargetException.getCause().getMessage());

    }

    private static void assertIllegalStateExceptionMessage(
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

    private static void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static void invokeAplicaJanelaEdicaoPlanningBookCommunity(
            DemandPlanningService demandPlanningService,
            DemandPlan demandPlan,
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan) throws Exception {

        Method aplicaJanelaEdicaoPlanningBookCommunityMethod = DemandPlanningService.class.getDeclaredMethod(
                "aplicaJanelaEdicaoPlanningBookCommunity",
                DemandPlan.class,
                PerfilExecucaoDemandPlan.class);
        aplicaJanelaEdicaoPlanningBookCommunityMethod.setAccessible(true);
        aplicaJanelaEdicaoPlanningBookCommunityMethod.invoke(
                demandPlanningService,
                demandPlan,
                perfilExecucaoDemandPlan);

    }

    @SuppressWarnings("unchecked")
    private static List<DemandPlanItem> invokeGetDemandPlanItemsGeradasParaPersistenciaCommunity(
            DemandPlanningService demandPlanningService,
            List<DemandPlanItem> demandPlanItemsGeradas) throws Exception {

        Method getDemandPlanItemsGeradasParaPersistenciaCommunityMethod =
                DemandPlanningService.class.getDeclaredMethod(
                        "getDemandPlanItemsGeradasParaPersistenciaCommunity",
                        List.class);
        getDemandPlanItemsGeradasParaPersistenciaCommunityMethod.setAccessible(true);
        return (List<DemandPlanItem>) getDemandPlanItemsGeradasParaPersistenciaCommunityMethod.invoke(
                demandPlanningService,
                demandPlanItemsGeradas);

    }

    private static DemandPlan criaDemandPlanSalvoInicialParaTeste() {

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setId(42L);
        demandPlan.setDescricao("Demand Plan Community");
        demandPlan.setPerfilExecucaoDemandPlan(new PerfilExecucaoDemandPlan("PROFILE"));
        demandPlan.setHorarioGeracao(LocalDateTime.of(2026, 1, 1, 8, 30));
        demandPlan.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));
        demandPlan.setDataFimPlano(LocalDateTime.of(2026, 3, 31, 23, 59));
        demandPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        return demandPlan;

    }

    private static DemandPlanItem criaDemandPlanItemSalvaParaTeste() {

        return new DemandPlanItem(
                new DemandPlanItem.DemandPlanItemKey(
                        criaDemandPlanSalvoInicialParaTeste(),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        LocalDateTime.of(2026, 1, 31, 23, 59)));

    }

    private static ParametrosForecastProjection getCommunityParametrosForecastProjection() {

        return new ParametrosForecastProjection(
                Constantes.DPModeloEstatistico.ARIMA,
                null,
                null,
                null,
                null,
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                120);

    }

    private static Calendario getCalendarioForecastTeste() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);

    }

    private static ParametrosGeraisDemandPlanningProjection getParametrosGeraisDemandPlanningProjectionBottomUp() {

        return new ParametrosGeraisDemandPlanningProjection(
                true,
                new ParametrosAgregacaoForecast(
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.BOTTOM_UP),
                3,
                false,
                false,
                0,
                new UnidadeMedida("UN"),
                false,
                null);

    }

    private static EnumSet<Constantes.DPModeloEstatistico> getDpModelosEstatisticosCommunity() {

        return EnumSet.of(
                Constantes.DPModeloEstatistico.MM,
                Constantes.DPModeloEstatistico.RMM,
                Constantes.DPModeloEstatistico.ARIMA,
                Constantes.DPModeloEstatistico.HOLT_WINTERS,
                Constantes.DPModeloEstatistico.ES);

    }

    private static void assertRequiresEnterpriseVersionException(
            DemandPlanningService demandPlanningService,
            String methodName,
            Object parameter) throws Exception {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidation(
                        demandPlanningService,
                        methodName,
                        parameter));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    private static void invokeValidation(
            DemandPlanningService demandPlanningService,
            String methodName,
            Object parameter) throws Exception {

        Method validationMethod = DemandPlanningService.class.getDeclaredMethod(
                methodName,
                parameter.getClass());
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                demandPlanningService,
                parameter);

    }

    private static void invokeValidaParametrosForecastCommunity(
            DemandPlanningService demandPlanningService,
            ParametrosForecastProjection parametrosForecastProjection) throws Exception {

        Method validaParametrosForecastCommunityMethod = DemandPlanningService.class.getDeclaredMethod(
                "validaParametrosForecastCommunity",
                ParametrosForecastProjection.class);
        validaParametrosForecastCommunityMethod.setAccessible(true);
        validaParametrosForecastCommunityMethod.invoke(
                demandPlanningService,
                parametrosForecastProjection);

    }

    private static UnidadeMedidaProjection invokeGetUnidadeMedidaProjectionObrigatoria(
            DemandPlanningService demandPlanningService,
            UnidadeMedidaProjection unidadeMedidaProjection,
            String contexto) throws Exception {

        Method getUnidadeMedidaProjectionObrigatoriaMethod = DemandPlanningService.class.getDeclaredMethod(
                "getUnidadeMedidaProjectionObrigatoria",
                UnidadeMedidaProjection.class,
                String.class);
        getUnidadeMedidaProjectionObrigatoriaMethod.setAccessible(true);
        return (UnidadeMedidaProjection) getUnidadeMedidaProjectionObrigatoriaMethod.invoke(
                demandPlanningService,
                unidadeMedidaProjection,
                contexto);

    }

    @SuppressWarnings("unchecked")
    private static List<ClusterLocations> invokeGetClusterLocationsDemandPlanningParaExecucaoCommunity(
            DemandPlanningService demandPlanningService,
            List<ClusterLocations> clusterLocationList) throws Exception {

        Method getClusterLocationsDemandPlanningParaExecucaoCommunityMethod =
                DemandPlanningService.class.getDeclaredMethod(
                        "getClusterLocationsDemandPlanningParaExecucaoCommunity",
                        List.class);
        getClusterLocationsDemandPlanningParaExecucaoCommunityMethod.setAccessible(true);
        return (List<ClusterLocations>) getClusterLocationsDemandPlanningParaExecucaoCommunityMethod.invoke(
                demandPlanningService,
                clusterLocationList);

    }

    @SuppressWarnings("unchecked")
    private static List<ClusterProdutosDemandPlanning> invokeGetClusterMateriaisDemandPlanningParaExecucaoCommunity(
            DemandPlanningService demandPlanningService,
            List<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningList) throws Exception {

        Method getClusterMateriaisDemandPlanningParaExecucaoCommunityMethod =
                DemandPlanningService.class.getDeclaredMethod(
                        "getClusterMateriaisDemandPlanningParaExecucaoCommunity",
                        List.class);
        getClusterMateriaisDemandPlanningParaExecucaoCommunityMethod.setAccessible(true);
        return (List<ClusterProdutosDemandPlanning>) getClusterMateriaisDemandPlanningParaExecucaoCommunityMethod.invoke(
                demandPlanningService,
                clusterMateriaisDemandPlanningList);

    }

    private static ParametrosDemandPlanProjection invokeGetParametrosDemandPlanProjectionObrigatoria(
            DemandPlanningService demandPlanningService,
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan) throws Exception {

        Method getParametrosDemandPlanProjectionObrigatoriaMethod =
                DemandPlanningService.class.getDeclaredMethod(
                        "getParametrosDemandPlanProjectionObrigatoria",
                        ParametrosDemandPlanProjection.class,
                        PerfilExecucaoDemandPlan.class);
        getParametrosDemandPlanProjectionObrigatoriaMethod.setAccessible(true);
        return (ParametrosDemandPlanProjection) getParametrosDemandPlanProjectionObrigatoriaMethod.invoke(
                demandPlanningService,
                parametrosDemandPlanProjection,
                perfilExecucaoDemandPlan);

    }

    private static ParametrosDemandPlanNivelClusterProjection invokeGetParametrosDemandPlanNivelClusterProjectionObrigatoria(
            DemandPlanningService demandPlanningService,
            ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection,
            ClusterLocations clusterLocations,
            ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning) throws Exception {

        Method getParametrosDemandPlanNivelClusterProjectionObrigatoriaMethod =
                DemandPlanningService.class.getDeclaredMethod(
                        "getParametrosDemandPlanNivelClusterProjectionObrigatoria",
                        ParametrosDemandPlanNivelClusterProjection.class,
                        ClusterLocations.class,
                        ClusterProdutosDemandPlanning.class);
        getParametrosDemandPlanNivelClusterProjectionObrigatoriaMethod.setAccessible(true);
        return (ParametrosDemandPlanNivelClusterProjection) getParametrosDemandPlanNivelClusterProjectionObrigatoriaMethod.invoke(
                demandPlanningService,
                parametrosDemandPlanNivelClusterProjection,
                clusterLocations,
                clusterMateriaisDemandPlanning);

    }

    private static void invokeValidaPerfilExecucaoDemandPlanDisponivel(
            DemandPlanningService demandPlanningService,
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            ParametrosGlobais parametrosGlobais) {

        demandPlanningService.validaPerfilExecucaoDemandPlanDisponivel(
                perfilExecucaoDemandPlan,
                parametrosGlobais);

    }

    @FunctionalInterface
    private interface ThrowingReflectionCall {

        void execute() throws Exception;

    }

    private static class CapturingJdbcTemplate extends JdbcTemplate {

        private final List<String> sqlStatements = new ArrayList<>();

        @Override
        public int update(String sql, Object... args) {

            sqlStatements.add(sql);
            return 1;

        }

    }

    private static class CapturingDemandPlanItemRepositoryInvocationHandler implements java.lang.reflect.InvocationHandler {

        private final List<DemandPlanItem> savedDemandPlanItems = new ArrayList<>();

        private DemandPlanItemRepository getProxy() {

            return (DemandPlanItemRepository) Proxy.newProxyInstance(
                    DemandPlanItemRepository.class.getClassLoader(),
                    new Class<?>[]{DemandPlanItemRepository.class},
                    this);

        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {

            if ("saveAll".equals(method.getName())) {
                Iterable<?> demandPlanItemIterable = (Iterable<?>) args[0];
                for (Object demandPlanItem : demandPlanItemIterable) {
                    savedDemandPlanItems.add((DemandPlanItem) demandPlanItem);
                }
                return savedDemandPlanItems;
            }
            if ("toString".equals(method.getName())) {
                return "CapturingDemandPlanItemRepository";
            }
            throw new UnsupportedOperationException("Metodo nao esperado no teste: " + method.getName());

        }

    }

    private static class BrokenDemandPlanningProjection extends DemandPlanningProjection {

        private final Set<DemandPlanItem> demandPlanItems;

        private BrokenDemandPlanningProjection(Set<DemandPlanItem> demandPlanItems) {

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
            this.demandPlanItems = demandPlanItems;

        }

        @Override
        public Set<DemandPlanItem> getTodosDemandPlanItems() {

            return demandPlanItems;

        }

        @Override
        public Set<DemandPlanItem> getDemandPlanItems(int posicaoPeriodo) {

            return demandPlanItems;

        }

    }

    private static class DuplicatedDemandPlanItemSet extends AbstractSet<DemandPlanItem> {

        private final List<DemandPlanItem> demandPlanItems;

        private DuplicatedDemandPlanItemSet(
                DemandPlanItem demandPlanItem) {

            this.demandPlanItems = List.of(
                    demandPlanItem,
                    demandPlanItem);

        }

        @Override
        public Iterator<DemandPlanItem> iterator() {

            return demandPlanItems.iterator();

        }

        @Override
        public int size() {

            return demandPlanItems.size();

        }

    }

    private static class ClusterEParametrosProjectionStub extends ClusterEParametrosProjection {

        ClusterEParametrosProjectionStub(ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

    }

    private static class ClusterEParametrosProjectionFactoryStub extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        ClusterEParametrosProjectionFactoryStub(ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return clusterEParametrosProjection;

        }

    }

    private static class UnidadeMedidaProjectionFactoryStub extends UnidadeMedidaProjectionFactory {

        private boolean unidadeMedidaProjectionRequested;

        @Override
        public UnidadeMedidaProjection getUnidadeMedidaProjectionCompletoDeCache() {

            unidadeMedidaProjectionRequested = true;
            return new UnidadeMedidaProjection();

        }

    }

    private static class SalesProjectionFactoryFailingStub extends SalesProjectionFactory {

        private boolean salesProjectionRequested;

        @Override
        public SalesProjectionLocationMaterialData getSalesProjectionLocationMaterialData(
                Constantes.TipoDocumentoVenda tipoDocumentoVenda,
                Calendario calendario,
                Set<Location> locations,
                Set<Produto> produtos,
                UnidadeMedidaProjection unidadeMedidaProjection,
                ClusterEParametrosProjection clusterEParametrosProjection,
                UnidadeMedida unidadePadrao) {

            salesProjectionRequested = true;
            throw new AssertionError("Sales projection nao deveria ser carregada antes da validacao de forecast Community.");

        }

    }

    private static DemandForecastWorkflowService criaDemandForecastWorkflowService() throws Exception {

        DemandForecastWorkflowService demandForecastWorkflowService =
                new DemandForecastWorkflowService();
        setField(
                demandForecastWorkflowService,
                "demandForecastStockoutTreatmentProcessor",
                new DemandForecastStockoutTreatmentProcessor());
        setField(
                demandForecastWorkflowService,
                "demandForecastHistoryCleaningProcessor",
                new DemandForecastHistoryCleaningProcessor());
        return demandForecastWorkflowService;

    }

}
