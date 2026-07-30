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
        return "date(?1, 'start of month','+1 month','-1 day')";
    }

    @Override
    protected String getDomingoDaSemanaSemHorarioPattern() {
        return "date(?1,'weekday 0')";
    }

    @Override
    protected String getDataSemHorarioPattern() {
        return "DATE(?1)";
    }
}
