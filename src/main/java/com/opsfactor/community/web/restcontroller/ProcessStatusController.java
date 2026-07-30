package com.opsfactor.community.web.restcontroller;

import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.platform.scheduler.facade.dto.TaskSchedulingDTO;
import com.opsfactor.community.platform.scheduler.facade.TaskSchedulingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Endpoints de consulta e limpeza do historico tecnico de execucoes imediatas.
 *
 * <p>No OpsFactor Community nao existe fila, worker batch, scheduler recorrente
 * nem processamento em background. O modulo {@code scheduler} permanece apenas
 * para registrar auditoria/status das tarefas executadas de forma sincronizada
 * pela propria request.</p>
 */
@Slf4j
@RestController
public class ProcessStatusController {

    /**
     * Service que consulta e remove o historico tecnico de tarefas imediatas.
     * No Community ele nao representa uma fila ativa nem scheduler recorrente.
     */
    @Autowired
    private TaskSchedulingService taskSchedulingService;

    /**
     * Fachada usada para registrar a exclusao do historico como uma execucao
     * sincronizada, mantendo a tela de Process Status consistente com as demais
     * acoes administrativas.
     */
    @Autowired
    private WebControllerTaskSchedulingService webControllerTaskSchedulingService;
        
    /**
     * Traduz estouro de memoria em resposta HTTP padronizada.
     *
     * <p>O metodo nao possui chamadas Java diretas porque e invocado pelo
     * dispatcher do Spring a partir da anotacao {@link ExceptionHandler}.</p>
     */
    @ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR,
                reason="Server has run out of memory processing files")
    @ExceptionHandler(OutOfMemoryError.class)
    @SuppressWarnings("unused")
    public void outOfMemory() {

    }

    /**
     * Lista apenas o historico das tarefas imediatas ja executadas pelo Community.
     * O endpoint legado e mantido para compatibilidade do front compartilhado, mas
     * nao representa fila ativa nem monitoramento de jobs assíncronos.
     */
    @GetMapping("api/secured/scheduler/status")
    @Secured({"ROLE_ADMIN", "ROLE_DEMAND_PLANNING_EXECUTION", "ROLE_SUPPLY_PLANNING_EXECUTION"})
    public List<TaskSchedulingDTO> getScheduledTaskHistory() {

        return taskSchedulingService.getTaskSchedulingDTOList();

    }

    /**
     * Remove entradas do historico tecnico de execucao sincronizada.
     *
     * <p>Como a edicao Community nao possui handles de fila ou agendamento, esta
     * operacao nao cancela processos em andamento; ela apenas apaga registros
     * persistidos de status.</p>
     */
    @PostMapping("api/secured/scheduler/delete")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> deleteScheduledTaskHistory(@RequestBody List<TaskSchedulingDTO> taskSchedulingDTOList) {

        validaTaskSchedulingDTOListParaExclusaoCommunity(taskSchedulingDTOList);

        return webControllerTaskSchedulingService.runImediatoSync(
                () -> {
                    try {
                        taskSchedulingService.deleteScheduledTasks(taskSchedulingDTOList);
                    } catch (RuntimeException e) {
                        // A exclusao Community apaga apenas historico
                        // persistido. Falhas possiveis aqui sao runtime de
                        // validacao/repository e precisam virar status HTTP.
                        log.error("Error deleting Community scheduled task history", e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
                    }
                    return "Scheduled task history deleted successfully";
                },
                "DeleteScheduledTasks");

    }

    /**
     * Valida a selecao de historico antes de registrar a propria task de
     * limpeza.
     *
     * <p>No Community o endpoint apaga apenas registros historicos. Lista
     * vazia, item nulo ou task sem id nao tem semantica operacional e nao deve
     * criar uma nova linha de Process Status para falhar depois dentro do
     * supplier.</p>
     */
    private void validaTaskSchedulingDTOListParaExclusaoCommunity(
            List<TaskSchedulingDTO> taskSchedulingDTOList) {

        if (taskSchedulingDTOList == null) {
            throw new IllegalArgumentException(
                    "Scheduled task history delete payload list is required");
        }

        if (taskSchedulingDTOList.isEmpty()) {
            throw new IllegalArgumentException("At least one scheduled task history row must be selected for deletion.");
        }

        for (int indiceTaskSchedulingDTO = 0;
             indiceTaskSchedulingDTO < taskSchedulingDTOList.size();
             indiceTaskSchedulingDTO++) {
            if (taskSchedulingDTOList.get(indiceTaskSchedulingDTO) == null) {
                throw new IllegalArgumentException(
                        "Scheduled task history delete payload list cannot contain null value at index "
                                + indiceTaskSchedulingDTO + ".");
            }
        }

        if (taskSchedulingDTOList.stream()
                .anyMatch(taskSchedulingDTO -> taskSchedulingDTO.getTaskId() == null
                        || taskSchedulingDTO.getTaskId().isBlank())) {
            throw new IllegalArgumentException("Scheduled task history delete payload task id is required.");
        }

    }
    

}
