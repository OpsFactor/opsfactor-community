package com.opsfactor.community.capability.cluster.domain;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import org.hibernate.annotations.ColumnDefault;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Protege o DDL de uma instalação Community nova contra defaults booleanos
 * herdados do dialeto anterior, que o PostgreSQL não converte de inteiro.
 */
class ClusterPostgreSqlDefaultTest {

    @Test
    void clusterProductsShouldDeclareThePostgreSqlBooleanDefault()
            throws NoSuchFieldException {

        assertBooleanDefaultIsPostgreSqlLiteral(ClusterProdutos.class);

    }

    @Test
    void clusterLocationsShouldDeclareThePostgreSqlBooleanDefault()
            throws NoSuchFieldException {

        assertBooleanDefaultIsPostgreSqlLiteral(ClusterLocations.class);

    }

    /**
     * Verifica a anotação, que é a fonte usada pelo Hibernate ao gerar o DDL
     * inicial das tabelas de cluster para o runtime PostgreSQL portátil.
     */
    private static void assertBooleanDefaultIsPostgreSqlLiteral(Class<?> clusterType)
            throws NoSuchFieldException {

        Field defaultField = clusterType.getDeclaredField("padrao");
        ColumnDefault columnDefault = defaultField.getAnnotation(ColumnDefault.class);

        assertNotNull(columnDefault, clusterType.getSimpleName() + " must declare a database default.");
        assertEquals("false", columnDefault.value());

    }
}
