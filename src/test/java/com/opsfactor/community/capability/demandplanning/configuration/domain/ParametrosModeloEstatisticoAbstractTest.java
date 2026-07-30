package com.opsfactor.community.capability.demandplanning.configuration.domain;

import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParametrosModeloEstatisticoAbstractTest {

    @Test
    public void getDescricaoModeloEstatisticoShouldFormatMovingAverageDescriptions() {

        ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoMovingAverage =
                criaParametrosModeloEstatisticoAbstract(Constantes.DPModeloEstatistico.MM, 45);
        ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoRollingMovingAverage =
                criaParametrosModeloEstatisticoAbstract(Constantes.DPModeloEstatistico.RMM, 60);

        Assertions.assertEquals(
                "Moving Average (# days=45)",
                parametrosModeloEstatisticoMovingAverage.getDescricaoModeloEstatistico());
        Assertions.assertEquals(
                "Rolling Moving Average (# days=60)",
                parametrosModeloEstatisticoRollingMovingAverage.getDescricaoModeloEstatistico());

    }

    private static ParametrosModeloEstatisticoAbstract criaParametrosModeloEstatisticoAbstract(
            Constantes.DPModeloEstatistico dpModeloEstatistico,
            Integer diasMediaMovelDp) {

        return new ParametrosModeloEstatisticoAbstract(
                dpModeloEstatistico,
                diasMediaMovelDp,
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
                null) {
        };

    }

}
