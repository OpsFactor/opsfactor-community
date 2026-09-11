package com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection;

import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.calendar.PeriodoCalendario;
import java.time.Duration;
import com.opsfactor.community.platform.exception.IncompatibleCalendarException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Projection de conversao temporal entre dois calendarios.
 *
 * <p>No Community o split de um calendario mais agregado para outro mais
 * detalhado usa somente a curva flat implicita. Curvas temporais configuraveis,
 * filtros e pesos por DFU pertencem ao Enterprise.</p>
 */
@Getter
public class SplitTemporalProjection {

    protected final Calendario calendarioOrigem; // ex : calendário DP
    protected final Calendario calendarioTarget; // ex : calendário SNP

//    // qualquer tipo de split : será inicializado a depender de um CurvaSplitTemporal ter sido passado na inicialização ou não
//    // se sim, pode ser SplitTemporalProjectionCurva MesDiaMes / SemanaDiaSemana
//    // se não for passado um CurvaSplitTemporal, será um CurvaFlat
    protected SplitTemporalProjectionCurva splitTemporalProjectionCurvaBase;

    // Periodo Origem -> Conjunto de Períodos Target com algum overlap
    protected Map<Integer,Set<Integer>> mapaSetPeriodosTargetDentroDePeriodoOrigem = new ConcurrentHashMap<>();

    // CONSTRUTOR
    public SplitTemporalProjection(Calendario calendarioOrigem, Calendario calendarioTarget) {
        this.calendarioOrigem = calendarioOrigem;
        this.calendarioTarget = calendarioTarget;
        inicializaMapaSetPeriodosTargetDentroDePeriodoOrigem();
    }

    public Set<Integer> getPeriodosOrigemAPartirPeriodoCalendarioTarget(int periodoCalendarioTarget) {
        return mapaSetPeriodosTargetDentroDePeriodoOrigem
                .entrySet()
                .stream()
                .filter(x -> x.getValue().contains(periodoCalendarioTarget))
                .map(x -> x.getKey())
                .collect(Collectors.toSet());

    }

    public double getValorNoCalendarioTargetSplitTemporalComCurvaBase(
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoCalendarioTarget) {
        return getValorNoCalendarioTargetSplitTemporal(
                splitTemporalProjectionCurvaBase,
                valorPorPeriodoCalendarioOriginal,
                posicaoPeriodoCalendarioTarget);
    }

    public double getValorNoCalendarioTargetSplitTemporal(
            SplitTemporalProjectionCurva splitTemporalProjectionCurva,
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoCalendarioTarget) throws IncompatibleCalendarException, UnitOfMeasureConversionException {

        Integer posicaoPeriodoOrigemComMesmoIntervalo = getPosicaoPeriodoOrigemComMesmoIntervalo(
                posicaoPeriodoCalendarioTarget);

        // A cópia direta só é válida quando as fronteiras inclusivas são idênticas.
        // Comparar apenas o bucket nominal não funciona num calendário misto: duas
        // posições semanais podem ser uma semana cheia e uma semana técnica cortada.
        if (posicaoPeriodoOrigemComMesmoIntervalo != null) {
            return valorPorPeriodoCalendarioOriginal.applyAsDouble(posicaoPeriodoOrigemComMesmoIntervalo);
        }

        if (splitTemporalProjectionCurva == null) {
            throw new IllegalArgumentException("Curva de split temporal obrigatória para períodos com fronteiras diferentes");
        }
        return splitTemporalProjectionCurva.getValorNoCalendarioTargetSplitTemporalComDesagregacao(
                valorPorPeriodoCalendarioOriginal,
                posicaoPeriodoCalendarioTarget);

    }

    public double getValorNoCalendarioTargetSplitTemporalComCurvaBase(
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoInicialCalendarioTarget, int posicaoPeriodoFinalCalendarioTarget) throws IncompatibleCalendarException, UnitOfMeasureConversionException {
        return getValorNoCalendarioTargetSplitTemporal(
                splitTemporalProjectionCurvaBase,
                valorPorPeriodoCalendarioOriginal,
                posicaoPeriodoInicialCalendarioTarget,
                posicaoPeriodoFinalCalendarioTarget);
    }

