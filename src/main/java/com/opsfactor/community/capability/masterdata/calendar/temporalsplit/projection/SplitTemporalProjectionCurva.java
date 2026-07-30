package com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection;

import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Curva de decomposicao temporal entre calendario origem e target.
 *
 * <p>A implementacao Community concreta e a curva flat. Subclasses Enterprise
 * podem preencher participacoes diferentes quando curvas temporais configuradas
 * por DFU forem reabertas.</p>
 */
@Getter
public abstract class SplitTemporalProjectionCurva {

    /**
     * Calendario origem, normalmente Demand Planning.
     */
    protected final Calendario calendarioOrigem;

    /**
     * Calendario target, normalmente Supply Planning.
     */
    protected final Calendario calendarioTarget;

    /**
     * Periodo target -> periodo origem -> percentual do periodo origem a
     * considerar no split.
     */
    private Map<Integer,Map<Integer,Double>> mapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem = new HashMap<>();

    /**
     * Participacao diaria usada como passo intermediario para construir o mapa
     * de decomposicao.
     */
    @Setter
    private Map<LocalDate,Double> participacaoDataNoAno = new HashMap<>();

    // CONSTRUTOR
    public SplitTemporalProjectionCurva(
            Calendario calendarioOrigem,
            Calendario calendarioTarget) {

        this.calendarioOrigem = calendarioOrigem;
        this.calendarioTarget = calendarioTarget;

    }


    public double getValorNoCalendarioTargetSplitTemporalComDesagregacao(
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoCalendarioTarget) {
                //        } else {
                double valorAcumulado = 0;
                Map<Integer, Double> mapaParticipacaoPeriodosTargetNoCalendarioOrigem = getMapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem().get(posicaoPeriodoCalendarioTarget);
                if (mapaParticipacaoPeriodosTargetNoCalendarioOrigem == null) {
                    return 0;
                }
                for (Integer posicaoPeriodoCalendarioOrigem : mapaParticipacaoPeriodosTargetNoCalendarioOrigem.keySet()) {
                    double valorNoCalendarioOriginal = valorPorPeriodoCalendarioOriginal.applyAsDouble(posicaoPeriodoCalendarioOrigem);
                    valorAcumulado += mapaParticipacaoPeriodosTargetNoCalendarioOrigem.get(posicaoPeriodoCalendarioOrigem) * valorNoCalendarioOriginal;
                }
                return valorAcumulado;
            }

    public double getValorNoCalendarioTargetSplitTemporalComDesagregacao(
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoInicialCalendarioTarget, int posicaoPeriodoFinalCalendarioTarget) {
                double valorAcumulado = 0;
                for (int i = posicaoPeriodoInicialCalendarioTarget; i <= posicaoPeriodoFinalCalendarioTarget; i++) {
                    valorAcumulado += getValorNoCalendarioTargetSplitTemporalComDesagregacao(valorPorPeriodoCalendarioOriginal, i);
                }
                return valorAcumulado;
            }

