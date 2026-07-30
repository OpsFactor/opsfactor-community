package com.opsfactor.community.capability.cluster.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data @JsonInclude(JsonInclude.Include.NON_NULL)
public class RegraAlocaoClusterLocationsPaisEstadoDTO extends RegraAlocaoClusterLocationsDTO {

    private String pais;
    private String estado;

}
