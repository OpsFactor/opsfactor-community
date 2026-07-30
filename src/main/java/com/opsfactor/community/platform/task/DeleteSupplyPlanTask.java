package com.opsfactor.community.platform.task;

import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanDTO;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskExecutionService;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskPersistenceService;
import com.opsfactor.community.platform.scheduler.services.Task;

/**
 * Task imediata Community para exclusao de Supply Plan selecionado na tela.
 *
 * <p>Assim como na exclusao de Demand Plan, o wrapper web executa itens de
 * lista individualmente para que qualquer falha pare o processamento e preserve
 * uma resposta clara ao usuario.</p>
 */
public class DeleteSupplyPlanTask extends Task <SupplyPlanDTO, SupplyPlanService> {

    /**
     * Construtor padrao instanciado pelo {@link ScheduledTaskExecutionService}.
     *
     * @param scheduledTaskAbstract registro de execucao imediata persistido antes da chamada
     * @param scheduledTaskPersistenceService persistência do histórico e estado da task
     * @param service service funcional de Supply Planning que executa a exclusao
     */
    public DeleteSupplyPlanTask(
            ScheduledTaskAbstract scheduledTaskAbstract,
            ScheduledTaskPersistenceService scheduledTaskPersistenceService,
            SupplyPlanService service) {

        super(scheduledTaskAbstract, scheduledTaskPersistenceService, service);

    }

    /**
     * Remove o Supply Plan pelo id recebido do DTO da tela.
     */
    @Override
    public void executaTask(SupplyPlanDTO supplyPlanADeletar, SupplyPlanService service) {

        service.deleteSupplyPlan(supplyPlanADeletar.supplyPlanId);

    }
    
}
