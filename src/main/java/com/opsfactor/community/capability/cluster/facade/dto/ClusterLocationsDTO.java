package com.opsfactor.community.capability.cluster.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import lombok.Data;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Data @JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterLocationsDTO {
    
    public Long id;
    public String description;
    private Integer priority;

    private List<RegraAlocaoClusterLocationsDTO> regraAlocacaoClusterDTOList = new ArrayList<>();

    @Nullable
    private List<LocationDTO> locations;

}
