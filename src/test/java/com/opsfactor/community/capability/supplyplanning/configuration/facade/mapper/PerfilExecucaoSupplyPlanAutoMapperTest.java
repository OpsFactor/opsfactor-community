package com.opsfactor.community.capability.supplyplanning.configuration.facade.mapper;

import com.opsfactor.community.capability.supplyplanning.configuration.facade.dto.PerfilExecucaoSupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

/** Verifica o contrato bidirecional das preferencias Finance persistidas no perfil compartilhado. */
class PerfilExecucaoSupplyPlanAutoMapperTest {

    private final PerfilExecucaoSupplyPlanAutoMapper perfilExecucaoSupplyPlanAutoMapper =
            Mappers.getMapper(PerfilExecucaoSupplyPlanAutoMapper.class);

    @Test
    void shouldMapProfitLossPreferencesInBothDirections() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setGenerateProfitLoss(true);
        perfilExecucaoSupplyPlan.setAllowSalesProfitLossBomRetroaction(false);

        PerfilExecucaoSupplyPlanDTO dto = perfilExecucaoSupplyPlanAutoMapper.converte(perfilExecucaoSupplyPlan);

        Assertions.assertTrue(dto.getGenerateProfitLoss());
        Assertions.assertFalse(dto.getAllowSalesProfitLossBomRetroaction());

        PerfilExecucaoSupplyPlanDTO reverseDto = new PerfilExecucaoSupplyPlanDTO();
        reverseDto.setGenerateProfitLoss(false);
        reverseDto.setAllowSalesProfitLossBomRetroaction(true);

        PerfilExecucaoSupplyPlan reverseProfile = perfilExecucaoSupplyPlanAutoMapper.converte(reverseDto);

        Assertions.assertFalse(reverseProfile.getGenerateProfitLoss());
        Assertions.assertTrue(reverseProfile.getAllowSalesProfitLossBomRetroaction());

    }

    @Test
    void shouldNeutralizeEnterpriseOptimizerScalarDefaultsInCommunityDto() {

        PerfilExecucaoSupplyPlan defaultProfile = new PerfilExecucaoSupplyPlan();
        PerfilExecucaoSupplyPlanDTO defaultDto = perfilExecucaoSupplyPlanAutoMapper.converte(defaultProfile);

        /*
         * A entidade compartilhada ainda oferece defaults historicos por seus
         * getters. O DTO publico deve neutraliza-los para que a listagem de um
         * perfil Community nao pareca ativar optimizer, Greenfield ou detailed
         * scheduling e nao seja rejeitada pela propria facade.
         */
        Assertions.assertNull(defaultDto.getEntityTabuRatio());
        Assertions.assertNull(defaultDto.getAcceptedCountLimit());
        Assertions.assertFalse(defaultDto.getConsiderBudgetForGreenfieldLocationActivation());
        Assertions.assertNull(defaultDto.getGreenfieldLocationActivationBudget());
        Assertions.assertFalse(defaultDto.getRoundProductionAndSetupsToDetailedPlanBucket());

    }

    @Test
    void shouldFixCommunityHeuristicPipelineAndProTabs() {

        PerfilExecucaoSupplyPlan defaultProfile = new PerfilExecucaoSupplyPlan();
        defaultProfile.setGeraPlanoIrrestrito(false);
        defaultProfile.setHeuristicUnconstrainedPlanCapacityLeveling(false);
        defaultProfile.setIgnoraRestricaoProducaoIrrestrito(true);
        defaultProfile.setLocationsClienteApenasPropagamDemanda(true);
        defaultProfile.setModoPropagacaoDemanda(
                PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda.PROPAGACAO_DEMANDA_LOCATIONS_INTERNAS);

        PerfilExecucaoSupplyPlanDTO communityDto =
                perfilExecucaoSupplyPlanAutoMapper.converte(defaultProfile);

        Assertions.assertTrue(communityDto.getAutomaticallyRunConstrainedPlan());
        Assertions.assertTrue(communityDto.getGenerateUnconstrainedPlan());
        Assertions.assertTrue(communityDto.getHeuristicUnconstrainedPlanCapacityLeveling());
        Assertions.assertFalse(communityDto.getIgnoreProductionConstraintsForUnconstrainedPlan());
        Assertions.assertFalse(communityDto.getConsolidateClientDemand());
        Assertions.assertNull(communityDto.getDemandConsolidationMode());

        PerfilExecucaoSupplyPlanDTO proDto = new PerfilExecucaoSupplyPlanDTO();
        proDto.setAutomaticallyRunConstrainedPlan(false);
        proDto.setGenerateUnconstrainedPlan(false);
        proDto.setHeuristicUnconstrainedPlanCapacityLeveling(false);
        proDto.setIgnoreProductionConstraintsForUnconstrainedPlan(true);
        proDto.setConsolidateClientDemand(true);
        proDto.setDemandConsolidationMode(
                PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda.PROPAGACAO_DEMANDA_REGIOES_COMERCIAIS);

        PerfilExecucaoSupplyPlan communityProfile =
                perfilExecucaoSupplyPlanAutoMapper.converte(proDto);

        Assertions.assertTrue(communityProfile.getEncadeiaExecucaoPlanoRestrito());
        Assertions.assertTrue(communityProfile.getGeraPlanoIrrestrito());
        Assertions.assertTrue(communityProfile.getHeuristicUnconstrainedPlanCapacityLeveling());
        Assertions.assertFalse(communityProfile.getIgnoraRestricaoProducaoIrrestrito());
        Assertions.assertFalse(communityProfile.getLocationsClienteApenasPropagamDemanda());
        Assertions.assertNull(ReflectionTestUtils.getField(
                communityProfile,
                "modoPropagacaoDemanda"));

    }

    /**
     * O DTO e compartilhado com o overlay Enterprise, mas o Community nao pode
     * materializar nem persistir a capability privada de sequenciamento.
     */
    @Test
    void shouldNeutralizeEnterpriseCpSatCapabilitiesInBothCommunityMapperDirections() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setModoExecucao(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        perfilExecucaoSupplyPlan.setEnableLineSequencing(true);
        perfilExecucaoSupplyPlan.setEnableGreenfieldBrownfield(true);

        PerfilExecucaoSupplyPlanDTO dto = perfilExecucaoSupplyPlanAutoMapper.converte(perfilExecucaoSupplyPlan);

        Assertions.assertFalse(dto.getEnableLineSequencing());
        Assertions.assertFalse(dto.getEnableGreenfieldBrownfield());

        PerfilExecucaoSupplyPlanDTO dtoEnterprise = new PerfilExecucaoSupplyPlanDTO();
        dtoEnterprise.setExecutionModel(PerfilExecucaoSupplyPlan.ModoExecucao.OTIMIZADOR);
        dtoEnterprise.setEnableLineSequencing(true);
        dtoEnterprise.setEnableGreenfieldBrownfield(true);

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlanCommunity =
                perfilExecucaoSupplyPlanAutoMapper.converte(dtoEnterprise);

        Assertions.assertFalse(perfilExecucaoSupplyPlanCommunity.getEnableLineSequencing());
        Assertions.assertFalse(perfilExecucaoSupplyPlanCommunity.getEnableGreenfieldBrownfield());

    }

}
