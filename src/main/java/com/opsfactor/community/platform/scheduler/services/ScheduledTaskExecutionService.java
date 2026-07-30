package com.opsfactor.community.platform.scheduler.services;

import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.facade.TaskSchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * Reconstrói instâncias de {@link Task} a partir do contrato persistido.
 *
 * <p>Uma task não é bean singleton do Spring porque contém o registro
 * persistido e o estado de uma execução específica. O service funcional usado
 * por ela, ao contrário, deve ser resolvido pelo Spring para preservar
 * transações e dependências. Este service mantém essa regra em um único lugar
 * para que Community, servidor web Enterprise e consumidor de jobs Enterprise
 * não implementem versões divergentes da mesma reflexão.</p>
 *
 * <p>O service recebe o componente de persistência estreito que será entregue
 * ao construtor da task. Esse desenho substitui a antiga entrega do
 * {@link TaskSchedulingService} inteiro: além de tornar a assinatura
 * inteligível, elimina a dependência circular entre o coordenador Enterprise e
 * o scheduler recorrente restrito ao processo web.</p>
 */
@Service
public class ScheduledTaskExecutionService {

    /**
     * Contexto Spring usado exclusivamente para localizar o service funcional
     * declarado no segundo parâmetro genérico da task.
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Persistência compartilhada entregue a cada nova instância de task.
     */
    @Autowired
    private ScheduledTaskPersistenceService scheduledTaskPersistenceService;

    /**
     * Resolve e valida uma classe de task persistida por nome.
     */
    public static Class<? extends Task> resolveTaskClass(String taskClassName)
            throws ClassNotFoundException {

        CanonicalScheduledTaskClassPolicy.validateCanonicalTaskClassName(taskClassName);
        Class<?> taskClass = Class.forName(taskClassName);

        if (!Task.class.isAssignableFrom(taskClass)) {
            throw new IllegalStateException(
                    "Scheduled task class "
                            + taskClassName
                            + " must extend "
                            + Task.class.getName()
                            + ".");
        }

        return taskClass.asSubclass(Task.class);

    }

    /**
     * Resolve o service funcional declarado em {@code Task<DTO, Service>}.
     */
    public static Class<?> resolveTaskServiceClass(Class<?> taskClass) {

        Type taskGenericSuperclass = taskClass.getGenericSuperclass();

        if (!(taskGenericSuperclass instanceof ParameterizedType taskParameterizedType)) {
            throw new IllegalStateException(
                    "Scheduled task class "
                            + taskClass.getName()
                            + " must declare Task<DTO, Service> directly.");
        }

        Type taskServiceType = taskParameterizedType.getActualTypeArguments()[1];
        if (!(taskServiceType instanceof Class<?> taskServiceClass)) {
            throw new IllegalStateException(
                    "Scheduled task class "
                            + taskClass.getName()
                            + " must declare a concrete Spring service class as the second Task generic parameter.");
        }

        return taskServiceClass;

    }

    /**
     * Resolve o nome do bean que o próprio Spring escolheria para o tipo.
     *
     * <p>Quando existe mais de um bean, inclusive uma extensão Enterprise
     * {@code @Primary}, não escolhemos a primeira posição de um array. O nome
     * persistido corresponde ao bean efetivamente resolvido por tipo.</p>
     */
    public <S> String resolveTaskServiceBeanName(Class<S> taskServiceClass) {

        ApplicationContext requiredApplicationContext = getRequiredApplicationContext();
        String[] taskServiceBeanNames =
                requiredApplicationContext.getBeanNamesForType(taskServiceClass);

        if (taskServiceBeanNames.length == 0) {
            throw new IllegalStateException(
                    "No Spring bean found for task service "
                            + taskServiceClass.getName()
                            + ". Task services must be annotated with @Component or @Service.");
        }

        if (taskServiceBeanNames.length == 1) {
            return taskServiceBeanNames[0];
        }

        S taskServiceResolvedBySpring = requiredApplicationContext.getBean(taskServiceClass);
        List<String> matchingTaskServiceBeanNameList = Arrays.stream(taskServiceBeanNames)
                .filter(taskServiceBeanName ->
                        requiredApplicationContext.getBean(taskServiceBeanName) == taskServiceResolvedBySpring)
                .toList();

        if (matchingTaskServiceBeanNameList.size() == 1) {
            return matchingTaskServiceBeanNameList.getFirst();
        }

        throw new IllegalStateException(
                "Could not resolve a unique Spring bean name for task service "
                        + taskServiceClass.getName()
                        + ". Candidates: "
                        + Arrays.toString(taskServiceBeanNames));

    }

