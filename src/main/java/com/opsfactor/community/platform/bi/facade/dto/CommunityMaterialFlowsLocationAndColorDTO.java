package com.opsfactor.community.platform.bi.facade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Uma location da matriz e a cor atribuída à sua posição no gráfico. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommunityMaterialFlowsLocationAndColorDTO(String location, String color) {
}
