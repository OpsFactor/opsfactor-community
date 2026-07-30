package com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opsfactor.community.capability.planningbook.facade.dto.KeyFigureDTOAbstract;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureCoberturaEstoque;
import com.opsfactor.community.platform.calendar.Calendario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consolida Stock in Days pela trajetória física agregada, e não pela soma
 * das coberturas já calculadas nas folhas.
 */
public class KeyFigureDTOCoberturaEstoque extends KeyFigureDTOAbstract<
        DFUDataKeyFigureCoberturaEstoque, KeyFigureDTOCoberturaEstoque> {

    private static final double EPSILON = 0.00001d;

    @JsonIgnore private Calendario calendario;
    @JsonIgnore private Map<Integer, Double> estoqueProjetadoPorPeriodo = new HashMap<>();
    @JsonIgnore private Map<Integer, Double> saldoEntradasSaidasPorPeriodo = new HashMap<>();

    public KeyFigureDTOCoberturaEstoque(String keyFigure, EditMode editMode) {

        super(keyFigure, editMode);

    }

    @Override
    public void importaDadosDFUDataKeyFigure(
            Calendario calendario,
            List<DFUDataKeyFigureCoberturaEstoque> dados) {

        this.calendario = calendario;
        for (DFUDataKeyFigureCoberturaEstoque dado : dados) {
            int periodo = calendario.getPosicaoPeriodo(dado.getData());
            estoqueProjetadoPorPeriodo.merge(periodo, dado.getQuantidadeEstoqueProjetado(), Double::sum);
            saldoEntradasSaidasPorPeriodo.merge(periodo, dado.getSaldoEntradasSaidas(), Double::sum);
        }
        recalculaValores();

    }

    @Override
    public KeyFigureDTOCoberturaEstoque getCopiaSomenteComKeyFigureIdEEditMode() {

        return new KeyFigureDTOCoberturaEstoque(keyFigure, editMode);

    }

    @Override
    public KeyFigureDTOCoberturaEstoque getCopiaCompleta() {

        KeyFigureDTOCoberturaEstoque copia = getCopiaSomenteComKeyFigureIdEEditMode();
        copia.values = values == null ? null : new HashMap<>(values);
        copia.unavailableReasons = unavailableReasons == null ? null : new HashMap<>(unavailableReasons);
        copia.toolTips = toolTips == null ? null : new HashMap<>(toolTips);
        copia.additionalClasses = additionalClasses == null ? null : new HashMap<>(additionalClasses);
        copia.calendario = calendario;
        copia.estoqueProjetadoPorPeriodo = new HashMap<>(estoqueProjetadoPorPeriodo);
        copia.saldoEntradasSaidasPorPeriodo = new HashMap<>(saldoEntradasSaidasPorPeriodo);
        return copia;

    }

    @Override
    public void incorporaValoresDeKeyFigure(KeyFigureDTOCoberturaEstoque outro) {

        if (calendario == null) {
            calendario = outro.calendario;
        } else if (outro.calendario != null && calendario != outro.calendario) {
            throw new IllegalArgumentException("Only stock coverage key figures from the same calendar can be aggregated");
        }

        outro.estoqueProjetadoPorPeriodo.forEach((periodo, quantidadeEstoque) ->
                estoqueProjetadoPorPeriodo.merge(periodo, quantidadeEstoque, Double::sum));
        outro.saldoEntradasSaidasPorPeriodo.forEach((periodo, saldoEntradasSaidas) ->
                saldoEntradasSaidasPorPeriodo.merge(periodo, saldoEntradasSaidas, Double::sum));
        recalculaValores();

    }

    /**
     * Para cada fechamento de período, consome os fluxos agregados dos
     * períodos futuros até esgotar o estoque. A quantidade de dias do bucket
     * é aplicada uma única vez, independentemente do número de DFUs-filhas.
     */
    private void recalculaValores() {

        values = new HashMap<>();
        if (calendario == null) {
            return;
        }

        estoqueProjetadoPorPeriodo.keySet().stream()
                .sorted()
                .forEach(periodoInicial -> values.put(
                        calendario.getUltimaDataHorarioPeriodo(periodoInicial).toString(),
                        calculaCoberturaEmDias(periodoInicial)));

    }

    private double calculaCoberturaEmDias(int periodoInicial) {

        double quantidadeEstoque = estoqueProjetadoPorPeriodo.getOrDefault(periodoInicial, 0.0d);
        if (quantidadeEstoque <= 0.0d
                || periodoInicial == calendario.getNumeroPeriodosTotais()) {
            return 0.0d;
        }

        double quantidadeDiasEstoque = 0.0d;
        for (int periodoFuturo = periodoInicial + 1;
                periodoFuturo < calendario.getNumeroPeriodosTotais();
                periodoFuturo++) {

            double saldoEntradasSaidas = saldoEntradasSaidasPorPeriodo.getOrDefault(periodoFuturo, 0.0d);
            double novaQuantidadeEstoque = quantidadeEstoque + saldoEntradasSaidas;
            if (novaQuantidadeEstoque >= EPSILON) {
                quantidadeDiasEstoque += calendario.getNumeroDiasNoPeriodo(periodoFuturo);
            } else {
                if (saldoEntradasSaidas >= 0.0d) {
                    return quantidadeDiasEstoque;
                }
                return quantidadeDiasEstoque
                        + calendario.getNumeroDiasNoPeriodo(periodoFuturo)
                        * quantidadeEstoque
                        / -saldoEntradasSaidas;
            }
            quantidadeEstoque = Math.max(novaQuantidadeEstoque, 0.0d);
        }

        return quantidadeDiasEstoque;

    }

}
