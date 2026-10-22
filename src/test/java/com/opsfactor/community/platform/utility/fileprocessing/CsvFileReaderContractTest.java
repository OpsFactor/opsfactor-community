package com.opsfactor.community.platform.utility.fileprocessing;

import com.opsfactor.community.platform.exception.DataUploadException;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Exported CSV must be reusable as input without corrupting identifiers or numeric values. */
class CsvFileReaderContractTest {

    @Test
    void commaExportKeepsFiveUnitIdentifiersInsteadOfCreatingConcatenatedIds() throws IOException {
        ProcessedFile file = read("Id,Description\r\nB64,Base64\r\nCXS,Caixa\r\nFDO,Fardo\r\nTON,Tonelada\r\nUN,Unidade\r\n");
        assertEquals(6, file.getFileRows().size());
        assertEquals(List.of("Id", "Description"), row(file, 0));
        assertEquals(List.of("B64", "CXS", "FDO", "TON", "UN"),
                file.getFileRows().subList(1, 6).stream().map(r -> r.getColumnValueAsString(0)).toList());
        assertEquals(List.of("CXS", "Caixa"), row(file, 2));
        assertEquals("sample.csv", file.getName());
    }

    @Test
    void readsLegacySemicolonAndPreservesDecimalCommaUntilTypedAccess() throws IOException {
        ProcessedFile file = read("Material;Quantity;Description\n000123;3,912;Bobina, origem A\n");
        assertEquals(List.of("000123", "3,912", "Bobina, origem A"), row(file, 1));
        assertEquals(3.912, file.getFileRows().get(1).getColumnValueAsDouble(1));
        assertEquals(3.912f, file.getFileRows().get(1).getColumnValueAsFloat(1));
        assertEquals("000123", file.getFileRows().get(1).getColumnValueAsString(0));
    }

    @Test
    void readsStandardAndQuotedCommaDecimalsWithoutInferringGrouping() throws IOException {
        ProcessedFile file = read("Material,Quantity\n000123,3.912\n000124,\"3,912\"\n000125,\"-1,25e2\"\n");
        assertEquals(3.912, file.getFileRows().get(1).getColumnValueAsDouble(1));
        assertEquals(3.912f, file.getFileRows().get(1).getColumnValueAsFloat(1));
        assertEquals(3.912, file.getFileRows().get(2).getColumnValueAsDouble(1));
        assertEquals(-125.0, file.getFileRows().get(3).getColumnValueAsDouble(1));
        ProcessedFileRow invalid = new ProcessedFileRow(List.of("1,234.56", "1,2,3"));
        assertThrows(DataUploadException.class, () -> invalid.getColumnValueAsDouble(0));
        assertThrows(DataUploadException.class, () -> invalid.getColumnValueAsDouble(1));
    }

    @Test
    void preservesQuotedDelimitersEscapedQuotesAndTrailingEmptyColumns() throws IOException {
        ProcessedFile file = read("Id,Description,Optional,Last\r\n000123,\"Bobina; \"\"origem A\"\", branca\",,\r\n");
        assertEquals(List.of("000123", "Bobina; \"origem A\", branca", "", ""), row(file, 1));
        assertEquals(4, file.getFileRows().get(1).row().size());
    }

    @Test
    void utf8BomAndQuotedHeaderPunctuationDoNotChangeDelimiterDetection() throws IOException {
        ProcessedFile file = read("\uFEFF\"Id;code\",Description\n000123,Ação\n");
        assertEquals(List.of("Id;code", "Description"), row(file, 0));
        assertEquals(List.of("000123", "Ação"), row(file, 1));
    }

    @Test
    void quotedMultilineCellsKeepPhysicalSourceRowNumbers() throws IOException {
        ProcessedFile file = read("Id,Description\r\n000123,\"First\r\nsecond\"\r\n000124,Next\r\n");
        assertEquals(3, file.getFileRows().size());
        assertEquals("First\r\nsecond", file.getFileRows().get(1).getColumnValueAsString(1));
        assertEquals(1, file.getFileRows().get(0).sourceRowNumber());
        assertEquals(2, file.getFileRows().get(1).sourceRowNumber());
        assertEquals(4, file.getFileRows().get(2).sourceRowNumber());
    }

    @Test
    void oneColumnLegacyFileDoesNotSplitCommaDecimalData() throws IOException {
        ProcessedFile file = read("Quantity\n3,912\n");
        assertEquals(List.of("3,912"), row(file, 1));
        assertEquals(3.912, file.getFileRows().get(1).getColumnValueAsDouble(0));
    }

    @Test
    void emptyFileAndBlankRowsArePreservedWithoutInventingCells() throws IOException {
        assertTrue(read("").getFileRows().isEmpty());
        ProcessedFile file = read("Id;Description\n\n000123;\n");
        assertEquals(3, file.getFileRows().size());
        assertEquals(List.of(""), row(file, 1));
        assertEquals(List.of("000123", ""), row(file, 2));
        assertEquals(3, file.getFileRows().get(2).sourceRowNumber());
    }

    private static ProcessedFile read(String csv) throws IOException {
        return FileProcessing.getProcessedFileFromMultipartFile(
                new CsvMultipartFile(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<String> row(ProcessedFile file, int index) {
        return file.getFileRowAsStringListWithEmptyFieldsAsEmptyStrings(index);
    }

    private record CsvMultipartFile(byte[] content) implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return "sample.csv"; }
        @Override public String getContentType() { return "text/csv"; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), content); }
    }
}
