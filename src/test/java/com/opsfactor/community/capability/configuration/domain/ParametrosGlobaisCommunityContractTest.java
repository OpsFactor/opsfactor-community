package com.opsfactor.community.capability.configuration.domain;

import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato de dominio dos parametros globais compartilhados pelo Community.
 *
 * <p>Services e policies bloqueiam payloads novos invalidos, mas a entidade
 * tambem precisa proteger consumidores internos que recebem snapshots antigos
 * ou dados carregados diretamente pelo repository. Defaults nulos continuam
 * validos; valores explicitamente cadastrados precisam preservar semantica
 * funcional.</p>
 */
class ParametrosGlobaisCommunityContractTest {

    @Test
    void forecastHorizonShouldDefaultWhenUnset() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();

        Assertions.assertEquals(
                Constantes.DP_PADRAO_DIAS_HORIZONTE_FORECAST,
                parametrosGlobais.getHorizonteForecastDias());

    }

    @Test
    void forecastHorizonShouldUsePositiveConfiguredValue() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setHorizonteForecastDias(84);

        Assertions.assertEquals(
                84,
                parametrosGlobais.getHorizonteForecastDias());

    }

    @Test
    void forecastHorizonShouldRejectNonPositiveConfiguredValue() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setHorizonteForecastDias(0);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                parametrosGlobais::getHorizonteForecastDias);

        Assertions.assertEquals(
                "Global forecast horizon in days must be positive when explicitly configured: 0.",
                illegalStateException.getMessage());

    }

    @Test
    void locationSafetyStockIndirectDemandShouldInheritGlobalOnlyWhenOverrideIsNull() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        Location location = new Location("LOC-SAFETY-STOCK");
        parametrosGlobais.setIncluiDemandaIndiretaNoSafetyStock(true);

        /*
         * Null nao significa false: preserva o default global, inclusive
         * quando a API de Global Parameters passa a editar esse default.
         */
        location.setIncluiDemandaIndiretaNoSafetyStock(null);
        Assertions.assertTrue(location.getIncluiDemandaIndiretaNoSafetyStock(parametrosGlobais));

        /*
         * False e override local explicito e, portanto, deve prevalecer sobre
         * o global true. Isso impede que a nova superficie global apague a
         * semantica por Location ja persistida.
         */
        location.setIncluiDemandaIndiretaNoSafetyStock(false);
        Assertions.assertFalse(location.getIncluiDemandaIndiretaNoSafetyStock(parametrosGlobais));

        parametrosGlobais.setIncluiDemandaIndiretaNoSafetyStock(false);
        location.setIncluiDemandaIndiretaNoSafetyStock(null);
        Assertions.assertFalse(location.getIncluiDemandaIndiretaNoSafetyStock(parametrosGlobais));

    }

}
