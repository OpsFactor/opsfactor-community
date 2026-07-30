package com.opsfactor.community.capability.demandplanning.configuration.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos Community dos parametros gerais materializados para a rodada de
 * Demand Planning.
 */
class ParametrosGeraisDemandPlanningProjectionTest {

    @Test
    void constructorShouldUseGlobalDefaultWhenHistoricalForecastWindowIsNull() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setDiasHistoricosForecastEstatistico(540);

        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                new ParametrosGeraisDemandPlanningProjection(
                        true,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.TOP_DOWN,
                                Constantes.DPNivelAgregacao.TOP_DOWN),
                        null,
                        true,
                        true,
                        0,
                        new UnidadeMedida("UN"),
                        false,
                        parametrosGlobais);

        Assertions.assertEquals(
                540,
                parametrosGeraisDemandPlanningProjection.getDiasHistoricosForecastEstatistico());

    }

    @Test
    void constructorShouldRejectNonPositiveHistoricalForecastWindow() {

        IllegalArgumentException illegalArgumentException =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new ParametrosGeraisDemandPlanningProjection(
                                true,
                                new ParametrosAgregacaoForecast(
                                        Constantes.DPNivelAgregacao.TOP_DOWN,
                                        Constantes.DPNivelAgregacao.TOP_DOWN),
                                0,
                                true,
                                true,
                                0,
                                new UnidadeMedida("UN"),
                                false,
                                new ParametrosGlobais()));

        Assertions.assertEquals(
                "Dias historicos de forecast estatistico devem ser positivos.",
                illegalArgumentException.getMessage());

    }

    @Test
    void constructorShouldNeutralizeNewMaterialWindowEvenWhenRawValueIsPositive() {

        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                new ParametrosGeraisDemandPlanningProjection(
                        true,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.TOP_DOWN,
                                Constantes.DPNivelAgregacao.TOP_DOWN),
                        365,
                        true,
                        true,
                        45,
                        new UnidadeMedida("UN"),
                        false,
                        new ParametrosGlobais());

        /*
         * New Materials e uma capacidade Enterprise. Mesmo que banco legado ou
         * chamada direta ainda tragam janela positiva, a projection Community
         * precisa entregar 0 para impedir tratamento especial durante a rodada.
         */
        Assertions.assertEquals(
                0,
                parametrosGeraisDemandPlanningProjection.getNumeroDiasProdutoNovo());

    }

}
