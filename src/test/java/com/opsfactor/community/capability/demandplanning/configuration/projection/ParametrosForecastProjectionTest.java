package com.opsfactor.community.capability.demandplanning.configuration.projection;

import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosMediaMovel;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos de copia dos parametros de forecast compartilhados.
 */
class ParametrosForecastProjectionTest {

    @Test
    void constructorShouldUseDefaultWhenTopDownSplitWindowIsNull() {

        ParametrosForecastProjection parametrosForecastProjection =
                new ParametrosForecastProjection(
                        Constantes.DPModeloEstatistico.MM,
                        new ParametrosMediaMovel(30),
                        null,
                        null,
                        null,
                        Constantes.DPModeloSplit.HISTORICAL_SALES,
                        null);

        Assertions.assertEquals(
                Constantes.DP_PADRAO_DIAS_HISTORICOS_TOP_DOWN,
                parametrosForecastProjection.getNumeroDiasSplitTopDown());

    }

    @Test
    void constructorShouldRejectNonPositiveTopDownSplitWindow() {

        IllegalArgumentException illegalArgumentException =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new ParametrosForecastProjection(
                                Constantes.DPModeloEstatistico.MM,
                                new ParametrosMediaMovel(30),
                                null,
                                null,
                                null,
                                Constantes.DPModeloSplit.HISTORICAL_SALES,
                                0));

        Assertions.assertEquals(
                "Historical Sales split reference window must be positive.",
                illegalArgumentException.getMessage());

    }

    @Test
    void copyConstructorShouldPreserveEnterpriseMapeAggregationLevelIds() {

        ParametrosForecastProjection parametrosForecastProjection =
                new ParametrosForecastProjection(
                        Constantes.DPModeloEstatistico.MM,
                        new ParametrosMediaMovel(30),
                        null,
                        null,
                        null,
                        Constantes.DPModeloSplit.HISTORICAL_SALES,
                        30);
        parametrosForecastProjection.setNivelAgregacaoMaterialMapeId(
                "MAPE_MATERIAL");
        parametrosForecastProjection.setNivelAgregacaoLocationMapeId(
                "MAPE_LOCATION");

        ParametrosForecastProjection parametrosForecastProjectionCopiados =
                new ParametrosForecastProjection(
                        parametrosForecastProjection,
                        null);

        Assertions.assertEquals(
                "MAPE_MATERIAL",
                parametrosForecastProjectionCopiados.getNivelAgregacaoMaterialMapeId());
        Assertions.assertEquals(
                "MAPE_LOCATION",
                parametrosForecastProjectionCopiados.getNivelAgregacaoLocationMapeId());

    }

}
