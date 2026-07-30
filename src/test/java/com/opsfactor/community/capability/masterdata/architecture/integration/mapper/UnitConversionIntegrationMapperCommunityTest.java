package com.opsfactor.community.capability.masterdata.architecture.integration.mapper;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.ConversaoUnidadeIntegrationMapper;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.ConversaoUnidadeProdutoIntegrationMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Contrato Community dos mappers de conversao de unidade de medida.
 *
 * <p>Conversoes globais e especificas por material sao parte do Community
 * porque sustentam projections, planning books e calculos fisicos. O contrato
 * de arquivo deve continuar pequeno, imutavel e livre de dados Enterprise como
 * custos, precos, embalagens logisticas ou atributos de distribution.</p>
 */
public class UnitConversionIntegrationMapperCommunityTest {

    @Test
    public void globalUnitConversionHeadersShouldExposeOnlyCommunityColumns() {

        ConversaoUnidadeIntegrationMapper conversaoUnidadeIntegrationMapper =
                new ConversaoUnidadeIntegrationMapper();

        List<String> processedFileHeaders = conversaoUnidadeIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertTrue(ConversaoUnidadeIntegrationMapper.class.isAnnotationPresent(Component.class));
        Assertions.assertEquals(List.of(
                "Origin Unit of Measure Id",
                "Target Unit of Measure Id",
                "Origin Quantity",
                "Target Quantity"
        ), processedFileHeaders);
        assertHeadersAreImmutable(processedFileHeaders);
        assertNoEnterpriseUnitConversionHeader(processedFileHeaders);

    }

    @Test
    public void materialUnitConversionHeadersShouldExposeOnlyCommunityColumns() {

        ConversaoUnidadeProdutoIntegrationMapper conversaoUnidadeProdutoIntegrationMapper =
                new ConversaoUnidadeProdutoIntegrationMapper();

        List<String> processedFileHeaders = conversaoUnidadeProdutoIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertTrue(ConversaoUnidadeProdutoIntegrationMapper.class.isAnnotationPresent(Component.class));
        Assertions.assertEquals(List.of(
                "Material Id",
                "Origin Unit of Measure Id",
                "Target Unit of Measure Id",
                "Origin Quantity",
                "Target Quantity"
        ), processedFileHeaders);
        assertHeadersAreImmutable(processedFileHeaders);
        assertNoEnterpriseUnitConversionHeader(processedFileHeaders);

    }

    private static void assertHeadersAreImmutable(List<String> processedFileHeaders) {

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Enterprise Test Header"));

    }

    private static void assertNoEnterpriseUnitConversionHeader(List<String> processedFileHeaders) {

        String joinedHeaders = String.join(" | ", processedFileHeaders).toLowerCase();

        Assertions.assertFalse(joinedHeaders.contains("enterprise"));
        Assertions.assertFalse(joinedHeaders.contains("cost"));
        Assertions.assertFalse(joinedHeaders.contains("price"));
        Assertions.assertFalse(joinedHeaders.contains("logistic"));
        Assertions.assertFalse(joinedHeaders.contains("distribution"));
        Assertions.assertFalse(joinedHeaders.contains("packaging"));

    }

}
