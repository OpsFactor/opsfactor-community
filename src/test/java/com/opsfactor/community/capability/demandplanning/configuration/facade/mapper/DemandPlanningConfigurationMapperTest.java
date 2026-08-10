package com.opsfactor.community.capability.demandplanning.configuration.facade.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningClusterLevelConfigurationDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningForecastParametersDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningGeneralParametersDTO;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Valida a borda Community da configuracao de forecast de Demand Planning.
 *
 * O metodo testado e privado porque a regra pertence ao mapper completo de
 * configuracao, mas os caminhos publicos dependem de repositories/factories.
 * A reflexao aqui mantem o teste focado no contrato de edicao sem subir Spring
 * nem criar fixtures de banco.
 */
public class DemandPlanningConfigurationMapperTest {

    private static final Set<String> COMMUNITY_ACCEPTED_GENERAL_FIELD_NAMES = Set.of(
            "executeDemandPlan",
            "uomId",
            "roundToSalesUnit",
            "considerHistoricalSalesOfInactiveDfus",
            "generateForecastForDiscontinuedMaterials",
            "materialAggregationType",
            "locationAggregationType",
            "daysSalesHistory");

    private static final Set<String> COMMUNITY_ACCEPTED_FORECAST_FIELD_NAMES = Set.of(
            "daysMovingAverageModel",
            "daysTopDownSplit",
            "alpha",
            "beta",
            "gamma");

    @Test
    public void getProjectionDeDtoShouldRejectEnterprisePayloadBeforeRepositories() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.statisticalModel =
                Constantes.DPModeloEstatistico.CHRONOS;

