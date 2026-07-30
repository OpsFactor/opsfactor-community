package com.opsfactor.community.platform.database;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registra a persistencia compartilhada pela distribuicao Community.
 *
 * <p>Entidades e repositories acompanham as capabilities donas dos conceitos,
 * portanto nao podem depender do package historico {@code platform.model}.
 * Scheduler e security permanecem transversais e conservam suas raizes
 * explicitas.</p>
 */
@Configuration(proxyBeanMethods = false)
@EntityScan(basePackages = {
        "com.opsfactor.community.capability",
        "com.opsfactor.community.platform.scheduler",
        "com.opsfactor.community.platform.security"
})
@EnableJpaRepositories(basePackages = {
        "com.opsfactor.community.capability",
        "com.opsfactor.community.platform.scheduler.repository",
        "com.opsfactor.community.platform.security"
})
public class CommunityJpaConfiguration {
}
