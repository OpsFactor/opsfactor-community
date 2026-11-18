package com.opsfactor.community.platform.integration.capability;

import java.util.Set;

/** Typed input and authorization shared by read and import capabilities. */
public interface DataOperationDefinition<S> {

    Class<S> selectionType();

    String processName();

    Set<String> requiredRoles();

    void validate(S selection);

}
