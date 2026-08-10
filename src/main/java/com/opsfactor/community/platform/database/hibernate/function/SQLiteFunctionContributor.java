package com.opsfactor.community.platform.database.hibernate.function;

import org.hibernate.dialect.Dialect;

/**
 * Registra as funcoes auxiliares de data para o dialeto SQLite usado em
 * cenarios locais e testes leves.
 *
 * Este contributor e carregado indiretamente pela declaracao de ServiceLoader
 * em {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
 */
public class SQLiteFunctionContributor extends AbstractDateFunctionContributor {

    private static final String SQLITE_DIALECT = "org.hibernate.community.dialect.SQLiteDialect";

    @Override
    protected boolean supportsDialect(Dialect dialect) {
        return SQLITE_DIALECT.equals(dialect.getClass().getName());
    }

    @Override
    protected String getUltimoDiaMesSemHorarioPattern() {

        /*
         * O SQLiteDialect do Hibernate persiste LocalDateTime como epoch em
         * milissegundos. As funcoes date/datetime do SQLite esperam segundos
         * quando o modificador unixepoch e usado. O resultado volta a epoch
         * em milissegundos para o JDBC materializar LocalDate sem depender do
         * parser textual especifico do driver SQLite.
         */
        return "CAST(strftime('%s', date(?1 / 1000, 'unixepoch', 'start of month','+1 month','-1 day')) AS INTEGER) * 1000";
    }

    @Override
    protected String getDomingoDaSemanaSemHorarioPattern() {

        return "CAST(strftime('%s', date(?1 / 1000, 'unixepoch', 'weekday 0')) AS INTEGER) * 1000";
    }

    @Override
    protected String getDataSemHorarioPattern() {

        return "CAST(strftime('%s', date(?1 / 1000, 'unixepoch')) AS INTEGER) * 1000";
    }
}
