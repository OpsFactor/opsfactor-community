package com.opsfactor.community.capability.supplyplanning.configuration.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato Community do modo de execucao do perfil de Supply Planning.
 *
 * <p>Community implementa somente heuristico, mas a entidade precisa preservar
 * valores Enterprise persistidos ou recebidos de payload compartilhado. Essa
 * preservacao permite que o service falhe explicitamente quando nao houver SPI
 * Enterprise, e que o overlay privado consiga delegar para o bean real quando
 * estiver no classpath.</p>
 */
class PerfilExecucaoSupplyPlanCommunityContractTest {

    @Test
    void clientDemandConsolidationShouldDefaultToFalseWhenUnset() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();

        Assertions.assertFalse(perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda());

    }

    @Test
    void getModoExecucaoShouldDefaultToHeuristicWhenUnset() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();

        Assertions.assertEquals(
                PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO,
                perfilExecucaoSupplyPlan.getModoExecucao());

    }

    @Test
    void getModoExecucaoShouldPreserveOptimizerSelectionForEnterpriseBoundary() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);

        Assertions.assertEquals(
                PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR,
                perfilExecucaoSupplyPlan.getModoExecucao());

    }

    @Test
    void getModoExecucaoShouldPreserveProcessChainSelectionForEnterpriseBoundary() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN);

        Assertions.assertEquals(
                PerfilExecucaoSupplyPlan.ModoExecucao.PROCESS_CHAIN,
                perfilExecucaoSupplyPlan.getModoExecucao());

    }

    @Test
    void constrainedExecutionShouldRemainMandatoryForLegacyProfilesThatDisableIt() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setEncadeiaExecucaoPlanoRestrito(false);

        /*
         * A rodada heurística sempre materializa primeiro o Unconstrained Plan
         * e depois o Constrained Plan. O valor legado não pode mais suprimir a
         * segunda rodada nem deixar o Working Plan sem uma série viável.
         */
        Assertions.assertTrue(perfilExecucaoSupplyPlan.getEncadeiaExecucaoPlanoRestrito());
        Assertions.assertEquals(
                Constantes.TipoPlano.PLANO_RESTRITO,
                perfilExecucaoSupplyPlan.getTipoPlanoTrabalho());

    }

    @Test
    void explicitUnconstrainedWorkingPlanShouldRemainSelectableAfterMandatoryConstrainedRound() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setTipoPlanoTrabalho(Constantes.TipoPlano.PLANO_IRRESTRITO);

        Assertions.assertEquals(
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                perfilExecucaoSupplyPlan.getTipoPlanoTrabalho());

    }

    @Test
    void profitLossPreferencesShouldKeepSafeDefaultsWhenUnset() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();

        Assertions.assertFalse(perfilExecucaoSupplyPlan.getGenerateProfitLoss());
        Assertions.assertTrue(perfilExecucaoSupplyPlan.getAllowSalesProfitLossBomRetroaction());

    }

    @Test
    void profitLossPreferencesShouldPreserveExplicitConfigurationForEnterpriseLifecycle() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setGenerateProfitLoss(true);
        perfilExecucaoSupplyPlan.setAllowSalesProfitLossBomRetroaction(false);

        Assertions.assertTrue(perfilExecucaoSupplyPlan.getGenerateProfitLoss());
        Assertions.assertFalse(perfilExecucaoSupplyPlan.getAllowSalesProfitLossBomRetroaction());

    }

    @Test
    void requisitionRoundingPeriodsShouldDefaultToOneWhenUnset() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();

        Assertions.assertEquals(
                1,
                perfilExecucaoSupplyPlan.getNumeroPeriodosExpedicaoArredondaRequisicoesLoteMinimoEMultiplo());

    }

    @Test
    void requisitionRoundingPeriodsShouldRejectNonPositiveConfiguredValue() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setNumeroPeriodosExpedicaoArredondaRequisicoesLoteMinimoEMultiplo(0);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                perfilExecucaoSupplyPlan::getNumeroPeriodosExpedicaoArredondaRequisicoesLoteMinimoEMultiplo);

        Assertions.assertEquals(
                "Supply planning requisition MOQ/lot-size rounding expedition periods "
                        + "must be positive when explicitly configured: 0.",
                illegalStateException.getMessage());

    }

    @Test
    void productionRoundingPeriodsShouldDefaultToOneWhenUnset() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();

        Assertions.assertEquals(
                1,
                perfilExecucaoSupplyPlan.getNumeroPeriodosArredondaProducaoLoteMinimoEMultiplo());

    }

    @Test
    void productionRoundingPeriodsShouldRejectNonPositiveConfiguredValue() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setNumeroPeriodosArredondaProducaoLoteMinimoEMultiplo(-1);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                perfilExecucaoSupplyPlan::getNumeroPeriodosArredondaProducaoLoteMinimoEMultiplo);

        Assertions.assertEquals(
                "Supply planning production MOQ/lot-size rounding periods "
                        + "must be positive when explicitly configured: -1.",
                illegalStateException.getMessage());

    }

    @Test
    void planHorizonShouldUseGlobalDefaultWhenUnset() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setHorizonteForecastDias(84);

        Assertions.assertEquals(
                84,
                perfilExecucaoSupplyPlan.getHorizontePlanoDias(parametrosGlobais));

    }

    @Test
    void planHorizonShouldUsePositiveConfiguredValue() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setHorizontePlanoDias(21);

        /*
         * Quando o perfil possui horizonte proprio, o getter nao precisa
         * consultar o default global. Isso preserva o contrato de override e
         * deixa a validacao do valor cadastrado local ao perfil.
         */
        Assertions.assertEquals(
                21,
                perfilExecucaoSupplyPlan.getHorizontePlanoDias(null));

    }

    @Test
    void planHorizonShouldRejectNonPositiveConfiguredValue() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setHorizontePlanoDias(0);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> perfilExecucaoSupplyPlan.getHorizontePlanoDias(null));

        Assertions.assertEquals(
                "Supply planning plan horizon in days must be positive when explicitly configured: 0.",
                illegalStateException.getMessage());

    }

    @Test
    void modoPropagacaoDemandaShouldExposeExplicitOriginAndDestinationLocations() {

        Assertions.assertEquals(
                LocationAbstract.TipoLocation.INTERNA,
                PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda
                        .PROPAGACAO_DEMANDA_LOCATIONS_INTERNAS
                        .getTipoLocationDestinoPropagacao());
        Assertions.assertEquals(
                List.of(
                        LocationAbstract.TipoLocation.CLIENTE_FINAL,
                        LocationAbstract.TipoLocation.REGIAO_COMERCIAL),
                PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda
                        .PROPAGACAO_DEMANDA_LOCATIONS_INTERNAS
                        .getTiposLocationOrigemPropagacao());

        Assertions.assertEquals(
                LocationAbstract.TipoLocation.REGIAO_COMERCIAL,
                PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda
                        .PROPAGACAO_DEMANDA_REGIOES_COMERCIAIS
                        .getTipoLocationDestinoPropagacao());
        Assertions.assertEquals(
                List.of(LocationAbstract.TipoLocation.CLIENTE_FINAL),
                PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda
                        .PROPAGACAO_DEMANDA_REGIOES_COMERCIAIS
                        .getTiposLocationOrigemPropagacao());

    }

    @Test
    void optimizerTemporalObjectiveMultiplierShouldPreserveLegacyLinearDefaultsForEnterpriseOverlay() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setIncrementaImpactoFuncaoObjetivoPrimeirosPeriodos(true);
        perfilExecucaoSupplyPlan.setIncrementoPercentualImpactoFuncaoObjetivoPrimeiroPeriodo(4.0);
        Calendario calendario = criaCalendarioComDozePeriodosFuturos();

        /*
         * O Community bloqueia a configuracao de funcao objetivo nas bordas de
         * service, mas o Enterprise usa a mesma entidade no optimizer. Por isso
         * a regra numerica precisa continuar disponivel aqui, com defaults
         * compativeis com o legado.
         */
        Assertions.assertEquals(
                5.0,
                perfilExecucaoSupplyPlan.getMultiplicadorTemporalFuncaoObjetivoPrimeirosPeriodos(0, calendario),
                1E-9);
        Assertions.assertEquals(
                1.0,
                perfilExecucaoSupplyPlan.getMultiplicadorTemporalFuncaoObjetivoPrimeirosPeriodos(11, calendario),
                1E-9);

    }

    @Test
    void optimizerTemporalObjectiveMultiplierShouldApplyExponentialDecayForEnterpriseOverlay() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setIncrementaImpactoFuncaoObjetivoPrimeirosPeriodos(true);
        perfilExecucaoSupplyPlan.setIncrementoPercentualImpactoFuncaoObjetivoPrimeiroPeriodo(4.0);
        perfilExecucaoSupplyPlan.setModeloDecaimentoImpactoTemporalFuncaoObjetivo(
                PerfilExecucaoSupplyPlan.ModeloDecaimentoImpactoTemporal.EXPONENCIAL);
        perfilExecucaoSupplyPlan.setFatorDecaimentoExponencialImpactoTemporalFuncaoObjetivo(0.35);
        perfilExecucaoSupplyPlan.setPisoMultiplicadorImpactoTemporalFuncaoObjetivo(0.20);
        Calendario calendario = criaCalendarioComDozePeriodosFuturos();

        Assertions.assertEquals(
                5.0,
                perfilExecucaoSupplyPlan.getMultiplicadorTemporalFuncaoObjetivoPrimeirosPeriodos(0, calendario),
                1E-9);
        Assertions.assertEquals(
                1.75,
                perfilExecucaoSupplyPlan.getMultiplicadorTemporalFuncaoObjetivoPrimeirosPeriodos(1, calendario),
                1E-9);
        Assertions.assertEquals(
                0.6125,
                perfilExecucaoSupplyPlan.getMultiplicadorTemporalFuncaoObjetivoPrimeirosPeriodos(2, calendario),
                1E-9);
        Assertions.assertEquals(
                0.20,
                perfilExecucaoSupplyPlan.getMultiplicadorTemporalFuncaoObjetivoPrimeirosPeriodos(4, calendario),
                1E-9);
        Assertions.assertEquals(
                -1.75,
                perfilExecucaoSupplyPlan.aplicaMultiplicadorTemporalFuncaoObjetivo(-1.0, 1, calendario),
                1E-9);

    }

    @Test
    void optimizerTemporalObjectiveMultiplierShouldRejectInvalidExponentialFactorForEnterpriseOverlay() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setFatorDecaimentoExponencialImpactoTemporalFuncaoObjetivo(1.01d);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                perfilExecucaoSupplyPlan::getFatorDecaimentoExponencialImpactoTemporalFuncaoObjetivo);

        Assertions.assertEquals(
                "Temporal objective exponential decay factor must be finite and between 0 and 1: 1.01.",
                illegalStateException.getMessage());

    }

    @Test
    void optimizerEconomicObjectiveScalarsShouldDefaultToZeroForEnterpriseOverlay() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();

        /*
         * Estes campos sao bloqueados nos services Community, mas ficam na
         * entidade compartilhada para que o Enterprise consuma o mesmo perfil.
         * Ausencia historica segue como zero operacional; valor presente
         * invalido deve falhar nos getters especificos.
         */
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getPenalizacaoUnitariaDemandaNaoAtendida());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getValorUnitarioVenda());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getCustoPercentualWorkingCapital());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getCoeficienteImpactoGapSafetyStock());

    }

    @Test
    void optimizerPercentageAndMultiplierScalarsShouldDefaultForEnterpriseOverlay() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();

        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getIncrementoPercentualAtendimentoDemandaFuncaoObjetivo());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getValorMinimoAtendimentoDemandaFuncaoObjetivo());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getPercentualPenalizacaoDemandaNaoAtendidaFatFunObj());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getPenalidadePercentualFairShareDemandaNaoAtendida());
        Assertions.assertEquals(1.0d, perfilExecucaoSupplyPlan.getCoeficienteImpactoDemandaAtendidaPlanoDemanda());
        Assertions.assertEquals(1.0d, perfilExecucaoSupplyPlan.getCoeficienteImpactoDemandaAtendidaCarteira());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getIncrementoPercentualMaximoImpactoWorkingCapitalLoteMaisAntigo());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getPenalidadePercentualDiferencaParaTargetVariavel());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getAmplitudeDesvioComoPercentualValorTarget());
        Assertions.assertEquals(0.0d, perfilExecucaoSupplyPlan.getIncentivoPercentualAtendimentoOrdemFirmeSobreCogs());

    }

    @Test
    void openOrderFlagsShouldDefaultToFalseAndPreserveEnterpriseOverlayValues() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();

        /*
         * Community nao expõe pedidos transacionais em Supply Planning, mas a
         * entidade compartilhada precisa guardar os booleans que o overlay
         * Enterprise salva e que o optimizer usa para materializar
         * PedidosAbertosProjection. Nulo segue sendo o default Community: false.
         */
        Assertions.assertFalse(perfilExecucaoSupplyPlan.getConsideraOrdensSellout());
        Assertions.assertFalse(perfilExecucaoSupplyPlan.getConsideraOrdensSellin());
        Assertions.assertFalse(perfilExecucaoSupplyPlan.getConsideraOrdensTransferencia());
        Assertions.assertFalse(perfilExecucaoSupplyPlan.getConsideraOrdensCompra());

        perfilExecucaoSupplyPlan.setConsideraOrdensSelloutBacklog(true);
        perfilExecucaoSupplyPlan.setConsideraOrdensSellinFuturas(true);
        perfilExecucaoSupplyPlan.setConsideraOrdensTransferenciaBacklog(true);
        perfilExecucaoSupplyPlan.setConsideraOrdensCompraFuturas(true);

        Assertions.assertTrue(perfilExecucaoSupplyPlan.getConsideraOrdensSellout());
        Assertions.assertTrue(perfilExecucaoSupplyPlan.getConsideraOrdensSellin());
        Assertions.assertTrue(perfilExecucaoSupplyPlan.getConsideraOrdensTransferencia());
        Assertions.assertTrue(perfilExecucaoSupplyPlan.getConsideraOrdensCompra());

    }

    @Test
    void materialFilterShouldPersistOnlyItsIdWithoutCommunityEntityDependency() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setMaterialFilterId("MATERIAL-FILTER");

        Assertions.assertEquals(
                "MATERIAL-FILTER",
                perfilExecucaoSupplyPlan.getMaterialFilterId());

        perfilExecucaoSupplyPlan.setMaterialFilterId("MATERIAL-FILTER-ONLY");

        Assertions.assertEquals(
                "MATERIAL-FILTER-ONLY",
                perfilExecucaoSupplyPlan.getMaterialFilterId());

    }

    private Calendario criaCalendarioComDozePeriodosFuturos() {

        return Calendario.criaCalendarioDeOffsetsPeriodos(
                Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                0,
                0,
                12,
                0);

    }

}