    /**
     * Versão por intervalo do split flat padrão, usada quando o consumidor
     * desloca uma janela temporal em dias em vez de apontar para uma posição
     * específica do calendário de destino.
     */
    public double getValorNoRangeSplitTemporalComCurvaBase(
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            LocalDateTime dataHorarioInicialInclusivo,
            LocalDateTime dataHorarioFinalInclusivo) {

        if (splitTemporalProjectionCurvaBase == null) {
            throw new IllegalStateException("Curva base de split temporal obrigatória");
        }
        return splitTemporalProjectionCurvaBase.getValorNoRangeSplitTemporal(
                valorPorPeriodoCalendarioOriginal,
                dataHorarioInicialInclusivo,
                dataHorarioFinalInclusivo);

    }

    public double getValorNoCalendarioTargetSplitTemporal(
            SplitTemporalProjectionCurva splitTemporalProjectionCurva,
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoInicialCalendarioTarget, int posicaoPeriodoFinalCalendarioTarget) throws IncompatibleCalendarException, UnitOfMeasureConversionException {

        double valorAcumulado = 0;
        for (int i = posicaoPeriodoInicialCalendarioTarget; i <= posicaoPeriodoFinalCalendarioTarget ; i++) {
            valorAcumulado += getValorNoCalendarioTargetSplitTemporal(
                    splitTemporalProjectionCurva,
                    valorPorPeriodoCalendarioOriginal,
                    i);
        }

        return valorAcumulado;

    }

    private void inicializaMapaSetPeriodosTargetDentroDePeriodoOrigem() {
        for (Integer periodoTarget : calendarioTarget.getListaPosicoesPeriodo()) {
            PeriodoCalendario periodoCalendarioTarget = calendarioTarget.getPeriodo(periodoTarget);

            for (Integer periodoOrigem : calendarioOrigem.getListaPosicoesPeriodo()) {
                PeriodoCalendario periodoCalendarioOrigem = calendarioOrigem.getPeriodo(periodoOrigem);

                // As fronteiras são inclusivas. Basta comparar os intervalos; não
                // precisamos percorrer dia a dia nem conhecer o bucket de destino.
                boolean periodosPossuemIntersecao =
                        !periodoCalendarioOrigem.dataHorarioFinal().isBefore(periodoCalendarioTarget.dataHorarioInicial())
                        && !periodoCalendarioOrigem.dataHorarioInicial().isAfter(periodoCalendarioTarget.dataHorarioFinal());
                if (periodosPossuemIntersecao) {
                    mapaSetPeriodosTargetDentroDePeriodoOrigem
                            .computeIfAbsent(periodoOrigem, x -> new HashSet<>())
                            .add(periodoTarget);
                }
            }
        }
    }

    /**
     * Retorna a posição de origem somente quando origem e destino representam
     * exatamente a mesma janela inclusiva. Essa decisão local substitui as antigas
     * comparações de bucket global e funciona igualmente para períodos simples,
     * compostos e semanas técnicas.
     */
    protected Integer getPosicaoPeriodoOrigemComMesmoIntervalo(int posicaoPeriodoCalendarioTarget) {

        PeriodoCalendario periodoCalendarioTarget = calendarioTarget.getPeriodo(posicaoPeriodoCalendarioTarget);
        return getPeriodosOrigemAPartirPeriodoCalendarioTarget(posicaoPeriodoCalendarioTarget)
                .stream()
                .filter(posicaoPeriodoOrigem -> {
                    PeriodoCalendario periodoCalendarioOrigem = calendarioOrigem.getPeriodo(posicaoPeriodoOrigem);
                    return periodoCalendarioOrigem.dataHorarioInicial().equals(periodoCalendarioTarget.dataHorarioInicial())
                            && periodoCalendarioOrigem.dataHorarioFinal().equals(periodoCalendarioTarget.dataHorarioFinal());
                })
                .findFirst()
                .orElse(null);

    }

    
}
