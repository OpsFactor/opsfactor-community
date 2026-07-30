package com.opsfactor.community.web.restcontroller.planning;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.CellDetailsDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.DFUMalhaCircularDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.masterdata.demand.dfu.facade.dto.DFUDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.LowLevelCodeFacade;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.SupplyPlanFacade;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanPeriodDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.VersaoSupplyPlanDTO;
import com.opsfactor.community.platform.task.DeleteSupplyPlanTask;
import com.opsfactor.community.platform.task.SupplyPlanningTask;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.opsfactor.community.platform.security.login.CommunitySecurityConstants;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API Community de Supply Planning.
 *
 * <p>Este controller cobre execucao sincronizada do plano heuristico,
 * consulta de versoes, Planning Book material/location, low level code tecnico
 * e exclusao de planos. Ele nao publica optimizer/process chain, line
 * scheduling, Constraint Tracker/root cause, Supply Network Flows, custos,
 * P&L, cost-to-serve ou qualquer processamento assíncrono.</p>
 */
@Slf4j
@RestController
public class SupplyPlanningController {

    /**
     * Fachada funcional do Supply Planning Book Community, incluindo consulta
     * de planos, versoes, periodos e ajustes manuais material/location.
     */
    @Autowired
    private SupplyPlanFacade supplyPlanFrontService;

    /**
     * Service tecnico para low level code da malha Community. Este calculo e
     * usado para entendimento operacional da explosao de materiais e nao
     * substitui telas Enterprise de Supply Network Flows.
     */
    @Autowired
    private LowLevelCodeFacade lowLevelCodeFrontService;

    /**
     * Scheduler web limitado ao modo imediato/sincrono no Community. Filas,
     * workers e execucoes batch recorrentes pertencem ao overlay Enterprise.
     */
    @Autowired
    private WebControllerTaskSchedulingService webControllerTaskSchedulingService;

    /**
     * Fonte centralizada e validada do usuario autenticado Community.
     *
     * <p>O Planning Book Supply usa o user id para preferencias/configuracoes
     * de visao. A leitura direta do `SecurityContextHolder` fica concentrada no
     * {@link AuthenticationService}, evitando duplicacao de validacoes nos
     * controllers.</p>
     */
    @Autowired
    private AuthenticationService authenticationService;


