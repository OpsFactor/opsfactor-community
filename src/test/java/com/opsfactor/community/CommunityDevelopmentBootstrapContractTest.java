package com.opsfactor.community;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protege o bootstrap local da edição Community contra o fixture legado, que
 * não representa mais todas as colunas do schema migrado.
 */
class CommunityDevelopmentBootstrapContractTest {

    @Test
    void devProfileShouldSelectTheDedicatedCommunitySeed() throws IOException {

        Properties developmentProperties = new Properties();
        try (InputStream developmentPropertiesStream = getClass()
                .getClassLoader()
                .getResourceAsStream("application-dev.properties")) {

            assertTrue(developmentPropertiesStream != null);
            developmentProperties.load(developmentPropertiesStream);
        }

        /*
         * A URI explicita mantem o banco efemero entre as conexoes Hibernate e
         * Hikari. O seed separado evita que o dev carregue o dataset historico.
         */
        assertEquals(
                "jdbc:sqlite:file:opsfactor-community-dev?mode=memory&cache=shared",
                developmentProperties.getProperty("spring.datasource.url"));
        assertEquals(
                "classpath:data-community-dev.sql",
                developmentProperties.getProperty("spring.sql.init.data-locations"));
        assertEquals(
                "individually",
                developmentProperties.getProperty(
                        "spring.jpa.properties.hibernate.hbm2ddl.jdbc_metadata_extraction_strategy"));
    }

    @Test
    void dedicatedCommunitySeedShouldContainOnlyTheMinimalPublicBootstrap() throws IOException {

        String communitySeed;
        try (InputStream communitySeedStream = getClass()
                .getClassLoader()
                .getResourceAsStream("data-community-dev.sql")) {

            assertTrue(communitySeedStream != null);
            communitySeed = new String(communitySeedStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(communitySeed.contains("INSERT INTO user"));
        assertTrue(communitySeed.contains("INSERT INTO user_role"));
        assertTrue(communitySeed.contains("INSERT INTO parametros_globais"));
        assertFalse(communitySeed.contains("cluster_locations_id"));
    }
}
