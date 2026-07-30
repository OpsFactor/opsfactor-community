package com.opsfactor.community.capability.configuration.facade.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Congela o contrato JSON publico dos parametros material/location.
 *
 * <p>O Community aceita apenas `materialID`, `material` e nomes operacionais
 * em ingles para que o front trabalhe em um contrato unico.</p>
 */
public class ParametrosMaterialLocationDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    public void shouldRejectHistoricalMaterialLocationPayloadFields() {

        for (String historicalFieldName : List.of("produtoID", "produto", "loteMinimo")) {
            Assertions.assertThrows(
                    JsonProcessingException.class,
                    () -> objectMapper.readValue(
                            "{\"" + historicalFieldName + "\":\"historical-value\"}",
                            ParametrosMaterialLocationDTO.class));
        }

    }

    @Test
    public void shouldRoundTripProductionMinimumAndMultipleUsingEnglishFields() throws Exception {

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO = objectMapper.readValue(
                """
                {
                  "locationID": "LOC-01",
                  "materialID": "MAT-01",
                  "productionMinimumQuantity": 12.5,
                  "productionMultipleQuantity": 2.5
                }
                """,
                ParametrosMaterialLocationDTO.class);

        Assertions.assertEquals(12.5d, parametrosMaterialLocationDTO.getProductionMinimumQuantity());
        Assertions.assertEquals(2.5d, parametrosMaterialLocationDTO.getProductionMultipleQuantity());

        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(parametrosMaterialLocationDTO));

        Assertions.assertEquals(12.5d, jsonNode.get("productionMinimumQuantity").asDouble());
        Assertions.assertEquals(2.5d, jsonNode.get("productionMultipleQuantity").asDouble());
        Assertions.assertFalse(jsonNode.has("produtoID"));
        Assertions.assertFalse(jsonNode.has("produto"));
        Assertions.assertFalse(jsonNode.has("loteMinimo"));

    }

    @Test
    public void shouldRoundTripFrozenDemandPlanningHorizonInDays() throws Exception {

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO = objectMapper.readValue(
                """
                {
                  "locationID": "LOC-01",
                  "materialID": "MAT-01",
                  "frozenHorizonDpInDays": 14
                }
                """,
                ParametrosMaterialLocationDTO.class);

        Assertions.assertEquals(14, parametrosMaterialLocationDTO.getFrozenHorizonDpInDays());

        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(parametrosMaterialLocationDTO));

        Assertions.assertEquals(14, jsonNode.get("frozenHorizonDpInDays").asInt());

    }

    @Test
    public void shouldRoundTripConfiguredMaterialLocationUomOverrides() throws Exception {

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO = objectMapper.readValue(
                """
                {
                  "locationID": "LOC-01",
                  "materialID": "MAT-01",
                  "defaultUomId": "UN",
                  "productionMinimumMultipleUomId": "CX"
                }
                """,
                ParametrosMaterialLocationDTO.class);

        Assertions.assertEquals("UN", parametrosMaterialLocationDTO.getDefaultUomId());
        Assertions.assertEquals(
                "CX",
                parametrosMaterialLocationDTO.getProductionMinimumMultipleUomId());

        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(parametrosMaterialLocationDTO));

        Assertions.assertEquals("UN", jsonNode.get("defaultUomId").asText());
        Assertions.assertEquals("CX", jsonNode.get("productionMinimumMultipleUomId").asText());

    }

    @Test
    public void shouldRoundTripMaterialLocationLifecycleFieldsUsingIntegrationFormats() throws Exception {

        ParametrosMaterialLocationDTO parametrosMaterialLocationDTO = objectMapper.readValue(
                """
                {
                  "locationID": "LOC-01",
                  "materialID": "MAT-01",
                  "lifecycleStage": "New",
                  "introductionDate": "2026-01-10T00:00:00",
                  "discontinuationDate": "2026-12-31T00:00:00"
                }
                """,
                ParametrosMaterialLocationDTO.class);

        Assertions.assertEquals(Constantes.StatusProduto.NOVO, parametrosMaterialLocationDTO.getLifecycleStage());
        Assertions.assertEquals(
                LocalDateTime.of(2026, 1, 10, 0, 0),
                parametrosMaterialLocationDTO.getIntroductionDate());
        Assertions.assertEquals(
                LocalDateTime.of(2026, 12, 31, 0, 0),
                parametrosMaterialLocationDTO.getDiscontinuationDate());

        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(parametrosMaterialLocationDTO));

        Assertions.assertEquals("New", jsonNode.get("lifecycleStage").asText());
        Assertions.assertEquals("2026-01-10T00:00:00", jsonNode.get("introductionDate").asText());
        Assertions.assertEquals("2026-12-31T00:00:00", jsonNode.get("discontinuationDate").asText());

    }

    @Test
    public void shouldRejectLifecycleOrDateValuesOutsideIntegrationContract() {

        Assertions.assertThrows(
                JsonProcessingException.class,
                () -> objectMapper.readValue(
                        "{\"lifecycleStage\":\"Unsupported\"}",
                        ParametrosMaterialLocationDTO.class));
        Assertions.assertThrows(
                JsonProcessingException.class,
                () -> objectMapper.readValue(
                        "{\"introductionDate\":\"2026/01/10\"}",
                        ParametrosMaterialLocationDTO.class));

    }

}
