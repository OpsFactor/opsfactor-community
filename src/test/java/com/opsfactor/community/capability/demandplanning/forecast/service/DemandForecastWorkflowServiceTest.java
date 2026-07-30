package com.opsfactor.community.capability.demandplanning.forecast.service;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosMediaMovel;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionAgregado;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastHistoryCleaningProcessor;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastStockoutTreatmentProcessor;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * Valida a borda Spring transicional do workflow de forecast Community.
 *
 * <p>O teste nao sobe Spring: instancia o service diretamente porque o contrato
 * aqui e puramente de orquestracao. Isso garante que a limpeza historica rode
 * sobre a unidade agregada antes do forecast estatistico e que a desagregacao
 * devolva series finais para as folhas material/location.</p>
 */
class DemandForecastWorkflowServiceTest {

    @Test
    void forecastProcessorsShouldBeExplicitSpringBeans() throws Exception {

        Field demandForecastStockoutTreatmentProcessorField =
                DemandForecastWorkflowService.class.getDeclaredField("demandForecastStockoutTreatmentProcessor");
        Field demandForecastHistoryCleaningProcessorField =
                DemandForecastWorkflowService.class.getDeclaredField("demandForecastHistoryCleaningProcessor");

        Assertions.assertNotNull(demandForecastStockoutTreatmentProcessorField.getAnnotation(Autowired.class));
        Assertions.assertNotNull(demandForecastHistoryCleaningProcessorField.getAnnotation(Autowired.class));
        Assertions.assertNotNull(DemandForecastStockoutTreatmentProcessor.class.getAnnotation(Component.class));
        Assertions.assertNotNull(DemandForecastHistoryCleaningProcessor.class.getAnnotation(Component.class));

    }

