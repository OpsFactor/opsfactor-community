package com.opsfactor.community.platform.scheduler.facade;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskExecutionService;
import com.opsfactor.community.platform.scheduler.services.ScheduledTaskPersistenceService;
import com.opsfactor.community.platform.scheduler.services.Task;
import com.opsfactor.community.platform.utility.Constantes.ModoExecucaoProcesso;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskExecution;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskImediato;
import com.opsfactor.community.platform.scheduler.facade.dto.TaskSchedulingDTO;
import com.opsfactor.community.platform.scheduler.exception.TaskSchedulingException;
import com.opsfactor.community.platform.scheduler.repository.ScheduledTaskAbstractRepository;
import com.opsfactor.community.platform.scheduler.repository.ScheduledTaskImediatoRepository;
import com.opsfactor.community.platform.scheduler.repository.dto.ScheduledTaskHistoryRowSnapshot;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * Servico central de registro e execucao imediata de tarefas Community.
 *
 * <p>O modulo Community nao possui fila, worker batch, recorrencia real,
 * consumidor cloud ou processamento em background. Esta classe preserva o
 * contrato tecnico de scheduler usado pelos controllers e pelas tasks, mas
 * executa tudo na propria thread da chamada. Implementacoes Enterprise podem
 * estender esse desenho com filas e runners dedicados, sem reintroduzir esses
 * conceitos no repositorio aberto.</p>
 */
@Slf4j
@Service
public class TaskSchedulingService {

    /**
     * Repository da entidade base usada pela tela de Process Status para listar
     * historico de tasks imediatas e suas execucoes.
     */
    @Autowired
    private ScheduledTaskAbstractRepository scheduledTaskAbstractRepository;

    /**
     * Repository especializado em tarefas imediatas. No Community este e o
     * unico tipo concreto de {@link ScheduledTaskAbstract} que deve ser criado
     * pelo scheduler.
     */
    @Autowired
    private ScheduledTaskImediatoRepository scheduledTaskImediatoRepository;

    /**
     * Persistência compartilhada de tasks e de seu histórico de execuções.
     *
     * <p>O coordenador mantém este campo explícito para deixar visível que
     * gravar estado é uma responsabilidade separada da decisão de como
     * executar. Os métodos públicos legados de persistência abaixo delegam para
     * este componente durante a migração e preservam compatibilidade de fonte.</p>
     */
    @Autowired
    private ScheduledTaskPersistenceService scheduledTaskPersistenceService;

    /**
     * Resolve o service funcional, constrói a instância de {@link Task} e
     * executa registros já persistidos.
     *
     * <p>Community usa este componente na mesma thread da chamada; Enterprise
     * reutiliza o mesmo contrato tanto no consumidor de fila quanto no
     * scheduler recorrente web.</p>
    */
    @Autowired
    private ScheduledTaskExecutionService scheduledTaskExecutionService;

    /**
     * Expõe o executor compartilhado somente para especializações reais do
     * coordenador, mantendo a dependência injetada encapsulada.
     */
    protected ScheduledTaskExecutionService getScheduledTaskExecutionService() {

        return scheduledTaskExecutionService;

    }

    /**
     * Cria, salva e executa uma task imediata conforme o modo solicitado.
     *
     * <p>No Community somente {@link ModoExecucaoProcesso#SYNC} e aceito.
     * Qualquer outro modo falha antes de tocar repositories ou instanciar tasks,
     * garantindo que payloads de front/legado nao ativem filas ou batch por
     * acidente.</p>
     */
    public <A,S,T extends Task<A,S>> ScheduledTaskImediato criaSalvaEExecutaScheduledTaskImediatoComTask(
            Class<T> classeTask, String tipoProcesso,
            String scheduledTaskId, String userId, String descricao, String timeZoneId,
            ModoExecucaoProcesso modoExecucaoProcesso,
            A dtoParametros) throws ReflectiveOperationException, JsonProcessingException {

        validaModoExecucaoProcessoCommunity(modoExecucaoProcesso);
        return criaSalvaEExecutaScheduledTaskImediatoSincronoComTask(
                classeTask, tipoProcesso, scheduledTaskId, userId, descricao, timeZoneId, dtoParametros);

    }

