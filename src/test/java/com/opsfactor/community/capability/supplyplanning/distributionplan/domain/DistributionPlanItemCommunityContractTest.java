package com.opsfactor.community.capability.supplyplanning.distributionplan.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contrato Community das linhas de plano de distribuicao.
 *
 * <p>Distribuicao continua fazendo parte do Supply Planning heuristico aberto:
 * compras e transferencias materializam o abastecimento entre locations. As
 * quantidades persistidas precisam manter `null` como ausencia operacional
 * zero, mas valores presentes negativos ou nao finitos indicam snapshot
 * corrompido e devem falhar antes de alimentar estoque em transito,
 * capacidade logistica ou Planning Book.</p>
 */
class DistributionPlanItemCommunityContractTest {

    @Test
    void distributionQuantitiesShouldTreatNullAsOperationalZero() {

        DistributionPlanItem distributionPlanItem =
                criaDistributionPlanItem();

        Assertions.assertEquals(
                0.0d,
                distributionPlanItem.getQuantidadeOrdemPlanejadaIrrestrita());
        Assertions.assertEquals(
                0.0d,
                distributionPlanItem.getParcelaOrdemFirmeRestritaAtendimentoDemandaDireta());

    }

    @Test
    void distributionQuantitiesShouldRejectNegativeRegisteredValues() {

        DistributionPlanItem distributionPlanItem =
                criaDistributionPlanItem();
        distributionPlanItem.setQuantidadeOrdemPlanejadaIrrestrita(-1.0d);

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        distributionPlanItem::getQuantidadeOrdemPlanejadaIrrestrita);

        Assertions.assertEquals(
                "Distribution plan quantity unrestricted planned distribution must be finite and non-negative for "
                        + "material FG_LINE / origin ORIGIN / destination DESTINATION / shipping date 2026-01-01T00:00 "
                        + "/ receipt date 2026-01-03T00:00: -1.0.",
                illegalStateException.getMessage());

    }

    @Test
    void distributionDirectDemandParcelsShouldRejectNonFiniteRegisteredValues() {

        DistributionPlanItem distributionPlanItem =
                criaDistributionPlanItem();
        distributionPlanItem.setParcelaOrdemFirmeRestritaAtendimentoDemandaDireta(Double.NaN);

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        distributionPlanItem::getParcelaOrdemFirmeRestritaAtendimentoDemandaDireta);

        Assertions.assertEquals(
                "Distribution plan quantity restricted firm direct demand parcel must be finite and non-negative for "
                        + "material FG_LINE / origin ORIGIN / destination DESTINATION / shipping date 2026-01-01T00:00 "
                        + "/ receipt date 2026-01-03T00:00: NaN.",
                illegalStateException.getMessage());

    }

    private static DistributionPlanItem criaDistributionPlanItem() {

        SupplyPlan supplyPlan = new SupplyPlan();
        Produto material = new Produto("FG_LINE");
        Location locationOrigem = new Location("ORIGIN");
        Location locationDestino = new Location("DESTINATION");

        DistributionPlanItem.DistributionPlanItemKey key =
                new DistributionPlanItem.DistributionPlanItemKey(
                        supplyPlan,
                        locationDestino,
                        locationOrigem,
                        material,
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 1, 3, 0, 0));

        return new DistributionPlanItem(key);

    }

}
