package com.opsfactor.community.platform.database.hibernate.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * Dialeto MariaDB usado pela distribuicao Community.
 *
 * <p>Mantem o comportamento historico da plataforma: enums podem crescer sem
 * depender de {@code CHECK constraints} geradas automaticamente pelo Hibernate
 * no schema do banco.</p>
 */
@SuppressWarnings("unused")
public class PlanningMariaDBDialect extends MariaDBDialect {

    public PlanningMariaDBDialect() {

        super();

    }

    public PlanningMariaDBDialect(DatabaseVersion version) {

        super(version);

    }

    public PlanningMariaDBDialect(DialectResolutionInfo info) {

        super(info);

    }

    /**
     * Impede a criacao de {@code CHECK} na definicao da coluna.
     */
    @Override
    public boolean supportsColumnCheck() {

        return false;

    }

    /**
     * Impede a criacao de {@code CHECK} na definicao da tabela.
     */
    @Override
    public boolean supportsTableCheck() {

        return false;

    }

}