    /**
     * Classifica o modo de execucao recebido antes de tocar persistencia.
     *
     * <p>Modo nulo e payload invalido do caller. Modos assíncronos ou batch sao
     * capabilities Enterprise, porque exigem fila/worker/job que nao existem no
     * artefato Community.</p>
     */
    private void validaModoExecucaoProcessoCommunity(ModoExecucaoProcesso modoExecucaoProcesso) {

        if (modoExecucaoProcesso == null) {
            throw new IllegalArgumentException("Modo de execucao do processo nao pode ser nulo");
        }

        if (!ModoExecucaoProcesso.SYNC.equals(modoExecucaoProcesso)) {
            throw new RequiresEnterpriseVersionException("Asynchronous or batch process execution");
        }

    }

    /**
     * Gera o identificador tecnico da task a partir de processo, usuario e
     * horario no timezone operacional, entao delega para a sobrecarga principal.
     *
     * <p>A validacao do modo vem antes da geracao do id para manter a borda de
     * edicao como primeiro erro funcional. Em runtime Community, payloads
     * {@code ASYNC}/{@code BATCH} nao devem chegar sequer ao lookup de timezone,
     * pois esses modos pertencem ao overlay Enterprise.</p>
     */
    public <A,S,T extends Task<A,S>> ScheduledTaskImediato criaSalvaEExecutaScheduledTaskImediatoComTask(
            Class<T> classeTask, String tipoProcesso,
            String userId, String descricao, String timeZoneId,
            ModoExecucaoProcesso modoExecucaoProcesso,
            A dtoParametros) throws ReflectiveOperationException, JsonProcessingException {

        validaModoExecucaoProcessoCommunity(modoExecucaoProcesso);
        String scheduledTaskId = getScheduledTaskIdDeTipoProcessoEUserIdEDataHora(tipoProcesso, userId, timeZoneId);

        return criaSalvaEExecutaScheduledTaskImediatoComTask(
                classeTask, tipoProcesso, scheduledTaskId, userId, descricao, timeZoneId,
                modoExecucaoProcesso, dtoParametros);

    }

    /**
     * Cria e persiste o registro de uma task imediata sem executa-la.
     *
     * <p>Este metodo e usado como etapa interna da execucao sincronizada e tambem
     * permanece como contrato tecnico para fluxos que precisem materializar o
     * status antes de disparar a rotina. O {@code beanServico} gravado e o nome
     * do service Spring associado ao tipo generico da {@link Task}.</p>
     */
    public <A,S,T extends Task<A,S>> ScheduledTaskImediato criaScheduledTaskImediato(
            Class<T> classeTask, String tipoProcesso, String scheduledTaskId, String userId, String descricao,
            String timeZoneId, A dtoParametros) throws JsonProcessingException {

        Class<S> classeServico = (Class<S>) resolveClasseServicoTask(classeTask);

        validaScheduledTaskImediatoAindaNaoExiste(scheduledTaskId);

        ScheduledTaskImediato scheduledTaskImediato = new ScheduledTaskImediato(scheduledTaskId);

        scheduledTaskImediato.setAtivo(true);
        scheduledTaskImediato.setUserId(userId);
        scheduledTaskImediato.setTimeZone(timeZoneId);
        scheduledTaskImediato.setDescricao(descricao);
        scheduledTaskImediato.setTipoProcesso(tipoProcesso);
        scheduledTaskImediato.setClasseTask(classeTask.getName());
        scheduledTaskImediato.setBeanServico(resolveNomeBeanServicoTask(classeServico));
        scheduledTaskImediato.setHorarioCriacao(scheduledTaskImediato.getDataHorarioAtualNoTimeZone());
        scheduledTaskImediato.setConfiguracoesExecucaoJson(getParametrosComoStringJson(dtoParametros));

        ScheduledTaskImediato scheduledTaskImediatoSalvo =
                scheduledTaskImediatoRepository.save(scheduledTaskImediato);
        validaScheduledTaskImediatoSalvoCommunity(scheduledTaskImediatoSalvo);
        return scheduledTaskImediatoSalvo;

    }

    /**
     * Cria task imediata gerando automaticamente o identificador tecnico.
     */
    public <A,S,T extends Task<A,S>> ScheduledTaskImediato criaScheduledTaskImediato(
            Class<T> classeTask, String tipoProcesso, String userId, String descricao, String timeZoneId,
            A dtoParametros) throws JsonProcessingException {

        String scheduledTaskId = getScheduledTaskIdDeTipoProcessoEUserIdEDataHora(tipoProcesso, userId, timeZoneId);
        return criaScheduledTaskImediato(classeTask, tipoProcesso, scheduledTaskId, userId, descricao, timeZoneId, dtoParametros);

    }

