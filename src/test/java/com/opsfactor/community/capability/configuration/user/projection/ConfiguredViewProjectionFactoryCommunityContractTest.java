package com.opsfactor.community.capability.configuration.user.projection;

import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewRepository;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Contrato Community da materializacao de views de Planning Book.
 *
 * <p>A configuracao persistida Community nao possui filtros, agrupamentos ou
 * colunas de caracteristicas dinamicas nem selecao livre de key figures. A
 * projection deve, portanto, nascer diretamente no escopo material/location e
 * aplicar apenas filtros ad-hoc do proprio fluxo Community.</p>
 */
public class ConfiguredViewProjectionFactoryCommunityContractTest {

    @Test
    public void getConfiguredViewProjectionShouldStartWithMaterialLocationScopeCommunity() throws Exception {

        Produto material = new Produto("M1");
        Location location = new Location("L1");

        ClusterEParametrosProjection clusterEParametrosProjection = new ClusterEParametrosProjection();
        setField(clusterEParametrosProjection, "materialSet", Set.of(material));
        setField(clusterEParametrosProjection, "locationSet", Set.of(location));

        ConfiguredView configuredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "admin",
                "Community view",
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK));

        ConfiguredViewProjection configuredViewProjection =
                new ConfiguredViewProjectionFactory()
                        .getConfiguredViewProjection(configuredView, clusterEParametrosProjection);

        Assertions.assertEquals(Set.of(material), configuredViewProjection.getMateriaisFiltrados());
        Assertions.assertEquals(Set.of(location), configuredViewProjection.getLocationsFiltradas());
        Assertions.assertEquals(1, configuredViewProjection.getPlanningBookDfuScopes().size());
        Assertions.assertEquals(
                Set.of(material),
                configuredViewProjection.getPlanningBookDfuScopes().iterator().next().getMateriais());
        Assertions.assertEquals(
                Set.of(location),
                configuredViewProjection.getPlanningBookDfuScopes().iterator().next().getLocations());

    }

    @Test
    public void getConfiguredViewProjectionShouldReflectAdHocDfuFiltersInTechnicalGroupsCommunity() throws Exception {

        Produto materialMantido = new Produto("M1");
        Produto materialFiltrado = new Produto("M2");
        Location locationMantida = new Location("L1");
        Location locationFiltrada = new Location("L2");

        ClusterEParametrosProjection clusterEParametrosProjection = new ClusterEParametrosProjection();
        setField(clusterEParametrosProjection, "materialSet", Set.of(materialMantido, materialFiltrado));
        setField(clusterEParametrosProjection, "locationSet", Set.of(locationMantida, locationFiltrada));

        ConfiguredView configuredView = new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "admin",
                "Ad-hoc DFU filter",
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK));

        ConfiguredViewProjection configuredViewProjection =
                new ConfiguredViewProjectionFactory()
                        .getConfiguredViewProjection(
                                configuredView,
                                clusterEParametrosProjection,
                                (material, location) -> material.equals(materialMantido) && location.equals(locationMantida));

        /*
         * Filtros ad-hoc Community, como esconder DFUs sem historico, precisam
         * reduzir tanto a DFUProjection quanto o escopo material/location usado
         * em seguida pelo Planning Book. Caso contrario a view poderia montar
         * linhas para materiais/locations ja removidos do escopo.
         */
        Assertions.assertEquals(Set.of(materialMantido), configuredViewProjection.getMateriaisFiltrados());
        Assertions.assertEquals(Set.of(locationMantida), configuredViewProjection.getLocationsFiltradas());
        Assertions.assertEquals(1, configuredViewProjection.getPlanningBookDfuScopes().size());
        Assertions.assertEquals(
                Set.of(materialMantido),
                configuredViewProjection.getPlanningBookDfuScopes().iterator().next().getMateriais());
        Assertions.assertEquals(
                Set.of(locationMantida),
                configuredViewProjection.getPlanningBookDfuScopes().iterator().next().getLocations());

    }

    @Test
    public void getConfiguredViewProjectionShouldRejectBrokenInputsBeforeDfuProjection() throws Exception {

        ConfiguredViewProjectionFactory configuredViewProjectionFactory =
                new ConfiguredViewProjectionFactory();
        ClusterEParametrosProjection clusterEParametrosProjection = new ClusterEParametrosProjection();
        setField(clusterEParametrosProjection, "materialSet", Set.of(new Produto("M1")));
        setField(clusterEParametrosProjection, "locationSet", Set.of(new Location("L1")));

        /*
         * A montagem direta e usada por services que ja carregaram a entidade.
         * Entidade nula, chave incompleta ou projection estrutural quebrada
         * devem falhar antes de acessar tipo da view, materiais ativos ou
         * locations ativas.
         */
        IllegalArgumentException viewAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        null,
                        clusterEParametrosProjection));
        IllegalArgumentException chaveAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        new ConfiguredView(),
                        clusterEParametrosProjection));
        IllegalArgumentException projectionAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        getConfiguredViewValida(),
                        null));

        Assertions.assertEquals(
                "Configured view is required for Planning Book projection.",
                viewAusenteException.getMessage());
        Assertions.assertEquals(
                "Configured view primary key is required for Planning Book projection.",
                chaveAusenteException.getMessage());
        Assertions.assertEquals(
                "Cluster and parameters projection is required for Planning Book projection.",
                projectionAusenteException.getMessage());

    }

    @Test
    public void getConfiguredViewProjectionShouldRejectBrokenMaterialLocationSnapshotsBeforeDfuProjection() throws Exception {

        ConfiguredViewProjectionFactory configuredViewProjectionFactory =
                new ConfiguredViewProjectionFactory();
        ClusterEParametrosProjection clusterEParametrosProjectionSemMateriais =
                new ClusterEParametrosProjection();
        ClusterEParametrosProjection clusterEParametrosProjectionSemLocations =
                new ClusterEParametrosProjection();
        setField(clusterEParametrosProjectionSemLocations, "materialSet", Set.of(new Produto("M1")));

        IllegalArgumentException materiaisAusentesException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        getConfiguredViewValida(),
                        clusterEParametrosProjectionSemMateriais));
        IllegalArgumentException locationsAusentesException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        getConfiguredViewValida(),
                        clusterEParametrosProjectionSemLocations));

        Assertions.assertEquals(
                "Material snapshot is required in cluster and parameters projection for Planning Book projection.",
                materiaisAusentesException.getMessage());
        Assertions.assertEquals(
                "Location snapshot is required in cluster and parameters projection for Planning Book projection.",
                locationsAusentesException.getMessage());

    }

    @Test
    public void getConfiguredViewProjectionShouldRejectBrokenMaterialLocationItemsBeforeActiveFilters() throws Exception {

        ConfiguredViewProjectionFactory configuredViewProjectionFactory =
                new ConfiguredViewProjectionFactory();

        Set<Produto> materialSetComItemNulo = new HashSet<>();
        materialSetComItemNulo.add(null);
        ClusterEParametrosProjection projectionComMaterialNulo = getClusterEParametrosProjection(
                materialSetComItemNulo,
                Set.of(new Location("L1", "Location 1")));

        IllegalArgumentException materialNuloException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        getConfiguredViewValida(),
                        projectionComMaterialNulo));
        Assertions.assertEquals(
                "Material at index 0 is required in cluster and parameters projection for Planning Book projection.",
                materialNuloException.getMessage());

        ClusterEParametrosProjection projectionComMaterialSemId = getClusterEParametrosProjection(
                Set.of(new Produto(" ")),
                Set.of(new Location("L1", "Location 1")));
        IllegalArgumentException materialSemIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        getConfiguredViewValida(),
                        projectionComMaterialSemId));
        Assertions.assertEquals(
                "Material id is required in cluster and parameters projection for Planning Book projection.",
                materialSemIdException.getMessage());

        Set<Produto> materialSetComDuplicidade = Collections.newSetFromMap(new IdentityHashMap<>());
        materialSetComDuplicidade.add(new Produto("M1"));
        materialSetComDuplicidade.add(new Produto("M1"));
        ClusterEParametrosProjection projectionComMaterialDuplicado = getClusterEParametrosProjection(
                materialSetComDuplicidade,
                Set.of(new Location("L1", "Location 1")));
        IllegalArgumentException materialDuplicadoException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        getConfiguredViewValida(),
                        projectionComMaterialDuplicado));
        Assertions.assertEquals(
                "Duplicate material id M1 found in cluster and parameters projection for Planning Book projection.",
                materialDuplicadoException.getMessage());

        Set<Location> locationSetComItemNulo = new HashSet<>();
        locationSetComItemNulo.add(null);
        ClusterEParametrosProjection projectionComLocationNula = getClusterEParametrosProjection(
                Set.of(new Produto("M1")),
                locationSetComItemNulo);
        IllegalArgumentException locationNulaException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        getConfiguredViewValida(),
                        projectionComLocationNula));
        Assertions.assertEquals(
                "Location at index 0 is required in cluster and parameters projection for Planning Book projection.",
                locationNulaException.getMessage());

        ClusterEParametrosProjection projectionComLocationSemId = getClusterEParametrosProjection(
                Set.of(new Produto("M1")),
                Set.of(new Location(" ", "Location 1")));
        IllegalArgumentException locationSemIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        getConfiguredViewValida(),
                        projectionComLocationSemId));
        Assertions.assertEquals(
                "Location id is required in cluster and parameters projection for Planning Book projection.",
                locationSemIdException.getMessage());

        Set<Location> locationSetComDuplicidade = Collections.newSetFromMap(new IdentityHashMap<>());
        locationSetComDuplicidade.add(new Location("L1", "Location 1"));
        locationSetComDuplicidade.add(new Location("L1", "Location 1"));
        ClusterEParametrosProjection projectionComLocationDuplicada = getClusterEParametrosProjection(
                Set.of(new Produto("M1")),
                locationSetComDuplicidade);
        IllegalArgumentException locationDuplicadaException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        getConfiguredViewValida(),
                        projectionComLocationDuplicada));
        Assertions.assertEquals(
                "Duplicate location id L1 found in cluster and parameters projection for Planning Book projection.",
                locationDuplicadaException.getMessage());

    }

    @Test
    public void getConfiguredViewProjectionShouldRejectNullRepositoryOptionalBeforeProjection() throws Exception {

        ConfiguredViewProjectionFactory configuredViewProjectionFactory =
                new ConfiguredViewProjectionFactory();
        setField(
                configuredViewProjectionFactory,
                "configuredViewRepository",
                getConfiguredViewRepositoryComOptional(null));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        "admin",
                        "Community view",
                        ConfiguredView.TipoView.DEMANDPLANNINGBOOK,
                        new ClusterEParametrosProjection()));

        Assertions.assertEquals(
                "Configured View repository returned null Optional for user admin, view Community view and type DEMANDPLANNINGBOOK.",
                illegalStateException.getMessage());

    }

    @Test
    public void getConfiguredViewProjectionShouldPreserveMissingViewAsFunctionalNoResult() throws Exception {

        ConfiguredViewProjectionFactory configuredViewProjectionFactory =
                new ConfiguredViewProjectionFactory();
        setField(
                configuredViewProjectionFactory,
                "configuredViewRepository",
                getConfiguredViewRepositoryComOptional(Optional.empty()));

        NoResultException noResultException = Assertions.assertThrows(
                NoResultException.class,
                () -> configuredViewProjectionFactory.getConfiguredViewProjection(
                        "admin",
                        "Missing view",
                        ConfiguredView.TipoView.SUPPLYPLANNINGBOOK,
                        new ClusterEParametrosProjection()));

        Assertions.assertEquals(
                "No configured view found for user admin, view Missing view for SUPPLYPLANNINGBOOK",
                noResultException.getMessage());

    }

    private ClusterEParametrosProjection getClusterEParametrosProjection(
            Set<Produto> materialSet,
            Set<Location> locationSet) throws Exception {

        ClusterEParametrosProjection clusterEParametrosProjection = new ClusterEParametrosProjection();
        setField(clusterEParametrosProjection, "materialSet", materialSet);
        setField(clusterEParametrosProjection, "locationSet", locationSet);
        return clusterEParametrosProjection;

    }

    private ConfiguredView getConfiguredViewValida() {

        return new ConfiguredView(new ConfiguredView.ConfiguredViewCompositeKey(
                "admin",
                "Community view",
                ConfiguredView.TipoView.DEMANDPLANNINGBOOK));

    }

    private ConfiguredViewRepository getConfiguredViewRepositoryComOptional(
            Optional<ConfiguredView> configuredViewOptional) {

        return (ConfiguredViewRepository) Proxy.newProxyInstance(
                ConfiguredViewRepository.class.getClassLoader(),
                new Class<?>[]{ConfiguredViewRepository.class},
                (proxy, method, args) -> {
                    if ("findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView".equals(method.getName())) {
                        return configuredViewOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ConfiguredViewRepository para teste de projection factory Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
