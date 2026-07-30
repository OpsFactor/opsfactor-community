package com.opsfactor.community.capability.cluster.facade.dto.allocation;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para visibilidade da alocacao de locations em clusters.
 */
@Data
@Builder
public class AlocacaoClusterLocationDTO {

    private Long clusterId;
    private String clusterDescription;
    private String locationId;
    private String locationDescription;
}
