package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contratos Community da projection operacional de politica de estoques.
 */
class PoliticaEstoquesProjectionCommunityContractTest {

    @Test
    void getSNPFrequenciaReabastecimentoDiasShouldRemainNeutralEvenWithLegacyData() {

        Produto material = new Produto("MAT_01");
        Location location = new Location("LOC_01");
        PoliticaEstoquesProjectionTestavel politicaEstoquesProjectionTestavel =
                new PoliticaEstoquesProjectionTestavel();
        politicaEstoquesProjectionTestavel.adicionaPoliticaLegadaComFrequencia(
                0,
                material,
                location,
                7.0d);

        /*
         * A frequencia de reabastecimento e parametro de otimizacao de politica
         * de estoques. O Community pode manter a coluna por compatibilidade de
         * schema, mas a projection operacional precisa neutralizar bases
         * legadas para nao alterar o safety stock heuristico.
         */
        Assertions.assertEquals(
                0.0d,
                politicaEstoquesProjectionTestavel.getSNPFrequenciaReabastecimentoDias(
                        0,
                        material,
                        location));

    }

    private static class PoliticaEstoquesProjectionTestavel extends PoliticaEstoquesProjection {

        private void adicionaPoliticaLegadaComFrequencia(
                int posicaoPeriodo,
                Produto material,
                Location location,
                Double frequenciaReabastecimentoDias) {

            PoliticaEstoques politicaEstoques = new PoliticaEstoques();
            politicaEstoques.setId("INV_POLICY_01");

            PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation =
                    new PoliticaEstoquesMaterialLocation(
                            new PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey(
                                    politicaEstoques,
                                    material,
                                    location));
            politicaEstoquesMaterialLocation.setFrequenciaReabastecimentoDias(frequenciaReabastecimentoDias);

            mapaPoliticaEstoquesVigenteParaPeridoLocationMaterial =
                    new ConcurrentHashMap<>(
                            Map.of(
                                    posicaoPeriodo,
                                    Map.of(
                                            location,
                                            Map.of(
                                                    material,
                                                    politicaEstoquesMaterialLocation))));

        }

    }

}
