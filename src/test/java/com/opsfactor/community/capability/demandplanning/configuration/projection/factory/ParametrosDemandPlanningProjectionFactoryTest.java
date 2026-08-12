package com.opsfactor.community.capability.demandplanning.configuration.projection.factory;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import com.opsfactor.community.capability.demandplanning.configuration.repository.ParametrosDemandPlanNivelClusterRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Contrato Community da factory de parametros de Demand Planning.
 *
 * <p>A factory gera uma projection por combinacao cluster location/material e
 * usa parametros persistidos apenas quando existem. Este teste fixa duas
 * fronteiras importantes para o repositorio aberto: ausencia de parametros
 * continua gerando defaults validos, enquanto snapshots quebrados vindos do
 * repository/projection central falham antes dos mapas e loops paralelos.</p>
 */
public class ParametrosDemandPlanningProjectionFactoryTest {

    @Test
    public void getParametrosDemandPlanProjectionShouldCreateDefaultProjectionWhenRepositoryIsEmpty() throws Exception {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("DP_PROFILE");
        ClusterLocations clusterLocations = getClusterLocations(10L);
        ClusterMateriais clusterMateriaisDemandPlanning =
                getClusterMateriaisDemandPlanning(20L);

        ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory =
                getParametrosDemandPlanningProjectionFactory(
                        getClusterEParametrosProjection(
                                List.of(clusterLocations),
                                List.of(clusterMateriaisDemandPlanning)),
                        List.of());

        ParametrosDemandPlanProjection parametrosDemandPlanProjection =
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjection(
                        perfilExecucaoDemandPlan);
        ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection =
                parametrosDemandPlanProjection.getParametrosDemandPlanNivelClusterProjection(
                        clusterLocations,
                        clusterMateriaisDemandPlanning);

        Assertions.assertNotNull(
                parametrosDemandPlanNivelClusterProjection,
                "Combinacao cluster location/material sem linha persistida deve gerar parametros default.");
        Assertions.assertEquals(
                perfilExecucaoDemandPlan,
                parametrosDemandPlanNivelClusterProjection.getPerfilExecucaoDemandPlan());
        Assertions.assertTrue(
                parametrosDemandPlanNivelClusterProjection.isUseExecutionProfileAutofitModel(),
                "Linha ausente deve materializar o default efetivo que permite o AutoFit do perfil no overlay Enterprise.");
        Assertions.assertEquals(
                1,
                parametrosDemandPlanProjection.getStreamParametrosDemandPlanNivelClusterProjection().count());

    }

    @Test
    public void getParametrosDemandPlanProjectionShouldMaterializeExecutionProfileAutofitOptOut()
            throws Exception {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("DP_PROFILE");
        ClusterLocations clusterLocations = getClusterLocations(10L);
        ClusterMateriais clusterMateriaisDemandPlanning =
                getClusterMateriaisDemandPlanning(20L);
        ParametrosDemandPlanNivelCluster persistedParameters = getParametrosDemandPlanNivelCluster(
                perfilExecucaoDemandPlan,
                clusterMateriaisDemandPlanning.getId(),
                clusterLocations.getId());
        persistedParameters.setUseExecutionProfileAutofitModel(false);

        ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory =
                getParametrosDemandPlanningProjectionFactory(
                        getClusterEParametrosProjection(
                                List.of(clusterLocations),
                                List.of(clusterMateriaisDemandPlanning)),
                        List.of(persistedParameters));

        ParametrosDemandPlanNivelClusterProjection clusterProjection =
                parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjection(
                        perfilExecucaoDemandPlan)
                        .getParametrosDemandPlanNivelClusterProjection(
                                clusterLocations,
                                clusterMateriaisDemandPlanning);

        Assertions.assertFalse(
                clusterProjection.isUseExecutionProfileAutofitModel(),
                "Opt-out persistido deve chegar à projection para que o overlay Enterprise preserve o forecast manual.");

    }

    @Test
    public void executionProfileAutofitOptInShouldDefaultToTrueWhenPersistedValueIsNull() {

        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster =
                new ParametrosDemandPlanNivelCluster();

        Assertions.assertTrue(
                parametrosDemandPlanNivelCluster.usesExecutionProfileAutofitModel(),
                "Null deve preservar compatibilidade com linhas existentes e significar opt-in ao AutoFit do perfil.");

    }

