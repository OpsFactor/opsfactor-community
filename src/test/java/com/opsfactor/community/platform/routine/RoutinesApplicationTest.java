package com.opsfactor.community.platform.routine;

import com.opsfactor.community.bootstrap.CommunityRoutinesApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

/**
 * Contrato do bootstrap auxiliar das rotinas Community.
 */
class RoutinesApplicationTest {

    @Test
    void componentScanShouldIncludeModelAndRoutinesPackages() {

        ComponentScan componentScan = CommunityRoutinesApplication.class.getAnnotation(ComponentScan.class);

        /*
         * Demand Planning registra processors de limpeza historica e tratamento
         * de stockout como beans do modulo routines. Se o scan isolado apontar
         * apenas para model, testes ou ferramentas que sobem RoutinesApplication
         * deixam de enxergar os componentes do proprio modulo.
         */
        Assertions.assertNotNull(componentScan);
        Assertions.assertEquals(
                List.of(
                        "com.opsfactor.community.capability",
                        "com.opsfactor.community.platform.model",
                        "com.opsfactor.community.platform.routine"),
                List.of(componentScan.basePackages()));

    }

}