    /**
     * Resolve o bean funcional escolhido pelo Spring para o tipo declarado.
     */
    public <S> S resolveTaskService(Class<S> taskServiceClass) {

        String taskServiceBeanName = resolveTaskServiceBeanName(taskServiceClass);
        return getRequiredApplicationContext().getBean(taskServiceBeanName, taskServiceClass);

    }

    /**
     * Reconstrói uma task já persistida usando também o nome de bean gravado.
     */
    public Task<?, ?> createTaskInstance(ScheduledTaskAbstract scheduledTaskAbstract)
            throws ReflectiveOperationException {

        Class<? extends Task> taskClass =
                resolveTaskClass(scheduledTaskAbstract.getClasseTask());
        Class<?> taskServiceClass = resolveTaskServiceClass(taskClass);
        Object taskService = getRequiredApplicationContext().getBean(
                scheduledTaskAbstract.getBeanServico(),
                taskServiceClass);

        return createTask(
                taskClass,
                taskServiceClass,
                scheduledTaskAbstract,
                taskService);

    }

    /**
     * Cria uma instância de task para uma execução específica.
     *
     * <p>O construtor público esperado recebe somente o registro persistido, o
     * ciclo de vida de execução e o service funcional. Alterar essa assinatura
     * exige migrar todas as tasks de forma atômica; o nome de classe persistido
     * permanece compatível porque a assinatura do construtor não é gravada no
     * banco.</p>
     */
    public Task<?, ?> createTask(
            Class<?> taskClass,
            Class<?> taskServiceClass,
            ScheduledTaskAbstract scheduledTaskAbstract,
            Object taskService) throws ReflectiveOperationException {

        Constructor<?> taskConstructor = taskClass.getConstructor(
                ScheduledTaskAbstract.class,
                ScheduledTaskPersistenceService.class,
                taskServiceClass);

        return (Task<?, ?>) taskConstructor.newInstance(
                scheduledTaskAbstract,
                scheduledTaskPersistenceService,
                taskService);

    }

    /**
     * Reconstrói e executa imediatamente uma task já persistida.
     *
     * <p>Este é o ponto comum usado quando o runtime já decidiu executar agora:
     * Community o chama na mesma thread da requisição e o consumidor
     * Enterprise o chama depois de receber o id pela fila. O scheduler
     * recorrente web utiliza {@link #createTaskInstance(ScheduledTaskAbstract)}
     * porque precisa entregar o {@link Runnable} ao {@code TaskScheduler} em
     * vez de executá-lo durante a reconstrução feita no bootstrap.</p>
     */
    public void executePersistedTask(ScheduledTaskAbstract scheduledTaskAbstract)
            throws ReflectiveOperationException {

        createTaskInstance(scheduledTaskAbstract).run();

    }

    /**
     * Falha explicitamente quando o service é usado fora de um contexto Spring.
     *
     * <p>Em produção o campo é obrigatório. A validação local preserva um
     * diagnóstico inteligível também em testes unitários e em bootstrap
     * incompleto, antes que uma chamada reflexiva resulte em
     * {@link NullPointerException}.</p>
     */
    private ApplicationContext getRequiredApplicationContext() {

        if (applicationContext == null) {
            throw new IllegalStateException(
                    "ApplicationContext precisa estar configurado para executar Scheduled Task");
        }

        return applicationContext;

    }

}
