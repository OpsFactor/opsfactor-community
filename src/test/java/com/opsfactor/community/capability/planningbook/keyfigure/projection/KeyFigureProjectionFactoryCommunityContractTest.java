package com.opsfactor.community.capability.planningbook.keyfigure.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.projection.inmemorybi.applied.BIProjectionMaterialLocationPeriodo;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardMonetariaDemandPlanning;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardSupplyPlanning;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureAbstract;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigurePadrao;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.capability.demandplanning.planningbook.domain.DemandPlanningPlanningBookCatalog;
import com.opsfactor.community.capability.supplyplanning.planningbook.domain.SupplyPlanningPlanningBookCatalog;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Valida o contrato Community de key figures publicaveis no Planning Book.
 *
 * <p>A factory monta a lista efetiva que chega ao front. Por isso ela deve
 * falhar com `RequiresEnterpriseVersionException` quando uma view antiga,
 * payload manual ou configuracao Enterprise tentar introduzir KFs fora do
 * subconjunto Community.</p>
 */
class KeyFigureProjectionFactoryCommunityContractTest {

    @Test
    void validaKeyFigureDemandPlanningBookCommunityShouldAcceptCommunityKeyFigures() throws Exception {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        for (KeyFigureStandardEnum keyFigureStandardEnum : List.of(
                KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP,
                KeyFigureStandardEnum.HISTORICO_VENDAS,
                KeyFigureStandardEnum.BASELINE,
                KeyFigureStandardEnum.AJUSTE_DEMANDA)) {
            invokeValidaKeyFigureDemandPlanningBookCommunity(
                    keyFigureProjectionFactory,
                    new KeyFigureStandard(keyFigureStandardEnum));
        }

    }

    @Test
    void validaKeyFigureDemandPlanningBookCommunityShouldAcceptRuntimeInfoCatalogIds() throws Exception {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        for (String keyFigureId : DemandPlanningPlanningBookCatalog.getKeyFiguresVisiveisDemandPlanningBookCommunity()) {
            KeyFigureStandardEnum keyFigureStandardEnum =
                    MetodosUtilidade.getValorEnumDeJsonProperty(
                            KeyFigureStandardEnum.class,
                            keyFigureId);

            /*
             * O RuntimeInfo publica labels publicos para a SPA, enquanto a
             * projection trabalha com o enum tecnico. Validar a conversao aqui
             * impede que o catalogo visual Community anuncie uma linha que a
             * factory bloquearia como selecao Enterprise.
             */
            invokeValidaKeyFigureDemandPlanningBookCommunity(
                    keyFigureProjectionFactory,
                    new KeyFigureStandard(keyFigureStandardEnum));
        }

    }

    @Test
    void editableDemandPlanningBookRuntimeInfoIdsShouldHaveEditableMode() {

        for (String keyFigureId : DemandPlanningPlanningBookCatalog.getKeyFiguresEditaveisDemandPlanningBookCommunity()) {
            KeyFigureStandardEnum keyFigureStandardEnum =
                    MetodosUtilidade.getValorEnumDeJsonProperty(
                            KeyFigureStandardEnum.class,
                            keyFigureId);

            /*
             * O RuntimeInfo orienta a SPA sobre quais linhas podem receber
             * edicao direta. Esse contrato nao pode ser mais permissivo que o
             * enum tecnico usado pela projection; `Baseline`, por exemplo,
             * segue visivel, mas e somente-leitura no Community.
             */
            Assertions.assertNotEquals(
                    EditMode.NOEDIT,
                    keyFigureStandardEnum.getEditMode(),
                    "RuntimeInfo publicou KF de Demand como editavel sem EditMode editavel: " + keyFigureId);
        }

    }

