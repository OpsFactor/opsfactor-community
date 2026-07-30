package com.opsfactor.community.platform.database.hibernate.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;

/**
 * Registra as funcoes auxiliares de data para dialetos MySQL.
 *
 * <p>O Enterprise usa {@code PlanningMySQLDialect}, que herda de
 * {@link MySQLDialect}. MariaDB permanece atendido pelo contributor proprio,
 * pois tambem herda de {@code MySQLDialect} no Hibernate 6.</p>
 */
public class MySQLFunctionContributor extends AbstractDateFunctionContributor {

    @Override
    protected boolean supportsDialect(Dialect dialect) {

        return dialect instanceof MySQLDialect && !(dialect instanceof MariaDBDialect);
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
