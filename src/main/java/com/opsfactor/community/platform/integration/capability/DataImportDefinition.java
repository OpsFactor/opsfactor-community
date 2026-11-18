package com.opsfactor.community.platform.integration.capability;

import java.io.InputStream;

/** Optional write capability for data models that already accept file imports. */
public interface DataImportDefinition<S> extends DataOperationDefinition<S>,
        DataImportCapability<S, DataImportDefinition.InputFile, Void> {

    /** Parses and persists the uploaded stream using the established domain service. */
    void readAndSave(S selection, InputStream input, String fileName, String requestedBy) throws Exception;

    record InputFile(InputStream input, String filename, String requestedBy) { }

    @Override
    default Void importData(S selection, InputFile input) throws Exception {

        readAndSave(selection, input.input(), input.filename(), input.requestedBy());
        return null;

    }

}