        /*
         * Nao injetamos repositories/factories de proposito. Payload Enterprise
         * deve falhar na validacao Community antes de qualquer lookup de perfil,
         * cluster, UOM ou projection.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningConfigurationMapper.getProjectionDeDto(demandPlanningClusterLevelConfigurationDTO));

    }

    @Test
    public void getDemandPlanningConfigurationDtoFromEntitiesShouldFailExplicitlyForIncompleteEntity() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper =
                new DemandPlanningConfigurationMapper();

        assertClusterLevelEntityReadValidationFailure(
                demandPlanningConfigurationMapper,
                null,
                "Demand Planning cluster-level parameters are required");

        assertClusterLevelEntityReadValidationFailure(
                demandPlanningConfigurationMapper,
                new ParametrosDemandPlanNivelCluster(),
                "Demand Planning cluster-level parameter key is required");

        assertClusterLevelEntityReadValidationFailure(
                demandPlanningConfigurationMapper,
                getParametrosDemandPlanNivelCluster(null, getMaterialCluster(11L), getLocationCluster(12L)),
                "Demand Planning execution profile is required for cluster-level configuration");

        assertClusterLevelEntityReadValidationFailure(
                demandPlanningConfigurationMapper,
                getParametrosDemandPlanNivelCluster(new PerfilExecucaoDemandPlan(), getMaterialCluster(11L), getLocationCluster(12L)),
                "Demand Planning execution profile id is required for cluster-level configuration");

        assertClusterLevelEntityReadValidationFailure(
                demandPlanningConfigurationMapper,
                getParametrosDemandPlanNivelCluster(getDemandPlanExecutionProfile("DP_PROFILE"), getMaterialCluster(11L), null),
                "Demand Planning location cluster is required for cluster-level configuration");

        assertClusterLevelEntityReadValidationFailure(
                demandPlanningConfigurationMapper,
                getParametrosDemandPlanNivelCluster(getDemandPlanExecutionProfile("DP_PROFILE"), getMaterialCluster(11L), new ClusterLocations()),
                "Demand Planning location cluster id is required for cluster-level configuration");

        assertClusterLevelEntityReadValidationFailure(
                demandPlanningConfigurationMapper,
                getParametrosDemandPlanNivelCluster(getDemandPlanExecutionProfile("DP_PROFILE"), null, getLocationCluster(12L)),
                "Demand Planning material cluster is required for cluster-level configuration");

        assertClusterLevelEntityReadValidationFailure(
                demandPlanningConfigurationMapper,
                getParametrosDemandPlanNivelCluster(
                        getDemandPlanExecutionProfile("DP_PROFILE"),
                        new ClusterProdutosDemandPlanning(),
                        getLocationCluster(12L)),
                "Demand Planning material cluster id is required for cluster-level configuration");

    }

    @Test
    public void validaConfiguracoesEnterpriseCommunityShouldRejectEveryNonCommunityGeneralField() throws Exception {

        /*
         * Os parametros gerais aceitos pelo Community ficam na allowlist acima.
         * Todos os demais campos existem apenas para payload compartilhado e
         * devem falhar antes de repositories/factories.
         */
        for (Field field : DemandPlanningGeneralParametersDTO.class.getDeclaredFields()) {
            if (field.isSynthetic() || COMMUNITY_ACCEPTED_GENERAL_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            Object enterpriseFieldValue = getEnterpriseGeneralFieldValue(field);
            Assertions.assertNotNull(
                    enterpriseFieldValue,
                    "Campo geral sem valor de teste Enterprise configurado: " + field.getName());

            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                    getCommunityDemandPlanningClusterLevelConfigurationDTO();
            field.setAccessible(true);
            field.set(
                    demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters,
                    enterpriseFieldValue);

            assertRequiresEnterpriseVersionException(
                    demandPlanningConfigurationMapper,
                    demandPlanningClusterLevelConfigurationDTO);
        }

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectEveryNonCommunityForecastField() throws Exception {

        /*
         * Campos como statisticalModel e splitModel sao funcionais apenas para
         * valores Community. O teste injeta valores Enterprise por campo para
         * garantir que nenhuma opcao bloqueada seja aceita silenciosamente.
         */
        for (Field field : DemandPlanningForecastParametersDTO.class.getDeclaredFields()) {
            if (field.isSynthetic() || COMMUNITY_ACCEPTED_FORECAST_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            Object enterpriseFieldValue = getEnterpriseForecastFieldValue(field);
            Assertions.assertNotNull(
                    enterpriseFieldValue,
                    "Campo de forecast sem valor de teste Enterprise configurado: " + field.getName());

            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
            DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO =
                    getCommunityDemandPlanningForecastParametersDTO();
            field.setAccessible(true);
            field.set(demandPlanningForecastParametersDTO, enterpriseFieldValue);

            assertRequiresEnterpriseVersionException(
                    demandPlanningConfigurationMapper,
                    demandPlanningForecastParametersDTO);
        }

    }

    @Test
    public void getNovaEntidadeParametrosDeDtoShouldRejectEnterprisePayloadBeforeRepositories() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.useExecutionProfileAutofitModel = true;

        /*
         * Esta entrada tambem e publica, entao precisa proteger chamadas diretas
         * sem depender da ordem de validacao de getProjectionDeDto.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningConfigurationMapper.getNovaEntidadeParametrosDeDto(demandPlanningClusterLevelConfigurationDTO));

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldAcceptCommunityForecastConfiguration() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = getCommunityDemandPlanningForecastParametersDTO();

        invokeValidaForecastParametersEnterpriseCommunity(
                demandPlanningConfigurationMapper,
                demandPlanningForecastParametersDTO);

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectNonPositiveMovingAverageWindow() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO =
                getCommunityDemandPlanningForecastParametersDTO();
        demandPlanningForecastParametersDTO.daysMovingAverageModel = 0;

        InvocationTargetException invocationTargetException =
                Assertions.assertThrows(
                        InvocationTargetException.class,
                        () -> invokeValidaForecastParametersEnterpriseCommunity(
                                demandPlanningConfigurationMapper,
                                demandPlanningForecastParametersDTO));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Demand Planning Moving Average historical window must be positive",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectNonPositiveTopDownSplitWindow() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO =
                getCommunityDemandPlanningForecastParametersDTO();
        demandPlanningForecastParametersDTO.daysTopDownSplit = 0;

        InvocationTargetException invocationTargetException =
                Assertions.assertThrows(
                        InvocationTargetException.class,
                        () -> invokeValidaForecastParametersEnterpriseCommunity(
                                demandPlanningConfigurationMapper,
                                demandPlanningForecastParametersDTO));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Demand Planning Historical Sales split reference window must be positive",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    public void validaConfiguracoesEnterpriseCommunityShouldAcceptCommunityGeneralParameters() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();

        demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                demandPlanningClusterLevelConfigurationDTO);

    }

    @Test
    public void validaConfiguracoesEnterpriseCommunityShouldRejectNonPositiveSalesHistoryWindow() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.daysSalesHistory = 0;

        IllegalArgumentException illegalArgumentException =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                                demandPlanningClusterLevelConfigurationDTO));

        Assertions.assertEquals(
                "Demand Planning statistical forecast historical window must be positive",
                illegalArgumentException.getMessage());

    }

    @Test
    public void demandPlanningGeneralParametersDtoShouldRejectHistoricalPayloadFieldNames() {

        for (String historicalFieldName : List.of(
                "generateForecastForDiscontinuedProducts",
                "daysAsNewProduct")) {
            Assertions.assertThrows(
                    JsonProcessingException.class,
                    () -> new ObjectMapper().readValue(
                            "{\"" + historicalFieldName + "\":true}",
                            DemandPlanningGeneralParametersDTO.class));
        }

    }

    @Test
    public void demandPlanningGeneralParametersDtoShouldRoundTripCanonicalMaterialPayloadFieldNames()
            throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        DemandPlanningGeneralParametersDTO demandPlanningGeneralParametersDTO = objectMapper.readValue(
                """
                {
                  "generateForecastForDiscontinuedMaterials": true,
                  "daysAsNewMaterial": 30
                }
                """,
                DemandPlanningGeneralParametersDTO.class);

        Assertions.assertTrue(
                demandPlanningGeneralParametersDTO.generateForecastForDiscontinuedMaterials);
        Assertions.assertEquals(30, demandPlanningGeneralParametersDTO.daysAsNewMaterial);

        String serializedJson = objectMapper.writeValueAsString(demandPlanningGeneralParametersDTO);
        Assertions.assertTrue(serializedJson.contains("\"generateForecastForDiscontinuedMaterials\""));
        Assertions.assertTrue(serializedJson.contains("\"daysAsNewMaterial\""));

    }

    @Test
    public void validaConfiguracoesEnterpriseCommunityShouldFailExplicitlyForIncompletePayload() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();

        IllegalArgumentException missingConfigurationException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(null));
        Assertions.assertEquals(
                "Demand Planning cluster-level configuration DTO is required",
                missingConfigurationException.getMessage());

        DemandPlanningClusterLevelConfigurationDTO missingGeneralParametersDTO =
                new DemandPlanningClusterLevelConfigurationDTO();
        missingGeneralParametersDTO.demandPlanningForecastParameters =
                getCommunityDemandPlanningForecastParametersDTO();
        IllegalArgumentException missingGeneralParametersException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                        missingGeneralParametersDTO));
        Assertions.assertEquals(
                "Demand Planning general parameters are required",
                missingGeneralParametersException.getMessage());

        DemandPlanningClusterLevelConfigurationDTO missingForecastParametersDTO =
                new DemandPlanningClusterLevelConfigurationDTO();
        missingForecastParametersDTO.demandPlanningGeneralParameters =
                getCommunityDemandPlanningGeneralParametersDTO();
        IllegalArgumentException missingForecastParametersException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                        missingForecastParametersDTO));
        Assertions.assertEquals(
                "Demand Planning forecast parameters are required",
                missingForecastParametersException.getMessage());

        DemandPlanningClusterLevelConfigurationDTO missingStatisticalModelDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        missingStatisticalModelDTO.demandPlanningForecastParameters.statisticalModel = null;
        IllegalArgumentException missingStatisticalModelException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                        missingStatisticalModelDTO));
        Assertions.assertEquals(
                "Demand Planning statistical forecast model is required",
                missingStatisticalModelException.getMessage());

        DemandPlanningClusterLevelConfigurationDTO missingSplitModelDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        missingSplitModelDTO.demandPlanningForecastParameters.splitModel = null;
        IllegalArgumentException missingSplitModelException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                        missingSplitModelDTO));
        Assertions.assertEquals(
                "Demand Planning forecast split model is required",
                missingSplitModelException.getMessage());

    }

    @Test
    public void validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunityShouldFailExplicitlyForIncompletePayload() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();

        IllegalArgumentException missingConfigurationException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunity(null));
        Assertions.assertEquals(
                "Demand Planning cluster-level configuration DTO is required",
                missingConfigurationException.getMessage());

        DemandPlanningClusterLevelConfigurationDTO missingExecutionProfileDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        missingExecutionProfileDTO.demandPlanExecutionProfileId = null;
        IllegalArgumentException missingExecutionProfileException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunity(
                        missingExecutionProfileDTO));
        Assertions.assertEquals(
                "Demand Planning execution profile id is required",
                missingExecutionProfileException.getMessage());

        DemandPlanningClusterLevelConfigurationDTO missingLocationClusterDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        missingLocationClusterDTO.locationClusterId = null;
        IllegalArgumentException missingLocationClusterException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunity(
                        missingLocationClusterDTO));
        Assertions.assertEquals(
                "Demand Planning location cluster id is required",
                missingLocationClusterException.getMessage());

        DemandPlanningClusterLevelConfigurationDTO missingMaterialClusterDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        missingMaterialClusterDTO.materialClusterId = null;
        IllegalArgumentException missingMaterialClusterException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.validaIdentidadeDemandPlanningClusterLevelConfigurationDTOCommunity(
                        missingMaterialClusterDTO));
        Assertions.assertEquals(
                "Demand Planning material cluster id is required",
                missingMaterialClusterException.getMessage());

    }

    @Test
    public void validaConfiguracoesEnterpriseCommunityShouldRejectSupportSeries() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.regressionTimeSeries =
                List.of("support-series-id");

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                demandPlanningClusterLevelConfigurationDTO);

    }

    @Test
    public void validaConfiguracoesEnterpriseCommunityShouldRejectDefaultAutoFitConfiguration() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.useExecutionProfileAutofitModel = true;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                demandPlanningClusterLevelConfigurationDTO);

    }

    @Test
    public void validaConfiguracoesEnterpriseCommunityShouldRejectProAggregationAndSalesUomRounding() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();

        DemandPlanningClusterLevelConfigurationDTO bottomUpConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        bottomUpConfigurationDTO.demandPlanningGeneralParameters.materialAggregationType =
                Constantes.DPNivelAgregacao.BOTTOM_UP;

        DemandPlanningClusterLevelConfigurationDTO salesUomRoundingConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        salesUomRoundingConfigurationDTO.demandPlanningGeneralParameters.roundToSalesUnit = true;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                bottomUpConfigurationDTO);
        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                salesUomRoundingConfigurationDTO);

    }

    @Test
    public void validaConfiguracoesEnterpriseCommunityShouldRejectBudgetAndNewProductTreatment() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningClusterLevelConfigurationDTO budgetForecastConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        budgetForecastConfigurationDTO.demandPlanningGeneralParameters.budgetId = 1L;

        DemandPlanningClusterLevelConfigurationDTO newProductTreatmentConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        newProductTreatmentConfigurationDTO.demandPlanningGeneralParameters.daysAsNewMaterial = 30;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                budgetForecastConfigurationDTO);
        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                newProductTreatmentConfigurationDTO);

    }

    @Test
    public void validaConfiguracoesEnterpriseCommunityShouldRejectInternalForecastRegressors() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningClusterLevelConfigurationDTO targetTrendRegressorConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        targetTrendRegressorConfigurationDTO.demandPlanningGeneralParameters.considerTargetTrendGrowthYoy = true;

        DemandPlanningClusterLevelConfigurationDTO workingDaysRegressorConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        workingDaysRegressorConfigurationDTO.demandPlanningGeneralParameters.includeWorkingDaysRegressor = true;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                targetTrendRegressorConfigurationDTO);
        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                workingDaysRegressorConfigurationDTO);

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectEnterpriseForecastModels() throws Exception {

        Set<Constantes.DPModeloEstatistico> modelosEstatisticosEnterprise = EnumSet.complementOf(EnumSet.of(
                Constantes.DPModeloEstatistico.MM,
                Constantes.DPModeloEstatistico.RMM,
                Constantes.DPModeloEstatistico.ARIMA,
                Constantes.DPModeloEstatistico.HOLT_WINTERS,
                Constantes.DPModeloEstatistico.ES));

        for (Constantes.DPModeloEstatistico dpModeloEstatistico : modelosEstatisticosEnterprise) {
            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
            DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = getCommunityDemandPlanningForecastParametersDTO();
            demandPlanningForecastParametersDTO.statisticalModel = dpModeloEstatistico;

            assertRequiresEnterpriseVersionException(
                    demandPlanningConfigurationMapper,
                    demandPlanningForecastParametersDTO);
        }

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectEnterpriseSplitModels() throws Exception {

        Set<Constantes.DPModeloSplit> modelosSplitEnterprise = EnumSet.complementOf(EnumSet.of(
                Constantes.DPModeloSplit.HISTORICAL_SALES));

        for (Constantes.DPModeloSplit dpModeloSplit : modelosSplitEnterprise) {
            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
            DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = getCommunityDemandPlanningForecastParametersDTO();
            demandPlanningForecastParametersDTO.splitModel = dpModeloSplit;

            assertRequiresEnterpriseVersionException(
                    demandPlanningConfigurationMapper,
                    demandPlanningForecastParametersDTO);
        }

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectStockoutTreatment() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = getCommunityDemandPlanningForecastParametersDTO();
        demandPlanningForecastParametersDTO.considerStockoutData = true;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                demandPlanningForecastParametersDTO);

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectEventUplift() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = getCommunityDemandPlanningForecastParametersDTO();
        demandPlanningForecastParametersDTO.upliftModel = Constantes.DPModeloUplift.ALAVANCAGEM_EVENTO;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                demandPlanningForecastParametersDTO);

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectOutlierSmoothing() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = getCommunityDemandPlanningForecastParametersDTO();
        demandPlanningForecastParametersDTO.smoothingModel = Constantes.DPModeloNormalizacao.PERCENTIS;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                demandPlanningForecastParametersDTO);

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectOutlierSmoothingParameters() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();

        DemandPlanningForecastParametersDTO daysSmoothingConfigurationDTO = getCommunityDemandPlanningForecastParametersDTO();
        daysSmoothingConfigurationDTO.daysSmoothingModel = 180;

        DemandPlanningForecastParametersDTO upperPercentileConfigurationDTO = getCommunityDemandPlanningForecastParametersDTO();
        upperPercentileConfigurationDTO.enableUpperPercentileSmoothing = true;

        DemandPlanningForecastParametersDTO lowerPercentileConfigurationDTO = getCommunityDemandPlanningForecastParametersDTO();
        lowerPercentileConfigurationDTO.smoothingLowerPercentile = 5.0;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                daysSmoothingConfigurationDTO);
        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                upperPercentileConfigurationDTO);
        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                lowerPercentileConfigurationDTO);

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectProphetParameters() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = getCommunityDemandPlanningForecastParametersDTO();
        demandPlanningForecastParametersDTO.prophetAutoSeasonalityPriorScale = false;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                demandPlanningForecastParametersDTO);

    }

    @Test
    public void validaForecastParametersEnterpriseCommunityShouldRejectChronosParameters() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = getCommunityDemandPlanningForecastParametersDTO();
        demandPlanningForecastParametersDTO.chronosForceAggregatedForecast = true;

        assertRequiresEnterpriseVersionException(
                demandPlanningConfigurationMapper,
                demandPlanningForecastParametersDTO);

    }

    @Test
    public void getDemandPlanningForecastParametersDTOFromEntitiesShouldNeutralizeLegacyEnterpriseModelAndSplit() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster = new ParametrosDemandPlanNivelCluster();
        parametrosDemandPlanNivelCluster.setDpModeloEstatistico(Constantes.DPModeloEstatistico.CHRONOS);
        parametrosDemandPlanNivelCluster.setDpModeloSplit(Constantes.DPModeloSplit.HTS);

        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO =
                invokeGetDemandPlanningForecastParametersDTOFromEntities(
                        demandPlanningConfigurationMapper,
                        parametrosDemandPlanNivelCluster);

        /*
         * A leitura Community nao deve devolver configuracao Enterprise salva
         * em base transicional. Payloads legados podem reenviar estes campos para
         * montar os seletores, portanto expor CHRONOS/HTS aqui reabriria uma
         * feature bloqueada mesmo que o save posterior falhasse.
         */
        Assertions.assertEquals(
                Constantes.DPModeloEstatistico.MM,
                demandPlanningForecastParametersDTO.statisticalModel);
        Assertions.assertEquals(
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                demandPlanningForecastParametersDTO.splitModel);

    }

    @Test
    public void getDemandPlanningGeneralParametersDTOFromEntitiesShouldReturnNeutralEnterpriseDefaults() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        setField(
                demandPlanningConfigurationMapper,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory());

        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster = new ParametrosDemandPlanNivelCluster();
        parametrosDemandPlanNivelCluster.setExecutaDp(true);
        parametrosDemandPlanNivelCluster.setUnidadeMedidaPadraoDP(new UnidadeMedida("CX"));
        parametrosDemandPlanNivelCluster.setArredondaParaUnidadeVenda(true);
        parametrosDemandPlanNivelCluster.setDpUsaHistoricoDemandaInativos(false);
        parametrosDemandPlanNivelCluster.setDpGeraForecastParaDescontinuados(true);
        parametrosDemandPlanNivelCluster.setMaterialAggregationType(Constantes.DPNivelAgregacao.BOTTOM_UP);
        parametrosDemandPlanNivelCluster.setLocationAggregationType(Constantes.DPNivelAgregacao.TOP_DOWN);
        parametrosDemandPlanNivelCluster.setDiasHistoricosForecastEstatistico(180);

        DemandPlanningGeneralParametersDTO demandPlanningGeneralParametersDTO =
                invokeGetDemandPlanningGeneralParametersDTOFromEntities(
                        demandPlanningConfigurationMapper,
                        parametrosDemandPlanNivelCluster);

        Assertions.assertEquals(true, demandPlanningGeneralParametersDTO.executeDemandPlan);
        Assertions.assertEquals("CX", demandPlanningGeneralParametersDTO.uomId);
        Assertions.assertEquals(false, demandPlanningGeneralParametersDTO.roundToSalesUnit);
        Assertions.assertEquals(false, demandPlanningGeneralParametersDTO.considerHistoricalSalesOfInactiveDfus);
        Assertions.assertEquals(true, demandPlanningGeneralParametersDTO.generateForecastForDiscontinuedMaterials);
        Assertions.assertEquals(Constantes.DPNivelAgregacao.TOP_DOWN, demandPlanningGeneralParametersDTO.materialAggregationType);
        Assertions.assertEquals(Constantes.DPNivelAgregacao.TOP_DOWN, demandPlanningGeneralParametersDTO.locationAggregationType);
        Assertions.assertEquals(180, demandPlanningGeneralParametersDTO.daysSalesHistory);

        /*
         * Campos Enterprise existem no DTO compartilhado apenas para rejeicao
         * defensiva. Na resposta Community eles devem voltar neutros para que
         * uma base transicional nao reative budget, support series, auto-fit,
         * produto novo ou regressores estatisticos no front.
         */
        Assertions.assertNull(demandPlanningGeneralParametersDTO.budgetId);
        Assertions.assertEquals(0, demandPlanningGeneralParametersDTO.daysAsNewMaterial);
        Assertions.assertTrue(demandPlanningGeneralParametersDTO.regressionTimeSeries.isEmpty());
        Assertions.assertFalse(demandPlanningGeneralParametersDTO.considerTargetTrendGrowthYoy);
        Assertions.assertEquals(365, demandPlanningGeneralParametersDTO.numberOfDaysCurrentLevelAsAverageOfHistoricalStl);
        Assertions.assertEquals(0.0d, demandPlanningGeneralParametersDTO.targetGrowthYoy, 0.0001d);
        Assertions.assertFalse(demandPlanningGeneralParametersDTO.includeWorkingDaysRegressor);
        Assertions.assertFalse(demandPlanningGeneralParametersDTO.useExecutionProfileAutofitModel);

    }

