package com.opsfactor.community.capability.cluster.service;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.cluster.repository.material.ClusterProdutosDemandPlanningRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Optional;

/**
 * Contratos Community do service minimo de clusterizacao.
 *
 * <p>O cluster default de materiais para Demand Planning é parte do bootstrap
 * funcional Community. Ele precisa voltar com id e descrição canônica porque
 * os parametros de forecast são vinculados por cluster material/location.</p>
 */
public class ClusteringServiceCommunityContractTest {

    private static final String CLUSTER_PADRAO_HASH_DEMAND_PLANNING = "Default Material DP Cluster";

    @Test
    public void getClusterProdutosDemandPlanningDefaultShouldReturnExistingDefaultCluster() throws Exception {

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanning =
                getClusterProdutosDemandPlanningDefault(1L);
        RepositoryState repositoryState =
                new RepositoryState(Optional.of(clusterProdutosDemandPlanning), null);
        ClusteringService clusteringService = getClusteringService(repositoryState);

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanningRetornado =
                clusteringService.getClusterProdutosDemandPlanningDefault();

        Assertions.assertSame(clusterProdutosDemandPlanning, clusterProdutosDemandPlanningRetornado);
        Assertions.assertFalse(repositoryState.saveCalled);

    }

    @Test
    public void getClusterProdutosDemandPlanningDefaultShouldCreateMissingDefaultCluster() throws Exception {

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanningSalvo =
                getClusterProdutosDemandPlanningDefault(1L);
        RepositoryState repositoryState =
                new RepositoryState(Optional.empty(), clusterProdutosDemandPlanningSalvo);
        ClusteringService clusteringService = getClusteringService(repositoryState);

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanningRetornado =
                clusteringService.getClusterProdutosDemandPlanningDefault();

        Assertions.assertSame(clusterProdutosDemandPlanningSalvo, clusterProdutosDemandPlanningRetornado);
        Assertions.assertTrue(repositoryState.saveCalled);
        Assertions.assertEquals(
                CLUSTER_PADRAO_HASH_DEMAND_PLANNING,
                repositoryState.savedEntity.getDescricao());
        Assertions.assertTrue(repositoryState.savedEntity.getPadrao());

    }

    @Test
    public void getClusterProdutosDemandPlanningDefaultShouldRejectNullLookupOptional() throws Exception {

        RepositoryState repositoryState =
                new RepositoryState(null, getClusterProdutosDemandPlanningDefault(1L));
        ClusteringService clusteringService = getClusteringService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                clusteringService::getClusterProdutosDemandPlanningDefault);

        Assertions.assertEquals(
                "Default material DP cluster lookup result is required.",
                illegalArgumentException.getMessage());
        Assertions.assertFalse(repositoryState.saveCalled);

    }

    @Test
    public void getClusterProdutosDemandPlanningDefaultShouldRejectExistingClusterWithoutId() throws Exception {

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanning =
                getClusterProdutosDemandPlanningDefault(null);
        RepositoryState repositoryState =
                new RepositoryState(Optional.of(clusterProdutosDemandPlanning), null);
        ClusteringService clusteringService = getClusteringService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                clusteringService::getClusterProdutosDemandPlanningDefault);

        Assertions.assertEquals(
                "Default material DP cluster must have an id.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getClusterProdutosDemandPlanningDefaultShouldRejectNullSavedCluster() throws Exception {

        RepositoryState repositoryState = new RepositoryState(Optional.empty(), null);
        ClusteringService clusteringService = getClusteringService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                clusteringService::getClusterProdutosDemandPlanningDefault);

        Assertions.assertEquals(
                "Default material DP cluster is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getClusterProdutosDemandPlanningDefaultShouldRejectSavedClusterWithWrongDescription() throws Exception {

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanning =
                getClusterProdutosDemandPlanningDefault(1L);
        clusterProdutosDemandPlanning.setDescricao("Broken Cluster");
        RepositoryState repositoryState =
                new RepositoryState(Optional.empty(), clusterProdutosDemandPlanning);
        ClusteringService clusteringService = getClusteringService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                clusteringService::getClusterProdutosDemandPlanningDefault);

        Assertions.assertEquals(
                "Default material DP cluster must have description Default Material DP Cluster.",
                illegalArgumentException.getMessage());

    }

    private static ClusteringService getClusteringService(
            RepositoryState repositoryState) throws Exception {

        ClusteringService clusteringService = new ClusteringService();
        setField(
                clusteringService,
                "clusterMateriaisDemandPlanningRepository",
                getClusterProdutosDemandPlanningRepository(repositoryState));
        return clusteringService;

    }

    private static ClusterProdutosDemandPlanningRepository getClusterProdutosDemandPlanningRepository(
            RepositoryState repositoryState) {

        return (ClusterProdutosDemandPlanningRepository) Proxy.newProxyInstance(
                ClusterProdutosDemandPlanningRepository.class.getClassLoader(),
                new Class<?>[]{ClusterProdutosDemandPlanningRepository.class},
                (proxy, method, args) -> {
                    if ("findByDescricao".equals(method.getName())) {
                        return repositoryState.findByDescricaoResult;
                    }
                    if ("save".equals(method.getName())) {
                        repositoryState.saveCalled = true;
                        repositoryState.savedEntity = (ClusterProdutosDemandPlanning) args[0];
                        return repositoryState.saveResult;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ClusterProdutosDemandPlanningRepository test double";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new AssertionError(
                            "Repository method should not be called by ClusteringService test: "
                                    + method.getName());
                });

    }

    private static ClusterProdutosDemandPlanning getClusterProdutosDemandPlanningDefault(
            Long id) {

        ClusterProdutosDemandPlanning clusterProdutosDemandPlanning =
                new ClusterProdutosDemandPlanning(
                        CLUSTER_PADRAO_HASH_DEMAND_PLANNING,
                        true,
                        9999999);
        clusterProdutosDemandPlanning.setId(id);
        return clusterProdutosDemandPlanning;

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static class RepositoryState {

        private final Optional<ClusterProdutosDemandPlanning> findByDescricaoResult;

        private final ClusterProdutosDemandPlanning saveResult;

        private boolean saveCalled;

        private ClusterProdutosDemandPlanning savedEntity;

        private RepositoryState(
                Optional<ClusterProdutosDemandPlanning> findByDescricaoResult,
                ClusterProdutosDemandPlanning saveResult) {

            this.findByDescricaoResult = findByDescricaoResult;
            this.saveResult = saveResult;

        }

    }

}
