package com.opsfactor.community.capability.configuration.projection.parametros;

import com.opsfactor.community.capability.configuration.projection.cluster.ClusterizacaoProjectionFactory;
import com.opsfactor.community.capability.configuration.projection.cluster.ClusterizacaoProjection;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.cluster.repository.material.ClusterMateriaisRepository;
import com.opsfactor.community.capability.configuration.repository.ParametrosProdutoLocationRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsPaisEstadoRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsTipoLocationRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.produto.RegraAlocacaoClusterProdutosCaracteristicaRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.produto.RegraAlocacaoClusterProdutosStatusRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.masterdata.product.material.service.MaterialService;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaLocationRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaMaterialRepository;
import com.opsfactor.community.capability.demandplanning.configuration.repository.ParametrosDemandPlanNivelClusterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Exercises actual Spring cache interception, including repository invalidation. */
class ProjectionProfileCacheIsolationTest {

    @Test
    void identicalLocalClusterIdsAreIsolatedByProfileAndMemoized() {

        try (var context = newCacheContext()) {
            var factory = context.getBean(ClusterizacaoProjectionFactory.class);
            var a = factory.getClusterizacaoProjectionDeCache("PROFILE_A");
            var b = factory.getClusterizacaoProjectionDeCache("PROFILE_B");
            var material = a.getCadastroEParametrosProjection().getMaterialPersistido("M1");

            assertSame(a, factory.getClusterizacaoProjectionDeCache("PROFILE_A"));
            assertNotSame(a, b);
            assertSame(a.getCadastroEParametrosProjection(), b.getCadastroEParametrosProjection());
            assertEquals(7L, a.getClusterProdutosDemandPlanning(material).getId());
            assertEquals(7L, b.getClusterProdutosDemandPlanning(material).getId());
            assertNotEquals(a.getClusterProdutosDemandPlanning(material), b.getClusterProdutosDemandPlanning(material));
            verify(context.getBean(ClusterMateriaisRepository.class), times(1)).findAllByPerfilClusterizacaoId("PROFILE_A");
            verify(context.getBean(ClusterMateriaisRepository.class), times(1)).findAllByPerfilClusterizacaoId("PROFILE_B");
            assertEquals(1, context.getBean(Source.class).masterLoads.get());
        }

    }

    @Test
    void productLocationMutationEvictsGenericDataAndEveryProfile() {

        try (var context = newCacheContext()) {
            var factory = context.getBean(ClusterizacaoProjectionFactory.class);
            var beforeA = factory.getClusterizacaoProjectionDeCache("PROFILE_A");
            var beforeB = factory.getClusterizacaoProjectionDeCache("PROFILE_B");
            context.getBean(Source.class).revision.incrementAndGet();

            // This invokes the production repository's actual @CacheEvict contract.
            context.getBean(ParametrosProdutoLocationRepository.class).save(new ParametrosProdutoLocation());

            var afterA = factory.getClusterizacaoProjectionDeCache("PROFILE_A");
            var afterB = factory.getClusterizacaoProjectionDeCache("PROFILE_B");
            assertNotSame(beforeA, afterA);
            assertNotSame(beforeB, afterB);
            assertNotSame(beforeA.getCadastroEParametrosProjection(), afterA.getCadastroEParametrosProjection());
            assertSame(afterA.getCadastroEParametrosProjection(), afterB.getCadastroEParametrosProjection());
            assertTrue(afterA.getClusterProdutosPadraoDP().getDescricao().endsWith("2"));
            assertEquals(2, context.getBean(Source.class).masterLoads.get());
        }

    }

    @Test
    void publishedClassificationRejectsForeignProfilesAndIndexMutation() {

        try (var context = newCacheContext()) {
            var factory = context.getBean(ClusterizacaoProjectionFactory.class);
            var a = factory.getClusterizacaoProjectionDeCache("PROFILE_A");
            var b = factory.getClusterizacaoProjectionDeCache("PROFILE_B");
            assertThrows(IllegalArgumentException.class,
                    () -> a.getLocationsDeClusterLocations(b.getClusterLocationsPadrao(), false));
            assertThrows(IllegalArgumentException.class,
                    () -> a.getMateriaisDeClusterProdutos(b.getClusterProdutosPadraoDP(), false));
            assertThrows(UnsupportedOperationException.class, () -> a.getClusterMateriaisDemandPlanningMap().clear());
            assertThrows(UnsupportedOperationException.class, () -> a.getClusterLocationsList().clear());
        }

    }

    @Test
    void supplyNetworkCarriesOnlyUnversionedMasterParameters() {

        var fields = Arrays.stream(SupplyNetworkProjection.class.getDeclaredFields()).toList();
        assertTrue(fields.stream().anyMatch(field -> field.getType() == CadastroEParametrosProjection.class));
        assertFalse(fields.stream().anyMatch(field -> field.getType() == ClusterizacaoProjection.class));
        assertFalse(Arrays.stream(CadastroEParametrosProjection.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("perfilClusterizacaoId")));

    }

