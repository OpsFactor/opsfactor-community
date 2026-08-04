package com.opsfactor.community.bootstrap;

import com.opsfactor.community.platform.database.CommunityJpaConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

/**
 * Bootstrap Spring Boot do modulo Community de modelo.
 *
 * <p>O contexto habilita cache e serve como ponto isolado para subir beans de
 * model/projection em testes ou execucoes tecnicas desse recorte.</p>
 */
@SpringBootApplication
@EnableCaching
@Import(CommunityJpaConfiguration.class)
public class CommunityModelApplication {

    /**
     * Inicia o contexto Spring do modulo Community de modelo.
     */
    public static void main(String[] args) {

        SpringApplication.run(CommunityModelApplication.class, args);

    }

}
