package com.opsfactor.community.capability.demandplanning.demandplan.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * Contratos Community da entidade `DemandPlanItem`.
 *
 * <p>A entidade ainda preserva campos fisicos de Uplift e New Materials durante
 * a migracao do schema, mas os calculos agregados do Community devem operar
 * somente com Baseline e Demand Adjustment.</p>
 */
class DemandPlanItemCommunityContractTest {

    @Test
    void demandPlanQuantitiesShouldTreatNullAsOperationalZero() {

        DemandPlanItem demandPlanItem = new DemandPlanItem();

        Assertions.assertEquals(
                0.0d,
                demandPlanItem.getQuantidadeBaseline());
        Assertions.assertEquals(
                0.0d,
                demandPlanItem.getQuantidadeAjusteDemanda());
        Assertions.assertEquals(
                0.0d,
                demandPlanItem.getQuantidadeBaselineTrend());

    }

    @Test
    void demandPlanQuantitiesShouldAllowNegativeFiniteAdjustmentAndTrendComponents() {

        DemandPlanItem demandPlanItem =
                criaDemandPlanItem();
        demandPlanItem.setQuantidadeAjusteDemanda(-3.0d);
        demandPlanItem.setQuantidadeBaselineTrend(-2.0d);

        Assertions.assertEquals(
                -3.0d,
                demandPlanItem.getQuantidadeAjusteDemanda());
        Assertions.assertEquals(
                -2.0d,
                demandPlanItem.getQuantidadeBaselineTrend());

    }

    @Test
    void demandPlanQuantitiesShouldRejectNonFiniteValues() {

        DemandPlanItem demandPlanItem =
                criaDemandPlanItem();
        demandPlanItem.setQuantidadeBaseline(Double.POSITIVE_INFINITY);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanItem::getQuantidadeBaseline);

        Assertions.assertEquals(
                "Demand Plan quantity unrestricted baseline must be finite for "
                        + "material MAT / location PLANT / reference date 2026-01-01T00:00: Infinity.",
                illegalStateException.getMessage());

    }

    @Test
    void setQuantidadeTotalShouldIgnoreAndNeutralizeEnterpriseKeyFiguresInCommunityProportionalUpdate() {

        DemandPlanItem demandPlanItem = new DemandPlanItem();
        demandPlanItem.setQuantidadeBaseline(20.0d);
        demandPlanItem.setQuantidadeAjusteDemanda(20.0d);
        demandPlanItem.setQuantidadeItensNovos(15.0d);
        demandPlanItem.setQuantidadeUplift(45.0d);

        demandPlanItem.setQuantidadeTotal(
                80.0d,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                Constantes.ModificacaoAgregadaPlano.PROPORCIONAL_OU_BASELINE);

        Assertions.assertEquals(40.0d, demandPlanItem.getQuantidadeBaseline(), 0.0001d);
        Assertions.assertEquals(40.0d, demandPlanItem.getQuantidadeAjusteDemanda(), 0.0001d);
        Assertions.assertEquals(0.0d, demandPlanItem.getQuantidadeItensNovos(), 0.0001d);
        Assertions.assertEquals(0.0d, demandPlanItem.getQuantidadeUplift(), 0.0001d);

    }

    @Test
    void setQuantidadeTotalShouldIgnoreAndNeutralizeEnterpriseKeyFiguresInCommunityAdjustmentLineUpdate() {

        DemandPlanItem demandPlanItem = new DemandPlanItem();
        demandPlanItem.setQuantidadeBaseline(20.0d);
        demandPlanItem.setQuantidadeAjusteDemanda(5.0d);
        demandPlanItem.setQuantidadeItensNovos(15.0d);
        demandPlanItem.setQuantidadeUplift(45.0d);

        demandPlanItem.setQuantidadeTotal(
                40.0d,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                Constantes.ModificacaoAgregadaPlano.LINHA_AJUSTE);

        Assertions.assertEquals(20.0d, demandPlanItem.getQuantidadeBaseline(), 0.0001d);
        Assertions.assertEquals(20.0d, demandPlanItem.getQuantidadeAjusteDemanda(), 0.0001d);
        Assertions.assertEquals(0.0d, demandPlanItem.getQuantidadeItensNovos(), 0.0001d);
        Assertions.assertEquals(0.0d, demandPlanItem.getQuantidadeUplift(), 0.0001d);

    }

    @Test
    void trendSeasonalReadShouldRejectNullComponentBeforeUnitConversion() {

        DemandPlanItem demandPlanItem = new DemandPlanItem();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanItem.getQuantidadeTrendSeason(
                        0,
                        null,
                        null,
                        null,
                        null,
                        null));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "DemandPlanItem can read only materialized trend/seasonal components"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("received null"));

    }

    @Test
    void getQuantidadeShouldRejectNonPhysicalPlanWithContractMessage() {

        DemandPlanItem demandPlanItem = new DemandPlanItem();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanItem.getQuantidade(
                        Constantes.TipoDemanda.BASELINE,
                        Constantes.TipoPlano.PLANO_TRABALHO));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "DemandPlanItem getQuantidade accepts only Unconstrained Plan or Constrained Plan physical quantities"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("received plan=PLANO_TRABALHO"));

    }

    @Test
    void getQuantidadeShouldRejectSupplyAdjustmentComponentWithContractMessage() {

        DemandPlanItem demandPlanItem = new DemandPlanItem();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanItem.getQuantidade(
                        Constantes.TipoDemanda.AJUSTE_SUPPLY,
                        Constantes.TipoPlano.PLANO_IRRESTRITO));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "accepts only materialized demand components"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("Supply Adjustment"));

    }

    @Test
    void getQuantidadeShouldRejectSupplyKeyFigureWithContractMessage() {

        DemandPlanItem demandPlanItem = new DemandPlanItem();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanItem.getQuantidade(
                        new KeyFigureStandard(KeyFigureStandardEnum.ESTOQUE)));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "accepts only Demand Plan physical key figures"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("New Materials"));
        Assertions.assertFalse(illegalArgumentException.getMessage().contains("New Products"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("received key figure=ESTOQUE"));

    }

    private static DemandPlanItem criaDemandPlanItem() {

        return new DemandPlanItem(
                new DemandPlanItem.DemandPlanItemKey(
                        new DemandPlan(),
                        new Location("PLANT"),
                        new Produto("MAT"),
                        LocalDateTime.of(2026, 1, 1, 0, 0)));

    }

}
