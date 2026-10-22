package com.opsfactor.community.platform.utility.fileprocessing;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/** Reads exported comma CSV and legacy semicolon CSV without changing cell values. */
final class CsvFileReader {

    private CsvFileReader() {
    }

    static ProcessedFile read(MultipartFile multipartFile) throws IOException {
        ProcessedFile result = new ProcessedFile();
        result.setName(multipartFile.getOriginalFilename());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8))) {
            reader.mark(1);
            if (reader.read() != '\uFEFF') {
                reader.reset();
            }
            Header header = readHeader(reader);
            CSVFormat format = CSVFormat.RFC4180.builder()
                    .setDelimiter(header.delimiter())
                    .setIgnoreEmptyLines(false)
                    .get();
            try (CSVParser parser = format.parse(prepend(header.content(), reader))) {
                long sourceRow = 1;
                for (CSVRecord record : parser) {
                    result.addNewRow();
                    result.getFileRows().get(result.getFileRows().size() - 1)
                            .sourceRowNumber(Math.toIntExact(sourceRow));
                    for (String cell : record) {
                        result.addToLastRow(cell);
                    }
                    sourceRow = parser.getCurrentLineNumber() + 1;
                }
            }
        }
        return result;
    }

    /** Count separators in the first complete record; quoted punctuation is data. */
    private static Header readHeader(BufferedReader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        boolean quoted = false;
        int commas = 0;
        int semicolons = 0;
        int value;
        while ((value = reader.read()) != -1) {
            content.append((char) value);
            if (value == '"') {
                quoted = !quoted;
            } else if (!quoted) {
                if (value == '\n' || value == '\r') {
                    break;
                }
                if (value == ',') {
                    commas++;
                } else if (value == ';') {
                    semicolons++;
                }
            }
        }
        // Preserve the legacy single-column case, including comma-decimal data.
        return new Header(commas > semicolons ? ',' : ';', content.toString());
    }


    private record Header(char delimiter, String content) {
    }

    /** Replay only the inspected header; the remaining file stays streamed. */
    private static Reader prepend(String prefix, Reader remainder) {
        return new Reader() {
            private final StringReader first = new StringReader(prefix);

            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                int count = first.read(buffer, offset, length);
                return count != -1 ? count : remainder.read(buffer, offset, length);
            }

            @Override
            public void close() throws IOException {
                first.close();
                remainder.close();
            }
        };
    }
}
