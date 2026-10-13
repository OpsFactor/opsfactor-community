package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.UnidadeMedidaDataUploadDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.UnidadeMedidaIntegrationAutoMapper;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.fileprocessing.FileProcessing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/** Validates incoming keys while accepting pre-existing identifiers in the snapshot. */
class UnitOfMeasureIdentifierUploadGuardTest {

    @Test
    void jsonMustRejectEmbeddedAndTrailingEnterWithoutWriting() {

        for (String invalidId : List.of("K\nG", "KG\n", "KG\r\n", " KG", "KG ", "=SUM(A1:A2)")) {
            UnidadeMedidaRepository repository = mock(UnidadeMedidaRepository.class);
            when(repository.findAll()).thenReturn(List.of());
            UnidadeMedidaIntegrationService service = new UnidadeMedidaIntegrationService();
            ReflectionTestUtils.setField(service, "unidadeMedidaRepository", repository);

            DataUploadException exception = assertThrows(DataUploadException.class,
                    () -> service.saveDTOList(List.of(UnidadeMedidaDataUploadDTO.builder()
                            .id(invalidId).build())));
            assertTrue(exception.getMessage().contains("Unit of Measure Id"));
            assertTrue(exception.getMessage().contains("line 1"));
            if (invalidId.startsWith("=")) {
                assertTrue(exception.getMessage().contains("Excel formulas are not accepted"));
            }
            verify(repository, never()).saveAll(anyList());
        }

    }

    @Test
    void validImportMustNotRejectDirtyIdsAlreadyInTheDatabase() {

        UnidadeMedidaRepository repository = mock(UnidadeMedidaRepository.class);
        when(repository.findAll()).thenReturn(List.of(new UnidadeMedida("OLD\nID"), new UnidadeMedida("=OLD")));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        UnidadeMedidaIntegrationService service = new UnidadeMedidaIntegrationService();
        ReflectionTestUtils.setField(service, "unidadeMedidaRepository", repository);

        assertEquals("Units of Measure uploaded", service.saveDTOList(List.of(
                UnidadeMedidaDataUploadDTO.builder().id("KG").description("Weight").build())));
        verify(repository).saveAll(anyList());

    }

    @Test
    void unitOfMeasureFileRejectsLiteralAndRealFormulaKeysAtPhysicalRow181() throws Exception {

        UnidadeMedidaIntegrationAutoMapper mapper = mock(
                UnidadeMedidaIntegrationAutoMapper.class, CALLS_REAL_METHODS);
        UnidadeMedidaIntegrationService service = new UnidadeMedidaIntegrationService();
        ReflectionTestUtils.setField(service, "unidadeMedidaDataUploadAutoMapper", mapper);
        for (boolean actualFormula : List.of(false, true)) {
            byte[] workbookBytes;
            try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                var sheet = workbook.createSheet("Upload");
                var header = sheet.createRow(0);
                header.createCell(0).setCellValue("Unit of Measure Id");
                header.createCell(1).setCellValue("Description");
                header.createCell(2).setCellValue("Delete");
                var row = sheet.createRow(180);
                var identifierCell = row.createCell(0);
                if (actualFormula) {
                    identifierCell.setCellFormula("\"KG\"");
                    workbook.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(identifierCell);
                } else {
                    identifierCell.setCellValue("=SUM(A1:A2)");
                }
                row.createCell(1).setCellValue("Weight");
                row.createCell(2).setCellValue("");
                workbook.write(output);
                workbookBytes = output.toByteArray();
            }
            MockMultipartFile upload = new MockMultipartFile("file", "upload.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookBytes);
            DataUploadException exception = assertThrows(DataUploadException.class,
                    () -> service.saveFile(FileProcessing.getProcessedFileFromMultipartFile(upload)));
            assertEquals("Primary key field 'Unit of Measure Id': "
                    + "Excel formulas are not accepted at line 181", exception.getMessage());
        }

    }

    @Test
    void rawFileKeyMustBeCheckedBeforeTheParserTrimsIt() {

        UnidadeMedidaIntegrationAutoMapper mapper = mock(
                UnidadeMedidaIntegrationAutoMapper.class, CALLS_REAL_METHODS);
        assertThrows(DataUploadException.class,
                () -> mapper.converteLinhaArquivoParaDTO(List.of("KG\n", "Weight", "")));
        var dto = mapper.converteLinhaArquivoParaDTO(List.of("KG=UNIT", "=First\nSecond", ""));
        assertEquals("KG=UNIT", dto.id);
        assertEquals("=First\nSecond", dto.description);

    }

}
