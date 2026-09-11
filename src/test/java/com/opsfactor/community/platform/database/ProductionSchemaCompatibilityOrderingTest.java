package com.opsfactor.community.platform.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductionSchemaCompatibilityOrderingTest {

    @ParameterizedTest
    @ValueSource(strings = {"SQLite", "H2", "MySQL"})
    void otherDatabaseEnginesRemainUntouched(String databaseProduct) throws Exception {

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn(databaseProduct);
        new ProductionSchemaCompatibilityInitializer(dataSource).afterPropertiesSet();
        verify(connection, never()).setAutoCommit(false);
        verify(connection, never()).createStatement();
        verify(connection).close();

    }

    @Test
    void entityManagerFactoryMustDependOnCompatibilityBeforeHibernateCanStart() {

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition("entityManagerFactory",
                new RootBeanDefinition(LocalContainerEntityManagerFactoryBean.class));
        CommunityJpaConfiguration.productionSchemaCompatibilityOrdering().postProcessBeanFactory(factory);
        assertThat(factory.getBeanDefinition("entityManagerFactory").getDependsOn())
                .contains("productionSchemaCompatibilityInitializer");

    }
}
