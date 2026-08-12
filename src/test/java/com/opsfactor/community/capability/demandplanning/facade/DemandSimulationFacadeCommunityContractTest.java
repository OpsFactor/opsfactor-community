package com.opsfactor.community.capability.demandplanning.facade;

import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningClusterLevelConfigurationDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningForecastParametersDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningGeneralParametersDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.dto.DemandPlanningPreviaForecastRequestDTO;
import com.opsfactor.community.capability.demandplanning.configuration.facade.mapper.DemandPlanningConfigurationMapper;
import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterMateriais;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosGeraisDemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosAgregacaoForecast;
import com.opsfactor.community.capability.demandplanning.configuration.projection.forecast.ParametrosMediaMovel;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionFactory;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanForecastProjection;
import com.opsfactor.community.capability.cluster.repository.material.ClusterMateriaisRepository;
import com.opsfactor.community.capability.demandplanning.configuration.repository.ParametrosDemandPlanNivelClusterRepository;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.capability.cluster.service.ClusterLocationService;
import com.opsfactor.community.capability.demandplanning.facade.dto.SimulatedDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.facade.mapper.DemandAnalysisMapper;
import com.opsfactor.community.capability.demandplanning.service.DemandPlanningService;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.persistence.NoResultException;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class DemandSimulationFacadeCommunityContractTest {

    @Test
    public void saveParametrosDemandPlanningShouldRejectEnterpriseConfigurationBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                new DemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters =
                new DemandPlanningGeneralParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters =
                new DemandPlanningForecastParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.statisticalModel =
                Constantes.DPModeloEstatistico.CHRONOS;

        /*
         * Apenas o mapper e injetado: se o service tentar acessar projection ou
         * repository antes da validacao, este teste quebrara com NullPointer
         * em vez de RequiresEnterpriseVersionException.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandSimulationFrontService.saveParametrosDemandPlanning(demandPlanningClusterLevelConfigurationDTO));

    }

    @Test
    public void saveParametrosDemandPlanningShouldRejectEnterpriseSplitBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getEnterpriseHtsDemandPlanningClusterLevelConfigurationDTO();

        /*
         * Historical Sales e o unico split Community. Como `splitModel` fica
         * visivel no OpenAPI com apenas esse valor, qualquer payload manual
         * com HTS precisa falhar antes de repository/factory.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandSimulationFrontService.saveParametrosDemandPlanning(
                        demandPlanningClusterLevelConfigurationDTO));

    }

    @Test
    public void saveParametrosDemandPlanningShouldRejectMissingConfigurationPayloadBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());

        /*
         * Payload ausente e erro de contrato da borda REST/service. O teste
         * injeta apenas o mapper para garantir que nenhum repository ou
         * projection precise existir para a mensagem ser deterministica.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandSimulationFrontService.saveParametrosDemandPlanning(null));
        Assertions.assertEquals(
                "Demand Planning cluster-level configuration DTO is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveParametrosDemandPlanningShouldRejectMissingIdentityBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanExecutionProfileId = " ";

        /*
         * Configuracao Community valida, mas sem perfil de execucao nao ha
         * chave funcional para buscar parametros. O erro precisa sair antes de
         * qualquer repository para nao depender de detalhes do Spring Data.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandSimulationFrontService.saveParametrosDemandPlanning(
                        demandPlanningClusterLevelConfigurationDTO));
        Assertions.assertEquals(
                "Demand Planning execution profile id is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void saveParametrosDemandPlanningShouldFailExplicitlyWhenMaterialClusterDoesNotExistBeforeRepositoryAccess() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "perfilExecucaoDemandPlanRepository",
                getPerfilExecucaoDemandPlanRepositoryStub(Optional.of(new PerfilExecucaoDemandPlan())));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory(
                        Optional.empty(),
                        new ClusterLocations()));

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanExecutionProfileId = "DP_PROFILE";
        demandPlanningClusterLevelConfigurationDTO.locationClusterId = 12L;
        demandPlanningClusterLevelConfigurationDTO.materialClusterId = 13L;

        /*
         * O repository de parametros nao e injetado. Cluster material ausente
         * deve falhar antes de tentar buscar/criar parametros cluster-level.
         */
        NoResultException noResultException = Assertions.assertThrows(
                NoResultException.class,
                () -> demandSimulationFrontService.saveParametrosDemandPlanning(
                        demandPlanningClusterLevelConfigurationDTO));

        Assertions.assertEquals(
                "Demand Planning Material Cluster 13 not found in parameter projection",
                noResultException.getMessage());

    }

    @Test
    public void saveParametrosDemandPlanningShouldRejectNullClusterLevelParametersOptionalBeforeCreationFallback() {

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan();
        perfilExecucaoDemandPlan.setId("DP_PROFILE");
        ClusterMateriais clusterMateriaisDemandPlanning =
                new ClusterMateriais();
        clusterMateriaisDemandPlanning.setId(13L);
        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(12L);

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "perfilExecucaoDemandPlanRepository",
                getPerfilExecucaoDemandPlanRepositoryStub(Optional.of(perfilExecucaoDemandPlan)));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory(
                        Optional.of(clusterMateriaisDemandPlanning),
                        clusterLocations));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "parametrosDemandPlanNivelClusterRepository",
                getParametrosDemandPlanNivelClusterRepositoryComFindRetornando(null));

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();

        /*
         * Optional.empty() cria o primeiro cadastro cluster-level. Optional
         * nulo nao pode cair nesse fallback, pois mascararia repository
         * quebrado como configuracao nova.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandSimulationFrontService.saveParametrosDemandPlanning(
                        demandPlanningClusterLevelConfigurationDTO));
        Assertions.assertEquals(
                "Demand Planning cluster-level parameter repository returned null Optional for Community profile DP_PROFILE, material cluster 13 and location cluster 12.",
                illegalStateException.getMessage());

    }

    @Test
    public void saveParametrosDemandPlanningShouldRejectBrokenSavedClusterLevelParameterSnapshot() {

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();

        DemandSimulationFacade demandSimulationFrontServiceComSnapshotNulo =
                getDemandSimulationFrontServiceParaSaveParametros(null);

        IllegalStateException snapshotNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandSimulationFrontServiceComSnapshotNulo.saveParametrosDemandPlanning(
                        demandPlanningClusterLevelConfigurationDTO));
        Assertions.assertEquals(
                "Saved Demand Planning cluster-level parameter snapshot is required.",
                snapshotNuloException.getMessage());

        DemandSimulationFacade demandSimulationFrontServiceComSnapshotSemChave =
                getDemandSimulationFrontServiceParaSaveParametros(
                        new ParametrosDemandPlanNivelCluster());

        IllegalStateException snapshotSemChaveException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandSimulationFrontServiceComSnapshotSemChave.saveParametrosDemandPlanning(
                        demandPlanningClusterLevelConfigurationDTO));
        Assertions.assertEquals(
                "Saved Demand Planning cluster-level parameter key is required.",
                snapshotSemChaveException.getMessage());

    }

    @Test
    public void saveParametrosDemandPlanningShouldRejectSavedClusterLevelParameterWithoutFunctionalIds() {

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan();
        perfilExecucaoDemandPlan.setId("DP_PROFILE");
        ClusterMateriais clusterMateriaisDemandPlanning =
                new ClusterMateriais();
        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(12L);
        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelClusterSemMaterialId =
                new ParametrosDemandPlanNivelCluster(
                        new ParametrosDemandPlanNivelCluster.ParametrosDemandPlanNivelClusterCompositeKey(
                                perfilExecucaoDemandPlan,
                                clusterMateriaisDemandPlanning,
                                clusterLocations));

        DemandSimulationFacade demandSimulationFrontServiceComMaterialSemId =
                getDemandSimulationFrontServiceParaSaveParametros(
                        parametrosDemandPlanNivelClusterSemMaterialId);

        IllegalStateException materialSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandSimulationFrontServiceComMaterialSemId.saveParametrosDemandPlanning(
                        demandPlanningClusterLevelConfigurationDTO));
        Assertions.assertEquals(
                "Saved Demand Planning cluster-level material cluster id is required.",
                materialSemIdException.getMessage());

    }

    @Test
    public void getSimulatedDemandPlanDTOShouldRejectNullForecastProjectionResultBeforeMapper() {

        DemandSimulationFacade demandSimulationFrontService =
                getDemandSimulationFrontServiceParaSimulacaoCompleta(
                        null,
                        new SimulatedDemandPlanDTO());

        /*
         * A simulacao reaproveita o service de Demand Planning para gerar a
         * mesma fotografia de forecast da rodada real. Se esse service devolver
         * nulo, a borda front deve falhar antes do mapper para nao transformar
         * snapshot quebrado em NPE ou resposta parcial.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandSimulationFrontService.getSimulatedDemandPlanDTO(
                        getCommunityDemandPlanningPreviaForecastRequestDTO()));

        Assertions.assertEquals(
                "Demand Planning forecast simulation requires forecast projection result.",
                illegalStateException.getMessage());

    }

    @Test
    public void getSimulatedDemandPlanDTOShouldRejectNullMapperResultBeforeDecoratingResponse() {

        DemandSimulationFacade demandSimulationFrontService =
                getDemandSimulationFrontServiceParaSimulacaoCompleta(
                        List.of(),
                        null);

        /*
         * Lista vazia de projections e uma simulacao valida para um recorte sem
         * DFUs. DTO nulo do mapper nao e valido: a tela receberia sucesso sem
         * contrato minimo de resposta e os decoradores de cluster/calendario
         * quebrariam sem contexto funcional.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandSimulationFrontService.getSimulatedDemandPlanDTO(
                        getCommunityDemandPlanningPreviaForecastRequestDTO()));

        Assertions.assertEquals(
                "Demand Planning forecast simulation mapper result is required.",
                illegalStateException.getMessage());

    }

    @Test
    public void getSimulatedDemandPlanDTOShouldRejectEnterpriseConfigurationBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());

        DemandPlanningPreviaForecastRequestDTO demandPlanningPreviaForecastRequestDTO =
                new DemandPlanningPreviaForecastRequestDTO();
        demandPlanningPreviaForecastRequestDTO.demandPlanningConfiguration =
                getEnterpriseChronosDemandPlanningClusterLevelConfigurationDTO();

        /*
         * Sem repositories/factories/data de referencia: modelo Enterprise deve
         * ser bloqueado antes de qualquer preparacao de simulacao.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandSimulationFrontService.getSimulatedDemandPlanDTO(demandPlanningPreviaForecastRequestDTO));

    }

    @Test
    public void getSimulatedDemandPlanDTOShouldKeepBudgetAsForecastBlockedAtCommunitySimulationBoundary() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new BudgetSimulationConfigurationMapper());

        DemandPlanningPreviaForecastRequestDTO demandPlanningPreviaForecastRequestDTO =
                new DemandPlanningPreviaForecastRequestDTO();
        demandPlanningPreviaForecastRequestDTO.demandPlanningConfiguration =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningPreviaForecastRequestDTO.demandPlanningConfiguration
                .demandPlanningForecastParameters.statisticalModel =
                Constantes.DPModeloEstatistico.BUDGET_DECOMPOSITION;

        /*
         * O mapper Community real tambem bloqueia Budget. Este stub isola o
         * gate da simulacao para provar que a borda se mantem segura mesmo se
         * a validacao anterior for especializada por um overlay.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandSimulationFrontService.getSimulatedDemandPlanDTO(
                        demandPlanningPreviaForecastRequestDTO));

        Assertions.assertEquals(
                "Budget as Forecast is not supported by Demand Planning forecast simulation.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getSimulatedDemandPlanDTOShouldRejectMissingRequestPayload() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandSimulationFrontService.getSimulatedDemandPlanDTO(null));
        Assertions.assertEquals(
                "Demand Planning simulation request is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getSimulatedDemandPlanDTOShouldRejectMissingConfigurationPayloadBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());

        DemandPlanningPreviaForecastRequestDTO demandPlanningPreviaForecastRequestDTO =
                new DemandPlanningPreviaForecastRequestDTO();

        /*
         * A request existe, mas a configuracao cluster-level nao. O mapper
         * centraliza essa validacao para save e simulacao manterem o mesmo
         * contrato publico.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandSimulationFrontService.getSimulatedDemandPlanDTO(
                        demandPlanningPreviaForecastRequestDTO));
        Assertions.assertEquals(
                "Demand Planning cluster-level configuration DTO is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getSimulatedDemandPlanDTOShouldRejectMissingReferenceDateBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());

        DemandPlanningPreviaForecastRequestDTO demandPlanningPreviaForecastRequestDTO =
                new DemandPlanningPreviaForecastRequestDTO();
        demandPlanningPreviaForecastRequestDTO.demandPlanningConfiguration =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();

        /*
         * A configuracao Community valida com sucesso, mas a data de referencia
         * da simulacao e obrigatoria antes de qualquer repository/factory. Sem
         * esse guard, o fluxo tentaria buscar clusters antes de acusar payload
         * incompleto.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandSimulationFrontService.getSimulatedDemandPlanDTO(
                        demandPlanningPreviaForecastRequestDTO));
        Assertions.assertEquals(
                "Demand Planning simulation reference date is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getSimulatedDemandPlanDTOShouldRejectEnterpriseSplitBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());

        DemandPlanningPreviaForecastRequestDTO demandPlanningPreviaForecastRequestDTO =
                new DemandPlanningPreviaForecastRequestDTO();
        demandPlanningPreviaForecastRequestDTO.demandPlanningConfiguration =
                getEnterpriseHtsDemandPlanningClusterLevelConfigurationDTO();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandSimulationFrontService.getSimulatedDemandPlanDTO(
                        demandPlanningPreviaForecastRequestDTO));

    }

    @Test
    public void getSimulatedDemandPlanDTOShouldRejectMissingMaterialClusterAfterReferenceDateBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());

        DemandPlanningPreviaForecastRequestDTO demandPlanningPreviaForecastRequestDTO =
                new DemandPlanningPreviaForecastRequestDTO();
        demandPlanningPreviaForecastRequestDTO.referenceDate = LocalDate.of(2026, 6, 25);
        demandPlanningPreviaForecastRequestDTO.demandPlanningConfiguration =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();
        demandPlanningPreviaForecastRequestDTO.demandPlanningConfiguration.materialClusterId = null;

        /*
         * Depois que a simulacao ja confirmou data de referencia e recorte
         * Community, os ids de cluster precisam ser validados antes de tocar
         * em repositories de cluster ou factories de sales/projection.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandSimulationFrontService.getSimulatedDemandPlanDTO(
                        demandPlanningPreviaForecastRequestDTO));
        Assertions.assertEquals(
                "Demand Planning material cluster id is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getDemandPlanningConfigurationDTOShouldRejectMissingPathKeysBeforeRepositories() {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandSimulationFrontService.getDemandPlanningConfigurationDTO(
                        " ",
                        10L,
                        20L));
        Assertions.assertEquals(
                "Demand Planning execution profile id is required",
                illegalArgumentException.getMessage());

        IllegalArgumentException locationClusterException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandSimulationFrontService.getDemandPlanningConfigurationDTO(
                        "DP_PROFILE",
                        null,
                        20L));
        Assertions.assertEquals(
                "Demand Planning location cluster id is required",
                locationClusterException.getMessage());

    }

    private DemandSimulationFacade getDemandSimulationFrontService() {

        return new DemandSimulationFacade();

    }

    private DemandSimulationFacade getDemandSimulationFrontServiceParaSaveParametros(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelClusterSalvos) {

        DemandSimulationFacade demandSimulationFrontService = getDemandSimulationFrontService();
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = new PerfilExecucaoDemandPlan();
        perfilExecucaoDemandPlan.setId("DP_PROFILE");
        ClusterMateriais clusterMateriaisDemandPlanning =
                new ClusterMateriais();
        clusterMateriaisDemandPlanning.setId(13L);
        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(12L);

        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new DemandPlanningConfigurationMapper());
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "perfilExecucaoDemandPlanRepository",
                getPerfilExecucaoDemandPlanRepositoryStub(Optional.of(perfilExecucaoDemandPlan)));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory(
                        Optional.of(clusterMateriaisDemandPlanning),
                        clusterLocations));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "parametrosDemandPlanNivelClusterRepository",
                getParametrosDemandPlanNivelClusterRepositoryStub(
                        parametrosDemandPlanNivelClusterSalvos));
        return demandSimulationFrontService;

    }

    private DemandSimulationFacade getDemandSimulationFrontServiceParaSimulacaoCompleta(
            List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionList,
            SimulatedDemandPlanDTO simulatedDemandPlanDTO) {

        DemandSimulationFacade demandSimulationFrontService =
                getDemandSimulationFrontService();
        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan =
                new PerfilExecucaoDemandPlan("DP_PROFILE");
        ClusterMateriais clusterMateriaisDemandPlanning =
                new ClusterMateriais();
        clusterMateriaisDemandPlanning.setId(13L);
        ClusterLocations clusterLocations = new ClusterLocations();
        clusterLocations.setId(12L);
        Location location = new Location("LOCATION");
        Produto material = new Produto("MATERIAL");
        TestUnidadeMedidaProjection unidadeMedidaProjection =
                new TestUnidadeMedidaProjection(parametrosGlobais);
        Calendario calendario = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                getCommunityDemandPlanningPreviaForecastRequestDTO().referenceDate.atStartOfDay(),
                0,
                2,
                1,
                0);
        SalesProjectionLocationMaterialData salesProjection =
                SalesProjectionLocationMaterialData.builder()
                        .calendario(calendario)
                        .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                        .build();
        ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection =
                getParametrosDemandPlanNivelClusterProjectionParaSimulacao(
                        perfilExecucaoDemandPlan,
                        clusterMateriaisDemandPlanning,
                        clusterLocations,
                        unidadeMedida,
                        parametrosGlobais);

        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningConfigurationMapper",
                new TestDemandPlanningConfigurationMapper(
                        parametrosDemandPlanNivelClusterProjection));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "clusterMateriaisDemandPlanningRepository",
                getClusterMateriaisDemandPlanningRepositoryStub(
                        Optional.of(clusterMateriaisDemandPlanning)));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "clusterLocationService",
                new TestClusterLocationService(Optional.of(clusterLocations)));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "unidadeMedidaProjectionFactory",
                new TestUnidadeMedidaProjectionFactory(unidadeMedidaProjection));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "clusterEParametrosProjectionFactory",
                new TestClusterEParametrosProjectionFactory(
                        new TestClusterEParametrosProjection(
                                Optional.of(clusterMateriaisDemandPlanning),
                                clusterLocations,
                                parametrosGlobais,
                                Set.of(material),
                                Set.of(location))));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "salesProjectionFactory",
                new TestSalesProjectionFactory(salesProjection));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandPlanningService",
                new TestDemandPlanningService(demandPlanForecastProjectionList));
        ReflectionTestUtils.setField(
                demandSimulationFrontService,
                "demandAnalysisMapper",
                new TestDemandAnalysisMapper(simulatedDemandPlanDTO));
        return demandSimulationFrontService;

    }

    private ParametrosDemandPlanNivelClusterProjection getParametrosDemandPlanNivelClusterProjectionParaSimulacao(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            ClusterMateriais clusterMateriaisDemandPlanning,
            ClusterLocations clusterLocations,
            UnidadeMedida unidadeMedida,
            ParametrosGlobais parametrosGlobais) {

        ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection =
                new ParametrosGeraisDemandPlanningProjection(
                        true,
                        new ParametrosAgregacaoForecast(
                                Constantes.DPNivelAgregacao.BOTTOM_UP,
                                Constantes.DPNivelAgregacao.BOTTOM_UP),
                        2,
                        false,
                        false,
                        0,
                        unidadeMedida,
                        false,
                        parametrosGlobais);
        ParametrosForecastProjection parametrosForecastProjection =
                new ParametrosForecastProjection(
                        Constantes.DPModeloEstatistico.MM,
                        new ParametrosMediaMovel(1),
                        null,
                        null,
                        null,
                        Constantes.DPModeloSplit.HISTORICAL_SALES,
                        1);

        return new TestParametrosDemandPlanNivelClusterProjection(
                perfilExecucaoDemandPlan,
                clusterLocations,
                clusterMateriaisDemandPlanning,
                parametrosGeraisDemandPlanningProjection,
                parametrosForecastProjection);

    }

    private static ClusterMateriaisRepository getClusterMateriaisDemandPlanningRepositoryStub(
            Optional<ClusterMateriais> clusterMateriaisDemandPlanningOptional) {

        return (ClusterMateriaisRepository) Proxy.newProxyInstance(
                ClusterMateriaisRepository.class.getClassLoader(),
                new Class<?>[]{ClusterMateriaisRepository.class},
                (proxy, method, args) -> {

                    if ("findById".equals(method.getName())) {
                        return clusterMateriaisDemandPlanningOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ClusterProdutosDemandPlanningRepositoryStub";
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

    private static ParametrosDemandPlanNivelClusterRepository
    getParametrosDemandPlanNivelClusterRepositoryStub(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelClusterSalvos) {

        return (ParametrosDemandPlanNivelClusterRepository) Proxy.newProxyInstance(
                ParametrosDemandPlanNivelClusterRepository.class.getClassLoader(),
                new Class<?>[]{ParametrosDemandPlanNivelClusterRepository.class},
                (proxy, method, args) -> {

                    if (method.getName().startsWith(
                            "findByParametrosClusterProdutosDemandPlanningClusterLocationsCompositeKey")) {
                        return Optional.empty();
                    }
                    if ("save".equals(method.getName())) {
                        return parametrosDemandPlanNivelClusterSalvos;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ParametrosDemandPlanNivelClusterRepositoryStub";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao suportado pelo stub do teste: " + method.getName());

                });

    }

    private static ParametrosDemandPlanNivelClusterRepository
    getParametrosDemandPlanNivelClusterRepositoryComFindRetornando(
            Optional<ParametrosDemandPlanNivelCluster> optionalParametrosDemandPlanNivelCluster) {

        return (ParametrosDemandPlanNivelClusterRepository) Proxy.newProxyInstance(
                ParametrosDemandPlanNivelClusterRepository.class.getClassLoader(),
                new Class<?>[]{ParametrosDemandPlanNivelClusterRepository.class},
                (proxy, method, args) -> {

                    if (method.getName().startsWith(
                            "findByParametrosClusterProdutosDemandPlanningClusterLocationsCompositeKey")) {
                        return optionalParametrosDemandPlanNivelCluster;
                    }
                    if ("toString".equals(method.getName())) {
                        return "ParametrosDemandPlanNivelClusterRepository find controlado para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao suportado pelo stub do teste: " + method.getName());

                });

    }

    private static class TestClusterLocationService extends ClusterLocationService {

        private final Optional<ClusterLocations> clusterLocationsOptional;

        private TestClusterLocationService(
                Optional<ClusterLocations> clusterLocationsOptional) {

            this.clusterLocationsOptional = clusterLocationsOptional;

        }

        @Override
        public Optional<ClusterLocations> getClusterLocation(long id) {

            return clusterLocationsOptional;

        }

    }

    private static class TestClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        private TestClusterEParametrosProjectionFactory(
                ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        private TestClusterEParametrosProjectionFactory(
                Optional<ClusterMateriais> clusterMateriaisDemandPlanningOptional,
                ClusterLocations clusterLocations) {

            this(new TestClusterEParametrosProjection(
                    clusterMateriaisDemandPlanningOptional,
                    clusterLocations,
                    new ParametrosGlobais()));

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return clusterEParametrosProjection;

        }

    }

    private static class TestClusterEParametrosProjection extends ClusterEParametrosProjection {

        private final Optional<ClusterMateriais> clusterMateriaisDemandPlanningOptional;
        private final ClusterLocations clusterLocations;
        private final ParametrosGlobais parametrosGlobais;
        private final Set<Produto> materialSet;
        private final Set<Location> locationSet;

        private TestClusterEParametrosProjection(
                Optional<ClusterMateriais> clusterMateriaisDemandPlanningOptional,
                ClusterLocations clusterLocations,
                ParametrosGlobais parametrosGlobais) {

            this(
                    clusterMateriaisDemandPlanningOptional,
                    clusterLocations,
                    parametrosGlobais,
                    Set.of(),
                    Set.of());

        }

        private TestClusterEParametrosProjection(
                Optional<ClusterMateriais> clusterMateriaisDemandPlanningOptional,
                ClusterLocations clusterLocations,
                ParametrosGlobais parametrosGlobais,
                Set<Produto> materialSet,
                Set<Location> locationSet) {

            this.clusterMateriaisDemandPlanningOptional = clusterMateriaisDemandPlanningOptional;
            this.clusterLocations = clusterLocations;
            this.parametrosGlobais = parametrosGlobais;
            this.materialSet = materialSet;
            this.locationSet = locationSet;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

        @Override
        public Optional<ClusterMateriais> getClusterMateriaisDemandPlanningDeId(Long clusterMateriaisId) {

            return clusterMateriaisDemandPlanningOptional;

        }

        @Override
        public ClusterLocations getClusterLocationsDeId(Long clusterLocationsId) {

            return clusterLocations;

        }

        @Override
        public Set<Produto> getMateriaisDeClusterMateriaisDemandPlanning(
                ClusterMateriais clusterMateriaisDemandPlanning,
                boolean somenteDfusAtivos) {

            return materialSet;

        }

        @Override
        public Set<Produto> getMateriaisDeClusterProdutosDemandPlanning(
                ClusterMateriais clusterMateriais,
                boolean somenteMateriaisAtivos) {

            return materialSet;

        }

        @Override
        public Set<Location> getLocationsDeClusterLocations(
                ClusterLocations clusterLocations,
                boolean somenteLocationsAtivas) {

            return locationSet;

        }

    }

    private static class TestDemandPlanningConfigurationMapper extends DemandPlanningConfigurationMapper {

        private final ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection;

        private TestDemandPlanningConfigurationMapper(
                ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection) {

            this.parametrosDemandPlanNivelClusterProjection = parametrosDemandPlanNivelClusterProjection;

        }

        @Override
        public ParametrosDemandPlanNivelClusterProjection getProjectionDeDto(
                DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

            return parametrosDemandPlanNivelClusterProjection;

        }

    }

    private static class BudgetSimulationConfigurationMapper extends DemandPlanningConfigurationMapper {

        @Override
        public void validaDemandPlanningClusterLevelConfigurationDTOCommunity(
                DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO) {

            /* Stub intencional: o teste verifica apenas o gate da simulacao. */

        }

    }

    private static class TestParametrosDemandPlanNivelClusterProjection
            extends ParametrosDemandPlanNivelClusterProjection {

        private final ParametrosForecastProjection parametrosForecastProjection;

        private TestParametrosDemandPlanNivelClusterProjection(
                PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
                ClusterLocations clusterLocations,
                ClusterMateriais clusterMateriaisDemandPlanning,
                ParametrosGeraisDemandPlanningProjection parametrosGeraisDemandPlanningProjection,
                ParametrosForecastProjection parametrosForecastProjection) {

            super(
                    perfilExecucaoDemandPlan,
                    clusterLocations,
                    clusterMateriaisDemandPlanning,
                    parametrosGeraisDemandPlanningProjection);
            this.parametrosForecastProjection = parametrosForecastProjection;

        }

        @Override
        public ParametrosForecastProjection getParametrosForecastProjection(
                Location location,
                Produto material) {

            return parametrosForecastProjection;

        }

    }

    private static class TestSalesProjectionFactory extends SalesProjectionFactory {

        private final SalesProjectionLocationMaterialData salesProjection;

        private TestSalesProjectionFactory(
                SalesProjectionLocationMaterialData salesProjection) {

            this.salesProjection = salesProjection;

        }

        @Override
        public SalesProjectionLocationMaterialData getSalesProjectionLocationMaterialData(
                Constantes.TipoDocumentoVenda tipoDocumentoVenda,
                Calendario calendario,
                Set<Location> locations,
                Set<Produto> produtos,
                UnidadeMedidaProjection unidadeMedidaProjection,
                ClusterEParametrosProjection clusterEParametrosProjection,
                UnidadeMedida unidadePadrao) {

            return salesProjection;

        }

    }

    private static class TestDemandPlanningService extends DemandPlanningService {

        private final List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionList;

        private TestDemandPlanningService(
                List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionList) {

            this.demandPlanForecastProjectionList = demandPlanForecastProjectionList;

        }

        @Override
        public List<? extends DemandPlanForecastProjection> geraDemandPlanForecastProjectionsExecucaoComForecast(
                Calendario calendario,
                ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection,
                MaterialProjection materialProjection,
                LocationProjection locationProjection,
                SalesProjectionLocationMaterialData salesProjection,
                ClusterEParametrosProjection clusterEParametrosProjection,
                boolean preencheHorizonteForecastComDemandaHistorica) {

            return demandPlanForecastProjectionList;

        }

    }

    private static class TestDemandAnalysisMapper extends DemandAnalysisMapper {

        private final SimulatedDemandPlanDTO simulatedDemandPlanDTO;

        private TestDemandAnalysisMapper(
                SimulatedDemandPlanDTO simulatedDemandPlanDTO) {

            this.simulatedDemandPlanDTO = simulatedDemandPlanDTO;

        }

        @Override
        public SimulatedDemandPlanDTO demandPlanProjectionToDemandModelSetupDTO(
                DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO,
                Calendario calendario,
                List<? extends DemandPlanForecastProjection> demandPlanForecastProjectionsExecucao,
                SalesProjectionLocationMaterialData salesProjection) {

            return simulatedDemandPlanDTO;

        }

    }

    private static class TestUnidadeMedidaProjectionFactory extends UnidadeMedidaProjectionFactory {

        private final UnidadeMedidaProjection unidadeMedidaProjection;

        private TestUnidadeMedidaProjectionFactory(
                UnidadeMedidaProjection unidadeMedidaProjection) {

            this.unidadeMedidaProjection = unidadeMedidaProjection;

        }

        @Override
        public UnidadeMedidaProjection getUnidadeMedidaProjectionCompletoDeCache() {

            return unidadeMedidaProjection;

        }

    }

    private static class TestUnidadeMedidaProjection extends UnidadeMedidaProjection {

        private final ParametrosGlobais parametrosGlobais;

        private TestUnidadeMedidaProjection(
                ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

    }

    private static DemandPlanningClusterLevelConfigurationDTO getCommunityDemandPlanningClusterLevelConfigurationDTO() {

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                new DemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters =
                new DemandPlanningGeneralParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters =
                new DemandPlanningForecastParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.statisticalModel =
                Constantes.DPModeloEstatistico.MM;
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.splitModel =
                Constantes.DPModeloSplit.HISTORICAL_SALES;
        demandPlanningClusterLevelConfigurationDTO.demandPlanExecutionProfileId = "DP_PROFILE";
        demandPlanningClusterLevelConfigurationDTO.locationClusterId = 12L;
        demandPlanningClusterLevelConfigurationDTO.materialClusterId = 13L;

        return demandPlanningClusterLevelConfigurationDTO;

    }

    private static DemandPlanningPreviaForecastRequestDTO getCommunityDemandPlanningPreviaForecastRequestDTO() {

        DemandPlanningPreviaForecastRequestDTO demandPlanningPreviaForecastRequestDTO =
                new DemandPlanningPreviaForecastRequestDTO();
        demandPlanningPreviaForecastRequestDTO.referenceDate = LocalDate.of(2026, 6, 25);
        demandPlanningPreviaForecastRequestDTO.demandPlanningConfiguration =
                getCommunityDemandPlanningClusterLevelConfigurationDTO();

        return demandPlanningPreviaForecastRequestDTO;

    }

    private static DemandPlanningClusterLevelConfigurationDTO getEnterpriseChronosDemandPlanningClusterLevelConfigurationDTO() {

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                new DemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters =
                new DemandPlanningGeneralParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters =
                new DemandPlanningForecastParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.statisticalModel =
                Constantes.DPModeloEstatistico.CHRONOS;

        return demandPlanningClusterLevelConfigurationDTO;

    }

    private static DemandPlanningClusterLevelConfigurationDTO getEnterpriseHtsDemandPlanningClusterLevelConfigurationDTO() {

        DemandPlanningClusterLevelConfigurationDTO demandPlanningClusterLevelConfigurationDTO =
                new DemandPlanningClusterLevelConfigurationDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningGeneralParameters =
                new DemandPlanningGeneralParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters =
                new DemandPlanningForecastParametersDTO();
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.statisticalModel =
                Constantes.DPModeloEstatistico.MM;
        demandPlanningClusterLevelConfigurationDTO.demandPlanningForecastParameters.splitModel =
                Constantes.DPModeloSplit.HTS;

        return demandPlanningClusterLevelConfigurationDTO;

    }

}
