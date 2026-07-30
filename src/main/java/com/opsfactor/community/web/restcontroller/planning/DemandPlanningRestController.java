package com.opsfactor.community.web.restcontroller.planning;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.DemandPlanningFacade;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanPeriodDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanSelectDTO;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.VersaoDemandPlanDTO;
import com.opsfactor.community.capability.demandplanning.planningbook.facade.PlanningBookExcelExportService;
import com.opsfactor.community.platform.task.DeleteDemandPlanTask;
import com.opsfactor.community.platform.task.DemandPlanningTask;
import com.opsfactor.community.platform.security.login.AuthenticationService;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


/**
 * REST controller Community para Demand Planning operacional.
 *
 * <p>A superficie publica cobre geracao sincronizada de Demand Plan,
 * listagem/consulta de versoes, Planning Book em nivel material/location,
 * export XLSX somente leitura e ajustes pela propria tela. Reference plan,
 * support series/regression series, upload de ajustes por arquivo, Demand
 * Accuracy, Auto-fit e Change Log permanecem Enterprise.</p>
 */
@Slf4j
@RestController
public class DemandPlanningRestController {

    /**
     * Service de fachada do Demand Planning usado para consultas, Planning Book
     * material/location e ajustes manuais permitidos no Community.
     */
    @Autowired
    private DemandPlanningFacade demandPlanningFrontService;

    /**
     * Scheduler web usado pelo endpoint compartilhado. No Community ele fixa
     * {@code SYNC}; o overlay Enterprise preserva a rota legada e resolve o
     * modo configurado de processos de planejamento.
     */
    @Autowired
    private WebControllerTaskSchedulingService webControllerTaskSchedulingService;

    /**
     * Exportador XLSX somente leitura do Planning Book. Upload/importacao de
     * ajustes por arquivo nao faz parte do recorte Community.
     */
    @Autowired
    private PlanningBookExcelExportService planningBookExcelExportService;

