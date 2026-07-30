package com.opsfactor.community.capability.demandplanning.engine;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.HistoricoDemandPlanItem;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosLimpezaHistoricoForecast;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosMediaMovel;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionAgregado;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.capability.demandplanning.forecast.statisticalmodel.engine.MovingAverageForecastEngine;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastHistoryCleaningProcessor;
import com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine.DemandForecastStockoutTreatmentProcessor;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Valida contratos Community das rotinas estatisticas de Demand Planning.
 *
 * <p>O Community aceita apenas split por venda historica. Os demais valores
 * permanecem no enum compartilhado para payloads Enterprise/legados, mas a
 * rotina aberta deve falhar com erro funcional claro antes de executar qualquer
 * caminho de desagregacao privado.</p>
 */
class DemandPlanningCommunityContractTest {

    @Test
    void communityHistoryProcessorsShouldKeepObservedSalesWithoutNormalization() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);
        demandPlanForecastProjectionMaterialLocation.demanda[0] = 10.0d;
        demandPlanForecastProjectionMaterialLocation.demanda[1] = 0.0d;
        demandPlanForecastProjectionMaterialLocation.demanda[2] = 30.0d;

        DemandForecastStockoutTreatmentProcessor demandForecastStockoutTreatmentProcessor =
                new DemandForecastStockoutTreatmentProcessor();
        DemandForecastHistoryCleaningProcessor demandForecastHistoryCleaningProcessor =
                new DemandForecastHistoryCleaningProcessor();

        demandForecastStockoutTreatmentProcessor.processa(demandPlanForecastProjectionMaterialLocation);
        demandForecastHistoryCleaningProcessor.processa(demandPlanForecastProjectionMaterialLocation);

        Assertions.assertArrayEquals(
                demandPlanForecastProjectionMaterialLocation.demanda,
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts);
        Assertions.assertArrayEquals(
                demandPlanForecastProjectionMaterialLocation.demanda,
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers);
        Assertions.assertNotSame(
                demandPlanForecastProjectionMaterialLocation.demanda,
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts);
        Assertions.assertNotSame(
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts,
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers);

    }

    @Test
    void communityHistoryProcessorsShouldKeepOnlyHistoricalWindowWhenDemandArrayHasFutureHorizon() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        true);
        demandPlanForecastProjectionMaterialLocation.demanda[0] = 10.0d;
        demandPlanForecastProjectionMaterialLocation.demanda[1] = 0.0d;
        demandPlanForecastProjectionMaterialLocation.demanda[2] = 30.0d;
        demandPlanForecastProjectionMaterialLocation.demanda[calendario.getPosicaoPeriodoPresente()] = 999.0d;

        DemandForecastStockoutTreatmentProcessor demandForecastStockoutTreatmentProcessor =
                new DemandForecastStockoutTreatmentProcessor();
        DemandForecastHistoryCleaningProcessor demandForecastHistoryCleaningProcessor =
                new DemandForecastHistoryCleaningProcessor();

        demandForecastStockoutTreatmentProcessor.processa(demandPlanForecastProjectionMaterialLocation);
        demandForecastHistoryCleaningProcessor.processa(demandPlanForecastProjectionMaterialLocation);

        /*
         * `demanda` pode carregar horizonte total para exibicao/simulacao, mas
         * as engines estatisticas treinam somente sobre a janela historica.
         * O valor futuro 999 nao deve aparecer em nenhuma serie tratada.
         */
        Assertions.assertEquals(
                calendario.getNumeroPeriodosPassados(),
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts.length);
        Assertions.assertArrayEquals(
                new double[]{10.0d, 0.0d, 30.0d},
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts);
        Assertions.assertArrayEquals(
                new double[]{10.0d, 0.0d, 30.0d},
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers);
        Assertions.assertNotSame(
                demandPlanForecastProjectionMaterialLocation.demanda,
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts);
        Assertions.assertNotSame(
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts,
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers);

    }

    @Test
    void communityStockoutProcessorShouldRejectEnterpriseStockoutTreatmentConfiguration() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);
        demandPlanForecastProjectionMaterialLocation.demanda[0] = 10.0d;

        DemandForecastStockoutTreatmentProcessor demandForecastStockoutTreatmentProcessor =
                new DemandForecastStockoutTreatmentProcessor();
        ParametrosForecastProjection parametrosForecastProjection =
                getParametrosForecastProjectionComLimpezaHistorica(
                        true,
                        Constantes.DPModeloNormalizacao.DESATIVADO);

        /*
         * O Community nao possui projection historica de estoque no workflow.
         * Quando um payload transicional ativa esse tratamento, a etapa deve
         * falhar como capability Enterprise em vez de copiar a serie observada
         * e aparentar que suavizou periodos com stockout.
         */
        RequiresEnterpriseVersionException requiresEnterpriseVersionException =
                Assertions.assertThrows(
                        RequiresEnterpriseVersionException.class,
                        () -> demandForecastStockoutTreatmentProcessor.processa(
                                calendario,
                                demandPlanForecastProjectionMaterialLocation,
                                parametrosForecastProjection));

        Assertions.assertTrue(
                requiresEnterpriseVersionException.getMessage().contains(
                        "Demand Planning stockout treatment"));

    }

    @Test
    void communityHistoryCleaningProcessorShouldRejectEnterpriseHistoryCleaningConfiguration() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        DemandForecastHistoryCleaningProcessor demandForecastHistoryCleaningProcessor =
                new DemandForecastHistoryCleaningProcessor();

        for (Constantes.DPModeloNormalizacao dpModeloNormalizacao : List.of(
                Constantes.DPModeloNormalizacao.PERCENTIS,
                Constantes.DPModeloNormalizacao.CAMPANHA)) {
            DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                    new DemandPlanForecastProjectionMaterialLocation(
                            calendario,
                            new UnidadeMedida("UN"),
                            new Location("LOCATION"),
                            new Produto("MATERIAL"),
                            false);
            demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts[0] = 10.0d;
            ParametrosForecastProjection parametrosForecastProjection =
                    getParametrosForecastProjectionComLimpezaHistorica(
                            false,
                            dpModeloNormalizacao);

            /*
             * PERCENTIS e CAMPANHA ficam no enum compartilhado para DTOs e
             * discovery visual. A rotina Community, porem, nao pode tratar
             * essas configuracoes como no-op; PERCENTIS e CAMPANHA sao
             * reabertos pelo processor Enterprise, mas seguem bloqueados no
             * processor Community.
             */
            RequiresEnterpriseVersionException requiresEnterpriseVersionException =
                    Assertions.assertThrows(
                            RequiresEnterpriseVersionException.class,
                            () -> demandForecastHistoryCleaningProcessor.processa(
                                    calendario,
                                    demandPlanForecastProjectionMaterialLocation,
                                    parametrosForecastProjection),
                            "Modelo Enterprise deveria falhar no processor Community: "
                                    + dpModeloNormalizacao);

            Assertions.assertTrue(
                    requiresEnterpriseVersionException.getMessage().contains(
                            "Demand Planning outlier/campaign history cleaning"));
        }

    }

    @Test
    void communityStockoutProcessorShouldRejectIncompleteForecastProjection() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        DemandForecastStockoutTreatmentProcessor demandForecastStockoutTreatmentProcessor =
                new DemandForecastStockoutTreatmentProcessor();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);

        IllegalArgumentException projectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastStockoutTreatmentProcessor.processa(null));
        demandPlanForecastProjectionMaterialLocation.demanda = null;
        IllegalArgumentException demandaException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastStockoutTreatmentProcessor.processa(demandPlanForecastProjectionMaterialLocation));
        demandPlanForecastProjectionMaterialLocation.demanda = new double[]{10.0d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts = null;
        IllegalArgumentException stockoutTargetException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastStockoutTreatmentProcessor.processa(demandPlanForecastProjectionMaterialLocation));
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts = new double[]{0.0d, 0.0d};
        IllegalArgumentException lengthException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastStockoutTreatmentProcessor.processa(demandPlanForecastProjectionMaterialLocation));

        Assertions.assertEquals(
                "Demand Plan forecast projection is required for stockout treatment.",
                projectionException.getMessage());
        Assertions.assertEquals(
                "Historical demand series is required for stockout treatment.",
                demandaException.getMessage());
        Assertions.assertEquals(
                "Stockout treatment target series is required.",
                stockoutTargetException.getMessage());
        Assertions.assertEquals(
                "Stockout treatment target series cannot be longer than historical demand series.",
                lengthException.getMessage());

    }

    private static ParametrosForecastProjection getParametrosForecastProjectionComLimpezaHistorica(
            boolean consideraDadosEstoque,
            Constantes.DPModeloNormalizacao dpModeloNormalizacao) {

        return new ParametrosForecastProjection(
                Constantes.DPModeloEstatistico.MM,
                new ParametrosMediaMovel(2),
                null,
                null,
                null,
                null,
                null,
                new ParametrosLimpezaHistoricoForecast(
                        consideraDadosEstoque,
                        dpModeloNormalizacao,
                        7,
                        true,
                        0.90d,
                        true,
                        0.10d,
                        null),
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                2);

    }

    @Test
    void desagregaForecastShouldRejectEnterpriseSplitModelsCommunity() {

        assertRequiresEnterpriseVersionException(Constantes.DPModeloSplit.FORECAST_PROPORTION);
        assertRequiresEnterpriseVersionException(Constantes.DPModeloSplit.HTS);

    }

    @Test
    void desagregaForecastShouldRejectMissingSplitModelBeforeSwitchingCommunityDisaggregation() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanning.desagregaForecast(
                        (Calendario) null,
                        null,
                        0,
                        null,
                        null));

        Assertions.assertEquals(
                "Demand Planning Split Model is required for Community forecast disaggregation.",
                illegalArgumentException.getMessage());

    }

    @Test
    void statisticalForecastShouldRejectEnterpriseModelsCommunity() {

        for (Constantes.DPModeloEstatistico dpModeloEstatisticoEnterprise : new Constantes.DPModeloEstatistico[]{
                Constantes.DPModeloEstatistico.SNAIVE,
                Constantes.DPModeloEstatistico.STL,
                Constantes.DPModeloEstatistico.PROPHET,
                Constantes.DPModeloEstatistico.ETS,
                Constantes.DPModeloEstatistico.TBATS,
                Constantes.DPModeloEstatistico.BUDGET_DECOMPOSITION,
                Constantes.DPModeloEstatistico.CHRONOS,
                Constantes.DPModeloEstatistico.PRICING_ML}) {
            assertRequiresEnterpriseVersionException(dpModeloEstatisticoEnterprise);
        }

    }

    @Test
    void statisticalForecastShouldRejectMissingForecastModelBeforeSwitchingCommunityEngine() {

        ParametrosForecastProjection parametrosForecastProjection =
                getParametrosForecastProjection(Constantes.DPModeloEstatistico.MM);
        parametrosForecastProjection.dpModeloEstatistico = null;

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanning.geraForecastAgregadoNoDemandPlanForecastProjection(
                        null,
                        parametrosForecastProjection,
                        null));

        Assertions.assertEquals(
                "Demand Planning Forecast Model is required for Community statistical forecast execution.",
                illegalArgumentException.getMessage());

    }

    @Test
    void movingAverageAndRollingMovingAverageShouldKeepTheirDistinctCommunityBehavior() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        ParametrosForecastProjection parametrosMediaMovel = getParametrosForecastProjection(
                Constantes.DPModeloEstatistico.MM);
        ParametrosForecastProjection parametrosMediaMovelRolling = getParametrosForecastProjection(
                Constantes.DPModeloEstatistico.RMM);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationMediaMovel =
                getDemandPlanForecastProjectionMaterialLocationComHistorico(calendario);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationMediaMovelRolling =
                getDemandPlanForecastProjectionMaterialLocationComHistorico(calendario);

        DemandPlanning.geraForecastAgregadoNoDemandPlanForecastProjection(
                calendario,
                parametrosMediaMovel,
                demandPlanForecastProjectionMaterialLocationMediaMovel);
        DemandPlanning.geraForecastAgregadoNoDemandPlanForecastProjection(
                calendario,
                parametrosMediaMovelRolling,
                demandPlanForecastProjectionMaterialLocationMediaMovelRolling);

        Assertions.assertEquals(
                30.0d,
                demandPlanForecastProjectionMaterialLocationMediaMovel.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                30.0d,
                demandPlanForecastProjectionMaterialLocationMediaMovel.forecastBaseline[calendario.getPosicaoPeriodoPresente() + 1],
                0.0001d);
        Assertions.assertEquals(
                30.0d,
                demandPlanForecastProjectionMaterialLocationMediaMovelRolling.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                35.0d,
                demandPlanForecastProjectionMaterialLocationMediaMovelRolling.forecastBaseline[calendario.getPosicaoPeriodoPresente() + 1],
                0.0001d);

    }

    @Test
    void forecastProjectionGenerationShouldRejectSalesProjectionWithDifferentBucket() {

        Calendario calendarioDemandPlan = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        Calendario calendarioSalesProjection = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.SEMANAL,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        SalesProjectionLocationMaterialData salesProjectionLocationMaterialData =
                SalesProjectionLocationMaterialData.builder()
                        .calendario(calendarioSalesProjection)
                        .build();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanning.geraDemandPlanForecastProjectionMaterialLocationListComDemandaHistoricaPopuladaCommunity(
                        calendarioDemandPlan,
                        null,
                        null,
                        null,
                        false,
                        salesProjectionLocationMaterialData,
                        null,
                        false));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "DemandPlanning requires the Demand Plan calendar bucket to match the Sales Projection calendar bucket"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("demand bucket=DIARIO"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("sales bucket=SEMANAL"));

    }

    @Test
    void forecastProjectionGenerationShouldRejectMissingStructuralInputsBeforeLoopingSales() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        SalesProjectionLocationMaterialData salesProjectionLocationMaterialData =
                SalesProjectionLocationMaterialData.builder()
                        .calendario(calendario)
                        .build();
        LocationProjection locationProjection = new LocationProjection();
        MaterialProjection materialProjection = new MaterialProjection();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        ClusterEParametrosProjection clusterEParametrosProjection =
                new ClusterEParametrosProjectionComDfusAtivas();

        /*
         * Esta rotina estatica e uma fachada transicional do legado e pode ser
         * chamada diretamente fora do service. Mesmo com sales vazio, os
         * snapshots estruturais precisam estar presentes para diferenciar
         * "nenhuma DFU no recorte" de "caller nao carregou projection".
         */
        IllegalArgumentException locationProjectionAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanning.geraDemandPlanForecastProjectionMaterialLocationListComDemandaHistoricaPopuladaCommunity(
                        calendario,
                        null,
                        materialProjection,
                        unidadeMedida,
                        false,
                        salesProjectionLocationMaterialData,
                        clusterEParametrosProjection,
                        false));
        IllegalArgumentException materialProjectionAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanning.geraDemandPlanForecastProjectionMaterialLocationListComDemandaHistoricaPopuladaCommunity(
                        calendario,
                        locationProjection,
                        null,
                        unidadeMedida,
                        false,
                        salesProjectionLocationMaterialData,
                        clusterEParametrosProjection,
                        false));
        IllegalArgumentException unidadeMedidaAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanning.geraDemandPlanForecastProjectionMaterialLocationListComDemandaHistoricaPopuladaCommunity(
                        calendario,
                        locationProjection,
                        materialProjection,
                        null,
                        false,
                        salesProjectionLocationMaterialData,
                        clusterEParametrosProjection,
                        false));
        IllegalArgumentException clusterEParametrosProjectionAusenteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> DemandPlanning.geraDemandPlanForecastProjectionMaterialLocationListComDemandaHistoricaPopuladaCommunity(
                        calendario,
                        locationProjection,
                        materialProjection,
                        unidadeMedida,
                        false,
                        salesProjectionLocationMaterialData,
                        null,
                        false));

        Assertions.assertEquals(
                "Demand Planning location projection is required for material/location forecast series generation.",
                locationProjectionAusenteException.getMessage());
        Assertions.assertEquals(
                "Demand Planning material projection is required for material/location forecast series generation.",
                materialProjectionAusenteException.getMessage());
        Assertions.assertEquals(
                "Demand Planning default UOM is required for material/location forecast series generation.",
                unidadeMedidaAusenteException.getMessage());
        Assertions.assertEquals(
                "Demand Planning cluster and parameters projection is required for material/location forecast series generation.",
                clusterEParametrosProjectionAusenteException.getMessage());

    }

    @Test
    void forecastProjectionGenerationShouldRejectBrokenMaterialAndLocationSnapshots() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        SalesProjectionLocationMaterialData salesProjectionLocationMaterialData =
                SalesProjectionLocationMaterialData.builder()
                        .calendario(calendario)
                        .build();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        ClusterEParametrosProjection clusterEParametrosProjection =
                new ClusterEParametrosProjectionComDfusAtivas();

        /*
         * Collections nulas dentro das projections indicam snapshot corrompido,
         * diferente de set vazio. O forecast deve falhar cedo antes de iterar
         * sales ou pular a rodada como se nao houvesse dados.
         */
        IllegalStateException locationSetNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> DemandPlanning.geraDemandPlanForecastProjectionMaterialLocationListComDemandaHistoricaPopuladaCommunity(
                        calendario,
                        new LocationProjectionComSetNulo(),
                        new MaterialProjection(),
                        unidadeMedida,
                        false,
                        salesProjectionLocationMaterialData,
                        clusterEParametrosProjection,
                        false));
        IllegalStateException materialSetNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> DemandPlanning.geraDemandPlanForecastProjectionMaterialLocationListComDemandaHistoricaPopuladaCommunity(
                        calendario,
                        new LocationProjection(),
                        new MaterialProjectionComSetNulo(),
                        unidadeMedida,
                        false,
                        salesProjectionLocationMaterialData,
                        clusterEParametrosProjection,
                        false));

        Assertions.assertEquals(
                "LocationProjection returned null location set for material/location forecast series generation.",
                locationSetNuloException.getMessage());
        Assertions.assertEquals(
                "MaterialProjection returned null material set for material/location forecast series generation.",
                materialSetNuloException.getMessage());

    }

    @Test
    void statisticalEnginesShouldRequireDisaggregationOnlyWhenAggregationIsTopDown() {

        MovingAverageForecastEngine movingAverageForecastEngine = new MovingAverageForecastEngine();
        ParametrosForecastProjection parametrosForecastProjection = getParametrosForecastProjection(
                Constantes.DPModeloEstatistico.MM);

        /*
         * O contrato estatistico Community e independente do modelo concreto:
         * bottom-up puro ja produz material/location; qualquer top-down cria
         * uma serie agregada que precisa ser aberta depois do forecast.
         */
        Assertions.assertFalse(
                movingAverageForecastEngine.requerDesagregacao(
                        parametrosForecastProjection,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.BOTTOM_UP,
                                Constantes.DPNivelAgregacao.BOTTOM_UP)));
        Assertions.assertTrue(
                movingAverageForecastEngine.requerDesagregacao(
                        parametrosForecastProjection,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.TOP_DOWN,
                                Constantes.DPNivelAgregacao.BOTTOM_UP)));
        Assertions.assertTrue(
                movingAverageForecastEngine.requerDesagregacao(
                        parametrosForecastProjection,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.BOTTOM_UP,
                                Constantes.DPNivelAgregacao.TOP_DOWN)));
        Assertions.assertTrue(
                movingAverageForecastEngine.requerDesagregacao(
                        parametrosForecastProjection,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.TOP_DOWN,
                                Constantes.DPNivelAgregacao.TOP_DOWN)));

    }

    @Test
    void communityForecastEnginesShouldRemainStateless() throws IOException {

        Path communityRoutinesModuleDirectory = resolveCommunityRoutinesModuleDirectory();
        Path forecastEngineDirectory = communityRoutinesModuleDirectory
                .resolve("src/main/java/com/opsfactor/community/capability/demandplanning/forecast/engine");
        List<String> violations = new ArrayList<>();

        /*
         * O workflow de Demand Planning pode processar clusters em paralelo.
         * Engines Community sao objetos puros/stateless: parametros, calendario
         * e resultado da rodada entram por argumento e ficam nas projections,
         * nunca em atributos mutaveis da propria engine.
         */
        for (Path javaSourcePath : findJavaSources(forecastEngineDirectory)) {
            if ("package-info.java".equals(javaSourcePath.getFileName().toString())) {
                continue;
            }

            List<String> sourceLines = Files.readAllLines(javaSourcePath, StandardCharsets.UTF_8);
            for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
                String sourceLine = sourceLines.get(lineIndex);
                if (isPrivateInstanceField(sourceLine)) {
                    violations.add(formatViolation(
                            communityRoutinesModuleDirectory,
                            javaSourcePath,
                            lineIndex,
                            sourceLine));
                }
            }
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "Engines Community de Demand Forecast devem permanecer stateless:\n"
                        + String.join("\n", violations));

    }

    @Test
    @SuppressWarnings("deprecation")
    void communityDemandPlanItemTotalShouldIgnoreEnterpriseKeyFigures() throws Exception {

        DemandPlanItem demandPlanItem = new DemandPlanItem();
        demandPlanItem.setQuantidadeBaseline(10.0);
        demandPlanItem.setQuantidadeAjusteDemanda(2.0);
        demandPlanItem.setQuantidadeItensNovos(50.0);
        demandPlanItem.setQuantidadeUplift(40.0);
        demandPlanItem.setQuantidadeBaselineAtendida(7.0);
        demandPlanItem.setQuantidadeAjusteDemandaAtendida(3.0);
        demandPlanItem.setQuantidadeItensNovosAtendida(30.0);
        demandPlanItem.setQuantidadeUpliftAtendida(20.0);

        Assertions.assertEquals(
                12.0,
                invokeGetQuantidadeTotalCommunityDemandPlanItem(
                        demandPlanItem,
                        Constantes.TipoPlano.PLANO_IRRESTRITO),
                0.0001d);
        Assertions.assertEquals(
                10.0,
                invokeGetQuantidadeTotalCommunityDemandPlanItem(
                        demandPlanItem,
                        Constantes.TipoPlano.PLANO_RESTRITO),
                0.0001d);

    }

    @Test
    void historicoDemandPlanItemGenerationShouldPersistNamedHistoricalTreatmentStages() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        DemandPlan demandPlan = new DemandPlan();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts[0] = 10.0d;
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[0] = 9.0d;
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts[1] = 20.0d;
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[1] = 18.0d;

        List<HistoricoDemandPlanItem> historicoDemandPlanItemList =
                DemandPlanning.geraHistoricoDemandPlanItemListDeDemandPlanForecastProjectionsExecucao(
                        demandPlan,
                        List.of(demandPlanForecastProjectionMaterialLocation),
                        calendario,
                        null,
                        new ClusterEParametrosProjectionComDfusAtivas());

        Assertions.assertEquals(
                2,
                historicoDemandPlanItemList.size());
        Assertions.assertEquals(
                10.0d,
                historicoDemandPlanItemList.get(0).getVendaHistoricaTratamentoStockouts(),
                0.0001d);
        Assertions.assertEquals(
                9.0d,
                historicoDemandPlanItemList.get(0).getVendaHistoricaTratamentoOutliers(),
                0.0001d);
        Assertions.assertEquals(
                20.0d,
                historicoDemandPlanItemList.get(1).getQuantidadeBase(),
                0.0001d);
        Assertions.assertEquals(
                18.0d,
                historicoDemandPlanItemList.get(1).getQuantidadeNormalizada(),
                0.0001d);

    }

    @Test
    void historicalSalesDisaggregationShouldSplitAggregateForecastByRecentHistoricalMix() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
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
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                new DemandPlanForecastProjectionAgregado(
                        calendario,
                        unidadeMedida,
                        false);

        /*
         * Com janela de dois dias, o primeiro leaf soma 40 e o segundo soma 60
         * nos dois ultimos periodos historicos. O forecast agregado deve cair
         * nos leafs com participacao 40%/60%.
         */
        demandPlanForecastProjectionMaterialLocationA.demanda[1] = 10.0d;
        demandPlanForecastProjectionMaterialLocationA.demanda[2] = 30.0d;
        demandPlanForecastProjectionMaterialLocationB.demanda[1] = 30.0d;
        demandPlanForecastProjectionMaterialLocationB.demanda[2] = 30.0d;
        demandPlanForecastProjectionAgregado.forecastBaseline[calendario.getPosicaoPeriodoPresente()] = 100.0d;
        demandPlanForecastProjectionAgregado.forecastBaseline[calendario.getPosicaoPeriodoPresente() + 1] = 200.0d;
        demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionDesagregados()
                .add(demandPlanForecastProjectionMaterialLocationA);
        demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionDesagregados()
                .add(demandPlanForecastProjectionMaterialLocationB);

        DemandPlanning.desagregaForecast(
                calendario,
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                2,
                demandPlanForecastProjectionAgregado,
                new ClusterEParametrosProjectionComDfusAtivas());

        Assertions.assertEquals(
                40.0d,
                demandPlanForecastProjectionMaterialLocationA.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                80.0d,
                demandPlanForecastProjectionMaterialLocationA.forecastBaseline[calendario.getPosicaoPeriodoPresente() + 1],
                0.0001d);
        Assertions.assertEquals(
                60.0d,
                demandPlanForecastProjectionMaterialLocationB.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                120.0d,
                demandPlanForecastProjectionMaterialLocationB.forecastBaseline[calendario.getPosicaoPeriodoPresente() + 1],
                0.0001d);

    }

    @Test
    void historicalSalesDisaggregationShouldRedistributeInactiveDfuShareToActiveDfus() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationAtiva =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        new Location("LOCATION_ACTIVE"),
                        new Produto("MATERIAL_ACTIVE"),
                        false);
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocationInativa =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        unidadeMedida,
                        new Location("LOCATION_INACTIVE"),
                        new Produto("MATERIAL_INACTIVE"),
                        false);
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                new DemandPlanForecastProjectionAgregado(
                        calendario,
                        unidadeMedida,
                        false);

        /*
         * A DFU inativa concentra a maior parte do historico, mas nao deve
         * receber forecast Community. A participacao dela e retirada do
         * denominador e o forecast agregado fica todo com a DFU ativa.
         */
        demandPlanForecastProjectionMaterialLocationAtiva.demanda[1] = 5.0d;
        demandPlanForecastProjectionMaterialLocationAtiva.demanda[2] = 5.0d;
        demandPlanForecastProjectionMaterialLocationInativa.demanda[1] = 45.0d;
        demandPlanForecastProjectionMaterialLocationInativa.demanda[2] = 45.0d;
        demandPlanForecastProjectionAgregado.forecastBaseline[calendario.getPosicaoPeriodoPresente()] = 100.0d;
        demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionDesagregados()
                .add(demandPlanForecastProjectionMaterialLocationAtiva);
        demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionDesagregados()
                .add(demandPlanForecastProjectionMaterialLocationInativa);

        DemandPlanning.desagregaForecast(
                calendario,
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                2,
                demandPlanForecastProjectionAgregado,
                new ClusterEParametrosProjectionComDfuInativa(
                        "MATERIAL_INACTIVE",
                        "LOCATION_INACTIVE"));

        Assertions.assertEquals(
                100.0d,
                demandPlanForecastProjectionMaterialLocationAtiva.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                0.0d,
                demandPlanForecastProjectionMaterialLocationInativa.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);

    }

    @Test
    void historicalSalesDisaggregationShouldSplitUniformlyWhenActiveHistoryIsZero() {

        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
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
        DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado =
                new DemandPlanForecastProjectionAgregado(
                        calendario,
                        unidadeMedida,
                        false);

        /*
         * Quando nenhuma DFU ativa tem historico na janela de split, o
         * Historical Sales Community preserva o total agregado distribuindo em
         * partes iguais entre as DFUs ativas.
         */
        demandPlanForecastProjectionAgregado.forecastBaseline[calendario.getPosicaoPeriodoPresente()] = 90.0d;
        demandPlanForecastProjectionAgregado.forecastBaseline[calendario.getPosicaoPeriodoPresente() + 1] = 30.0d;
        demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionDesagregados()
                .add(demandPlanForecastProjectionMaterialLocationA);
        demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionDesagregados()
                .add(demandPlanForecastProjectionMaterialLocationB);

        DemandPlanning.desagregaForecast(
                calendario,
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                2,
                demandPlanForecastProjectionAgregado,
                new ClusterEParametrosProjectionComDfusAtivas());

        Assertions.assertEquals(
                45.0d,
                demandPlanForecastProjectionMaterialLocationA.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                45.0d,
                demandPlanForecastProjectionMaterialLocationB.forecastBaseline[calendario.getPosicaoPeriodoPresente()],
                0.0001d);
        Assertions.assertEquals(
                15.0d,
                demandPlanForecastProjectionMaterialLocationA.forecastBaseline[calendario.getPosicaoPeriodoPresente() + 1],
                0.0001d);
        Assertions.assertEquals(
                15.0d,
                demandPlanForecastProjectionMaterialLocationB.forecastBaseline[calendario.getPosicaoPeriodoPresente() + 1],
                0.0001d);

    }

    private static ParametrosForecastProjection getParametrosForecastProjection(
            Constantes.DPModeloEstatistico dpModeloEstatistico) {

        return new ParametrosForecastProjection(
                dpModeloEstatistico,
                new ParametrosMediaMovel(2),
                null,
                null,
                null,
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                1);

    }

    private static DemandPlanForecastProjectionMaterialLocation getDemandPlanForecastProjectionMaterialLocationComHistorico(
            Calendario calendario) {

        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation(
                        calendario,
                        new UnidadeMedida("UN"),
                        new Location("LOCATION"),
                        new Produto("MATERIAL"),
                        false);
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[0] = 10.0d;
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[1] = 20.0d;
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers[2] = 40.0d;
        return demandPlanForecastProjectionMaterialLocation;

    }

    private static class ClusterEParametrosProjectionComDfusAtivas extends ClusterEParametrosProjection {

        @Override
        public boolean isDfuAtiva(Produto material, Location location) {

            return true;

        }

    }

    private static class ClusterEParametrosProjectionComDfuInativa extends ClusterEParametrosProjection {

        private final String materialInativoId;
        private final String locationInativaId;

        private ClusterEParametrosProjectionComDfuInativa(
                String materialInativoId,
                String locationInativaId) {

            this.materialInativoId = materialInativoId;
            this.locationInativaId = locationInativaId;

        }

        @Override
        public boolean isDfuAtiva(
                Produto material,
                Location location) {

            return !material.getId().equals(materialInativoId)
                    || !location.getId().equals(locationInativaId);

        }

    }

    private static class LocationProjectionComSetNulo extends LocationProjection {

        @Override
        public Set<Location> getLocationSet() {

            return null;

        }

    }

    private static class MaterialProjectionComSetNulo extends MaterialProjection {

        @Override
        public Set<Produto> getMaterialSet() {

            return null;

        }

    }

    private static void assertRequiresEnterpriseVersionException(Constantes.DPModeloSplit dpModeloSplit) {

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> DemandPlanning.desagregaForecast(
                        (Calendario) null,
                        dpModeloSplit,
                        0,
                        null,
                        null));
        Assertions.assertTrue(
                requiresEnterpriseVersionException.getMessage().contains("Demand Planning Split Model " + dpModeloSplit));

    }

    private static void assertRequiresEnterpriseVersionException(
            Constantes.DPModeloEstatistico dpModeloEstatistico) {

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> DemandPlanning.geraForecastAgregadoNoDemandPlanForecastProjection(
                        null,
                        getParametrosForecastProjection(dpModeloEstatistico),
                        null));
        Assertions.assertTrue(
                requiresEnterpriseVersionException.getMessage().contains("Demand Planning Forecast Model " + dpModeloEstatistico));

    }

    private static double invokeGetQuantidadeTotalCommunityDemandPlanItem(
            DemandPlanItem demandPlanItem,
            Constantes.TipoPlano tipoPlano) throws Exception {

        Method method = DemandPlanning.class.getDeclaredMethod(
                "getQuantidadeTotalCommunityDemandPlanItem",
                DemandPlanItem.class,
                Constantes.TipoPlano.class);
        method.setAccessible(true);
        return (double) method.invoke(
                null,
                demandPlanItem,
                tipoPlano);

    }

    private static Path resolveCommunityRoutinesModuleDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();
        while (currentDirectory != null
                && !"opsfactor-community".equals(currentDirectory.getFileName().toString())) {
            currentDirectory = currentDirectory.getParent();
        }
        if (currentDirectory == null) {
            throw new IllegalStateException("Could not resolve opsfactor-community workspace directory.");
        }
        return currentDirectory;

    }

    private static List<Path> findJavaSources(Path sourceDirectory) throws IOException {

        if (!Files.exists(sourceDirectory)) {
            return List.of();
        }

        try (Stream<Path> pathStream = Files.walk(sourceDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
        }

    }

    private static boolean isPrivateInstanceField(String line) {

        String trimmedLine = line.trim();
        return trimmedLine.startsWith("private ")
                && trimmedLine.endsWith(";")
                && !trimmedLine.contains(" static ");

    }

    private static String formatViolation(Path moduleDirectory, Path violationPath, int lineIndex, String line) {

        return moduleDirectory.relativize(violationPath) + ":" + (lineIndex + 1) + ": " + line.trim();

    }

}
