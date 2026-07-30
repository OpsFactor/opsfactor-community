package com.opsfactor.community.platform.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;

/**
 * Enums auxiliares para regras de calendario e dias especiais.
 */
public abstract class EnumsCalendario {

    public enum TipoDiaSemanaOuFeriado {
        @JsonProperty("Monday") SEGUNDA_FEIRA,
        @JsonProperty("Tuesday") TERÇA_FEIRA,
        @JsonProperty("Wednesday") QUARTA_FEIRA,
        @JsonProperty("Thursday") QUINTA_FEIRA,
        @JsonProperty("Friday") SEXTA_FEIRA,
        @JsonProperty("Saturday") SABADO,
        @JsonProperty("Sunday") DOMINGO,
        @JsonProperty("Holiday") FERIADO;

        public DayOfWeek getDayOfWeekDeTipoDiaSemanaOuFeriado() {
            switch (this) {
                case DOMINGO: return DayOfWeek.SUNDAY;
                case SEGUNDA_FEIRA: return DayOfWeek.MONDAY;
                case TERÇA_FEIRA: return DayOfWeek.TUESDAY;
                case QUARTA_FEIRA: return DayOfWeek.WEDNESDAY;
                case QUINTA_FEIRA: return DayOfWeek.THURSDAY;
                case SEXTA_FEIRA: return DayOfWeek.FRIDAY;
                case SABADO: return DayOfWeek.SATURDAY;
            }
            throw new IllegalStateException(
                    "TipoDiaSemanaOuFeriado " + this + " does not map to a java.time.DayOfWeek");
        }

        public static TipoDiaSemanaOuFeriado getTipoDiaSemanaOuFeriadoDeDayOfWeek(DayOfWeek dayOfWeek) {
            switch (dayOfWeek) {
                case SUNDAY: return DOMINGO;
                case MONDAY: return SEGUNDA_FEIRA;
                case TUESDAY: return TERÇA_FEIRA;
                case WEDNESDAY: return QUARTA_FEIRA;
                case THURSDAY: return QUINTA_FEIRA;
                case FRIDAY: return SEXTA_FEIRA;
                case SATURDAY: return SABADO;
            }
            throw new IllegalStateException(
                    "DayOfWeek " + dayOfWeek + " does not map to TipoDiaSemanaOuFeriado");
        }
    }


}
