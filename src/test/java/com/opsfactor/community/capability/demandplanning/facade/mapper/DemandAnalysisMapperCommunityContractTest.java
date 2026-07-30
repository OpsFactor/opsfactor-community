package com.opsfactor.community.capability.demandplanning.facade.mapper;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningClusterLevelConfigurationDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningGeneralParametersDTO;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.capability.demandplanning.facade.dto.SimulatedDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.facade.dto.SimulatedDemandPlanMaterialLocationDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Protege o conteudo do DTO de simulacao de Demand Analysis Community.
 *
 * <p>O shape publico ja e congelado por `DemandAnalysisDtoCommunityContractTest`.
 * Este teste cobre a montagem das series, garantindo que o mapper publica
 * apenas material/location, historico observado, historicos tratados por copia
 * e baseline forecast. Support series, campanhas/eventos, forecast agregado e
 * diagnosticos Enterprise nao participam deste mapper Community.</p>
 */
public class DemandAnalysisMapperCommunityContractTest {

    @Test
    public void demandPlanProjectionToDemandModelSetupDTOShouldMapCommunityMaterialLocationSeries() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                0,
                2,
                1);
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        Location location = new Location("LOCATION");
        Produto material = new Produto("MATERIAL");

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        location,
                        material,
                        true);
        demandPlanForecastProjectionMaterialLocation.demanda = new double[]{1.2345d, 2.3456d, 3.4567d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts = new double[]{1.1111d, 2.2222d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers = new double[]{1.0d, 2.0d};
        demandPlanForecastProjectionMaterialLocation.forecastBaseline = new double[]{1.5d, 2.5d, 4.4444d};

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                new DemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters =
                new DemandPlanningGeneralParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.uomId = "UN";

        SalesProjectionLocationMaterialData salesProjectionLocationMaterialData =
                SalesProjectionLocationMaterialData.builder()
                        .conversaoUnidadeMedidaProjection(new TestUnidadeMedidaProjection(unidadeMedida))
                        .calendario(calendario)
                        .locations(Set.of(location))
                        .materiais(Set.of(material))
                        .unidadeMedidaPadraoParaNulos(unidadeMedida)
                        .build();

        SimulatedDemandPlanDTO simulatedDemandPlanDTO = new DemandAnalysisMapper()
                .demandPlanProjectionToDemandModelSetupDTO(
                        demandPlanningClusterLevelConfigurationDTO,
                        calendario,
                        List.<DemandPlanForecastProjection>of(demandPlanForecastProjectionMaterialLocation),
                        salesProjectionLocationMaterialData);

        Assertions.assertEquals(calendario.getPosicaoPeriodoFinalFuturo() + 1, simulatedDemandPlanDTO.periodos.size());
        Assertions.assertEquals(1, simulatedDemandPlanDTO.materialLocationData.size());

        SimulatedDemandPlanMaterialLocationDTO simulatedDemandPlanMaterialLocationDTO =
                simulatedDemandPlanDTO.materialLocationData.get(0);
        Assertions.assertEquals("LOCATION", simulatedDemandPlanMaterialLocationDTO.locationId);
        Assertions.assertEquals("MATERIAL", simulatedDemandPlanMaterialLocationDTO.materialId);
        Assertions.assertArrayEquals(new double[]{1.235d, 2.346d, 3.457d},
                simulatedDemandPlanMaterialLocationDTO.historicalSales,
                0.0001d);
        Assertions.assertArrayEquals(new double[]{1.111d, 2.222d},
                simulatedDemandPlanMaterialLocationDTO.historicalSalesAfterStockoutTreatment,
                0.0001d);
        Assertions.assertArrayEquals(new double[]{1.0d, 2.0d},
                simulatedDemandPlanMaterialLocationDTO.historicalSalesAfterOutlierTreatment,
                0.0001d);
        Assertions.assertArrayEquals(new double[]{1.5d, 2.5d, 4.444d},
                simulatedDemandPlanMaterialLocationDTO.baselineForecast,
                0.0001d);
        Assertions.assertArrayEquals(new double[]{0.265d, 0.154d, 0.987d},
                simulatedDemandPlanMaterialLocationDTO.residual,
                0.0001d);
        Assertions.assertArrayEquals(new double[]{0.265d, 0.154d, 0.987d},
                simulatedDemandPlanMaterialLocationDTO.absoluteResidual,
                0.0001d);
        Assertions.assertNull(simulatedDemandPlanMaterialLocationDTO.trend);
        Assertions.assertNull(simulatedDemandPlanMaterialLocationDTO.seasonal);
        Assertions.assertNull(simulatedDemandPlanMaterialLocationDTO.lowerBound);
        Assertions.assertNull(simulatedDemandPlanMaterialLocationDTO.upperBound);

    }

    @Test
    public void demandPlanProjectionToDemandModelSetupDTOShouldRejectBrokenMaterialLocationSeriesBeforeMapping() {

        Calendario calendario = getCalendarioTeste();

        DemandPlanForecastProjectionMaterialLocation projectionComLocationSemId =
                getDemandPlanForecastProjectionMaterialLocationValida(
                        calendario,
                        new Location(""),
                        new Produto("MATERIAL"));
        assertMappingFails(
                projectionComLocationSemId,
                "location without id");

        DemandPlanForecastProjectionMaterialLocation projectionComForecastCurto =
                getDemandPlanForecastProjectionMaterialLocationValida(calendario);
        projectionComForecastCurto.forecastBaseline = new double[]{1.0d};
        assertMappingFails(
                projectionComForecastCurto,
                "baseline forecast");

        DemandPlanForecastProjectionMaterialLocation projectionComHistoricoNaoFinito =
                getDemandPlanForecastProjectionMaterialLocationValida(calendario);
        projectionComHistoricoNaoFinito.demanda = new double[]{1.0d, Double.NaN};
        assertMappingFails(
                projectionComHistoricoNaoFinito,
                "finite values");

        DemandPlanForecastProjectionMaterialLocation projectionComBoundsIncompletos =
                getDemandPlanForecastProjectionMaterialLocationValida(calendario);
        projectionComBoundsIncompletos.lowerBound = new double[]{0.5d, 0.5d, 0.5d};
        assertMappingFails(
                projectionComBoundsIncompletos,
                "lower and upper bounds together");

    }

    private static void assertMappingFails(
            DemandPlanForecastProjection demandPlanForecastProjection,
            String expectedMessageFragment) {

        Calendario calendario = getCalendarioTeste();
        assertMappingFails(
                () -> new DemandAnalysisMapper().demandPlanProjectionToDemandModelSetupDTO(
                        getDemandPlanningClusterLevelConfigurationDTO(),
                        calendario,
                        List.of(demandPlanForecastProjection),
                        getSalesProjectionLocationMaterialData(
                                calendario,
                                new UnidadeMedida("UN"),
                                new Location("LOCATION"),
                                new Produto("MATERIAL"))),
                expectedMessageFragment);

    }

    private static void assertMappingFails(
            Runnable runnable,
            String expectedMessageFragment) {

        RuntimeException runtimeException = Assertions.assertThrows(
                RuntimeException.class,
                runnable::run);

        Assertions.assertTrue(
                runtimeException.getMessage().contains(expectedMessageFragment),
                runtimeException.getMessage());

    }

    private static Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                0,
                2,
                1);

    }

    private static DemandPlanningClusterLevelConfigurationDTO getDemandPlanningClusterLevelConfigurationDTO() {

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                new DemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters =
                new DemandPlanningGeneralParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.uomId = "UN";
        return demandPlanningClusterLevelConfigurationDTO;

    }

    private static DemandPlanForecastProjectionMaterialLocation getDemandPlanForecastProjectionMaterialLocationValida(
            Calendario calendario) {

        return getDemandPlanForecastProjectionMaterialLocationValida(
                calendario,
                new Location("LOCATION"),
                new Produto("MATERIAL"));

    }

    private static DemandPlanForecastProjectionMaterialLocation getDemandPlanForecastProjectionMaterialLocationValida(
            Calendario calendario,
            Location location,
            Produto material) {

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        location,
                        material,
                        true);
        demandPlanForecastProjectionMaterialLocation.demanda = new double[]{1.0d, 2.0d, 3.0d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts =
                new double[]{1.0d, 2.0d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers =
                new double[]{1.0d, 2.0d};
        demandPlanForecastProjectionMaterialLocation.forecastBaseline =
                new double[]{1.0d, 2.0d, 3.0d};
        return demandPlanForecastProjectionMaterialLocation;

    }

    private static SalesProjectionLocationMaterialData getSalesProjectionLocationMaterialData(
            Calendario calendario,
            UnidadeMedida unidadeMedida,
            Location location,
            Produto material) {

        return SalesProjectionLocationMaterialData.builder()
                .conversaoUnidadeMedidaProjection(new TestUnidadeMedidaProjection(unidadeMedida))
                .calendario(calendario)
                .locations(Set.of(location))
                .materiais(Set.of(material))
                .unidadeMedidaPadraoParaNulos(unidadeMedida)
                .build();

    }

    /**
     * Projection minima para permitir que o mapper resolva a UOM declarada no
     * DTO sem depender de factory Spring nem repository de unidades.
     */
    private static class TestUnidadeMedidaProjection extends UnidadeMedidaProjection {

        private TestUnidadeMedidaProjection(UnidadeMedida unidadeMedida) {

            this.unidadeMedidaSet.add(unidadeMedida);

        }

    }

    private static class DemandPlanForecastProjectionComListaMaterialLocationNula
            extends DemandPlanForecastProjection {

        @Override
        public List<DemandPlanForecastProjectionMaterialLocation> getDemandPlanForecastProjectionMaterialLocationList() {

            return null;

        }

        @Override
        public void agregaForecastEDemandaHistoricaDemandPlanForecastProjectionAPartirNivelDesagregado() {

        }

    }

}
