package com.opsfactor.community.capability.demandplanning.configuration.facade.mapper;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.PerfilExecucaoDemandPlanDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Mapper MapStruct do perfil de execucao Demand Planning Community.
 *
 * <p>Campos de horizonte fixo, MAPE agregado, auto-fit e regression tree sao
 * neutralizados nesta conversao porque nao possuem runtime Community. Overlays
 * Enterprise devem reabrir esses campos em mapper/service proprio.</p>
 */
@Mapper(componentModel = "spring")
public interface PerfilExecucaoDemandPlanAutoMapper {

    @Mapping(source = "perfilExecucaoDemandPlan.id", target = "id")
    @Mapping(source = "perfilExecucaoDemandPlan.descricao", target = "description")
    @Mapping(expression = "java(com.opsfactor.community.platform.utility.Constantes.TipoDocumentoVenda.SELLOUT)", target = "historicalSalesDocumentType")
    @Mapping(source = "perfilExecucaoDemandPlan.tamanhoBucket", target = "bucketSize")
    @Mapping(source = "perfilExecucaoDemandPlan.numeroPeriodosHorizontePlanejamento", target = "planningHorizonInPeriods")
    @Mapping(expression = "java(false)", target = "constrainPlanEditPeriods")
    @Mapping(target = "initialPlanEditPeriod", ignore = true)
    @Mapping(target = "finalPlanEditPeriod", ignore = true)
    @Mapping(source = "perfilExecucaoDemandPlan.unidadeMedidaPadraoDP.id", target = "defaultDemandPlanningUomId")
    @Mapping(target = "mapeMaterialAggregationLevelId", ignore = true)
    @Mapping(target = "mapeLocationAggregationLevelId", ignore = true)
    @Mapping(target = "defaultAutoTunedDemandPlanConfigurationId", ignore = true)
    @Mapping(target = "autofitModelType", ignore = true)
    @Mapping(target = "modelAutofitObjectiveFunction", ignore = true)
    @Mapping(target = "modelAutofitNumberOfPeriodsForAccuracyEvaluation", ignore = true)
    @Mapping(target = "modelAutofitEvaluationLagInPeriods", ignore = true)
    @Mapping(target = "regressionTreeObjectiveFunction", ignore = true)
    @Mapping(target = "numberOfDimensionsUsedForCandidateSplits", ignore = true)
    @Mapping(target = "numberOfCandidateSplitsByDimension", ignore = true)
    @Mapping(target = "maxDepthAfterLastConfirmedSplit", ignore = true)
    @Mapping(target = "minimumPercentErrorReductionForNewSplits", ignore = true)
    @Mapping(target = "numberOfPeriodsForRegressionTreePruning", ignore = true)
    public PerfilExecucaoDemandPlanDTO converte(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            @Context ParametrosGlobais parametrosGlobais);

    public List<PerfilExecucaoDemandPlanDTO> converteListaEntidadesParaDtoList(
            Collection<PerfilExecucaoDemandPlan> perfilExecucaoDemandPlanList,
            @Context ParametrosGlobais parametrosGlobais);
    public Set<PerfilExecucaoDemandPlanDTO> converteListaEntidadesParaDtoSet(
            Collection<PerfilExecucaoDemandPlan> perfilExecucaoDemandPlanList,
            @Context ParametrosGlobais parametrosGlobais);

}
