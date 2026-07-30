package com.opsfactor.community.platform.utility.fileprocessing;

import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

}
