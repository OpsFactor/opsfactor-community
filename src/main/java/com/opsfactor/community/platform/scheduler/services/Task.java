package com.opsfactor.community.platform.scheduler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskExecution;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskImediato;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * O objeto DTOPARAMETROS e reconstruido a partir do JSON salvo em
 * {@link ScheduledTaskAbstract#getConfiguracoesExecucaoJson()} com Jackson.
 *
 * <p>Caso nao haja parametros, pode-se usar {@link Void} como classe que
 * representa um atributo nulo. O service associado deve ser {@code @Component}
 * ou {@code @Service}, para que a instanciacao automatica em
 * {@link ScheduledTaskExecutionService} consiga encontra-lo via ApplicationContext.</p>
 */
@Slf4j
public abstract class Task<DTOPARAMETROS,SERVICE> implements Runnable {

    private final Class<DTOPARAMETROS> persistentClass;

    /**
     * Ciclo de vida persistente da execução.
     *
     * <p>Este campo nao usa {@code @Autowired} porque {@code Task} nao e um
     * bean singleton do Spring. Cada instancia e criada pelo
     * {@link ScheduledTaskExecutionService} com o {@link ScheduledTaskAbstract} e o
     * service funcional ja resolvido no {@code ApplicationContext}. A task
     * recebe somente as operações necessárias para registrar início, fim e
     * estado persistido; ela não conhece se foi disparada sincronamente, por
     * fila ou por uma agenda local Enterprise.</p>
     */
    private final ScheduledTaskPersistenceService scheduledTaskPersistenceService;

    /**
     * Registro persistido que define usuario, timezone, payload JSON e status
     * da execucao atual.
     */
    private final ScheduledTaskAbstract scheduledTaskAbstract;

    /**
     * Service funcional que executa a regra concreta da task.
     *
     * <p>Assim como o scheduler acima, este objeto e recebido no construtor para
     * permitir uma task nova por execucao sem guardar estado em beans
     * compartilhados.</p>
     */
    private final SERVICE service;
    
    public Task(
            ScheduledTaskAbstract scheduledTaskAbstract,
            ScheduledTaskPersistenceService scheduledTaskPersistenceService,
            SERVICE service) {
        
        this.scheduledTaskAbstract = scheduledTaskAbstract;
        this.scheduledTaskPersistenceService = scheduledTaskPersistenceService;
        this.service = service;
        
        this.persistentClass = resolvePersistentClass();
        
    }

    /**
     * Resolve o DTO de parametros declarado no primeiro parametro generico da task.
     *
     * <p>O JSON persistido em {@link ScheduledTaskAbstract#getConfiguracoesExecucaoJson()}
     * precisa ser reconstruido com a mesma classe de DTO usada pelo controller
     * que criou a task. Por isso a task concreta deve declarar diretamente
     * {@code Task<DTO, Service>}; se esse contrato for quebrado, falhamos com
     * mensagem contextual antes de tentar desserializar com uma classe ambigua.</p>
     */
    private Class<DTOPARAMETROS> resolvePersistentClass() {

        Type taskGenericSuperclass = this.getClass().getGenericSuperclass();

        if (!(taskGenericSuperclass instanceof ParameterizedType taskParameterizedType)) {
            throw new IllegalStateException(
                    "Scheduled task class "
                            + this.getClass().getName()
                            + " must declare Task<DTO, Service> directly.");
        }

        Type dtoParametrosType = taskParameterizedType.getActualTypeArguments()[0];
        if (!(dtoParametrosType instanceof Class<?> dtoParametrosClass)) {
            throw new IllegalStateException(
                    "Scheduled task class "
                            + this.getClass().getName()
                            + " must declare a concrete DTO class as the first Task generic parameter.");
        }

        return (Class<DTOPARAMETROS>) dtoParametrosClass;

    }
    
    public void run() {

        if (!scheduledTaskAbstract.getAtivo()) {
            log.info("Scheduled Task is not active. Stopping execution");
            return;
        }

        ScheduledTaskExecution scheduledTaskExecution =
                scheduledTaskPersistenceService.createAndSaveExecution(scheduledTaskAbstract);

        try {
            // mapper Jackson mapa <-> dto 
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            DTOPARAMETROS dtoParametros = (scheduledTaskAbstract.getConfiguracoesExecucaoJson() == null) ?
                    null
                    : mapper.readValue(scheduledTaskAbstract.getConfiguracoesExecucaoJson(), persistentClass);
            log.info("Initiating task execution");
            executaTask(dtoParametros);
            log.info("Task " + scheduledTaskAbstract.getId() + " executed with Success");
        } catch (JsonProcessingException | RuntimeException | Error e) {
            log.error("Task {} ran into an error", scheduledTaskAbstract.getId(), e);
            scheduledTaskExecution.setMensagemErroResumida(ExceptionUtils.getMessage(e));
            scheduledTaskExecution.setMensagemErroStackTrace(ExceptionUtils.getStackTrace(e));
        }
        
        scheduledTaskExecution.setHorarioFim(scheduledTaskAbstract.getDataHorarioAtualNoTimeZone());
        
        log.info("Saving Task Execution Status - " + scheduledTaskAbstract.getId());
        scheduledTaskPersistenceService.saveExecution(scheduledTaskExecution);
        
        /*
         * Community executa apenas tarefas imediatas. O branch fica explícito
         * porque o encerramento do estado imediato é compartilhado com o
         * consumer Enterprise, enquanto o fechamento de uma agenda de disparo
         * único continua no wrapper web Enterprise, dono daquele subtipo.
         */
        if (scheduledTaskAbstract instanceof ScheduledTaskImediato) {
            scheduledTaskAbstract.setAtivo(false);
            log.info("Saving Task (single-time task) - " + scheduledTaskAbstract.getId());
            scheduledTaskPersistenceService.saveScheduledTask(scheduledTaskAbstract);
        }
                
    }
    
    private void executaTask(DTOPARAMETROS dtoParametros) {
        executaTask(dtoParametros, service);
    }

    public String getUserId() {
        return scheduledTaskAbstract.getUserId();
    }
    
    public abstract void executaTask(DTOPARAMETROS dtoParametros, SERVICE service);
        
}
