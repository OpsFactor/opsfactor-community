package com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosLimpezaHistoricoForecast;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testes do processor Community de limpeza historica final do forecast.
 */
class DemandForecastHistoryCleaningProcessorTest {

    @Test
    void historyCleaningShouldCopyStockoutSeriesIntoOutlierSeries() {

        DemandForecastHistoryCleaningProcessor demandForecastHistoryCleaningProcessor =
                new DemandForecastHistoryCleaningProcessor();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation();
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts =
                new double[]{11.0d, 22.0d, 33.0d, 44.0d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers =
                new double[3];

        demandForecastHistoryCleaningProcessor.processa(demandPlanForecastProjectionMaterialLocation);

        Assertions.assertArrayEquals(
                new double[]{11.0d, 22.0d, 33.0d},
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers,
                0.0001d);

    }

    @Test
    void historyCleaningShouldRejectEnterprisePercentileCleaningConfiguration() {

        DemandForecastHistoryCleaningProcessor demandForecastHistoryCleaningProcessor =
                new DemandForecastHistoryCleaningProcessor();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation();
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts =
                new double[]{11.0d, 22.0d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoOutliers =
                new double[2];

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandForecastHistoryCleaningProcessor.processa(
                        demandPlanForecastProjectionMaterialLocation,
                        criaParametrosForecastProjection(Constantes.DPModeloNormalizacao.PERCENTIS)));

        Assertions.assertTrue(requiresEnterpriseVersionException.getMessage().contains("history cleaning"));

    }

    private ParametrosForecastProjection criaParametrosForecastProjection(
            Constantes.DPModeloNormalizacao modeloNormalizacao) {

        return new ParametrosForecastProjection(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new ParametrosLimpezaHistoricoForecast(
                        false,
                        modeloNormalizacao,
                        30,
                        true,
                        0.95d,
                        true,
                        0.05d,
                        null),
                null,
                30);

    }

}
