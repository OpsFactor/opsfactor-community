
package com.opsfactor.community.platform.calendar;

import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


/**
 * Contratos basicos do calendario compartilhado pelas rotinas Community.
 */
public class CalendarioTest {

    @Test
    public void testGetPosicaoPeriodoEmOutroCalendario() {
        
        Calendario calendarioMensal = Calendario.criaCalendarioDeOffsetsPeriodos(Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2017, Month.AUGUST, 3, 6, 17), 10, 3, 15, 2);
        Calendario calendarioDiario = Calendario.criaCalendarioDeOffsetsPeriodos(Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2017, Month.JULY, 3, 6, 17), 10, 3, 15, 2);
        
        calendarioDiario.getPosicaoPeriodoDePosicaoPeriodoOutroCalendario(calendarioMensal, 2);
        calendarioMensal.getPosicaoPeriodoDePosicaoPeriodoOutroCalendario(calendarioDiario, 2);
        calendarioMensal.getPosicaoPeriodoDePosicaoPeriodoOutroCalendario(calendarioDiario, 50);
        
    }
    
    @Test
    public void testGetNumeroPeriodosNoBucketReferencia() {
        
        Calendario calendarioMensal = Calendario.criaCalendarioDeOffsetsPeriodos(Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2017, Month.AUGUST, 3, 6, 17), 10, 3, 15, 2);
        Calendario calendarioDiario = Calendario.criaCalendarioDeOffsetsPeriodos(Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2017, Month.JULY, 3, 6, 17), 10, 3, 15, 2);

        assertEquals(31, calendarioMensal.getNumeroPeriodosNoBucketReferencia(3, TamanhoBucket.DIARIO), 0.0001);
        assertEquals(1/30d, calendarioDiario.getNumeroPeriodosNoBucketReferencia(3, TamanhoBucket.MENSAL), 0.0001);
        assertEquals(1/7d, calendarioDiario.getNumeroPeriodosNoBucketReferencia(3, TamanhoBucket.SEMANAL), 0.0001);
        assertEquals(24, calendarioDiario.getNumeroPeriodosNoBucketReferencia(3, TamanhoBucket.HORARIO), 0.0001);
                
    }
    
    @Test
    public void testConsolidaDadosNoCalendario() {
        
        Map<LocalDate, Double> mapaDadosPorData = new HashMap<>();
        
        mapaDadosPorData.put(LocalDate.of(2025, Month.MARCH, 30), 32d);
        mapaDadosPorData.put(LocalDate.of(2025, Month.MARCH, 31), 14d);
        mapaDadosPorData.put(LocalDate.of(2025, Month.APRIL, 1), 93d);
        mapaDadosPorData.put(LocalDate.of(2025, Month.APRIL, 2), 37d);
        mapaDadosPorData.put(LocalDate.of(2025, Month.APRIL, 3), 22d);
        mapaDadosPorData.put(LocalDate.of(2025, Month.APRIL, 4), 15d);
        mapaDadosPorData.put(LocalDate.of(2025, Month.APRIL, 5), 25d);
        mapaDadosPorData.put(LocalDate.of(2025, Month.APRIL, 6), 16d);
        mapaDadosPorData.put(LocalDate.of(2025, Month.APRIL, 7), 54d);
        mapaDadosPorData.put(LocalDate.of(2025, Month.APRIL, 8), 47d);
        
        Calendario calendarioSemanal = Calendario.criaCalendarioDeOffsetsPeriodos(Constantes.TamanhoBucket.SEMANAL,
                LocalDateTime.of(2025, Month.APRIL, 3, 6, 17), 0, 0, 4, 0);
        Calendario calendarioTurno = Calendario.criaCalendarioDeOffsetsPeriodos(Constantes.TamanhoBucket.TURNO,
                LocalDateTime.of(2025, Month.APRIL, 3, 6, 17), 0, 0, 8, 0);

        assertEquals(222, calendarioSemanal.consolidaDadosNoCalendario(0, TamanhoBucket.DIARIO, x -> mapaDadosPorData.get(x.toLocalDate())), 0.0001);
        assertEquals(22d/3, calendarioTurno.consolidaDadosNoCalendario(0, TamanhoBucket.DIARIO, x -> mapaDadosPorData.get(x.toLocalDate())), 0.0001);        
                
    }

    @Test
    public void getNumeroMedioDiasPorPeriodoShouldRejectNullBucketWithCalendarContractMessage() {

        IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> Calendario.getNumeroMedioDiasPorPeriodo(null));

        assertTrue(illegalArgumentException.getMessage().contains(
                "Calendario.getNumeroMedioDiasPorPeriodo does not support calendar bucket null"));

    }

    @Test
    public void getAgregadorPeriodoShouldRejectUnsupportedSubHourlyBucketWithRealBucketName() {

        Calendario calendarioQuartoHora = Calendario.criaCalendarioDeOffsetsPeriodos(
                TamanhoBucket.QUARTO_HORA,
                LocalDateTime.of(2026, Month.JUNE, 24, 9, 15),
                0,
                0,
                1,
                0);

        IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> calendarioQuartoHora.getAgregadorPeriodo(0));

        assertTrue(illegalArgumentException.getMessage().contains(
                "Calendario.getAgregadorPeriodo does not support calendar bucket QUARTO_HORA"));
        assertFalse(illegalArgumentException.getMessage().contains("meia_hora"));

    }

    @Test
    public void getNumeroMedioPeriodosNoAnoShouldRejectSubDailyBucketWithCalendarContractMessage() {

        Calendario calendarioTurno = Calendario.criaCalendarioDeOffsetsPeriodos(
                TamanhoBucket.TURNO,
                LocalDateTime.of(2026, Month.JUNE, 24, 9, 0),
                0,
                0,
                1,
                0);

        IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                calendarioTurno::getNumeroMedioPeriodosNoAno);

        assertTrue(illegalArgumentException.getMessage().contains(
                "Calendario.getNumeroMedioPeriodosNoAno does not support calendar bucket TURNO"));
        assertFalse(illegalArgumentException.getMessage().contains("Not " + "Implemented"));

    }

    @Test
    public void verificaSeLocalDateShouldReturnFalseOnlyForMissingOrInvalidDateText() {

        /*
         * Este predicado e usado em bordas de leitura de dados. Ele nao deve
         * mascarar erros arbitrarios; null e formato invalido sao os dois
         * casos esperados de retorno false.
         */
        assertTrue(Calendario.verificaSeLocalDate("2026-06-25"));
        assertTrue(Calendario.verificaSeLocalDate("25/06/2026"));
        assertFalse(Calendario.verificaSeLocalDate(null));
        assertFalse(Calendario.verificaSeLocalDate("not-a-date"));

    }

    @Test
    public void stringToLocalDateShouldPreserveFinalParseCauseForInvalidText() {

        DateTimeParseException dateTimeParseException = assertThrows(
                DateTimeParseException.class,
                () -> Calendario.stringToLocalDate("not-a-date"));

        assertTrue(dateTimeParseException.getMessage().contains(
                "Incompatible date format : not-a-date"));
        assertTrue(dateTimeParseException.getCause() instanceof DateTimeParseException);

    }

    @Test
    public void stringToLocalDateTimeShouldPreserveFinalParseCauseForInvalidText() {

        DateTimeParseException dateTimeParseException = assertThrows(
                DateTimeParseException.class,
                () -> Calendario.stringToLocalDateTime("not-a-date-time"));

        assertTrue(dateTimeParseException.getMessage().contains(
                "Incompatible date format : not-a-date-time"));
        assertTrue(dateTimeParseException.getCause() instanceof DateTimeParseException);

    }

    @Test
    public void stringToLocalTimeShouldPreserveFinalParseCauseForInvalidText() {

        DateTimeParseException dateTimeParseException = assertThrows(
                DateTimeParseException.class,
                () -> Calendario.stringToLocalTime("not-a-time"));

        assertTrue(dateTimeParseException.getMessage().contains(
                "Incompatible date format : not-a-time"));
        assertTrue(dateTimeParseException.getCause() instanceof DateTimeParseException);

    }
    
    

}

