package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

class ParametrosChronosTest {

    @Test
    void defaultConstructorShouldUseTechnicalDefaultsWithoutOpeningCommunityExecution() {

        ParametrosChronos parametrosChronos = new ParametrosChronos();

        Assertions.assertEquals("chronos-default-sentinel", parametrosChronos.getModelId());
        Assertions.assertFalse(parametrosChronos.getModelId().contains("enterprise"));
        Assertions.assertEquals("cpu", parametrosChronos.getDeviceMap());
        Assertions.assertEquals(List.of(0.1D, 0.5D, 0.9D), parametrosChronos.getQuantileLevels());
        Assertions.assertEquals(Duration.ofMinutes(30), parametrosChronos.getTimeout());
        Assertions.assertFalse(parametrosChronos.isForceAggregatedForecast());

    }

    @Test
    void entityConstructorShouldReadOnlyPersistedReconciliationFlag() {

        ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract =
                new ParametrosModeloEstatisticoAbstract(
                        Constantes.DPModeloEstatistico.CHRONOS,
                        null,
                        null,
                        null,
                        null,
                        Constantes.DPModeloSplit.HISTORICAL_SALES,
                        null,
                        Constantes.DPModeloUplift.DESATIVADO,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true) {
                };

        ParametrosChronos parametrosChronos =
                new ParametrosChronos(parametrosModeloEstatisticoAbstract);

        Assertions.assertTrue(parametrosChronos.isForceAggregatedForecast());
        Assertions.assertEquals("chronos-default-sentinel", parametrosChronos.getModelId());

    }

    @Test
    void constructorShouldDefensivelyCopyQuantileLevels() {

        List<Double> quantileLevels = new ArrayList<>(List.of(0.2D, 0.5D, 0.8D));

        ParametrosChronos parametrosChronos =
                new ParametrosChronos(
                        "modelo",
                        "cuda",
                        quantileLevels,
                        Duration.ofMinutes(5),
                        true);

        quantileLevels.add(0.95D);

        Assertions.assertEquals(List.of(0.2D, 0.5D, 0.8D), parametrosChronos.getQuantileLevels());
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> parametrosChronos.getQuantileLevels().add(0.95D));

    }

    @Test
    void constructorShouldRejectInvalidQuantilesAndTimeout() {

        IllegalArgumentException quantileLevelException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ParametrosChronos(
                        "modelo",
                        "cpu",
                        List.of(0.2D, Double.NaN, 0.8D),
                        Duration.ofMinutes(5),
                        false));
        IllegalArgumentException timeoutException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ParametrosChronos(
                        "modelo",
                        "cpu",
                        List.of(0.2D, 0.5D, 0.8D),
                        Duration.ZERO,
                        false));

        Assertions.assertEquals(
                "Quantil Chronos invalido: NaN",
                quantileLevelException.getMessage());
        Assertions.assertEquals(
                "Timeout Chronos deve ser positivo.",
                timeoutException.getMessage());

    }

}
