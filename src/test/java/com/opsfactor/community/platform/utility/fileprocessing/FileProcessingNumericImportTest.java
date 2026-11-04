package com.opsfactor.community.platform.utility.fileprocessing;

import com.opsfactor.community.platform.exception.DataUploadException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;

/** The real XLSX parser must preserve large finite floating-point values before typed mapping. */
class FileProcessingNumericImportTest {

    @Test
    void numericXlsxValuesOutsideLongRangeRemainDoubleWithoutSaturating() throws IOException {

        double[] values = {1e20d, -1e20d, 0x1p63, Math.nextDown(-0x1p63)};
        ProcessedFile file = read(values);
        for (int index = 0; index < values.length; index++) {
            ProcessedFileRow row = file.getFileRows().get(index + 1);
            assertInstanceOf(Double.class, row.getColumnValue(0));
            assertEquals(values[index], row.getColumnValueAsDouble(0));
            assertEquals((float) values[index], row.getColumnValueAsFloat(0));
            assertThrows(DataUploadException.class, () -> row.getColumnValueAsLong(0));
        }

    }

    @Test
    void numericXlsxIntegralBoundariesAndFractionalValuesUseTheirCorrectTypes() throws IOException {

        ProcessedFile file = read(-0x1p63, Math.nextDown(0x1p63), 123d, 1.9d, 2147483648d);
        assertEquals(Long.MIN_VALUE, file.getFileRows().get(1).getColumnValueAsLong(0));
        assertEquals((long) Math.nextDown(0x1p63), file.getFileRows().get(2).getColumnValueAsLong(0));
        assertInstanceOf(Long.class, file.getFileRows().get(3).getColumnValue(0));
        assertEquals(123, file.getFileRows().get(3).getColumnValueAsInteger(0));
        assertInstanceOf(Double.class, file.getFileRows().get(4).getColumnValue(0));
        assertEquals(1.9d, file.getFileRows().get(4).getColumnValueAsDouble(0));
        assertThrows(DataUploadException.class, () -> file.getFileRows().get(4).getColumnValueAsInteger(0));
        assertThrows(DataUploadException.class, () -> file.getFileRows().get(4).getColumnValueAsLong(0));
        assertEquals(2147483648L, file.getFileRows().get(5).getColumnValueAsLong(0));
        assertThrows(DataUploadException.class, () -> file.getFileRows().get(5).getColumnValueAsInteger(0));

    }

    private static ProcessedFile read(double... values) throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Data");
            sheet.createRow(0).createCell(0).setCellValue("Value");
            for (int index = 0; index < values.length; index++) {
                sheet.createRow(index + 1).createCell(0).setCellValue(values[index]);
            }
            workbook.write(bytes);
            return FileProcessing.getProcessedFileFromMultipartFile(new NumericMultipartFile(bytes.toByteArray()));
        }

    }

    private record NumericMultipartFile(byte[] content) implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return "numeric.xlsx"; }
        @Override public String getContentType() { return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), content); }
    }
}
