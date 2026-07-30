package com.opsfactor.community.platform.task;

import com.opsfactor.community.capability.demandplanning.service.DemandPlanningService;
import com.opsfactor.community.capability.demandplanning.demandplan.facade.dto.VersaoDemandPlanDTO;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskExecutionService;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskPersistenceService;
import com.opsfactor.community.platform.scheduler.services.Task;

/**
 * Task imediata usada pelo controller Community para gerar um Demand Plan.
 *
 * <p>Apesar de a execucao passar pela infraestrutura de scheduler para manter
 * historico de processo e resposta padronizada ao front, no Community esta
 * classe e sempre executada de forma sincronizada na propria request. Modos
 * assíncronos, filas e runners batch pertencem ao OpsFactor Enterprise.</p>
 */
public class DemandPlanningTask extends Task<VersaoDemandPlanDTO, DemandPlanningService> {

    /**
     * Construtor padrao instanciado pelo {@link ScheduledTaskExecutionService}.
     *
     * @param scheduledTaskAbstract registro de execucao imediata persistido antes da chamada
     * @param scheduledTaskPersistenceService persistência do histórico e estado da task
     * @param service service funcional de Demand Planning que executa a regra de negocio
     */
    public DemandPlanningTask(
            ScheduledTaskAbstract scheduledTaskAbstract,
            ScheduledTaskPersistenceService scheduledTaskPersistenceService,
            DemandPlanningService service) {

        super(scheduledTaskAbstract, scheduledTaskPersistenceService, service);

    }

    /**
     * Encaminha a solicitacao da tela para o service funcional.
     *
     * <p>Os campos de Reference Plan permanecem no DTO por compatibilidade com
     * payloads legados/transicionais. No Community, {@link DemandPlanningService}
     * valida esses campos antes de carregar perfil ou criar qualquer plano
     * novo.</p>
     */
    @Override
    public void executaTask(VersaoDemandPlanDTO dtoParametros, DemandPlanningService service) {

        service.executaDemandPlanning(
                dtoParametros.getExecutionProfileId(),
                dtoParametros.getPeriodoReferencia(),
                dtoParametros.getDescricao(),
                dtoParametros.getDemandPlanReferenciaCopiaDados(),
                dtoParametros.getCopiaApenasNoHorizonteCongelado(),
                getUserId());
        
    }

}
