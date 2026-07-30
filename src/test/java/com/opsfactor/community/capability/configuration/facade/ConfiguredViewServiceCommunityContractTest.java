package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookParentSelectionDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewRepository;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Contrato Community da selecao de celulas do Planning Book.
 *
 * <p>O service de views e o ponto comum entre Demand Planning Book e Supply
 * Planning Book para transformar celulas recebidas do front em escopos
 * material/location. Como o Community nao permite ajuste agregado, qualquer
 * payload sem uma das duas dimensoes deve falhar antes de buscar entidades ou
 * montar projections.</p>
 */
class ConfiguredViewServiceCommunityContractTest {

    @Test
    void validaSelectedPlanningBookCellDTOCommunityShouldAcceptMaterialLocationCell() throws Exception {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();

        invokeValidaSelectedPlanningBookCellDTOCommunity(
                configuredViewService,
                getSelectedPlanningBookCellDTOValida());

    }

    @Test
    void validaSelectedPlanningBookCellDTOCommunityShouldRejectAggregatedCellBeforeEntityLookup() {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelectedPlanningBookCellDTOCommunity(
                        configuredViewService,
                        SelectedPlanningBookCellDTO.builder()
                                .materialDescriptionCols(Map.of("materialId", "MAT-1"))
                                .build()));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertTrue(
                invocationTargetException.getCause().getMessage().contains(
                        "Planning Book aggregated adjustments"));

    }

    @Test
    void validaSelectedPlanningBookCellDTOCommunityShouldRejectEnterpriseParentSelectionBeforeLeafValidation() {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();
        SelectedPlanningBookCellDTO selectedPlanningBookCellDTO = getSelectedPlanningBookCellDTOValida();
        selectedPlanningBookCellDTO.parentSelection = new PlanningBookParentSelectionDTO(
                "MATERIAL_LEVEL",
                "LOCATION_LEVEL",
                "MATERIAL_GROUP",
                "LOCATION_GROUP");

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelectedPlanningBookCellDTOCommunity(
                        configuredViewService,
                        selectedPlanningBookCellDTO));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertTrue(invocationTargetException.getCause().getMessage().contains(
                "Planning Book parent selections"));

    }

    @Test
    void validaSelectedPlanningBookCellDTOCommunityShouldRejectNullCellExplicitly() {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelectedPlanningBookCellDTOCommunity(
                        configuredViewService,
                        null));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Planning Book selected cell is required",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    void getConfiguredViewProjectionComDTOSelecaoCelulasShouldRejectNullCellCollectionBeforeFactory() {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewService.getConfiguredViewProjectionComDTOSelecaoCelulas(
                        "admin",
                        "DEFAULT_VIEW",
                        null,
                        null,
                        (Collection<SelectedPlanningBookCellDTO>) null,
                        null,
                        null));

        Assertions.assertEquals(
                "Planning Book selected cell collection is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void getDTOsNaoCobertosPorConfiguredViewsShouldRejectMissingViewTypeBeforeProjectionFactory() {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewService.getDTOsNaoCobertosPorConfiguredViews(null));

        Assertions.assertEquals(
                "Configured view type is required for configured view coverage",
                illegalArgumentException.getMessage());

    }

    @Test
    void getDTOsNaoCobertosPorConfiguredViewsShouldRejectBrokenViewSnapshotBeforeProjectionFactory() throws Exception {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();
        setPrivateField(
                configuredViewService,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory(
                        new TestClusterEParametrosProjection(new ParametrosGlobais())));
        setPrivateField(
                configuredViewService,
                "configuredViewRepository",
                getConfiguredViewRepositoryComViews(null));

        IllegalArgumentException colecaoAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewService.getDTOsNaoCobertosPorConfiguredViews(
                        ConfiguredView.TipoView.SUPPLYPLANNINGBOOK));

        setPrivateField(
                configuredViewService,
                "configuredViewRepository",
                getConfiguredViewRepositoryComViews(Collections.singletonList(null)));
        IllegalArgumentException itemAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewService.getDTOsNaoCobertosPorConfiguredViews(
                        ConfiguredView.TipoView.SUPPLYPLANNINGBOOK));

        /*
         * Lista vazia e ausencia operacional de cobertura. Ja lista nula ou
         * item nulo representam snapshot quebrado e devem falhar antes da
         * factory de projection tentar acessar chave composta/filtros da view.
         */
        Assertions.assertEquals(
                "Configured view collection is required for configured view coverage",
                colecaoAusenteException.getMessage());
        Assertions.assertEquals(
                "Configured view at index 0 is required for configured view coverage",
                itemAusenteException.getMessage());

    }

    @Test
    void getConfiguredViewProjectionComDTOSelecaoCelulasShouldRejectMissingBucketBeforeFactory() {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewService.getConfiguredViewProjectionComDTOSelecaoCelulas(
                        "admin",
                        "DEFAULT_VIEW",
                        ConfiguredView.TipoView.SUPPLYPLANNINGBOOK,
                        null,
                        List.of(getSelectedPlanningBookCellDTOValida()),
                        null,
                        null));

        Assertions.assertEquals(
                "Planning Book selected cell bucket size is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void validaDfuSelecionadoPertenceAViewCommunityShouldRejectInaccessibleDfuAsFunctionalPayloadError() throws Exception {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();
        Location selectedLocation = new Location("LOC-1");
        Produto selectedMaterial = new Produto("MAT-1");
        Location viewLocation = new Location("LOC-2");
        Produto viewMaterial = new Produto("MAT-2");
        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        configuredViewProjection.setConfiguredView(new ConfiguredView(
                new ConfiguredView.ConfiguredViewCompositeKey(
                        "admin",
                        "Supply Planning Book",
                        ConfiguredView.TipoView.SUPPLYPLANNINGBOOK)));
        configuredViewProjection.setDfuProjectionFiltrado(new FiltroDFUProjection(
                Set.of(viewLocation),
                Set.of(viewMaterial),
                new TestClusterEParametrosProjection(new ParametrosGlobais())));

        /*
         * DFU fora da view e payload/escopo invalido do Planning Book. A borda
         * transversal deve falhar como IllegalArgumentException, nao como Error
         * fatal da JVM, para que controllers e testes possam tratar a rejeicao
         * como erro funcional de selecao.
         */
        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDfuSelecionadoPertenceAViewCommunity(
                        configuredViewService,
                        configuredViewProjection,
                        selectedMaterial,
                        selectedLocation));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Material MAT-1 and location LOC-1 are not accessible for view Supply Planning Book",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    void validaDfuSelecionadoPertenceAViewCommunityShouldRejectBrokenSnapshotBeforeDfuLookup() throws Exception {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();
        ConfiguredViewProjection configuredViewProjectionSemDfuFiltrado = new ConfiguredViewProjection();
        ConfiguredViewProjection configuredViewProjectionSemConfiguredView =
                getConfiguredViewProjectionComDfuFiltradoSemConfiguredView();

        /*
         * A validacao de DFU selecionado e uma borda transversal de escrita do
         * Planning Book. Quando testes, overlays ou payloads quebrados chegam
         * sem projection, sem DFU filtrada ou sem chave material/location, o
         * erro deve explicar a pre-condicao ausente antes de consultar a
         * projection e gerar NPE.
         */
        assertInvalidDfuSelecionadoPertenceAViewCommunity(
                configuredViewService,
                null,
                new Produto("MAT-1"),
                new Location("LOC-1"),
                "Configured view projection is required for Planning Book selected cell validation");
        assertInvalidDfuSelecionadoPertenceAViewCommunity(
                configuredViewService,
                configuredViewProjectionSemDfuFiltrado,
                new Produto("MAT-1"),
                new Location("LOC-1"),
                "Filtered DFU projection is required for Planning Book selected cell validation");
        assertInvalidDfuSelecionadoPertenceAViewCommunity(
                configuredViewService,
                configuredViewProjectionSemConfiguredView,
                new Produto("MAT-1"),
                new Location("LOC-1"),
                "Configured view is required for Planning Book selected cell validation");
        assertInvalidDfuSelecionadoPertenceAViewCommunity(
                configuredViewService,
                configuredViewProjectionSemConfiguredView,
                null,
                new Location("LOC-1"),
                "Selected material is required for Planning Book selected cell validation");
        assertInvalidDfuSelecionadoPertenceAViewCommunity(
                configuredViewService,
                configuredViewProjectionSemConfiguredView,
                new Produto(" "),
                new Location("LOC-1"),
                "Selected material id is required for Planning Book selected cell validation");
        assertInvalidDfuSelecionadoPertenceAViewCommunity(
                configuredViewService,
                configuredViewProjectionSemConfiguredView,
                new Produto("MAT-1"),
                null,
                "Selected location is required for Planning Book selected cell validation");
        assertInvalidDfuSelecionadoPertenceAViewCommunity(
                configuredViewService,
                configuredViewProjectionSemConfiguredView,
                new Produto("MAT-1"),
                new Location(" "),
                "Selected location id is required for Planning Book selected cell validation");

    }

    @Test
    void validaSelectedPlanningBookCellDTOCommunityShouldRejectMissingRequiredCellFields() throws Exception {

        ConfiguredViewService configuredViewService = new ConfiguredViewService();

        assertInvalidSelectedCellField(
                configuredViewService,
                getSelectedPlanningBookCellDTO(
                        null,
                        "BASELINE",
                        "EA",
                        2.0),
                "Planning Book selected cell period is required");
        assertInvalidSelectedCellField(
                configuredViewService,
                getSelectedPlanningBookCellDTO(
                        LocalDate.of(2026, 1, 1),
                        " ",
                        "EA",
                        2.0),
                "Planning Book selected cell key figure is required");
        assertInvalidSelectedCellField(
                configuredViewService,
                getSelectedPlanningBookCellDTO(
                        LocalDate.of(2026, 1, 1),
                        "BASELINE",
                        "",
                        2.0),
                "Planning Book selected cell unit of measure is required");
        assertInvalidSelectedCellField(
                configuredViewService,
                getSelectedPlanningBookCellDTO(
                        LocalDate.of(2026, 1, 1),
                        "BASELINE",
                        "EA",
                        Double.NaN),
                "Planning Book selected cell new value must be finite");

    }

    private static void invokeValidaSelectedPlanningBookCellDTOCommunity(
            ConfiguredViewService configuredViewService,
            SelectedPlanningBookCellDTO selectedPlanningBookCellDTO) throws Exception {

        Method validationMethod = ConfiguredViewService.class.getDeclaredMethod(
                "validaSelectedPlanningBookCellDTOCommunity",
                SelectedPlanningBookCellDTO.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                configuredViewService,
                selectedPlanningBookCellDTO);

    }

    private static void assertInvalidSelectedCellField(
            ConfiguredViewService configuredViewService,
            SelectedPlanningBookCellDTO selectedPlanningBookCellDTO,
            String expectedMessage) throws Exception {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelectedPlanningBookCellDTOCommunity(
                        configuredViewService,
                        selectedPlanningBookCellDTO));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                expectedMessage,
                invocationTargetException.getCause().getMessage());

    }

    private static void invokeValidaDfuSelecionadoPertenceAViewCommunity(
            ConfiguredViewService configuredViewService,
            ConfiguredViewProjection configuredViewProjection,
            Produto materialSelecionado,
            Location locationSelecionada) throws Exception {

        Method validationMethod = ConfiguredViewService.class.getDeclaredMethod(
                "validaDfuSelecionadoPertenceAViewCommunity",
                ConfiguredViewProjection.class,
                Produto.class,
                Location.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                configuredViewService,
                configuredViewProjection,
                materialSelecionado,
                locationSelecionada);

    }

    private static void assertInvalidDfuSelecionadoPertenceAViewCommunity(
            ConfiguredViewService configuredViewService,
            ConfiguredViewProjection configuredViewProjection,
            Produto materialSelecionado,
            Location locationSelecionada,
            String expectedMessage) throws Exception {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDfuSelecionadoPertenceAViewCommunity(
                        configuredViewService,
                        configuredViewProjection,
                        materialSelecionado,
                        locationSelecionada));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                expectedMessage,
                invocationTargetException.getCause().getMessage());

    }

    private static ConfiguredViewProjection getConfiguredViewProjectionComDfuFiltradoSemConfiguredView() {

        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        configuredViewProjection.setDfuProjectionFiltrado(new FiltroDFUProjection(
                Set.of(new Location("LOC-2")),
                Set.of(new Produto("MAT-2")),
                new TestClusterEParametrosProjection(new ParametrosGlobais())));
        return configuredViewProjection;

    }

    private static SelectedPlanningBookCellDTO getSelectedPlanningBookCellDTOValida() {

        return getSelectedPlanningBookCellDTO(
                LocalDate.of(2026, 1, 1),
                "BASELINE",
                "EA",
                2.0);

    }

    private static SelectedPlanningBookCellDTO getSelectedPlanningBookCellDTO(
            LocalDate period,
            String keyFigure,
            String uom,
            Double newValue) {

        return SelectedPlanningBookCellDTO.builder()
                .materialDescriptionCols(Map.of("materialId", "MAT-1"))
                .locationDescriptionCols(Map.of("locationId", "LOC-1"))
                .keyFigure(keyFigure)
                .period(period)
                .uom(uom)
                .oldValue(1.0)
                .newValue(newValue)
                .build();

    }

    private static ClusterEParametrosProjection invokeGetClusterEParametrosProjectionObrigatoria(
            ConfiguredViewService configuredViewService,
            ClusterEParametrosProjection clusterEParametrosProjection,
            String contextoOperacional) throws Exception {

        Method validationMethod = ConfiguredViewService.class.getDeclaredMethod(
                "getClusterEParametrosProjectionObrigatoria",
                ClusterEParametrosProjection.class,
                String.class);
        validationMethod.setAccessible(true);
        return (ClusterEParametrosProjection) validationMethod.invoke(
                configuredViewService,
                clusterEParametrosProjection,
                contextoOperacional);

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static ConfiguredViewRepository getConfiguredViewRepositoryComViews(
            List<ConfiguredView> configuredViews) {

        return (ConfiguredViewRepository) Proxy.newProxyInstance(
                ConfiguredViewRepository.class.getClassLoader(),
                new Class<?>[]{ConfiguredViewRepository.class},
                (proxy, method, args) -> {
                    if ("customFindByConfiguredViewCompositeKeyTipoView".equals(method.getName())) {
                        return configuredViews;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ConfiguredViewRepository de teste para cobertura Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static class TestConfiguredViewProjectionFactory extends ConfiguredViewProjectionFactory {

        private final ConfiguredViewProjection configuredViewProjection;

        private TestConfiguredViewProjectionFactory(ConfiguredViewProjection configuredViewProjection) {

            this.configuredViewProjection = configuredViewProjection;

        }

        @Override
        public ConfiguredViewProjection getConfiguredViewProjection(
                String userId,
                String nomeView,
                ConfiguredView.TipoView tipoView,
                ClusterEParametrosProjection clusterEParametrosProjection,
                BiFunction<Produto, Location, Boolean> filtrosAdicionaisDfus) {

            return configuredViewProjection;

        }

        @Override
        public ConfiguredViewProjection getConfiguredViewProjection(
                ConfiguredView configuredView,
                ClusterEParametrosProjection clusterEParametrosProjection) {

            return configuredViewProjection;

        }

    }

    private static class TestClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        private TestClusterEParametrosProjectionFactory(ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return clusterEParametrosProjection;

        }

    }

    private static class TestClusterEParametrosProjection extends ClusterEParametrosProjection {

        private final ParametrosGlobais parametrosGlobais;

        private TestClusterEParametrosProjection(ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

    }

}