    @Test
    void validaKeyFigureDemandPlanningBookCommunityShouldRejectDirectDemandPerWorkingDay() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        assertDemandPlanningRequiresEnterpriseVersionException(
                keyFigureProjectionFactory,
                new KeyFigureStandard(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL));

    }

    @Test
    void validaKeyFigureDemandPlanningBookCommunityShouldRejectEnterpriseKeyFigure() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        assertDemandPlanningRequiresEnterpriseVersionException(
                keyFigureProjectionFactory,
                new KeyFigureStandard(KeyFigureStandardEnum.UPLIFT));

    }

    @Test
    void validaKeyFigureDemandPlanningBookCommunityShouldRejectCustomerOrders() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        assertDemandPlanningRequiresEnterpriseVersionException(
                keyFigureProjectionFactory,
                new KeyFigureStandard(KeyFigureStandardEnum.CARTEIRA));

    }

    @Test
    void validaKeyFigureDemandPlanningBookCommunityShouldRejectMonetarySalesKeyFigures() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        for (KeyFigureStandardEnum keyFigureStandardEnum : List.of(
                KeyFigureStandardEnum.VENDAS_GROSS,
                KeyFigureStandardEnum.VENDAS_NET)) {
            assertDemandPlanningRequiresEnterpriseVersionException(
                    keyFigureProjectionFactory,
                    new KeyFigureStandardMonetariaDemandPlanning(keyFigureStandardEnum));
        }

    }

    @Test
    void validaKeyFigureDemandPlanningBookCommunityShouldRejectCustomKeyFigure() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        assertDemandPlanningRequiresEnterpriseVersionException(
                keyFigureProjectionFactory,
                new CustomKeyFigure());

    }

    @Test
    void validaKeyFigureSupplyPlanningBookCommunityShouldAcceptCommunityKeyFigure() throws Exception {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        invokeValidaKeyFigureSupplyPlanningBookCommunity(
                keyFigureProjectionFactory,
                new KeyFigureStandardSupplyPlanning(
                        KeyFigureStandardEnum.ESTOQUE,
                        Constantes.TipoPlano.PLANO_TRABALHO));

    }

    @Test
    void supplyPlanningRuntimeShouldRejectRestrictedPlanKeyFigureEvenWhenSelectionBypassesSaveBoundary()
            throws Exception {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();
        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        configuredViewProjection.setKeyFiguresOrdenadasParaExibicao(List.of(
                new KeyFigureStandardSupplyPlanning(
                        KeyFigureStandardEnum.ESTOQUE,
                        Constantes.TipoPlano.PLANO_RESTRITO)));

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeAtualizaProjectionComKeyFiguresSupplyPlanningApresentados(
                        keyFigureProjectionFactory,
                        new KeyFigureProjection(),
                        configuredViewProjection));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    @Test
    void validaKeyFigureSupplyPlanningBookCommunityShouldAcceptRuntimeInfoCatalogIds() throws Exception {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        for (String keyFigureId : SupplyPlanningPlanningBookCatalog.getKeyFiguresVisiveisSupplyPlanningBookCommunity()) {
            KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning =
                    new KeyFigureStandardSupplyPlanning(keyFigureId);

            /*
             * O catalogo de RuntimeInfo usa os mesmos ids tecnicos que a
             * grade envia ao backend. Validar cada id aqui impede que a lista
             * publica da SPA se desalinhe da allowlist real da projection.
             */
            invokeValidaKeyFigureSupplyPlanningBookCommunity(
                    keyFigureProjectionFactory,
                    keyFigureStandardSupplyPlanning);
        }

    }

    @Test
    void editableSupplyPlanningBookRuntimeInfoIdsShouldBeEditableWorkingPlanKeyFigures() {

        for (String keyFigureId : SupplyPlanningPlanningBookCatalog.getKeyFiguresEditaveisSupplyPlanningBookCommunity()) {
            KeyFigureStandardSupplyPlanning keyFigureStandardSupplyPlanning =
                    new KeyFigureStandardSupplyPlanning(keyFigureId);

            Assertions.assertEquals(
                    Constantes.TipoPlano.PLANO_TRABALHO,
                    keyFigureStandardSupplyPlanning.getTipoPlano());
            Assertions.assertTrue(
                    keyFigureStandardSupplyPlanning.getPadraoPermiteEdicao(),
                    "RuntimeInfo publicou KF de Supply como editavel sem EditMode editavel: " + keyFigureId);
        }

    }

    @Test
    void supplyPlanningPresentedKeyFiguresShouldUseCommunityDefaultsWhenViewListIsNull() throws Exception {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();
        KeyFigureProjection keyFigureProjection = new KeyFigureProjection();
        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();

        invokeAtualizaProjectionComKeyFiguresSupplyPlanningApresentados(
                keyFigureProjectionFactory,
                keyFigureProjection,
                configuredViewProjection);

        List<String> keyFigureIds = keyFigureProjection.keyFiguresApresentadosEOrdenados.stream()
                .map(KeyFigureInterface::getId)
                .toList();
        Assertions.assertEquals(
                SupplyPlanningPlanningBookCatalog.getKeyFiguresVisiveisSupplyPlanningBookCommunity(),
                keyFigureIds);

    }

    @Test
    void supplyPlanningKeyFigureShouldRejectMissingIdentifierExplicitly() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new KeyFigureStandardSupplyPlanning(null));

        Assertions.assertEquals(
                "Id/descrição da Key Figure do supply planning não pode ser nulo",
                illegalArgumentException.getMessage());

    }

    @Test
    void demandPlanningKeyFigureSumShouldReuseExistingOutputByMaterialAndDate() throws Exception {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();
        KeyFigureProjection keyFigureProjection =
                createKeyFigureProjectionWithEmptyBi();
        Location location =
                new Location("LOC-1");
        Produto material =
                new Produto("MAT-1");
        KeyFigureStandard baselineKeyFigure =
                new KeyFigureStandard(KeyFigureStandardEnum.BASELINE);
        KeyFigureStandard demandAdjustmentKeyFigure =
                new KeyFigureStandard(KeyFigureStandardEnum.AJUSTE_DEMANDA);
        KeyFigureStandard totalDemandKeyFigure =
                new KeyFigureStandard(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP);
        LocalDateTime referenceDate =
                keyFigureProjection.getCalendario().getUltimaDataHorarioPeriodo(0);

        keyFigureProjection.addDadoDFUKeyFigurePadrao(
                location,
                material,
                referenceDate,
                baselineKeyFigure,
                4.0d);
        keyFigureProjection.addDadoDFUKeyFigurePadrao(
                location,
                material,
                referenceDate,
                demandAdjustmentKeyFigure,
                6.0d);
        keyFigureProjection.addDadoDFUKeyFigurePadrao(
                location,
                material,
                referenceDate,
                totalDemandKeyFigure,
                1.0d);

        /*
         * A KF de soma ja existe para a mesma DFU/data. A factory precisa
         * atualiza-la por data, em vez de criar uma segunda linha output.
         */
        invokeAtualizaKeyFigureProjectionDPComKeyFigureSoma(
                keyFigureProjectionFactory,
                keyFigureProjection,
                List.of(
                        baselineKeyFigure,
                        demandAdjustmentKeyFigure),
                totalDemandKeyFigure);

        List<DFUDataKeyFigureAbstract> totalDemandData =
                keyFigureProjection.getDadosKeyFigure(
                        totalDemandKeyFigure,
                        material,
                        location);

        Assertions.assertEquals(
                1,
                totalDemandData.size());
        Assertions.assertEquals(
                10.0d,
                ((DFUDataKeyFigurePadrao) totalDemandData.getFirst()).getValor());

    }

    @Test
    void validaKeyFigureSupplyPlanningBookCommunityShouldRejectEnterpriseKeyFigure() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        assertRequiresEnterpriseVersionException(
                keyFigureProjectionFactory,
                new KeyFigureStandard(KeyFigureStandardEnum.INBOUND_ESTOQUE_EM_TRANSITO));

    }

    @Test
    void validaKeyFigureSupplyPlanningBookCommunityShouldRejectEnterpriseInboundOrders() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        assertRequiresEnterpriseVersionException(
                keyFigureProjectionFactory,
                new KeyFigureStandard(KeyFigureStandardEnum.INBOUND_FIRME));

    }

    @Test
    void validaKeyFigureSupplyPlanningBookCommunityShouldRejectEnterpriseCustomerOrders() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        assertRequiresEnterpriseVersionException(
                keyFigureProjectionFactory,
                new KeyFigureStandard(KeyFigureStandardEnum.DEMANDA_DIRETA_CARTEIRA_SNP));

    }

    @Test
    void validaKeyFigureSupplyPlanningBookCommunityShouldRejectStandardOutsideCommunityWhitelist() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        assertRequiresEnterpriseVersionException(
                keyFigureProjectionFactory,
                new KeyFigureStandard(KeyFigureStandardEnum.OUTBOUND_PLANEJADO));

    }

    @Test
    void validaKeyFigureSupplyPlanningBookCommunityShouldRejectCustomKeyFigure() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();

        assertRequiresEnterpriseVersionException(
                keyFigureProjectionFactory,
                new CustomKeyFigure());

    }

    @Test
    void supplyPlanningProjectionCacheShouldRejectAggregatedLocationViewBeforeProjectionLoading() {

        KeyFigureProjectionFactory keyFigureProjectionFactory = new KeyFigureProjectionFactory();
        KeyFigureProjection keyFigureProjection = new KeyFigureProjection();
        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setPerfilExecucaoSupplyPlan(new PerfilExecucaoSupplyPlan());

        keyFigureProjection.supplyPlan = supplyPlan;
        keyFigureProjection.configuredViewProjection = new FakeConfiguredViewProjection(Set.of(
                new Location("LOC-1"),
                new Location("LOC-2")));

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeAtualizaCacheKeyFigureProjectionComSupplyPlanningProjection(
                        keyFigureProjectionFactory,
                        keyFigureProjection));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertTrue(invocationTargetException.getCause().getMessage().contains(
                "Aggregated Supply Planning Book location views"));
        Assertions.assertTrue(invocationTargetException.getCause().getMessage().contains(
                "received 2 locations"));

    }

    private static void assertRequiresEnterpriseVersionException(
            KeyFigureProjectionFactory keyFigureProjectionFactory,
            KeyFigureInterface<?> keyFigureInterface) {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaKeyFigureSupplyPlanningBookCommunity(
                        keyFigureProjectionFactory,
                        keyFigureInterface));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    private static void assertDemandPlanningRequiresEnterpriseVersionException(
            KeyFigureProjectionFactory keyFigureProjectionFactory,
            KeyFigureInterface<?> keyFigureInterface) {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaKeyFigureDemandPlanningBookCommunity(
                        keyFigureProjectionFactory,
                        keyFigureInterface));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    private static void invokeValidaKeyFigureDemandPlanningBookCommunity(
            KeyFigureProjectionFactory keyFigureProjectionFactory,
            KeyFigureInterface<?> keyFigureInterface) throws Exception {

        Method validationMethod = KeyFigureProjectionFactory.class.getDeclaredMethod(
                "validaKeyFigureDemandPlanningBookCommunity",
                KeyFigureInterface.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                keyFigureProjectionFactory,
                keyFigureInterface);

    }

    private static void invokeValidaKeyFigureSupplyPlanningBookCommunity(
            KeyFigureProjectionFactory keyFigureProjectionFactory,
            KeyFigureInterface<?> keyFigureInterface) throws Exception {

        Method validationMethod = KeyFigureProjectionFactory.class.getDeclaredMethod(
                "validaKeyFigureSupplyPlanningBookCommunity",
                KeyFigureInterface.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                keyFigureProjectionFactory,
                keyFigureInterface);

    }

    private static void invokeAtualizaCacheKeyFigureProjectionComSupplyPlanningProjection(
            KeyFigureProjectionFactory keyFigureProjectionFactory,
            KeyFigureProjection keyFigureProjection) throws Exception {

        Method validationMethod = KeyFigureProjectionFactory.class.getDeclaredMethod(
                "atualizaCacheKeyFigureProjectionComSupplyPlanningProjection",
                KeyFigureProjection.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                keyFigureProjectionFactory,
                keyFigureProjection);

    }

    private static void invokeAtualizaProjectionComKeyFiguresSupplyPlanningApresentados(
            KeyFigureProjectionFactory keyFigureProjectionFactory,
            KeyFigureProjection keyFigureProjection,
            ConfiguredViewProjection configuredViewProjection) throws Exception {

        Method validationMethod = KeyFigureProjectionFactory.class.getDeclaredMethod(
                "atualizaProjectionComKeyFiguresSupplyPlanningApresentados",
                KeyFigureProjection.class,
                ConfiguredViewProjection.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                keyFigureProjectionFactory,
                keyFigureProjection,
                configuredViewProjection);

    }

    private static void invokeAtualizaKeyFigureProjectionDPComKeyFigureSoma(
            KeyFigureProjectionFactory keyFigureProjectionFactory,
            KeyFigureProjection keyFigureProjection,
            Collection<KeyFigureInterface> keyFiguresInput,
            KeyFigureInterface keyFigureOutputSoma) throws Exception {

        Method validationMethod = KeyFigureProjectionFactory.class.getDeclaredMethod(
                "atualizaKeyFigureProjectionDPComKeyFigureSoma",
                KeyFigureProjection.class,
                Collection.class,
                KeyFigureInterface.class,
                Integer.class,
                Integer.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                keyFigureProjectionFactory,
                keyFigureProjection,
                keyFiguresInput,
                keyFigureOutputSoma,
                null,
                null);

    }

    private static KeyFigureProjection createKeyFigureProjectionWithEmptyBi() throws Exception {

        Calendario calendario =
                Calendario.criaCalendarioPeriodosFuturosDeDatas(
                        Constantes.TamanhoBucket.DIARIO,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 1, 1, 0, 0));
        KeyFigureProjection keyFigureProjection =
                new KeyFigureProjection();
        BIProjectionMaterialLocationPeriodo<DFUDataKeyFigureAbstract> biProjection =
                new BIProjectionMaterialLocationPeriodo<>(
                        calendario,
                        DFUDataKeyFigureAbstract::getProduto,
                        DFUDataKeyFigureAbstract::getLocation,
                        DFUDataKeyFigureAbstract::getData,
                        DFUDataKeyFigureAbstract.class,
                        true,
                        true);

        /*
         * O BI de produção registra KeyFigure como atributo adicional após
         * criar os índices comuns de material/location/periodo. A fixture
         * reproduz essa configuração para validar a atualização da linha soma.
         */
        biProjection.getBiEmMemoria().addObjectAttribute(
                "KeyFigure",
                KeyFigureInterface.class,
                DFUDataKeyFigureAbstract::getKeyFigure,
                true);

        setField(
                keyFigureProjection,
                "calendario",
                calendario);
        setField(
                keyFigureProjection,
                "biEmMemoriaDFUDataKeyFigure",
                biProjection);
        return keyFigureProjection;

    }

    private static KeyFigureProjectionFactory createKeyFigureProjectionFactory(
            SupplyNetworkProjection supplyNetworkProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) throws Exception {

        KeyFigureProjectionFactory keyFigureProjectionFactory =
                new KeyFigureProjectionFactory();

        setField(
                keyFigureProjectionFactory,
                "supplyNetworkProjectionFactory",
                new FakeSupplyNetworkProjectionFactory(supplyNetworkProjection));
        setField(
                keyFigureProjectionFactory,
                "unidadeMedidaProjectionFactory",
                new FakeUnidadeMedidaProjectionFactory(unidadeMedidaProjection));

        return keyFigureProjectionFactory;

    }

    private static ConfiguredViewProjection createConfiguredViewProjectionComParametrosGlobais()
            throws Exception {

        return new FakeConfiguredViewProjection(
                Set.of(),
                createClusterEParametrosProjectionComParametrosGlobais(),
                new ConfiguredView());

    }

    private static ClusterEParametrosProjection createClusterEParametrosProjectionComParametrosGlobais()
            throws Exception {

        ClusterEParametrosProjection clusterEParametrosProjection =
                new ClusterEParametrosProjection();

        setField(
                clusterEParametrosProjection,
                "parametrosGlobais",
                new ParametrosGlobais());

        return clusterEParametrosProjection;

    }

    private static UnidadeMedidaProjection createUnidadeMedidaProjectionComParametrosGlobais()
            throws Exception {

        UnidadeMedidaProjection unidadeMedidaProjection = new UnidadeMedidaProjection();

        setField(
                unidadeMedidaProjection,
                "parametrosGlobais",
                new ParametrosGlobais());

        return unidadeMedidaProjection;

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);

    }

    private static class FakeConfiguredViewProjection extends ConfiguredViewProjection {

        private final Set<Location> locationsFiltradas;
        private final ClusterEParametrosProjection clusterEParametrosProjection;
        private final ConfiguredView configuredView;

        private FakeConfiguredViewProjection(Set<Location> locationsFiltradas) {

            this(
                    locationsFiltradas,
                    null,
                    null);

        }

        private FakeConfiguredViewProjection(
                Set<Location> locationsFiltradas,
                ClusterEParametrosProjection clusterEParametrosProjection,
                ConfiguredView configuredView) {

            this.locationsFiltradas = locationsFiltradas;
            this.clusterEParametrosProjection = clusterEParametrosProjection;
            this.configuredView = configuredView;

        }

        @Override
        public Set<Location> getLocationsFiltradas() {

            return locationsFiltradas;

        }

        @Override
        public ClusterEParametrosProjection getClusterEParametrosProjection() {

            return clusterEParametrosProjection;

        }

        @Override
        public ConfiguredView getConfiguredView() {

            return configuredView;

        }

    }

    private static class FakeSupplyNetworkProjectionFactory extends SupplyNetworkProjectionFactory {

        private final SupplyNetworkProjection supplyNetworkProjection;

        private FakeSupplyNetworkProjectionFactory(SupplyNetworkProjection supplyNetworkProjection) {

            this.supplyNetworkProjection = supplyNetworkProjection;

        }

        @Override
        public SupplyNetworkProjection getSupplyNetworkProjectionCompletoDeCache() {

            return supplyNetworkProjection;

        }

    }

    private static class FakeUnidadeMedidaProjectionFactory extends UnidadeMedidaProjectionFactory {

        private final UnidadeMedidaProjection unidadeMedidaProjection;

        private FakeUnidadeMedidaProjectionFactory(UnidadeMedidaProjection unidadeMedidaProjection) {

            this.unidadeMedidaProjection = unidadeMedidaProjection;

        }

        @Override
        public UnidadeMedidaProjection getUnidadeMedidaProjectionCompletoDeCache() {

            return unidadeMedidaProjection;

        }

    }

    private static class CustomKeyFigure implements KeyFigureInterface<CustomKeyFigure> {

        @Override
        public String getId() {

            return "CUSTOM";

        }

        @Override
        public String getDescricao() {

            return "Custom";

        }

        @Override
        public TipoKeyFigure getTipoKeyFigure() {

            return TipoKeyFigure.CUSTOM;

        }

        @Override
        public boolean getPadraoPermiteEdicao() {

            return true;

        }

        @Override
        public EditMode getEditModePadrao() {

            return EditMode.CELLEDIT;

        }

    }

}
