package com.opsfactor.community.platform.calendar.dto;

import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/** Grade de leitura para telas; não é receita de cadastro nem integração de dados. */
public record ResolvedCalendarDTO(
        TamanhoBucket bucket,
        boolean mixedBuckets,
        LocalDateTime intervalStartTimestamp,
        LocalDateTime intervalEndTimestamp,
        List<LocalDateTime> periodStartTimestampList,
        List<PeriodDTO> periods) {

    /** Posição global e limites inclusivos, inclusive para uma semana parcial. */
    public record PeriodDTO(int periodIndex, TamanhoBucket bucketSize,
                            LocalDateTime startDateTime, LocalDateTime endDateTime) {
    }

    /**
     * Preserva bucket escalar nas grades uniformes. Na grade mista, null tem
     * significado explícito: não existe bucket único; usar periods, não inferir
     * granularidade pelo primeiro período ou substituir pelo bucket-base.
     */
    public static ResolvedCalendarDTO fromCalendar(Calendario calendar) {

        if (calendar == null) {
            throw new IllegalArgumentException("Resolved calendar is required.");
        }
        List<PeriodDTO> periods = IntStream.range(calendar.getPosicaoPeriodoInicial(),
                        calendar.getPosicaoPeriodoInicial() + calendar.getNumeroPeriodosTotais())
                .mapToObj(calendar::getPeriodo)
                .map(period -> new PeriodDTO(period.posicaoPeriodo(), period.tamanhoBucketNominal(),
                        period.dataHorarioInicial(), period.dataHorarioFinal()))
                .toList();
        if (periods.isEmpty()) {
            throw new IllegalStateException("Resolved calendar requires at least one period.");
        }
        List<TamanhoBucket> buckets = periods.stream().map(PeriodDTO::bucketSize).distinct().toList();
        boolean mixedBuckets = buckets.size() != 1;
        return new ResolvedCalendarDTO(mixedBuckets ? null : buckets.getFirst(), mixedBuckets,
                periods.getFirst().startDateTime(), periods.getLast().endDateTime(),
                periods.stream().map(PeriodDTO::startDateTime).toList(), periods);

    }

}
