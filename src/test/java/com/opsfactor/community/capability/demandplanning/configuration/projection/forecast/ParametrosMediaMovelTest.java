package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos dos parametros Community do modelo Moving Average.
 */
class ParametrosMediaMovelTest {

    @Test
    void constructorShouldUseDefaultWhenWindowIsNull() {

        ParametrosMediaMovel parametrosMediaMovel =
                new ParametrosMediaMovel((Integer) null);

        Assertions.assertEquals(
                Constantes.DP_PADRAO_DIAS_MEDIA_MOVEL,
                parametrosMediaMovel.getDiasHistoricosMediaMovel());

    }

    @Test
    void constructorShouldRejectNonPositiveWindow() {

        IllegalArgumentException illegalArgumentException =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new ParametrosMediaMovel(0));

        Assertions.assertEquals(
                "Moving Average historical window must be positive.",
                illegalArgumentException.getMessage());

    }

}
