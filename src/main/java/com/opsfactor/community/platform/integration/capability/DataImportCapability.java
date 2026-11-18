package com.opsfactor.community.platform.integration.capability;

/** Optional write capability composed with a reader where the domain model supports it. */
@FunctionalInterface
public interface DataImportCapability<S, I, R> {

    R importData(S selection, I input) throws Exception;

}
