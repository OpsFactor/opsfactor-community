package com.opsfactor.community.capability.configuration.projection.parametros;

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
import com.opsfactor.community.capability.cluster.service.ClusteringService;
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
 * <p>A `ClusterEParametrosProjectionFactory` materializa o snapshot mais usado
 * por Demand Planning, Supply Planning e projections auxiliares. Esses testes
 * protegem a fronteira inicial de material/location: a projection nao deve
 * chegar aos repositories de clusters nem aos `Collectors.toMap(...)` quando o
 * snapshot base vem nulo, com item nulo, sem id ou com id funcional duplicado.</p>
 */
class ClusterEParametrosProjectionFactoryCommunityContractTest {

    @Test
    void getParametrosProjectionBaseShouldRejectBrokenMaterialSnapshotBeforeDependencies() {

        ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory =
                new ClusterEParametrosProjectionFactory();

        assertIllegalArgumentMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        null),
                "Cluster and parameters projection requires material snapshot collection.");
        assertIllegalArgumentMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        getSnapshotComItemNulo()),
                "Cluster and parameters projection material snapshot item at index 0 is required.");
        assertIllegalArgumentMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        Set.of(new Produto())),
                "Cluster and parameters projection material snapshot item at index 0 must have id.");
        assertIllegalArgumentMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        getMateriaisComIdDuplicado()),
                "Cluster and parameters projection material snapshot has duplicated id MAT.");

    }

    @Test
    void getParametrosProjectionBaseShouldRejectBrokenLocationSnapshotBeforeDependencies() {

        ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory =
                new ClusterEParametrosProjectionFactory();

        assertIllegalArgumentMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        null,
                        Set.of(new Produto("MAT"))),
                "Cluster and parameters projection requires location snapshot collection.");
        assertIllegalArgumentMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        getSnapshotComItemNulo(),
                        Set.of(new Produto("MAT"))),
                "Cluster and parameters projection location snapshot item at index 0 is required.");
        assertIllegalArgumentMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location()),
                        Set.of(new Produto("MAT"))),
                "Cluster and parameters projection location snapshot item at index 0 must have id.");
        assertIllegalArgumentMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        getLocationsComIdDuplicado(),
                        Set.of(new Produto("MAT"))),
                "Cluster and parameters projection location snapshot has duplicated id LOC.");

    }

    @Test
    void getParametrosProjectionBaseShouldRejectDuplicatedMaterialClusterIdBeforeMapIndexing() throws Exception {

        ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory =
                getFactoryComSnapshotsDeRepositories(
                        List.of(
                                getClusterProdutosDemandPlanning(1L),
                                getClusterProdutosDemandPlanning(1L)),
                        List.of(),
                        List.of());

        assertIllegalStateMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        Set.of(new Produto("MAT"))),
                "Demand Planning material cluster repository has duplicated id 1 for Cluster and parameters projection.");

    }

    @Test
    void getParametrosProjectionBaseShouldRejectDuplicatedLocationClusterIdBeforeMapIndexing() throws Exception {

        ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory =
                getFactoryComSnapshotsDeRepositories(
                        List.of(),
                        getClusterLocationsComIdDuplicadoParaRepository(),
                        List.of());

        assertIllegalStateMessage(
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(new Location("LOC")),
                        Set.of(new Produto("MAT"))),
                "Location cluster repository has duplicated id 1 for Cluster and parameters projection.");

    }

    @Test
    void getParametrosProjectionBaseShouldRejectDuplicatedMaterialLocationParameterBeforeNestedMapIndexing() throws Exception {

        Produto material = new Produto("MAT");
        Location location = new Location("LOC");
        ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory =
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
                () -> clusterEParametrosProjectionFactory.getParametrosProjectionBase(
                        Set.of(location),
                        Set.of(material)),
                "Material/location parameter repository has duplicated key material MAT / location LOC for Cluster and parameters projection.");

    }

    private static ClusterEParametrosProjectionFactory getFactoryComClusterMaterialRepositoryResult(
            List<ClusterMateriais> clusterMateriaisList)
            throws Exception {

        ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory =
                new ClusterEParametrosProjectionFactory();
        ClusterMateriaisRepository clusterMateriaisRepository =
                Mockito.mock(ClusterMateriaisRepository.class);
        Mockito.when(clusterMateriaisRepository.findAll())
                .thenReturn(clusterMateriaisList);

        setPrivateField(
                clusterEParametrosProjectionFactory,
                "clusteringService",
                Mockito.mock(ClusteringService.class));
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "clusterMateriaisDemandPlanningRepository",
                clusterMateriaisRepository);

        return clusterEParametrosProjectionFactory;

    }

    private static ClusterEParametrosProjectionFactory getFactoryComSnapshotsDeRepositories(
            List<ClusterMateriais> clusterMateriaisList,
            List<ClusterLocations> clusterLocationsList,
            List<ParametrosProdutoLocation> parametrosProdutoLocationList)
            throws Exception {

        ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory =
                new ClusterEParametrosProjectionFactory();
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

        Mockito.when(clusterMateriaisRepository.findAll())
                .thenReturn(clusterMateriaisList);
        Mockito.when(regraAlocacaoClusterProdutosStatusRepository.findAll())
                .thenReturn(List.of());
        Mockito.when(regraAlocacaoClusterProdutosCaracteristicaRepository.findAll())
                .thenReturn(List.of());
        Mockito.when(clusterLocationsRepository.customFindAll())
                .thenReturn(clusterLocationsList);
        Mockito.when(regraAlocacaoClusterLocationsPaisEstadoRepository.findAll())
                .thenReturn(List.of());
        Mockito.when(regraAlocacaoClusterLocationsTipoLocationRepository.findAll())
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
                clusterEParametrosProjectionFactory,
                "clusteringService",
                Mockito.mock(ClusteringService.class));
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "clusterMateriaisDemandPlanningRepository",
                clusterMateriaisRepository);
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "regraAlocacaoClusterProdutosStatusRepository",
                regraAlocacaoClusterProdutosStatusRepository);
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "regraAlocacaoClusterProdutosCaracteristicaRepository",
                regraAlocacaoClusterProdutosCaracteristicaRepository);
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "clusterLocationsRepository",
                clusterLocationsRepository);
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "regraAlocacaoClusterLocationsPaisEstadoRepository",
                regraAlocacaoClusterLocationsPaisEstadoRepository);
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "regraAlocacaoClusterLocationsTipoLocationRepository",
                regraAlocacaoClusterLocationsTipoLocationRepository);
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "parametrosGlobaisService",
                parametrosGlobaisService);
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "parametrosProdutoLocationRepository",
                parametrosProdutoLocationRepository);
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "caracteristicaMaterialRepository",
                caracteristicaMaterialRepository);
        setPrivateField(
                clusterEParametrosProjectionFactory,
                "caracteristicaLocationRepository",
                caracteristicaLocationRepository);

        return clusterEParametrosProjectionFactory;

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

}
