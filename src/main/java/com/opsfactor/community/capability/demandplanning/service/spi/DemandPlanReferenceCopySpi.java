package com.opsfactor.community.capability.demandplanning.service.spi;

import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;

/**
 * Delimita a copia de um Demand Plan de referencia durante a geracao oficial.
 *
 * <p>O Community conserva o contrato de rejeitar a capability. O Enterprise
 * pode implementar a copia sem duplicar o fluxo de forecast, recebendo as
 * projections ja materializadas pela execucao base.</p>
 */
public interface DemandPlanReferenceCopySpi {

    /**
     * Valida a solicitacao antes de qualquer consulta ou persistencia do plano
     * novo.
     */
    void validateReferencePlanRequest(
            Long referenceDemandPlanId,
            boolean copyOnlyFrozenHorizon);

    /**
     * Copia a fotografia permitida do plano de referencia para o plano novo.
     */
    void copyReferencePlan(
            DemandPlan targetDemandPlan,
            Long referenceDemandPlanId,
            boolean copyOnlyFrozenHorizon,
            ParametrosDemandPlanProjection demandPlanParametersProjection,
            ClusterEParametrosProjection clusterAndParametersProjection,
            UnidadeMedidaProjection unitOfMeasureProjection);

}
