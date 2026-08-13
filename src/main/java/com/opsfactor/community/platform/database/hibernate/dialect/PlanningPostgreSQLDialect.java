package com.opsfactor.community.platform.database.hibernate.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * Dialeto PostgreSQL usado pela distribuição portátil Community.
 *
 * <p>Enum persistido não deve receber {@code CHECK} gerado automaticamente
 * pelo Hibernate, pois novas versões podem acrescentar valores antes de uma
 * migração física controlada.
 * O quoting global configurado no perfil protege os nomes históricos que são
 * palavras reservadas em PostgreSQL, como a tabela {@code user}.</p>
 */
@SuppressWarnings("unused")
public class PlanningPostgreSQLDialect extends PostgreSQLDialect {

    public PlanningPostgreSQLDialect() {

        super();

    }

    public PlanningPostgreSQLDialect(DatabaseVersion version) {

        super(version);

    }

    public PlanningPostgreSQLDialect(DialectResolutionInfo info) {

        super(info);

    }

    /**
     * Evita validar enum por {@code CHECK} no schema físico.
     */
    @Override
    public boolean supportsColumnCheck() {

        return false;

    }

    /**
     * Evita validar enum por {@code CHECK} no schema físico.
     */
    @Override
    public boolean supportsTableCheck() {

        return false;

    }
}
