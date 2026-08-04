package com.opsfactor.community.bootstrap;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

/**
 * Bootstrap auxiliar das rotinas Community para testes e execucoes isoladas.
 *
 * <p>A aplicacao principal continua no modulo web/services; este bootstrap
 * existe para subir o contexto minimo de model + routines quando uma rotina
 * precisa ser exercitada sem controllers.</p>
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        /*
         * O bootstrap isolado precisa encontrar entidades/repositories do model
         * e tambem os processors/engines anotados no proprio modulo routines.
         */
        "com.opsfactor.community.capability",
        "com.opsfactor.community.platform.model",
        "com.opsfactor.community.platform.routine"
})
public class CommunityRoutinesApplication {

    public static void main(String[] args) {

        new SpringApplicationBuilder()
                .sources(CommunityModelApplication.class, CommunityRoutinesApplication.class)
                .run(args);

    }

}
