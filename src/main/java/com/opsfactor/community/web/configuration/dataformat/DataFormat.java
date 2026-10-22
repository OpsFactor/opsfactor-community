package com.opsfactor.community.web.configuration.dataformat;

import java.util.Locale;

/** Representation of a data response. Absence of an explicit format preserves JSON. */
public enum DataFormat {
    JSON("application/json", "json"),
    ARROW("application/vnd.apache.arrow.stream", "arrow"),
    PARQUET("application/vnd.apache.parquet", "parquet"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    CSV("text/csv", "csv");

    private final String mediaType;
    private final String extension;

    DataFormat(String mediaType, String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String mediaType() { return mediaType; }
    public String extension() { return extension; }

    public static DataFormat fromParameter(String parameter) {
        if (parameter == null || parameter.isBlank()) return JSON;
        try {
            return valueOf(parameter.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported data format: " + parameter, exception);
        }
    }
}
