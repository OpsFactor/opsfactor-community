package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos do value object transicional de limpeza historica de forecast.
 */
class ParametrosLimpezaHistoricoForecastTest {

    @Test
    void constructorShouldUseNeutralDefaultsWhenEntityFieldsAreNull() {

        ParametrosLimpezaHistoricoForecast parametrosLimpezaHistoricoForecast =
                new ParametrosLimpezaHistoricoForecast(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new ParametrosGlobais());

        Assertions.assertFalse(parametrosLimpezaHistoricoForecast.isConsideraDadosEstoque());
        Assertions.assertEquals(
                Constantes.DPModeloNormalizacao.DESATIVADO,
                parametrosLimpezaHistoricoForecast.getModeloNormalizacao());
        Assertions.assertEquals(
                Constantes.DP_PADRAO_DIAS_NORMALIZACAO,
                parametrosLimpezaHistoricoForecast.getDiasHistoricosNormalizacao());
        Assertions.assertTrue(
                parametrosLimpezaHistoricoForecast.isHabilitaLimpezaHistoricoPercentilSuperior());
        Assertions.assertTrue(
                parametrosLimpezaHistoricoForecast.isHabilitaLimpezaHistoricoPercentilInferior());

    }

    @Test
    void entityConstructorShouldKeepPersistedPercentileParameters() {

        ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract =
                new ParametrosModeloEstatisticoAbstract(
                        Constantes.DPModeloEstatistico.MM,
                        null,
                        null,
                        null,
                        null,
                        Constantes.DPModeloSplit.HISTORICAL_SALES,
                        null,
                        Constantes.DPModeloUplift.DESATIVADO,
                        false,
                        Constantes.DPModeloNormalizacao.PERCENTIS,
                        180,
                        true,
                        0.80d,
                        true,
                        0.20d,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null) {
                };

        ParametrosLimpezaHistoricoForecast parametrosLimpezaHistoricoForecast =
                new ParametrosLimpezaHistoricoForecast(
                        parametrosModeloEstatisticoAbstract,
                        new ParametrosGlobais());

        Assertions.assertEquals(
                Constantes.DPModeloNormalizacao.PERCENTIS,
                parametrosLimpezaHistoricoForecast.getModeloNormalizacao());
        Assertions.assertEquals(
                180,
                parametrosLimpezaHistoricoForecast.getDiasHistoricosNormalizacao());
        Assertions.assertEquals(
                0.80d,
                parametrosLimpezaHistoricoForecast.getPercentilSuperiorLimpezaHistorico(),
                0.000001d);
        Assertions.assertEquals(
                0.20d,
                parametrosLimpezaHistoricoForecast.getPercentilInferiorLimpezaHistorico(),
                0.000001d);

    }

}