    @Test
    public void getDemandPlanningGeneralParametersDTOFromEntitiesShouldInheritDiscontinuedForecastFromGlobalParameters() throws Exception {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        setField(
                demandPlanningConfigurationMapper,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory(false));

        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster = new ParametrosDemandPlanNivelCluster();
        /*
         * Sem override no cluster, a configuracao precisa usar o mesmo default
         * efetivo global. O campo de historico de inativos nao deve influir na
         * decisao sobre materiais descontinuados.
         */
        parametrosDemandPlanNivelCluster.setDpUsaHistoricoDemandaInativos(true);

        DemandPlanningGeneralParametersDTO demandPlanningGeneralParametersDTO =
                invokeGetDemandPlanningGeneralParametersDTOFromEntities(
                        demandPlanningConfigurationMapper,
                        parametrosDemandPlanNivelCluster);

        Assertions.assertFalse(
                demandPlanningGeneralParametersDTO.generateForecastForDiscontinuedMaterials);

    }

    @Test
    public void atualizaEntidadeParametrosComDTOShouldPersistCommunityDefaultsForMissingOptionalFields() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        setField(
                demandPlanningConfigurationMapper,
                "unidadeMedidaRepository",
                getUnidadeMedidaRepositoryStub(Optional.of(new UnidadeMedida("UN"))));

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.uomId = "UN";
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.locationAggregationType = null;
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.materialAggregationType = null;
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.daysMovingAverageModel = null;
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.daysTopDownSplit = null;

        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster =
                new ParametrosDemandPlanNivelCluster();

