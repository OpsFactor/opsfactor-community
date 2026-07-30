package com.opsfactor.community.capability.demandplanning.forecast.disaggregation.engine;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionAgregado;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjectionMaterialLocation;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;

import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Desagregacao Community por Historical Sales.
 *
 * <p>A participacao de cada DFU ativa e calculada a partir da demanda historica
 * acumulada na janela anterior ao periodo de referencia do plano. Quando a
 * soma historica dos filhos ativos e zero, o forecast agregado e distribuido
 * uniformemente entre os filhos ativos.</p>
 */
public class HistoricalSalesForecastDisaggregation implements DemandForecastDisaggregationSpi {

    @Override
    public Constantes.DPModeloSplit getDpModeloSplit() {

        return Constantes.DPModeloSplit.HISTORICAL_SALES;

    }

    @Override
    public void desagregaForecast(
            Calendario calendario,
            int numeroDiasSplitTopDown,
            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        desagregaForecastComMixHistorico(
                calendario,
                demandPlanForecastProjectionAgregado,
                clusterEParametrosProjection,
                demandPlanForecastProjectionReferencia ->
                        calculaDemandaHistoricaReferencia(
                                calendario,
                                numeroDiasSplitTopDown,
                                demandPlanForecastProjectionReferencia));

    }

    private double calculaDemandaHistoricaReferencia(
            Calendario calendario,
            int numeroDiasSplitTopDown,
            DemandPlanForecastProjection demandPlanForecastProjectionReferencia) {

        double demandaHistoricaConsolidada = 0.0d;

        /*
         * A janela de referencia olha para os ultimos periodos antes do presente
         * do plano. O valor em dias e convertido para periodos e arredondado
         * para cima para garantir pelo menos um periodo quando o usuario
         * configurar uma janela positiva menor que o bucket.
         */
        for (int periodo = calendario.getPosicaoPeriodoFinalPassado();
             periodo > Math.max(
                     0,
                     calendario.getPosicaoPeriodoFinalPassado()
                             - Math.ceil(calendario.converteDiasParaPeriodosCalendario(numeroDiasSplitTopDown)));
             periodo--) {
            demandaHistoricaConsolidada += demandPlanForecastProjectionReferencia.demanda[periodo];
        }

        return demandaHistoricaConsolidada;

    }

    /**
     * Aplica o percentual historico do filho sobre todas as series geradas no
     * agregado. O metodo tambem inicializa arrays opcionais nos leafs quando o
     * modelo estatistico do agregado tiver produzido trend, seasonal ou bounds.
     */
    private void desagregaForecastComMixHistorico(
            Calendario calendario,
            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado,
            ClusterEParametrosProjection clusterEParametrosProjection,
            ToDoubleFunction<DemandPlanForecastProjection> funcaoValorReferenciaParaCalculoSplit) {

        List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationList =
                demandPlanForecastProjectionAgregado.getDemandPlanForecastProjectionMaterialLocationList();

        /*
         * DFUs inativas nao recebem forecast. Sua participacao historica e
         * redistribuida proporcionalmente entre as ativas para preservar o total
         * do agregado.
         */
        List<DemandPlanForecastProjectionMaterialLocation> demandPlanForecastProjectionMaterialLocationAtivas =
                demandPlanForecastProjectionMaterialLocationList
                        .stream()
                        .filter(demandPlanForecastProjectionMaterialLocation -> clusterEParametrosProjection.isDfuAtiva(
                                demandPlanForecastProjectionMaterialLocation.getMaterial(),
                                demandPlanForecastProjectionMaterialLocation.getLocation()))
                        .collect(Collectors.toList());

        /*
         * Lista vazia significa cluster/leaf sem DFU operacionalmente ativa na
         * data de referencia. Nesse caso o split e um no-op: nenhuma serie dos
         * leafs deve ser inicializada ou receber forecast agregado, preservando
         * a decisao upstream de nao planejar DFUs inativas.
         */
        double valorConsolidadoFilhosAtivos = demandPlanForecastProjectionMaterialLocationAtivas
                .stream()
                .mapToDouble(funcaoValorReferenciaParaCalculoSplit::applyAsDouble)
                .sum();

        for (DemandPlanForecastProjection demandPlanForecastProjectionLeaf : demandPlanForecastProjectionMaterialLocationList) {
            if (demandPlanForecastProjectionLeaf instanceof DemandPlanForecastProjectionMaterialLocation) {
                Produto material = ((DemandPlanForecastProjectionMaterialLocation) demandPlanForecastProjectionLeaf).getMaterial();
                Location location = ((DemandPlanForecastProjectionMaterialLocation) demandPlanForecastProjectionLeaf).getLocation();

                if (!clusterEParametrosProjection.isDfuAtiva(material, location)) {
                    continue;
                }
            }

            double participacaoFilhoNoPai = (Math.abs(valorConsolidadoFilhosAtivos) > 0)
                    ? funcaoValorReferenciaParaCalculoSplit.applyAsDouble(demandPlanForecastProjectionLeaf) / valorConsolidadoFilhosAtivos
                    : (demandPlanForecastProjectionMaterialLocationAtivas.isEmpty()
                            ? 0.0d
                            : 1.0d / demandPlanForecastProjectionMaterialLocationAtivas.size());

            inicializaArraysOpcionaisNoLeaf(
                    calendario,
                    demandPlanForecastProjectionAgregado,
                    demandPlanForecastProjectionLeaf);
            atualizaSeriesDoLeafComParticipacaoDoAgregado(
                    calendario,
                    demandPlanForecastProjectionAgregado,
                    demandPlanForecastProjectionLeaf,
                    participacaoFilhoNoPai);
        }

    }