    /**
     * Resolve dinamicamente a classe da task por nome totalmente qualificado e
     * cria o registro imediato correspondente.
     */
    public <A,S,T extends Task<A,S>> ScheduledTaskImediato criaScheduledTaskImediato(
            String classeTaskComoString, String tipoProcesso, String scheduledTaskId, String userId, String descricao,
            String timeZoneId, A dtoParametros) throws ClassNotFoundException, JsonProcessingException {

        Class<T> classeTask = (Class<T>) resolveClasseTask(classeTaskComoString);

        return criaScheduledTaskImediato(classeTask, tipoProcesso, scheduledTaskId, userId, descricao, timeZoneId, dtoParametros);

    }

    /**
     * Resolve dinamicamente a classe da task por nome e gera automaticamente o
     * identificador tecnico da task imediata.
     */
    public <A,S,T extends Task<A,S>> ScheduledTaskImediato criaScheduledTaskImediato(
            String classeTaskComoString, String tipoProcesso, String userId, String descricao, String timeZoneId,
            A dtoParametros) throws ClassNotFoundException, JsonProcessingException {

        Class<T> classeTask = (Class<T>) resolveClasseTask(classeTaskComoString);
        String scheduledTaskId = getScheduledTaskIdDeTipoProcessoEUserIdEDataHora(tipoProcesso, userId, timeZoneId);

        return criaScheduledTaskImediato(classeTask, tipoProcesso, scheduledTaskId, userId, descricao, timeZoneId, dtoParametros);

    }

    /**
     * Executa uma {@link Task} imediatamente na thread da chamada.
     *
     * <p>A task e criada por reflexao porque o scheduler precisa passar para o
     * construtor o registro persistido, o componente de persistência e o
     * service funcional resolvido no contexto Spring. Nenhum estado de rodada
     * fica guardado em beans singleton.</p>
     */
    public <A,S,T extends Task<A,S>> ScheduledTaskImediato criaSalvaEExecutaScheduledTaskImediatoSincronoComTask(
            Class<T> classeTask, String tipoProcesso, String scheduledTaskId, String userId, String descricao,
            String timeZoneId, A dtoParametros) throws ReflectiveOperationException, JsonProcessingException {

        validaScheduledTaskImediatoAindaNaoExiste(scheduledTaskId);

        Class<S> classeServico = (Class<S>) resolveClasseServicoTask(classeTask);
        /*
         * Resolve o bean funcional antes de construir a Task. A Task em si nao
         * e singleton Spring, mas o service que executa a regra concreta deve
         * existir no ApplicationContext para preservar escopo transacional,
         * repositories e demais dependencias declaradas com @Autowired.
         */
        S servico = resolveServicoTask(classeServico);

        ScheduledTaskImediato scheduledTaskImediato = criaScheduledTaskImediato(
                classeTask, tipoProcesso, scheduledTaskId, userId, descricao, timeZoneId, dtoParametros);

        Task<A,S> task;
        try {
            task = (Task<A,S>) instanciaTask(
                    classeTask,
                    classeServico,
                    scheduledTaskImediato,
                    servico);
            task.run();
        } catch (ReflectiveOperationException | RuntimeException e) {
            // A Task registra seu proprio ScheduledTaskExecution. Este bloco
            // cobre apenas falha reflexiva de construcao da instancia ou erro
            // runtime que escape do fluxo de execução antes de voltar ao controller.
            log.error("Erro ao executar task imediata sincronamente {}", scheduledTaskImediato.getId(), e);

            // Atualiza ScheduledTask como inativo
            scheduledTaskImediato.setAtivo(false);
            scheduledTaskImediato = scheduledTaskImediatoRepository.save(scheduledTaskImediato);
            validaScheduledTaskImediatoSalvoCommunity(scheduledTaskImediato);
            // cria novo ScheduledTaskExecution com mensagem de erro
            ScheduledTaskExecution scheduledTaskExecution = criaNovoTaskExecutionESalva(scheduledTaskImediato);
            scheduledTaskExecution.setHorarioFim(scheduledTaskImediato.getDataHorarioAtualNoTimeZone());
            scheduledTaskExecution.setMensagemErroResumida(e.getMessage());
            scheduledTaskExecution.setMensagemErroStackTrace(ExceptionUtils.getStackTrace(e));
            scheduledTaskPersistenceService.saveExecution(scheduledTaskExecution);

            throw e;
        }

        validaResultadoTaskSincrona(task, scheduledTaskImediato);

        return scheduledTaskImediato;

    }

