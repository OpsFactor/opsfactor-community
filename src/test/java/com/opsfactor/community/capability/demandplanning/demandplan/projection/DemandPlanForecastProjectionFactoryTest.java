package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Testes unitarios da montagem de projections de forecast de Demand Planning.
 *
 * <p>A factory e pura para este recorte: o teste nao sobe Spring nem acessa
 * repositories, validando apenas a relacao agregado/desagregado usada pelo
 * workflow estatistico Community.</p>
 */
public class DemandPlanForecastProjectionFactoryTest {

    @Test
    public void bottomUpBottomUpShouldUseMaterialLocationSeriesAsExecutionUnits() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location location = getLocation("LOCATION_A", true);
        Produto material = getMaterial("MATERIAL_A", true);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        location,
                        material,
                        10.0d);
        List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList =
                List.of(demandPlanForecastProjectionMaterialLocation);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        demandPlanForecastProjectionMaterialLocationList,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false);

        Assertions.assertSame(demandPlanForecastProjectionMaterialLocationList, demandPlanForecastProjectionsExecucao);
        Assertions.assertNull(demandPlanForecastProjectionMaterialLocation.getDemandPlanForecastProjectionAgregado());

    }

    @Test
    public void topDownBottomUpShouldCreateOneAggregatePerLocation() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location locationA = getLocation("LOCATION_A", true);
        Location locationB = getLocation("LOCATION_B", true);
        Produto materialA = getMaterial("MATERIAL_A", true);
        Produto materialB = getMaterial("MATERIAL_B", true);
        LocationProjection locationProjection = LocationProjectionFactory.getProjectionSetLocations(
                Set.of(locationA, locationB),
                null);

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationA =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        locationA,
                        materialA,
                        10.0d);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationB =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        locationA,
                        materialB,
                        5.0d);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationC =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        locationB,
                        materialA,
                        2.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        locationProjection,
                        true,
                        List.of(
                                demandPlanForecastProjectionMaterialLocationA,
                                demandPlanForecastProjectionMaterialLocationB,
                                demandPlanForecastProjectionMaterialLocationC),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false);

        Assertions.assertEquals(2, demandPlanForecastProjectionsExecucao.size());

        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregadoLocationA =
                getUnicoAgregadoComLocation(demandPlanForecastProjectionsExecucao, locationA);
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregadoLocationB =
                getUnicoAgregadoComLocation(demandPlanForecastProjectionsExecucao, locationB);

        Assertions.assertEquals(15.0d, demandPlanForecastProjectionAgregadoLocationA.vendaHistoricaTratamentoOutliers[0], 0.0001d);
        Assertions.assertEquals(2.0d, demandPlanForecastProjectionAgregadoLocationB.vendaHistoricaTratamentoOutliers[0], 0.0001d);
        Assertions.assertSame(
                demandPlanForecastProjectionAgregadoLocationA,
                demandPlanForecastProjectionMaterialLocationA.getDemandPlanForecastProjectionAgregado());
        Assertions.assertSame(
                demandPlanForecastProjectionAgregadoLocationA,
                demandPlanForecastProjectionMaterialLocationB.getDemandPlanForecastProjectionAgregado());
        Assertions.assertSame(
                demandPlanForecastProjectionAgregadoLocationB,
                demandPlanForecastProjectionMaterialLocationC.getDemandPlanForecastProjectionAgregado());

    }

    @Test
    public void topDownBottomUpShouldSkipLocationWithoutMaterialLocationSeries() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location locationComHistorico = getLocation("LOCATION_WITH_HISTORY", true);
        Location locationSemHistorico = getLocation("LOCATION_WITHOUT_HISTORY", true);
        Produto material = getMaterial("MATERIAL_A", true);
        LocationProjection locationProjection = LocationProjectionFactory.getProjectionSetLocations(
                Set.of(locationComHistorico, locationSemHistorico),
                null);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        locationComHistorico,
                        material,
                        10.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        locationProjection,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false);

        Assertions.assertEquals(1, demandPlanForecastProjectionsExecucao.size());
        Assertions.assertEquals(
                Set.of(locationComHistorico),
                demandPlanForecastProjectionsExecucao.get(0).getLocations());

    }

    @Test
    public void topDownBottomUpShouldReturnEmptyListWhenThereAreNoMaterialLocationSeries() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location location = getLocation("LOCATION_A", true);
        LocationProjection locationProjection = LocationProjectionFactory.getProjectionSetLocations(
                Set.of(location),
                null);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        locationProjection,
                        true,
                        List.of(),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false);

        /*
         * A factory nao cria placeholders vazios. Se o fluxo anterior filtrou
         * todas as DFUs do cluster/location, o service principal simplesmente
         * recebe uma lista vazia e nao entra no loop de forecast.
         */
        Assertions.assertTrue(demandPlanForecastProjectionsExecucao.isEmpty());

    }

    @Test
    public void topDownBottomUpShouldFailExplicitlyWhenLocationProjectionIsMissing() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false));

        /*
         * Esta combinacao cria uma unidade de execucao por location. A falha
         * precisa apontar o contrato ausente, nao deixar o fluxo cair em erro
         * anonimo dentro da leitura da projection.
         */
        Assertions.assertEquals(
                "locationProjection e obrigatorio para forecast TOP_DOWN/BOTTOM_UP",
                illegalArgumentException.getMessage());

    }

    @Test
    public void topDownBottomUpShouldFailWhenLocationScopeReturnsNullCollection() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        new LocationProjectionComSnapshotQuebrado(null),
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false));

        Assertions.assertEquals(
                "LocationProjection retornou colecao nula para forecast TOP_DOWN/BOTTOM_UP",
                illegalStateException.getMessage());

    }

    @Test
    public void topDownBottomUpShouldFailWhenLocationScopeContainsNullItem() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        new LocationProjectionComSnapshotQuebrado(new HashSet<>(Collections.singletonList(null))),
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false));

        Assertions.assertEquals(
                "LocationProjection retornou location nula no indice 0 para forecast TOP_DOWN/BOTTOM_UP",
                illegalStateException.getMessage());

    }

    @Test
    public void bottomUpTopDownShouldCreateOneAggregatePerMaterial() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location locationA = getLocation("LOCATION_A", true);
        Location locationB = getLocation("LOCATION_B", true);
        Produto materialA = getMaterial("MATERIAL_A", true);
        Produto materialB = getMaterial("MATERIAL_B", true);
        MaterialProjection materialProjection = MaterialProjectionFactory.getProjectionSetMateriais(
                Set.of(materialA, materialB),
                null);

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationA =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        locationA,
                        materialA,
                        10.0d);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationB =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        locationB,
                        materialA,
                        5.0d);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationC =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        locationA,
                        materialB,
                        2.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        materialProjection,
                        null,
                        true,
                        List.of(
                                demandPlanForecastProjectionMaterialLocationA,
                                demandPlanForecastProjectionMaterialLocationB,
                                demandPlanForecastProjectionMaterialLocationC),
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false);

        Assertions.assertEquals(2, demandPlanForecastProjectionsExecucao.size());

        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregadoMaterialA =
                getUnicoAgregadoComMaterial(demandPlanForecastProjectionsExecucao, materialA);
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregadoMaterialB =
                getUnicoAgregadoComMaterial(demandPlanForecastProjectionsExecucao, materialB);

        Assertions.assertEquals(15.0d, demandPlanForecastProjectionAgregadoMaterialA.vendaHistoricaTratamentoOutliers[0], 0.0001d);
        Assertions.assertEquals(2.0d, demandPlanForecastProjectionAgregadoMaterialB.vendaHistoricaTratamentoOutliers[0], 0.0001d);
        Assertions.assertSame(
                demandPlanForecastProjectionAgregadoMaterialA,
                demandPlanForecastProjectionMaterialLocationA.getDemandPlanForecastProjectionAgregado());
        Assertions.assertSame(
                demandPlanForecastProjectionAgregadoMaterialA,
                demandPlanForecastProjectionMaterialLocationB.getDemandPlanForecastProjectionAgregado());
        Assertions.assertSame(
                demandPlanForecastProjectionAgregadoMaterialB,
                demandPlanForecastProjectionMaterialLocationC.getDemandPlanForecastProjectionAgregado());

    }

    @Test
    public void bottomUpTopDownShouldSkipMaterialWithoutMaterialLocationSeries() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location location = getLocation("LOCATION_A", true);
        Produto materialComHistorico = getMaterial("MATERIAL_WITH_HISTORY", true);
        Produto materialSemHistorico = getMaterial("MATERIAL_WITHOUT_HISTORY", true);
        MaterialProjection materialProjection = MaterialProjectionFactory.getProjectionSetMateriais(
                Set.of(materialComHistorico, materialSemHistorico),
                null);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        location,
                        materialComHistorico,
                        10.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        materialProjection,
                        null,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false);

        Assertions.assertEquals(1, demandPlanForecastProjectionsExecucao.size());
        Assertions.assertEquals(
                Set.of(materialComHistorico),
                demandPlanForecastProjectionsExecucao.get(0).getMateriais());

    }

    @Test
    public void bottomUpTopDownShouldReturnEmptyListWhenThereAreNoMaterialLocationSeries() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Produto material = getMaterial("MATERIAL_A", true);
        MaterialProjection materialProjection = MaterialProjectionFactory.getProjectionSetMateriais(
                Set.of(material),
                null);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        materialProjection,
                        null,
                        true,
                        List.of(),
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false);

        /*
         * A ausencia de series material/location tambem precisa ficar explicita
         * no ramo por material. Isso protege overlays Enterprise que venham a
         * criar agregados auxiliares a partir do mesmo contrato da factory.
         */
        Assertions.assertTrue(demandPlanForecastProjectionsExecucao.isEmpty());

    }

    @Test
    public void bottomUpTopDownShouldFailExplicitlyWhenMaterialProjectionIsMissing() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false));

        /*
         * Esta combinacao cria uma unidade de execucao por material. A projection
         * de materiais precisa vir do fluxo que conhece o cluster; sem ela, a
         * factory falha cedo e com mensagem rastreavel.
         */
        Assertions.assertEquals(
                "materialProjection e obrigatorio para forecast BOTTOM_UP/TOP_DOWN",
                illegalArgumentException.getMessage());

    }

    @Test
    public void bottomUpTopDownShouldFailWhenMaterialScopeReturnsNullCollection() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        new MaterialProjectionComSnapshotQuebrado(null),
                        null,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false));

        Assertions.assertEquals(
                "MaterialProjection retornou colecao nula para forecast BOTTOM_UP/TOP_DOWN",
                illegalStateException.getMessage());

    }

    @Test
    public void bottomUpTopDownShouldFailWhenMaterialScopeContainsNullItem() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        new MaterialProjectionComSnapshotQuebrado(new HashSet<>(Collections.singletonList(null))),
                        null,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false));

        Assertions.assertEquals(
                "MaterialProjection retornou material nulo no indice 0 para forecast BOTTOM_UP/TOP_DOWN",
                illegalStateException.getMessage());

    }

    @Test
    public void materialLocationSeriesListShouldBeRequiredBeforeAnyAggregationBranch() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        null,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false));

        Assertions.assertEquals(
                "demandPlanForecastProjectionMaterialLocationDesagregadosList e obrigatoria para forecast",
                illegalArgumentException.getMessage());

    }

    @Test
    public void materialLocationSeriesListShouldNotContainNullItems() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        Collections.singletonList(null),
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false));

        Assertions.assertEquals(
                "demandPlanForecastProjectionMaterialLocationDesagregadosList contem item nulo no indice 0",
                illegalStateException.getMessage());

    }

    @Test
    public void materialLocationSeriesConstructorShouldRequireLocationIdentity() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Produto material = getMaterial("MATERIAL_A", true);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        null,
                        material,
                        false));

        Assertions.assertEquals(
                "Demand Plan forecast material/location projection requires location.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void materialLocationSeriesConstructorShouldRequireMaterialIdentity() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location location = getLocation("LOCATION_A", true);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        location,
                        null,
                        false));

        Assertions.assertEquals(
                "Demand Plan forecast material/location projection requires material.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void aggregateExecutionShouldRequireCalendarioWhenThereAreMaterialLocationSeries() {

        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        getCalendarioTeste(),
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        null,
                        null,
                        null,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false));

        Assertions.assertEquals(
                "calendario e obrigatorio para criar agregado de forecast",
                illegalArgumentException.getMessage());

    }

    @Test
    public void aggregateExecutionShouldRequireUnidadeMedidaWhenThereAreMaterialLocationSeries() {

        Calendario calendario = getCalendarioTeste();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        null,
                        false));

        Assertions.assertEquals(
                "unidadeMedida e obrigatoria para criar agregado de forecast",
                illegalArgumentException.getMessage());

    }

    @Test
    public void topDownTopDownShouldCreateSingleAggregateAndSetParentOnMaterialLocationSeries() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationA =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationB =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_B",
                        "MATERIAL_B",
                        15.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        List.of(
                                demandPlanForecastProjectionMaterialLocationA,
                                demandPlanForecastProjectionMaterialLocationB),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false);

        Assertions.assertEquals(1, demandPlanForecastProjectionsExecucao.size());
        Assertions.assertInstanceOf(
                DemandPlanForecastProjectionAgregado.class,
                demandPlanForecastProjectionsExecucao.get(0));

        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                (DemandPlanForecastProjectionAgregado) demandPlanForecastProjectionsExecucao.get(0);

        Assertions.assertSame(
                demandPlanForecastProjectionAgregado,
                demandPlanForecastProjectionMaterialLocationA.getDemandPlanForecastProjectionAgregado());
        Assertions.assertSame(
                demandPlanForecastProjectionAgregado,
                demandPlanForecastProjectionMaterialLocationB.getDemandPlanForecastProjectionAgregado());
        Assertions.assertEquals(25.0d, demandPlanForecastProjectionAgregado.vendaHistoricaTratamentoStockouts[0], 0.0001d);
        Assertions.assertEquals(25.0d, demandPlanForecastProjectionAgregado.vendaHistoricaTratamentoOutliers[0], 0.0001d);

    }

    @Test
    public void aggregateRecalculationShouldNotDuplicatePreviouslyConsolidatedValues() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationA =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationB =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_B",
                        "MATERIAL_B",
                        15.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        List.of(
                                demandPlanForecastProjectionMaterialLocationA,
                                demandPlanForecastProjectionMaterialLocationB),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false);

        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                (DemandPlanForecastProjectionAgregado) demandPlanForecastProjectionsExecucao.get(0);

        /*
         * A projection agregada e um snapshot recalculavel. Se uma etapa futura
         * Enterprise recriar agregados auxiliares MAPE/HTS a partir dos mesmos
         * filhos, uma chamada repetida nao pode dobrar o valor consolidado.
         */
        demandPlanForecastProjectionAgregado.agregaForecastEDemandaHistoricaDemandPlanForecastProjectionAPartirNivelDesagregado();

        Assertions.assertEquals(25.0d, demandPlanForecastProjectionAgregado.vendaHistoricaTratamentoStockouts[0], 0.0001d);
        Assertions.assertEquals(25.0d, demandPlanForecastProjectionAgregado.vendaHistoricaTratamentoOutliers[0], 0.0001d);

    }

    @Test
    public void topDownTopDownShouldReturnEmptyListWhenThereAreNoMaterialLocationSeries() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        List.of(),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false);

        Assertions.assertTrue(demandPlanForecastProjectionsExecucao.isEmpty());

    }

    @Test
    public void missingAggregationTypesShouldUseConservativeTopDownTopDown() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationA =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_A",
                        "MATERIAL_A",
                        10.0d);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationB =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        "LOCATION_B",
                        "MATERIAL_B",
                        15.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        null,
                        true,
                        List.of(
                                demandPlanForecastProjectionMaterialLocationA,
                                demandPlanForecastProjectionMaterialLocationB),
                        null,
                        null,
                        unidadeMedida,
                        false);

        /*
         * Nulos vindos de configuracoes antigas devem seguir o mesmo fallback de
         * ParametrosAgregacaoForecast: executar agregado e desagregar depois.
         */
        Assertions.assertEquals(1, demandPlanForecastProjectionsExecucao.size());
        Assertions.assertInstanceOf(
                DemandPlanForecastProjectionAgregado.class,
                demandPlanForecastProjectionsExecucao.get(0));
        Assertions.assertEquals(
                25.0d,
                demandPlanForecastProjectionsExecucao.get(0).vendaHistoricaTratamentoOutliers[0],
                0.0001d);

    }

    @Test
    public void topDownBottomUpShouldSkipInactiveLocationWhenExecutionUsesOnlyActiveDfus() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location inactiveLocation = getLocation("LOCATION_INACTIVE", false);
        Produto material = getMaterial("MATERIAL_A", true);
        LocationProjection locationProjection = LocationProjectionFactory.getProjectionSetLocations(
                Set.of(inactiveLocation),
                null);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        inactiveLocation,
                        material,
                        10.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        locationProjection,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false);

        /*
         * Mesmo que uma projection de escopo quebrada devolva location inativa
         * em getLocationsAtivas(), a factory nao deve criar agregado vazio ou
         * reintroduzir DFU inativa quando a execucao pediu somente DFUs ativas.
         */
        Assertions.assertTrue(demandPlanForecastProjectionsExecucao.isEmpty());

    }

    @Test
    public void topDownBottomUpShouldKeepInactiveLocationWhenExecutionAllowsInactiveDfus() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location inactiveLocation = getLocation("LOCATION_INACTIVE", false);
        Produto material = getMaterial("MATERIAL_A", true);
        LocationProjection locationProjection = LocationProjectionFactory.getProjectionSetLocations(
                Set.of(inactiveLocation),
                null);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        inactiveLocation,
                        material,
                        10.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        null,
                        locationProjection,
                        false,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        unidadeMedida,
                        false);

        Assertions.assertEquals(1, demandPlanForecastProjectionsExecucao.size());
        Assertions.assertEquals(
                Set.of(inactiveLocation),
                demandPlanForecastProjectionsExecucao.get(0).getLocations());

    }

    @Test
    public void bottomUpTopDownShouldSkipInactiveMaterialWhenExecutionUsesOnlyActiveDfus() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location location = getLocation("LOCATION_A", true);
        Produto inactiveMaterial = getMaterial("MATERIAL_INACTIVE", false);
        MaterialProjection materialProjection = MaterialProjectionFactory.getProjectionSetMateriais(
                Set.of(inactiveMaterial),
                null);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        location,
                        inactiveMaterial,
                        10.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        materialProjection,
                        null,
                        true,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false);

        /*
         * A protecao precisa ser simetrica entre material e location. Essa
         * borda e importante para agregacoes auxiliares Enterprise futuras:
         * uma projection material com snapshot ativo quebrado nao pode gerar
         * unidade agregada para material inativo em rodada active-only.
         */
        Assertions.assertTrue(demandPlanForecastProjectionsExecucao.isEmpty());

    }

    @Test
    public void bottomUpTopDownShouldKeepInactiveMaterialWhenExecutionAllowsInactiveDfus() {

        Calendario calendario = getCalendarioTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location location = getLocation("LOCATION_A", true);
        Produto inactiveMaterial = getMaterial("MATERIAL_INACTIVE", false);
        MaterialProjection materialProjection = MaterialProjectionFactory.getProjectionSetMateriais(
                Set.of(inactiveMaterial),
                null);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        location,
                        inactiveMaterial,
                        10.0d);

        List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao =
                DemandPlanForecastProjectionFactory.getDemandPlanForecastProjectionsExecucao(
                        calendario,
                        materialProjection,
                        null,
                        false,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.TOP_DOWN,
                        unidadeMedida,
                        false);

        Assertions.assertEquals(1, demandPlanForecastProjectionsExecucao.size());
        Assertions.assertEquals(
                Set.of(inactiveMaterial),
                demandPlanForecastProjectionsExecucao.get(0).getMateriais());

    }

    private static Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                2,
                2,
                0);

    }

    private static DemandPlanForecastProjectionMaterialLocation getDemandPlanForecastProjectionMaterialLocation(
            Calendario calendario,
            UnidadeMedida unidadeMedida,
            String locationId,
            String materialId,
            double demandaHistorica) {

        Location location = getLocation(locationId, true);
        Produto material = getMaterial(materialId, true);

        return getDemandPlanForecastProjectionMaterialLocation(
                calendario,
                unidadeMedida,
                location,
                material,
                demandaHistorica);

    }

    private static DemandPlanForecastProjectionMaterialLocation getDemandPlanForecastProjectionMaterialLocation(
            Calendario calendario,
            UnidadeMedida unidadeMedida,
            Location location,
            Produto material,
            double demandaHistorica) {

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        location,
                        material,
                        false);
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts[0] = demandaHistorica;
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[0] = demandaHistorica;

        return demandPlanForecastProjectionMaterialLocation;

    }

    private static Location getLocation(String locationId, boolean active) {

        Location location = new Location(locationId);
        location.setAtivo(active);
        return location;

    }

    private static Produto getMaterial(String materialId, boolean active) {

        Produto material = new Produto(materialId);
        material.setAtivo(active);
        return material;

    }

    private static DemandPlanForecastProjectionAgregado getUnicoAgregadoComLocation(
            List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao,
            Location location) {

        List<DemandPlanForecastProjectionAgregado> demandPlanForecastProjectionAgregadoList =
                demandPlanForecastProjectionsExecucao
                        .stream()
                        .map(DemandPlanForecastProjectionAgregado.class::cast)
                        .filter(demandPlanForecastProjectionAgregado ->
                                demandPlanForecastProjectionAgregado.getLocations().equals(Set.of(location)))
                        .toList();

        Assertions.assertEquals(1, demandPlanForecastProjectionAgregadoList.size());
        return demandPlanForecastProjectionAgregadoList.get(0);

    }

    private static DemandPlanForecastProjectionAgregado getUnicoAgregadoComMaterial(
            List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao,
            Produto material) {

        List<DemandPlanForecastProjectionAgregado> demandPlanForecastProjectionAgregadoList =
                demandPlanForecastProjectionsExecucao
                        .stream()
                        .map(DemandPlanForecastProjectionAgregado.class::cast)
                        .filter(demandPlanForecastProjectionAgregado ->
                                demandPlanForecastProjectionAgregado.getMateriais().equals(Set.of(material)))
                        .toList();

        Assertions.assertEquals(1, demandPlanForecastProjectionAgregadoList.size());
        return demandPlanForecastProjectionAgregadoList.get(0);

    }

    /**
     * Projection de teste que simula snapshot quebrado retornado por um escopo
     * de locations ja materializado. A factory deve capturar essa quebra antes
     * do loop de agregacao por location.
     */
    private static class LocationProjectionComSnapshotQuebrado extends LocationProjection {

        private final Set<Location> locations;

        private LocationProjectionComSnapshotQuebrado(Set<Location> locations) {

            this.locations = locations;

        }

        @Override
        public Set<Location> getLocationsAtivas() {

            return locations;

        }

    }

    /**
     * Projection de teste que simula snapshot quebrado retornado por um escopo
     * de materiais ja materializado. Isso evita depender de reflexao para
     * corromper campos protegidos da projection real.
     */
    private static class MaterialProjectionComSnapshotQuebrado extends MaterialProjection {

        private final Set<Produto> materiais;

        private MaterialProjectionComSnapshotQuebrado(Set<Produto> materiais) {

            this.materiais = materiais;

        }

        @Override
        public Set<Produto> getMateriaisAtivos() {

            return materiais;

        }

    }

}