    private void inicializaArraysOpcionaisNoLeaf(
            Calendario calendario,
            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado,
            DemandPlanForecastProjection demandPlanForecastProjectionLeaf) {

        if (demandPlanForecastProjectionAgregado.trend != null && demandPlanForecastProjectionLeaf.trend == null) {
            demandPlanForecastProjectionLeaf.trend = new double[calendario.getNumeroPeriodosTotais()];
        }
        if (demandPlanForecastProjectionAgregado.seasonal != null && demandPlanForecastProjectionLeaf.seasonal == null) {
            demandPlanForecastProjectionLeaf.seasonal = new double[calendario.getNumeroPeriodosTotais()];
        }
        if (demandPlanForecastProjectionAgregado.trendStlHistorico != null
                && demandPlanForecastProjectionLeaf.trendStlHistorico == null) {
            demandPlanForecastProjectionLeaf.trendStlHistorico = new double[calendario.getNumeroPeriodosPassados()];
        }
        if (demandPlanForecastProjectionAgregado.seasonalStlHistorico != null
                && demandPlanForecastProjectionLeaf.seasonalStlHistorico == null) {
            demandPlanForecastProjectionLeaf.seasonalStlHistorico = new double[calendario.getNumeroPeriodosPassados()];
        }
        if (demandPlanForecastProjectionAgregado.lowerBound != null
                && demandPlanForecastProjectionAgregado.upperBound != null) {
            if (demandPlanForecastProjectionLeaf.lowerBound == null) {
                demandPlanForecastProjectionLeaf.lowerBound = new double[calendario.getNumeroPeriodosTotais()];
            }
            if (demandPlanForecastProjectionLeaf.upperBound == null) {
                demandPlanForecastProjectionLeaf.upperBound = new double[calendario.getNumeroPeriodosTotais()];
            }
        }

    }

    private void atualizaSeriesDoLeafComParticipacaoDoAgregado(
            Calendario calendario,
            DemandPlanForecastProjectionAgregado demandPlanForecastProjectionAgregado,
            DemandPlanForecastProjection demandPlanForecastProjectionLeaf,
            double participacaoFilhoNoPai) {

        for (int periodo = 0; periodo <= calendario.getPosicaoPeriodoFinalFuturo(); periodo++) {
            demandPlanForecastProjectionLeaf.forecastBaseline[periodo] =
                    demandPlanForecastProjectionAgregado.forecastBaseline[periodo] * participacaoFilhoNoPai;
            if (demandPlanForecastProjectionAgregado.trend != null) {
                demandPlanForecastProjectionLeaf.trend[periodo] =
                        demandPlanForecastProjectionAgregado.trend[periodo] * participacaoFilhoNoPai;
            }
            if (demandPlanForecastProjectionAgregado.seasonal != null) {
                demandPlanForecastProjectionLeaf.seasonal[periodo] =
                        demandPlanForecastProjectionAgregado.seasonal[periodo] * participacaoFilhoNoPai;
            }
            if (demandPlanForecastProjectionAgregado.trendStlHistorico != null
                    && periodo <= calendario.getPosicaoPeriodoFinalPassado()) {
                demandPlanForecastProjectionLeaf.trendStlHistorico[periodo] =
                        demandPlanForecastProjectionAgregado.trendStlHistorico[periodo] * participacaoFilhoNoPai;
            }
            if (demandPlanForecastProjectionAgregado.seasonalStlHistorico != null
                    && periodo <= calendario.getPosicaoPeriodoFinalPassado()) {
                demandPlanForecastProjectionLeaf.seasonalStlHistorico[periodo] =
                        demandPlanForecastProjectionAgregado.seasonalStlHistorico[periodo] * participacaoFilhoNoPai;
            }
            if (demandPlanForecastProjectionAgregado.lowerBound != null
                    && demandPlanForecastProjectionAgregado.upperBound != null) {
                demandPlanForecastProjectionLeaf.lowerBound[periodo] =
                        demandPlanForecastProjectionAgregado.lowerBound[periodo] * participacaoFilhoNoPai;
                demandPlanForecastProjectionLeaf.upperBound[periodo] =
                        demandPlanForecastProjectionAgregado.upperBound[periodo] * participacaoFilhoNoPai;
            }
        }

        for (int periodo = 0; periodo <= calendario.getPosicaoPeriodoFinalPassado(); periodo++) {
            demandPlanForecastProjectionLeaf.vendaHistoricaTratamentoStockouts[periodo] =
                    demandPlanForecastProjectionAgregado.vendaHistoricaTratamentoStockouts[periodo] * participacaoFilhoNoPai;
            demandPlanForecastProjectionLeaf.vendaHistoricaTratamentoOutliers[periodo] =
                    demandPlanForecastProjectionAgregado.vendaHistoricaTratamentoOutliers[periodo] * participacaoFilhoNoPai;
        }

    }

}
