package com.opsfactor.community.platform.database.hibernate.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;

/**
 * Registra as funcoes auxiliares de data para o MariaDB usado no Community.
 *
 * <p>Este contributor e carregado indiretamente pela declaracao de ServiceLoader
 * em {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
 * Enterprise pode reintroduzir contributors para outros bancos quando expuser
 * seus proprios perfis de banco de dados.</p>
 */
public class MariaDBFunctionContributor extends AbstractDateFunctionContributor {

    @Override
    protected boolean supportsDialect(Dialect dialect) {
        return dialect instanceof MariaDBDialect;
    }

    @Override
    protected String getUltimoDiaMesSemHorarioPattern() {
        return "LAST_DAY(?1)";
    }

    @Override
    protected String getDomingoDaSemanaSemHorarioPattern() {
        return "DATE(?1 + INTERVAL (6 - WEEKDAY(?1)) DAY)";
    }

    @Override
    protected String getDataSemHorarioPattern() {
        return "DATE(?1)";
    }
}