    /**
     * Fonte centralizada e validada do usuario autenticado Community.
     *
     * <p>Endpoints de Planning Book precisam repassar o user id para services
     * que aplicam configuracoes de visao e preferencia. O controller nao acessa
     * `SecurityContextHolder` diretamente para manter a validacao estrutural em
     * um unico ponto da camada de seguranca.</p>
     */
    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Carrega os filtros/selects operacionais para selecionar e abrir Demand
     * Plans existentes.
     */
    @GetMapping("api/secured/planning/demand/filter")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<DemandPlanSelectDTO> createSearchData() {

        try {
            DemandPlanSelectDTO demandPlanSelectDTO = demandPlanningFrontService.getDemandPlanningSelectDTO();
            return ResponseEntity.ok(demandPlanSelectDTO);
        } catch (RuntimeException e) {
            log.error("Error loading Demand Planning filter data", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Gera um Demand Plan pelo modo fornecido pelo scheduler web ativo.
     *
     * <p>No Community, o scheduler compartilhado fixa {@code SYNC}. No
     * Enterprise, o mesmo endpoint legado le
     * {@code opsfactor.execution_mode.planning_processes} pelo overlay, sem
     * duplicar controller, task ou service de Demand Planning.</p>
     */
    @PostMapping("api/secured/planning/demand/generate")
    @Secured({"ROLE_ADMIN", "ROLE_DEMAND_PLANNING_EXECUTION"})
    public ResponseEntity<ResponseDTO> gerarDemandPlan(@RequestBody VersaoDemandPlanDTO versaoDemandPlanDTO) {
        
        validaVersaoDemandPlanDTOCommunity(versaoDemandPlanDTO);

        return webControllerTaskSchedulingService.runImediato(
                DemandPlanningTask.class, 
                versaoDemandPlanDTO, 
                "Demand Planning", 
                versaoDemandPlanDTO.getDescricao(), 
                webControllerTaskSchedulingService.getPlanningProcessExecutionMode());
        
    }

    /**
     * Lista versoes de Demand Plan disponiveis para consulta/Planning Book.
     */
    @GetMapping("api/secured/planning/demand/versions")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<VersaoDemandPlanDTO>> getVersoesDemandPlan() {

        try {
            return ResponseEntity.ok(demandPlanningFrontService.getListaVersaoDemandPlanDTO());
        } catch (RuntimeException e) {
            log.error("Error listing Demand Plan versions", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Carrega metadados de um Demand Plan especifico.
     */
    @GetMapping("api/secured/planning/demand/demandplan/{demandPlanId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<DemandPlanDTO> getDemandPlanDTOFromId(@PathVariable("demandPlanId") Long demandPlanId) {

        try {
            return ResponseEntity.ok(demandPlanningFrontService.getDemandPlanDTO(demandPlanId));
        } catch (RuntimeException e) {
            log.error("Error loading Demand Plan {}", demandPlanId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Lista Demand Plans em formato resumido para telas operacionais.
     */
    @GetMapping("api/secured/planning/demand/demandplan")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<DemandPlanDTO>> getDemandPlanDTOList() {

        try {
            return ResponseEntity.ok(demandPlanningFrontService.getDemandPlanDTOList());
        } catch (RuntimeException e) {
            log.error("Error listing Demand Plans", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Lista os períodos persistidos de um Demand Plan para filtros de leitura.
     */
    @GetMapping("api/secured/planning/demand/{demandPlanId}/periods")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<DemandPlanPeriodDTO>> getDemandPlanPeriodDTOList(
            @PathVariable("demandPlanId") Long demandPlanId) {

        try {
            return ResponseEntity.ok(demandPlanningFrontService.getDemandPlanPeriodDTOList(demandPlanId));
        } catch (RuntimeException e) {
            log.error("Error listing Demand Plan periods for {}", demandPlanId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Monta o Planning Book Community em nivel material/location.
     *
     * <p>Views agregadas, filtros DFU Enterprise e reference plan continuam
     * bloqueados pelos services/projections chamados por esta borda.</p>
     */
    @PostMapping("api/secured/planning/demand/planningbook")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<PlanningBookDTO> getPlanningBookDTO(
            @RequestBody ConfiguredViewSelectionDTO configuredViewSelectionDTO) {

        try {
            String userId = getUserIdAutenticadoCommunity();
            return ResponseEntity.ok(demandPlanningFrontService.getPlanningBookDTO(configuredViewSelectionDTO, userId));
        } catch (RuntimeException e) {
            log.error("Error loading Demand Planning Book", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Exporta o Planning Book atual para XLSX somente leitura.
     */
    @PostMapping(value = "api/secured/planning/demand/planningbook/xlsx", produces =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Secured("ROLE_ADMIN")
    public void getPlanningBookDTOComoArquivoExcel(
            @RequestBody ConfiguredViewSelectionDTO configuredViewSelectionDTO,
            HttpServletResponse response) {

        response.setHeader("Content-Disposition", "attachment; filename=\"planning-book.xlsx\"");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        String userId = getUserIdAutenticadoCommunity();
        try {
            ServletOutputStream out = response.getOutputStream();
            planningBookExcelExportService.writePlanningBookExcelToOutputStream(configuredViewSelectionDTO, userId, out);
        } catch (IOException | RuntimeException e) {
            log.error("Error exporting Demand Planning Book XLSX", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Aplica ajustes manuais enviados pela propria grade do Planning Book.
     */
    @PostMapping("api/secured/planning/demand/planningbook/update")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<PlanningBookDTO> updateDemandPlan(
            @RequestBody List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs) {

        try {
            String userId = getUserIdAutenticadoCommunity();
            return ResponseEntity.ok(demandPlanningFrontService.atualizaDemandPlan(selectedPlanningBookCellDTOs, userId));
        } catch (RuntimeException e) {
            log.error("Error updating Demand Planning Book", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);            
        }

    }

    /**
     * Remove Demand Plans tambem por execucao sincronizada Community.
     */
    @PostMapping("api/secured/planning/demand/delete")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> deleteDemandPlans(@RequestBody List<DemandPlanDTO> demandPlanDTOList) {

        validaDemandPlanDTOListParaExclusaoCommunity(demandPlanDTOList);

        return webControllerTaskSchedulingService.runImediato(
                DeleteDemandPlanTask.class,
                demandPlanDTOList,
                "DemandPlanDelete",
                demandPlanDTO -> "Demand Plan " + demandPlanDTO.demandPlanId + " - " + demandPlanDTO.description,
                Constantes.ModoExecucaoProcesso.SYNC);

    }

    /**
     * Valida o payload minimo para disparo sincronizado de Demand Planning.
     *
     * <p>Campos funcionais como perfil, periodo e bloqueios de Reference Plan
     * continuam validados por {@link DemandPlanningTask} e pelos services de
     * Demand Planning. Aqui a borda REST apenas impede payload nulo de gerar
     * NPE antes de registrar a task Community.</p>
     */
    private void validaVersaoDemandPlanDTOCommunity(VersaoDemandPlanDTO versaoDemandPlanDTO) {

        if (versaoDemandPlanDTO == null) {
            throw new IllegalArgumentException(
                    "Demand Planning generation payload is required");
        }

    }

    /**
     * Valida a lista de Demand Plans selecionados para exclusao.
     *
     * <p>Exclusao Community e executada item a item pelo scheduler sincronizado.
     * Lista vazia ou item sem id nao representa operacao funcional e deve
     * falhar antes da lambda de descricao e antes de criar historico de task.</p>
     */
    private void validaDemandPlanDTOListParaExclusaoCommunity(
            List<DemandPlanDTO> demandPlanDTOList) {

        if (demandPlanDTOList == null) {
            throw new IllegalArgumentException(
                    "Demand Plan delete payload list is required");
        }

        if (demandPlanDTOList.isEmpty()) {
            throw new IllegalArgumentException("At least one Demand Plan must be selected for deletion.");
        }

        for (int indiceDemandPlanDTO = 0; indiceDemandPlanDTO < demandPlanDTOList.size(); indiceDemandPlanDTO++) {
            if (demandPlanDTOList.get(indiceDemandPlanDTO) == null) {
                throw new IllegalArgumentException(
                        "Demand Plan delete payload list cannot contain null value at index "
                                + indiceDemandPlanDTO + ".");
            }
        }

        if (demandPlanDTOList.stream().anyMatch(demandPlanDTO -> demandPlanDTO.demandPlanId == null)) {
            throw new IllegalArgumentException("Demand Plan delete payload id is required.");
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
