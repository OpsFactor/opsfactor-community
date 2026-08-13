package com.opsfactor.community.bootstrap;

import com.opsfactor.community.platform.database.CommunityJpaConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

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
@Import(CommunityJpaConfiguration.class)
public class CommunityWebApplication {

    public static void main(String[] args) {

        SpringApplication.run(CommunityWebApplication.class, args);

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
