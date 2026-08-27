package com.opsfactor.community.platform.utility.fileprocessing;

import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Contratos do arquivo processado usado por data upload.
 *
 * <p>A classe agrega contexto operacional a erros de conversao linha-a-linha;
 * por isso os testes validam a traducao para {@link DataUploadException} com
 * numero da linha preservado.</p>
 */
class ProcessedFileCommunityContractTest {

    @Test
    void getDTOListFromProcessedFileShouldTranslateMapperFailureToDataUploadExceptionWithLineContext() {

        ProcessedFile processedFile = new ProcessedFile();
        processedFile.setName("test-upload.csv");
        processedFile.addNewRow();
        processedFile.addToLastRow("material_id");
        processedFile.addNewRow();
        processedFile.addToLastRow("MAT-001");
        IllegalStateException originalException = new IllegalStateException("Invalid material");

        DataUploadException dataUploadException = Assertions.assertThrows(
                DataUploadException.class,
                () -> processedFile.getDTOListFromProcessedFile(
                        1,
                        1,
                        processedFileRow -> {
                            throw originalException;
                        }));

        Assertions.assertEquals("Invalid material at line 1", dataUploadException.getMessage());
        Assertions.assertSame(originalException, dataUploadException.getCause());

    }

    @Test
    void removeColumnShouldRemoveHeaderAndValuesFromEveryRow() {

        ProcessedFile processedFile = new ProcessedFile(List.of(
                new SensitivityExportRow("LOC-1", false),
                new SensitivityExportRow("LOC-2", true)));

        processedFile.removeColumn("closestToCurrentSafetyStockDays");

        Assertions.assertEquals(
                List.of("locationId"),
                processedFile.getFileRowAsObjectList(0));
        Assertions.assertEquals(List.of("LOC-1"), processedFile.getFileRowAsObjectList(1));
        Assertions.assertEquals(List.of("LOC-2"), processedFile.getFileRowAsObjectList(2));

    }

    private static class SensitivityExportRow {

        private final String locationId;
        private final Boolean closestToCurrentSafetyStockDays;

        private SensitivityExportRow(String locationId, Boolean closestToCurrentSafetyStockDays) {

            this.locationId = locationId;
            this.closestToCurrentSafetyStockDays = closestToCurrentSafetyStockDays;
        }
    }

}
