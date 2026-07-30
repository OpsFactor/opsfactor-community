package com.opsfactor.community.capability.planningbook.keyfigure.domain;

import com.opsfactor.community.platform.utility.MetodosUtilidade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Protege a classificacao funcional das key figures padrao compartilhadas.
 *
 * <p>Algumas KFs Enterprise permanecem no enum Community apenas para decodificar
 * payloads antigos e falhar com mensagem clara nas bordas. Mesmo nesses casos,
 * o enum nao deve devolver {@code null}: se uma KF conhecida nao estiver
 * classificada como Demand Planning ou Supply Planning, o erro deve aparecer
 * neste contrato unitario antes de chegar ao Planning Book.</p>
 */
class KeyFigureStandardEnumCommunityContractTest {

    @Test
    void getTipoPlanoKeyFigureShouldClassifyEveryStandardKeyFigure() {

        for (KeyFigureStandardEnum keyFigureStandardEnum : KeyFigureStandardEnum.values()) {
            Assertions.assertNotNull(
                    keyFigureStandardEnum.getTipoPlanoKeyFigure(),
                    "Key figure standard sem tipo de plano: " + keyFigureStandardEnum);
        }

    }

    @Test
    void getTipoPlanoKeyFigureShouldClassifySupplyDirectDemandAsSupplyPlanning() {

        Assertions.assertEquals(
                KeyFigureStandardEnum.TipoPlanoKeyFigure.SUPPLY_PLAN,
                KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_SNP.getTipoPlanoKeyFigure());

    }

    @Test
    void getEditModeShouldClassifyEveryStandardKeyFigure() {

        for (KeyFigureStandardEnum keyFigureStandardEnum : KeyFigureStandardEnum.values()) {
            Assertions.assertNotNull(
                    keyFigureStandardEnum.getEditMode(),
                    "Key figure standard sem edit mode: " + keyFigureStandardEnum);
        }

    }

    @Test
    void monetarySalesShouldKeepSharedDemandPlanningIdentityAndPublicLabels() {

        Assertions.assertEquals(
                KeyFigureStandardEnum.TipoPlanoKeyFigure.DEMAND_PLAN,
                KeyFigureStandardEnum.VENDAS_GROSS.getTipoPlanoKeyFigure());
        Assertions.assertEquals(
                KeyFigureStandardEnum.TipoPlanoKeyFigure.DEMAND_PLAN,
                KeyFigureStandardEnum.VENDAS_NET.getTipoPlanoKeyFigure());
        Assertions.assertEquals(
                "Gross Sales",
                MetodosUtilidade.getValorJsonPropertyDeEnum(KeyFigureStandardEnum.VENDAS_GROSS));
        Assertions.assertEquals(
                "Net Sales",
                MetodosUtilidade.getValorJsonPropertyDeEnum(KeyFigureStandardEnum.VENDAS_NET));

    }

}
