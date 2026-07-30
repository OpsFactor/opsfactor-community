package com.opsfactor.community.capability.demandplanning.web.spi;

import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjectionFactory;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.springframework.stereotype.Component;

/**
 * Fallback Community para Comparison Plan.
 */
@Component
public class CommunityDemandPlanningComparisonPlanSpi implements DemandPlanningComparisonPlanSpi {

    /**
     * Comparison Plan e capability exclusiva do Enterprise.
     */
    @Override
    public void validateReferencePlanRequest(String referencePlanId) {

        if (referencePlanId != null) {
            throw new RequiresEnterpriseVersionException("Demand Planning reference plan");
        }

    }

    /**
     * Usa a projection normal depois de garantir que nao ha plano comparado.
     */
    @Override
    public KeyFigureProjection getKeyFigureProjection(
            ConfiguredViewSelectionDTO configuredViewSelectionDTO,
            DemandPlan demandPlan,
            ConfiguredViewProjection configuredViewProjection,
            KeyFigureProjectionFactory keyFigureProjectionFactory) {

        validateReferencePlanRequest(configuredViewSelectionDTO.referencePlanId);
        return keyFigureProjectionFactory.getKeyFigureProjectionDeDemandPlan(
                demandPlan,
                configuredViewProjection,
                true);

    }

}
