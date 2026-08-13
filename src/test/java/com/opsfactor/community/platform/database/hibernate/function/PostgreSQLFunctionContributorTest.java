package com.opsfactor.community.platform.database.hibernate.function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mantem explícitas as traducoes das funcoes JPQL de calendário para
 * PostgreSQL, sem depender de compatibilidade acidental com outro dialeto.
 */
class PostgreSQLFunctionContributorTest {

    private final PostgreSQLFunctionContributor postgreSQLFunctionContributor =
            new PostgreSQLFunctionContributor();

    @Test
    void deveTraduzirUltimoDiaDoMesParaPostgreSql() {

        assertEquals(
                "CAST(date_trunc('month', ?1) + INTERVAL '1 month - 1 day' AS date)",
                postgreSQLFunctionContributor.getUltimoDiaMesSemHorarioPattern());

    }

    @Test
    void deveTraduzirDomingoDaSemanaParaPostgreSql() {

        assertEquals(
                "CAST(?1 AS date) + (7 - CAST(EXTRACT(ISODOW FROM ?1) AS integer))",
                postgreSQLFunctionContributor.getDomingoDaSemanaSemHorarioPattern());

    }

    @Test
    void deveTraduzirDataSemHorarioParaPostgreSql() {

        assertEquals(
                "CAST(?1 AS date)",
                postgreSQLFunctionContributor.getDataSemHorarioPattern());

    }
}
