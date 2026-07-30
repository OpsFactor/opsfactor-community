package com.opsfactor.community.platform.database.hibernate.function;

import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato das funcoes JPQL de data quando a aplicacao usa um dialeto MySQL.
 */
class MySQLFunctionContributorTest {

    @Test
    void shouldSupportMySQLButLeaveMariaDBToItsDedicatedContributor() {

        MySQLFunctionContributor contributor = new MySQLFunctionContributor();

        Assertions.assertTrue(contributor.supportsDialect(new MySQLDialect()));
        Assertions.assertFalse(contributor.supportsDialect(new MariaDBDialect()));
    }

    @Test
    void shouldTranslateAllDateBucketsToMySQLFunctions() {

        MySQLFunctionContributor contributor = new MySQLFunctionContributor();

        Assertions.assertEquals("LAST_DAY(?1)", contributor.getUltimoDiaMesSemHorarioPattern());
        Assertions.assertEquals(
                "DATE(?1 + INTERVAL (6 - WEEKDAY(?1)) DAY)",
                contributor.getDomingoDaSemanaSemHorarioPattern());
        Assertions.assertEquals("DATE(?1)", contributor.getDataSemHorarioPattern());
    }
}