    @Test
    public void getParametrosDemandPlanProjectionShouldRejectMissingProfileBeforeRepository() throws Exception {

        ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory =
                getParametrosDemandPlanningProjectionFactory(
                        getClusterEParametrosProjection(
                                List.of(getClusterLocations(10L)),
                                List.of(getClusterMateriaisDemandPlanning(20L))),
                        List.of());

        IllegalArgumentException missingProfileException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjection(null));
        Assertions.assertEquals(
                "Demand Planning execution profile is required.",
                missingProfileException.getMessage());

        IllegalArgumentException missingProfileIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjection(
                        new PerfilExecucaoDemandPlan("")));
        Assertions.assertEquals(
                "Demand Planning execution profile id is required.",
                missingProfileIdException.getMessage());

    }

    @Test
    public void getParametrosDemandPlanProjectionShouldRejectBrokenClusterProjectionBeforeParallelLoop() throws Exception {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("DP_PROFILE");

        ParametrosDemandPlanningProjectionFactory nullProjectionFactory =
                getParametrosDemandPlanningProjectionFactory(null, List.of());

        IllegalStateException nullProjectionException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> nullProjectionFactory.getParametrosDemandPlanProjection(perfilExecucaoDemandPlan));
        Assertions.assertEquals(
                "Cluster/parameter projection returned null for Demand Planning parameters.",
                nullProjectionException.getMessage());

        ClusterEParametrosProjection clusterEParametrosProjection =
                getClusterEParametrosProjection(
                        List.of(getClusterLocations(10L)),
                        List.of(getClusterMateriaisDemandPlanning(20L)));
        setPrivateField(clusterEParametrosProjection, "clusterLocationsList", null);

        ParametrosDemandPlanningProjectionFactory nullLocationListFactory =
                getParametrosDemandPlanningProjectionFactory(clusterEParametrosProjection, List.of());

        IllegalStateException nullLocationListException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> nullLocationListFactory.getParametrosDemandPlanProjection(perfilExecucaoDemandPlan));
        Assertions.assertEquals(
                "Demand Planning cluster location list returned null collection.",
                nullLocationListException.getMessage());

    }

    @Test
    public void getParametrosDemandPlanProjectionShouldRejectNullRepositoryCollectionBeforeMapBuild() throws Exception {

        ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory =
                getParametrosDemandPlanningProjectionFactory(
                        getClusterEParametrosProjection(
                                List.of(getClusterLocations(10L)),
                                List.of(getClusterMateriaisDemandPlanning(20L))),
                        null);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjection(
                        new PerfilExecucaoDemandPlan("DP_PROFILE")));

        Assertions.assertEquals(
                "Demand Planning cluster-level parameter repository returned null collection for execution profile DP_PROFILE.",
                illegalStateException.getMessage());

    }

    @Test
    public void getParametrosDemandPlanProjectionShouldRejectBrokenRepositoryItemBeforeMapBuild() throws Exception {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("DP_PROFILE");
        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster =
                new ParametrosDemandPlanNivelCluster(
                        new ParametrosDemandPlanNivelCluster.ParametrosDemandPlanNivelClusterCompositeKey(
                                perfilExecucaoDemandPlan,
                                getClusterMateriaisDemandPlanning(20L),
                                getClusterLocations(null)));

        ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory =
                getParametrosDemandPlanningProjectionFactory(
                        getClusterEParametrosProjection(
                                List.of(getClusterLocations(10L)),
                                List.of(getClusterMateriaisDemandPlanning(20L))),
                        List.of(parametrosDemandPlanNivelCluster));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjection(
                        perfilExecucaoDemandPlan));

        Assertions.assertEquals(
                "Demand Planning cluster-level parameter repository returned item without cluster location id at index 0.",
                illegalStateException.getMessage());

    }

    @Test
    public void getParametrosDemandPlanProjectionShouldRejectDuplicatedClusterKeyBeforeMapBuild()
            throws Exception {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("DP_PROFILE");
        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelClusterUm =
                getParametrosDemandPlanNivelCluster(perfilExecucaoDemandPlan, 20L, 10L);
        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelClusterDois =
                getParametrosDemandPlanNivelCluster(perfilExecucaoDemandPlan, 20L, 10L);

        ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory =
                getParametrosDemandPlanningProjectionFactory(
                        getClusterEParametrosProjection(
                                List.of(getClusterLocations(10L)),
                                List.of(getClusterMateriaisDemandPlanning(20L))),
                        List.of(
                                parametrosDemandPlanNivelClusterUm,
                                parametrosDemandPlanNivelClusterDois));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> parametrosDemandPlanningProjectionFactory.getParametrosDemandPlanProjection(
                        perfilExecucaoDemandPlan));

        Assertions.assertEquals(
                "Demand Planning cluster-level parameter repository returned duplicated cluster key 10|20 for execution profile DP_PROFILE.",
                illegalStateException.getMessage());

    }

    private static ParametrosDemandPlanningProjectionFactory getParametrosDemandPlanningProjectionFactory(
            ClusterEParametrosProjection clusterEParametrosProjection,
            List<ParametrosDemandPlanNivelCluster> parametrosDemandPlanNivelClusterList) throws Exception {

        ParametrosDemandPlanningProjectionFactory parametrosDemandPlanningProjectionFactory =
                new ParametrosDemandPlanningProjectionFactory();
        setPrivateField(
                parametrosDemandPlanningProjectionFactory,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory(clusterEParametrosProjection));
        setPrivateField(
                parametrosDemandPlanningProjectionFactory,
                "parametrosDemandPlanNivelClusterRepository",
                getParametrosDemandPlanNivelClusterRepositoryProxy(parametrosDemandPlanNivelClusterList));

        return parametrosDemandPlanningProjectionFactory;

    }

    private static ParametrosDemandPlanNivelClusterRepository getParametrosDemandPlanNivelClusterRepositoryProxy(
            List<ParametrosDemandPlanNivelCluster> parametrosDemandPlanNivelClusterList) {

        return (ParametrosDemandPlanNivelClusterRepository) Proxy.newProxyInstance(
                ParametrosDemandPlanNivelClusterRepository.class.getClassLoader(),
                new Class<?>[]{ParametrosDemandPlanNivelClusterRepository.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return switch (method.getName()) {
                            case "toString" -> "ParametrosDemandPlanNivelClusterRepositoryProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError("Metodo Object inesperado: " + method.getName());
                        };
                    }
                    if ("findByPerfilExecucaoDemandPlanId".equals(method.getName())) {
                        return parametrosDemandPlanNivelClusterList;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    private static ParametrosDemandPlanNivelCluster getParametrosDemandPlanNivelCluster(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            Long clusterMateriaisDemandPlanningId,
            Long clusterLocationsId) {

        return new ParametrosDemandPlanNivelCluster(
                new ParametrosDemandPlanNivelCluster.ParametrosDemandPlanNivelClusterCompositeKey(
                        perfilExecucaoDemandPlan,
                        getClusterMateriaisDemandPlanning(clusterMateriaisDemandPlanningId),
                        getClusterLocations(clusterLocationsId)));

    }

    private static ClusterEParametrosProjection getClusterEParametrosProjection(
            List<ClusterLocations> clusterLocationsList,
            List<ClusterMateriais> clusterMateriaisDemandPlanningList) throws Exception {

        ClusterEParametrosProjection clusterEParametrosProjection = new ClusterEParametrosProjection();
        setPrivateField(clusterEParametrosProjection, "parametrosGlobais", new ParametrosGlobais());
        setPrivateField(clusterEParametrosProjection, "clusterLocationsList", clusterLocationsList);
        setPrivateField(
                clusterEParametrosProjection,
                "clusterProdutosDemandPlanningList",
                clusterMateriaisDemandPlanningList);

        return clusterEParametrosProjection;

    }

    private static ClusterLocations getClusterLocations(Long id) {

        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(id);
        return clusterLocations;

    }

    private static ClusterMateriais getClusterMateriaisDemandPlanning(Long id) {

        ClusterMateriais clusterMateriaisDemandPlanning =
                new ClusterMateriais();
        clusterMateriaisDemandPlanning.setId(id);
        return clusterMateriaisDemandPlanning;

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

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

}
