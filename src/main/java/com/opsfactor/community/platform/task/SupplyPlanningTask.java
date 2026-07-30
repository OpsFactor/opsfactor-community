package com.opsfactor.community.platform.task;

import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.VersaoSupplyPlanDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskExecutionService;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskPersistenceService;
import com.opsfactor.community.platform.scheduler.services.Task;

/**
 * Task imediata usada pelo controller Community para gerar Supply Plan.
 *
 * <p>No Community a task executa somente o motor heuristico exposto pelo
 * {@link SupplyPlanService}. Otimizador, AI optimizer e process chains sao
 * bloqueados nas validacoes do perfil/service ou reabertos pelo Enterprise.</p>
 */
public class SupplyPlanningTask extends Task <VersaoSupplyPlanDTO, SupplyPlanService> {
    
    /**
     * Construtor padrao instanciado pelo {@link ScheduledTaskExecutionService}.
     *
     * @param scheduledTaskAbstract registro de execucao imediata persistido antes da chamada
     * @param scheduledTaskPersistenceService persistência do histórico e estado da task
     * @param service service funcional de Supply Planning que executa a regra de negocio
     */
    public SupplyPlanningTask(
            ScheduledTaskAbstract scheduledTaskAbstract,
            ScheduledTaskPersistenceService scheduledTaskPersistenceService,
            SupplyPlanService service) {

        super(scheduledTaskAbstract, scheduledTaskPersistenceService, service);

    }

    /**
     * Encaminha a solicitacao da tela para o service funcional.
     *
     * <p>O DTO conserva campos usados por overlays privados, mas a borda
     * Community passa apenas os dados necessarios para criar/reexecutar o plano
     * heuristico com malha, bucket e periodo de referencia.</p>
     */
    @Override
    public void executaTask(VersaoSupplyPlanDTO dtoParametros, SupplyPlanService service) {

        service.executeSupplyPlan(
                dtoParametros.getDemandPlanId(), 
                dtoParametros.getSupplyPlanId(), 
                dtoParametros.getSupplyPlanIdForStartingStockProjection(),
                dtoParametros.getExecutionProfileId(),
                dtoParametros.getSupplyNetworkVersionId(),
                dtoParametros.getPresetConstraintGroupId(),
                dtoParametros.getTamanhoBucket(),
                (dtoParametros.getPeriodoReferencia() == null) ? null : Calendario.getPrimeiraDataFromDescricaoPeriodo(
                        dtoParametros.getPeriodoReferencia(), 
                        dtoParametros.getTamanhoBucket()),
                dtoParametros.getDescricaoSupplyPlan(),
                getUserId());
        
    }
    
}
