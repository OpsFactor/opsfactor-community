package com.opsfactor.community.platform.database.hibernate.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Mantém a compatibilidade de evolução de enums entre os dialetos Community.
 */
class PlanningPostgreSQLDialectTest {

    private final PlanningPostgreSQLDialect dialect = new PlanningPostgreSQLDialect();

    @Test
    void naoDeveGerarCheckImplicitamenteParaEnumsPersistidos() {

        assertFalse(dialect.supportsColumnCheck());
        assertFalse(dialect.supportsTableCheck());

    }
}
