package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.VersaoMalhaIntegrationDataDto;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.FileProcessing;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Exercises the actual published workbook through the same XLSX reader and mapper as upload. */
class VersaoMalhaOfficialWorkbookTest {

    @Test
    void readsWorkbookWithPhysicallyMissingOptionalTrailingCells() throws Exception {

        try (var input = getClass().getResourceAsStream("/supply-network-version-trailing-empty.xlsx")) {
            var upload = new MockMultipartFile("file", "network.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", input);
            var rows = new VersaoMalhaIntegrationMapper().convertProcessedFileToDTOList(
                    FileProcessing.getProcessedFileFromMultipartFile(upload), null);
            assertEquals(1, rows.size());
            var dto = rows.getFirst();
            assertEquals("MALHA_PADRAO_V2", dto.primaryKeyDto.supplyNetworkVersionId);
            assertNull(dto.defaultClientOriginLocationId);
            assertNull(dto.defaultRawMaterialOriginLocationId);
            assertNull(dto.defaultRawMaterialOriginLeadTimeDays);
            assertNull(dto.delete);
        }

    }

    @Test
    void trailingOptionalCellsDoNotTurnALocationNamedDIntoDelete() {

        var row = new ProcessedFileRow(Arrays.asList("NET", "Network", null, "D"));
        var dto = new VersaoMalhaIntegrationMapper().convertProcessedFileRowToDTO(row, null);
        assertEquals("D", dto.defaultRawMaterialOriginLocationId);
        assertNull(dto.defaultRawMaterialOriginLeadTimeDays);
        assertNull(dto.delete);

    }

    @Test
    void missingNetworkIdStillFailsRequiredKeyValidation() {

        var mapper = new VersaoMalhaIntegrationMapper();
        VersaoMalhaIntegrationDataDto dto = mapper.convertProcessedFileRowToDTO(
                new ProcessedFileRow(Arrays.asList(null, "Network")), null);
        assertThrows(DataUploadException.class, () -> mapper.convertDTOToEntity(dto, Map.of(), null, null));

    }
}
