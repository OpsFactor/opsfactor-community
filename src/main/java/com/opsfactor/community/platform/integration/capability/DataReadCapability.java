package com.opsfactor.community.platform.integration.capability;

/** Smallest integration capability, usable without persistence or upload support. */
@FunctionalInterface
public interface DataReadCapability<S, R> {

    R read(S selection);

}