    /**
     * Lista planos de Supply Planning em ordem decrescente de id, retornando
     * apenas o resumo necessario para a tela Community.
     */
    @GetMapping(value = "api/secured/planning/supply")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<SupplyPlanDTO>> getSupplyPlanDTOList() {

        try {
            List<?> supplyPlanDTOListSemTipo = supplyPlanFrontService.getSupplyPlanDTOList();
            List<SupplyPlanDTO> supplyPlanDTOList = supplyPlanDTOListSemTipo.stream()
                    .map(SupplyPlanDTO.class::cast)
                    .sorted(Comparator.comparingLong((SupplyPlanDTO supplyPlanDTO) -> supplyPlanDTO.supplyPlanId).reversed())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(supplyPlanDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing Supply Plans", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista os periodos de um Supply Plan ja persistido para filtros e
     * navegacao do Planning Book.
     */
    @GetMapping(value = "api/secured/planning/supply/{supplyPlanId}/periods")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<SupplyPlanPeriodDTO>> getSupplyPlanPeriodDTOList(@PathVariable("supplyPlanId") Long supplyPlanId) {

        try {
            List<SupplyPlanPeriodDTO> supplyPlanPeriodDTOList = supplyPlanFrontService.getSupplyPlanPeriodDTOList(supplyPlanId);
            return ResponseEntity.ok(supplyPlanPeriodDTOList);
        } catch (RuntimeException e) {
            log.error("Error listing Supply Plan periods for {}", supplyPlanId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Executa Supply Planning pelo modo fornecido pelo scheduler web ativo.
     *
     * <p>No Community, o scheduler compartilhado fixa {@code SYNC}. No
     * Enterprise, o mesmo controller preserva o endpoint legado e o scheduler
     * overlay le {@code opsfactor.execution_mode.planning_processes}, sem
     * duplicar a API de Supply Planning.</p>
     */
    @ResponseBody
    @PostMapping("api/secured/planning/supply/execute")
    @Secured({"ROLE_ADMIN", "ROLE_SUPPLY_PLANNING_EXECUTION"})
    public ResponseEntity<ResponseDTO> executeSupplyPlanImediato(@RequestBody VersaoSupplyPlanDTO versaoSupplyPlanDTO) {
        
        validaVersaoSupplyPlanDTOCommunity(versaoSupplyPlanDTO);

        return webControllerTaskSchedulingService.runImediato(
                SupplyPlanningTask.class, 
                versaoSupplyPlanDTO, 
                "Supply Planning", 
                versaoSupplyPlanDTO.getDescricaoSupplyPlan(), 
                webControllerTaskSchedulingService.getPlanningProcessExecutionMode());
                
    }

    /**
     * Calcula a relacao de DFUs por low level code de uma versao de malha.
     */
    @GetMapping("api/secured/planning/supply/lowlevelcode")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Map<Integer,Set<DFUDTO>>> getLowLevelCodes(
            @RequestParam String supplyNetworkVersionId) {
        try{
            LocalDateTime inicio = LocalDateTime.now();
            log.info("Iniciando geração da relação de DFUs por Low Level Code");
            Map<Integer,Set<DFUDTO>> mapaLowLevelCode = lowLevelCodeFrontService.getLowLevelCodePorDFU(
                    supplyNetworkVersionId, LocalDateTime.now());
            log.info("Tempo Total Relação LLC - DFU : " + Calendario.timeBetween(inicio,LocalDateTime.now()));
            return ResponseEntity.ok(mapaLowLevelCode);
        } catch (RuntimeException e) {
            log.error("Error calculating Low Level Code by DFU for supply network version {}", supplyNetworkVersionId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Retorna o diagnostico de ciclos da versao de malha informada.
     *
     * <p>O endpoint preserva o diagnostico tecnico legado sob a política de
     * acesso única do Community. O cálculo é integralmente delegado ao
     * {@link LowLevelCodeFacade}; o controller não persiste nem
     * reinterpreta a malha.</p>
     */
    @GetMapping("api/secured/alerts/circularnetwork/{supplyNetworkVersionId}")
    @Secured(CommunitySecurityConstants.COMMUNITY_ADMIN_ROLE)
    public ResponseEntity<Object> getCircularNetworkAlerts(
            @PathVariable("supplyNetworkVersionId") String supplyNetworkVersionId) {

        try {
            Set<DFUMalhaCircularDTO> circularNetworkAlertDTOSet =
                    lowLevelCodeFrontService.getDFUMalhaCircularDTOSet(
                            supplyNetworkVersionId,
                            LocalDateTime.now());
            return ResponseEntity.ok(circularNetworkAlertDTOSet);
        } catch (RuntimeException runtimeException) {
            log.error(
                    "Error calculating circular-network alerts for supply network version {}",
                    supplyNetworkVersionId,
                    runtimeException);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(runtimeException.getMessage());
        }

    }

    /**
     * Retorna o caminho tecnico de low level code para um material especifico.
     */
    @GetMapping("api/secured/planning/supply/lowlevelcode/material")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<LowLevelCodeDTO> getCaminhoLowLevelCodesDeMaterial(
            @RequestParam String supplyNetworkVersionId,
            @RequestParam String materialId) {
        try{
            return ResponseEntity.ok(lowLevelCodeFrontService.getLowLevelCodeDTO(
                    supplyNetworkVersionId, materialId));
        } catch (RuntimeException e) {
            log.error("Error calculating Low Level Code path for material {} and supply network version {}", materialId, supplyNetworkVersionId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Carrega o Planning Book Supply Community sempre em nivel material/location.
     *
     * <p>Views agregadas, key figures configuraveis, filtros/agregadores e
     * outputs de otimizador permanecem fora do recorte Community.</p>
     */
    @PostMapping("api/secured/planning/supply/planningbook")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<PlanningBookDTO> getPlanningBookDTO(
            @RequestBody ConfiguredViewSelectionDTO configuredViewSelectionDTO) {
        try {
            String userId = getUserIdAutenticadoCommunity();
            PlanningBookDTO planningBookDTO = supplyPlanFrontService.getPlanningBookDTO(configuredViewSelectionDTO, userId);
            return ResponseEntity.ok(planningBookDTO);
        } catch (RuntimeException e) {
            log.error("Error loading Supply Planning Book", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Aplica ajustes manuais do Planning Book Supply Community.
     *
     * <p>A edicao Community aceita apenas ajustes no proprio livro; upload de
     * arquivo de colaboracao ou inputs agregados pertencem ao Enterprise.</p>
     */
    @PostMapping("api/secured/planning/supply/planningbook/update")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<PlanningBookDTO> updateSupplyPlan(
            @RequestBody List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs) {
        try {
            String userId = getUserIdAutenticadoCommunity();
            PlanningBookDTO planningBookDTO = supplyPlanFrontService.modificaSupplyPlan(selectedPlanningBookCellDTOs, userId);
            return ResponseEntity.ok(planningBookDTO);
        } catch (RuntimeException e) {
            log.error("Error updating Supply Planning Book", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Retorna o detalhe de uma celula material/location do Planning Book.
     */
    @PostMapping("api/secured/planning/supply/planningbook/detail")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<CellDetailsDTO> getPlanningBookCellDetails(
            @RequestBody SelectedPlanningBookCellDTO selectedPlanningBookCellDTO) {
        try {
            String userId = getUserIdAutenticadoCommunity();
            CellDetailsDTO cellDetailsDTO = supplyPlanFrontService.getDetalhesSupplyPlanningBook(
                    selectedPlanningBookCellDTO,
                    userId);
            return ResponseEntity.ok(cellDetailsDTO);
        } catch (RuntimeException e) {
            log.error("Error loading Supply Planning Book cell details", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Persiste ajustes feitos dentro do detalhe da celula material/location.
     */
    @PostMapping("api/secured/planning/supply/planningbook/detail/update")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<PlanningBookDTO> updateReplenishmentDetails(
            @RequestBody CellDetailsDTO cellDetailsDTO) {
        try {
            String userId = getUserIdAutenticadoCommunity();
            PlanningBookDTO planningBookDTO = supplyPlanFrontService.modificaDetalhesSupplyPlan(cellDetailsDTO, userId);
            return ResponseEntity.ok(planningBookDTO);
        } catch (RuntimeException e) {
            log.error("Error updating Supply Planning Book cell details", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Remove planos selecionados no modo imediato/sincrono Community.
     */
    @PostMapping("api/secured/planning/supply/delete")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> deleteSupplyPlans(@RequestBody List<SupplyPlanDTO> supplyPlanDTOList) {

        validaSupplyPlanDTOListParaExclusaoCommunity(supplyPlanDTOList);

        /*
         * Evita erro em TaskSchedulingService ao converter atributos aninhados
         * para Map<String,String>; o delete nao precisa do DemandPlanDTO
         * incorporado em cada linha.
         */
        supplyPlanDTOList.stream().forEach(dto -> dto.demandPlanDTO = null);

        return webControllerTaskSchedulingService.runImediato(
                DeleteSupplyPlanTask.class,
                supplyPlanDTOList,
                "SupplyPlanDelete",
                supplyPlanDTO -> "Supply Plan " + supplyPlanDTO.supplyPlanId + " - " + supplyPlanDTO.description,
                Constantes.ModoExecucaoProcesso.SYNC);

    }

    /**
     * Valida o payload minimo para disparo sincronizado do Supply Plan.
     *
     * <p>Perfil, Demand Plan, malha e bucket continuam validados pela task e
     * pelo {@link SupplyPlanFacade}/{@code SupplyPlanService}. A borda
     * REST garante apenas que o payload exista antes de ler a descricao e
     * registrar a execucao imediata.</p>
     */
    private void validaVersaoSupplyPlanDTOCommunity(VersaoSupplyPlanDTO versaoSupplyPlanDTO) {

        if (versaoSupplyPlanDTO == null) {
            throw new IllegalArgumentException(
                    "Supply Planning execution payload is required");
        }

    }

    /**
     * Valida a lista de Supply Plans selecionados para exclusao.
     *
     * <p>A exclusao Community executa cada item de forma sincronizada. Lista
     * vazia, item nulo ou item sem id nao representa operacao funcional e deve
     * falhar antes da limpeza defensiva de `demandPlanDTO` e antes do scheduler.</p>
     */
    private void validaSupplyPlanDTOListParaExclusaoCommunity(
            List<SupplyPlanDTO> supplyPlanDTOList) {

        if (supplyPlanDTOList == null) {
            throw new IllegalArgumentException(
                    "Supply Plan delete payload list is required");
        }

        if (supplyPlanDTOList.isEmpty()) {
            throw new IllegalArgumentException("At least one Supply Plan must be selected for deletion.");
        }

        for (int indiceSupplyPlanDTO = 0; indiceSupplyPlanDTO < supplyPlanDTOList.size(); indiceSupplyPlanDTO++) {
            if (supplyPlanDTOList.get(indiceSupplyPlanDTO) == null) {
                throw new IllegalArgumentException(
                        "Supply Plan delete payload list cannot contain null value at index "
                                + indiceSupplyPlanDTO + ".");
            }
        }

        if (supplyPlanDTOList.stream().anyMatch(supplyPlanDTO -> supplyPlanDTO.supplyPlanId == null)) {
            throw new IllegalArgumentException("Supply Plan delete payload id is required.");
        }

    }

    /**
     * Retorna o usuario autenticado ja validado pelo contrato central de
     * seguranca Community.
     */
    private String getUserIdAutenticadoCommunity() {

        return authenticationService.getAuthenticatedUserId();

    }

}
