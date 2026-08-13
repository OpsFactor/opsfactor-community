package com.opsfactor.community.platform.scheduler.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Mantém a persistência JSON da task delegada ao dialeto ativo.
 *
 * <p>O PostgreSQL precisa de {@code jsonb} para que consultas com
 * {@code DISTINCT} sobre a task sejam válidas. A escolha cabe ao Hibernate,
 * não a um tipo SQL literal da entidade.</p>
 */
class ScheduledTaskAbstractPersistenceMappingTest {

    @Test
    void deveDelegarTipoJsonAoDialetoHibernate() throws NoSuchFieldException {

        Field configuracoesExecucaoJson = ScheduledTaskAbstract.class
                .getDeclaredField("configuracoesExecucaoJson");
        JdbcTypeCode jdbcTypeCode = configuracoesExecucaoJson.getAnnotation(JdbcTypeCode.class);

        assertNotNull(jdbcTypeCode);
        assertEquals(SqlTypes.JSON, jdbcTypeCode.value());

    }
}
