package com.opsfactor.community.platform.scheduler.facade;

import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.scheduler.services.*;
import com.opsfactor.community.platform.utility.Constantes.ModoExecucaoProcesso;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskExecution;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskImediato;
import com.opsfactor.community.platform.scheduler.facade.dto.TaskSchedulingDTO;
import com.opsfactor.community.platform.scheduler.exception.TaskSchedulingException;
import com.opsfactor.community.platform.scheduler.repository.ScheduledTaskAbstractRepository;
import com.opsfactor.community.platform.scheduler.repository.ScheduledTaskExecutionRepository;
import com.opsfactor.community.platform.scheduler.repository.ScheduledTaskImediatoRepository;
import com.opsfactor.community.platform.scheduler.repository.dto.ScheduledTaskHistoryRowSnapshot;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.Query;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Guarda o contrato Community do {@link TaskSchedulingService}.
 *
 * <p>O scheduler aberto e apenas um registrador de execucoes imediatas e
 * sincronas. Filas, recorrencia, workers, Service Bus e batch runners devem ser
 * adicionados pelo overlay Enterprise, nao por configuracao acidental deste
 * modulo.</p>
 */
public class TaskSchedulingServiceCommunityContractTest {

    @Test
    public void canonicalScheduledTaskClassPolicyShouldRejectLegacyNamespaceWithoutReflection() {

        Assertions.assertTrue(CanonicalScheduledTaskClassPolicy.isCanonicalTaskClassName(
                TestTask.class.getName()));
        Assertions.assertFalse(CanonicalScheduledTaskClassPolicy.isCanonicalTaskClassName(
                "com.opsfactor.planning.scheduler.LegacyTask"));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalScheduledTaskClassPolicy.validateCanonicalTaskClassName(
                        "com.opsfactor.planning.scheduler.LegacyTask"));

