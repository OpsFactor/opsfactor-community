package com.opsfactor.community.capability.masterdata.production.productionversion.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contratos Community da sentinela de versao de producao inexistente.
 */
class VersaoProducaoInexistenteCommunityContractTest {

    @Test
    void sentinelShouldFailExplicitlyWhenConsumedAsRealProductionVersion() {

        VersaoProducaoInexistente versaoProducaoInexistente =
                new VersaoProducaoInexistente();

        assertSentinelUseFailsExplicitly(
                () -> versaoProducaoInexistente.getMateriaisOutput(),
                "output materials");
        assertSentinelUseFailsExplicitly(
                () -> versaoProducaoInexistente.getMateriaisInput(),
                "input materials");
        assertSentinelUseFailsExplicitly(
                () -> versaoProducaoInexistente.getRoteiros(),
                "routings");
        assertSentinelUseFailsExplicitly(
                () -> versaoProducaoInexistente.getListasTecnicas(),
                "bills of materials");
        assertSentinelUseFailsExplicitly(
                () -> versaoProducaoInexistente.getDetalhePorVersaoProducao(
                        null,
                        null,
                        null,
                        1.0d),
                "routing/BOM production details");
        assertSentinelUseFailsExplicitly(
                () -> versaoProducaoInexistente.getCombinacoesRoteiroListaTecnica(),
                "routing/BOM combinations");

    }

    private static void assertSentinelUseFailsExplicitly(
            Runnable sentinelUsage,
            String expectedDetail) {

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                sentinelUsage::run);

        Assertions.assertTrue(illegalStateException.getMessage().contains(
                "Production version sentinel "
                        + VersaoProducaoInexistente.ID_VERSAO_PRODUCAO_VAZIA
                        + " does not expose "
                        + expectedDetail));
        Assertions.assertTrue(illegalStateException.getMessage().contains(
                "Resolve a real production version before using production planning maps."));

    }

}
