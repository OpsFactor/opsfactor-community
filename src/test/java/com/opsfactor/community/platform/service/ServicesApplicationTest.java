package com.opsfactor.community.platform.service;

import com.opsfactor.community.bootstrap.CommunityServicesApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

/**
 * Contrato do bootstrap auxiliar da camada de services Community.
 */
class ServicesApplicationTest {

    @Test
    void componentScanShouldIncludeServicesAndLowerCommunityLayers() {

        ComponentScan componentScan = CommunityServicesApplication.class.getAnnotation(ComponentScan.class);

        /*
         * ServicesApplication e usado por testes e ferramentas que sobem a
         * camada de services sem community-web. O scan precisa continuar
         * incluindo o proprio modulo e as camadas inferiores de model, dto e
         * routines para que repositories, mappers e processors fiquem visiveis.
         */
        Assertions.assertNotNull(componentScan);
        Assertions.assertEquals(
                List.of(
                        "com.opsfactor.community.capability",
                        "com.opsfactor.community.platform.service",
                        "com.opsfactor.community.platform.model",
                        "com.opsfactor.community.platform.dto",
                        "com.opsfactor.community.platform.routine"),
                List.of(componentScan.basePackages()));

    }

}
