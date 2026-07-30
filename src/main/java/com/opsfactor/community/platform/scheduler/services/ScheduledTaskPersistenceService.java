package com.opsfactor.community.platform.scheduler.services;

import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskExecution;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskExecution.ScheduledTaskExecutionCompositeKey;
import com.opsfactor.community.platform.scheduler.facade.TaskSchedulingService;
import com.opsfactor.community.platform.scheduler.repository.ScheduledTaskAbstractRepository;
import com.opsfactor.community.platform.scheduler.repository.ScheduledTaskExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Persiste uma task e seu histórico compartilhado de execuções.
 *
 * <p>Esta responsabilidade foi separada de {@link TaskSchedulingService}
 * porque uma {@link Task} precisa apenas abrir e fechar seu histórico de
 * execução. Ela não deve depender do coordenador que decide entre execução
 * síncrona, fila Enterprise ou agenda local Enterprise. A dependência anterior
 * no service amplo criava uma referência circular com o scheduler recorrente
 * web e escondia, no construtor de cada task, quais operações eram realmente
 * necessárias.</p>
 *
 * <p>O componente pertence ao Community porque o contrato persistente de
 * {@link ScheduledTaskAbstract} e {@link ScheduledTaskExecution} é
 * compartilhado. Community, servidor web Enterprise e consumidor de jobs
 * Enterprise gravam o mesmo histórico; somente a forma de disparar a execução
 * muda entre os runtimes.</p>
 */
@Service
public class ScheduledTaskPersistenceService {

    /**
     * Repository da entidade base. É usado somente quando a execução altera o
     * estado persistido da task, por exemplo ao encerrar uma task imediata.
     */
    @Autowired
    private ScheduledTaskAbstractRepository scheduledTaskAbstractRepository;

    /**
     * Repository do histórico de execuções associado à task persistida.
     */
    @Autowired
    private ScheduledTaskExecutionRepository scheduledTaskExecutionRepository;

    /**
     * Abre e persiste uma nova ocorrência de execução.
     *
     * <p>A relação bidirecional é atualizada antes do save. Esse detalhe evita
     * que um save posterior da task pai, cuja coleção em memória esteja vazia,
     * remova por {@code orphanRemoval} a execução recém-criada do Process
     * Status.</p>
     *
     * @param scheduledTaskAbstract task persistida que será executada
     * @return ocorrência salva com chave composta válida
     */
    public ScheduledTaskExecution createAndSaveExecution(
            ScheduledTaskAbstract scheduledTaskAbstract) {

        if (scheduledTaskAbstract == null) {
            throw new IllegalArgumentException("Scheduled task is required to start an execution.");
        }

        ScheduledTaskExecution scheduledTaskExecution = new ScheduledTaskExecution(
                new ScheduledTaskExecutionCompositeKey(
                        scheduledTaskAbstract,
                        scheduledTaskAbstract.getUltimoIdExecucao() + 1));

        scheduledTaskExecution.setHorarioInicio(
                scheduledTaskAbstract.getDataHorarioAtualNoTimeZone());
        scheduledTaskAbstract.addScheduledTaskExecution(scheduledTaskExecution);

        ScheduledTaskExecution persistedScheduledTaskExecution =
                scheduledTaskExecutionRepository.save(scheduledTaskExecution);
        validatePersistedExecution(persistedScheduledTaskExecution);
        return persistedScheduledTaskExecution;

    }

    /**
     * Persiste o estado final ou intermediário de uma ocorrência de execução.
     *
     * @param scheduledTaskExecution ocorrência que recebeu horário final e
     *                               eventual diagnóstico de erro
     */
    public void saveExecution(ScheduledTaskExecution scheduledTaskExecution) {

        validatePersistedExecution(
                scheduledTaskExecutionRepository.save(scheduledTaskExecution));

    }

    /**
     * Persiste alterações de estado da task dona da execução.
     *
     * @param scheduledTaskAbstract task cujo estado ativo/inativo foi alterado
     */
    public void saveScheduledTask(ScheduledTaskAbstract scheduledTaskAbstract) {

        validatePersistedTask(
                scheduledTaskAbstractRepository.save(scheduledTaskAbstract));

    }

    /**
     * Valida a identidade mínima de uma task retornada pela persistência.
     *
     * <p>O scheduler é uma borda operacional. Depois de salvar, os callers
     * assumem que existe uma linha identificável no Process Status; por isso um
     * retorno nulo ou sem id é erro de persistência, não ausência funcional.</p>
     */
    public static void validatePersistedTask(ScheduledTaskAbstract scheduledTaskAbstract) {

        if (scheduledTaskAbstract == null) {
            throw new IllegalStateException("Saved scheduled task snapshot is required.");
        }
        if (scheduledTaskAbstract.getId() == null || scheduledTaskAbstract.getId().isBlank()) {
            throw new IllegalStateException("Saved scheduled task id is required.");
        }

    }

    /**
     * Valida a chave composta mínima de uma execução retornada pela persistência.
     */
    public static void validatePersistedExecution(ScheduledTaskExecution scheduledTaskExecution) {

        if (scheduledTaskExecution == null) {
            throw new IllegalStateException("Saved scheduled task execution snapshot is required.");
        }
        if (scheduledTaskExecution.getScheduledTaskExecutionCompositeKey() == null
                || scheduledTaskExecution.getScheduledTask() == null
                || scheduledTaskExecution.getScheduledTask().getId() == null
                || scheduledTaskExecution.getScheduledTask().getId().isBlank()
                || scheduledTaskExecution.getIdExecucao() == null) {
            throw new IllegalStateException("Saved scheduled task execution key is required.");
        }

    }

}
