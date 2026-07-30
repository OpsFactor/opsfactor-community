package com.opsfactor.community.capability.demandplanning.demandplan.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contrato Community da entidade de historico persistido do Demand Plan.
 *
 * <p>As colunas fisicas continuam usando a nomenclatura transicional
 * `quantidadeBase` e `quantidadeNormalizada`, mas novos fluxos de forecast
 * devem usar os aliases de dominio para deixar evidente qual etapa da limpeza
 * historica esta sendo persistida.</p>
 */
class HistoricoDemandPlanItemCommunityContractTest {

    @Test
    void aliasesShouldReadAndWriteLegacyHistoricalQuantityFields() {

        HistoricoDemandPlanItem historicoDemandPlanItem = new HistoricoDemandPlanItem();

        historicoDemandPlanItem.setVendaHistoricaTratamentoStockouts(12.5d);
        historicoDemandPlanItem.setVendaHistoricaTratamentoOutliers(11.0d);

        Assertions.assertEquals(
                12.5d,
                historicoDemandPlanItem.getVendaHistoricaTratamentoStockouts(),
                0.0001d);
        Assertions.assertEquals(
                12.5d,
                historicoDemandPlanItem.getQuantidadeBase(),
                0.0001d);
        Assertions.assertEquals(
                12.5d,
                historicoDemandPlanItem.getQuantidadeBaseCadastrada(),
                0.0001d);
        Assertions.assertEquals(
                11.0d,
                historicoDemandPlanItem.getVendaHistoricaTratamentoOutliers(),
                0.0001d);
        Assertions.assertEquals(
                11.0d,
                historicoDemandPlanItem.getQuantidadeNormalizada(),
                0.0001d);
        Assertions.assertEquals(
                11.0d,
                historicoDemandPlanItem.getQuantidadeNormalizadaCadastrada(),
                0.0001d);

    }

    @Test
    void aliasesShouldKeepZeroFallbackAndRegisteredNullSemantics() {

        HistoricoDemandPlanItem historicoDemandPlanItem = new HistoricoDemandPlanItem();

        Assertions.assertEquals(
                0.0d,
                historicoDemandPlanItem.getVendaHistoricaTratamentoStockouts(),
                0.0001d);
        Assertions.assertNull(
                historicoDemandPlanItem.getVendaHistoricaTratamentoStockoutsCadastrada());
        Assertions.assertEquals(
                0.0d,
                historicoDemandPlanItem.getVendaHistoricaTratamentoOutliers(),
                0.0001d);
        Assertions.assertNull(
                historicoDemandPlanItem.getVendaHistoricaTratamentoOutliersCadastrada());

    }

    @Test
    void treatedHistoricalSalesShouldRejectNegativeRegisteredValues() {

        HistoricoDemandPlanItem historicoDemandPlanItem =
                criaHistoricoDemandPlanItem();
        historicoDemandPlanItem.setVendaHistoricaTratamentoStockouts(-1.0d);

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        historicoDemandPlanItem::getVendaHistoricaTratamentoStockouts);

        Assertions.assertEquals(
                "Demand Plan stockout-treated historical sales must be finite and non-negative for "
                        + "material MAT_01 / location LOC_01 / reference date 2026-01-01T00:00: -1.0.",
                illegalStateException.getMessage());

    }

    @Test
    void treatedHistoricalSalesShouldRejectNonFiniteRegisteredValues() {

        HistoricoDemandPlanItem historicoDemandPlanItem =
                criaHistoricoDemandPlanItem();
        historicoDemandPlanItem.setVendaHistoricaTratamentoOutliers(Double.NaN);

        IllegalStateException illegalStateException =
                Assertions.assertThrows(
                        IllegalStateException.class,
                        historicoDemandPlanItem::getVendaHistoricaTratamentoOutliers);

        Assertions.assertEquals(
                "Demand Plan outlier-treated historical sales must be finite and non-negative for "
                        + "material MAT_01 / location LOC_01 / reference date 2026-01-01T00:00: NaN.",
                illegalStateException.getMessage());

    }

    @Test
    void trendAndSeasonalComponentsShouldKeepSignedStatisticalSemantics() {

        HistoricoDemandPlanItem historicoDemandPlanItem = new HistoricoDemandPlanItem();
        historicoDemandPlanItem.setQuantidadeTrend(-2.0d);
        historicoDemandPlanItem.setQuantidadeSeasonal(-3.0d);

        Assertions.assertEquals(
                -2.0d,
                historicoDemandPlanItem.getQuantidadeTrend(),
                0.0001d);
        Assertions.assertEquals(
                -3.0d,
                historicoDemandPlanItem.getQuantidadeSeasonal(),
                0.0001d);

    }

    private static HistoricoDemandPlanItem criaHistoricoDemandPlanItem() {

        return new HistoricoDemandPlanItem(
                new HistoricoDemandPlanItem.HistoricoDemandPlanItemKey(
                        new DemandPlan(),
                        new Location("LOC_01"),
                        new Produto("MAT_01"),
                        LocalDateTime.of(2026, 1, 1, 0, 0)));

    }

}