    /**
     * Atualiza Map<Integer,Map<Integer,Float>> mapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem, o principal mapa
     * usado para o split, a partir de Map<LocalDate,Float> participacaoDataNoAno
     */
    protected void atualizaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem() {

        for (int periodoTarget = 0; periodoTarget < calendarioTarget.getNumeroPeriodosTotais(); periodoTarget++) {

            LocalDateTime dataHorarioInicialPeriodoTarget = calendarioTarget.getPrimeiraDataHorarioPeriodo(periodoTarget);
            LocalDateTime dataHorarioFinalPeriodoTarget = calendarioTarget.getUltimaDataHorarioPeriodo(periodoTarget);

            for (int periodoOrigem = 0; periodoOrigem < calendarioOrigem.getNumeroPeriodosTotais(); periodoOrigem++) {

                LocalDateTime dataHorarioInicialPeriodoOrigem = calendarioOrigem.getPrimeiraDataHorarioPeriodo(periodoOrigem);
                LocalDateTime dataHorarioFinalPeriodoOrigem = calendarioOrigem.getUltimaDataHorarioPeriodo(periodoOrigem);

                double acumuladoParticipacoesPeriodoTarget = 0;
                double acumuladoParticipacoesPeriodoOrigem = 0;

                LocalDateTime dataHorarioAtual = dataHorarioInicialPeriodoOrigem.plusDays(0);

                while (dataHorarioAtual.isBefore(dataHorarioFinalPeriodoOrigem.plusSeconds(1))) {

                    double participacaoIncrementalPeriodoOrigem = getParticipacaoDataNoAno().get(dataHorarioAtual.toLocalDate());
                    switch (calendarioTarget.getTamanhoBucket()) {
                        // se < 1 dia : acumular apenas uma fração do valor do dia
                        case TURNO:
                            acumuladoParticipacoesPeriodoOrigem += participacaoIncrementalPeriodoOrigem / 3;
                            break;
                        case HORARIO:
                            acumuladoParticipacoesPeriodoOrigem += participacaoIncrementalPeriodoOrigem / 24;
                            break;
                        case MEIA_HORA:
                            acumuladoParticipacoesPeriodoOrigem += participacaoIncrementalPeriodoOrigem / 48;
                            break;
                        case QUARTO_HORA:
                            acumuladoParticipacoesPeriodoOrigem += participacaoIncrementalPeriodoOrigem / 96;
                            break;
                        case SEXTO_HORA:
                            acumuladoParticipacoesPeriodoOrigem += participacaoIncrementalPeriodoOrigem / 144;
                            break;
                        case MINUTO:
                            acumuladoParticipacoesPeriodoOrigem += participacaoIncrementalPeriodoOrigem / 1440;
                            break;
                        case SEGUNDO:
                            acumuladoParticipacoesPeriodoOrigem += participacaoIncrementalPeriodoOrigem / 86400;
                            break;
                        // se >= dia, lançar valor exato da participação do dia
                        default:
                            acumuladoParticipacoesPeriodoOrigem += participacaoIncrementalPeriodoOrigem;
                    }

                    if (dataHorarioAtual.isAfter(dataHorarioInicialPeriodoTarget.minusSeconds(1)) && dataHorarioAtual.isBefore(dataHorarioFinalPeriodoTarget.plusSeconds(1))) {

                        double participacaoIncrementalPeriodoTarget = getParticipacaoDataNoAno().get(dataHorarioAtual.toLocalDate());
                        switch (calendarioTarget.getTamanhoBucket()) {
                            // se < 1 dia : acumular apenas uma fração do valor do dia
                            case TURNO:
                                acumuladoParticipacoesPeriodoTarget += participacaoIncrementalPeriodoTarget / 3;
                                break;
                            case HORARIO:
                                acumuladoParticipacoesPeriodoTarget += participacaoIncrementalPeriodoTarget / 24;
                                break;
                            case MEIA_HORA:
                                acumuladoParticipacoesPeriodoTarget += participacaoIncrementalPeriodoTarget / 48;
                                break;
                            case QUARTO_HORA:
                                acumuladoParticipacoesPeriodoTarget += participacaoIncrementalPeriodoTarget / 96;
                                break;
                            case SEXTO_HORA:
                                acumuladoParticipacoesPeriodoTarget += participacaoIncrementalPeriodoTarget / 144;
                                break;
                            case MINUTO:
                                acumuladoParticipacoesPeriodoTarget += participacaoIncrementalPeriodoTarget / 1440;
                                break;
                            case SEGUNDO:
                                acumuladoParticipacoesPeriodoTarget += participacaoIncrementalPeriodoTarget / 86400;
                                break;
                            // se >= dia, lançar valor exato da participação do dia
                            default:
                                acumuladoParticipacoesPeriodoTarget += participacaoIncrementalPeriodoTarget;
                        }

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

                if (acumuladoParticipacoesPeriodoTarget > 0 && acumuladoParticipacoesPeriodoOrigem > 0) {
                    getMapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem()
                            .computeIfAbsent(periodoTarget, x -> new HashMap<>())
                            .put(periodoOrigem, acumuladoParticipacoesPeriodoTarget / acumuladoParticipacoesPeriodoOrigem);
                }

            }
        }

    }

    protected abstract void atualizaParticipacaoDataNoAno();

    protected void inicializaSplitTemporalProjectionCurva() {
        atualizaParticipacaoDataNoAno();
        atualizaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem();
    }

    
}
