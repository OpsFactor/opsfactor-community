package com.opsfactor.community.platform.database.hibernate.function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Garante que as funcoes de calendario SQLite interpretem o formato fisico
 * usado pelo SQLiteDialect do Hibernate para valores LocalDateTime.
 */
class SQLiteFunctionContributorTest {

    private final SQLiteFunctionContributor sqliteFunctionContributor =
            new SQLiteFunctionContributor();

    @Test
    void deveConverterEpochMillisParaUltimoDiaDoMes() {

        assertEquals(
                "CAST(strftime('%s', date(?1 / 1000, 'unixepoch', 'start of month','+1 month','-1 day')) AS INTEGER) * 1000",
                sqliteFunctionContributor.getUltimoDiaMesSemHorarioPattern());

    }

    @Test
    void deveConverterEpochMillisParaDomingoDaSemana() {

        assertEquals(
                "CAST(strftime('%s', date(?1 / 1000, 'unixepoch', 'weekday 0')) AS INTEGER) * 1000",
                sqliteFunctionContributor.getDomingoDaSemanaSemHorarioPattern());

    }

    @Test
    void deveConverterEpochMillisParaDataSemHorario() {

        assertEquals(
                "CAST(strftime('%s', date(?1 / 1000, 'unixepoch')) AS INTEGER) * 1000",
                sqliteFunctionContributor.getDataSemHorarioPattern());

    }
}
