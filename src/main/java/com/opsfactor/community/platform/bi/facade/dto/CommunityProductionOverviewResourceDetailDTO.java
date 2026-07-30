package com.opsfactor.community.platform.bi.facade.dto;

import java.time.LocalDateTime;

/**
 * Linha não agregada de produção para um recurso e período selecionados.
 *
 * <p>Quantidade e capacidade carregam as respectivas unidades explicitamente;
 * por isso o consumidor não deve somar linhas de materiais distintos sem uma
 * escolha de UOM feita fora desta superfície.</p>
 */
public class CommunityProductionOverviewResourceDetailDTO {

    public Long supplyPlanId;
    public String locationId;
    public String locationDescription;
    public String productionResourceId;
    public String productionResourceDescription;
    public Integer periodIndex;
    public LocalDateTime plannedDate;
    public String outputMaterialId;
    public String outputMaterialDescription;
    public String productionVersionId;
    public String routingId;
    public String routingDescription;
    public String billOfMaterialsId;
    public String billOfMaterialsDescription;
    public String resourceCapacityUnitOfMeasureId;
    public String unitOfMeasureId;
    public Double unconstrainedHours;
    public Double constrainedHours;
    public Double workPlanHours;
    public Double throughputQuantityPerHour;
    public Double unconstrainedQuantity;
    public Double constrainedQuantity;
    public Double workPlanQuantity;

}
