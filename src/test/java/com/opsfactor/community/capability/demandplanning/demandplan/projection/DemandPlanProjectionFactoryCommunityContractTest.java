package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.HistoricoDemandPlanItem;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanItemRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Contrato Community da factory de projection de Demand Plan.
 *
 * <p>O Community pode montar projections basicas de Demand Planning para
 * collaboration e Supply Planning heuristico, mas nao pode ativar demand
 * catch-up por venda passada. Esse teste protege a borda da factory, que e
 * chamada por mais de um fluxo historico.</p>
 */
public class DemandPlanProjectionFactoryCommunityContractTest {

    @Test
    public void getDemandPlanningProjectionVazioShouldRejectCatchUp() {

        DemandPlanProjectionFactory demandPlanProjectionFactory = new DemandPlanProjectionFactory();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanProjectionFactory.getDemandPlanningProjectionVazio(
                        null,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        true));

    }

    @Test
    public void filtroDfuProjectionShouldRejectNullLocationSetBeforeDemandPlanFactory() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new FiltroDFUProjection(
                        (Set<Location>) null,
                        Set.of(new Produto("MAT")),
                        getClusterEParametrosProjectionComParametrosGlobais()));

        Assertions.assertEquals(
                "FiltroDFUProjection location set is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getDemandPlanningProjectionCompletoComDadosLagShouldRejectNegativeLagBeforeBuildingProjection() {

        DemandPlanProjectionFactory demandPlanProjectionFactory = new DemandPlanProjectionFactory();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanProjectionFactory.getDemandPlanningProjectionCompletoComDadosLag(
                        null,
                        -1,
                        null,
                        null,
                        null,
                        null,
                        false));

        Assertions.assertEquals(
                "Demand Plan projection lag line snapshot requires non-negative lag periods.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void populaDemandPlanningProjectionComDemandPlanParaLagPeriodosShouldRejectNegativeLagBeforeProjection() {

        DemandPlanProjectionFactory demandPlanProjectionFactory = new DemandPlanProjectionFactory();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanProjectionFactory.populaDemandPlanningProjectionComDemandPlanParaLagPeriodos(
                        null,
                        -1));

        Assertions.assertEquals(
                "Demand Plan projection lag line snapshot requires non-negative lag periods.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void populaDemandPlanningProjectionComDemandPlanShouldRejectNullRepositoryResult() throws Exception {

        DemandPlanProjectionFactory demandPlanProjectionFactory = new DemandPlanProjectionFactory();
        setField(
                demandPlanProjectionFactory,
                "demandPlanItemRepository",
                createDemandPlanItemRepository(null));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandPlanProjectionFactory.populaDemandPlanningProjectionComDemandPlan(
                        getDemandPlanningProjectionComFiltro()));

        Assertions.assertEquals(
                "Demand Plan projection line snapshot repository returned null Demand Plan line collection.",
                illegalStateException.getMessage());

    }

    @Test
    public void populaDemandPlanningProjectionComDemandPlanItemsShouldRejectNullCollection() {

        DemandPlanProjectionFactory demandPlanProjectionFactory = new DemandPlanProjectionFactory();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanProjectionFactory.populaDemandPlanningProjectionComDemandPlanItems(
                        Mockito.mock(DemandPlanningProjection.class),
                        null));

        Assertions.assertEquals(
                "Demand Plan projection population received null Demand Plan line collection.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void populaDemandPlanningProjectionComHistoricoDemandPlanItemsShouldRejectIncompleteLine() {

        DemandPlanProjectionFactory demandPlanProjectionFactory = new DemandPlanProjectionFactory();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanProjectionFactory.populaDemandPlanningProjectionComHistoricoDemandPlanItems(
                        Mockito.mock(DemandPlanningProjection.class),
                        List.of(new HistoricoDemandPlanItem())));

        Assertions.assertEquals(
                "Demand Plan historical projection population received historical line without demand plan, location, material or reference date.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void populaDemandPlanningProjectionComHistoricoDemandPlanItemsShouldRejectDuplicatedHistoricalKeyBeforeParallelIndex() {

        DemandPlanProjectionFactory demandPlanProjectionFactory = new DemandPlanProjectionFactory();
        DemandPlan demandPlan = getDemandPlanComPerfil();
        Location location = new Location("LOC");
        Produto produto = new Produto("MAT");
        LocalDateTime dataReferencia = LocalDateTime.of(2026, 1, 10, 0, 0);
        HistoricoDemandPlanItem primeiraLinhaHistorica = getHistoricoDemandPlanItem(
                demandPlan,
                location,
                produto,
                dataReferencia);
        HistoricoDemandPlanItem segundaLinhaHistorica = getHistoricoDemandPlanItem(
                demandPlan,
                location,
                produto,
                dataReferencia);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanProjectionFactory.populaDemandPlanningProjectionComHistoricoDemandPlanItems(
                        Mockito.mock(DemandPlanningProjection.class),
                        List.of(primeiraLinhaHistorica, segundaLinhaHistorica)));

        Assertions.assertEquals(
                "Demand Plan historical projection population received duplicated historical line for demand plan 1, location LOC, material MAT and reference date 2026-01-10T00:00.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void demandPlanProjectionFactoryShouldNotKeepLegacyHistoricalSnapshotCallsCommented() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path demandPlanProjectionFactorySourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/demandplanning/demandplan/projection/DemandPlanProjectionFactory.java");
        String demandPlanProjectionFactorySource = Files.readString(
                demandPlanProjectionFactorySourcePath,
                StandardCharsets.UTF_8);

        /*
         * Os metodos com lag carregam snapshots de linhas do plano por janela
         * temporal. A chamada historica sem filtro continua existindo para
         * fluxos completos, mas nao deve ficar pendurada como alternativa
         * comentada nos caminhos de lag.
         */
        Assertions.assertFalse(
                demandPlanProjectionFactorySource.contains(
                        "//        populaDemandPlanningProjectionComHistoricoDemandPlan(demandPlanningProjection);"),
                "DemandPlanProjectionFactory nao deve manter chamadas historicas antigas comentadas nos caminhos de lag.");

    }

    private DemandPlan getDemandPlanComPerfil() {

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setId(1L);
        demandPlan.setTamanhoBucket(Constantes.TamanhoBucket.DIARIO);
        demandPlan.setDataInicioPlano(LocalDateTime.of(2026, 1, 10, 0, 0));
        demandPlan.setPerfilExecucaoDemandPlan(new PerfilExecucaoDemandPlan("DP_PROFILE"));

        return demandPlan;

    }

    private HistoricoDemandPlanItem getHistoricoDemandPlanItem(
            DemandPlan demandPlan,
            Location location,
            Produto produto,
            LocalDateTime dataReferencia) {

        return new HistoricoDemandPlanItem(
                new HistoricoDemandPlanItem.HistoricoDemandPlanItemKey(
                        demandPlan,
                        location,
                        produto,
                        dataReferencia));

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        if ("community".equals(currentDirectory.getFileName().toString())
                || currentDirectory.getFileName().toString().startsWith("community-")) {
            return currentDirectory.getParent();
        }

        return currentDirectory;

    }

    private Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                0,
                1,
                0);

    }

    private FiltroDFUProjection getFiltroDFUProjection() {

        return new FiltroDFUProjection(
                Set.of(new Location("LOC")),
                Set.of(new Produto("MAT")),
                getClusterEParametrosProjectionComParametrosGlobais());

    }

    private ClusterEParametrosProjection getClusterEParametrosProjectionComParametrosGlobais() {

        ClusterEParametrosProjection clusterEParametrosProjection =
                Mockito.mock(ClusterEParametrosProjection.class);
        Mockito.when(clusterEParametrosProjection.getParametrosGlobais())
                .thenReturn(new ParametrosGlobais());

        return clusterEParametrosProjection;

    }

    private ParametrosDemandPlanProjection getParametrosDemandPlanProjection() {

        return ParametrosDemandPlanProjection.builder()
                .perfilExecucaoDemandPlan(new PerfilExecucaoDemandPlan("DP_PROFILE"))
                .parametrosGlobais(new ParametrosGlobais())
                .build();

    }

    private UnidadeMedidaProjection getUnidadeMedidaProjectionComParametrosGlobais() throws Exception {

        UnidadeMedidaProjection unidadeMedidaProjection = new UnidadeMedidaProjection();
        setField(
                unidadeMedidaProjection,
                "parametrosGlobais",
                new ParametrosGlobais());

        return unidadeMedidaProjection;

    }

    private DemandPlanningProjection getDemandPlanningProjectionComFiltro() {

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setId(1L);

        DemandPlanningProjection demandPlanningProjection =
                Mockito.mock(DemandPlanningProjection.class);
        Mockito.when(demandPlanningProjection.getDemandPlan())
                .thenReturn(demandPlan);
        Mockito.when(demandPlanningProjection.getFiltroDfuProjection())
                .thenReturn(new FiltroDFUProjection(
                        Set.of(new Location("LOC")),
                        Set.of(new Produto("MAT")),
                        null));

        return demandPlanningProjection;

    }

    private DemandPlanningProjection getDemandPlanningProjectionOriginalParaConsolidacao() {

        ClusterEParametrosProjection clusterEParametrosProjection =
                getClusterEParametrosProjectionComParametrosGlobais();
        FiltroDFUProjection filtroDFUProjection = getFiltroDFUProjection();
        DemandPlanningProjection demandPlanningProjection = Mockito.mock(DemandPlanningProjection.class);
        Mockito.when(demandPlanningProjection.getDemandPlan())
                .thenReturn(getDemandPlanComPerfil());
        Mockito.when(demandPlanningProjection.getUnidadeMedidaProjection())
                .thenReturn(new UnidadeMedidaProjection());
        Mockito.when(demandPlanningProjection.getClusterEParametrosProjection())
                .thenReturn(clusterEParametrosProjection);
        Mockito.when(demandPlanningProjection.getParametrosDemandPlanProjection())
                .thenReturn(getParametrosDemandPlanProjection());
        Mockito.when(demandPlanningProjection.getCalendario())
                .thenReturn(getCalendarioTeste());
        Mockito.when(demandPlanningProjection.getFiltroDfuProjection())
                .thenReturn(filtroDFUProjection);

        return demandPlanningProjection;

    }

    private DemandPlanItemRepository createDemandPlanItemRepository(
            Collection<?> demandPlanItemCollection) {

        return (DemandPlanItemRepository) Proxy.newProxyInstance(
                DemandPlanItemRepository.class.getClassLoader(),
                new Class<?>[]{DemandPlanItemRepository.class},
                (proxy, method, args) -> {

                    if (method.getName().equals(
                            "customFindByDemandPlanItemKeyDemandPlanIdAndDemandPlanItemKeyLocationInAndDemandPlanItemKeyProdutoIn")) {
                        return demandPlanItemCollection;
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no teste: " + method.getName());

                });

    }

    private void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(
                target,
                value);

    }

}
