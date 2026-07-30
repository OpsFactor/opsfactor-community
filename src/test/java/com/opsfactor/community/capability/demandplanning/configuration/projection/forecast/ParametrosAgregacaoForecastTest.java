package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Congela o contrato da decisao top-down/bottom-up usada pelo workflow de
 * forecast.
 *
 * <p>Configuracoes antigas ou incompletas devem cair no modo conservador
 * `TOP_DOWN`, porque ele exige projection agregada e split explicito. Tratar
 * nulos como bottom-up permitiria executar forecast material/location sem que a
 * configuracao tivesse declarado esse comportamento.</p>
 */
class ParametrosAgregacaoForecastTest {

    @Test
    void constructorShouldNormalizeMissingAggregationTypesToTopDown() {

        ParametrosAgregacaoForecast parametrosAgregacaoForecast =
                new ParametrosAgregacaoForecast(null, null);

        Assertions.assertEquals(
                Constantes.DPNivelAgregacao.TOP_DOWN,
                parametrosAgregacaoForecast.getLocationAggregationType());
        Assertions.assertEquals(
                Constantes.DPNivelAgregacao.TOP_DOWN,
                parametrosAgregacaoForecast.getMaterialAggregationType());
        Assertions.assertTrue(parametrosAgregacaoForecast.isQualquerDimensaoTopDown());

    }

    @Test
    void settersShouldKeepNullAsTopDown() {

        ParametrosAgregacaoForecast parametrosAgregacaoForecast =
                new ParametrosAgregacaoForecast(
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.BOTTOM_UP);

        parametrosAgregacaoForecast.setLocationAggregationType(null);
        parametrosAgregacaoForecast.setMaterialAggregationType(null);

        Assertions.assertEquals(
                Constantes.DPNivelAgregacao.TOP_DOWN,
                parametrosAgregacaoForecast.getLocationAggregationType());
        Assertions.assertEquals(
                Constantes.DPNivelAgregacao.TOP_DOWN,
                parametrosAgregacaoForecast.getMaterialAggregationType());
        Assertions.assertTrue(parametrosAgregacaoForecast.isQualquerDimensaoTopDown());

    }

    @Test
    void shouldOnlySkipDisaggregationWhenBothDimensionsAreBottomUp() {

        ParametrosAgregacaoForecast parametrosAgregacaoForecast =
                new ParametrosAgregacaoForecast(
                        Constantes.DPNivelAgregacao.BOTTOM_UP,
                        Constantes.DPNivelAgregacao.BOTTOM_UP);

        Assertions.assertFalse(parametrosAgregacaoForecast.isQualquerDimensaoTopDown());

        parametrosAgregacaoForecast.setMaterialAggregationType(Constantes.DPNivelAgregacao.TOP_DOWN);

        Assertions.assertTrue(parametrosAgregacaoForecast.isQualquerDimensaoTopDown());

    }

    @Test
    void constructorFromClusterParametersShouldRejectMissingSourceObject() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ParametrosAgregacaoForecast((ParametrosDemandPlanNivelCluster) null));

        Assertions.assertEquals(
                "ParametrosDemandPlanNivelCluster e obrigatorio para derivar ParametrosAgregacaoForecast",
                illegalArgumentException.getMessage());

    }

}
