package com.opsfactor.community.capability.configuration.projection.parametros;

import com.opsfactor.community.capability.configuration.projection.cluster.ClusterizacaoProjectionFactory;
import com.opsfactor.community.capability.configuration.projection.cluster.ClusterizacaoProjection;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.cluster.repository.material.ClusterMateriaisRepository;
import com.opsfactor.community.capability.configuration.repository.ParametrosProdutoLocationRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsPaisEstadoRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsTipoLocationRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.produto.RegraAlocacaoClusterProdutosStatusRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.produto.RegraAlocacaoClusterProdutosCaracteristicaRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaLocationRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaMaterialRepository;
import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contratos Community da factory central de parametros e master data basico.
 *
 * <p>A `CadastroEParametrosProjectionFactory` materializa o snapshot mais usado
 * por Demand Planning, Supply Planning e projections auxiliares. Esses testes
 * protegem a fronteira inicial de material/location: a projection nao deve
 * chegar aos repositories de clusters nem aos `Collectors.toMap(...)` quando o
 * snapshot base vem nulo, com item nulo, sem id ou com id funcional duplicado.</p>
 */
class CadastroEParametrosProjectionFactoryCommunityContractTest {

    @Test
    void getParametrosProjectionBaseShouldRejectBrokenMaterialSnapshotBeforeDependencies() {

        CadastroEParametrosProjectionFactory cadastroEParametrosProjectionFactory =
                new CadastroEParametrosProjectionFactory();

        assertIllegalArgumentMessage(
                () -> cadastroEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        null),
                "Cluster and parameters projection requires material snapshot collection.");
        assertIllegalArgumentMessage(
                () -> cadastroEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        getSnapshotComItemNulo()),
                "Cluster and parameters projection material snapshot item at index 0 is required.");
        assertIllegalArgumentMessage(
                () -> cadastroEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        Set.of(new Produto())),
                "Cluster and parameters projection material snapshot item at index 0 must have id.");
        assertIllegalArgumentMessage(
                () -> cadastroEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        getMateriaisComIdDuplicado()),
                "Cluster and parameters projection material snapshot has duplicated id MAT.");

    }

    @Test
    void getParametrosProjectionBaseShouldRejectBrokenLocationSnapshotBeforeDependencies() {

        CadastroEParametrosProjectionFactory cadastroEParametrosProjectionFactory =
                new CadastroEParametrosProjectionFactory();

        assertIllegalArgumentMessage(
                () -> cadastroEParametrosProjectionFactory.getParametrosProjectionBase(
                        null,
                        Set.of(new Produto("MAT"))),
                "Cluster and parameters projection requires location snapshot collection.");
        assertIllegalArgumentMessage(
                () -> cadastroEParametrosProjectionFactory.getParametrosProjectionBase(
                        getSnapshotComItemNulo(),
                        Set.of(new Produto("MAT"))),
                "Cluster and parameters projection location snapshot item at index 0 is required.");
        assertIllegalArgumentMessage(
                () -> cadastroEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location()),
                        Set.of(new Produto("MAT"))),
                "Cluster and parameters projection location snapshot item at index 0 must have id.");
        assertIllegalArgumentMessage(
                () -> cadastroEParametrosProjectionFactory.getParametrosProjectionBase(
                        getLocationsComIdDuplicado(),
                        Set.of(new Produto("MAT"))),
                "Cluster and parameters projection location snapshot has duplicated id LOC.");

    }

    @Test
    void getParametrosProjectionBaseShouldRejectDuplicatedMaterialClusterIdBeforeMapIndexing() throws Exception {

        ClusterizacaoProjectionFactory cadastroEParametrosProjectionFactory =
                getClusteringFactory(
                        List.of(
                                getClusterProdutosDemandPlanning(1L),
                                getClusterProdutosDemandPlanning(1L)),
                        List.of());

        assertIllegalStateMessage(
                () -> cadastroEParametrosProjectionFactory.getClusterizacaoProjectionBase("LEGADO", emptyMasters()),
                "Demand Planning material cluster repository has duplicated id 1 for Cluster and parameters projection.");

    }

    @Test
    void getParametrosProjectionBaseShouldRejectDuplicatedLocationClusterIdBeforeMapIndexing() throws Exception {

        ClusterizacaoProjectionFactory cadastroEParametrosProjectionFactory =
                getClusteringFactory(
                        List.of(),
                        getClusterLocationsComIdDuplicadoParaRepository());

        assertIllegalStateMessage(
                () -> cadastroEParametrosProjectionFactory.getClusterizacaoProjectionBase("LEGADO", emptyMasters()),
                "Location cluster repository has duplicated id 1 for Cluster and parameters projection.");

    }

    @Test
    void getParametrosProjectionBaseShouldRejectDuplicatedMaterialLocationParameterBeforeNestedMapIndexing() throws Exception {

        Produto material = new Produto("MAT");
        Location location = new Location("LOC");
        CadastroEParametrosProjectionFactory cadastroEParametrosProjectionFactory =
                getFactoryComSnapshotsDeRepositories(
                        List.of(),
                        List.of(),
                        List.of(
                                new ParametrosProdutoLocation(
                                        new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(
                                                material,
                                                location)),
                                new ParametrosProdutoLocation(
                                        new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(
                                                new Produto("MAT"),
                                                new Location("LOC")))));

        assertIllegalStateMessage(
                () -> cadastroEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(location),
                        Set.of(material)),
                "Material/location parameter repository has duplicated key material MAT / location LOC for Cluster and parameters projection.");

    }

    private static CadastroEParametrosProjectionFactory getFactoryComSnapshotsDeRepositories(
            List<ClusterMateriais> clusterMateriaisList,
            List<ClusterLocations> clusterLocationsList,
            List<ParametrosProdutoLocation> parametrosProdutoLocationList)
            throws Exception {

        CadastroEParametrosProjectionFactory cadastroEParametrosProjectionFactory =
                new CadastroEParametrosProjectionFactory();
        ClusterMateriaisRepository clusterMateriaisRepository =
                Mockito.mock(ClusterMateriaisRepository.class);
        RegraAlocacaoClusterProdutosStatusRepository regraAlocacaoClusterProdutosStatusRepository =
                Mockito.mock(RegraAlocacaoClusterProdutosStatusRepository.class);
        RegraAlocacaoClusterProdutosCaracteristicaRepository regraAlocacaoClusterProdutosCaracteristicaRepository =
                Mockito.mock(RegraAlocacaoClusterProdutosCaracteristicaRepository.class);
        ClusterLocationsRepository clusterLocationsRepository =
                Mockito.mock(ClusterLocationsRepository.class);
        RegraAlocacaoClusterLocationsPaisEstadoRepository regraAlocacaoClusterLocationsPaisEstadoRepository =
                Mockito.mock(RegraAlocacaoClusterLocationsPaisEstadoRepository.class);
        RegraAlocacaoClusterLocationsTipoLocationRepository regraAlocacaoClusterLocationsTipoLocationRepository =
                Mockito.mock(RegraAlocacaoClusterLocationsTipoLocationRepository.class);
        ParametrosGlobaisService parametrosGlobaisService =
                Mockito.mock(ParametrosGlobaisService.class);
        ParametrosProdutoLocationRepository parametrosProdutoLocationRepository =
                Mockito.mock(ParametrosProdutoLocationRepository.class);
        CaracteristicaMaterialRepository caracteristicaMaterialRepository =
                Mockito.mock(CaracteristicaMaterialRepository.class);
        CaracteristicaLocationRepository caracteristicaLocationRepository =
                Mockito.mock(CaracteristicaLocationRepository.class);

        if (clusterMateriaisList.isEmpty()) {
            ClusterMateriais materialDefault = getClusterProdutosDemandPlanning(90L);
            materialDefault.setPadrao(true);
            clusterMateriaisList = List.of(materialDefault);
        }
        if (clusterLocationsList.isEmpty()) {
            ClusterLocations locationDefault = getClusterLocations(90L);
            locationDefault.setPadrao(true);
            clusterLocationsList = List.of(locationDefault);
        }

        Mockito.when(clusterMateriaisRepository.findAllByPerfilClusterizacaoId(Mockito.anyString()))
                .thenReturn(clusterMateriaisList);
        Mockito.when(regraAlocacaoClusterProdutosStatusRepository.findAllForClusteringProfile(Mockito.anyString()))
                .thenReturn(List.of());
        Mockito.when(regraAlocacaoClusterProdutosCaracteristicaRepository.findAllForClusteringProfile(Mockito.anyString()))
                .thenReturn(List.of());
        Mockito.when(clusterLocationsRepository.findAllByPerfilClusterizacaoId(Mockito.anyString()))
                .thenReturn(clusterLocationsList);
        Mockito.when(regraAlocacaoClusterLocationsPaisEstadoRepository.findAllForClusteringProfile(Mockito.anyString()))
                .thenReturn(List.of());
        Mockito.when(regraAlocacaoClusterLocationsTipoLocationRepository.findAllForClusteringProfile(Mockito.anyString()))
                .thenReturn(List.of());
        Mockito.when(parametrosGlobaisService.getParametrosGlobais())
                .thenReturn(Mockito.mock(ParametrosGlobais.class));
        Mockito.when(parametrosProdutoLocationRepository.customFindAllComFetchAtributosManyToOne())
                .thenReturn(parametrosProdutoLocationList);
        Mockito.when(caracteristicaMaterialRepository.findAllWithValues())
                .thenReturn(List.of());
        Mockito.when(caracteristicaLocationRepository.findAllWithValues())
                .thenReturn(List.of());

        setPrivateField(
                cadastroEParametrosProjectionFactory,
                "parametrosGlobaisService",
                parametrosGlobaisService);
        setPrivateField(
                cadastroEParametrosProjectionFactory,
                "parametrosProdutoLocationRepository",
                parametrosProdutoLocationRepository);
        setPrivateField(
                cadastroEParametrosProjectionFactory,
                "caracteristicaMaterialRepository",
                caracteristicaMaterialRepository);
        setPrivateField(
                cadastroEParametrosProjectionFactory,
                "caracteristicaLocationRepository",
                caracteristicaLocationRepository);

        return cadastroEParametrosProjectionFactory;

    }

    private static ClusterMateriais getClusterProdutosDemandPlanning(Long id) {

        ClusterMateriais clusterMateriais =
                new ClusterMateriais("Cluster Material", false, 1);
        clusterMateriais.setId(id);
        return clusterMateriais;

    }

    private static ClusterLocations getClusterLocations(Long id) {

        ClusterLocations clusterLocations =
                new ClusterLocations("Cluster Location", false, 1);
        clusterLocations.setId(id);
        return clusterLocations;

    }

    private static List<ClusterLocations> getClusterLocationsComIdDuplicadoParaRepository() {

        return List.of(
                getClusterLocations(1L),
                getClusterLocations(1L));

    }

    private static <T> List<T> getSnapshotComItemNulo() {

        List<T> snapshotList = new ArrayList<>();
        snapshotList.add(null);
        return snapshotList;

    }

    private static List<Produto> getMateriaisComIdDuplicado() {

        return List.of(
                new Produto("MAT"),
                new Produto("MAT"));

    }

    private static List<Location> getLocationsComIdDuplicado() {

        return List.of(
                new Location("LOC"),
                new Location("LOC"));

    }

    private static void assertIllegalArgumentMessage(
            Executable executable,
            String expectedMessage) {

        IllegalArgumentException illegalArgumentException =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        executable);

        Assertions.assertEquals(
                expectedMessage,
                illegalArgumentException.getMessage());

    }

    private static void assertIllegalStateMessage(
            Executable executable,
            String expectedMessage) {

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        executable);

        Assertions.assertEquals(
                expectedMessage,
                illegalStateException.getMessage());

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }


    @Test
    void shouldKeepIdenticalLocalIdsInSeparateProfileSnapshots() throws Exception {

        ClusterMateriais materialA = getClusterProdutosDemandPlanning(1L);
        materialA.setPadrao(true);
        materialA.setPerfilClusterizacao(new PerfilClusterizacao("A", "Perfil A"));
        ClusterLocations locationA = getClusterLocations(1L);
        locationA.setPadrao(true);
        locationA.setPerfilClusterizacao(materialA.getPerfilClusterizacao());
        ClusterizacaoProjectionFactory factory = getClusteringFactory(
                List.of(materialA), List.of(locationA));
        ClusterMateriaisRepository materials = (ClusterMateriaisRepository) getPrivateField(factory, "clusterMateriaisDemandPlanningRepository");
        ClusterLocationsRepository locations = (ClusterLocationsRepository) getPrivateField(factory, "clusterLocationsRepository");
        ClusterMateriais materialB = getClusterProdutosDemandPlanning(1L);
        materialB.setPadrao(true);
        materialB.setPerfilClusterizacao(new PerfilClusterizacao("B", "Perfil B"));
        ClusterLocations locationB = getClusterLocations(1L);
        locationB.setPadrao(true);
        locationB.setPerfilClusterizacao(materialB.getPerfilClusterizacao());
        Mockito.when(materials.findAllByPerfilClusterizacaoId("B")).thenReturn(List.of(materialB));
        Mockito.when(locations.findAllByPerfilClusterizacaoId("B")).thenReturn(List.of(locationB));

        ClusterizacaoProjection snapshotA = factory.getClusterizacaoProjectionBase("A", emptyMasters());
        ClusterizacaoProjection snapshotB = factory.getClusterizacaoProjectionBase("B", emptyMasters());

        Assertions.assertEquals("A", snapshotA.getPerfilClusterizacaoId());
        Assertions.assertEquals("B", snapshotB.getPerfilClusterizacaoId());
        Assertions.assertSame(materialA, snapshotA.getClusterMateriaisList().getFirst());
        Assertions.assertSame(materialB, snapshotB.getClusterMateriaisList().getFirst());
        Assertions.assertNotEquals(materialA, materialB);
        Mockito.verify(materials, Mockito.never()).findAll();
        Mockito.verify(locations, Mockito.never()).customFindAll();

    }

    @Test
    void shouldRejectProfileWithoutItsOwnMaterialDefault() throws Exception {

        ClusterMateriais nonDefault = getClusterProdutosDemandPlanning(1L);
        ClusterizacaoProjectionFactory factory = getClusteringFactory(
                List.of(nonDefault), List.of());
        IllegalStateException error = Assertions.assertThrows(IllegalStateException.class,
                () -> factory.getClusterizacaoProjectionBase("NOVO", emptyMasters()));
        Assertions.assertTrue(error.getMessage().contains("NOVO"));
        Assertions.assertTrue(error.getMessage().contains("encontrados 0"));

    }

    private static CadastroEParametrosProjection emptyMasters() {
        var masters = new CadastroEParametrosProjection();
        masters.materialSet = Set.of();
        masters.locationSet = Set.of();
        masters.parametrosGlobais = new ParametrosGlobais();
        return masters;
    }

    private static ClusterizacaoProjectionFactory getClusteringFactory(
            List<ClusterMateriais> materials, List<ClusterLocations> locations) throws Exception {
        var factory = new ClusterizacaoProjectionFactory();
        var materialRepository = Mockito.mock(ClusterMateriaisRepository.class);
        var locationRepository = Mockito.mock(ClusterLocationsRepository.class);
        if (materials.isEmpty()) {
            var value = getClusterProdutosDemandPlanning(90L);
            value.setPadrao(true);
            materials = List.of(value);
        }
        if (locations.isEmpty()) {
            var value = getClusterLocations(90L);
            value.setPadrao(true);
            locations = List.of(value);
        }
        Mockito.when(materialRepository.findAllByPerfilClusterizacaoId(Mockito.anyString())).thenReturn(materials);
        Mockito.when(locationRepository.findAllByPerfilClusterizacaoId(Mockito.anyString())).thenReturn(locations);
        setPrivateField(factory, "clusterMateriaisDemandPlanningRepository", materialRepository);
        setPrivateField(factory, "clusterLocationsRepository", locationRepository);
        setPrivateField(factory, "regraAlocacaoClusterProdutosStatusRepository", Mockito.mock(RegraAlocacaoClusterProdutosStatusRepository.class));
        setPrivateField(factory, "regraAlocacaoClusterProdutosCaracteristicaRepository", Mockito.mock(RegraAlocacaoClusterProdutosCaracteristicaRepository.class));
        setPrivateField(factory, "regraAlocacaoClusterLocationsPaisEstadoRepository", Mockito.mock(RegraAlocacaoClusterLocationsPaisEstadoRepository.class));
        setPrivateField(factory, "regraAlocacaoClusterLocationsTipoLocationRepository", Mockito.mock(RegraAlocacaoClusterLocationsTipoLocationRepository.class));
        return factory;
    }

    private static Object getPrivateField(Object target, String name) throws Exception {

        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);

    }
}
