package com.opsfactor.community.platform.bi.facade.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CommunityDemandPlanSalesReportRowDTOTest {

    /**
     * O contrato da futura borda mantém somente a chave DFU/período/UOM e os
     * quatro valores agregados; nenhum detalhe de entidade pode vazar.
     */
    @Test
    void shouldExposeExactlyTheExpectedJsonFields() throws Exception {

        CommunityDemandPlanSalesReportRowDTO reportRow = new CommunityDemandPlanSalesReportRowDTO(
                "PRODUCT-1",
                "LOCATION-1",
                LocalDateTime.of(2026, 7, 31, 23, 59, 59),
                "EA",
                12.5d,
                150.0d,
                120.0d,
                75.0d);

        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(reportRow);

        Set<String> fieldNames = new java.util.HashSet<>();
        json.fieldNames().forEachRemaining(fieldNames::add);
        Assertions.assertEquals(
                Set.of(
                        "productId",
                        "locationId",
                        "periodReferenceDate",
                        "unitOfMeasureId",
                        "quantity",
                        "grossSales",
                        "netSales",
                        "cogs"),
                fieldNames);
        Assertions.assertEquals("PRODUCT-1", json.get("productId").asText());
        Assertions.assertEquals("LOCATION-1", json.get("locationId").asText());
        Assertions.assertEquals("EA", json.get("unitOfMeasureId").asText());
        Assertions.assertEquals(12.5d, json.get("quantity").asDouble());
        Assertions.assertEquals(150.0d, json.get("grossSales").asDouble());
        Assertions.assertEquals(120.0d, json.get("netSales").asDouble());
        Assertions.assertEquals(75.0d, json.get("cogs").asDouble());

    }

    /** A resposta pode atravessar a borda HTTP ou processamento assíncrono. */
    @Test
    void shouldBeSerializable() {

        CommunityDemandPlanSalesReportRowDTO reportRow = new CommunityDemandPlanSalesReportRowDTO(
                "PRODUCT-1",
                "LOCATION-1",
                LocalDateTime.of(2026, 7, 31, 23, 59, 59),
                "EA",
                12.5d,
                150.0d,
                120.0d,
                75.0d);

        Assertions.assertInstanceOf(Serializable.class, reportRow);

    }
}
