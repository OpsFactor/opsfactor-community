package com.opsfactor.community.platform.bi.facade.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Cabeçalho da célula aberta e suas linhas de produção sem agregação. */
public class CommunityProductionOverviewResourceDetailResponseDTO {

    public Long supplyPlanId;
    public String locationId;
    public String locationDescription;
    public String productionResourceId;
    public String productionResourceDescription;
    public Integer periodIndex;
    public LocalDateTime plannedDate;
    public String resourceCapacityUnitOfMeasureId;
    public Double availableCapacityInHoursOrQuantity;
    public List<CommunityProductionOverviewResourceDetailDTO> rows = new ArrayList<>();

}
