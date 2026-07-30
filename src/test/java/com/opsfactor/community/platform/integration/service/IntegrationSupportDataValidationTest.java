package com.opsfactor.community.platform.integration.service;

import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Contrato do validador comum de support data dos uploads Community.
 *
 * <p>Os services de integracao usam este helper antes dos mappers para
 * diferenciar snapshot estruturalmente quebrado de referencia funcional
 * inexistente em uma linha especifica do arquivo.</p>
 */
class IntegrationSupportDataValidationTest {

    @Test
    void getMapaPorIdObrigatorioShouldReturnDeterministicMapForValidSnapshot() {

        SupportEntity firstSupportEntity = new SupportEntity("A");
        SupportEntity secondSupportEntity = new SupportEntity("B");

        Map<String, SupportEntity> mapaPorId =
                IntegrationSupportDataValidation.getMapaPorIdObrigatorio(
                        List.of(
                                firstSupportEntity,
                                secondSupportEntity),
                        SupportEntity::id,
                        "Support snapshot");

        Assertions.assertEquals(
                List.of("A", "B"),
                mapaPorId.keySet().stream().toList());
        Assertions.assertSame(
                firstSupportEntity,
                mapaPorId.get("A"));
        Assertions.assertSame(
                secondSupportEntity,
                mapaPorId.get("B"));

    }

    @Test
    void getMapaPorIdObrigatorioShouldRejectNullSnapshotBeforeMapper() {

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> IntegrationSupportDataValidation.getMapaPorIdObrigatorio(
                        null,
                        SupportEntity::id,
                        "Support snapshot"));

        Assertions.assertEquals(
                "Support snapshot returned null.",
                dataUploadException.getMessage());

    }

    @Test
    void getMapaPorIdObrigatorioShouldRejectDuplicatedIdBeforeMapper() {

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> IntegrationSupportDataValidation.getMapaPorIdObrigatorio(
                        List.of(
                                new SupportEntity("A"),
                                new SupportEntity("A")),
                        SupportEntity::id,
                        "Support snapshot"));

        Assertions.assertEquals(
                "Support snapshot returned duplicated id A.",
                dataUploadException.getMessage());

    }

    /**
     * Entidade minima de teste para deixar claro que o helper nao depende de
     * JPA, repositories ou classes reais de master data.
     */
    private record SupportEntity(String id) {

    }

}