        demandPlanningConfigurationMapper.atualizaEntidadeParametrosComDTO(
                parametrosDemandPlanNivelCluster,
                demandPlanningClusterLevelConfigurationDTO);

        /*
         * O Community aceita apenas campos verdadeiramente opcionais nulos como
         * pedido de default. Modelo estatistico e split sao obrigatorios e ja
         * chegam explicitamente no DTO validado.
         */
        Assertions.assertEquals(
                Constantes.DPNivelAgregacao.TOP_DOWN,
                parametrosDemandPlanNivelCluster.getLocationAggregationType());
        Assertions.assertEquals(
                Constantes.DPNivelAgregacao.TOP_DOWN,
                parametrosDemandPlanNivelCluster.getMaterialAggregationType());
        Assertions.assertEquals(
                Constantes.DPModeloEstatistico.HOLT_WINTERS,
                parametrosDemandPlanNivelCluster.getDpModeloEstatistico());
        Assertions.assertEquals(
                Constantes.DPModeloSplit.HISTORICAL_SALES,
                parametrosDemandPlanNivelCluster.getDpModeloSplit());
        Assertions.assertEquals(
                120,
                parametrosDemandPlanNivelCluster.getDiasMediaMovelDp());
        Assertions.assertEquals(
                120,
                parametrosDemandPlanNivelCluster.getNumeroDiasSplitTopDown());
        Assertions.assertEquals(
                "UN",
                parametrosDemandPlanNivelCluster.getUnidadeMedidaPadraoDP().getId());

    }

    @Test
    public void atualizaEntidadeParametrosComDTOShouldKeepNullUomAsGlobalDefaultWithoutRepositoryLookup() {

        DemandPlanningConfigurationMapper demandPlanningConfigurationMapper = new DemandPlanningConfigurationMapper();
        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters.uomId = null;

        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster =
                new ParametrosDemandPlanNivelCluster();

        demandPlanningConfigurationMapper.atualizaEntidadeParametrosComDTO(
                parametrosDemandPlanNivelCluster,
                demandPlanningClusterLevelConfigurationDTO);

        /*
         * Entidade com UOM nula usa a UOM global na leitura/projection. O mapper
         * nao deve consultar repository com id nulo nem exigir bean em teste para
         * esse caminho.
         */
        Assertions.assertNull(parametrosDemandPlanNivelCluster.getUnidadeMedidaPadraoDP());

    }

    @Test
    public void getUnidadeMedidaPadraoDpOuNullShouldDeclareNullableReturnContract() throws NoSuchMethodException {

        Method getUnidadeMedidaPadraoDpOuNullMethod =
                DemandPlanningConfigurationMapper.class.getDeclaredMethod(
                        "getUnidadeMedidaPadraoDpOuNull",
                        DemandPlanningGeneralParametersDTO.class);

        /*
         * UOM nula nao e erro de configuracao: e a forma documentada de herdar
         * a unidade global do ambiente. O @Nullable no retorno deixa esse
         * contrato explicito para futuros recortes de parametros globais e para
         * overlays Enterprise que reaproveitem a validacao base.
         */
        Assertions.assertTrue(
                getUnidadeMedidaPadraoDpOuNullMethod.isAnnotationPresent(Nullable.class),
                "getUnidadeMedidaPadraoDpOuNull deve declarar @Nullable no retorno.");

    }

    private static void assertClusterLevelEntityReadValidationFailure(
            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper,
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster,
            String mensagemEsperada) {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningConfigurationMapper.getDemandPlanningConfigurationDtoFromEntities(
                        parametrosDemandPlanNivelCluster));

        Assertions.assertEquals(mensagemEsperada, illegalArgumentException.getMessage());

    }

    private static ParametrosDemandPlanNivelCluster getParametrosDemandPlanNivelCluster(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning,
            ClusterLocations clusterLocations) {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlanParaEmbeddedId =
                perfilExecucaoDemandPlan == null ? new PerfilExecucaoDemandPlan() : perfilExecucaoDemandPlan;
        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanningParaEmbeddedId =
                clusterMateriaisDemandPlanning == null
                        ? new ClusterProdutosDemandPlanning()
                        : clusterMateriaisDemandPlanning;
        ClusterLocations clusterLocationsParaEmbeddedId =
                clusterLocations == null ? new ClusterLocations() : clusterLocations;

        ParametrosDemandPlanNivelCluster.ParametrosDemandPlanNivelClusterCompositeKey compositeKey =
                new ParametrosDemandPlanNivelCluster.ParametrosDemandPlanNivelClusterCompositeKey();
        compositeKey.setPerfilExecucaoDemandPlan(perfilExecucaoDemandPlanParaEmbeddedId);
        compositeKey.setClusterProdutosDemandPlanning(clusterMateriaisDemandPlanningParaEmbeddedId);
        compositeKey.setClusterLocations(clusterLocationsParaEmbeddedId);

        /*
         * O embedded-id marca relacionamentos como obrigatorios e seus setters
         * rejeitam null. Para testar a defesa do mapper contra snapshots
         * corrompidos, simulamos o estado invalido depois da montagem normal da
         * chave, sem depender de JPA ou banco.
         */
        if (perfilExecucaoDemandPlan == null) {
            setField(compositeKey, "perfilExecucaoDemandPlan", null);
        }
        if (clusterMateriaisDemandPlanning == null) {
            setField(compositeKey, "clusterProdutosDemandPlanning", null);
        }
        if (clusterLocations == null) {
            setField(compositeKey, "clusterLocations", null);
        }

        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster =
                new ParametrosDemandPlanNivelCluster();
        parametrosDemandPlanNivelCluster.setParametrosDemandPlanNivelClusterCompositeKey(compositeKey);

        return parametrosDemandPlanNivelCluster;

    }

    private static PerfilExecucaoDemandPlan getDemandPlanExecutionProfile(String id) {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan();
        perfilExecucaoDemandPlan.setId(id);

        return perfilExecucaoDemandPlan;

    }

    private static ClusterProdutosDemandPlanning getMaterialCluster(Long id) {

        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning = new ClusterProdutosDemandPlanning();
        clusterMateriaisDemandPlanning.setId(id);

        return clusterMateriaisDemandPlanning;

    }

    private static ClusterLocations getLocationCluster(Long id) {

        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(id);

        return clusterLocations;

    }

    private static DemandPlanningForecastParametersDTO getCommunityDemandPlanningForecastParametersDTO() {

        DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO = new DemandPlanningForecastParametersDTO();
        demandPlanningForecastParametersDTO.statisticalModel = Constantes.DPModeloEstatistico.HOLT_WINTERS;
        demandPlanningForecastParametersDTO.splitModel = Constantes.DPModeloSplit.HISTORICAL_SALES;
        demandPlanningForecastParametersDTO.upliftModel = Constantes.DPModeloUplift.DESATIVADO;
        demandPlanningForecastParametersDTO.considerStockoutData = false;
        demandPlanningForecastParametersDTO.smoothingModel = Constantes.DPModeloNormalizacao.DESATIVADO;
        demandPlanningForecastParametersDTO.daysSmoothingModel = 365;
        demandPlanningForecastParametersDTO.enableUpperPercentileSmoothing = false;
        demandPlanningForecastParametersDTO.smoothingUpperPercentile = 85.0;
        demandPlanningForecastParametersDTO.enableLowerPercentileSmoothing = false;
        demandPlanningForecastParametersDTO.smoothingLowerPercentile = 15.0;
        demandPlanningForecastParametersDTO.prophetAutoSeasonalityPriorScale = true;
        demandPlanningForecastParametersDTO.prophetSeasonalityPriorScale = 10.0;
        demandPlanningForecastParametersDTO.prophetAutoChangepointPriorScale = true;
        demandPlanningForecastParametersDTO.prophetChangepointPriorScale = 0.05;
        demandPlanningForecastParametersDTO.prophetAutoYearlyFourierOrder = true;
        demandPlanningForecastParametersDTO.prophetYearlyFourierOrder = 10;
        demandPlanningForecastParametersDTO.chronosForceAggregatedForecast = false;

        return demandPlanningForecastParametersDTO;

    }

    private static DemandPlanningClusterLevelConfigurationDTO getCommunityDemandPlanningClusterLevelConfigurationDTO() {

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                new DemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanExecutionProfileId = "DP_PROFILE";
        demandPlanningClusterLevelConfigurationDTO.locationClusterId = 1L;
        demandPlanningClusterLevelConfigurationDTO.materialClusterId = 2L;
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters =
                getCommunityDemandPlanningGeneralParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters =
                getCommunityDemandPlanningForecastParametersDTO();

        return demandPlanningClusterLevelConfigurationDTO;

    }

    private static DemandPlanningGeneralParametersDTO getCommunityDemandPlanningGeneralParametersDTO() {

        DemandPlanningGeneralParametersDTO demandPlanningGeneralParametersDTO = new DemandPlanningGeneralParametersDTO();
        demandPlanningGeneralParametersDTO.useExecutionProfileAutofitModel = false;
        demandPlanningGeneralParametersDTO.roundToSalesUnit = false;
        demandPlanningGeneralParametersDTO.materialAggregationType = Constantes.DPNivelAgregacao.TOP_DOWN;
        demandPlanningGeneralParametersDTO.locationAggregationType = Constantes.DPNivelAgregacao.TOP_DOWN;
        demandPlanningGeneralParametersDTO.budgetId = null;
        demandPlanningGeneralParametersDTO.daysAsNewMaterial = 0;
        demandPlanningGeneralParametersDTO.regressionTimeSeries = List.of();
        demandPlanningGeneralParametersDTO.considerTargetTrendGrowthYoy = false;
        demandPlanningGeneralParametersDTO.numberOfDaysCurrentLevelAsAverageOfHistoricalStl = 365;
        demandPlanningGeneralParametersDTO.targetGrowthYoy = 0.0;
        demandPlanningGeneralParametersDTO.includeWorkingDaysRegressor = false;

        return demandPlanningGeneralParametersDTO;

    }

    private static void assertRequiresEnterpriseVersionException(
            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper,
            DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO) throws Exception {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaForecastParametersEnterpriseCommunity(
                        demandPlanningConfigurationMapper,
                        demandPlanningForecastParametersDTO));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    private static void assertRequiresEnterpriseVersionException(
            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper,
            DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningConfigurationMapper.validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                        demandPlanningClusterLevelConfigurationDTO));

    }

    private static void invokeValidaForecastParametersEnterpriseCommunity(
            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper,
            DemandPlanningForecastParametersDTO demandPlanningForecastParametersDTO) throws Exception {

        Method validaForecastParametersEnterpriseCommunityMethod = DemandPlanningConfigurationMapper.class.getDeclaredMethod(
                "validaForecastParametersEnterpriseCommunity",
                DemandPlanningForecastParametersDTO.class);
        validaForecastParametersEnterpriseCommunityMethod.setAccessible(true);
        validaForecastParametersEnterpriseCommunityMethod.invoke(
                demandPlanningConfigurationMapper,
                demandPlanningForecastParametersDTO);

    }

    private static DemandPlanningForecastParametersDTO invokeGetDemandPlanningForecastParametersDTOFromEntities(
            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper,
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster) throws Exception {

        Method getDemandPlanningForecastParametersDTOFromEntitiesMethod = DemandPlanningConfigurationMapper.class.getDeclaredMethod(
                "getDemandPlanningForecastParametersDTOFromEntities",
                com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract.class);
        getDemandPlanningForecastParametersDTOFromEntitiesMethod.setAccessible(true);
        return (DemandPlanningForecastParametersDTO) getDemandPlanningForecastParametersDTOFromEntitiesMethod.invoke(
                demandPlanningConfigurationMapper,
                parametrosDemandPlanNivelCluster);

    }

    private static DemandPlanningGeneralParametersDTO invokeGetDemandPlanningGeneralParametersDTOFromEntities(
            DemandPlanningConfigurationMapper demandPlanningConfigurationMapper,
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster) throws Exception {

        Method getDemandPlanningGeneralParametersDTOFromEntitiesMethod = DemandPlanningConfigurationMapper.class.getDeclaredMethod(
                "getDemandPlanningGeneralParametersDTOFromEntities",
                ParametrosDemandPlanNivelCluster.class);
        getDemandPlanningGeneralParametersDTOFromEntitiesMethod.setAccessible(true);
        return (DemandPlanningGeneralParametersDTO) getDemandPlanningGeneralParametersDTOFromEntitiesMethod.invoke(
                demandPlanningConfigurationMapper,
                parametrosDemandPlanNivelCluster);

    }

    private static void setField(Object target, String fieldName, Object value) {

        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Nao foi possivel injetar campo de teste: " + fieldName, exception);
        }

    }

    private static UnidadeMedidaRepository getUnidadeMedidaRepositoryStub(
            Optional<UnidadeMedida> unidadeMedidaOptional) {

        return (UnidadeMedidaRepository) Proxy.newProxyInstance(
                UnidadeMedidaRepository.class.getClassLoader(),
                new Class<?>[]{UnidadeMedidaRepository.class},
                (proxy, method, args) -> {

                    if ("findById".equals(method.getName())) {
                        return unidadeMedidaOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "UnidadeMedidaRepositoryStub";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao suportado pelo stub do teste: " + method.getName());

                });

    }

    private static PerfilExecucaoDemandPlanRepository getPerfilExecucaoDemandPlanRepositoryStub(
            Optional<PerfilExecucaoDemandPlan> perfilExecucaoDemandPlanOptional) {

        return (PerfilExecucaoDemandPlanRepository) Proxy.newProxyInstance(
                PerfilExecucaoDemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{PerfilExecucaoDemandPlanRepository.class},
                (proxy, method, args) -> {

                    if ("findById".equals(method.getName())) {
                        return perfilExecucaoDemandPlanOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "PerfilExecucaoDemandPlanRepositoryStub";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao suportado pelo stub do teste: " + method.getName());

                });

    }

    private static class TestClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final Optional<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningOptional;
        private final ClusterLocations clusterLocations;
        private final boolean demandPlanningGenerateForecastForDiscontinuedMaterials;

        private TestClusterEParametrosProjectionFactory() {

            this(
                    Optional.of(new ClusterProdutosDemandPlanning()),
                    new ClusterLocations(),
                    true);

        }

        private TestClusterEParametrosProjectionFactory(
                boolean demandPlanningGenerateForecastForDiscontinuedMaterials) {

            this(
                    Optional.of(new ClusterProdutosDemandPlanning()),
                    new ClusterLocations(),
                    demandPlanningGenerateForecastForDiscontinuedMaterials);

        }

        private TestClusterEParametrosProjectionFactory(
                Optional<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningOptional,
                ClusterLocations clusterLocations) {

            this(
                    clusterMateriaisDemandPlanningOptional,
                    clusterLocations,
                    true);

        }

        private TestClusterEParametrosProjectionFactory(
                Optional<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningOptional,
                ClusterLocations clusterLocations,
                boolean demandPlanningGenerateForecastForDiscontinuedMaterials) {

            this.clusterMateriaisDemandPlanningOptional = clusterMateriaisDemandPlanningOptional;
            this.clusterLocations = clusterLocations;
            this.demandPlanningGenerateForecastForDiscontinuedMaterials =
                    demandPlanningGenerateForecastForDiscontinuedMaterials;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return new TestClusterEParametrosProjection(
                    clusterMateriaisDemandPlanningOptional,
                    clusterLocations,
                    demandPlanningGenerateForecastForDiscontinuedMaterials);

        }

    }

    private static class NullClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return null;

        }
    }

    private static class MissingGlobalParametersClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return new ClusterEParametrosProjection();

        }
    }

    private static class TestClusterEParametrosProjection extends ClusterEParametrosProjection {

        private final Optional<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningOptional;
        private final ClusterLocations clusterLocations;

        private TestClusterEParametrosProjection(
                Optional<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningOptional,
                ClusterLocations clusterLocations,
                boolean demandPlanningGenerateForecastForDiscontinuedMaterials) {

            this.parametrosGlobais = new ParametrosGlobais();
            this.parametrosGlobais.setDiasHistoricosForecastEstatistico(120);
            this.parametrosGlobais.setDpGeraForecastParaDescontinuados(
                    demandPlanningGenerateForecastForDiscontinuedMaterials);
            this.clusterMateriaisDemandPlanningOptional = clusterMateriaisDemandPlanningOptional;
            this.clusterLocations = clusterLocations;

        }

        @Override
        public Optional<ClusterProdutosDemandPlanning> getClusterMateriaisDemandPlanningDeId(Long clusterMateriaisId) {

            return clusterMateriaisDemandPlanningOptional;

        }

        @Override
        public ClusterLocations getClusterLocationsDeId(Long clusterLocationsId) {

            return clusterLocations;

        }

    }

    private static Object getEnterpriseGeneralFieldValue(Field field) {

        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        if ("regressionTimeSeries".equals(fieldName)) {
            return List.of("support-series-id");
        }
        if ("numberOfDaysCurrentLevelAsAverageOfHistoricalStl".equals(fieldName)) {
            return 180;
        }
        if ("daysAsNewMaterial".equals(fieldName)) {
            return 30;
        }
        if (Boolean.class.equals(fieldType)) {
            return true;
        }
        if (Long.class.equals(fieldType)) {
            return 1L;
        }
        if (Integer.class.equals(fieldType)) {
            return 1;
        }
        if (Double.class.equals(fieldType)) {
            return 0.1d;
        }
        return null;

    }

    private static Object getEnterpriseForecastFieldValue(Field field) {

        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        if ("statisticalModel".equals(fieldName)) {
            return Constantes.DPModeloEstatistico.CHRONOS;
        }
        if ("splitModel".equals(fieldName)) {
            return Constantes.DPModeloSplit.HTS;
        }
        if ("upliftModel".equals(fieldName)) {
            return Constantes.DPModeloUplift.ALAVANCAGEM_EVENTO;
        }
        if ("smoothingModel".equals(fieldName)) {
            return Constantes.DPModeloNormalizacao.PERCENTIS;
        }
        if ("daysSmoothingModel".equals(fieldName)) {
            return 180;
        }
        if ("smoothingUpperPercentile".equals(fieldName)) {
            return 90.0d;
        }
        if ("smoothingLowerPercentile".equals(fieldName)) {
            return 5.0d;
        }
        if ("prophetAutoSeasonalityPriorScale".equals(fieldName)
                || "prophetAutoChangepointPriorScale".equals(fieldName)
                || "prophetAutoYearlyFourierOrder".equals(fieldName)) {
            return false;
        }
        if (Boolean.class.equals(fieldType)) {
            return true;
        }
        if (Integer.class.equals(fieldType)) {
            return 1;
        }
        if (Double.class.equals(fieldType)) {
            return 1.0d;
        }
        return null;

    }

}
