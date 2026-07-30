package com.opsfactor.community.capability.demandplanning.service.spi;

import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.springframework.stereotype.Component;

/**
 * Guarda Community para copia de Demand Plan de referencia.
 *
 * <p>Uma instalacao Community pode encontrar campos transicionais no payload
 * ou em dados migrados, mas nunca pode interpretar esses valores como ordem de
 * copiar dados privados.</p>
 */
@Component
public class CommunityDemandPlanReferenceCopySpi implements DemandPlanReferenceCopySpi {

    @Override
    public void validateReferencePlanRequest(
            Long referenceDemandPlanId,
            boolean copyOnlyFrozenHorizon) {

        if (referenceDemandPlanId != null) {
            throw new RequiresEnterpriseVersionException("Demand Planning reference plan");
        }

        if (copyOnlyFrozenHorizon) {
            throw new RequiresEnterpriseVersionException("Demand Planning frozen horizon copy");
        }

    }

    @Override
    public void copyReferencePlan(
            DemandPlan targetDemandPlan,
            Long referenceDemandPlanId,
            boolean copyOnlyFrozenHorizon,
            ParametrosDemandPlanProjection demandPlanParametersProjection,
            ClusterEParametrosProjection clusterAndParametersProjection,
            UnidadeMedidaProjection unitOfMeasureProjection) {

        validateReferencePlanRequest(referenceDemandPlanId, copyOnlyFrozenHorizon);

    }

}