    /**
     * Confirma o resultado funcional gravado pela task sincronizada.
     *
     * <p>{@link Task#run()} captura falhas da regra para sempre concluir e
     * persistir o historico usado por runners assíncronos. No Community, porem,
     * a task roda dentro da request: uma execucao registrada como falha precisa
     * voltar como erro HTTP, evitando que a interface mostre sucesso enquanto o
     * Process Status mostra {@code Failed}.</p>
     */
    private void validaResultadoTaskSincrona(
            Task<?, ?> task,
            ScheduledTaskImediato scheduledTaskImediato) {

        ScheduledTaskExecution scheduledTaskExecution =
                task.getLastScheduledTaskExecution();

        if (scheduledTaskExecution == null) {
            throw new TaskSchedulingException(
                    "Synchronous task "
                            + scheduledTaskImediato.getId()
                            + " did not produce an execution result.");
        }

        String mensagemErroResumida = scheduledTaskExecution.getMensagemErroResumida();
        if (mensagemErroResumida != null && !mensagemErroResumida.isBlank()) {
            throw new TaskSchedulingException(mensagemErroResumida);
        }

    }

    /**
     * Resolve o nome do bean funcional associado ao tipo generico da task.
     *
     * <p>O nome fica persistido em {@link ScheduledTaskAbstract#getBeanServico()}
     * para auditoria e para o overlay Enterprise conseguir recarregar a task
     * persistida em outro processo. Falhar explicitamente aqui evita o erro
     * opaco de acessar a primeira posicao de um array vazio quando a task aponta
     * para um service que nao foi registrado como bean Spring.</p>
     */
    private <S> String resolveNomeBeanServicoTask(Class<S> classeServico) {

        return scheduledTaskExecutionService.resolveTaskServiceBeanName(classeServico);

    }

    /**
     * Resolve o bean funcional que sera entregue ao construtor da task.
     */
    private <S> S resolveServicoTask(Class<S> classeServico) {

        return scheduledTaskExecutionService.resolveTaskService(classeServico);

    }

    /**
     * Resolve a classe concreta da task persistida ou recebida por API.
     *
     * <p>O scheduler aceita nomes de classe porque o overlay Enterprise pode
     * recarregar uma task em outro processo. Mesmo assim, a classe precisa
     * extender {@link Task}; validar isso cedo evita erro reflexivo tardio e
     * deixa claro quando um payload ou registro persistido aponta para uma
     * classe que nao pertence ao contrato do scheduler.</p>
     */
    protected Class<? extends Task> resolveClasseTask(String classeTaskComoString) throws ClassNotFoundException {

        return ScheduledTaskExecutionService.resolveTaskClass(classeTaskComoString);

    }

    /**
     * Resolve o service funcional declarado no segundo parametro generico da task.
     *
     * <p>O scheduler usa esse tipo para persistir o nome do bean e para entregar
     * ao construtor da task o service Spring correto. Tasks sem a declaracao
     * direta {@code Task<DTO, Service>} sao consideradas erro de contrato,
     * porque o runtime nao deve tentar adivinhar qual bean transacional executar.</p>
     */
    protected Class<?> resolveClasseServicoTask(Class<?> classeTask) {

        return ScheduledTaskExecutionService.resolveTaskServiceClass(classeTask);

    }

    /**
     * Instancia uma {@link Task} usando o contrato reflexivo padrao.
     *
     * <p>Toda task concreta deve expor um construtor publico com
     * {@link ScheduledTaskAbstract}, {@link ScheduledTaskPersistenceService} e
     * o service funcional declarado no segundo parametro generico da propria
     * task. A construção fica centralizada para que Community e Enterprise não
     * mantenham contratos reflexivos diferentes.</p>
     */
    protected Task<?, ?> instanciaTask(
            Class<?> classeTask,
            Class<?> classeServico,
            ScheduledTaskAbstract scheduledTaskAbstract,
            Object servico) throws ReflectiveOperationException {

        return scheduledTaskExecutionService.createTask(
                classeTask,
                classeServico,
                scheduledTaskAbstract,
                servico);

    }

