package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewCaracteristicaDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewKeyFigureDTO;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Valida as travas Community da configuracao de User Views.
 *
 * <p>O front Community deve ocultar controles Enterprise, mas a API tambem
 * precisa bloquear payloads manuais antes de tocar em repositories. Por isso os
 * testes usam apenas valores invalidos: todos falham na validacao inicial do
 * service, sem necessidade de mocks de persistencia.</p>
 */
public class ConfiguredViewFacadeCommunityTest {

    @Test
    public void saveConfiguredViewDTOShouldRejectWorkflowStageCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        configuredViewDTO.demandPlanWorkflowStageId = "WF_STAGE_01";

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectWorkflowSelectionCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        configuredViewDTO.demandPlanWorkflowId = "WF_01";

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectAggregatedMaterialLevelCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        configuredViewDTO.showMaterialLevel = false;

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectAggregatedLocationLevelCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        configuredViewDTO.showLocationLevel = false;

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectCustomDirectDemandKeyFigureCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        configuredViewDTO.directDemandUpdateKeyFigure = KeyFigureStandardEnum.ITENS_NOVOS.name();

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectDemandPlanningEnterpriseKeyFigureCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO = new ConfiguredViewKeyFigureDTO();
        configuredViewKeyFigureDTO.keyFigure = KeyFigureStandardEnum.ITENS_NOVOS.name();
        configuredViewDTO.keyFigureList = List.of(configuredViewKeyFigureDTO);

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectDemandPlanningWorkingDayKeyFigureSelectionCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO = new ConfiguredViewKeyFigureDTO();
        configuredViewKeyFigureDTO.keyFigure = KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL.name();
        configuredViewDTO.keyFigureList = List.of(configuredViewKeyFigureDTO);

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectSupplyPlanningEnterpriseKeyFigureCommunity() {

        ConfiguredViewDTO configuredViewDTO = getSupplyPlanningBookConfiguredViewDTO();
        ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO = new ConfiguredViewKeyFigureDTO();
        configuredViewKeyFigureDTO.keyFigure = KeyFigureStandardEnum.PRODUCAO_FIRME.name();
        configuredViewDTO.keyFigureList = List.of(configuredViewKeyFigureDTO);

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldPreserveSupplyPlanningUnknownKeyFigureCauseCommunity() {

        ConfiguredViewDTO configuredViewDTO = getSupplyPlanningBookConfiguredViewDTO();
        ConfiguredViewKeyFigureDTO configuredViewKeyFigureDTO = new ConfiguredViewKeyFigureDTO();
        configuredViewKeyFigureDTO.keyFigure = "PRIVATE_SUPPLY_KEY_FIGURE";
        configuredViewDTO.keyFigureList = List.of(configuredViewKeyFigureDTO);

        RequiresEnterpriseVersionException requiresEnterpriseVersionException =
                assertRequiresEnterpriseVersion(configuredViewDTO);

        Assertions.assertTrue(requiresEnterpriseVersionException.getMessage().contains(
                "REQUIRES_ENTERPRISE_VERSION: Planning Book key figure selection requires OpsFactor Enterprise."));
        Assertions.assertTrue(requiresEnterpriseVersionException.getCause() instanceof IllegalArgumentException);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectCharacteristicGroupingCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO = new ConfiguredViewCaracteristicaDTO();
        configuredViewCaracteristicaDTO.characteristicId = "REGION";
        configuredViewCaracteristicaDTO.aggregationType = ConfiguredView.TipoAgregacao.GROUPBY;
        configuredViewDTO.materialCharacteristicDetailList = List.of(configuredViewCaracteristicaDTO);

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectCharacteristicPresentationAndPositionCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO = new ConfiguredViewCaracteristicaDTO();
        configuredViewCaracteristicaDTO.characteristicDescription = "Brand";
        configuredViewCaracteristicaDTO.columnPosition = 1;
        configuredViewDTO.materialCharacteristicDetailList = List.of(configuredViewCaracteristicaDTO);

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectMaterialCharacteristicFilterCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO = new ConfiguredViewCaracteristicaDTO();
        configuredViewCaracteristicaDTO.characteristicId = "BRAND";
        configuredViewCaracteristicaDTO.filteredValues = List.of("B1");
        configuredViewDTO.materialCharacteristicDetailList = List.of(configuredViewCaracteristicaDTO);

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectLocationCharacteristicFilterCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO = new ConfiguredViewCaracteristicaDTO();
        configuredViewCaracteristicaDTO.characteristicId = "REGION";
        configuredViewCaracteristicaDTO.filteredValues = List.of("SOUTH");
        configuredViewDTO.locationCharacteristicDetailList = List.of(configuredViewCaracteristicaDTO);

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    @Test
    public void saveConfiguredViewDTOShouldRejectMaterialLocationDfuFiltersCommunity() {

        ConfiguredViewDTO configuredViewDTO = getDemandPlanningBookConfiguredViewDTO();
        ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO = new ConfiguredViewCaracteristicaDTO();
        configuredViewCaracteristicaDTO.characteristicId = "DFU_SEGMENT";
        configuredViewCaracteristicaDTO.filteredValues = List.of("A");
        configuredViewDTO.materialLocationCharacteristicDetailList = List.of(configuredViewCaracteristicaDTO);

        assertRequiresEnterpriseVersion(configuredViewDTO);

    }

    private RequiresEnterpriseVersionException assertRequiresEnterpriseVersion(ConfiguredViewDTO configuredViewDTO) {

        ConfiguredViewFacade configuredViewFrontService = new ConfiguredViewFacade();

        return Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> configuredViewFrontService.saveConfiguredViewDTO(
                        configuredViewDTO,
                        configuredViewDTO.userId,
                        false));

    }

    private ConfiguredViewDTO getDemandPlanningBookConfiguredViewDTO() {

        ConfiguredViewDTO configuredViewDTO = getConfiguredViewDTOBase();
        configuredViewDTO.viewType = ConfiguredView.TipoView.DEMANDPLANNINGBOOK;
        return configuredViewDTO;

    }

    private ConfiguredViewDTO getSupplyPlanningBookConfiguredViewDTO() {

        ConfiguredViewDTO configuredViewDTO = getConfiguredViewDTOBase();
        configuredViewDTO.viewType = ConfiguredView.TipoView.SUPPLYPLANNINGBOOK;
        return configuredViewDTO;

    }

    private ConfiguredViewDTO getConfiguredViewDTOBase() {

        ConfiguredViewDTO configuredViewDTO = new ConfiguredViewDTO();
        configuredViewDTO.userId = "admin";
        configuredViewDTO.viewName = "Default";
        configuredViewDTO.showMaterialLevel = true;
        configuredViewDTO.showLocationLevel = true;
        return configuredViewDTO;

    }

}
