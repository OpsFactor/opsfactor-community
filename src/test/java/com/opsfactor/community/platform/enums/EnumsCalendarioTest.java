package com.opsfactor.community.platform.enums;

import java.time.DayOfWeek;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testes dos enums de calendario compartilhados por rotinas Community.
 */
public class EnumsCalendarioTest {

    @Test
    public void tipoDiaSemanaOuFeriadoShouldRoundTripWeekDays() {

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            EnumsCalendario.TipoDiaSemanaOuFeriado tipoDiaSemanaOuFeriado =
                    EnumsCalendario.TipoDiaSemanaOuFeriado.getTipoDiaSemanaOuFeriadoDeDayOfWeek(dayOfWeek);

            Assertions.assertEquals(
                    dayOfWeek,
                    tipoDiaSemanaOuFeriado.getDayOfWeekDeTipoDiaSemanaOuFeriado());
        }

    }

    @Test
    public void tipoDiaSemanaOuFeriadoShouldRejectHolidayWhenDayOfWeekIsRequired() {

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                EnumsCalendario.TipoDiaSemanaOuFeriado.FERIADO::getDayOfWeekDeTipoDiaSemanaOuFeriado);

        Assertions.assertEquals(
                "TipoDiaSemanaOuFeriado FERIADO does not map to a java.time.DayOfWeek",
                illegalStateException.getMessage());

    }

}