        Assertions.assertEquals(
                "Scheduled task class must belong to the Community or Enterprise runtime: "
                        + "com.opsfactor.planning.scheduler.LegacyTask",
                illegalArgumentException.getMessage());

    }

    @Test
    public void taskSchedulingServiceShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                TaskSchedulingService.class,
                "scheduledTaskAbstractRepository",
                "scheduledTaskImediatoRepository",
                "scheduledTaskPersistenceService",
                "scheduledTaskExecutionService");
        assertAutowiredFields(
                ScheduledTaskPersistenceService.class,
                "scheduledTaskAbstractRepository",
                "scheduledTaskExecutionRepository");
        assertAutowiredFields(
                ScheduledTaskExecutionService.class,
                "applicationContext",
                "scheduledTaskPersistenceService");

        Assertions.assertNotNull(TestTask.class.getDeclaredConstructor(
                ScheduledTaskAbstract.class,
                ScheduledTaskPersistenceService.class,
                TestService.class));
        Assertions.assertThrows(
                NoSuchMethodException.class,
                () -> TestTask.class.getDeclaredConstructor(
                        ScheduledTaskAbstract.class,
                        TaskSchedulingService.class,
                        TestService.class));

    }

    @Test
    public void scheduledTaskHistoryRepositoryShouldProjectStatusRowsWithoutLob() throws Exception {

        Method method = ScheduledTaskAbstractRepository.class.getDeclaredMethod(
                "findAllProcessStatusRows");
        Query query = method.getAnnotation(Query.class);

        Assertions.assertEquals(
                List.class,
                method.getReturnType(),
                "Snapshot de historico do scheduler deve preservar cardinalidade em List.");
        Assertions.assertNotNull(
                query,
                "Snapshot de historico do scheduler deve declarar constructor projection explicita.");
        Assertions.assertTrue(
                query.value().contains("ScheduledTaskHistoryRowSnapshot"),
                "Historico deve projetar somente os campos usados pelo Process Status.");
        Assertions.assertTrue(
                query.value().contains("LEFT JOIN scheduledTask.scheduledTaskExecutionSet execution"),
                "Projection deve manter tasks sem execucao e evitar N+1.");
        Assertions.assertFalse(
                query.value().contains("FETCH") || query.value().contains("mensagemErroStackTrace"),
                "Process Status nao deve materializar a entidade de execucao nem o stack trace LOB.");

        Transactional transactional = TaskSchedulingService.class
                .getDeclaredMethod("getTaskSchedulingDTOList")
                .getAnnotation(Transactional.class);
        Assertions.assertNotNull(transactional);
        Assertions.assertTrue(transactional.readOnly());

    }

    @Test
    public void taskSchedulingServiceShouldRejectAsyncBeforePersistenceAccess() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoComTask(
                        TestTask.class,
                        "TestProcess",
                        "TestProcess-admin-2026-06-24T00:00",
                        "admin",
                        "Async blocked in Community",
                        "UTC",
                        ModoExecucaoProcesso.ASYNC,
                        null));

    }

    @Test
    public void taskSchedulingServiceShouldRejectAsyncGeneratedIdBeforeTimezoneLookup() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoComTask(
                        TestTask.class,
                        "TestProcess",
                        "admin",
                        "Async generated id blocked in Community",
                        "Invalid/Timezone/ShouldNotBeRead",
                        ModoExecucaoProcesso.ASYNC,
                        null));

        /*
         * Esta sobrecarga gera o id internamente. O Community precisa validar o
         * modo antes de calcular data/hora no timezone para que ASYNC/BATCH
         * falhem sempre como capability Enterprise, nao como erro incidental de
         * timezone, id ou persistencia.
         */
        Assertions.assertEquals(
                "REQUIRES_ENTERPRISE_VERSION: Asynchronous or batch process execution requires OpsFactor Enterprise.",
                requiresEnterpriseVersionException.getMessage());

    }

    @Test
    public void taskSchedulingServiceShouldRejectNullExecutionModeBeforePersistenceAccess() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();

        /*
         * O service e instanciado sem repositories de proposito. O modo nulo
         * deve falhar como payload invalido antes de consultar persistencia,
         * refletindo o mesmo contrato publicado pela fachada web.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoComTask(
                        TestTask.class,
                        "TestProcess",
                        "TestProcess-admin-2026-06-24T00:00",
                        "admin",
                        "Null execution mode",
                        "UTC",
                        null,
                        null));

        Assertions.assertEquals(
                "Modo de execucao do processo nao pode ser nulo",
                illegalArgumentException.getMessage());

    }

    @Test
    public void taskSchedulingServiceShouldRejectBatchBeforePersistenceAccess() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoComTask(
                        TestTask.class,
                        "TestProcess",
                        "TestProcess-admin-2026-06-24T00:00",
                        "admin",
                        "Batch blocked in Community",
                        "UTC",
                        ModoExecucaoProcesso.BATCH,
                        null));

    }

    @Test
    public void taskSchedulingServiceShouldRejectNullSupplierBeforePersistenceAccess() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();

        /*
         * Supplier nulo e erro de contrato do caller. A falha precisa acontecer
         * antes de gerar/persistir ScheduledTaskImediato para nao criar uma
         * execucao sincronizada sem rotina funcional real.
         */
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoSincronoComSupplier(
                        null,
                        "TestProcess",
                        "admin",
                        "Null supplier",
                        "UTC"));

        Assertions.assertEquals(
                "Synchronous instant task supplier is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void inactiveInstantTaskShouldSkipSynchronousSupplierExecution() throws Exception {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskImediato scheduledTaskImediato = new ScheduledTaskImediato("Task-1");
        AtomicBoolean supplierExecutado = new AtomicBoolean(false);
        Supplier<String> supplier = () -> {
            supplierExecutado.set(true);
            return "executado";
        };
        scheduledTaskImediato.setAtivo(false);

        Method executeInstantTaskSameThreadMethod = TaskSchedulingService.class.getDeclaredMethod(
                "executeInstantTaskSameThread",
                ScheduledTaskImediato.class,
                Supplier.class);
        executeInstantTaskSameThreadMethod.setAccessible(true);
        Assertions.assertTrue(
                executeInstantTaskSameThreadMethod.isAnnotationPresent(Nullable.class),
                "executeInstantTaskSameThread must declare @Nullable because inactive tasks skip supplier execution.");

        /*
         * Task inativa representa execucao ja encerrada/cancelada no controle
         * persistido. O Community nao deve chamar novamente o supplier, pois nao
         * ha fila/worker capaz de reconciliar uma segunda execucao.
         */
        String resultado = (String) executeInstantTaskSameThreadMethod.invoke(
                taskSchedulingService,
                scheduledTaskImediato,
                supplier);

        Assertions.assertNull(resultado);
        Assertions.assertFalse(supplierExecutado.get());

    }

    @Test
    public void synchronousSupplierFailureShouldPreserveCauseInTaskSchedulingException() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskImediatoRepository scheduledTaskImediatoRepository =
                Mockito.mock(ScheduledTaskImediatoRepository.class);
        ScheduledTaskExecutionRepository scheduledTaskExecutionRepository =
                Mockito.mock(ScheduledTaskExecutionRepository.class);
        IllegalStateException supplierFailure = new IllegalStateException("supplier failed");

        Mockito.when(scheduledTaskImediatoRepository.customFindById(Mockito.anyString()))
                .thenReturn(Optional.empty());
        Mockito.when(scheduledTaskImediatoRepository.save(Mockito.any(ScheduledTaskImediato.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        Mockito.when(scheduledTaskExecutionRepository.save(Mockito.any(ScheduledTaskExecution.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskImediatoRepository",
                scheduledTaskImediatoRepository);
        injectPersistenceService(
                taskSchedulingService,
                null,
                scheduledTaskExecutionRepository);

        /*
         * O supplier sincrono ja grava mensagem resumida e stack trace em
         * ScheduledTaskExecution. A excecao relancada para a API deve manter a
         * mesma mensagem publica e tambem encadear a falha original.
         */
        TaskSchedulingException taskSchedulingException = Assertions.assertThrows(
                TaskSchedulingException.class,
                () -> taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoSincronoComSupplier(
                        () -> {
                            throw supplierFailure;
                        },
                        "TestProcess",
                        "admin",
                        "Broken supplier",
                        "UTC"));

        Assertions.assertEquals("supplier failed", taskSchedulingException.getMessage());
        Assertions.assertSame(supplierFailure, taskSchedulingException.getCause());
        Mockito.verify(scheduledTaskExecutionRepository, Mockito.times(2))
                .save(Mockito.any(ScheduledTaskExecution.class));

    }

    @Test
    public void synchronousTaskFailureShouldReturnFailureAfterPersistingProcessStatus() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskImediatoRepository scheduledTaskImediatoRepository =
                Mockito.mock(ScheduledTaskImediatoRepository.class);
        ScheduledTaskAbstractRepository scheduledTaskAbstractRepository =
                Mockito.mock(ScheduledTaskAbstractRepository.class);
        ScheduledTaskExecutionRepository scheduledTaskExecutionRepository =
                Mockito.mock(ScheduledTaskExecutionRepository.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        ScheduledTaskPersistenceService scheduledTaskPersistenceService =
                new ScheduledTaskPersistenceService();
        ScheduledTaskExecutionService scheduledTaskExecutionService =
                new ScheduledTaskExecutionService();
        TestService testService = new TestService();

        Mockito.when(scheduledTaskImediatoRepository.customFindById("Task-1"))
                .thenReturn(Optional.empty());
        Mockito.when(scheduledTaskImediatoRepository.save(Mockito.any(ScheduledTaskImediato.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        Mockito.when(scheduledTaskAbstractRepository.save(Mockito.any(ScheduledTaskAbstract.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        Mockito.when(scheduledTaskExecutionRepository.save(Mockito.any(ScheduledTaskExecution.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        Mockito.when(applicationContext.getBeanNamesForType(TestService.class))
                .thenReturn(new String[]{"testService"});
        Mockito.when(applicationContext.getBean("testService", TestService.class))
                .thenReturn(testService);

        ReflectionTestUtils.setField(
                scheduledTaskPersistenceService,
                "scheduledTaskAbstractRepository",
                scheduledTaskAbstractRepository);
        ReflectionTestUtils.setField(
                scheduledTaskPersistenceService,
                "scheduledTaskExecutionRepository",
                scheduledTaskExecutionRepository);
        ReflectionTestUtils.setField(
                scheduledTaskExecutionService,
                "applicationContext",
                applicationContext);
        ReflectionTestUtils.setField(
                scheduledTaskExecutionService,
                "scheduledTaskPersistenceService",
                scheduledTaskPersistenceService);
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskImediatoRepository",
                scheduledTaskImediatoRepository);
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskPersistenceService",
                scheduledTaskPersistenceService);
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskExecutionService",
                scheduledTaskExecutionService);

        /*
         * Task.run() grava a falha e continua compatível com Runnable para o
         * runner Enterprise. O coordenador sincrono deve inspecionar esse
         * resultado e relancar uma falha funcional somente depois de salvar o
         * mesmo registro exibido no Process Status.
         */
        TaskSchedulingException taskSchedulingException = Assertions.assertThrows(
                TaskSchedulingException.class,
                () -> taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoSincronoComTask(
                        FailingTestTask.class,
                        "TestProcess",
                        "Task-1",
                        "admin",
                        "Failing task",
                        "UTC",
                        null));

        Assertions.assertEquals(
                "IllegalStateException: planned task failure",
                taskSchedulingException.getMessage());
        Mockito.verify(scheduledTaskExecutionRepository, Mockito.times(2))
                .save(Mockito.any(ScheduledTaskExecution.class));
        Mockito.verify(scheduledTaskAbstractRepository)
                .save(Mockito.any(ScheduledTaskAbstract.class));
        Mockito.verify(scheduledTaskImediatoRepository)
                .save(Mockito.any(ScheduledTaskImediato.class));

    }

    @Test
    public void taskSchedulingServiceShouldTreatDuplicateInstantTaskIdAsStateConflict() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskImediatoRepository scheduledTaskImediatoRepository =
                Mockito.mock(ScheduledTaskImediatoRepository.class);
        ScheduledTaskImediato scheduledTaskImediatoExistente =
                new ScheduledTaskImediato("TestProcess-admin-2026-06-24T00:00");

        Mockito.when(scheduledTaskImediatoRepository.customFindById("TestProcess-admin-2026-06-24T00:00"))
                .thenReturn(Optional.of(scheduledTaskImediatoExistente));
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskImediatoRepository",
                scheduledTaskImediatoRepository);

        /*
         * Duplicidade de id e conflito de estado persistido, nao feature ausente.
         * A validacao precisa acontecer antes de resolver ApplicationContext ou
         * instanciar a task, para que a mensagem operacional fique clara.
         */
        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> taskSchedulingService.criaSalvaEExecutaScheduledTaskImediatoSincronoComTask(
                        TestTask.class,
                        "TestProcess",
                        "TestProcess-admin-2026-06-24T00:00",
                        "admin",
                        "Duplicate id",
                        "UTC",
                        null));

        Assertions.assertTrue(
                illegalStateException.getMessage().contains("already exists"));
        Mockito.verify(scheduledTaskImediatoRepository)
                .customFindById("TestProcess-admin-2026-06-24T00:00");
        Mockito.verifyNoMoreInteractions(scheduledTaskImediatoRepository);

    }

    @Test
    public void taskSchedulingServiceShouldPersistResolvedTaskServiceBeanName() throws Exception {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskImediatoRepository scheduledTaskImediatoRepository =
                Mockito.mock(ScheduledTaskImediatoRepository.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);

        Mockito.when(scheduledTaskImediatoRepository.customFindById("Task-1"))
                .thenReturn(Optional.empty());
        Mockito.when(scheduledTaskImediatoRepository.save(Mockito.any(ScheduledTaskImediato.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        Mockito.when(applicationContext.getBeanNamesForType(TestService.class))
                .thenReturn(new String[]{"testService"});
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskImediatoRepository",
                scheduledTaskImediatoRepository);
        injectExecutionService(taskSchedulingService, applicationContext);

        /*
         * O nome do bean fica gravado no ScheduledTask para auditoria e para o
         * overlay Enterprise conseguir recarregar a task em worker separado.
         * A resolucao precisa ser explicita, sem depender da primeira posicao
         * de um array opaco do ApplicationContext.
         */
        ScheduledTaskImediato scheduledTaskImediato =
                taskSchedulingService.criaScheduledTaskImediato(
                        TestTask.class,
                        "TestProcess",
                        "Task-1",
                        "admin",
                        "Resolved service bean",
                        "UTC",
                        null);

        Assertions.assertEquals("testService", scheduledTaskImediato.getBeanServico());
        Mockito.verify(applicationContext)
                .getBeanNamesForType(TestService.class);
        Mockito.verifyNoMoreInteractions(applicationContext);

    }

    @Test
    public void taskSchedulingServiceShouldRejectNullSavedInstantTaskSnapshot() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskImediatoRepository scheduledTaskImediatoRepository =
                Mockito.mock(ScheduledTaskImediatoRepository.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);

        Mockito.when(scheduledTaskImediatoRepository.customFindById("Task-1"))
                .thenReturn(Optional.empty());
        Mockito.when(scheduledTaskImediatoRepository.save(Mockito.any(ScheduledTaskImediato.class)))
                .thenReturn(null);
        Mockito.when(applicationContext.getBeanNamesForType(TestService.class))
                .thenReturn(new String[]{"testService"});
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskImediatoRepository",
                scheduledTaskImediatoRepository);
        injectExecutionService(taskSchedulingService, applicationContext);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> taskSchedulingService.criaScheduledTaskImediato(
                        TestTask.class,
                        "TestProcess",
                        "Task-1",
                        "admin",
                        "Broken saved snapshot",
                        "UTC",
                        null));

        Assertions.assertEquals(
                "Saved scheduled task snapshot is required.",
                illegalStateException.getMessage());

    }

    @Test
    public void taskSchedulingServiceShouldRejectNullSavedTaskExecutionSnapshot() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskExecutionRepository scheduledTaskExecutionRepository =
                Mockito.mock(ScheduledTaskExecutionRepository.class);
        ScheduledTaskImediato scheduledTaskImediato = new ScheduledTaskImediato("Task-1");
        scheduledTaskImediato.setTimeZone("UTC");

        Mockito.when(scheduledTaskExecutionRepository.save(Mockito.any(ScheduledTaskExecution.class)))
                .thenReturn(null);
        injectPersistenceService(
                taskSchedulingService,
                null,
                scheduledTaskExecutionRepository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> taskSchedulingService.criaNovoTaskExecutionESalva(scheduledTaskImediato));

        Assertions.assertEquals(
                "Saved scheduled task execution snapshot is required.",
                illegalStateException.getMessage());

    }

    @Test
    public void taskSchedulingServiceShouldRejectNullSavedTaskAbstractSnapshot() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskAbstractRepository scheduledTaskAbstractRepository =
                Mockito.mock(ScheduledTaskAbstractRepository.class);
        ScheduledTaskImediato scheduledTaskImediato = new ScheduledTaskImediato("Task-1");

        Mockito.when(scheduledTaskAbstractRepository.save(Mockito.any(ScheduledTaskAbstract.class)))
                .thenReturn(null);
        injectPersistenceService(
                taskSchedulingService,
                scheduledTaskAbstractRepository,
                null);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> taskSchedulingService.saveScheduledTaskAbstract(scheduledTaskImediato));

        Assertions.assertEquals(
                "Saved scheduled task snapshot is required.",
                illegalStateException.getMessage());

    }

    @Test
    public void taskSchedulingServiceShouldRejectTaskWithoutSpringServiceBean() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskImediatoRepository scheduledTaskImediatoRepository =
                Mockito.mock(ScheduledTaskImediatoRepository.class);
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);

        Mockito.when(scheduledTaskImediatoRepository.customFindById("Task-1"))
                .thenReturn(Optional.empty());
        Mockito.when(applicationContext.getBeanNamesForType(TestService.class))
                .thenReturn(new String[0]);
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskImediatoRepository",
                scheduledTaskImediatoRepository);
        injectExecutionService(taskSchedulingService, applicationContext);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> taskSchedulingService.criaScheduledTaskImediato(
                        TestTask.class,
                        "TestProcess",
                        "Task-1",
                        "admin",
                        "Missing service bean",
                        "UTC",
                        null));

        Assertions.assertEquals(
                "No Spring bean found for task service "
                        + TestService.class.getName()
                        + ". Task services must be annotated with @Component or @Service.",
                illegalStateException.getMessage());
        Mockito.verify(scheduledTaskImediatoRepository)
                .customFindById("Task-1");
        Mockito.verifyNoMoreInteractions(scheduledTaskImediatoRepository);

    }

    @Test
    public void taskSchedulingServiceShouldRejectClassNameThatDoesNotExtendTaskBeforePersistenceAccess() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> taskSchedulingService.criaScheduledTaskImediato(
                        TestService.class.getName(),
                        "TestProcess",
                        "Task-1",
                        "admin",
                        "Invalid task class",
                        "UTC",
                        null));

        Assertions.assertEquals(
                "Scheduled task class "
                        + TestService.class.getName()
                        + " must extend "
                        + Task.class.getName()
                        + ".",
                illegalStateException.getMessage());

    }

    @Test
    public void scheduledTaskShouldTreatDuplicateExecutionAsStateConflict() {

        ScheduledTaskImediato scheduledTaskImediato = new ScheduledTaskImediato("Task-1");
        ScheduledTaskExecution scheduledTaskExecution = new ScheduledTaskExecution(
                new ScheduledTaskExecution.ScheduledTaskExecutionCompositeKey(
                        scheduledTaskImediato,
                        1L));

        scheduledTaskImediato.addScheduledTaskExecution(scheduledTaskExecution);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> scheduledTaskImediato.addScheduledTaskExecution(scheduledTaskExecution));

        Assertions.assertTrue(
                illegalStateException.getMessage().contains("already present at Scheduled Task Task-1"));

    }

    @Test
    public void taskSchedulingDtoShouldExposeCommunityHistoryFieldsAndSharedEnterpriseCronSlot() {

        List<String> declaredFieldNameList = Arrays
                .stream(TaskSchedulingDTO.class.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(Field::getName)
                .toList();

        Assertions.assertEquals(
                List.of(
                        "taskType",
                        "taskId",
                        "processType",
                        "active",
                        "userId",
                        "description",
                        "timeZone",
                        "taskCreationTime",
                        "scheduledExecutionTime",
                        "cronExpression",
                        "taskInstance",
                        "startTime",
                        "endTime",
                        "errorMessage"),
                declaredFieldNameList,
                "DTO Community deve preservar o slot cron compartilhado para o overlay Enterprise, "
                        + "sem expor fila ou cancelamento no runtime aberto.");

    }

    @Test
    public void immediateTaskDtoFillShouldUseCommunityTypeHookAndLeaveCronEmpty() throws Exception {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        TaskSchedulingDTO taskSchedulingDTO = new TaskSchedulingDTO();
        ScheduledTaskImediato scheduledTaskImediato = new ScheduledTaskImediato("Task-1");
        LocalDateTime horarioCriacao = LocalDateTime.of(2026, 6, 24, 10, 30);
        scheduledTaskImediato.setHorarioCriacao(horarioCriacao);

        Method method = TaskSchedulingService.class.getDeclaredMethod(
                "preencheDadosTaskSchedulingPorTipo",
                TaskSchedulingDTO.class,
                ScheduledTaskAbstract.class);
        method.setAccessible(true);
        method.invoke(taskSchedulingService, taskSchedulingDTO, scheduledTaskImediato);

        Assertions.assertEquals("Instant", taskSchedulingDTO.getTaskType());
        Assertions.assertEquals(horarioCriacao, taskSchedulingDTO.getScheduledExecutionTime());
        Assertions.assertNull(
                taskSchedulingDTO.getCronExpression(),
                "Community nao possui task recorrente; somente o override Enterprise preenche cronExpression.");

    }

    @Test
    public void getTaskSchedulingDTOListShouldRejectNullHistorySnapshot() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskAbstractRepository scheduledTaskAbstractRepository =
                Mockito.mock(ScheduledTaskAbstractRepository.class);

        Mockito.when(scheduledTaskAbstractRepository.findAllProcessStatusRows())
                .thenReturn(null);
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskAbstractRepository",
                scheduledTaskAbstractRepository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                taskSchedulingService::getTaskSchedulingDTOList);

        Assertions.assertEquals(
                "Scheduled task history snapshot is required.",
                illegalStateException.getMessage());

    }

    @Test
    public void getTaskSchedulingDTOListShouldRejectBrokenHistoryItem() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskAbstractRepository scheduledTaskAbstractRepository =
                Mockito.mock(ScheduledTaskAbstractRepository.class);

        Mockito.when(scheduledTaskAbstractRepository.findAllProcessStatusRows())
                .thenReturn(List.of(new ScheduledTaskHistoryRowSnapshot(
                        new ScheduledTaskImediato(),
                        null,
                        null,
                        null,
                        null)));
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskAbstractRepository",
                scheduledTaskAbstractRepository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                taskSchedulingService::getTaskSchedulingDTOList);

        Assertions.assertEquals(
                "Saved scheduled task id is required.",
                illegalStateException.getMessage());

    }

    @Test
    public void getTaskSchedulingDTOListShouldPreserveTaskWithoutExecution() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskAbstractRepository scheduledTaskAbstractRepository =
                Mockito.mock(ScheduledTaskAbstractRepository.class);
        ScheduledTaskImediato scheduledTaskImediato = new ScheduledTaskImediato("Task-1");
        scheduledTaskImediato.setHorarioCriacao(LocalDateTime.of(2026, 8, 19, 10, 0));

        Mockito.when(scheduledTaskAbstractRepository.findAllProcessStatusRows())
                .thenReturn(List.of(new ScheduledTaskHistoryRowSnapshot(
                        scheduledTaskImediato,
                        null,
                        null,
                        null,
                        null)));
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskAbstractRepository",
                scheduledTaskAbstractRepository);

        List<TaskSchedulingDTO> taskSchedulingDTOList =
                taskSchedulingService.getTaskSchedulingDTOList();

        Assertions.assertEquals(1, taskSchedulingDTOList.size());
        Assertions.assertEquals("Task-1", taskSchedulingDTOList.getFirst().getTaskId());
        Assertions.assertNull(taskSchedulingDTOList.getFirst().getTaskInstance());

    }

    @Test
    public void getTaskSchedulingDTOListShouldRejectDuplicatedHistoryExecutionKeyBeforeDtoBuild() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskAbstractRepository scheduledTaskAbstractRepository =
                Mockito.mock(ScheduledTaskAbstractRepository.class);

        ScheduledTaskImediato scheduledTaskImediato = new ScheduledTaskImediato("Task-1");
        Mockito.when(scheduledTaskAbstractRepository.findAllProcessStatusRows())
                .thenReturn(List.of(
                        new ScheduledTaskHistoryRowSnapshot(
                                scheduledTaskImediato,
                                1L,
                                null,
                                null,
                                null),
                        new ScheduledTaskHistoryRowSnapshot(
                                scheduledTaskImediato,
                                1L,
                                null,
                                null,
                                null)));
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskAbstractRepository",
                scheduledTaskAbstractRepository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                taskSchedulingService::getTaskSchedulingDTOList);

        Assertions.assertEquals(
                "Scheduled task history snapshot has duplicated task execution key Task-1#1.",
                illegalStateException.getMessage());

    }

    @Test
    public void deleteScheduledTasksShouldRejectMissingSelectionBeforeRepository() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();

        IllegalArgumentException nullSelectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> taskSchedulingService.deleteScheduledTasks(null));

        Assertions.assertEquals(
                "Scheduled task history selection is required for delete.",
                nullSelectionException.getMessage());

        IllegalArgumentException emptySelectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> taskSchedulingService.deleteScheduledTasks(List.of()));

        Assertions.assertEquals(
                "Scheduled task history selection is required for delete.",
                emptySelectionException.getMessage());

    }

    @Test
    public void deleteScheduledTasksShouldRejectBrokenSelectionItemBeforeRepository() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        TaskSchedulingDTO taskSchedulingDTO = new TaskSchedulingDTO();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> taskSchedulingService.deleteScheduledTasks(List.of(taskSchedulingDTO)));

        Assertions.assertEquals(
                "Scheduled task history selection item at index 0 must have a task id for delete.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void deleteScheduledTasksShouldDeleteSelectedTaskIds() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskAbstractRepository scheduledTaskAbstractRepository =
                Mockito.mock(ScheduledTaskAbstractRepository.class);
        TaskSchedulingDTO taskSchedulingDTO = new TaskSchedulingDTO();
        taskSchedulingDTO.setTaskId("Task-1");

        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskAbstractRepository",
                scheduledTaskAbstractRepository);

        Mockito.when(scheduledTaskAbstractRepository.existsByIdInAndAtivoTrue(List.of("Task-1")))
                .thenReturn(false);

        taskSchedulingService.deleteScheduledTasks(List.of(taskSchedulingDTO));

        Mockito.verify(scheduledTaskAbstractRepository)
                .existsByIdInAndAtivoTrue(List.of("Task-1"));
        Mockito.verify(scheduledTaskAbstractRepository)
                .deleteAllById(List.of("Task-1"));
        Mockito.verifyNoMoreInteractions(scheduledTaskAbstractRepository);

    }

    @Test
    public void deleteScheduledTasksShouldRejectActiveTaskBeforeDelete() {

        TaskSchedulingService taskSchedulingService = new TaskSchedulingService();
        ScheduledTaskAbstractRepository scheduledTaskAbstractRepository =
                Mockito.mock(ScheduledTaskAbstractRepository.class);
        TaskSchedulingDTO taskSchedulingDTO = new TaskSchedulingDTO();
        taskSchedulingDTO.setTaskId("Task-active");

        Mockito.when(scheduledTaskAbstractRepository.existsByIdInAndAtivoTrue(List.of("Task-active")))
                .thenReturn(true);
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskAbstractRepository",
                scheduledTaskAbstractRepository);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> taskSchedulingService.deleteScheduledTasks(List.of(taskSchedulingDTO)));

        Assertions.assertEquals(
                "Active scheduled tasks cannot be deleted from Process Status. Wait for completion.",
                illegalStateException.getMessage());
        Mockito.verify(scheduledTaskAbstractRepository)
                .existsByIdInAndAtivoTrue(List.of("Task-active"));
        Mockito.verify(scheduledTaskAbstractRepository, Mockito.never())
                .deleteAllById(Mockito.anyCollection());

    }

    @Test
    public void taskSchedulingServiceShouldNotUseGenericExceptionCatch() throws Exception {

        Path taskSchedulingServicePath = Path.of(
                "src/main/java/com/opsfactor/community/platform/scheduler/facade/TaskSchedulingService.java");
        String taskSchedulingServiceSource = Files.readString(
                taskSchedulingServicePath,
                StandardCharsets.UTF_8);

        /*
         * Task.run() continua sendo o limite que captura checked exceptions da
         * task concreta para salvar Process Status. O service central, por sua
         * vez, deve capturar apenas falhas reflexivas/runtime dos caminhos
         * sincronizados que ele mesmo coordena.
         */
        Assertions.assertFalse(
                taskSchedulingServiceSource.contains("catch (Exception"),
                "TaskSchedulingService deve evitar captura generica de Exception.");

    }

    private static void assertAutowiredFields(Class<?> serviceClass, String... fieldNameArray) throws Exception {

        for (String fieldName : fieldNameArray) {
            Field field = serviceClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);
            Assertions.assertNotNull(
                    autowired,
                    serviceClass.getSimpleName() + "." + fieldName + " deve declarar @Autowired explicitamente.");
            Assertions.assertTrue(
                    autowired.required(),
                    serviceClass.getSimpleName() + "." + fieldName + " deve ser bean obrigatorio.");
        }

    }

    private static class TestTask extends Task<Void, TestService> {

        public TestTask(
                ScheduledTaskAbstract scheduledTaskAbstract,
                ScheduledTaskPersistenceService scheduledTaskPersistenceService,
                TestService testService) {

            super(scheduledTaskAbstract, scheduledTaskPersistenceService, testService);

        }

        @Override
        public void executaTask(Void dtoParametros, TestService testService) {

        }

    }

    public static class FailingTestTask extends Task<Void, TestService> {

        public FailingTestTask(
                ScheduledTaskAbstract scheduledTaskAbstract,
                ScheduledTaskPersistenceService scheduledTaskPersistenceService,
                TestService testService) {

            super(scheduledTaskAbstract, scheduledTaskPersistenceService, testService);

        }

        @Override
        public void executaTask(Void dtoParametros, TestService testService) {

            throw new IllegalStateException("planned task failure");

        }

    }

    private static class TestService {

    }

    private static void injectPersistenceService(
            TaskSchedulingService taskSchedulingService,
            ScheduledTaskAbstractRepository scheduledTaskAbstractRepository,
            ScheduledTaskExecutionRepository scheduledTaskExecutionRepository) {

        ScheduledTaskPersistenceService scheduledTaskPersistenceService =
                new ScheduledTaskPersistenceService();

        if (scheduledTaskAbstractRepository != null) {
            ReflectionTestUtils.setField(
                    scheduledTaskPersistenceService,
                    "scheduledTaskAbstractRepository",
                    scheduledTaskAbstractRepository);
        }
        if (scheduledTaskExecutionRepository != null) {
            ReflectionTestUtils.setField(
                    scheduledTaskPersistenceService,
                    "scheduledTaskExecutionRepository",
                    scheduledTaskExecutionRepository);
        }

        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskPersistenceService",
                scheduledTaskPersistenceService);

    }

    private static void injectExecutionService(
            TaskSchedulingService taskSchedulingService,
            ApplicationContext applicationContext) {

        ScheduledTaskExecutionService scheduledTaskExecutionService =
                new ScheduledTaskExecutionService();
        ReflectionTestUtils.setField(
                scheduledTaskExecutionService,
                "applicationContext",
                applicationContext);
        ReflectionTestUtils.setField(
                taskSchedulingService,
                "scheduledTaskExecutionService",
                scheduledTaskExecutionService);

    }

}
