package com.opsfactor.community.capability.planningbook.keyfigure.service;

import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardMonetariaDemandPlanning;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardSupplyPlanning;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Contrato Community das normalizacoes internas de key figures.
 *
 * <p>O Planning Book Community aceita `Direct Demand` como totalizador
 * ajustavel e direciona o delta para `Demand Adjustment`. Outras KFs que ainda
 * existam no enum compartilhado, como `Direct Demand / Working Day`, devem
 * permanecer visiveis para as validacoes de fronteira e nao podem ser
 * convertidas silenciosamente para uma KF editavel.</p>
 */
public class KeyFigureServiceCommunityContractTest {

    @Test
    public void getKeyFiguresDpQueCompoemDemandaDiretaShouldExposeOnlyCommunityDemandComposition() {

        KeyFigureService keyFigureService = new KeyFigureService();
        List<KeyFigureInterface> keyFiguresQueCompoemDemandaDireta =
                keyFigureService.getKeyFiguresDpQueCompoemDemandaDireta();

        /*
         * Este contrato protege o recorte Community do Demand Planning Book:
         * a demanda direta futura vem apenas de forecast base e ajuste manual.
         * Uplift, New Products, carteira e KFs customizadas continuam no enum
         * compartilhado apenas para leitura/bloqueio em outras bordas.
         */
        Assertions.assertEquals(
                List.of(
                        new KeyFigureStandard(KeyFigureStandardEnum.BASELINE),
                        new KeyFigureStandard(KeyFigureStandardEnum.AJUSTE_DEMANDA)),
                keyFiguresQueCompoemDemandaDireta);

    }

    @Test
    public void getKeyFigureAjusteDemandaCommunityShouldMapDirectDemandToDemandAdjustment() {

        KeyFigureService keyFigureService = new KeyFigureService();
        KeyFigureInterface keyFigureAjuste = keyFigureService.getKeyFigureAjusteDemandaCommunity(
                new KeyFigureStandard(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP));

        Assertions.assertEquals(
                new KeyFigureStandard(KeyFigureStandardEnum.AJUSTE_DEMANDA),
                keyFigureAjuste);

    }

    @Test
    public void getKeyFigureAjusteDemandaCommunityShouldNotNormalizeWorkingDayKeyFigure() {

        KeyFigureService keyFigureService = new KeyFigureService();
        KeyFigureStandard keyFigureWorkingDay =
                new KeyFigureStandard(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL);

        KeyFigureInterface keyFigureAjuste = keyFigureService.getKeyFigureAjusteDemandaCommunity(
                keyFigureWorkingDay);

        Assertions.assertEquals(keyFigureWorkingDay, keyFigureAjuste);

    }

    @Test
    public void getKeyFigureDeIdShouldResolveStandardKeyFigureByJsonLabel() {

        KeyFigureService keyFigureService = new KeyFigureService();

        KeyFigureInterface keyFigureInterface = keyFigureService.getKeyFigureDeId("Baseline");

        Assertions.assertEquals(
                new KeyFigureStandard(KeyFigureStandardEnum.BASELINE),
                keyFigureInterface);

    }

    @Test
    public void getKeyFigureDeIdShouldResolveTypedSupplyPlanningBookKeyFigure() {

        KeyFigureService keyFigureService = new KeyFigureService();

        KeyFigureInterface keyFigureInterface = keyFigureService.getKeyFigureDeId(
                "Planned Production-Working Plan");

        /*
         * Supply Planning Book Community ainda permite KFs padrao tipadas por
         * plano para linhas quantitativas. Isso e diferente de KFs customizadas:
         * o sufixo de plano faz parte do id conhecido, nao de uma configuracao
         * dinamica de view.
         */
        Assertions.assertEquals(
                new KeyFigureStandardSupplyPlanning(
                        KeyFigureStandardEnum.PRODUCAO_PLANEJADA,
                        Constantes.TipoPlano.PLANO_TRABALHO),
                keyFigureInterface);

    }

    @Test
    public void getKeyFigureStandardShouldResolveGrossAndNetAsMonetarySharedIdentities() {

        KeyFigureStandard keyFigureGross = KeyFigureService
                .getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.VENDAS_GROSS);
        KeyFigureStandard keyFigureNet = KeyFigureService
                .getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.VENDAS_NET);

        Assertions.assertInstanceOf(KeyFigureStandardMonetariaDemandPlanning.class, keyFigureGross);
        Assertions.assertInstanceOf(KeyFigureStandardMonetariaDemandPlanning.class, keyFigureNet);
        Assertions.assertEquals(
                Constantes.TipoValor.GROSS,
                ((KeyFigureStandardMonetariaDemandPlanning) keyFigureGross).getTipoValor());
        Assertions.assertEquals(
                Constantes.TipoValor.NET,
                ((KeyFigureStandardMonetariaDemandPlanning) keyFigureNet).getTipoValor());

    }

    @Test
    public void getKeyFigureDeDescricaoShouldRejectCustomKeyFiguresAsEnterpriseCapability() {

        KeyFigureService keyFigureService = new KeyFigureService();

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> keyFigureService.getKeyFigureDeDescricao("Gross Margin"));

        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Custom key figures requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void getKeyFiguresQueCompoemDemandaDiretaShouldIgnoreConfiguredViewProjectionInCommunity() {

        KeyFigureService keyFigureService = new KeyFigureService();

        List<KeyFigureInterface> keyFiguresQueCompoemDemandaDireta =
                keyFigureService.getKeyFiguresQueCompoemDemandaDireta(null);

        Assertions.assertEquals(
                keyFigureService.getKeyFiguresDpQueCompoemDemandaDireta(),
                keyFiguresQueCompoemDemandaDireta);

    }

}
