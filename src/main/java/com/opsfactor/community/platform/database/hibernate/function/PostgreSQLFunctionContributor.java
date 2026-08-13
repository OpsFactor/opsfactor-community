package com.opsfactor.community.platform.database.hibernate.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

/**
 * Registra as funcoes auxiliares de data para o PostgreSQL portatil do
 * Community.
 *
 * <p>O PostgreSQL usa {@code date_trunc} e o numero ISO do dia da semana. As
 * expressoes retornam {@code date}, preservando o contrato {@code LocalDate}
 * das projections legadas sem duplicar queries JPQL por banco.</p>
 */
public class PostgreSQLFunctionContributor extends AbstractDateFunctionContributor {

    @Override
    protected boolean supportsDialect(Dialect dialect) {

        return dialect instanceof PostgreSQLDialect;

    }

    @Override
    protected String getUltimoDiaMesSemHorarioPattern() {

        return "CAST(date_trunc('month', ?1) + INTERVAL '1 month - 1 day' AS date)";

    }

    @Override
    protected String getDomingoDaSemanaSemHorarioPattern() {

        return "CAST(?1 AS date) + (7 - CAST(EXTRACT(ISODOW FROM ?1) AS integer))";

    }

    @Override
    protected String getDataSemHorarioPattern() {

        return "CAST(?1 AS date)";

    }
}
