package com.opsfactor.community.capability.masterdata.production.productionresource.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.DisponibilidadeRecursoProdutivo.DisponibilidadeRecursoProdutivoCompositeKey;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.DisponibilidadeRecursoProdutivoRepository;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contrato Community da projection de capacidade produtiva.
 *
 * <p>O Community deve calcular restricoes produtivas somente por horas totais
 * por dia. Quantidade por UOM e alocacao por turnos podem existir nos enums e
 * snapshots compartilhados, mas precisam falhar explicitamente antes de
 * qualquer leitura de BI ou de snapshot persistido.</p>
 */
class BIProjectionCapacidadeProdutivaCommunityContractTest {

    @Test
    void communityProjectionShouldRejectQuantityBasedCapacityBeforeUsingSnapshot() {

        ProjectionFixture projectionFixture = criaProjectionComTipoCapacidade(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM);

        /*
         * O snapshot e preenchido manualmente para provar que a validacao de
         * edicao acontece antes de reutilizar um resultado antigo. Isso evita
         * que um plano Enterprise salvo rode parcialmente no Community.
         */
        projectionFixture.biProjectionCapacidadeProdutiva
                .mapaCapacidadeEfetivaSupplyPlanPorRecursoPeriodo
                .put(projectionFixture.recursoProdutivo, new HashMap<>(Map.of(0, 10D)));

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> projectionFixture.biProjectionCapacidadeProdutiva
                        .getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                                0,
                                projectionFixture.recursoProdutivo,
                                BIProjectionCapacidadeProdutiva.MasterOrPlanningData.PLANNING_DATA));

    }

    @Test
    void communityProjectionShouldRejectShiftBasedCapacityBeforeUsingMasterData() {

        ProjectionFixture projectionFixture = criaProjectionComTipoCapacidade(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.ALOCACAO_TURNOS);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> projectionFixture.biProjectionCapacidadeProdutiva
                        .getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                                0,
                                projectionFixture.recursoProdutivo,
                                BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA));

    }

    @Test
    void communityProjectionShouldNotExposeErasedShiftHooksReservedForEnterprise() {

        /*
         * Turnos permitidos e as horas de um turno exigem entidades que vivem
         * no Enterprise. O Community nao deve manter uma ponte generica
         * (Object ou Set<T>) que devolva vazio: ela mascara a fronteira e
         * permite que consumers compilarem contra uma API sem semantica.
         */
        for (Method metodoPublico : BIProjectionCapacidadeProdutiva.class.getMethods()) {
            Assertions.assertNotEquals("getTurnosPermitidos", metodoPublico.getName());
            Assertions.assertNotEquals("getNumeroHorasDisponiveis", metodoPublico.getName());
        }

    }

    @Test
    void communityProjectionShouldKeepHoursPerDayAsTheOnlySupportedCapacityType() {

        ProjectionFixture projectionFixture = criaProjectionComTipoCapacidade(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA);

        double capacidadeEmHoras = projectionFixture.biProjectionCapacidadeProdutiva
                .getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                        0,
                        projectionFixture.recursoProdutivo,
                        BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA);

        Assertions.assertEquals(0D, capacidadeEmHoras);

    }

    @Test
    void communityProjectionShouldRejectMissingCapacityTypeWithContractMessage() {

        ProjectionFixture projectionFixture = criaProjectionComTipoCapacidade(null);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> projectionFixture.biProjectionCapacidadeProdutiva
                        .getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                                0,
                                projectionFixture.recursoProdutivo,
                                BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "BIProjectionCapacidadeProdutiva Community supports only HORAS_POR_DIA"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("received null"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "missing configuration must be fixed before Supply Planning execution"));

    }

    @Test
    void factoryShouldRejectNullPersistedCapacityRepositoryResultBeforeIteratingSnapshot() throws Exception {

        ProjectionFixture projectionFixture = criaProjectionComTipoCapacidade(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA,
                getCalendarioTeste(),
                new ParametrosGlobais());
        BIProjectionCapacidadeProdutivaFactory biProjectionCapacidadeProdutivaFactory =
                new BIProjectionCapacidadeProdutivaFactory();
        CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository repository =
                Mockito.mock(CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository.class);
        Mockito.when(repository.customFindBySupplyPlan(projectionFixture.supplyPlan()))
                .thenReturn(null);

        setPrivateField(
                biProjectionCapacidadeProdutivaFactory,
                "supplyNetworkProjectionFactory",
                new TestSupplyNetworkProjectionFactory(
                        new FakeSupplyNetworkProjection(
                                projectionFixture.recursoProdutivo().getLocation(),
                                projectionFixture.recursoProdutivo(),
                                new ParametrosGlobais())));
        setPrivateField(
                biProjectionCapacidadeProdutivaFactory,
                "capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository",
                repository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> biProjectionCapacidadeProdutivaFactory.getBIProjectionCapacidadeProdutivaDeSupplyPlan(
                        projectionFixture.supplyPlan(),
                        getCalendarioTeste()));

        Assertions.assertEquals(
                "Persisted effective production capacity repository returned null list for Supply Plan "
                        + projectionFixture.supplyPlan().getId()
                        + ".",
                illegalStateException.getMessage());

    }

    @Test
    void factoryShouldRejectNullMasterDataCapacityRepositoryResultBeforeIteratingSnapshot() throws Exception {

        ProjectionFixture projectionFixture = criaProjectionComTipoCapacidade(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA,
                getCalendarioTeste(),
                new ParametrosGlobais());
        BIProjectionCapacidadeProdutivaFactory biProjectionCapacidadeProdutivaFactory =
                new BIProjectionCapacidadeProdutivaFactory();
        DisponibilidadeRecursoProdutivoRepository repository =
                Mockito.mock(DisponibilidadeRecursoProdutivoRepository.class);
        Mockito.when(repository.customFindAllWhereDataReferenciaBetween(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 3)))
                .thenReturn(null);

        setPrivateField(
                biProjectionCapacidadeProdutivaFactory,
                "supplyNetworkProjectionFactory",
                new TestSupplyNetworkProjectionFactory(
                        new FakeSupplyNetworkProjection(
                                projectionFixture.recursoProdutivo().getLocation(),
                                projectionFixture.recursoProdutivo(),
                                new ParametrosGlobais())));
        setPrivateField(
                biProjectionCapacidadeProdutivaFactory,
                "disponibilidadeRecursoProdutivoRepository",
                repository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> biProjectionCapacidadeProdutivaFactory.getBIProjectionCapacidadeProdutiva(
                        projectionFixture.supplyPlan(),
                        getCalendarioTeste()));

        Assertions.assertEquals(
                "Production resource availability repository returned null list for master data capacity projection.",
                illegalStateException.getMessage());

    }

    @Test
    void projectionShouldRejectDuplicatedMasterDataCapacityResourceDateKey() {

        ProjectionFixture projectionFixture = criaProjectionComTipoCapacidade(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA,
                getCalendarioTeste(),
                new ParametrosGlobais());
        DisponibilidadeRecursoProdutivo primeiraDisponibilidade =
                criaDisponibilidadeRecursoProdutivo(
                        projectionFixture.recursoProdutivo(),
                        LocalDate.of(2026, 1, 1),
                        8.0f,
                        null);
        DisponibilidadeRecursoProdutivo segundaDisponibilidade =
                criaDisponibilidadeRecursoProdutivo(
                        projectionFixture.recursoProdutivo(),
                        LocalDate.of(2026, 1, 1),
                        9.0f,
                        null);

        projectionFixture.biProjectionCapacidadeProdutiva()
                .addDadoAoBI(primeiraDisponibilidade);
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> projectionFixture.biProjectionCapacidadeProdutiva()
                        .addDadoAoBI(segundaDisponibilidade));

        Assertions.assertEquals(
                "Production resource availability repository returned duplicated resource/date key "
                        + projectionFixture.recursoProdutivo().getId()
                        + "/2026-01-01.",
                illegalStateException.getMessage());

    }

    @Test
    void projectionShouldRejectDuplicatedPersistedCapacityResourcePeriodKey() {

        ProjectionFixture projectionFixture = criaProjectionComTipoCapacidade(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA,
                getCalendarioTeste(),
                new ParametrosGlobais());
        LocalDateTime dataReferencia = LocalDateTime.of(2026, 1, 1, 0, 0);
        CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan primeiraCapacidadeEfetiva =
                criaCapacidadeProdutivaEfetiva(
                        projectionFixture.supplyPlan(),
                        projectionFixture.recursoProdutivo(),
                        dataReferencia,
                        8.0d);
        CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan segundaCapacidadeEfetiva =
                criaCapacidadeProdutivaEfetiva(
                        projectionFixture.supplyPlan(),
                        projectionFixture.recursoProdutivo(),
                        dataReferencia,
                        9.0d);

        projectionFixture.biProjectionCapacidadeProdutiva()
                .addDadoAoBI(primeiraCapacidadeEfetiva);
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> projectionFixture.biProjectionCapacidadeProdutiva()
                        .addDadoAoBI(segundaCapacidadeEfetiva));

        Assertions.assertEquals(
                "Persisted effective production capacity snapshot returned duplicated resource/period key "
                        + projectionFixture.recursoProdutivo().getId()
                        + "/0 for Supply Plan "
                        + projectionFixture.supplyPlan().getId()
                        + ".",
                illegalStateException.getMessage());

    }

    @Test
    void projectionShouldKeepPersistedUnitOfMeasureAlongsideCapacitySnapshot() {

        ProjectionFixture projectionFixture = criaProjectionComTipoCapacidade(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM,
                getCalendarioTeste(),
                new ParametrosGlobais());
        CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidadeProdutivaEfetiva =
                criaCapacidadeProdutivaEfetiva(
                        projectionFixture.supplyPlan(),
                        projectionFixture.recursoProdutivo(),
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        8.0d);
        capacidadeProdutivaEfetiva.setTipoCapacidadeProdutiva(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.QUANTIDADE_POR_UOM);
        capacidadeProdutivaEfetiva.setUnidadeMedidaCapacidade(new UnidadeMedida("EA"));

        projectionFixture.biProjectionCapacidadeProdutiva().addDadoAoBI(capacidadeProdutivaEfetiva);

        Assertions.assertEquals(
                "EA",
                projectionFixture.biProjectionCapacidadeProdutiva()
                        .getUnitOfMeasureIdByProductionResourceAndPeriod()
                        .get(projectionFixture.recursoProdutivo())
                        .get(0));

    }

    private static ProjectionFixture criaProjectionComTipoCapacidade(
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva) {

        return criaProjectionComTipoCapacidade(tipoCapacidadeProdutiva, null, null);

    }

    private static ProjectionFixture criaProjectionComTipoCapacidade(
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva,
            Calendario calendario,
            ParametrosGlobais parametrosGlobais) {

        Location location = new Location();
        location.setId("LOC");
        location.setAtivo(true);

        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId("RES");
        recursoProdutivo.setLocation(location);

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(7L);
        supplyPlan.setPerfilExecucaoSupplyPlan(new PerfilExecucaoSupplyPlan());

        BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva =
                new BIProjectionCapacidadeProdutiva(
                        supplyPlan,
                        calendario,
                        new FakeSupplyNetworkProjection(location, recursoProdutivo, parametrosGlobais));

        biProjectionCapacidadeProdutiva.tipoCapacidadeProdutivaPorRecurso.put(
                recursoProdutivo,
                tipoCapacidadeProdutiva);

        return new ProjectionFixture(supplyPlan, biProjectionCapacidadeProdutiva, recursoProdutivo);

    }

    private static CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan criaCapacidadeProdutivaEfetiva(
            SupplyPlan supplyPlan,
            RecursoProdutivo recursoProdutivo,
            LocalDateTime dataReferencia,
            Double capacidadeEfetiva) {

        CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan capacidadeProdutivaEfetiva =
                new CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan(
                        new CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey(
                                supplyPlan,
                                recursoProdutivo,
                                dataReferencia));
        capacidadeProdutivaEfetiva.setTipoCapacidadeProdutiva(
                PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva.HORAS_POR_DIA);
        capacidadeProdutivaEfetiva.setCapacidadeEfetiva(capacidadeEfetiva);

        return capacidadeProdutivaEfetiva;

    }

    private static DisponibilidadeRecursoProdutivo criaDisponibilidadeRecursoProdutivo(
            RecursoProdutivo recursoProdutivo,
            LocalDate dataReferencia,
            Float horasDisponiveis,
            Float capacidadeEmQuantidade) {

        DisponibilidadeRecursoProdutivo disponibilidadeRecursoProdutivo =
                new DisponibilidadeRecursoProdutivo(
                        new DisponibilidadeRecursoProdutivoCompositeKey(
                                recursoProdutivo,
                                dataReferencia));
        disponibilidadeRecursoProdutivo.setHorasDisponiveis(horasDisponiveis);
        disponibilidadeRecursoProdutivo.setCapacidadeEmQuantidade(capacidadeEmQuantidade);

        return disponibilidadeRecursoProdutivo;

    }

    private static Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0));

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static class TestSupplyNetworkProjectionFactory extends SupplyNetworkProjectionFactory {

        private final SupplyNetworkProjection supplyNetworkProjection;

        private TestSupplyNetworkProjectionFactory(SupplyNetworkProjection supplyNetworkProjection) {

            this.supplyNetworkProjection = supplyNetworkProjection;

        }

        @Override
        public SupplyNetworkProjection getSupplyNetworkProjectionCompletoDeCache() {

            return supplyNetworkProjection;

        }

    }

    private record ProjectionFixture(
            SupplyPlan supplyPlan,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            RecursoProdutivo recursoProdutivo) {
    }

    private static class FakeSupplyNetworkProjection extends SupplyNetworkProjection {

        private final FakeClusterEParametrosProjection clusterEParametrosProjection;
        private final RecursoProdutivo recursoProdutivo;

        private FakeSupplyNetworkProjection(Location location, RecursoProdutivo recursoProdutivo) {

            this(location, recursoProdutivo, null);

        }

        private FakeSupplyNetworkProjection(
                Location location,
                RecursoProdutivo recursoProdutivo,
                ParametrosGlobais parametrosGlobais) {

            this.clusterEParametrosProjection = new FakeClusterEParametrosProjection(location, parametrosGlobais);
            this.recursoProdutivo = recursoProdutivo;

        }

        @Override
        public ClusterEParametrosProjection getClusterEParametrosProjection() {

            return clusterEParametrosProjection;

        }

        @Override
        public Set<RecursoProdutivo> getRecursoProdutivoAtivoSet(Location location) {

            return Set.of(recursoProdutivo);

        }

    }

    private static class FakeClusterEParametrosProjection extends ClusterEParametrosProjection {

        private final Location location;
        private final ParametrosGlobais parametrosGlobais;

        private FakeClusterEParametrosProjection(Location location) {

            this(location, null);

        }

        private FakeClusterEParametrosProjection(Location location, ParametrosGlobais parametrosGlobais) {

            this.location = location;
            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public Set<Location> getLocationsAtivas() {

            return Set.of(location);

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

    }

}
