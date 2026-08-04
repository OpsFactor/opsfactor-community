package com.opsfactor.community.platform.bi.facade.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/** Congela os nomes JSON consumidos pelo front legado nas séries quantitativas. */
class CommunityProductionOverviewDtoTest {

    @Test
    void shouldPreserveFlattenedLegacySeriesNamesAndQuantityUom() throws Exception {

        CommunitySupplyOverviewBaseDTO.StockAndProductionDTO stockAndProduction =
                new CommunitySupplyOverviewBaseDTO.StockAndProductionDTO(
                        "PLANT", Map.of("materialId", "MAT-1"), "EA", 2);
        stockAndProduction.unconstrainedInventory[0] = 12.0f;
        stockAndProduction.constrainedProduction[1] = 7.0f;

        String json = new ObjectMapper().writeValueAsString(stockAndProduction);

        Assertions.assertTrue(json.contains("\"locationId\":\"PLANT\""));
        Assertions.assertTrue(json.contains("\"quantityUomId\":\"EA\""));
        Assertions.assertTrue(json.contains("\"materialId\":\"MAT-1\""));
        Assertions.assertTrue(json.contains("\"unconstrainedInventory\":[12.0,0.0]"));
        Assertions.assertTrue(json.contains("\"constrainedProduction\":[0.0,7.0]"));

    }
}
