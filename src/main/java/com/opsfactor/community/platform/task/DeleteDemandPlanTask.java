package com.opsfactor.community.platform.task;

import com.opsfactor.community.capability.demandplanning.service.DemandPlanningService;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.DemandPlanDTO;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskExecutionService;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskPersistenceService;
import com.opsfactor.community.platform.scheduler.services.Task;

/**
 * Task imediata Community para exclusao de Demand Plan selecionado na tela.
 *
 * <p>O controller pode receber uma lista de planos, mas o wrapper web executa
 * cada item individualmente e para no primeiro erro. Essa semantica evita
 * devolver sucesso quando apenas parte da lista foi excluida.</p>
 */
public class DeleteDemandPlanTask extends Task<DemandPlanDTO, DemandPlanningService> {

    /**
     * Construtor padrao instanciado pelo {@link ScheduledTaskExecutionService}.
     *
     * @param scheduledTaskAbstract registro de execucao imediata persistido antes da chamada
     * @param scheduledTaskPersistenceService persistência do histórico e estado da task
     * @param service service funcional de Demand Planning que executa a exclusao
     */
    public DeleteDemandPlanTask(
            ScheduledTaskAbstract scheduledTaskAbstract,
            ScheduledTaskPersistenceService scheduledTaskPersistenceService,
            DemandPlanningService service) {

        super(scheduledTaskAbstract, scheduledTaskPersistenceService, service);

    }

    /**
     * Remove o Demand Plan pelo id recebido do DTO da tela.
     */
    @Override
    public void executaTask(DemandPlanDTO demandPlanADeletar, DemandPlanningService service) {

        service.deleteDemandPlan(demandPlanADeletar.demandPlanId);

    }
    
}