    /**
     * Executa um Supplier imediatamente na thread da chamada e retorna a mensagem String gerada pelo proprio Supplier.
     * Este caminho cobre tarefas leves de controller que nao precisam virar uma classe Task dedicada.
     *
     * Sugestão : usar lambda para o tarefaAExecutar, como abaixo:
     * () -> { return "Executado com Sucesso"; }
     * @param tarefaAExecutar
     * @param tipoProcesso
     * @param userId
     * @param descricao
     * @param timeZoneId
     */
    public String criaSalvaEExecutaScheduledTaskImediatoSincronoComSupplier(
            Supplier<String> tarefaAExecutar, String tipoProcesso, String userId, String descricao, String timeZoneId) {

        validaSupplierExecucaoSincrona(tarefaAExecutar);

        String scheduledTaskId = getScheduledTaskIdDeTipoProcessoEUserIdEDataHora(tipoProcesso, userId, timeZoneId);

        validaScheduledTaskImediatoAindaNaoExiste(scheduledTaskId);

        ScheduledTaskImediato scheduledTaskImediato = new ScheduledTaskImediato(scheduledTaskId);

        scheduledTaskImediato.setAtivo(true);
        scheduledTaskImediato.setUserId(userId);
        scheduledTaskImediato.setTimeZone(timeZoneId);
        scheduledTaskImediato.setDescricao(descricao);
        scheduledTaskImediato.setTipoProcesso(tipoProcesso);
        scheduledTaskImediato.setHorarioCriacao(scheduledTaskImediato.getDataHorarioAtualNoTimeZone());

        scheduledTaskImediato = scheduledTaskImediatoRepository.save(scheduledTaskImediato);
        validaScheduledTaskImediatoSalvoCommunity(scheduledTaskImediato);

        String mensagemOutput = "";
        Throwable erroExecucao = null;

        // cria novo ScheduledTaskExecution
        ScheduledTaskExecution scheduledTaskExecution = criaNovoTaskExecutionESalva(scheduledTaskImediato);

        try {
            mensagemOutput = executeInstantTaskSameThread(scheduledTaskImediato, tarefaAExecutar);
        } catch (RuntimeException | Error e) {
            // Supplier nao declara checked exception; no caminho sincronizado,
            // runtime errors precisam ser convertidos em historico de task para
            // a tela de Process Status e em TaskSchedulingException para a API.
            log.error("Erro ao executar supplier sincronamente {}", scheduledTaskImediato.getId(), e);
            erroExecucao = e;
            mensagemOutput = e.getMessage();
            scheduledTaskExecution.setMensagemErroResumida(e.getMessage());
            scheduledTaskExecution.setMensagemErroStackTrace(ExceptionUtils.getStackTrace(e));
        }

        // Atualiza ScheduledTask como inativo
        scheduledTaskImediato.setAtivo(false);
        scheduledTaskImediato = scheduledTaskImediatoRepository.save(scheduledTaskImediato);
        validaScheduledTaskImediatoSalvoCommunity(scheduledTaskImediato);

        // preenche campos faltantes de ScheduledTaskExecution e salva
        scheduledTaskExecution.setHorarioFim(scheduledTaskImediato.getDataHorarioAtualNoTimeZone());
        scheduledTaskPersistenceService.saveExecution(scheduledTaskExecution);

        // lança exceção para retornar HttpStatus de erro no front-end
        if (scheduledTaskExecution.getMensagemErroResumida() != null) throw new TaskSchedulingException(
                scheduledTaskExecution.getMensagemErroResumida(),
                erroExecucao);

        return mensagemOutput;

    }

