package com.opsfactor.community.bootstrap;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * Executavel web/API da distribuicao Community.
 *
 * <p>Este bootstrap publica apenas o backend. O front Community/Enterprise roda
 * em repositorios/servidores separados e nao deve voltar a ser empacotado como
 * templates ou assets dentro deste modulo.</p>
 *
 * <p>A fronteira Community/Enterprise fica nos packages. Por isso o scan deste
 * executavel permanece limitado a {@code com.opsfactor.community}; o Enterprise
 * possui bootstrap web proprio para adicionar os packages privados sem
 * reabrir uma varredura raiz em {@code com.opsfactor}.</p>
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.opsfactor.community.capability",
        "com.opsfactor.community.platform.model",
        "com.opsfactor.community.platform.scheduler",
        "com.opsfactor.community.platform.dto",
        "com.opsfactor.community.platform.routine",
        "com.opsfactor.community.platform.service",
        "com.opsfactor.community.platform.bi",
        "com.opsfactor.community.platform.cache",
        "com.opsfactor.community.platform.runtime",
        "com.opsfactor.community.web",
        "com.opsfactor.community.platform.security"
})
@EnableCaching
public class CommunityWebApplication {

    public static void main(String[] args) {

        new SpringApplicationBuilder()
                .sources(CommunityModelApplication.class, CommunitySchedulerApplication.class,
                        CommunityDtoApplication.class, CommunityServicesApplication.class,
                        CommunityRoutinesApplication.class, CommunityWebApplication.class)
                .run(args);

    }

    /**
     * Mantem JVM, Hibernate e calendario operacional alinhados ao timezone
     * padrao documentado nos defaults Community/Enterprise.
     */
    @PostConstruct
    void started() {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    }

}
