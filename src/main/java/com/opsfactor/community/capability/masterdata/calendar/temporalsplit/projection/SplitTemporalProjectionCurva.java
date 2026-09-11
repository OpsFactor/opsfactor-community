package com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection;

import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.calendar.PeriodoCalendario;
import java.time.Duration;
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

    protected final Calendario calendarioOrigem; // ex : calendário DP
    protected final Calendario calendarioTarget; // ex : calendário SNP

    // Map<PeriodoTarget -> PeriodoOrigem -> % do periodo origem a considerar
    private Map<Integer,Map<Integer,Double>> mapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem = new HashMap<>();

    // Denominador integral de cada período de origem, reutilizado nos ranges
    // deslocados para evitar recalcular toda a curva em loops do Supply Planning.
    private Map<Integer,Double> mapaParticipacaoPorPeriodoOrigem = new HashMap<>();

    // passo intermediário para a construção da curva acima
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

        // O mapa já representa qualquer interseção: mesma granularidade,
        // consolidação, desagregação ou fronteira técnica parcial.
        double valorAcumulado = 0;

        Map<Integer,Double> mapaParticipacaoPeriodosTargetNoCalendarioOrigem = getMapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem()
                .get(posicaoPeriodoCalendarioTarget);

        // periodo target não tem correspondente no período origem (do projection)
        // portanto se retorna 0 (periodo target fora do horizonte deste projection)
        if (mapaParticipacaoPeriodosTargetNoCalendarioOrigem == null) {
            return 0;
        }

        for (Integer posicaoPeriodoCalendarioOrigem : mapaParticipacaoPeriodosTargetNoCalendarioOrigem.keySet()) {
            double valorNoCalendarioOriginal = valorPorPeriodoCalendarioOriginal.applyAsDouble(posicaoPeriodoCalendarioOrigem);
            valorAcumulado +=
                    mapaParticipacaoPeriodosTargetNoCalendarioOrigem.get(posicaoPeriodoCalendarioOrigem) // quanto % o periodo target representa do periodo origem
                    * valorNoCalendarioOriginal;
        }

        return valorAcumulado;

    }

    public double getValorNoCalendarioTargetSplitTemporalComDesagregacao(
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            int posicaoPeriodoInicialCalendarioTarget, int posicaoPeriodoFinalCalendarioTarget) {

        double valorAcumulado = 0;
        for (int i = posicaoPeriodoInicialCalendarioTarget; i <= posicaoPeriodoFinalCalendarioTarget ; i++) {
            valorAcumulado += getValorNoCalendarioTargetSplitTemporalComDesagregacao(
                    valorPorPeriodoCalendarioOriginal,
                    i);
        }

        return valorAcumulado;

    }

    /**
     * Calcula o valor da série de origem contido num intervalo arbitrário. O
     * denominador de cada parcela continua sendo o período integral de origem;
     * isso permite deslocar as fronteiras de um período de supply por lead time
     * em dias sem converter toda a demanda para uma série diária intermediária.
     */
    public double getValorNoRangeSplitTemporal(
            ToDoubleFunction<Integer> valorPorPeriodoCalendarioOriginal,
            LocalDateTime dataHorarioInicialInclusivo,
            LocalDateTime dataHorarioFinalInclusivo) {

        if (dataHorarioInicialInclusivo.isAfter(dataHorarioFinalInclusivo)) {
            throw new IllegalArgumentException("Intervalo de split temporal invertido");
        }

        int posicaoPeriodoInicialOrigem = Math.max(
                calendarioOrigem.getPosicaoPeriodoInicial(),
                calendarioOrigem.getPosicaoPeriodo(dataHorarioInicialInclusivo));
        int posicaoPeriodoFinalOrigem = Math.min(
                calendarioOrigem.getPosicaoPeriodoInicial() + calendarioOrigem.getNumeroPeriodosTotais() - 1,
                calendarioOrigem.getPosicaoPeriodo(dataHorarioFinalInclusivo));
        if (posicaoPeriodoInicialOrigem > posicaoPeriodoFinalOrigem) {
            return 0;
        }

        double valorAcumulado = 0;
        for (int posicaoPeriodoOrigem = posicaoPeriodoInicialOrigem;
                posicaoPeriodoOrigem <= posicaoPeriodoFinalOrigem;
                posicaoPeriodoOrigem++) {
            PeriodoCalendario periodoCalendarioOrigem = calendarioOrigem.getPeriodo(posicaoPeriodoOrigem);
            LocalDateTime dataHorarioInicialIntersecao = periodoCalendarioOrigem.dataHorarioInicial()
                    .isAfter(dataHorarioInicialInclusivo)
                    ? periodoCalendarioOrigem.dataHorarioInicial()
                    : dataHorarioInicialInclusivo;
            LocalDateTime dataHorarioFinalIntersecao = periodoCalendarioOrigem.dataHorarioFinal()
                    .isBefore(dataHorarioFinalInclusivo)
                    ? periodoCalendarioOrigem.dataHorarioFinal()
                    : dataHorarioFinalInclusivo;
            if (dataHorarioInicialIntersecao.isAfter(dataHorarioFinalIntersecao)) {
                continue;
            }

            double participacaoPeriodoOrigem = mapaParticipacaoPorPeriodoOrigem.getOrDefault(
                    posicaoPeriodoOrigem,
                    0d);
            if (participacaoPeriodoOrigem <= 0) {
                continue;
            }

            double participacaoIntersecao = calculaParticipacaoNoIntervalo(
                    dataHorarioInicialIntersecao,
                    dataHorarioFinalIntersecao);
            valorAcumulado += valorPorPeriodoCalendarioOriginal.applyAsDouble(posicaoPeriodoOrigem)
                    * participacaoIntersecao / participacaoPeriodoOrigem;
        }

        return valorAcumulado;

    }

    /**
     * Atualiza Map<Integer,Map<Integer,Float>> mapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem, o principal mapa
     * usado para o split, a partir de Map<LocalDate,Float> participacaoDataNoAno
     */
    protected void atualizaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem() {

        for (Integer periodoOrigem : calendarioOrigem.getListaPosicoesPeriodo()) {
            PeriodoCalendario periodoCalendarioOrigem = calendarioOrigem.getPeriodo(periodoOrigem);

            // O denominador permanece a participação da janela INTEGRAL de origem.
            // Portanto, se um plano começa no meio de um mês de demanda, apenas a
            // interseção remanescente é levada ao destino; ela não é renormalizada
            // para voltar a representar 100% daquele mês.
            double acumuladoParticipacoesPeriodoOrigem = calculaParticipacaoNoIntervalo(
                    periodoCalendarioOrigem.dataHorarioInicial(),
                    periodoCalendarioOrigem.dataHorarioFinal());
            mapaParticipacaoPorPeriodoOrigem.put(periodoOrigem, acumuladoParticipacoesPeriodoOrigem);

            if (acumuladoParticipacoesPeriodoOrigem <= 0) {
                continue;
            }

            int periodoTargetInicial = Math.max(
                    calendarioTarget.getPosicaoPeriodoInicial(),
                    calendarioTarget.getPosicaoPeriodo(periodoCalendarioOrigem.dataHorarioInicial()));
            int periodoTargetFinal = Math.min(
                    calendarioTarget.getPosicaoPeriodoInicial() + calendarioTarget.getNumeroPeriodosTotais() - 1,
                    calendarioTarget.getPosicaoPeriodo(periodoCalendarioOrigem.dataHorarioFinal()));
            for (int periodoTarget = periodoTargetInicial; periodoTarget <= periodoTargetFinal; periodoTarget++) {
                PeriodoCalendario periodoCalendarioTarget = calendarioTarget.getPeriodo(periodoTarget);
                LocalDateTime dataHorarioInicialIntersecao = periodoCalendarioOrigem.dataHorarioInicial()
                        .isAfter(periodoCalendarioTarget.dataHorarioInicial())
                        ? periodoCalendarioOrigem.dataHorarioInicial()
                        : periodoCalendarioTarget.dataHorarioInicial();
                LocalDateTime dataHorarioFinalIntersecao = periodoCalendarioOrigem.dataHorarioFinal()
                        .isBefore(periodoCalendarioTarget.dataHorarioFinal())
                        ? periodoCalendarioOrigem.dataHorarioFinal()
                        : periodoCalendarioTarget.dataHorarioFinal();

                if (dataHorarioInicialIntersecao.isAfter(dataHorarioFinalIntersecao)) {
                    continue;
                }

                double acumuladoParticipacoesPeriodoTarget = calculaParticipacaoNoIntervalo(
                        dataHorarioInicialIntersecao,
                        dataHorarioFinalIntersecao);
                if (acumuladoParticipacoesPeriodoTarget > 0) {
                    getMapaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem()
                            .computeIfAbsent(periodoTarget, x -> new HashMap<>())
                            .put(periodoOrigem, acumuladoParticipacoesPeriodoTarget / acumuladoParticipacoesPeriodoOrigem);
                }
            }
        }

    }

    /**
     * Integra a curva diária no intervalo inclusivo informado. A fração de cada
     * dia é calculada em segundos, o que preserva o comportamento legado para
     * turnos/horas sem depender de um bucket global do calendário de destino.
     */
    private double calculaParticipacaoNoIntervalo(
            LocalDateTime dataHorarioInicial,
            LocalDateTime dataHorarioFinal) {

        double participacaoAcumulada = 0;
        LocalDate dataAtual = dataHorarioInicial.toLocalDate();
        LocalDate dataFinal = dataHorarioFinal.toLocalDate();

        while (!dataAtual.isAfter(dataFinal)) {
            LocalDateTime dataHorarioInicialDia = dataAtual.atStartOfDay();
            LocalDateTime dataHorarioFinalDia = dataAtual.plusDays(1).atStartOfDay().minusSeconds(1);
            LocalDateTime dataHorarioInicialTrecho = dataHorarioInicial.isAfter(dataHorarioInicialDia)
                    ? dataHorarioInicial
                    : dataHorarioInicialDia;
            LocalDateTime dataHorarioFinalTrecho = dataHorarioFinal.isBefore(dataHorarioFinalDia)
                    ? dataHorarioFinal
                    : dataHorarioFinalDia;

            double numeroSegundosTrecho = Duration.between(
                    dataHorarioInicialTrecho,
                    dataHorarioFinalTrecho).getSeconds() + 1d;
            Double participacaoData = getParticipacaoDataNoAno().get(dataAtual);
            if (participacaoData == null) {
                throw new IllegalStateException("Participação diária ausente na curva para " + dataAtual);
            }
            participacaoAcumulada += participacaoData * numeroSegundosTrecho / 86400d;
            dataAtual = dataAtual.plusDays(1);
        }

        return participacaoAcumulada;

    }

    protected abstract void atualizaParticipacaoDataNoAno();

    protected void inicializaSplitTemporalProjectionCurva() {
        atualizaParticipacaoDataNoAno();
        atualizaDecomposicaoPeriodoTargetComoSomaSplitsPeriodosOrigem();
    }

    
}
