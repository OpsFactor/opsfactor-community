package com.opsfactor.community.web.restcontroller.planning;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.CellDetailsDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.DFUMalhaCircularDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.DemandPlanningFacade;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.LowLevelCodeFacade;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.SupplyPlanFacade;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.VersaoDemandPlanDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanPeriodDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.VersaoSupplyPlanDTO;
import com.opsfactor.community.platform.exception.SupplyPlanException;
import com.opsfactor.community.platform.task.DeleteDemandPlanTask;
import com.opsfactor.community.platform.task.DeleteSupplyPlanTask;
import com.opsfactor.community.platform.task.DemandPlanningTask;
import com.opsfactor.community.platform.task.SupplyPlanningTask;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.web.restcontroller.configuration.PerfilExecucaoDemandPlanController;
import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.scheduler.services.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Contrato da superficie REST de planejamento no OpsFactor Community.
 *
 * <p>Os controllers Community podem expor apenas execucao sincronizada,
 * consulta de versoes, Planning Book material/location, simulacao de forecast
 * estatistico e o constrained plan heuristico. Optimizer/process chain,
 * Constraint Tracker/root cause, Supply Network Flows, Demand Accuracy,
 * Auto-fit, Change Log, upload de ajustes e demais analytics Enterprise devem
 * nascer em controllers/overlays Enterprise.</p>
 */
public class PlanningControllersCommunityContractTest {

