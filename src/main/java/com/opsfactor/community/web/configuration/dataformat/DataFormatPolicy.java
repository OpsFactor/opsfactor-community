package com.opsfactor.community.web.configuration.dataformat;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.Locale;

/** Runtime edition boundary. JSON remains a data API contract; Community files use CSV or XLSX. */
@Component
public class DataFormatPolicy {

    private final Set<DataFormat> formats;

    public DataFormatPolicy() {

        this(Set.of(DataFormat.JSON, DataFormat.CSV, DataFormat.XLSX));

    }

    /** Private editions extend the policy without changing shared representation codecs. */
    protected DataFormatPolicy(Set<DataFormat> formats) {

        this.formats = Set.copyOf(formats);

    }

    public boolean supports(DataFormat format) {

        return formats.contains(format);

    }

    /** Multipart uploads in Community are restricted before reaching the legacy CSV fallback parser. */
    public boolean supportsMultipartFile(String filename) {

        if (filename == null) return false;
        String normalized = filename.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".csv") || normalized.endsWith(".xlsx");

    }

}
