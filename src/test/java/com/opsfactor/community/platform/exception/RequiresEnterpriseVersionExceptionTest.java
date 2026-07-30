package com.opsfactor.community.platform.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contrato da excecao usada pelo Community para bloquear capacidades
 * implementadas apenas no OpsFactor Enterprise.
 */
class RequiresEnterpriseVersionExceptionTest {

    @Test
    void exceptionShouldExposeStableErrorCodeAndFeatureMessage() {

        RequiresEnterpriseVersionException requiresEnterpriseVersionException =
                new RequiresEnterpriseVersionException("Supply Planning optimizer");

        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION",
                RequiresEnterpriseVersionException.ERROR_CODE);
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Supply Planning optimizer requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    void exceptionShouldPreserveTechnicalCauseWithoutChangingPublicMessage() {

        IllegalArgumentException illegalArgumentException =
                new IllegalArgumentException("Invalid private option");

        RequiresEnterpriseVersionException requiresEnterpriseVersionException =
                new RequiresEnterpriseVersionException(
                        "Planning Book key figure selection",
                        illegalArgumentException);

        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Planning Book key figure selection requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());
        Assertions.assertSame(
                illegalArgumentException,
                requiresEnterpriseVersionException.getCause());

    }

}
