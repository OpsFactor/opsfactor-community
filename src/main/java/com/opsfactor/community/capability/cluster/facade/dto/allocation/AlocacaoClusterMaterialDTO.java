package com.opsfactor.community.capability.cluster.facade.dto.allocation;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para visibilidade da alocacao de materiais em clusters.
 */
@Data
@Builder
public class AlocacaoClusterMaterialDTO {

    private Long clusterId;
    private String clusterDescription;
    private String materialId;
    private String materialDescription;
}
