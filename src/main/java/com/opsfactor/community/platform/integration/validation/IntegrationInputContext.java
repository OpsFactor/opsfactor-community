package com.opsfactor.community.platform.integration.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Private input provenance, independent of DTO business equality and batching.
 * File rows and JSON records use one-based positions. Headers are an immutable
 * snapshot of the mapper's functional contract; they never contain row data.
 */
public record IntegrationInputContext(boolean file, int rowNumber, List<String> fileHeaders) {

    public IntegrationInputContext {

        if (rowNumber < 1) throw new IllegalArgumentException("Input position must be positive");
        fileHeaders = Collections.unmodifiableList(new ArrayList<>(fileHeaders));

    }

    /** Describes the actual source instead of treating JSON as an Excel file. */
    public String location() {

        return file ? " at line " + rowNumber : " at JSON record " + rowNumber;

    }

}
