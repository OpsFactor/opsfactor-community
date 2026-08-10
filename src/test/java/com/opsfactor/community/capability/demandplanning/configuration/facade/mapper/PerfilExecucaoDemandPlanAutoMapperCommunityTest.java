package com.opsfactor.community.capability.demandplanning.configuration.facade.mapper;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.PerfilExecucaoDemandPlanDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Congela o contrato de resposta Community do perfil de execucao Demand Planning.
 *
 * <p>Mesmo que uma base transicional tenha sido criada antes do recorte e ainda
 * contenha documento historico Enterprise, a resposta do mapper Community deve
 * continuar apresentando apenas Sell-out e esconder os campos de MAPE,
 * auto-fit e regression tree que pertencem ao OpsFactor Enterprise.</p>
 */
public class PerfilExecucaoDemandPlanAutoMapperCommunityTest {

    private final PerfilExecucaoDemandPlanAutoMapper perfilExecucaoDemandPlanAutoMapper =
            Mappers.getMapper(PerfilExecucaoDemandPlanAutoMapper.class);

    @Test
    public void converteShouldSanitizeHistoricalDocumentAndEnterpriseFieldsForCommunityResponse() {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("PERFIL_PADRAO");
        perfilExecucaoDemandPlan.setDescricao("Perfil Padrao");
        perfilExecucaoDemandPlan.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLIN);
        perfilExecucaoDemandPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        perfilExecucaoDemandPlan.setNumeroPeriodosHorizontePlanejamento(12);
        perfilExecucaoDemandPlan.setRestringePeriodosEdicaoPlano(true);
        perfilExecucaoDemandPlan.setPeriodoInicialEdicaoPlano(1);
        perfilExecucaoDemandPlan.setPeriodoFinalEdicaoPlano(3);
        perfilExecucaoDemandPlan.setUnidadeMedidaPadraoDP(new UnidadeMedida("UN"));

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.PEDIDO);

        PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO =
                perfilExecucaoDemandPlanAutoMapper.converte(
                        perfilExecucaoDemandPlan,
                        parametrosGlobais);

        Assertions.assertEquals("PERFIL_PADRAO", perfilExecucaoDemandPlanDTO.id);
        Assertions.assertEquals("Perfil Padrao", perfilExecucaoDemandPlanDTO.description);
        Assertions.assertEquals(Constantes.TipoDocumentoVenda.SELLOUT, perfilExecucaoDemandPlanDTO.historicalSalesDocumentType);
        Assertions.assertEquals(Constantes.TamanhoBucket.MENSAL, perfilExecucaoDemandPlanDTO.bucketSize);
        Assertions.assertEquals(12, perfilExecucaoDemandPlanDTO.planningHorizonInPeriods);
        Assertions.assertEquals(false, perfilExecucaoDemandPlanDTO.constrainPlanEditPeriods);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.initialPlanEditPeriod);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.finalPlanEditPeriod);
        Assertions.assertEquals("UN", perfilExecucaoDemandPlanDTO.defaultDemandPlanningUomId);
        assertEnterpriseFieldsHidden(perfilExecucaoDemandPlanDTO);

    }

    @Test
    public void converteListaEntidadesParaDtoListShouldApplyTheSameCommunitySanitization() {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan("PERFIL_PADRAO");
        perfilExecucaoDemandPlan.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.PEDIDO);

        List<PerfilExecucaoDemandPlanDTO> perfilExecucaoDemandPlanDTOList =
                perfilExecucaoDemandPlanAutoMapper.converteListaEntidadesParaDtoList(
                        List.of(perfilExecucaoDemandPlan),
                        new ParametrosGlobais());

        Assertions.assertEquals(1, perfilExecucaoDemandPlanDTOList.size());
        Assertions.assertEquals(Constantes.TipoDocumentoVenda.SELLOUT,
                perfilExecucaoDemandPlanDTOList.get(0).historicalSalesDocumentType);
        assertEnterpriseFieldsHidden(perfilExecucaoDemandPlanDTOList.get(0));

    }

    private static void assertEnterpriseFieldsHidden(PerfilExecucaoDemandPlanDTO perfilExecucaoDemandPlanDTO) {

        Assertions.assertNull(perfilExecucaoDemandPlanDTO.mapeMaterialAggregationLevelId);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.mapeLocationAggregationLevelId);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.defaultAutoTunedDemandPlanConfigurationId);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.autofitModelType);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.modelAutofitObjectiveFunction);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.modelAutofitNumberOfPeriodsForAccuracyEvaluation);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.modelAutofitEvaluationLagInPeriods);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.regressionTreeObjectiveFunction);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.numberOfDimensionsUsedForCandidateSplits);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.numberOfCandidateSplitsByDimension);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.maxDepthAfterLastConfirmedSplit);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.minimumPercentErrorReductionForNewSplits);
        Assertions.assertNull(perfilExecucaoDemandPlanDTO.numberOfPeriodsForRegressionTreePruning);

    }

}
