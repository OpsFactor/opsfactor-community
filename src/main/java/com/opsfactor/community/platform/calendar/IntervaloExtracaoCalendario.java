package com.opsfactor.community.platform.calendar;

import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;

import java.time.LocalDateTime;

/** Faixa inclusiva de uma chamada de extração; preserva a fronteira de cada item. */
public record IntervaloExtracaoCalendario(LocalDateTime dataHorarioInicial,
                                         LocalDateTime dataHorarioFinal, TamanhoBucket tamanhoBucket) {

    public IntervaloExtracaoCalendario {

        if (dataHorarioInicial == null || dataHorarioFinal == null || tamanhoBucket == null) {
            throw new IllegalArgumentException("Fronteiras e bucket são obrigatórios no intervalo de extração");
        }
        if (dataHorarioInicial.isAfter(dataHorarioFinal)) {
            throw new IllegalArgumentException("Intervalo de extração invertido");
        }

    }
}
