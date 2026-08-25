package com.opsfactor.community.capability.configuration.user.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredViewDefaultTest {

    @Test
    void shouldRequireBatchSaveWhenAutoSubmitIsUnset() {

        ConfiguredView configuredView = new ConfiguredView();

        assertFalse(configuredView.getSubmissaoAutomaticaAlteracoes());

    }

    @Test
    void shouldPreserveExplicitAutoSubmitPreference() {

        ConfiguredView configuredView = new ConfiguredView();
        configuredView.setSubmissaoAutomaticaAlteracoes(true);

        assertTrue(configuredView.getSubmissaoAutomaticaAlteracoes());

    }
}
