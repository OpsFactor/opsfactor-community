package com.opsfactor.community.capability.demandplanning.forecast.configuration;

import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

/**
 * Protege o catalogo Community de modelos estatisticos de Demand Planning.
 */
class DemandPlanningModelCatalogTest {

    @Test
    void getDpModelosEstatisticosCommunityShouldExposeOnlyOpenModels() {

        Set<Constantes.DPModeloEstatistico> dpModelosEstatisticosCommunity =
                DemandPlanningModelCatalog.getDpModelosEstatisticosCommunity();

        Assertions.assertEquals(
                Set.of(
                        Constantes.DPModeloEstatistico.MM,
                        Constantes.DPModeloEstatistico.RMM,
                        Constantes.DPModeloEstatistico.ARIMA,
                        Constantes.DPModeloEstatistico.HOLT_WINTERS,
                        Constantes.DPModeloEstatistico.ES),
                dpModelosEstatisticosCommunity);
        Assertions.assertFalse(
                dpModelosEstatisticosCommunity.contains(Constantes.DPModeloEstatistico.SNAIVE));
        Assertions.assertFalse(
                dpModelosEstatisticosCommunity.contains(Constantes.DPModeloEstatistico.ETS));
        Assertions.assertFalse(
                dpModelosEstatisticosCommunity.contains(Constantes.DPModeloEstatistico.TBATS));
        Assertions.assertFalse(
                dpModelosEstatisticosCommunity.contains(Constantes.DPModeloEstatistico.CHRONOS));

    }

    @Test
    void getDpModelosEstatisticosOpenApiCommunityShouldExposeLabelsInUiOrder() {

        Assertions.assertEquals(
                List.of(
                        "Moving Average",
                        "Rolling Moving Average",
                        "ARIMA",
                        "Holt-Winters",
                        "Exponential Smoothing"),
                DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiCommunity());

    }

    @Test
    void getDpModelosEstatisticosRuntimeOptionsShouldExposeEnterpriseOptionsForBlockedUi() {

        Assertions.assertEquals(
                List.of(
                        "Moving Average",
                        "Rolling Moving Average",
                        "ARIMA",
                        "Holt-Winters",
                        "Exponential Smoothing",
                        "Seasonal Naive",
                        "STL",
                        "Prophet",
                        "ETS",
                        "TBATS",
                        "Budget as Forecast",
                        "Chronos"),
                DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiRuntimeOptions());

    }

