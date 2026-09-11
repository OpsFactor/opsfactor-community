package com.opsfactor.community.platform.calendar;

import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Janela global de cálculo. As duas fronteiras são inclusivas, com precisão de
 * segundos, compatíveis com as datas persistidas dos planos. Uma semana técnica
 * conserva o bucket nominal SEMANAL, mas sua duração é a da janela recortada.
 */
public record PeriodoCalendario(Integer posicaoPeriodo, LocalDateTime dataHorarioInicial,
                               LocalDateTime dataHorarioFinal, TamanhoBucket tamanhoBucketNominal) {

    public PeriodoCalendario {

        if (posicaoPeriodo == null || dataHorarioInicial == null || dataHorarioFinal == null
                || tamanhoBucketNominal == null) {
            throw new IllegalArgumentException("Posição, fronteiras e bucket nominal são obrigatórios no período");
        }
        if (dataHorarioInicial.isAfter(dataHorarioFinal)) {
            throw new IllegalArgumentException("Período com início posterior ao fim");
        }
        if (dataHorarioInicial.getNano() != 0 || dataHorarioFinal.getNano() != 0) {
            throw new IllegalArgumentException("Fronteiras de período devem ter precisão de segundos");
        }

    }

    /** Inclui o último segundo, sem perder um segundo por dia/semana técnica. */
    public double numeroDias() {

        return (Duration.between(dataHorarioInicial, dataHorarioFinal).getSeconds() + 1d) / 86400d;

    }

    /** Lookup de timestamps respeita a precisão de segundos dos dados do plano. */
    public boolean contem(LocalDateTime dataHorario) {

        return !dataHorario.isBefore(dataHorarioInicial) && dataHorario.isBefore(dataHorarioFinal.plusSeconds(1));

    }
}
