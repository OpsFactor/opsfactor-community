package com.opsfactor.community.platform.database.hibernate.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

/**
 * Registra as funcoes auxiliares de data para os testes automatizados com H2.
 *
 * Este contributor e carregado indiretamente pela declaracao de ServiceLoader
 * em {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
 */
public class H2FunctionContributor extends AbstractDateFunctionContributor {

    @Override
    protected boolean supportsDialect(Dialect dialect) {
        return dialect instanceof H2Dialect;
    }

    @Override
    protected String getUltimoDiaMesSemHorarioPattern() {
        return "CAST(DATEADD(DAY, -DAY(DATEADD(MONTH,1,?1)), DATEADD(MONTH,1,?1)) AS DATE)";
    }

    @Override
    protected String getDomingoDaSemanaSemHorarioPattern() {
        return "CAST(DATEADD(DAY, 7 - ISO_DAY_OF_WEEK(?1), ?1) AS DATE)";
    }

    @Override
    protected String getDataSemHorarioPattern() {
        return "CAST(?1 AS DATE)";
    }
}
