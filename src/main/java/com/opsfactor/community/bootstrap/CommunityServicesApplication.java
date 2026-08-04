package com.opsfactor.community.bootstrap;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

/**
 * Bootstrap auxiliar do modulo services para testes e execucoes locais
 * isoladas da camada web.
 *
 * <p>A aplicacao principal Community continua no modulo web. Este bootstrap
 * apenas materializa o scan de services, model, dto e routines quando um teste
 * ou ferramenta precisa subir a camada de servicos sem servidor HTTP.</p>
 */
@SpringBootApplication
@EnableCaching
@ComponentScan(basePackages = {
        /*
         * A camada de services e usada em testes de integracao sem o bootstrap
         * web completo. O scan explicito inclui o proprio package e as camadas
         * inferiores que fornecem repositories, DTOs e rotinas de calculo.
         */
        "com.opsfactor.community.capability",
        "com.opsfactor.community.platform.service",
        "com.opsfactor.community.platform.model",
        "com.opsfactor.community.platform.dto",
        "com.opsfactor.community.platform.routine"
})
public class CommunityServicesApplication {

    public static void main(String[] args) {

        new SpringApplicationBuilder()
                .sources(CommunityModelApplication.class, CommunityDtoApplication.class,
                        CommunityRoutinesApplication.class, CommunityServicesApplication.class)
                .run(args);

    }

}