    @Test
    void executaForecastEDesagregacaoShouldRejectMissingForecastParameters() {

        DemandForecastWorkflowService demandForecastWorkflowService =
                criaDemandForecastWorkflowService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastWorkflowService.executaForecastEDesagregacao(
                        null,
                        null,
                        null,
                        null,
                        null));
        Assertions.assertEquals(
                "Demand Planning forecast parameters are required",
                illegalArgumentException.getMessage());

    }

    @Test
    void executaForecastEDesagregacaoShouldRejectMissingAggregationParameters() {

        DemandForecastWorkflowService demandForecastWorkflowService =
                criaDemandForecastWorkflowService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastWorkflowService.executaForecastEDesagregacao(
                        null,
                        getParametrosForecastProjectionMediaMovel(),
                        null,
                        null,
                        null));
        Assertions.assertEquals(
                "Demand Planning forecast aggregation parameters are required",
                illegalArgumentException.getMessage());

    }

    @Test
    void executaForecastEDesagregacaoShouldRejectMissingStatisticalModel() {

        DemandForecastWorkflowService demandForecastWorkflowService =
                criaDemandForecastWorkflowService();
        ParametrosForecastProjection parametrosForecastProjection =
                getParametrosForecastProjectionMediaMovel();
        parametrosForecastProjection.setDpModeloEstatistico(null);

        /*
         * Modelo nulo representa payload/configuracao incompleta, nao feature
         * Enterprise bloqueada. A falha deve acontecer na borda comum do
         * workflow antes de processor, engine estatistica ou split.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastWorkflowService.executaForecastEDesagregacao(
                        getCalendarioForecastTeste(),
                        parametrosForecastProjection,
                        getParametrosAgregacaoForecastTopDown(),
                        getDemandPlanForecastProjectionMaterialLocationVazia(),
                        new ClusterEParametrosProjectionComDfusAtivas()));
        Assertions.assertEquals(
                "Demand Planning statistical forecast model is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void executaForecastEDesagregacaoShouldRejectMissingSplitModel() {

        DemandForecastWorkflowService demandForecastWorkflowService =
                criaDemandForecastWorkflowService();
        ParametrosForecastProjection parametrosForecastProjection =
                getParametrosForecastProjectionMediaMovel();
        parametrosForecastProjection.setDpModeloSplit(null);

        /*
         * Split nulo tambem e contrato quebrado da configuracao. Tratar isso
         * como erro funcional evita que a rotina caia em mensagens genericas
         * de switch ou em falso bloqueio Enterprise.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastWorkflowService.executaForecastEDesagregacao(
                        getCalendarioForecastTeste(),
                        parametrosForecastProjection,
                        getParametrosAgregacaoForecastTopDown(),
                        getDemandPlanForecastProjectionMaterialLocationVazia(),
                        new ClusterEParametrosProjectionComDfusAtivas()));
        Assertions.assertEquals(
                "Demand Planning forecast split model is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void executaForecastEDesagregacaoShouldRejectMissingCalendar() {

        DemandForecastWorkflowService demandForecastWorkflowService =
                criaDemandForecastWorkflowService();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                getDemandPlanForecastProjectionMaterialLocationVazia();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastWorkflowService.executaForecastEDesagregacao(
                        null,
                        getParametrosForecastProjectionMediaMovel(),
                        getParametrosAgregacaoForecastTopDown(),
                        demandPlanForecastProjectionMaterialLocation,
                        new ClusterEParametrosProjectionComDfusAtivas()));
        Assertions.assertEquals(
                "Demand Planning calendar is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void executaForecastEDesagregacaoShouldRejectMissingForecastProjection() {

        DemandForecastWorkflowService demandForecastWorkflowService =
                criaDemandForecastWorkflowService();
        Calendario calendario = getCalendarioForecastTeste();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastWorkflowService.executaForecastEDesagregacao(
                        calendario,
                        getParametrosForecastProjectionMediaMovel(),
                        getParametrosAgregacaoForecastTopDown(),
                        null,
                        new ClusterEParametrosProjectionComDfusAtivas()));
        Assertions.assertEquals(
                "Demand Planning forecast projection is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void executaForecastEDesagregacaoShouldRejectMissingClusterProjectionWhenDisaggregationIsRequired() {

        DemandForecastWorkflowService demandForecastWorkflowService =
                criaDemandForecastWorkflowService();
        Calendario calendario = getCalendarioForecastTeste();
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                new DemandPlanForecastProjectionAgregado(
                        calendario,
                        new UnidadeMedida("UN"),
                        false);
        demandPlanForecastProjectionAgregado.demanda[1] = 40.0d;
        demandPlanForecastProjectionAgregado.demanda[2] = 60.0d;

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastWorkflowService.executaForecastEDesagregacao(
                        calendario,
                        getParametrosForecastProjectionMediaMovel(),
                        getParametrosAgregacaoForecastTopDown(),
                        demandPlanForecastProjectionAgregado,
                        null));

        Assertions.assertEquals(
                "Demand Planning cluster parameters projection is required for forecast disaggregation",
                illegalArgumentException.getMessage());

    }

    @Test
    void executaForecastEDesagregacaoShouldTreatAggregateHistoryBeforeHistoricalSalesSplit() {

        DemandForecastWorkflowService demandForecastWorkflowService = criaDemandForecastWorkflowService();
        Calendario calendario = getCalendarioForecastTeste();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                new DemandPlanForecastProjectionAgregado(
                        calendario,
                        unidadeMedida,
                        false);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationA =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        new Location("LOCATION_A"),
                        new Produto("MATERIAL_A"),
                        false);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationB =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        new Location("LOCATION_B"),
                        new Produto("MATERIAL_B"),
                        false);
        ParametrosForecastProjection parametrosForecastProjection =
                getParametrosForecastProjectionMediaMovel();

        /*
         * O agregado tem historico total 100 nos dois ultimos periodos. Os leafs
         * guardam apenas a serie observada usada para calcular o mix 40%/60%.
         */
        demandPlanForecastProjectionAgregado.demanda[1] = 40.0d;
        demandPlanForecastProjectionAgregado.demanda[2] = 60.0d;
        demandPlanForecastProjectionMaterialLocationA.demanda[1] = 10.0d;
        demandPlanForecastProjectionMaterialLocationA.demanda[2] = 30.0d;
        demandPlanForecastProjectionMaterialLocationB.demanda[1] = 30.0d;
        demandPlanForecastProjectionMaterialLocationB.demanda[2] = 30.0d;
        demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionDesagregados()
                .add(demandPlanForecastProjectionMaterialLocationA);
        demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionDesagregados()
                .add(demandPlanForecastProjectionMaterialLocationB);

        demandForecastWorkflowService.executaForecastEDesagregacao(
                calendario,
                parametrosForecastProjection,
                getParametrosAgregacaoForecastTopDown(),
                demandPlanForecastProjectionAgregado,
                new ClusterEParametrosProjectionComDfusAtivas());

        Assertions.assertEquals(
                50.0d,
                demandPlanForecastProjectionAgregado.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                20.0d,
                demandPlanForecastProjectionMaterialLocationA.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                30.0d,
                demandPlanForecastProjectionMaterialLocationB.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                24.0d,
                demandPlanForecastProjectionMaterialLocationA.vendaHistoricaTratamentoStockouts[2],
                0.0001d);
        Assertions.assertEquals(
                24.0d,
                demandPlanForecastProjectionMaterialLocationA.vendaHistoricaTratamentoOutliers[2],
                0.0001d);
        Assertions.assertEquals(
                36.0d,
                demandPlanForecastProjectionMaterialLocationB.vendaHistoricaTratamentoStockouts[2],
                0.0001d);
        Assertions.assertEquals(
                36.0d,
                demandPlanForecastProjectionMaterialLocationB.vendaHistoricaTratamentoOutliers[2],
                0.0001d);

    }

    @Test
    void executaForecastEDesagregacaoShouldAllowBottomUpWithoutClusterProjection() {

        DemandForecastWorkflowService demandForecastWorkflowService = criaDemandForecastWorkflowService();
        Calendario calendario = getCalendarioForecastTeste();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);
        demandPlanForecastProjectionMaterialLocation.demanda[1] = 40.0d;
        demandPlanForecastProjectionMaterialLocation.demanda[2] = 60.0d;

        demandForecastWorkflowService.executaForecastEDesagregacao(
                calendario,
                getParametrosForecastProjectionMediaMovel(),
                getParametrosAgregacaoForecastBottomUp(),
                demandPlanForecastProjectionMaterialLocation,
                null);

        /*
         * Bottom-up executa o forecast diretamente no leaf material/location.
         * Nao ha decisao de DFU ativa nem redistribuicao top-down, portanto o
         * workflow nao deve exigir ClusterEParametrosProjection nesse caminho.
         */
        Assertions.assertEquals(
                50.0d,
                demandPlanForecastProjectionMaterialLocation.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);

    }

    @Test
    void executaForecastEDesagregacaoShouldFailWhenTopDownConfigReceivesLeafProjection() {

        DemandForecastWorkflowService demandForecastWorkflowService = criaDemandForecastWorkflowService();
        Calendario calendario = getCalendarioForecastTeste();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);
        demandPlanForecastProjectionMaterialLocation.demanda[1] = 40.0d;
        demandPlanForecastProjectionMaterialLocation.demanda[2] = 60.0d;

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandForecastWorkflowService.executaForecastEDesagregacao(
                        calendario,
                        getParametrosForecastProjectionMediaMovel(),
                        getParametrosAgregacaoForecastTopDown(),
                        demandPlanForecastProjectionMaterialLocation,
                        new ClusterEParametrosProjectionComDfusAtivas()));

        Assertions.assertTrue(
                illegalStateException.getMessage().contains("requer desagregacao"));

    }

    @Test
    void executaForecastEDesagregacaoShouldFailWhenBottomUpConfigReceivesAggregateProjection() {

        DemandForecastWorkflowService demandForecastWorkflowService = criaDemandForecastWorkflowService();
        Calendario calendario = getCalendarioForecastTeste();
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                new DemandPlanForecastProjectionAgregado(
                        calendario,
                        new UnidadeMedida("UN"),
                        false);
        demandPlanForecastProjectionAgregado.demanda[1] = 40.0d;
        demandPlanForecastProjectionAgregado.demanda[2] = 60.0d;

        /*
         * Bottom-up material/location nao tem etapa posterior de abertura. Se a
         * factory entregar um agregado nesse caminho, o forecast ficaria preso
         * na unidade errada e a persistencia final nao receberia series DFU.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandForecastWorkflowService.executaForecastEDesagregacao(
                        calendario,
                        getParametrosForecastProjectionMediaMovel(),
                        getParametrosAgregacaoForecastBottomUp(),
                        demandPlanForecastProjectionAgregado,
                        null));

        Assertions.assertTrue(
                illegalStateException.getMessage().contains("nao requer desagregacao"));

    }

    private static ParametrosForecastProjection getParametrosForecastProjectionMediaMovel() {

        return new ParametrosForecastProjection(
                Constantes.DPModeloEstatistico.MM,
                new ParametrosMediaMovel(2),
                null,
                null,
                null,
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                2);

    }

    private static Calendario getCalendarioForecastTeste() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);

    }

    private static DemandPlanForecastProjectionMaterialLocation getDemandPlanForecastProjectionMaterialLocationVazia() {

        Calendario calendario = getCalendarioForecastTeste();
        return new DemandPlanForecastProjectionMaterialLocation(
                calendario,
                new UnidadeMedida("UN"),
                new Location("LOCATION"),
                new Produto("MATERIAL"),
                false);

    }

    private static DemandForecastWorkflowService criaDemandForecastWorkflowService() {

        DemandForecastWorkflowService demandForecastWorkflowService =
                new DemandForecastWorkflowService();
        ReflectionTestUtils.setField(
                demandForecastWorkflowService,
                "demandForecastStockoutTreatmentProcessor",
                new DemandForecastStockoutTreatmentProcessor());
        ReflectionTestUtils.setField(
                demandForecastWorkflowService,
                "demandForecastHistoryCleaningProcessor",
                new DemandForecastHistoryCleaningProcessor());
        return demandForecastWorkflowService;

    }

    private static ParametrosAgregacaoForecast getParametrosAgregacaoForecastTopDown() {

        return new ParametrosAgregacaoForecast(
                Constantes.DPNivelAgregacao.TOP_DOWN,
                Constantes.DPNivelAgregacao.TOP_DOWN);

    }

    private static ParametrosAgregacaoForecast getParametrosAgregacaoForecastBottomUp() {

        return new ParametrosAgregacaoForecast(
                Constantes.DPNivelAgregacao.BOTTOM_UP,
                Constantes.DPNivelAgregacao.BOTTOM_UP);

    }

    private static class ClusterEParametrosProjectionComDfusAtivas extends ClusterEParametrosProjection {

        @Override
        public boolean isDfuAtiva(Produto material, Location location) {

            return true;

        }

    }

}