    @Test
    public void demandPlanningControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                DemandPlanningRestController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/planning/demand/demandplan"),
                        new ControllerEndpoint("GET", "api/secured/planning/demand/demandplan/{demandPlanId}"),
                        new ControllerEndpoint("GET", "api/secured/planning/demand/filter"),
                        new ControllerEndpoint("GET", "api/secured/planning/demand/versions"),
                        new ControllerEndpoint("GET", "api/secured/planning/demand/{demandPlanId}/periods"),
                        new ControllerEndpoint("POST", "api/secured/planning/demand/delete"),
                        new ControllerEndpoint("POST", "api/secured/planning/demand/generate"),
                        new ControllerEndpoint("POST", "api/secured/planning/demand/planningbook"),
                        new ControllerEndpoint("POST", "api/secured/planning/demand/planningbook/update"),
                        new ControllerEndpoint("POST", "api/secured/planning/demand/planningbook/xlsx")));

    }

    @Test
    public void demandAnalysisControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                DemandAnalysisRestController.class,
                List.of(
                        new ControllerEndpoint(
                                "GET",
                                "api/secured/demandPlanConfiguration/get/{executionProfileId}/{locationClusterId}/{materialClusterId}"),
                        new ControllerEndpoint("POST", "api/secured/demandPlanConfiguration/save"),
                        new ControllerEndpoint("POST", "api/secured/demandPlanConfiguration/simulate")));

    }

    @Test
    public void demandPlanningControllerShouldRunGenerateAndDeleteInSyncMode() throws Exception {

        DemandPlanningRestController demandPlanningRestController = new DemandPlanningRestController();
        CapturingWebControllerTaskSchedulingService capturingWebControllerTaskSchedulingService =
                new CapturingWebControllerTaskSchedulingService();
        injectField(
                demandPlanningRestController,
                "webControllerTaskSchedulingService",
                capturingWebControllerTaskSchedulingService);

        VersaoDemandPlanDTO versaoDemandPlanDTO = new VersaoDemandPlanDTO();
        versaoDemandPlanDTO.setDescricao("Demand Plan Community");

        ResponseEntity<ResponseDTO> generateResponseEntity =
                demandPlanningRestController.gerarDemandPlan(versaoDemandPlanDTO);

        Assertions.assertEquals(HttpStatus.OK, generateResponseEntity.getStatusCode());
        Assertions.assertEquals(DemandPlanningTask.class, capturingWebControllerTaskSchedulingService.taskClass.get());
        Assertions.assertSame(versaoDemandPlanDTO, capturingWebControllerTaskSchedulingService.dtoParametros.get());
        Assertions.assertEquals("Demand Planning", capturingWebControllerTaskSchedulingService.tipoProcesso.get());
        Assertions.assertEquals("Demand Plan Community", capturingWebControllerTaskSchedulingService.descricaoExecucao.get());
        Assertions.assertEquals(
                Constantes.ModoExecucaoProcesso.SYNC,
                capturingWebControllerTaskSchedulingService.modoExecucaoProcesso.get());

        Method generateDemandPlanMethod = DemandPlanningRestController.class.getDeclaredMethod(
                "gerarDemandPlan",
                VersaoDemandPlanDTO.class);
        Secured secured = generateDemandPlanMethod.getAnnotation(Secured.class);
        Assertions.assertNotNull(secured);
        Assertions.assertArrayEquals(
                new String[]{"ROLE_ADMIN", "ROLE_DEMAND_PLANNING_EXECUTION"},
                secured.value());

        DemandPlanDTO demandPlanDTO = new DemandPlanDTO();
        demandPlanDTO.demandPlanId = 10L;
        demandPlanDTO.description = "Plano para deletar";

        ResponseEntity<ResponseDTO> deleteResponseEntity =
                demandPlanningRestController.deleteDemandPlans(List.of(demandPlanDTO));

        Assertions.assertEquals(HttpStatus.OK, deleteResponseEntity.getStatusCode());
        Assertions.assertEquals(DeleteDemandPlanTask.class, capturingWebControllerTaskSchedulingService.taskClass.get());
        Assertions.assertSame(demandPlanDTO, capturingWebControllerTaskSchedulingService.dtoParametros.get());
        Assertions.assertEquals("DemandPlanDelete", capturingWebControllerTaskSchedulingService.tipoProcesso.get());
        Assertions.assertEquals(
                "Demand Plan 10 - Plano para deletar",
                capturingWebControllerTaskSchedulingService.descricaoExecucao.get());
        Assertions.assertEquals(
                Constantes.ModoExecucaoProcesso.SYNC,
                capturingWebControllerTaskSchedulingService.modoExecucaoProcesso.get());

    }

    @Test
    public void demandPlanningControllerShouldRejectMissingGenerateAndDeletePayloadBeforeScheduler() {

        DemandPlanningRestController demandPlanningRestController = new DemandPlanningRestController();

        IllegalArgumentException generatePayloadException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningRestController.gerarDemandPlan(null));

        Assertions.assertEquals(
                "Demand Planning generation payload is required",
                generatePayloadException.getMessage());

        IllegalArgumentException deleteListException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningRestController.deleteDemandPlans(null));

        Assertions.assertEquals(
                "Demand Plan delete payload list is required",
                deleteListException.getMessage());

        IllegalArgumentException emptyListException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningRestController.deleteDemandPlans(List.of()));

        Assertions.assertEquals(
                "At least one Demand Plan must be selected for deletion.",
                emptyListException.getMessage());

        IllegalArgumentException nullItemException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningRestController.deleteDemandPlans(Arrays.asList(new DemandPlanDTO(), null)));

        Assertions.assertEquals(
                "Demand Plan delete payload list cannot contain null value at index 1.",
                nullItemException.getMessage());

        DemandPlanDTO demandPlanDTO = new DemandPlanDTO();
        IllegalArgumentException missingIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandPlanningRestController.deleteDemandPlans(List.of(demandPlanDTO)));

        Assertions.assertEquals(
                "Demand Plan delete payload id is required.",
                missingIdException.getMessage());

    }

    @Test
    public void demandPlanningBookEndpointsShouldUseAuthenticatedUserFromAuthenticationService() throws Exception {

        DemandPlanningRestController demandPlanningRestController = new DemandPlanningRestController();
        DemandPlanningFacade demandPlanningFrontService = Mockito.mock(DemandPlanningFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOList =
                List.of(new SelectedPlanningBookCellDTO());
        PlanningBookDTO planningBookDTO = Mockito.mock(PlanningBookDTO.class);
        PlanningBookDTO planningBookDTOAtualizado = Mockito.mock(PlanningBookDTO.class);

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(demandPlanningFrontService.getPlanningBookDTO(configuredViewSelectionDTO, "admin"))
                .thenReturn(planningBookDTO);
        Mockito.when(demandPlanningFrontService.atualizaDemandPlan(selectedPlanningBookCellDTOList, "admin"))
                .thenReturn(planningBookDTOAtualizado);

        injectField(demandPlanningRestController, "demandPlanningFrontService", demandPlanningFrontService);
        injectField(demandPlanningRestController, "authenticationService", authenticationService);

        /*
         * Planning Book Community precisa do user id para configuracoes de
         * visao/preferencia. O teste protege contra regressao para acesso direto
         * ao SecurityContextHolder dentro do controller.
         */
        ResponseEntity<PlanningBookDTO> planningBookResponseEntity =
                demandPlanningRestController.getPlanningBookDTO(configuredViewSelectionDTO);
        ResponseEntity<PlanningBookDTO> updatedPlanningBookResponseEntity =
                demandPlanningRestController.updateDemandPlan(selectedPlanningBookCellDTOList);

        Assertions.assertSame(planningBookDTO, planningBookResponseEntity.getBody());
        Assertions.assertSame(planningBookDTOAtualizado, updatedPlanningBookResponseEntity.getBody());
        Mockito.verify(authenticationService, Mockito.times(2)).getAuthenticatedUserId();
        Mockito.verify(demandPlanningFrontService).getPlanningBookDTO(configuredViewSelectionDTO, "admin");
        Mockito.verify(demandPlanningFrontService).atualizaDemandPlan(selectedPlanningBookCellDTOList, "admin");
        Mockito.verifyNoMoreInteractions(demandPlanningFrontService);

    }

    @Test
    public void supplyPlanningControllerShouldRunExecuteAndDeleteInSyncMode() throws Exception {

        SupplyPlanningController supplyPlanningController = new SupplyPlanningController();
        CapturingWebControllerTaskSchedulingService capturingWebControllerTaskSchedulingService =
                new CapturingWebControllerTaskSchedulingService();
        injectField(
                supplyPlanningController,
                "webControllerTaskSchedulingService",
                capturingWebControllerTaskSchedulingService);

        VersaoSupplyPlanDTO versaoSupplyPlanDTO = new VersaoSupplyPlanDTO();
        versaoSupplyPlanDTO.setDescricaoSupplyPlan("Supply Plan Community");

        ResponseEntity<ResponseDTO> executeResponseEntity =
                supplyPlanningController.executeSupplyPlanImediato(versaoSupplyPlanDTO);

        Assertions.assertEquals(HttpStatus.OK, executeResponseEntity.getStatusCode());
        Assertions.assertEquals(SupplyPlanningTask.class, capturingWebControllerTaskSchedulingService.taskClass.get());
        Assertions.assertSame(versaoSupplyPlanDTO, capturingWebControllerTaskSchedulingService.dtoParametros.get());
        Assertions.assertEquals("Supply Planning", capturingWebControllerTaskSchedulingService.tipoProcesso.get());
        Assertions.assertEquals("Supply Plan Community", capturingWebControllerTaskSchedulingService.descricaoExecucao.get());
        Assertions.assertEquals(
                Constantes.ModoExecucaoProcesso.SYNC,
                capturingWebControllerTaskSchedulingService.modoExecucaoProcesso.get());

        Method executeSupplyPlanMethod = SupplyPlanningController.class.getDeclaredMethod(
                "executeSupplyPlanImediato",
                VersaoSupplyPlanDTO.class);
        Secured secured = executeSupplyPlanMethod.getAnnotation(Secured.class);
        Assertions.assertNotNull(secured);
        Assertions.assertArrayEquals(
                new String[]{"ROLE_ADMIN", "ROLE_SUPPLY_PLANNING_EXECUTION"},
                secured.value());

        SupplyPlanDTO supplyPlanDTO = new SupplyPlanDTO();
        supplyPlanDTO.supplyPlanId = 20L;
        supplyPlanDTO.description = "Supply para deletar";
        supplyPlanDTO.demandPlanDTO = new DemandPlanDTO();

        ResponseEntity<ResponseDTO> deleteResponseEntity =
                supplyPlanningController.deleteSupplyPlans(List.of(supplyPlanDTO));

        Assertions.assertEquals(HttpStatus.OK, deleteResponseEntity.getStatusCode());
        Assertions.assertEquals(DeleteSupplyPlanTask.class, capturingWebControllerTaskSchedulingService.taskClass.get());
        Assertions.assertSame(supplyPlanDTO, capturingWebControllerTaskSchedulingService.dtoParametros.get());
        Assertions.assertNull(supplyPlanDTO.demandPlanDTO);
        Assertions.assertEquals("SupplyPlanDelete", capturingWebControllerTaskSchedulingService.tipoProcesso.get());
        Assertions.assertEquals(
                "Supply Plan 20 - Supply para deletar",
                capturingWebControllerTaskSchedulingService.descricaoExecucao.get());
        Assertions.assertEquals(
                Constantes.ModoExecucaoProcesso.SYNC,
                capturingWebControllerTaskSchedulingService.modoExecucaoProcesso.get());

    }

    @Test
    public void supplyPlanningBookEndpointsShouldUseAuthenticatedUserFromAuthenticationService() throws Exception {

        SupplyPlanningController supplyPlanningController = new SupplyPlanningController();
        SupplyPlanFacade supplyPlanFrontService = Mockito.mock(SupplyPlanFacade.class);
        AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOList =
                List.of(new SelectedPlanningBookCellDTO());
        SelectedPlanningBookCellDTO selectedPlanningBookCellDTO = new SelectedPlanningBookCellDTO();
        CellDetailsDTO cellDetailsDTO = new CellDetailsDTO();
        PlanningBookDTO planningBookDTO = Mockito.mock(PlanningBookDTO.class);
        PlanningBookDTO planningBookDTOAtualizado = Mockito.mock(PlanningBookDTO.class);
        PlanningBookDTO planningBookDTOComDetalheAtualizado = Mockito.mock(PlanningBookDTO.class);
        CellDetailsDTO cellDetailsDTOCarregado = Mockito.mock(CellDetailsDTO.class);

        Mockito.when(authenticationService.getAuthenticatedUserId()).thenReturn("admin");
        Mockito.when(supplyPlanFrontService.getPlanningBookDTO(configuredViewSelectionDTO, "admin"))
                .thenReturn(planningBookDTO);
        Mockito.when(supplyPlanFrontService.modificaSupplyPlan(selectedPlanningBookCellDTOList, "admin"))
                .thenReturn(planningBookDTOAtualizado);
        Mockito.when(supplyPlanFrontService.getDetalhesSupplyPlanningBook(selectedPlanningBookCellDTO, "admin"))
                .thenReturn(cellDetailsDTOCarregado);
        Mockito.when(supplyPlanFrontService.modificaDetalhesSupplyPlan(cellDetailsDTO, "admin"))
                .thenReturn(planningBookDTOComDetalheAtualizado);

        injectField(supplyPlanningController, "supplyPlanFrontService", supplyPlanFrontService);
        injectField(supplyPlanningController, "authenticationService", authenticationService);

        /*
         * Todos os endpoints de Planning Book Supply que repassam configuracao
         * de usuario devem obter o user id pelo contrato central de seguranca.
         */
        ResponseEntity<PlanningBookDTO> planningBookResponseEntity =
                supplyPlanningController.getPlanningBookDTO(configuredViewSelectionDTO);
        ResponseEntity<PlanningBookDTO> updatedPlanningBookResponseEntity =
                supplyPlanningController.updateSupplyPlan(selectedPlanningBookCellDTOList);
        ResponseEntity<CellDetailsDTO> cellDetailsResponseEntity =
                supplyPlanningController.getPlanningBookCellDetails(selectedPlanningBookCellDTO);
        ResponseEntity<PlanningBookDTO> updatedCellDetailsResponseEntity =
                supplyPlanningController.updateReplenishmentDetails(cellDetailsDTO);

        Assertions.assertSame(planningBookDTO, planningBookResponseEntity.getBody());
        Assertions.assertSame(planningBookDTOAtualizado, updatedPlanningBookResponseEntity.getBody());
        Assertions.assertSame(cellDetailsDTOCarregado, cellDetailsResponseEntity.getBody());
        Assertions.assertSame(planningBookDTOComDetalheAtualizado, updatedCellDetailsResponseEntity.getBody());
        Mockito.verify(authenticationService, Mockito.times(4)).getAuthenticatedUserId();
        Mockito.verify(supplyPlanFrontService).getPlanningBookDTO(configuredViewSelectionDTO, "admin");
        Mockito.verify(supplyPlanFrontService).modificaSupplyPlan(selectedPlanningBookCellDTOList, "admin");
        Mockito.verify(supplyPlanFrontService).getDetalhesSupplyPlanningBook(selectedPlanningBookCellDTO, "admin");
        Mockito.verify(supplyPlanFrontService).modificaDetalhesSupplyPlan(cellDetailsDTO, "admin");
        Mockito.verifyNoMoreInteractions(supplyPlanFrontService);

    }

    @Test
    public void supplyPlanningControllerShouldPreserveCircularNetworkAlertContract() throws Exception {

        SupplyPlanningController supplyPlanningController = new SupplyPlanningController();
        LowLevelCodeFacade lowLevelCodeFrontService = Mockito.mock(LowLevelCodeFacade.class);
        Set<DFUMalhaCircularDTO> circularNetworkAlertDTOSet = Set.of(
                DFUMalhaCircularDTO.builder()
                        .masterData("Transportation Line")
                        .masterDataId("TL-1")
                        .lowLevelCode(2)
                        .circularNetworkId(1)
                        .materialId("MAT-1")
                        .outputMaterialId("MAT-2")
                        .build());
        Mockito.when(lowLevelCodeFrontService.getDFUMalhaCircularDTOSet(
                        Mockito.eq("network-1"),
                        Mockito.any(LocalDateTime.class)))
                .thenReturn(circularNetworkAlertDTOSet);
        injectField(supplyPlanningController, "lowLevelCodeFrontService", lowLevelCodeFrontService);

        ResponseEntity<Object> successfulResponseEntity =
                supplyPlanningController.getCircularNetworkAlerts("network-1");

        Assertions.assertEquals(HttpStatus.OK, successfulResponseEntity.getStatusCode());
        Assertions.assertSame(circularNetworkAlertDTOSet, successfulResponseEntity.getBody());
        Mockito.verify(lowLevelCodeFrontService).getDFUMalhaCircularDTOSet(
                Mockito.eq("network-1"),
                Mockito.any(LocalDateTime.class));

        Mockito.reset(lowLevelCodeFrontService);
        Mockito.when(lowLevelCodeFrontService.getDFUMalhaCircularDTOSet(
                        Mockito.eq("network-2"),
                        Mockito.any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("Invalid supply network"));

        ResponseEntity<Object> failedResponseEntity =
                supplyPlanningController.getCircularNetworkAlerts("network-2");

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failedResponseEntity.getStatusCode());
        Assertions.assertEquals("Invalid supply network", failedResponseEntity.getBody());

        Method circularNetworkAlertMethod = SupplyPlanningController.class.getDeclaredMethod(
                "getCircularNetworkAlerts",
                String.class);
        GetMapping getMapping = circularNetworkAlertMethod.getAnnotation(GetMapping.class);
        Secured secured = circularNetworkAlertMethod.getAnnotation(Secured.class);

        Assertions.assertNotNull(getMapping);
        Assertions.assertArrayEquals(
                new String[]{"api/secured/alerts/circularnetwork/{supplyNetworkVersionId}"},
                getMapping.value());
        Assertions.assertNotNull(secured);
        Assertions.assertArrayEquals(
                new String[]{CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE},
                secured.value());

    }

    @Test
    public void supplyPlanningControllerShouldRejectMissingExecuteAndDeletePayloadBeforeScheduler() {

        SupplyPlanningController supplyPlanningController = new SupplyPlanningController();

        IllegalArgumentException executePayloadException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningController.executeSupplyPlanImediato(null));

        Assertions.assertEquals(
                "Supply Planning execution payload is required",
                executePayloadException.getMessage());

        IllegalArgumentException deleteListException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningController.deleteSupplyPlans(null));

        Assertions.assertEquals(
                "Supply Plan delete payload list is required",
                deleteListException.getMessage());

        IllegalArgumentException emptyListException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningController.deleteSupplyPlans(List.of()));

        Assertions.assertEquals(
                "At least one Supply Plan must be selected for deletion.",
                emptyListException.getMessage());

        IllegalArgumentException nullItemException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningController.deleteSupplyPlans(Arrays.asList(new SupplyPlanDTO(), null)));

        Assertions.assertEquals(
                "Supply Plan delete payload list cannot contain null value at index 1.",
                nullItemException.getMessage());

        SupplyPlanDTO supplyPlanDTO = new SupplyPlanDTO();
        IllegalArgumentException missingIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanningController.deleteSupplyPlans(List.of(supplyPlanDTO)));

        Assertions.assertEquals(
                "Supply Plan delete payload id is required.",
                missingIdException.getMessage());

    }

    @Test
    public void demandPlanningControllersShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                DemandPlanningRestController.class,
                "demandPlanningFrontService",
                "webControllerTaskSchedulingService",
                "planningBookExcelExportService",
                "authenticationService");
        assertAutowiredFields(
                DemandAnalysisRestController.class,
                "demandSimulationFrontService");
        assertAutowiredFields(
                PerfilExecucaoDemandPlanController.class,
                "perfilExecucaoDemandPlanFrontService");

    }

    @Test
    public void supplyPlanningControllersShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                SupplyPlanningController.class,
                "supplyPlanFrontService",
                "lowLevelCodeFrontService",
                "webControllerTaskSchedulingService",
                "authenticationService");
        assertAutowiredFields(
                ConstrainedPlanController.class,
                "constrainedPlanService",
                "webControllerTaskSchedulingService");

    }

    @Test
    public void supplyPlanningPeriodControllerShouldDelegateAndPreserveCurrentErrorMapping() throws Exception {

        SupplyPlanningController supplyPlanningController = new SupplyPlanningController();
        SupplyPlanFacade supplyPlanFrontService = Mockito.mock(SupplyPlanFacade.class);
        List<SupplyPlanPeriodDTO> supplyPlanPeriodDTOList = List.of(new SupplyPlanPeriodDTO(
                0,
                "Mar 2030",
                Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2030, 3, 1, 0, 0),
                LocalDateTime.of(2030, 3, 1, 0, 0),
                LocalDateTime.of(2030, 3, 31, 23, 59)));
        injectField(supplyPlanningController, "supplyPlanFrontService", supplyPlanFrontService);

        Mockito.when(supplyPlanFrontService.getSupplyPlanPeriodDTOList(31L))
                .thenReturn(supplyPlanPeriodDTOList);

        ResponseEntity<List<SupplyPlanPeriodDTO>> responseEntity =
                supplyPlanningController.getSupplyPlanPeriodDTOList(31L);

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertSame(supplyPlanPeriodDTOList, responseEntity.getBody());
        Mockito.verify(supplyPlanFrontService).getSupplyPlanPeriodDTOList(31L);

        Mockito.when(supplyPlanFrontService.getSupplyPlanPeriodDTOList(404L))
                .thenThrow(new SupplyPlanException("Supply Plan 404 not found for Supply Planning period list."));

        ResponseStatusException responseStatusException = Assertions.assertThrows(
                ResponseStatusException.class,
                () -> supplyPlanningController.getSupplyPlanPeriodDTOList(404L));

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseStatusException.getStatusCode());
        Assertions.assertEquals(
                "Supply Plan 404 not found for Supply Planning period list.",
                responseStatusException.getReason());

    }

    @Test
    public void supplyPlanningControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                SupplyPlanningController.class,
                List.of(
                        new ControllerEndpoint("GET", "api/secured/alerts/circularnetwork/{supplyNetworkVersionId}"),
                        new ControllerEndpoint("GET", "api/secured/planning/supply"),
                        new ControllerEndpoint("GET", "api/secured/planning/supply/lowlevelcode"),
                        new ControllerEndpoint("GET", "api/secured/planning/supply/lowlevelcode/material"),
                        new ControllerEndpoint("GET", "api/secured/planning/supply/{supplyPlanId}/periods"),
                        new ControllerEndpoint("POST", "api/secured/planning/supply/delete"),
                        new ControllerEndpoint("POST", "api/secured/planning/supply/execute"),
                        new ControllerEndpoint("POST", "api/secured/planning/supply/planningbook"),
                        new ControllerEndpoint("POST", "api/secured/planning/supply/planningbook/detail"),
                        new ControllerEndpoint("POST", "api/secured/planning/supply/planningbook/detail/update"),
                        new ControllerEndpoint("POST", "api/secured/planning/supply/planningbook/update")));

    }

    @Test
    public void communityWebSchedulerShouldKeepPlanningProcessesInSyncMode() {

        WebControllerTaskSchedulingService webControllerTaskSchedulingService =
                new WebControllerTaskSchedulingService();

        Assertions.assertEquals(
                Constantes.ModoExecucaoProcesso.SYNC,
                webControllerTaskSchedulingService.getPlanningProcessExecutionMode());

    }

    @Test
    public void constrainedPlanControllerShouldExposeOnlyCommunityEndpoints() {

        assertControllerEndpoints(
                ConstrainedPlanController.class,
                List.of(new ControllerEndpoint("GET", "api/secured/planning/constrained/execute/{supplyPlanId}")));

    }

    private static void assertControllerEndpoints(
            Class<?> controllerClass,
            List<ControllerEndpoint> expectedControllerEndpointList) {

        List<ControllerEndpoint> controllerEndpointList = Arrays
                .stream(controllerClass.getDeclaredMethods())
                .flatMap(PlanningControllersCommunityContractTest::getControllerEndpoints)
                .sorted(Comparator.comparing(ControllerEndpoint::httpMethod).thenComparing(ControllerEndpoint::path))
                .toList();

        Assertions.assertEquals(
                expectedControllerEndpointList,
                controllerEndpointList,
                controllerClass.getSimpleName() + " possui endpoint fora do recorte Community aprovado.");

    }

    private static Stream<ControllerEndpoint> getControllerEndpoints(Method method) {

        return Stream.of(
                        getDirectEndpointPaths(method, GetMapping.class).map(path -> new ControllerEndpoint("GET", path)),
                        getDirectEndpointPaths(method, PostMapping.class).map(path -> new ControllerEndpoint("POST", path)),
                        getDirectEndpointPaths(method, PutMapping.class).map(path -> new ControllerEndpoint("PUT", path)),
                        getDirectEndpointPaths(method, DeleteMapping.class).map(path -> new ControllerEndpoint("DELETE", path)),
                        getDirectEndpointPaths(method, PatchMapping.class).map(path -> new ControllerEndpoint("PATCH", path)),
                        getDirectEndpointPaths(method, RequestMapping.class).map(path -> new ControllerEndpoint("REQUEST", path)))
                .flatMap(controllerEndpointStream -> controllerEndpointStream);

    }

    private static <T extends Annotation> Stream<String> getDirectEndpointPaths(
            Method method,
            Class<T> annotationClass) {

        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null) return Stream.empty();

        try {
            String[] valueArray = (String[]) annotationClass.getMethod("value").invoke(annotation);
            String[] pathArray = (String[]) annotationClass.getMethod("path").invoke(annotation);
            return Stream.concat(Arrays.stream(valueArray), Arrays.stream(pathArray)).distinct();
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(
                    "Nao foi possivel ler paths de " + annotationClass.getSimpleName(),
                    reflectiveOperationException);
        }

    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static void assertAutowiredFields(
            Class<?> controllerClass,
            String... fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = controllerClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    controllerClass.getSimpleName() + "." + fieldName + " deve usar @Autowired explicito");
            Assertions.assertTrue(
                    autowired.required(),
                    controllerClass.getSimpleName() + "." + fieldName + " deve ser bean obrigatorio");
        }

    }

    private record ControllerEndpoint(String httpMethod, String path) {

    }

    private static class CapturingWebControllerTaskSchedulingService extends WebControllerTaskSchedulingService {

        private final AtomicReference<Class<?>> taskClass = new AtomicReference<>();
        private final AtomicReference<Object> dtoParametros = new AtomicReference<>();
        private final AtomicReference<String> tipoProcesso = new AtomicReference<>();
        private final AtomicReference<String> descricaoExecucao = new AtomicReference<>();
        private final AtomicReference<Constantes.ModoExecucaoProcesso> modoExecucaoProcesso =
                new AtomicReference<>();

        @Override
        public <A, S, T extends Task<A, S>> ResponseEntity<ResponseDTO> runImediato(
                Class<T> taskClass,
                A dtoParametros,
                String tipoProcesso,
                String descricaoExecucao,
                Constantes.ModoExecucaoProcesso modoExecucaoProcesso) {

            /*
             * O teste captura exatamente a chamada que o controller faria ao
             * scheduler real. Nao executamos Task nem Spring Security aqui
             * porque o contrato em questao e a escolha do modo SYNC.
             */
            this.taskClass.set(taskClass);
            this.dtoParametros.set(dtoParametros);
            this.tipoProcesso.set(tipoProcesso);
            this.descricaoExecucao.set(descricaoExecucao);
            this.modoExecucaoProcesso.set(modoExecucaoProcesso);
            return ResponseDTO.getResponseEntity(tipoProcesso + " captured", HttpStatus.OK);

        }

        @Override
        public <A, S, T extends Task<A, S>> ResponseEntity<ResponseDTO> runImediato(
                Class<T> taskClass,
                List<A> dtoParametrosList,
                String tipoProcesso,
                Function<A, String> funcaoExtratoraDescricaoExecucao,
                Constantes.ModoExecucaoProcesso modoExecucaoProcesso) {

            A dtoParametros = dtoParametrosList.getFirst();
            return runImediato(
                    taskClass,
                    dtoParametros,
                    tipoProcesso,
                    funcaoExtratoraDescricaoExecucao.apply(dtoParametros),
                    modoExecucaoProcesso);

        }

    }

}
