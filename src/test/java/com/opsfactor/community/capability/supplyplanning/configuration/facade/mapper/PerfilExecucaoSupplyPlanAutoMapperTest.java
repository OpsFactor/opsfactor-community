package com.opsfactor.community.capability.supplyplanning.configuration.facade.mapper;

import com.opsfactor.community.capability.supplyplanning.configuration.facade.dto.PerfilExecucaoSupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

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
    void shouldRoundTripSharedOptimizerScalarControlsWithHistoricalDefaults() {

        PerfilExecucaoSupplyPlan defaultProfile = new PerfilExecucaoSupplyPlan();
        PerfilExecucaoSupplyPlanDTO defaultDto = perfilExecucaoSupplyPlanAutoMapper.converte(defaultProfile);

        Assertions.assertEquals(0.1, defaultDto.getEntityTabuRatio());
        Assertions.assertEquals(10_000, defaultDto.getAcceptedCountLimit());
        Assertions.assertFalse(defaultDto.getConsiderBudgetForGreenfieldLocationActivation());
        Assertions.assertEquals(0.0, defaultDto.getGreenfieldLocationActivationBudget());
        Assertions.assertTrue(defaultDto.getRoundProductionAndSetupsToDetailedPlanBucket());

        PerfilExecucaoSupplyPlanDTO configuredDto = new PerfilExecucaoSupplyPlanDTO();
        configuredDto.setEntityTabuRatio(0.25);
        configuredDto.setAcceptedCountLimit(500);
        configuredDto.setConsiderBudgetForGreenfieldLocationActivation(true);
        configuredDto.setGreenfieldLocationActivationBudget(2_500.0);
        configuredDto.setRoundProductionAndSetupsToDetailedPlanBucket(false);

        PerfilExecucaoSupplyPlan configuredProfile = perfilExecucaoSupplyPlanAutoMapper.converte(configuredDto);
        PerfilExecucaoSupplyPlanDTO roundTrippedDto = perfilExecucaoSupplyPlanAutoMapper.converte(configuredProfile);

        Assertions.assertEquals(0.25, roundTrippedDto.getEntityTabuRatio());
        Assertions.assertEquals(500, roundTrippedDto.getAcceptedCountLimit());
        Assertions.assertTrue(roundTrippedDto.getConsiderBudgetForGreenfieldLocationActivation());
        Assertions.assertEquals(2_500.0, roundTrippedDto.getGreenfieldLocationActivationBudget());
        Assertions.assertFalse(roundTrippedDto.getRoundProductionAndSetupsToDetailedPlanBucket());

    }

    @Test
    void shouldRoundTripHeuristicCapacityLevelingWithFalseDefault() {

        PerfilExecucaoSupplyPlan defaultProfile = new PerfilExecucaoSupplyPlan();

        Assertions.assertFalse(defaultProfile.getHeuristicUnconstrainedPlanCapacityLeveling());
        Assertions.assertFalse(
                perfilExecucaoSupplyPlanAutoMapper.converte(defaultProfile)
                        .getHeuristicUnconstrainedPlanCapacityLeveling());

        PerfilExecucaoSupplyPlanDTO enabledDto = new PerfilExecucaoSupplyPlanDTO();
        enabledDto.setHeuristicUnconstrainedPlanCapacityLeveling(true);

        PerfilExecucaoSupplyPlan enabledProfile =
                perfilExecucaoSupplyPlanAutoMapper.converte(enabledDto);

        Assertions.assertTrue(enabledProfile.getHeuristicUnconstrainedPlanCapacityLeveling());
        Assertions.assertTrue(
                perfilExecucaoSupplyPlanAutoMapper.converte(enabledProfile)
                        .getHeuristicUnconstrainedPlanCapacityLeveling());

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
