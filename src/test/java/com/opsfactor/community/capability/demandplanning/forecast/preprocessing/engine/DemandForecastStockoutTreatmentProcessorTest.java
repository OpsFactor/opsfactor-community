package com.opsfactor.community.capability.demandplanning.forecast.preprocessing.engine;

import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosLimpezaHistoricoForecast;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testes do processor Community de tratamento de stockout no workflow de forecast.
 */
class DemandForecastStockoutTreatmentProcessorTest {

    @Test
    void stockoutTreatmentShouldCopyHistoricalDemandIntoStockoutSeries() {

        DemandForecastStockoutTreatmentProcessor demandForecastStockoutTreatmentProcessor =
                new DemandForecastStockoutTreatmentProcessor();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation();
        demandPlanForecastProjectionMaterialLocation.demanda =
                new double[]{10.0d, 20.0d, 30.0d, 40.0d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts =
                new double[3];

        demandForecastStockoutTreatmentProcessor.processa(demandPlanForecastProjectionMaterialLocation);

        Assertions.assertArrayEquals(
                new double[]{10.0d, 20.0d, 30.0d},
                demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts,
                0.0001d);

    }

    @Test
    void stockoutTreatmentShouldRejectEnterpriseStockoutConfiguration() {

        DemandForecastStockoutTreatmentProcessor demandForecastStockoutTreatmentProcessor =
                new DemandForecastStockoutTreatmentProcessor();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation();
        demandPlanForecastProjectionMaterialLocation.demanda =
                new double[]{10.0d, 20.0d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts =
                new double[2];

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandForecastStockoutTreatmentProcessor.processa(
                        demandPlanForecastProjectionMaterialLocation,
                        criaParametrosForecastProjection(true, Constantes.DPModeloNormalizacao.DESATIVADO)));

        Assertions.assertTrue(requiresEnterpriseVersionException.getMessage().contains("stockout"));

    }

    @Test
    void stockoutTreatmentShouldRejectTargetLongerThanHistoricalDemand() {

        DemandForecastStockoutTreatmentProcessor demandForecastStockoutTreatmentProcessor =
                new DemandForecastStockoutTreatmentProcessor();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation();
        demandPlanForecastProjectionMaterialLocation.demanda =
                new double[]{10.0d, 20.0d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts =
                new double[3];

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastStockoutTreatmentProcessor.processa(demandPlanForecastProjectionMaterialLocation));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains("cannot be longer"));

    }

    @Test
    void stockoutTreatmentShouldRejectNonFiniteHistoricalDemandBeforeCopy() {

        DemandForecastStockoutTreatmentProcessor demandForecastStockoutTreatmentProcessor =
                new DemandForecastStockoutTreatmentProcessor();
        DemandPlanForecastProjectionMaterialLocation demandPlanForecastProjectionMaterialLocation =
                new DemandPlanForecastProjectionMaterialLocation();
        demandPlanForecastProjectionMaterialLocation.demanda =
                new double[]{10.0d, Double.NaN, 30.0d};
        demandPlanForecastProjectionMaterialLocation.vendaHistoricaTratamentoStockouts =
                new double[3];

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandForecastStockoutTreatmentProcessor.processa(demandPlanForecastProjectionMaterialLocation));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains("finite values"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("period 1"));

    }

    private ParametrosForecastProjection criaParametrosForecastProjection(
            boolean consideraDadosEstoque,
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
                        consideraDadosEstoque,
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