    @Test
    void getDpModelosEstatisticosCommunityShouldBeImmutable() {

        Set<Constantes.DPModeloEstatistico> dpModelosEstatisticosCommunity =
                DemandPlanningModelCatalog.getDpModelosEstatisticosCommunity();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> dpModelosEstatisticosCommunity.add(Constantes.DPModeloEstatistico.SNAIVE));

    }

    @Test
    void getDpModelosEstatisticosOpenApiCommunityShouldBeImmutable() {

        List<String> dpModelosEstatisticosOpenApiCommunity =
                DemandPlanningModelCatalog.getDpModelosEstatisticosOpenApiCommunity();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> dpModelosEstatisticosOpenApiCommunity.add("Seasonal Naive"));

    }

    @Test
    void getDpModelosSplitCommunityShouldExposeOnlyHistoricalSales() {

        Set<Constantes.DPModeloSplit> dpModelosSplitCommunity =
                DemandPlanningModelCatalog.getDpModelosSplitCommunity();

        Assertions.assertEquals(
                Set.of(Constantes.DPModeloSplit.HISTORICAL_SALES),
                dpModelosSplitCommunity);
        Assertions.assertTrue(
                DemandPlanningModelCatalog.isDpModeloSplitCommunity(Constantes.DPModeloSplit.HISTORICAL_SALES));
        Assertions.assertFalse(
                DemandPlanningModelCatalog.isDpModeloSplitCommunity(Constantes.DPModeloSplit.HTS));
        Assertions.assertFalse(
                DemandPlanningModelCatalog.isDpModeloSplitCommunity(Constantes.DPModeloSplit.FORECAST_PROPORTION));

    }

    @Test
    void getDpModelosSplitOpenApiCommunityShouldExposeLabelsInUiOrder() {

        Assertions.assertEquals(
                List.of("Historical Sales"),
                DemandPlanningModelCatalog.getDpModelosSplitOpenApiCommunity());

    }

    @Test
    void getDpModelosSplitRuntimeOptionsShouldExposeEnterpriseOptionsForBlockedUi() {

        Assertions.assertEquals(
                List.of(
                        "Historical Sales",
                        "DFU-Level STL Forecast",
                        "Hierarchical Reconciliation"),
                DemandPlanningModelCatalog.getDpModelosSplitOpenApiRuntimeOptions());

    }

    @Test
    void getDpModelosSplitCommunityShouldBeImmutable() {

        Set<Constantes.DPModeloSplit> dpModelosSplitCommunity =
                DemandPlanningModelCatalog.getDpModelosSplitCommunity();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> dpModelosSplitCommunity.add(Constantes.DPModeloSplit.HTS));

    }

    @Test
    void getDpModelosSplitOpenApiCommunityShouldBeImmutable() {

        List<String> dpModelosSplitOpenApiCommunity =
                DemandPlanningModelCatalog.getDpModelosSplitOpenApiCommunity();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> dpModelosSplitOpenApiCommunity.add("Hierarchical Reconciliation"));

    }

    @Test
    void getDpModelosLimpezaHistoricoCommunityShouldExposeOnlyInactive() {

        Set<Constantes.DPModeloNormalizacao> dpModelosLimpezaHistoricoCommunity =
                DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoCommunity();

        Assertions.assertEquals(
                Set.of(Constantes.DPModeloNormalizacao.DESATIVADO),
                dpModelosLimpezaHistoricoCommunity);
        Assertions.assertTrue(
                DemandPlanningModelCatalog.isDpModeloLimpezaHistoricoCommunity(
                        Constantes.DPModeloNormalizacao.DESATIVADO));
        Assertions.assertFalse(
                DemandPlanningModelCatalog.isDpModeloLimpezaHistoricoCommunity(
                        Constantes.DPModeloNormalizacao.PERCENTIS));
        Assertions.assertFalse(
                DemandPlanningModelCatalog.isDpModeloLimpezaHistoricoCommunity(
                        Constantes.DPModeloNormalizacao.CAMPANHA));

    }

    @Test
    void getDpModelosLimpezaHistoricoOpenApiCommunityShouldExposeLabelsInUiOrder() {

        Assertions.assertEquals(
                List.of("Inactive"),
                DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiCommunity());

    }

    @Test
    void getDpModelosTratamentoStockoutAndUpliftCommunityShouldExposeOnlyInactiveOptions() {

        Assertions.assertEquals(
                List.of("Inactive"),
                DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiCommunity());
        Assertions.assertEquals(
                List.of("No Uplift Calculation"),
                DemandPlanningModelCatalog.getDpModelosUpliftOpenApiCommunity());

    }

    @Test
    void getDpModelosTratamentoStockoutAndUpliftRuntimeOptionsShouldExposeEnterpriseOptions() {

        Assertions.assertEquals(
                List.of(
                        "Inactive",
                        "Smoothing of Stockout Periods"),
                DemandPlanningModelCatalog.getDpModelosTratamentoStockoutOpenApiRuntimeOptions());
        Assertions.assertEquals(
                List.of(
                        "No Uplift Calculation",
                        "Event Uplift"),
                DemandPlanningModelCatalog.getDpModelosUpliftOpenApiRuntimeOptions());

    }

    @Test
    void getDpModelosLimpezaHistoricoRuntimeOptionsShouldExposeEnterpriseOptions() {

        Assertions.assertEquals(
                List.of(
                        "Inactive",
                        "Percentile",
                        "Campaign"),
                DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiRuntimeOptions());

    }

    @Test
    void getDpModelosLimpezaHistoricoCommunityShouldBeImmutable() {

        Set<Constantes.DPModeloNormalizacao> dpModelosLimpezaHistoricoCommunity =
                DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoCommunity();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> dpModelosLimpezaHistoricoCommunity.add(Constantes.DPModeloNormalizacao.PERCENTIS));

    }

    @Test
    void getDpModelosLimpezaHistoricoOpenApiCommunityShouldBeImmutable() {

        List<String> dpModelosLimpezaHistoricoOpenApiCommunity =
                DemandPlanningModelCatalog.getDpModelosLimpezaHistoricoOpenApiCommunity();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> dpModelosLimpezaHistoricoOpenApiCommunity.add("Percentile"));

    }

    @Test
    void getTiposDocumentoHistoricoCommunityShouldExposeOnlySellOut() {

        Set<Constantes.TipoDocumentoVenda> tiposDocumentoHistoricoCommunity =
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoCommunity();

        Assertions.assertEquals(
                Set.of(Constantes.TipoDocumentoVenda.SELLOUT),
                tiposDocumentoHistoricoCommunity);
        Assertions.assertTrue(
                DemandPlanningModelCatalog.isTipoDocumentoHistoricoCommunity(
                        Constantes.TipoDocumentoVenda.SELLOUT));
        Assertions.assertFalse(
                DemandPlanningModelCatalog.isTipoDocumentoHistoricoCommunity(
                        Constantes.TipoDocumentoVenda.SELLIN));
        Assertions.assertFalse(
                DemandPlanningModelCatalog.isTipoDocumentoHistoricoCommunity(
                        Constantes.TipoDocumentoVenda.PEDIDO));

    }

    @Test
    void getTiposDocumentoHistoricoOpenApiCommunityShouldExposeLabelsInUiOrder() {

        Assertions.assertEquals(
                List.of("Sell-out"),
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiCommunity());

    }

    @Test
    void getTiposDocumentoHistoricoRuntimeOptionsShouldExposeEnterpriseOptionsForBlockedUi() {

        Assertions.assertEquals(
                List.of(
                        "Sell-out",
                        "Sell-in",
                        "Sales Orders"),
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiRuntimeOptions());

    }

    @Test
    void getTiposDocumentoHistoricoCanonicalOpenApiCommunityShouldExposeEnumNamesInPayloadOrder() {

        Assertions.assertEquals(
                List.of("SELLOUT"),
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoCanonicalOpenApiCommunity());

    }

    @Test
    void getTiposDocumentoHistoricoCommunityShouldBeImmutable() {

        Set<Constantes.TipoDocumentoVenda> tiposDocumentoHistoricoCommunity =
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoCommunity();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> tiposDocumentoHistoricoCommunity.add(Constantes.TipoDocumentoVenda.SELLIN));

    }

    @Test
    void getTiposDocumentoHistoricoOpenApiCommunityShouldBeImmutable() {

        List<String> tiposDocumentoHistoricoOpenApiCommunity =
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoOpenApiCommunity();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> tiposDocumentoHistoricoOpenApiCommunity.add("Sell-in"));

    }

    @Test
    void getTiposDocumentoHistoricoCanonicalOpenApiCommunityShouldBeImmutable() {

        List<String> tiposDocumentoHistoricoCanonicalOpenApiCommunity =
                DemandPlanningModelCatalog.getTiposDocumentoHistoricoCanonicalOpenApiCommunity();

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> tiposDocumentoHistoricoCanonicalOpenApiCommunity.add("SELLIN"));

    }

}
