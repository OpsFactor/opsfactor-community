package com.opsfactor.community.capability.supplyplanning.supplyplan.domain;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha.TipoDemandaDireta;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato das mensagens de fail-fast das linhas de Supply Planning.
 *
 * <p>Os enums de plano e firme/planejado sao compartilhados por varias telas e
 * DTOs, portanto payloads antigos ou Enterprise podem chegar ate objetos
 * Community durante testes e migracoes. As entidades devem falhar antes de
 * mascarar esses valores como zero ou como "nao implementado" generico.</p>
 */
class SupplyPlanningDataContractTest {

    @Test
    void productionPlanLinhaShouldExplainUnsupportedFirmDeliveryBucket() {

        ProductionPlanLinha productionPlanLinha = new ProductionPlanLinha();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> productionPlanLinha.getQuantidade(
                        Constantes.TipoPlano.PLANO_IRRESTRITO,
                        Constantes.FirmePlanejado.REMESSA));

        assertMessageContains(
                illegalArgumentException,
                "ProductionPlanLinha.getQuantidade",
                "supports firm/planned buckets [ORDEM, PLANEJADO, TOTAL]",
                "received REMESSA",
                "Firm deliveries/remessas are transactional Enterprise data");

    }

    @Test
    void inventoryPlanLinhaShouldExplainUnsupportedHistoricalPlanVariant() {

        InventoryPlanLinha inventoryPlanLinha = new InventoryPlanLinha();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> inventoryPlanLinha.getQuantidadeEstoqueProjetado(Constantes.TipoPlano.HISTORICO));

        assertMessageContains(
                illegalArgumentException,
                "InventoryPlanLinha.getQuantidadeEstoqueProjetado",
                "supports plan variants [PLANO_IRRESTRITO, PLANO_RESTRITO, PLANO_TRABALHO]",
                "received HISTORICO",
                "Budget, historical and unmet-demand variants are not stored");

    }

    @Test
    void distributionPlanItemShouldExplainUnsupportedFirmDeliveryBucket() {

        DistributionPlanItem distributionPlanItem = new DistributionPlanItem();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> distributionPlanItem.getQuantidade(
                        Constantes.FirmePlanejado.REMESSA,
                        Constantes.TipoPlano.PLANO_IRRESTRITO));

        assertMessageContains(
                illegalArgumentException,
                "DistributionPlanItem.getQuantidade",
                "supports firm/planned buckets [ORDEM, PLANEJADO, TOTAL]",
                "received REMESSA",
                "Firm deliveries/remessas are transactional Enterprise data");

    }

    @Test
    void demandaDiretaConsideradaLinhaShouldExplainUnsupportedHistoricalPlanVariant() {

        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha =
                new DemandaDiretaConsideradaLinha();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaConsideradaSegregada(
                        TipoDemandaDireta.TOTAL,
                        Constantes.TipoPlano.HISTORICO));

        assertMessageContains(
                illegalArgumentException,
                "DemandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaConsideradaSegregada",
                "supports plan variants [PLANO_IRRESTRITO, PLANO_RESTRITO, PLANO_TRABALHO]",
                "received HISTORICO");

    }

    @Test
    void demandaDiretaConsideradaLinhaShouldExplainThatTotalDirectDemandIsReadOnly() {

        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha =
                new DemandaDiretaConsideradaLinha();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaConsideradaSegregada(
                        10.0,
                        TipoDemandaDireta.TOTAL,
                        Constantes.TipoPlano.PLANO_IRRESTRITO));

        assertMessageContains(
                illegalArgumentException,
                "DemandaDiretaConsideradaLinha.setQuantidadeDemandaDiretaConsideradaSegregada",
                "cannot write TOTAL directly",
                "TOTAL is a read aggregation");

    }

    private static void assertMessageContains(
            Throwable throwable,
            String... expectedTextArray) {

        String message = throwable.getMessage();

        for (String expectedText : expectedTextArray) {
            Assertions.assertTrue(
                    message.contains(expectedText),
                    () -> "Expected message to contain [" + expectedText + "], but was [" + message + "]");
        }

    }

}
