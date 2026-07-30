package com.opsfactor.community.capability.cluster.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegraAlocaoClusterLocationsTipoLocationDTO extends RegraAlocaoClusterLocationsDTO {

    private Location.TipoLocation locationType;

}
