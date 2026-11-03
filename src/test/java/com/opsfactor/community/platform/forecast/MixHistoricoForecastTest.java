package com.opsfactor.community.platform.forecast;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MixHistoricoForecastTest {

    @Test
    void commonResponseCancelsAndPreservesAuthoritativeTotal() {

        var mix = MixHistoricoForecast.calcular(new double[]{60, 40}, new double[]{80, 20}, new double[]{160, 40});
        assertArrayEquals(new double[]{120, 80}, MixHistoricoForecast.distribuir(200, mix), 1e-10);
        assertEquals("RECENT_HISTORY", mix.regra());

    }

    @Test
    void preservesDifferentRegionalResponsesRatherThanReplacingThemWithFlatShares() {

        var mix = MixHistoricoForecast.calcular(new double[]{50, 50}, new double[]{10, 10}, new double[]{20, 10});
        assertArrayEquals(new double[]{20, 10}, MixHistoricoForecast.distribuir(30, mix), 1e-10);

    }

    @Test
    void noRecentSalesUsesExplicitTrainingHistoryAndNoMassCannotDistributePositiveForecast() {

        var mix = MixHistoricoForecast.calcular(new double[]{0, 0}, new double[]{30, 10}, null);
        assertEquals("FULL_TRAINING_HISTORY", mix.regra());
        assertArrayEquals(new double[]{60, 20}, MixHistoricoForecast.distribuir(80, mix), 1e-10);
        var empty = MixHistoricoForecast.calcular(new double[]{0, 0}, new double[]{0, 0}, null);
        assertArrayEquals(new double[]{0, 0}, MixHistoricoForecast.distribuir(0, empty));
        assertThrows(IllegalStateException.class, () -> MixHistoricoForecast.distribuir(1, empty));
        assertThrows(IllegalArgumentException.class, () -> MixHistoricoForecast.calcular(new double[]{Double.NaN}, new double[]{1}, null));

    }
}
