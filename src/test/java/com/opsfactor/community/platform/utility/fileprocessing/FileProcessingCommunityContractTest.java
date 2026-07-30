package com.opsfactor.community.platform.utility.fileprocessing;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Contratos de entrada do utilitario de leitura de arquivos enviados por data
 * upload.
 *
 * <p>A validacao completa de formatos fica nos controllers/mappers de
 * integracao; aqui protegemos apenas a fronteira comum que transforma um
 * {@code MultipartFile} em {@link ProcessedFile}.</p>
 */
class FileProcessingCommunityContractTest {

    @Test
    void getProcessedFileFromMultipartFileShouldRejectMissingPayload() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FileProcessing.getProcessedFileFromMultipartFile(null));

        Assertions.assertEquals(
                "Multipart file payload is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    void getProcessedFileFromMultipartFileShouldReadCsvWithSemicolonSeparator() throws IOException {

        ProcessedFile processedFile = FileProcessing.getProcessedFileFromMultipartFile(
                new MultipartFileTeste(
                        "sample.csv",
                        "Material;Location\nMAT01;LOC01".getBytes(StandardCharsets.UTF_8)));

        Assertions.assertEquals("sample.csv", processedFile.getName());
        Assertions.assertEquals(
                List.of("Material", "Location"),
                processedFile.getFileRowAsStringListWithEmptyFieldsAsEmptyStrings(0));
        Assertions.assertEquals(
                List.of("MAT01", "LOC01"),
                processedFile.getFileRowAsStringListWithEmptyFieldsAsEmptyStrings(1));

    }

    @Test
    void getProcessedFileFromMultipartFileShouldRejectMissingOriginalFilename() {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FileProcessing.getProcessedFileFromMultipartFile(
                        new MultipartFileTeste(null, new byte[0])));

        Assertions.assertEquals(
                "Multipart file original filename is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    void getProcessedFileFromMultipartFileShouldRejectXlsxWithoutHeaderRow() throws IOException {

        IOException ioException = Assertions.assertThrows(
                IOException.class,
                () -> FileProcessing.getProcessedFileFromMultipartFile(
                        new MultipartFileTeste("empty.xlsx", criaPlanilhaSemLinhaCabecalho())));

        Assertions.assertEquals(
                "First row on first sheet (Empty) is empty",
                ioException.getMessage());

    }

    private static byte[] criaPlanilhaSemLinhaCabecalho() throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            workbook.createSheet("Empty");
            workbook.write(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }

    }

    /**
     * MultipartFile minimo para testar a fronteira do utilitario sem adicionar
     * dependencia de spring-test ao modulo support.
     */
    private record MultipartFileTeste(String originalFilename, byte[] content) implements MultipartFile {

        @Override
        public String getName() {

            return "file";

        }

        @Override
        public String getOriginalFilename() {

            return originalFilename;

        }

        @Override
        public String getContentType() {

            return null;

        }

        @Override
        public boolean isEmpty() {

            return content.length == 0;

        }

        @Override
        public long getSize() {

            return content.length;

        }

        @Override
        public byte[] getBytes() {

            return content;

        }

        @Override
        public InputStream getInputStream() {

            return new ByteArrayInputStream(content);

        }

        @Override
        public void transferTo(File dest) throws IOException {

            Files.write(dest.toPath(), content);

        }

    }

}
