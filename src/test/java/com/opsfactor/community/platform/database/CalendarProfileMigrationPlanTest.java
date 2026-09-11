package com.opsfactor.community.platform.database;

import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CalendarProfileMigrationPlanTest {

    @Test
    void demandExplicitoPodeMigrarSemTrocarBucketOuHorizonte() {

        var resultado = CalendarProfileMigrationPlan.planejarDemand(TamanhoBucket.SEMANAL, 18);
        assertTrue(resultado.podeMigrar());
        assertEquals(TamanhoBucket.SEMANAL, resultado.bucketSize());
        assertEquals(18, resultado.numberOfBasePeriods());

    }

    @Test
    void demandNuloNaoViraUmMesPorFallback() {

        var resultado = CalendarProfileMigrationPlan.planejarDemand(TamanhoBucket.MENSAL, null);
        assertFalse(resultado.podeMigrar());
        assertTrue(resultado.pendencia().contains("defaults históricos divergentes"));
        assertFalse(CalendarProfileMigrationPlan.planejarDemand(null, 12).podeMigrar());
        assertFalse(CalendarProfileMigrationPlan.planejarDemand(TamanhoBucket.MENSAL, 0).podeMigrar());

    }

    @Test
    void supplyHistoricoRepeteGradeDoResolvedorExistente() {

        LocalDateTime inicio = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 6, 30, 23, 59, 59);
        var resultado = CalendarProfileMigrationPlan.planejarSupplyPlan(TamanhoBucket.MENSAL, inicio, fim);
        assertTrue(resultado.podeMigrar());
        assertEquals(4, resultado.numberOfBasePeriods());
        var historico = CalendarioSimples.criaCalendarioPeriodosFuturosDeDatas(TamanhoBucket.MENSAL, inicio, fim);
        var migrado = CalendarioSimples.criaCalendarioDeOffsetsPeriodos(
                resultado.bucketSize(), inicio, 0, 0, resultado.numberOfBasePeriods(), 0);
        for (int periodo = 0; periodo < resultado.numberOfBasePeriods(); periodo++) {
            assertEquals(historico.getPrimeiraDataHorarioPeriodo(periodo), migrado.getPrimeiraDataHorarioPeriodo(periodo));
            assertEquals(historico.getUltimaDataHorarioPeriodo(periodo), migrado.getUltimaDataHorarioPeriodo(periodo));
        }

    }

    @Test
    void supplySemBucketDatasOuComDatasInvertidasFicaPendente() {

        LocalDateTime inicio = LocalDateTime.of(2026, 3, 1, 0, 0);
        assertFalse(CalendarProfileMigrationPlan.planejarSupplyPlan(null, inicio, inicio).podeMigrar());
        assertFalse(CalendarProfileMigrationPlan.planejarSupplyPlan(TamanhoBucket.MENSAL, inicio, null).podeMigrar());
        assertFalse(CalendarProfileMigrationPlan.planejarSupplyPlan(
                TamanhoBucket.MENSAL, inicio, inicio.minusDays(1)).podeMigrar());

    }

}
