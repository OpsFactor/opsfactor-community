package com.opsfactor.community.platform.integration.capability;

import com.opsfactor.community.web.configuration.dataformat.DataFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Read-only integration capability. The selection is typed and the writer is
 * independent of HTTP, persistence and scheduling. It can write to the direct
 * response or to the private artifact store without duplicating extraction.
 */
public interface DataExportDefinition<S> extends DataOperationDefinition<S>, DataReadCapability<S, DataExportDefinition.ExportContent> {

    String fileName(S selection, DataFormat format);

    void write(S selection, OutputStream output, DataFormat format,
               Map<String, String> options) throws IOException;

    /** The destination is selected at consumption time; domain services retain their own transaction boundaries. */
    @Override
    default ExportContent read(S selection) {

        return (output, format, options) -> write(selection, output, format, options);

    }

    @FunctionalInterface
    interface ExportContent {

        void write(OutputStream output, DataFormat format, Map<String, String> options) throws IOException;

    }

}
