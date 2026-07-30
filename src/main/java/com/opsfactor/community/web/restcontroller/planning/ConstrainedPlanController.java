package com.opsfactor.community.web.restcontroller.planning;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.capability.supplyplanning.service.heuristic.ConstrainedPlanService;
import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller Community para gerar plano restrito heuristico.
 *
 * <p>Este endpoint nao e Constraint Tracker. Ele executa apenas a restricao
 * imediata do Supply Plan ja gerado pelo motor heuristico. Backtracking,
 * analise de causa raiz, explicabilidade de restricoes e diagnosticos de
 * otimizador pertencem ao Enterprise.</p>
 */
@RestController
public class ConstrainedPlanController {

    /**
     * Service heuristico que recalcula o plano restrito a partir de um Supply
     * Plan Community ja gerado.
     */
    @Autowired
    private ConstrainedPlanService constrainedPlanService;

    /**
     * Executor web imediato. O endpoint Community nao publica fila, retries,
     * workers batch nem processamento async.
     */
    @Autowired
    private WebControllerTaskSchedulingService webControllerTaskSchedulingService;

    /**
     * Executa a restricao imediata do Supply Plan Community.
     *
     * <p>O Community aceita apenas perfis heurísticos. Process chains e
     * otimizador sao recusados pelo service como capacidades Enterprise.</p>
     *
     * @param supplyPlanId identificador do Supply Plan a restringir.
     * @return resposta padrao de task imediata/sincrona.
     */
    @GetMapping("api/secured/planning/constrained/execute/{supplyPlanId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> restringePlanoHeuristico(@PathVariable("supplyPlanId") Long supplyPlanId){

        return webControllerTaskSchedulingService.runImediatoSync(
                () -> {
                    constrainedPlanService.restringePlanoComPerfilHeuristico(supplyPlanId);
                    return "Constrained Plan successfully generated";
                },
                "ExecuteConstrainedPlan");

    }
}
