package com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection;

import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.platform.calendar.Calendario;
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

    /**
     * Calendario origem, por exemplo Demand Planning.
     */
    protected final Calendario calendarioOrigem;

    /**
     * Calendario target, por exemplo Supply Planning.
     */
    protected final Calendario calendarioTarget;

    /*
     * Community usa somente a curva flat implicita. Implementacoes Enterprise
     * podem reintroduzir selecao configuravel de curvas temporais por DFU.
     */
    protected SplitTemporalProjectionCurva splitTemporalProjectionCurvaBase;

    /**
     * Periodo origem -> periodos target com algum overlap.
     */
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
                if (calendarioTarget.getTamanhoBucket().equals(calendarioOrigem.getTamanhoBucket())) {
                    int posicaoPeriodoCalendarioOrigem = calendarioOrigem.getPosicaoPeriodo(calendarioTarget.getPrimeiraDataHorarioPeriodo(posicaoPeriodoCalendarioTarget));
                    return valorPorPeriodoCalendarioOriginal.applyAsDouble(posicaoPeriodoCalendarioOrigem);
                } else if (calendarioTarget.getTamanhoBucket().getNivelAgregacao() > calendarioOrigem.getTamanhoBucket().getNivelAgregacao()) {
                    return getValorOndeCalendarioTargetMaisAgregadoQueCalendarioOriginal(valorPorPeriodoCalendarioOriginal, posicaoPeriodoCalendarioTarget);
                } else {
                    double valorAcumulado = 0;
                    Map<Integer, Double> mapaParticipacaoPeriodosTargetNoCalendarioOrigem = splitTemporalProjectionCurva.getMapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem().get(posicaoPeriodoCalendarioTarget);
                    if (mapaParticipacaoPeriodosTargetNoCalendarioOrigem == null) {
                        return 0;
                    }
                    for (Integer posicaoPeriodoCalendarioOrigem : mapaParticipacaoPeriodosTargetNoCalendarioOrigem.keySet()) {
                        double valorNoCalendarioOriginal = valorPorPeriodoCalendarioOriginal.applyAsDouble(posicaoPeriodoCalendarioOrigem);
                        valorAcumulado += mapaParticipacaoPeriodosTargetNoCalendarioOrigem.get(posicaoPeriodoCalendarioOrigem) * valorNoCalendarioOriginal;
                    }
                    return valorAcumulado;
                }
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

    public double getValorNoCalendarioTargetSplitTemporal(
            SplitTemporalProjectionCurva splitTemporalProjectionCurva,
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoInicialCalendarioTarget, int posicaoPeriodoFinalCalendarioTarget) throws IncompatibleCalendarException, UnitOfMeasureConversionException {
                double valorAcumulado = 0;
                for (int i = posicaoPeriodoInicialCalendarioTarget; i <= posicaoPeriodoFinalCalendarioTarget; i++) {
                    valorAcumulado += getValorNoCalendarioTargetSplitTemporal(splitTemporalProjectionCurva, valorPorPeriodoCalendarioOriginal, i);
                }
                return valorAcumulado;
            }

    protected double getValorOndeCalendarioTargetMaisAgregadoQueCalendarioOriginal(
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoCalendarioTarget) {
                LocalDateTime dataHorarioInicialPosicaoPeriodoCalendarioTarget = calendarioTarget.getPrimeiraDataHorarioPeriodo(posicaoPeriodoCalendarioTarget);
                LocalDateTime dataHorarioFinalPosicaoPeriodoCalendarioTarget = calendarioTarget.getUltimaDataHorarioPeriodo(posicaoPeriodoCalendarioTarget);
                int posicaoPeriodoInicialCalendarioOriginal = calendarioOrigem.getPosicaoPeriodo(dataHorarioInicialPosicaoPeriodoCalendarioTarget);
                int posicaoPeriodoFinalCalendarioOriginal = calendarioOrigem.getPosicaoPeriodo(dataHorarioFinalPosicaoPeriodoCalendarioTarget);
                double valorAcumulado = 0;
                for (int i = posicaoPeriodoInicialCalendarioOriginal; i <= posicaoPeriodoFinalCalendarioOriginal; i++) {
                    valorAcumulado += valorPorPeriodoCalendarioOriginal.applyAsDouble(i);
                }
                return valorAcumulado;
            }

    private void inicializaMapaSetPeriodosTargetDentroDePeriodoOrigem() {
        for (int periodoTarget = 0; periodoTarget < calendarioTarget.getNumeroPeriodosTotais(); periodoTarget++) {

            LocalDateTime dataHorarioInicialPeriodoTarget = calendarioTarget.getPrimeiraDataHorarioPeriodo(periodoTarget);
            LocalDateTime dataHorarioFinalPeriodoTarget = calendarioTarget.getUltimaDataHorarioPeriodo(periodoTarget);

            for (int periodoOrigem = 0; periodoOrigem < calendarioOrigem.getNumeroPeriodosTotais(); periodoOrigem++) {

                LocalDateTime dataHorarioInicialPeriodoOrigem = calendarioOrigem.getPrimeiraDataHorarioPeriodo(periodoOrigem);
                LocalDateTime dataHorarioFinalPeriodoOrigem = calendarioOrigem.getUltimaDataHorarioPeriodo(periodoOrigem);

                LocalDateTime dataHorarioAtual = dataHorarioInicialPeriodoOrigem.plusDays(0);

                while (dataHorarioAtual.isBefore(dataHorarioFinalPeriodoOrigem.plusSeconds(1))) {
                    if (dataHorarioAtual.isAfter(dataHorarioInicialPeriodoTarget.minusSeconds(1)) && dataHorarioAtual.isBefore(dataHorarioFinalPeriodoTarget.plusSeconds(1))) {
                        // mapa de suporte, sem relação com o cálculo
                        mapaSetPeriodosTargetDentroDePeriodoOrigem
                                .computeIfAbsent(periodoOrigem,
                                        x -> new HashSet<>())
                                .add(periodoTarget);
                    }

                    switch (calendarioTarget.getTamanhoBucket()) {
                        case TURNO:
                            dataHorarioAtual = dataHorarioAtual.plusHours(8);
                            break;
                        case HORARIO:
                            dataHorarioAtual = dataHorarioAtual.plusHours(1);
                            break;
                        case MEIA_HORA:
                            dataHorarioAtual = dataHorarioAtual.plusMinutes(30);
                            break;
                        case QUARTO_HORA:
                            dataHorarioAtual = dataHorarioAtual.plusMinutes(15);
                            break;
                        case SEXTO_HORA:
                            dataHorarioAtual = dataHorarioAtual.plusMinutes(10);
                            break;
                        case MINUTO:
                            dataHorarioAtual = dataHorarioAtual.plusMinutes(1);
                            break;
                        case SEGUNDO:
                            dataHorarioAtual = dataHorarioAtual.plusSeconds(1);
                            break;
                        // se >= dia, somar 1 dia
                        default:
                            dataHorarioAtual = dataHorarioAtual.plusDays(1);
                    }
                }
            }
        }
    }

    
}