    public ScheduledTaskExecution criaNovoTaskExecutionESalva(ScheduledTaskAbstract scheduledTaskAbstract) {

        return scheduledTaskPersistenceService.createAndSaveExecution(scheduledTaskAbstract);

    }
    /**
     * Execução de ScheduledTaskImediato na mesma thread. Usa um Supplier<String> ao invés de um Runnable
     * pois tipicamente neste caso se espera uma resposta (String) a ser passada para o usuário (por exemplo
     * uma mensagem de sucesso/erro)
     */
    @Nullable
    private String executeInstantTaskSameThread(ScheduledTaskImediato scheduledTaskImediato, Supplier<String> tarefaAExecutar) {

        if (scheduledTaskImediato == null) {
            throw new IllegalArgumentException("Scheduled instant task is required for synchronous supplier execution.");
        }
        validaSupplierExecucaoSincrona(tarefaAExecutar);

        if (!scheduledTaskImediato.getAtivo()) {
            /*
             * Uma task imediata inativa ja foi encerrada/cancelada no controle
             * persistido. No Community sync-only nao existe worker posterior
             * para retomar essa execucao; portanto o supplier funcional nao
             * deve ser chamado novamente.
             */
            return null;
        }

        // roda na mesma thread
        return tarefaAExecutar.get();

    }

    /**
     * Valida a rotina funcional recebida para execucao sincronizada.
     *
     * <p>Supplier nulo e erro de contrato do caller. No Community essa falha
     * deve acontecer antes de criar `ScheduledTaskImediato`, porque nao existe
     * worker/queue posterior capaz de recuperar ou completar a execucao.</p>
     */
    private void validaSupplierExecucaoSincrona(Supplier<String> tarefaAExecutar) {

        if (tarefaAExecutar == null) {
            throw new IllegalArgumentException(
                    "Synchronous instant task supplier is required.");
        }

    }

    /**
     * Garante que o identificador tecnico gerado para a task ainda nao existe.
     *
     * <p>Duplicidade de id nao representa capability Enterprise ausente nem
     * modo de execucao bloqueado. Ela indica conflito de estado persistido ou
     * colisao operacional na geracao do identificador, por isso falha como
     * {@link IllegalStateException} antes de criar ou executar qualquer task.</p>
     */
    private void validaScheduledTaskImediatoAindaNaoExiste(String scheduledTaskId) {

        Optional<ScheduledTaskImediato> optionalScheduledTaskImediato =
                scheduledTaskImediatoRepository.customFindById(scheduledTaskId);

        if (optionalScheduledTaskImediato.isPresent()) {
            throw new IllegalStateException(
                    "Scheduled Instant Execution Task " + scheduledTaskId + " already exists");
        }

    }

