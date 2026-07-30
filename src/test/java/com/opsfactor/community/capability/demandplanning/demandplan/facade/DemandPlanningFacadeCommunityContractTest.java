package com.opsfactor.community.capability.demandplanning.demandplan.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlanItem;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.factory.ParametrosDemandPlanningProjectionFactory;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.demandplanning.demandplan.repository.DemandPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.platform.exception.DemandPlanException;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.mapper.DemandPlanAutoMapper;
import com.opsfactor.community.capability.demandplanning.service.DemandPlanningService;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanItemDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanPeriodDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.VersaoDemandPlanDTO;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javatuples.Quartet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Valida regras Community do Planning Book de demanda sem subir Spring.
 *
 * <p>As entidades ainda carregam colunas e enums compartilhados com o
 * Enterprise, mas a colaboracao aberta deve permitir apenas os ajustes
 * Community. Este teste protege essa borda contra payloads manuais do front
 * compartilhado.</p>
 */
class DemandPlanningFacadeCommunityContractTest {

    @Test
    void getDemandPlanPeriodDTOListShouldReturnPersistedCalendarWithoutAdditionalDependencies() throws Exception {

        DemandPlan demandPlan = criaDemandPlanListagem(77L, "DP-PROFILE", LocalDateTime.of(2026, 1, 1, 8, 0));
        demandPlan.setTamanhoBucket(Constantes.TamanhoBucket.DIARIO);
        demandPlan.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));
        demandPlan.setDataFimPlano(LocalDateTime.of(2026, 1, 2, 23, 59, 59));
        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        setField(demandPlanningFrontService, "demandPlanRepository", getDemandPlanRepositoryComDemandPlan(demandPlan));

        List<DemandPlanPeriodDTO> periods = demandPlanningFrontService.getDemandPlanPeriodDTOList(77L);

        Assertions.assertEquals(2, periods.size());
        Assertions.assertEquals(0, periods.get(0).periodIndex);
        Assertions.assertEquals(Constantes.TamanhoBucket.DIARIO, periods.get(0).bucketSize);
        Assertions.assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), periods.get(0).referenceDate);
        Assertions.assertEquals(LocalDateTime.of(2026, 1, 2, 23, 59, 59), periods.get(1).endDateTime);

    }

    @Test
    void getDemandPlanPeriodDTOListShouldRejectIncompleteOrInvertedPersistedCalendar() throws Exception {

        DemandPlan incompleteDemandPlan = criaDemandPlanListagem(77L, "DP-PROFILE", LocalDateTime.of(2026, 1, 1, 8, 0));
        incompleteDemandPlan.setDataFimPlano(null);
        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        setField(demandPlanningFrontService, "demandPlanRepository", getDemandPlanRepositoryComDemandPlan(incompleteDemandPlan));
        DemandPlanException incompleteException = Assertions.assertThrows(
                DemandPlanException.class,
                () -> demandPlanningFrontService.getDemandPlanPeriodDTOList(77L));
        Assertions.assertTrue(incompleteException.getMessage().contains("requires persisted start and end dates"));

        DemandPlan invertedDemandPlan = criaDemandPlanListagem(77L, "DP-PROFILE", LocalDateTime.of(2026, 1, 1, 8, 0));
        invertedDemandPlan.setDataFimPlano(LocalDateTime.of(2025, 12, 31, 23, 59, 59));
        setField(demandPlanningFrontService, "demandPlanRepository", getDemandPlanRepositoryComDemandPlan(invertedDemandPlan));
        DemandPlanException invertedException = Assertions.assertThrows(
                DemandPlanException.class,
                () -> demandPlanningFrontService.getDemandPlanPeriodDTOList(77L));
        Assertions.assertTrue(invertedException.getMessage().contains("end date before start date"));

    }

    @Test
    void planningBookAggregationLevelsShouldRemainEnterpriseOnly() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.materialAggregationLevelId = "MATERIAL_LEVEL";
        configuredViewSelectionDTO.locationAggregationLevelId = "LOCATION_LEVEL";

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningFrontService.validaNiveisAgrupamentoPlanningBookCommunity(
                        configuredViewSelectionDTO));

    }

    @Test
    void serviceShouldUseExplicitAutowiredBeanFieldsWithoutDuplicateUomFactory() throws Exception {

        assertAutowiredFields(
                "demandPlanAutoMapper",
                "demandPlanningService",
                "parametrosGlobaisService",
                "keyFigureService",
                "planningBookService",
                "configuredViewService",
                "demandPlanRepository",
                "demandPlanItemRepository",
                "locationRepository",
                "configuredViewProjectionFactory",
                "conversaoUnidadeMedidaProjectionFactory",
                "clusterEParametrosProjectionFactory",
                "demandPlanProjectionFactory",
                "salesProjectionFactory",
                "keyFigureProjectionFactory",
                "parametrosDemandPlanningProjectionFactory");

        /*
         * A fachada usa uma unica projection factory de UOM. Manter dois campos
         * do mesmo bean dificulta entender qual rota calcula conversao para
         * Planning Book e tende a acumular atributo morto.
         */
        Assertions.assertThrows(
                NoSuchFieldException.class,
                () -> DemandPlanningFacade.class.getDeclaredField("unidadeMedidaProjectionFactory"));

    }

    @Test
    void demandPlanningBookServiceMethodsShouldNotExposeLegacyJavassistNotFoundException() throws Exception {

        Method getPlanningBookDTOMethod = DemandPlanningFacade.class.getDeclaredMethod(
                "getPlanningBookDTO",
                ConfiguredViewSelectionDTO.class,
                String.class);
        Method atualizaDemandPlanMethod = DemandPlanningFacade.class.getDeclaredMethod(
                "atualizaDemandPlan",
                List.class,
                String.class);

        /*
         * Plano ausente e erro funcional de DemandPlanException; payload
         * invalido falha como IllegalArgumentException. A excecao checked do
         * Javassist nao faz parte do contrato Community do Planning Book.
         */
        Assertions.assertTrue(Arrays.stream(getPlanningBookDTOMethod.getExceptionTypes())
                .noneMatch(exceptionType -> "javassist.NotFoundException".equals(exceptionType.getName())));
        Assertions.assertTrue(Arrays.stream(atualizaDemandPlanMethod.getExceptionTypes())
                .noneMatch(exceptionType -> "javassist.NotFoundException".equals(exceptionType.getName())));

    }

    @Test
    void demandPlanningBookCellUpdateShouldNotUseGenericExceptionCatch() throws Exception {

        Path demandPlanningFrontServicePath = Path.of(
                "src/main/java/com/opsfactor/community/capability/demandplanning/demandplan/facade/DemandPlanningFacade.java");
        String demandPlanningFrontServiceSource = Files.readString(
                demandPlanningFrontServicePath,
                StandardCharsets.UTF_8);

        /*
         * A atualizacao de celulas do Planning Book isola erro funcional por
         * celula, mas o metodo chamado nao declara checked exception. Manter a
         * captura em RuntimeException deixa claro que nao estamos engolindo
         * checked exceptions arbitrarias nessa borda de colaboracao Community.
         */
        Assertions.assertFalse(
                demandPlanningFrontServiceSource.contains("catch (Exception"),
                "DemandPlanningFrontService deve capturar somente RuntimeException em atualizacao por celula.");

    }

    @Test
    void getListaVersaoDemandPlanDTOShouldValidateAndSortRepositorySnapshot() throws Exception {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        DemandPlan demandPlanAntigo = criaDemandPlanListagem(
                1L,
                "DP-PROFILE",
                LocalDateTime.of(2026, 1, 1, 8, 0));
        DemandPlan demandPlanRecente = criaDemandPlanListagem(
                2L,
                "DP-PROFILE",
                LocalDateTime.of(2026, 1, 2, 8, 0));
        setField(
                demandPlanningFrontService,
                "demandPlanRepository",
                getDemandPlanRepositoryComListagem(List.of(demandPlanAntigo, demandPlanRecente)));

        /*
         * O repository nao precisa devolver as versoes ordenadas. A fachada
         * valida a identidade minima e ordena pela geracao mais recente para o
         * seletor do front.
         */
        List<VersaoDemandPlanDTO> versaoDemandPlanDTOList =
                demandPlanningFrontService.getListaVersaoDemandPlanDTO();

        Assertions.assertEquals(
                List.of(2L, 1L),
                versaoDemandPlanDTOList.stream()
                        .map(VersaoDemandPlanDTO::getId)
                        .toList());

    }

    @Test
    void versaoDemandPlanDTOShouldPublishBucketSizeFromExecutionProfileForLegacyVersionListing()
            throws Exception {

        DemandPlan demandPlan = criaDemandPlanListagem(
                1L,
                "DP-PROFILE",
                LocalDateTime.of(2026, 1, 1, 8, 0));
        demandPlan.setTamanhoBucket(Constantes.TamanhoBucket.DIARIO);
        demandPlan.getPerfilExecucaoDemandPlan().setTamanhoBucket(Constantes.TamanhoBucket.SEMANAL);

        VersaoDemandPlanDTO versaoDemandPlanDTO = new VersaoDemandPlanDTO(demandPlan);
        String versaoDemandPlanJson = new ObjectMapper().findAndRegisterModules()
                .writeValueAsString(versaoDemandPlanDTO);

        Assertions.assertEquals(Constantes.TamanhoBucket.SEMANAL, versaoDemandPlanDTO.getBucketSize());
        Assertions.assertTrue(versaoDemandPlanJson.contains("\"bucketSize\":\"Weekly\""));

    }

    @Test
    void demandPlanningSelectorsShouldFetchExecutionProfilesInTheRepositoryQuery() throws Exception {

        Path demandPlanningFrontServicePath = Path.of(
                "src/main/java/com/opsfactor/community/capability/demandplanning/demandplan/facade/DemandPlanningFacade.java");
        String demandPlanningFrontServiceSource = Files.readString(
                demandPlanningFrontServicePath,
                StandardCharsets.UTF_8);

        /*
         * Ambos os seletores criam DTOs que acessam o perfil de execucao do
         * plano. A fonte deve ser o fetch join existente, e nao findAll(),
         * para que a quantidade de versoes nao transforme a tela em N+1.
         */
        String getDemandPlanningSelectDTOSource = extraiCorpoMetodo(
                demandPlanningFrontServiceSource,
                "public DemandPlanSelectDTO getDemandPlanningSelectDTO()");
        String getListaVersaoDemandPlanDTOSource = extraiCorpoMetodo(
                demandPlanningFrontServiceSource,
                "public List<VersaoDemandPlanDTO> getListaVersaoDemandPlanDTO()");

        Assertions.assertTrue(getDemandPlanningSelectDTOSource.contains(
                "demandPlanRepository.customFindAllComPerfilExecucao()"));
        Assertions.assertFalse(getDemandPlanningSelectDTOSource.contains(
                "demandPlanRepository.findAll()"));
        Assertions.assertTrue(getListaVersaoDemandPlanDTOSource.contains(
                "demandPlanRepository.customFindAllComPerfilExecucao()"));
        Assertions.assertFalse(getListaVersaoDemandPlanDTOSource.contains(
                "demandPlanRepository.findAll()"));

    }

    @Test
    void getDemandPlanningSelectDTOShouldRejectBrokenLocationSnapshotBeforeProjection() throws Exception {

        DemandPlanningFacade demandPlanningFrontServiceComListaNula =
                new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComListaNula,
                "locationRepository",
                getLocationRepositoryComListagem(null));

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComListaNula::getDemandPlanningSelectDTO);
        Assertions.assertEquals(
                "Demand Planning selector location list snapshot is required.",
                nullListException.getMessage());

        DemandPlanningFacade demandPlanningFrontServiceComItemNulo =
                new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComItemNulo,
                "locationRepository",
                getLocationRepositoryComListagem(java.util.Collections.singletonList(null)));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComItemNulo::getDemandPlanningSelectDTO);
        Assertions.assertEquals(
                "Demand Planning selector location at index 0 is required.",
                nullItemException.getMessage());

        DemandPlanningFacade demandPlanningFrontServiceComLocationSemId =
                new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComLocationSemId,
                "locationRepository",
                getLocationRepositoryComListagem(List.of(new Location())));

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComLocationSemId::getDemandPlanningSelectDTO);
        Assertions.assertEquals(
                "Demand Planning selector location at index 0 has no id.",
                missingIdException.getMessage());

    }

    @Test
    void getListaVersaoDemandPlanDTOShouldRejectBrokenRepositorySnapshotBeforeDtoConstruction() throws Exception {

        DemandPlanningFacade demandPlanningFrontServiceComListaNula = new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComListaNula,
                "demandPlanRepository",
                getDemandPlanRepositoryComListagem(null));

        IllegalStateException listaNulaException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComListaNula::getListaVersaoDemandPlanDTO);

        Assertions.assertEquals(
                "Demand Plan repository returned null list for Demand Planning version listing.",
                listaNulaException.getMessage());

        List<DemandPlan> demandPlanListComItemNulo = new ArrayList<>();
        demandPlanListComItemNulo.add(null);
        DemandPlanningFacade demandPlanningFrontServiceComItemNulo = new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComItemNulo,
                "demandPlanRepository",
                getDemandPlanRepositoryComListagem(demandPlanListComItemNulo));

        IllegalStateException itemNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComItemNulo::getListaVersaoDemandPlanDTO);

        Assertions.assertEquals(
                "Demand Plan repository returned null item at index 0 for Demand Planning version listing.",
                itemNuloException.getMessage());

        DemandPlanningFacade demandPlanningFrontServiceComPlanoSemId = new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComPlanoSemId,
                "demandPlanRepository",
                getDemandPlanRepositoryComListagem(List.of(criaDemandPlanListagem(
                        null,
                        "DP-PROFILE",
                        LocalDateTime.of(2026, 1, 1, 8, 0)))));

        IllegalStateException planoSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComPlanoSemId::getListaVersaoDemandPlanDTO);

        Assertions.assertEquals(
                "Demand Plan repository returned item without id at index 0 for Demand Planning version listing.",
                planoSemIdException.getMessage());

        DemandPlanningFacade demandPlanningFrontServiceComPerfilAusente = new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComPerfilAusente,
                "demandPlanRepository",
                getDemandPlanRepositoryComListagem(List.of(criaDemandPlanListagem(
                        1L,
                        null,
                        LocalDateTime.of(2026, 1, 1, 8, 0)))));

        IllegalStateException perfilAusenteException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComPerfilAusente::getListaVersaoDemandPlanDTO);

        Assertions.assertEquals(
                "Demand Plan repository returned item without execution profile id at index 0 for Demand Planning version listing.",
                perfilAusenteException.getMessage());

    }

    @Test
    void getDemandPlanDTOListShouldValidateMapperSnapshotBeforeSorting() throws Exception {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        setField(
                demandPlanningFrontService,
                "demandPlanRepository",
                getDemandPlanRepositoryComListagem(List.of(criaDemandPlanListagem(
                        1L,
                        "DP-PROFILE",
                        LocalDateTime.of(2026, 1, 1, 8, 0)))));
        setField(
                demandPlanningFrontService,
                "demandPlanAutoMapper",
                getDemandPlanAutoMapperComListaSemLinhas(List.of(
                        criaDemandPlanDTOListagem(1L),
                        criaDemandPlanDTOListagem(3L),
                        criaDemandPlanDTOListagem(2L))));

        List<DemandPlanDTO> demandPlanDTOList =
                demandPlanningFrontService.getDemandPlanDTOList();

        Assertions.assertEquals(
                List.of(3L, 2L, 1L),
                demandPlanDTOList.stream()
                        .map(demandPlanDTO -> demandPlanDTO.demandPlanId)
                        .toList());

    }

    @Test
    void getDemandPlanDTOListShouldRejectBrokenMapperSnapshotBeforeSorting() throws Exception {

        DemandPlan demandPlanValido = criaDemandPlanListagem(
                1L,
                "DP-PROFILE",
                LocalDateTime.of(2026, 1, 1, 8, 0));

        DemandPlanningFacade demandPlanningFrontServiceComMapperNulo = new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComMapperNulo,
                "demandPlanRepository",
                getDemandPlanRepositoryComListagem(List.of(demandPlanValido)));
        setField(
                demandPlanningFrontServiceComMapperNulo,
                "demandPlanAutoMapper",
                getDemandPlanAutoMapperComListaSemLinhas(null));

        IllegalStateException listaNulaException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComMapperNulo::getDemandPlanDTOList);

        Assertions.assertEquals(
                "Demand Plan mapper returned null DTO list for Demand Planning DTO listing.",
                listaNulaException.getMessage());

        List<DemandPlanDTO> demandPlanDTOListComItemNulo = new ArrayList<>();
        demandPlanDTOListComItemNulo.add(null);
        DemandPlanningFacade demandPlanningFrontServiceComItemNulo = new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComItemNulo,
                "demandPlanRepository",
                getDemandPlanRepositoryComListagem(List.of(demandPlanValido)));
        setField(
                demandPlanningFrontServiceComItemNulo,
                "demandPlanAutoMapper",
                getDemandPlanAutoMapperComListaSemLinhas(demandPlanDTOListComItemNulo));

        IllegalStateException itemNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComItemNulo::getDemandPlanDTOList);

        Assertions.assertEquals(
                "Demand Plan mapper returned null DTO at index 0 for Demand Planning DTO listing.",
                itemNuloException.getMessage());

        DemandPlanningFacade demandPlanningFrontServiceComDtoSemId = new DemandPlanningFacade();
        setField(
                demandPlanningFrontServiceComDtoSemId,
                "demandPlanRepository",
                getDemandPlanRepositoryComListagem(List.of(demandPlanValido)));
        setField(
                demandPlanningFrontServiceComDtoSemId,
                "demandPlanAutoMapper",
                getDemandPlanAutoMapperComListaSemLinhas(List.of(new DemandPlanDTO())));

        IllegalStateException dtoSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                demandPlanningFrontServiceComDtoSemId::getDemandPlanDTOList);

        Assertions.assertEquals(
                "Demand Plan mapper returned DTO without demandPlanId at index 0 for Demand Planning DTO listing.",
                dtoSemIdException.getMessage());

    }

    @Test
    void getPlanningBookDTOShouldRejectReferencePlanBeforeLoadingPlanOrView() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.planId = "1";
        configuredViewSelectionDTO.referencePlanId = "reference-plan";

        /*
         * Nenhum collaborator Spring e injetado. Se o service carregar plano ou
         * view antes de validar reference plan, este teste quebrara com NPE em
         * vez da excecao de capability Enterprise.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningFrontService.getPlanningBookDTO(configuredViewSelectionDTO, "admin"));

    }

    @Test
    void getPlanningBookDTOShouldRejectMissingSelectionBeforeLoadingPlanOrView() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningFrontService.getPlanningBookDTO(null, "admin"));

        Assertions.assertEquals(
                "Demand Planning Book view selection is required",
                illegalArgumentException.getMessage());

        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.viewName = "Demand Planning Book";

        IllegalArgumentException missingPlanIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningFrontService.getPlanningBookDTO(
                        configuredViewSelectionDTO,
                        "admin"));

        Assertions.assertEquals(
                "Demand Planning Book plan id is required",
                missingPlanIdException.getMessage());

        configuredViewSelectionDTO.planId = "1";
        configuredViewSelectionDTO.viewName = null;

        IllegalArgumentException missingViewNameException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningFrontService.getPlanningBookDTO(
                        configuredViewSelectionDTO,
                        "admin"));

        Assertions.assertEquals(
                "Demand Planning Book view name is required",
                missingViewNameException.getMessage());

    }

    @Test
    void getPlanningBookDTOShouldRejectInvalidPlanIdBeforeLoadingPlanOrView() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.planId = "abc";
        configuredViewSelectionDTO.viewName = "Demand Planning Book";

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningFrontService.getPlanningBookDTO(
                        configuredViewSelectionDTO,
                        "admin"));

        Assertions.assertEquals(
                "Demand Planning Book plan id must be numeric: abc",
                illegalArgumentException.getMessage());

    }

    @Test
    void getDemandPlanDTOShouldRejectBrokenDetailedMapperSnapshotBeforeCleanup() throws Exception {

        DemandPlanningFacade demandPlanningFrontServiceComDTONulo =
                criaDemandPlanningFrontServiceParaDetalhe(null);

        IllegalStateException nullDtoException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandPlanningFrontServiceComDTONulo.getDemandPlanDTO(77L));
        Assertions.assertEquals(
                "Demand Plan mapper returned null DTO for Demand Planning detail.",
                nullDtoException.getMessage());

        DemandPlanDTO demandPlanDTOSemId = criaDemandPlanDTODetalhado(
                null,
                List.of(new DemandPlanItemDTO()));
        DemandPlanningFacade demandPlanningFrontServiceComDTOSemId =
                criaDemandPlanningFrontServiceParaDetalhe(demandPlanDTOSemId);

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandPlanningFrontServiceComDTOSemId.getDemandPlanDTO(77L));
        Assertions.assertEquals(
                "Demand Plan mapper returned DTO without demandPlanId for Demand Planning detail.",
                missingIdException.getMessage());

        DemandPlanDTO demandPlanDTOComIdDivergente = criaDemandPlanDTODetalhado(
                88L,
                List.of(new DemandPlanItemDTO()));
        DemandPlanningFacade demandPlanningFrontServiceComIdDivergente =
                criaDemandPlanningFrontServiceParaDetalhe(demandPlanDTOComIdDivergente);

        IllegalStateException mismatchedIdException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandPlanningFrontServiceComIdDivergente.getDemandPlanDTO(77L));
        Assertions.assertEquals(
                "Demand Plan mapper returned DTO with mismatched demandPlanId for Demand Planning detail.",
                mismatchedIdException.getMessage());

        DemandPlanDTO demandPlanDTOComDetalheNulo = criaDemandPlanDTODetalhado(
                77L,
                null);
        DemandPlanningFacade demandPlanningFrontServiceComDetalheNulo =
                criaDemandPlanningFrontServiceParaDetalhe(demandPlanDTOComDetalheNulo);

        IllegalStateException nullDetailListException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandPlanningFrontServiceComDetalheNulo.getDemandPlanDTO(77L));
        Assertions.assertEquals(
                "Demand Plan mapper returned null detail list for Demand Planning detail.",
                nullDetailListException.getMessage());

        DemandPlanDTO demandPlanDTOComLinhaNula = criaDemandPlanDTODetalhado(
                77L,
                java.util.Collections.singletonList(null));
        DemandPlanningFacade demandPlanningFrontServiceComLinhaNula =
                criaDemandPlanningFrontServiceParaDetalhe(demandPlanDTOComLinhaNula);

        IllegalStateException nullLineException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandPlanningFrontServiceComLinhaNula.getDemandPlanDTO(77L));
        Assertions.assertEquals(
                "Demand Plan mapper returned null detail line at index 0 for Demand Planning detail.",
                nullLineException.getMessage());

    }

    @Test
    void getDemandPlanDTOShouldKeepNullQuantitiesAndCleanZeroQuantities() throws Exception {

        DemandPlanItemDTO demandPlanItemDTO = new DemandPlanItemDTO();
        demandPlanItemDTO.baselineQtyConstrained = 0.0;
        demandPlanItemDTO.baselineQtyUnconstrained = null;
        demandPlanItemDTO.demandAdjustmentQtyConstrained = 2.0;
        DemandPlanDTO demandPlanDTO = criaDemandPlanDTODetalhado(
                77L,
                List.of(demandPlanItemDTO));
        DemandPlanningFacade demandPlanningFrontService =
                criaDemandPlanningFrontServiceParaDetalhe(demandPlanDTO);

        DemandPlanDTO demandPlanDTOCarregado = demandPlanningFrontService.getDemandPlanDTO(77L);

        /*
         * Campos ja nulos continuam nulos. Zeros sao removidos para preservar
         * o contrato historico de JSON enxuto sem gerar NPE na limpeza.
         */
        Assertions.assertNull(demandPlanDTOCarregado.demandPlanDetail.get(0).baselineQtyConstrained);
        Assertions.assertNull(demandPlanDTOCarregado.demandPlanDetail.get(0).baselineQtyUnconstrained);
        Assertions.assertEquals(
                2.0,
                demandPlanDTOCarregado.demandPlanDetail.get(0).demandAdjustmentQtyConstrained);

    }

    @Test
    void validaConfiguredViewProjectionDemandPlanningBookCommunityShouldRejectBrokenProjectionSnapshot() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        InvocationTargetException missingProjectionException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaConfiguredViewProjectionDemandPlanningBookCommunity(
                        demandPlanningFrontService,
                        null));
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                missingProjectionException.getCause());
        Assertions.assertEquals(
                "Demand Planning Book display requires configured view projection",
                missingProjectionException.getCause().getMessage());

        InvocationTargetException missingDfuProjectionException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaConfiguredViewProjectionDemandPlanningBookCommunity(
                        demandPlanningFrontService,
                        new ConfiguredViewProjection()));
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                missingDfuProjectionException.getCause());
        Assertions.assertEquals(
                "Demand Planning Book display requires DFU projection in configured view projection",
                missingDfuProjectionException.getCause().getMessage());

    }

    @Test
    void validaConfiguredViewProjectionDemandPlanningBookCommunityShouldRejectEmptyLocationScope() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        ConfiguredViewProjection configuredViewProjection = getConfiguredViewProjection(
                Set.of(),
                Set.of(new Produto("MAT-1")));

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaConfiguredViewProjectionDemandPlanningBookCommunity(
                        demandPlanningFrontService,
                        configuredViewProjection));

        Assertions.assertInstanceOf(
                DemandPlanException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "No locations conform to the filters. Please review the filters in the Admin -> User Data View menu",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    void validaConfiguredViewProjectionDemandPlanningBookCommunityShouldRejectEmptyMaterialScope() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        ConfiguredViewProjection configuredViewProjection = getConfiguredViewProjection(
                Set.of(new Location("LOC-1")),
                Set.of());

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaConfiguredViewProjectionDemandPlanningBookCommunity(
                        demandPlanningFrontService,
                        configuredViewProjection));

        Assertions.assertInstanceOf(
                DemandPlanException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "No materials conform to the filters. Please review the filters in the Admin -> User Data View menu",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    void validaKeyFigureAjustePlanningBookCommunityShouldAcceptCommunityKeyFigures() throws Exception {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        invokeValidaKeyFigureAjustePlanningBookCommunity(
                demandPlanningFrontService,
                new KeyFigureStandard(KeyFigureStandardEnum.BASELINE));
        invokeValidaKeyFigureAjustePlanningBookCommunity(
                demandPlanningFrontService,
                new KeyFigureStandard(KeyFigureStandardEnum.AJUSTE_DEMANDA));
        invokeValidaKeyFigureAjustePlanningBookCommunity(
                demandPlanningFrontService,
                new KeyFigureStandard(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP));

    }

    @Test
    void validaKeyFigureAjustePlanningBookCommunityShouldRejectKeyFiguresOutsideCommunityAllowlist() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        assertRequiresEnterpriseVersionException(
                demandPlanningFrontService,
                new KeyFigureStandard(KeyFigureStandardEnum.ITENS_NOVOS));
        assertRequiresEnterpriseVersionException(
                demandPlanningFrontService,
                new KeyFigureStandard(KeyFigureStandardEnum.UPLIFT));
        assertRequiresEnterpriseVersionException(
                demandPlanningFrontService,
                new KeyFigureStandard(KeyFigureStandardEnum.CARTEIRA));
        assertRequiresEnterpriseVersionException(
                demandPlanningFrontService,
                new KeyFigureStandard(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP_POR_DIA_UTIL));

    }

    @Test
    void atualizaDemandPlanEmKeyFigureShouldRejectEnterpriseKeyFiguresInTotalizationListBeforeProjectionAccess() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        /*
         * Projections, unidade de medida e filtro ficam nulos de proposito. A
         * borda Community deve rejeitar a key figure Enterprise antes de tentar
         * ler qualquer dado de rodada.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningFrontService.atualizaDemandPlanEmKeyFigure(
                        List.of(new KeyFigureStandard(KeyFigureStandardEnum.UPLIFT)),
                        new KeyFigureStandard(KeyFigureStandardEnum.AJUSTE_DEMANDA),
                        List.of(new KeyFigureStandard(KeyFigureStandardEnum.BASELINE)),
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        10.0d,
                        null,
                        null,
                        null,
                        "admin",
                        "contract-test"));

    }

    @Test
    void atualizaDemandPlanEmKeyFigureShouldRejectEnterpriseKeyFiguresInSplitReferenceBeforeProjectionAccess() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> demandPlanningFrontService.atualizaDemandPlanEmKeyFigure(
                        List.of(new KeyFigureStandard(KeyFigureStandardEnum.BASELINE)),
                        new KeyFigureStandard(KeyFigureStandardEnum.AJUSTE_DEMANDA),
                        List.of(new KeyFigureStandard(KeyFigureStandardEnum.ITENS_NOVOS)),
                        LocalDateTime.of(2026, 1, 1, 0, 0),
                        10.0d,
                        null,
                        null,
                        null,
                        "admin",
                        "contract-test"));

    }

    @Test
    void planningBookFilterValidationShouldAcceptAggregatedDfuSelection() throws Exception {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        FiltroDFUProjection filtroDFUProjectionAgregado = new FiltroDFUProjection(
                Set.of(new Location("LOC-1"), new Location("LOC-2")),
                Set.of(new Produto("MAT-1")),
                null);

        /*
         * O Community conserva o mesmo rateio do legado para uma ou várias
         * DFUs. Esta borda valida somente que a interseção não ficou vazia.
         */
        invokeValidaFiltroDFUProjectionPlanningBookDemand(
                demandPlanningFrontService,
                filtroDFUProjectionAgregado);

    }

    @Test
    void validaReferencePlanPlanningBookCommunityShouldAcceptCellsWithoutReferencePlan() throws Exception {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        invokeValidaReferencePlanPlanningBookCommunity(
                demandPlanningFrontService,
                List.of(
                        SelectedPlanningBookCellDTO.builder()
                                .planId(1L)
                                .viewName("Community View")
                                .keyFigure(KeyFigureStandardEnum.AJUSTE_DEMANDA.name())
                                .period(LocalDate.of(2026, 1, 31))
                                .uom("UN")
                                .oldValue(5.0)
                                .newValue(10.0)
                                .build(),
                        SelectedPlanningBookCellDTO.builder()
                                .planId(1L)
                                .viewName("Community View")
                                .keyFigure(KeyFigureStandardEnum.AJUSTE_DEMANDA.name())
                                .period(LocalDate.of(2026, 2, 28))
                                .uom("UN")
                                .oldValue(6.0)
                                .newValue(11.0)
                                .build()));

    }

    @Test
    void validaReferencePlanPlanningBookCommunityShouldRejectMissingCellPayload() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        InvocationTargetException nullPayloadException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaReferencePlanPlanningBookCommunity(
                        demandPlanningFrontService,
                        null));
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                nullPayloadException.getCause());
        Assertions.assertEquals(
                "Demand Planning Planning Book adjustment cells are required",
                nullPayloadException.getCause().getMessage());

        InvocationTargetException emptyPayloadException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaReferencePlanPlanningBookCommunity(
                        demandPlanningFrontService,
                        List.of()));
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                emptyPayloadException.getCause());
        Assertions.assertEquals(
                "At least one Demand Planning Planning Book adjustment cell is required.",
                emptyPayloadException.getCause().getMessage());

    }

    @Test
    void validaReferencePlanPlanningBookCommunityShouldRejectNullCellBeforeReferencePlanScan() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaReferencePlanPlanningBookCommunity(
                        demandPlanningFrontService,
                        Arrays.asList(
                                SelectedPlanningBookCellDTO.builder()
                                        .planId(1L)
                                        .build(),
                                null)));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Demand Planning Planning Book adjustment cells cannot contain null value at index 1.",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    void validaReferencePlanPlanningBookCommunityShouldRejectReferencePlanInAnyCell() {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaReferencePlanPlanningBookCommunity(
                        demandPlanningFrontService,
                        List.of(
                                SelectedPlanningBookCellDTO.builder()
                                        .planId(1L)
                                        .build(),
                                SelectedPlanningBookCellDTO.builder()
                                        .planId(1L)
                                        .referencePlanId("reference-plan")
                                        .build())));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    @Test
    void materializaDemandPlanItemsCommunityParaPersistenciaShouldNeutralizeEnterpriseKeyFigures() throws Exception {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        DemandPlanItem demandPlanItem = new DemandPlanItem();
        demandPlanItem.setQuantidadeBaseline(10.0);
        demandPlanItem.setQuantidadeAjusteDemanda(2.0);
        demandPlanItem.setQuantidadeItensNovos(7.0);
        demandPlanItem.setQuantidadeItensNovosAtendida(3.0);
        demandPlanItem.setQuantidadeUplift(5.0);
        demandPlanItem.setQuantidadeUpliftAtendida(4.0);

        List<DemandPlanItem> demandPlanItemsParaPersistencia =
                invokeMaterializaDemandPlanItemsCommunityParaPersistencia(
                        demandPlanningFrontService,
                        List.of(Quartet.with(
                                demandPlanItem,
                                Constantes.TipoDemanda.BASELINE,
                                1.0,
                                10.0)));

        Assertions.assertEquals(1, demandPlanItemsParaPersistencia.size());
        DemandPlanItem demandPlanItemParaPersistencia = demandPlanItemsParaPersistencia.get(0);
        Assertions.assertEquals(10.0, demandPlanItemParaPersistencia.getQuantidadeBaseline(), 0.0001d);
        Assertions.assertEquals(2.0, demandPlanItemParaPersistencia.getQuantidadeAjusteDemanda(), 0.0001d);
        Assertions.assertEquals(0.0, demandPlanItemParaPersistencia.getQuantidadeItensNovos(), 0.0001d);
        Assertions.assertEquals(0.0, demandPlanItemParaPersistencia.getQuantidadeItensNovosAtendida(), 0.0001d);
        Assertions.assertEquals(0.0, demandPlanItemParaPersistencia.getQuantidadeUplift(), 0.0001d);
        Assertions.assertEquals(0.0, demandPlanItemParaPersistencia.getQuantidadeUpliftAtendida(), 0.0001d);

    }

    @Test
    void materializaDemandPlanItemsCommunityParaPersistenciaShouldRejectBrokenAdjustedLines() throws Exception {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        InvocationTargetException colecaoAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeMaterializaDemandPlanItemsCommunityParaPersistencia(
                        demandPlanningFrontService,
                        null));
        InvocationTargetException itemAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeMaterializaDemandPlanItemsCommunityParaPersistencia(
                        demandPlanningFrontService,
                        Arrays.asList(
                                Quartet.with(
                                        new DemandPlanItem(),
                                        Constantes.TipoDemanda.BASELINE,
                                        1.0,
                                        10.0),
                                null)));
        InvocationTargetException linhaAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeMaterializaDemandPlanItemsCommunityParaPersistencia(
                        demandPlanningFrontService,
                        List.of(Quartet.with(
                                null,
                                Constantes.TipoDemanda.BASELINE,
                                1.0,
                                10.0))));

        /*
         * O ajuste do Planning Book nasce em projection mutavel e pode ser
         * alimentado por filas paralelas. Snapshot quebrado deve falhar antes
         * de neutralizar KFs Enterprise ou chamar repository.
         */
        Assertions.assertEquals(
                "Demand Planning Planning Book adjusted line collection is required for Community persistence.",
                colecaoAusenteException.getCause().getMessage());
        Assertions.assertEquals(
                "Demand Planning Planning Book adjusted line at index 1 is required for Community persistence.",
                itemAusenteException.getCause().getMessage());
        Assertions.assertEquals(
                "Demand Plan line at adjusted index 0 is required for Community Planning Book persistence.",
                linhaAusenteException.getCause().getMessage());

    }

    @Test
    void validaDemandPlanItemsSalvasPlanningBookCommunityShouldRejectBrokenSavedSnapshot() throws Exception {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();

        InvocationTargetException colecaoAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDemandPlanItemsSalvasPlanningBookCommunity(
                        demandPlanningFrontService,
                        null));
        InvocationTargetException itemAusenteException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDemandPlanItemsSalvasPlanningBookCommunity(
                        demandPlanningFrontService,
                        Arrays.asList((DemandPlanItem) null)));
        InvocationTargetException retornoParcialException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaDemandPlanItemsSalvasPlanningBookCommunity(
                        demandPlanningFrontService,
                        List.of(new DemandPlanItem()),
                        2));

        Assertions.assertEquals(
                "Saved Demand Planning Planning Book line collection is required for Community persistence.",
                colecaoAusenteException.getCause().getMessage());
        Assertions.assertEquals(
                "Saved Demand Planning Planning Book line at index 0 is required for Community persistence.",
                itemAusenteException.getCause().getMessage());
        Assertions.assertEquals(
                "Saved Demand Planning Planning Book line collection size 1 differs from expected size 2.",
                retornoParcialException.getCause().getMessage());

    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static DemandPlanningFacade criaDemandPlanningFrontServiceParaDetalhe(
            DemandPlanDTO demandPlanDTO) throws Exception {

        DemandPlanningFacade demandPlanningFrontService = new DemandPlanningFacade();
        setField(
                demandPlanningFrontService,
                "demandPlanRepository",
                getDemandPlanRepositoryComDemandPlan(criaDemandPlanListagem(
                        77L,
                        "DP-PROFILE",
                        LocalDateTime.of(2026, 1, 1, 8, 0))));
        setField(
                demandPlanningFrontService,
                "parametrosGlobaisService",
                new TestParametrosGlobaisService(new ParametrosGlobais()));
        setField(
                demandPlanningFrontService,
                "demandPlanAutoMapper",
                getDemandPlanAutoMapperComDetalhe(demandPlanDTO));

        return demandPlanningFrontService;

    }

    private static DemandPlanRepository getDemandPlanRepositoryVazio() {

        return getDemandPlanRepositoryComOptional(Optional.empty());

    }

    private static DemandPlanRepository getDemandPlanRepositoryComDemandPlan(
            DemandPlan demandPlan) {

        return getDemandPlanRepositoryComOptional(Optional.of(demandPlan));

    }

    private static DemandPlanRepository getDemandPlanRepositoryComOptional(
            Optional<DemandPlan> demandPlanOptional) {

        return (DemandPlanRepository) Proxy.newProxyInstance(
                DemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{DemandPlanRepository.class},
                (proxy, method, args) -> {
                    if ("customFindByIdComPerfilExecucao".equals(method.getName())) {
                        return demandPlanOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DemandPlanRepository com Optional controlado para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static LocationRepository getLocationRepositoryComListagem(List<Location> locationList) {

        return (LocationRepository) Proxy.newProxyInstance(
                LocationRepository.class.getClassLoader(),
                new Class<?>[]{LocationRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return locationList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "LocationRepository com listagem para teste Community";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

    }

    private static DemandPlanRepository getDemandPlanRepositoryComListagem(
            List<DemandPlan> demandPlanList) {

        return (DemandPlanRepository) Proxy.newProxyInstance(
                DemandPlanRepository.class.getClassLoader(),
                new Class<?>[]{DemandPlanRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())
                            || "customFindAllComPerfilExecucao".equals(method.getName())) {
                        return demandPlanList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DemandPlanRepository com listagem para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static DemandPlanAutoMapper getDemandPlanAutoMapperComListaSemLinhas(
            List<DemandPlanDTO> demandPlanDTOList) {

        return (DemandPlanAutoMapper) Proxy.newProxyInstance(
                DemandPlanAutoMapper.class.getClassLoader(),
                new Class<?>[]{DemandPlanAutoMapper.class},
                (proxy, method, args) -> {
                    if ("converteListaSemLinhas".equals(method.getName())) {
                        return demandPlanDTOList;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DemandPlanAutoMapper com lista sem linhas para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static DemandPlanAutoMapper getDemandPlanAutoMapperComDetalhe(
            DemandPlanDTO demandPlanDTO) {

        return (DemandPlanAutoMapper) Proxy.newProxyInstance(
                DemandPlanAutoMapper.class.getClassLoader(),
                new Class<?>[]{DemandPlanAutoMapper.class},
                (proxy, method, args) -> {
                    if ("converte".equals(method.getName())) {
                        return demandPlanDTO;
                    }
                    if ("toString".equals(method.getName())) {
                        return "DemandPlanAutoMapper com detalhe para teste Community";
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no proxy de teste: " + method.getName());
                });

    }

    private static DemandPlan criaDemandPlanListagem(
            Long demandPlanId,
            String perfilExecucaoDemandPlanId,
            LocalDateTime horarioGeracao) {

        DemandPlan demandPlan = new DemandPlan();
        demandPlan.setId(demandPlanId);
        if (perfilExecucaoDemandPlanId != null) {
            demandPlan.setPerfilExecucaoDemandPlan(new PerfilExecucaoDemandPlan(perfilExecucaoDemandPlanId));
        }
        demandPlan.setHorarioGeracao(horarioGeracao);
        demandPlan.setDataInicioPlano(LocalDateTime.of(2026, 1, 1, 0, 0));
        demandPlan.setDataFimPlano(LocalDateTime.of(2026, 12, 31, 0, 0));
        demandPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);

        return demandPlan;

    }

    private static DemandPlanDTO criaDemandPlanDTODetalhado(
            Long demandPlanId,
            List<DemandPlanItemDTO> demandPlanDetail) {

        DemandPlanDTO demandPlanDTO = new DemandPlanDTO();
        demandPlanDTO.demandPlanId = demandPlanId;
        demandPlanDTO.demandPlanDetail = demandPlanDetail;

        return demandPlanDTO;

    }

    private static DemandPlanDTO criaDemandPlanDTOListagem(
            Long demandPlanId) {

        DemandPlanDTO demandPlanDTO = new DemandPlanDTO();
        demandPlanDTO.demandPlanId = demandPlanId;

        return demandPlanDTO;

    }

    private static class TestParametrosGlobaisService extends ParametrosGlobaisService {

        private final ParametrosGlobais parametrosGlobais;

        private TestParametrosGlobaisService(
                ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

    }

    private static class TestDemandPlanningService extends DemandPlanningService {

        private final DemandPlan demandPlan;

        private TestDemandPlanningService(
                DemandPlan demandPlan) {

            this.demandPlan = demandPlan;

        }

        @Override
        public DemandPlan getDemandPlanDeId(Long id) {

            return demandPlan;

        }

    }

    private static class TestClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        private TestClusterEParametrosProjectionFactory(
                ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return clusterEParametrosProjection;

        }

    }

    private static class TestClusterEParametrosProjection extends ClusterEParametrosProjection {

        private final ParametrosGlobais parametrosGlobais;

        private TestClusterEParametrosProjection(
                ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

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

    private static class TestParametrosDemandPlanningProjectionFactory extends ParametrosDemandPlanningProjectionFactory {

        private final ParametrosDemandPlanProjection parametrosDemandPlanProjection;

        private TestParametrosDemandPlanningProjectionFactory(
                ParametrosDemandPlanProjection parametrosDemandPlanProjection) {

            this.parametrosDemandPlanProjection = parametrosDemandPlanProjection;

        }

        @Override
        public ParametrosDemandPlanProjection getParametrosDemandPlanProjectionDeCache(
                PerfilExecucaoDemandPlan perfilExecucaoDemandPlan) {

            return parametrosDemandPlanProjection;

        }
    }

    private static ConfiguredViewProjection getConfiguredViewProjection(
            Set<Location> locations,
            Set<Produto> materiais) {

        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        configuredViewProjection.setDfuProjectionFiltrado(
                new FiltroDFUProjection(
                        locations,
                        materiais,
                        null));

        return configuredViewProjection;

    }

    private static ConfiguredViewSelectionDTO getConfiguredViewSelectionDTO() {

        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.planId = "1";
        configuredViewSelectionDTO.viewName = "Community View";

        return configuredViewSelectionDTO;

    }

    private static void assertAutowiredFields(String... fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = DemandPlanningFacade.class.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    "DemandPlanningFrontService." + fieldName + " deve usar @Autowired explicito");
            Assertions.assertTrue(
                    autowired.required(),
                    "DemandPlanningFrontService." + fieldName + " deve ser bean obrigatorio");
        }

    }

    private static void assertRequiresEnterpriseVersionException(
            DemandPlanningFacade demandPlanningFrontService,
            KeyFigureInterface<?> keyFigureInterface) {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaKeyFigureAjustePlanningBookCommunity(
                        demandPlanningFrontService,
                        keyFigureInterface));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    private static SelectedPlanningBookCellDTO getSelectedPlanningBookCellDTO(
            Long demandPlanId,
            String viewName) {

        return SelectedPlanningBookCellDTO.builder()
                .planId(demandPlanId)
                .viewName(viewName)
                .keyFigure(KeyFigureStandardEnum.AJUSTE_DEMANDA.name())
                .period(LocalDate.of(2026, 1, 31))
                .uom("UN")
                .oldValue(5.0)
                .newValue(10.0)
                .build();

    }

    private static void invokeValidaKeyFigureAjustePlanningBookCommunity(
            DemandPlanningFacade demandPlanningFrontService,
            KeyFigureInterface<?> keyFigureInterface) throws Exception {

        Method validationMethod = DemandPlanningFacade.class.getDeclaredMethod(
                "validaKeyFigureAjustePlanningBookCommunity",
                KeyFigureInterface.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(demandPlanningFrontService, keyFigureInterface);

    }

    private static void invokeValidaReferencePlanPlanningBookCommunity(
            DemandPlanningFacade demandPlanningFrontService,
            List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs) throws Exception {

        Method validationMethod = DemandPlanningFacade.class.getDeclaredMethod(
                "validaReferencePlanPlanningBookCommunity",
                List.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(demandPlanningFrontService, selectedPlanningBookCellDTOs);

    }

    private static void invokeValidaConfiguredViewProjectionDemandPlanningBookCommunity(
            DemandPlanningFacade demandPlanningFrontService,
            ConfiguredViewProjection configuredViewProjection) throws Exception {

        Method validationMethod = DemandPlanningFacade.class.getDeclaredMethod(
                "validaConfiguredViewProjectionDemandPlanningBookCommunity",
                ConfiguredViewProjection.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(demandPlanningFrontService, configuredViewProjection);

    }

    private static void invokeValidaFiltroDFUProjectionPlanningBookDemand(
            DemandPlanningFacade demandPlanningFrontService,
            FiltroDFUProjection filtroDFUProjection) throws Exception {

        Method validationMethod = DemandPlanningFacade.class.getDeclaredMethod(
                "validaFiltroDFUProjectionPlanningBookDemand",
                FiltroDFUProjection.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(demandPlanningFrontService, filtroDFUProjection);

    }

    @SuppressWarnings("unchecked")
    private static List<DemandPlanItem> invokeMaterializaDemandPlanItemsCommunityParaPersistencia(
            DemandPlanningFacade demandPlanningFrontService,
            List<Quartet<DemandPlanItem,Constantes.TipoDemanda,Double,Double>> demandPlanItemsAjustados) throws Exception {

        Method materializaMethod = DemandPlanningFacade.class.getDeclaredMethod(
                "materializaDemandPlanItemsCommunityParaPersistencia",
                java.util.Collection.class);
        materializaMethod.setAccessible(true);
        return (List<DemandPlanItem>) materializaMethod.invoke(
                demandPlanningFrontService,
                demandPlanItemsAjustados);

    }

    private static void invokeValidaDemandPlanItemsSalvasPlanningBookCommunity(
            DemandPlanningFacade demandPlanningFrontService,
            List<DemandPlanItem> demandPlanItemsSalvas) throws Exception {

        invokeValidaDemandPlanItemsSalvasPlanningBookCommunity(
                demandPlanningFrontService,
                demandPlanItemsSalvas,
                demandPlanItemsSalvas == null ? 0 : demandPlanItemsSalvas.size());

    }

    private static void invokeValidaDemandPlanItemsSalvasPlanningBookCommunity(
            DemandPlanningFacade demandPlanningFrontService,
            List<DemandPlanItem> demandPlanItemsSalvas,
            int numeroDemandPlanItemsEsperado) throws Exception {

        Method validationMethod = DemandPlanningFacade.class.getDeclaredMethod(
                "validaDemandPlanItemsSalvasPlanningBookCommunity",
                List.class,
                int.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                demandPlanningFrontService,
                demandPlanItemsSalvas,
                numeroDemandPlanItemsEsperado);

    }

    /**
     * Extrai um unico corpo de metodo para que uma assercao estrutural nao
     * confunda chamadas iguais existentes em outra rota da fachada.
     */
    private static String extraiCorpoMetodo(String source, String methodSignature) {

        int methodStartIndex = source.indexOf(methodSignature);
        Assertions.assertTrue(methodStartIndex >= 0, "Metodo esperado nao encontrado: " + methodSignature);

        int openingBraceIndex = source.indexOf('{', methodStartIndex);
        Assertions.assertTrue(openingBraceIndex >= 0, "Abertura do metodo nao encontrada: " + methodSignature);

        int braceDepth = 0;
        for (int index = openingBraceIndex; index < source.length(); index++) {
            char currentCharacter = source.charAt(index);

            if (currentCharacter == '{') {
                braceDepth++;
            } else if (currentCharacter == '}') {
                braceDepth--;
                if (braceDepth == 0) {
                    return source.substring(openingBraceIndex, index + 1);
                }
            }
        }

        throw new IllegalStateException("Fechamento do metodo nao encontrado: " + methodSignature);

    }

}
