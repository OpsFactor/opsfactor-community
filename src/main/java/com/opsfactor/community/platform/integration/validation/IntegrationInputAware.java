package com.opsfactor.community.platform.integration.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Internal provenance contract; Jackson must neither expose nor accept it. */
public interface IntegrationInputAware {

    @JsonIgnore
    IntegrationInputContext getIntegrationInputContext();

    @JsonIgnore
    void setIntegrationInputContext(IntegrationInputContext integrationInputContext);

    @JsonIgnore
    boolean isIntegrationPrimaryKey();

}