    /**
     * Lista o histórico técnico de tasks imediatas e suas execuções.
     *
     * <p>A consulta traz uma linha projetada por execução em um único
     * round-trip, sem materializar o stack trace {@code @Lob}. A fronteira
     * transacional read-only mantém explícito que o Process Status apenas lê a
     * fotografia persistida.</p>
     */
    @Transactional(readOnly = true)
    public List<TaskSchedulingDTO> getTaskSchedulingDTOList() {

        List<TaskSchedulingDTO> listaTaskSchedulingDTO = new ArrayList<>();

        List<ScheduledTaskHistoryRowSnapshot> scheduledTaskHistoryRowSnapshotList =
                scheduledTaskAbstractRepository.findAllProcessStatusRows();
        validaScheduledTaskHistorySnapshotCommunity(scheduledTaskHistoryRowSnapshotList);

        for (ScheduledTaskHistoryRowSnapshot scheduledTaskHistoryRowSnapshot
                : scheduledTaskHistoryRowSnapshotList) {

            ScheduledTaskAbstract scheduledTask = scheduledTaskHistoryRowSnapshot.scheduledTask();
            TaskSchedulingDTO dto = new TaskSchedulingDTO();

            preencheDadosTaskSchedulingPorTipo(dto, scheduledTask);

            dto.setActive(scheduledTask.getAtivo());
            dto.setDescription(scheduledTask.getDescricao());
            dto.setProcessType(scheduledTask.getTipoProcesso());
            dto.setStartTime(scheduledTaskHistoryRowSnapshot.startTime());
            dto.setEndTime(scheduledTaskHistoryRowSnapshot.endTime());
            dto.setTaskCreationTime(scheduledTask.getHorarioCriacao());
            dto.setTimeZone(scheduledTask.getTimeZone());
            dto.setUserId(scheduledTask.getUserId());
            dto.setErrorMessage(scheduledTaskHistoryRowSnapshot.errorMessage());
            dto.setTaskId(scheduledTask.getId());
            dto.setTaskInstance(scheduledTaskHistoryRowSnapshot.taskInstance());

            listaTaskSchedulingDTO.add(dto);

        }

        // A tela de Process Status mistura linhas com execucao e linhas apenas agendadas.
        // Nessas linhas sem historico, taskInstance permanece nulo e precisa entrar na
        // ordenacao sem derrubar o endpoint quando houver empate no horario de criacao.
        listaTaskSchedulingDTO.sort(Comparator
                .comparing(
                        TaskSchedulingDTO::getTaskCreationTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                        TaskSchedulingDTO::getTaskInstance,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return listaTaskSchedulingDTO;

    }

    /**
     * Preenche os campos de apresentacao que dependem do subtipo persistido.
     *
     * <p>O Community so materializa tasks imediatas. O hook protegido evita que
     * o overlay Enterprise precise duplicar toda a montagem do Process Status
     * apenas para representar um discriminator privado, como uma task cron.</p>
     */
    protected void preencheDadosTaskSchedulingPorTipo(
            TaskSchedulingDTO taskSchedulingDTO,
            ScheduledTaskAbstract scheduledTaskAbstract) {

        taskSchedulingDTO.setTaskType("Instant");
        taskSchedulingDTO.setScheduledExecutionTime(scheduledTaskAbstract.getHorarioCriacao());

    }

    /**
     * Valida a fotografia lida para a tela de Process Status.
     *
     * <p>Lista vazia é ausência operacional válida. Retorno nulo, linha nula,
     * task sem identidade ou chave repetida indicam quebra da fotografia e
     * falham antes da montagem de DTOs parcialmente corretos.</p>
     */
    private void validaScheduledTaskHistorySnapshotCommunity(
            List<ScheduledTaskHistoryRowSnapshot> scheduledTaskHistoryRowSnapshotList) {

        if (scheduledTaskHistoryRowSnapshotList == null) {
            throw new IllegalStateException("Scheduled task history snapshot is required.");
        }

        Set<String> scheduledTaskHistoryKeys = new HashSet<>();
        int scheduledTaskHistoryRowIndex = 0;
        for (ScheduledTaskHistoryRowSnapshot scheduledTaskHistoryRowSnapshot
                : scheduledTaskHistoryRowSnapshotList) {
            if (scheduledTaskHistoryRowSnapshot == null) {
                throw new IllegalStateException(
                        "Scheduled task history item at index "
                                + scheduledTaskHistoryRowIndex
                                + " is required.");
            }

            ScheduledTaskAbstract scheduledTaskAbstract =
                    scheduledTaskHistoryRowSnapshot.scheduledTask();
            validaScheduledTaskAbstractSalvaCommunity(scheduledTaskAbstract);

            String scheduledTaskHistoryKey = scheduledTaskAbstract.getId()
                    + "#"
                    + Objects.toString(scheduledTaskHistoryRowSnapshot.taskInstance(), "scheduled");
            if (!scheduledTaskHistoryKeys.add(scheduledTaskHistoryKey)) {
                throw new IllegalStateException(
                        "Scheduled task history snapshot has duplicated task execution key "
                                + scheduledTaskHistoryKey
                                + ".");
            }

            scheduledTaskHistoryRowIndex++;
        }

    }

    /**
     * Persiste uma execucao ja criada pela {@link Task}. Mantido no service para
     * centralizar o ponto de escrita usado por tasks que nao sao beans Spring.
     */
    public void saveScheduledTaskExecution(ScheduledTaskExecution scheduledTaskExecution) {

        scheduledTaskPersistenceService.saveExecution(scheduledTaskExecution);

    }

    /**
     * Persiste o registro da task apos a execucao sincronizada atualizar o
     * status ativo/inativo.
     */
    public void saveScheduledTaskAbstract(ScheduledTaskAbstract scheduledTaskAbstract) {

        scheduledTaskPersistenceService.saveScheduledTask(scheduledTaskAbstract);

    }

    /**
     * Valida o snapshot salvo da task imediata Community.
     *
     * <p>O scheduler e uma borda de status operacional: controllers e tasks
     * assumem que, depois do save, existe uma linha identificavel no Process
     * Status. Retorno nulo ou sem id do repository deve falhar aqui, antes de
     * criar execucao filha ou devolver sucesso para a API.</p>
     */
    private void validaScheduledTaskImediatoSalvoCommunity(
            ScheduledTaskImediato scheduledTaskImediato) {

        validaScheduledTaskAbstractSalvaCommunity(scheduledTaskImediato);

    }

    private void validaScheduledTaskAbstractSalvaCommunity(
            ScheduledTaskAbstract scheduledTaskAbstract) {

        ScheduledTaskPersistenceService.validatePersistedTask(scheduledTaskAbstract);

    }

    /**
     * Serializa os parametros da task removendo campos nulos e preservando
     * tipos Java Time em formato legivel pelo JSON armazenado.
     */
    private <T> String getParametrosComoStringJson(T dto) throws JsonProcessingException {

        // ignora campos nulos
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return objectMapper.writeValueAsString(dto);

    }

    /**
     * Gera chave tecnica deterministica o suficiente para uso operacional,
     * combinando tipo de processo, usuario e timestamp no timezone configurado.
     */
    protected String getScheduledTaskIdDeTipoProcessoEUserIdEDataHora(
            String tipoProcesso,
            String userId,
            String timeZoneId) {

        LocalDateTime dataHorarioAtual = ScheduledTaskAbstract.getDataHorarioAtualNoTimeZone(timeZoneId);
        String scheduledTaskId = tipoProcesso + "-" + userId + "-" + dataHorarioAtual.toString();

        return scheduledTaskId;

    }

    /**
     * Remove registros historicos selecionados na tela de Process Status.
     *
     * <p>Como o Community nao possui fila ativa, esta operacao nao tenta cancelar
     * workers externos. Ela apenas apaga tasks encerradas e, por cascade, suas
     * execucoes associadas. O bloqueio explicito de tarefas ativas tambem protege
     * o runtime Enterprise que herda este service: a mensagem pendente na fila
     * precisa continuar encontrando seu registro persistido.</p>
     */
    public void deleteScheduledTasks(List<TaskSchedulingDTO> taskSchedulingDTOList) {

        validaTaskSchedulingDTOListParaDeleteCommunity(taskSchedulingDTOList);

        List<String> scheduledTaskIdList = taskSchedulingDTOList
                .stream()
                .map(TaskSchedulingDTO::getTaskId)
                .toList();

        if (scheduledTaskAbstractRepository.existsByIdInAndAtivoTrue(scheduledTaskIdList)) {
            throw new IllegalStateException(
                    "Active scheduled tasks cannot be deleted from Process Status. Wait for completion.");
        }

        /*
         * Community nao possui handles de agendamento em memoria. Excluir o registro
         * persistido e suficiente porque a consulta em lote acima confirmou que todas
         * as tarefas selecionadas ja sao somente historico de status.
         */
        scheduledTaskAbstractRepository.deleteAllById(scheduledTaskIdList);
    }

    /**
     * Valida a selecao enviada pela tela de Process Status antes da remocao.
     *
     * <p>No Community a delecao nao cancela fila nem worker externo; ela apenas
     * remove historico persistido. No Enterprise, a verificacao em lote de task
     * ativa impede que esta mesma operacao apague o estado que uma mensagem ainda
     * precisa consumir. Ainda assim, a lista precisa apontar para tasks concretas
     * por id para evitar chamadas de repository com chave nula ou vazia.</p>
     */
    private void validaTaskSchedulingDTOListParaDeleteCommunity(
            List<TaskSchedulingDTO> taskSchedulingDTOList) {

        if (taskSchedulingDTOList == null || taskSchedulingDTOList.isEmpty()) {
            throw new IllegalArgumentException("Scheduled task history selection is required for delete.");
        }

        int indiceTaskSchedulingDTO = 0;
        for (TaskSchedulingDTO taskSchedulingDTO : taskSchedulingDTOList) {
            if (taskSchedulingDTO == null) {
                throw new IllegalArgumentException(
                        "Scheduled task history selection item at index "
                                + indiceTaskSchedulingDTO
                                + " is required for delete.");
            }
            if (taskSchedulingDTO.getTaskId() == null || taskSchedulingDTO.getTaskId().isBlank()) {
                throw new IllegalArgumentException(
                        "Scheduled task history selection item at index "
                                + indiceTaskSchedulingDTO
                                + " must have a task id for delete.");
            }
            indiceTaskSchedulingDTO++;
        }

    }

}