    @Test
    void forecastCopiesNeverShareMutableRunSelections() {

        var original = new ParametrosForecastProjection(null, null, null, null,
                null, null, null, null, null, null);
        original.setSelectedExternalSeriesIds(new HashSet<>(Set.of("WEATHER")));
        original.setRegressoresFixosReceitaSharding(new HashSet<>(Set.of("EVENT")));
        var a = new ParametrosForecastProjection(original, new ParametrosGlobais());
        var b = new ParametrosForecastProjection(original, new ParametrosGlobais());

        a.getSelectedExternalSeriesIds().clear();
        a.getRegressoresFixosReceitaSharding().add("PRICE");
        assertEquals(Set.of("WEATHER"), original.getSelectedExternalSeriesIds());
        assertEquals(Set.of("WEATHER"), b.getSelectedExternalSeriesIds());
        assertEquals(Set.of("EVENT"), b.getRegressoresFixosReceitaSharding());

    }

    /** Service doubles are pre-registered singletons so Spring does not inject their real implementation fields. */
    private AnnotationConfigApplicationContext newCacheContext() {

        var context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("serviceDouble0", mock(ParametrosGlobaisService.class));
        context.getBeanFactory().registerSingleton("serviceDouble1", mock(MaterialService.class));
        context.getBeanFactory().registerSingleton("serviceDouble2", mock(LocationService.class));
        context.register(CacheConfiguration.class);
        context.refresh();
        return context;

    }

    static class Source {
        final AtomicInteger revision = new AtomicInteger(1);
        final AtomicInteger masterLoads = new AtomicInteger();
    }

    static class MasterFactory extends CadastroEParametrosProjectionFactory {
        private final Source source;

        MasterFactory(Source source) {

            this.source = source;

        }

        @Override
        @Cacheable(value = "cadastroEParametrosProjection", sync = true)
        public CadastroEParametrosProjection getParametrosProjectionCompletoDeCache() {

            source.masterLoads.incrementAndGet();
            var material = new Produto("M1");
            material.setAtivo(true);
            var location = new Location("L1");
            location.setAtivo(true);
            var result = new CadastroEParametrosProjection();
            result.materialSet = Set.of(material);
            result.materialMap = Map.of("M1", material);
            result.locationSet = Set.of(location);
            result.locationMap = Map.of("L1", location);
            result.parametrosGlobais = new ParametrosGlobais();
            result.mapaParametrosProdutoLocation = new HashMap<>();
            result.locationForProductLocationParametersMap = Map.of(location, location);
            return result;

        }
    }

    @Configuration
    @EnableCaching
    static class CacheConfiguration {
        @Bean Source source() { return new Source(); }
        @Bean CacheManager cacheManager() { return new ConcurrentMapCacheManager(); }
        @Bean CadastroEParametrosProjectionFactory masterFactory(Source source) { return new MasterFactory(source); }
        @Bean ClusterizacaoProjectionFactory clusteringFactory() { return new ClusterizacaoProjectionFactory(); }
        @Bean ClusterMateriaisRepository materials(Source source) {
            var repository = mock(ClusterMateriaisRepository.class);
            when(repository.findAllByPerfilClusterizacaoId(anyString())).thenAnswer(call -> {
                var cluster = new ClusterMateriais();
                cluster.setId(7L);
                cluster.setPerfilClusterizacao(new PerfilClusterizacao(call.getArgument(0), null));
                cluster.setDescricao("revision-" + source.revision.get());
                cluster.setPadrao(true);
                return List.of(cluster);
            });
            return repository;
        }
        @Bean ClusterLocationsRepository locations() {
            var repository = mock(ClusterLocationsRepository.class);
            when(repository.findAllByPerfilClusterizacaoId(anyString())).thenAnswer(call -> {
                var cluster = new ClusterLocations();
                cluster.setId(7L);
                cluster.setPerfilClusterizacao(new PerfilClusterizacao(call.getArgument(0), null));
                cluster.setPadrao(true);
                return List.of(cluster);
            });
            return repository;
        }
        @Bean CaracteristicaLocationRepository dependency3() { return mock(CaracteristicaLocationRepository.class); }
        @Bean CaracteristicaMaterialRepository dependency4() { return mock(CaracteristicaMaterialRepository.class); }
        @Bean ParametrosDemandPlanNivelClusterRepository dependency9() { return mock(ParametrosDemandPlanNivelClusterRepository.class); }
        @Bean ParametrosProdutoLocationRepository productLocations() { return mock(ParametrosProdutoLocationRepository.class); }
        @Bean RegraAlocacaoClusterProdutosCaracteristicaRepository materialRules() { return mock(RegraAlocacaoClusterProdutosCaracteristicaRepository.class); }
        @Bean RegraAlocacaoClusterProdutosStatusRepository statusRules() { return mock(RegraAlocacaoClusterProdutosStatusRepository.class); }
        @Bean RegraAlocacaoClusterLocationsPaisEstadoRepository geographyRules() { return mock(RegraAlocacaoClusterLocationsPaisEstadoRepository.class); }
        @Bean RegraAlocacaoClusterLocationsTipoLocationRepository typeRules() { return mock(RegraAlocacaoClusterLocationsTipoLocationRepository.class); }
    }
}
