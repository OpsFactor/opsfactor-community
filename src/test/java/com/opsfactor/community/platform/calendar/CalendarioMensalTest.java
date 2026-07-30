package com.opsfactor.community.platform.calendar;

import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 * Testes de calendario mensal usado por projections e rotinas Community.
 */
public class CalendarioMensalTest {

    @Test
    public void testCalendarioCriadoPorOffsetsPeriodosComPeriodosAdicionais() {
        
        Calendario calendario = Calendario.criaCalendarioDeOffsetsPeriodos(TamanhoBucket.MENSAL,
                LocalDateTime.of(2017, Month.AUGUST, 3, 6, 17), 10, 3, 15, 2);
        
        assertEquals(LocalDateTime.of(2016, Month.JULY, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(0));
        assertEquals(LocalDateTime.of(2016, Month.JULY, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoInicialPassadoAdicional()));
        assertEquals(LocalDateTime.of(2017, Month.APRIL, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoFinalPassadoAdicional()));
        assertEquals(LocalDateTime.of(2017, Month.MAY, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoInicialPassado()));
        assertEquals(LocalDateTime.of(2017, Month.JULY, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoFinalPassado()));
        assertEquals(LocalDateTime.of(2017, Month.AUGUST, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoInicialFuturo()));
        assertEquals(LocalDateTime.of(2018, Month.OCTOBER, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoFinalFuturo()));
        assertEquals(LocalDateTime.of(2018, Month.NOVEMBER, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoInicialFuturoAdicional()));
        assertEquals(LocalDateTime.of(2018, Month.DECEMBER, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoFinalFuturoAdicional()));
        
    }
 
    @Test
    public void testCalendarioCriadoPorOffsetsPeriodosSemPeriodosAdicionais() {
        
        Calendario calendario = Calendario.criaCalendarioDeOffsetsPeriodos(TamanhoBucket.MENSAL,
                LocalDateTime.of(2017, Month.AUGUST, 3, 6, 17), 0, 3, 15, 0);
        
        assertEquals(LocalDateTime.of(2017, Month.MAY, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(0));
        
        assertNull(calendario.getPosicaoPeriodoInicialPassadoAdicional());
        assertNull(calendario.getPosicaoPeriodoInicialFuturoAdicional());
        
        assertEquals(LocalDateTime.of(2017, Month.MAY, 1, 0, 0),
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoInicialPassado()));
        assertEquals(LocalDateTime.of(2017, Month.JULY, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoFinalPassado()));
        assertEquals(LocalDateTime.of(2017, Month.AUGUST, 1, 0, 0), 
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoInicialFuturo()));
        assertEquals(LocalDateTime.of(2018, Month.OCTOBER, 1, 0, 0),
                calendario.getPrimeiraDataHorarioPeriodo(
                        calendario.getPosicaoPeriodoFinalFuturo()));
        
    }
    
    @Test
    public void testGetPosicaoPeriodo() {
        
        Calendario calendario = Calendario.criaCalendarioDeOffsetsPeriodos(TamanhoBucket.MENSAL,
                LocalDateTime.of(2017, Month.AUGUST, 3, 6, 17), 10, 3, 15, 2);
        
        assertEquals((long) calendario.getPosicaoPeriodo(LocalDateTime.of(2017, Month.JANUARY, 6, 15, 5)), 6L);
        
    }

}
