package com.opsfactor.community.platform.scheduler.repository;

import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.repository.dto.ScheduledTaskHistoryRowSnapshot;
import com.opsfactor.community.platform.scheduler.repository.dto.ScheduledTaskPayloadPreflightSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository de leitura do historico de tasks Community.
 */
@Repository
public interface ScheduledTaskAbstractRepository extends JpaRepository<ScheduledTaskAbstract,String> {

    /**
     * Busca a fotografia escalar do histórico técnico para Process Status.
     *
     * <p>O {@code LEFT JOIN} mantém tasks sem execução e retorna uma linha por
     * execução em um único round-trip. A constructor projection seleciona
     * somente a mensagem resumida e exclui fisicamente o stack trace
     * {@code @Lob}, que não pertence ao contrato da tela.</p>
     */
    @Query("SELECT new com.opsfactor.community.platform.scheduler.repository.dto.ScheduledTaskHistoryRowSnapshot("
            + "scheduledTask, "
            + "execution.scheduledTaskExecutionCompositeKey.idExecucao, "
            + "execution.horarioInicio, "
            + "execution.horarioFim, "
            + "execution.mensagemErroResumida) "
            + "FROM ScheduledTaskAbstract scheduledTask "
            + "LEFT JOIN scheduledTask.scheduledTaskExecutionSet execution")
    public List<ScheduledTaskHistoryRowSnapshot> findAllProcessStatusRows();

    /**
     * Recupera somente o conjunto ainda operacional para gates de cutover.
     *
     * <p>O preflight Enterprise nao exibe Process Status nem acessa o historico
     * de execucoes. Por isso, esta consulta intencionalmente nao usa
     * {@code JOIN FETCH}: ela le uma unica tabela, evita carregar uma colecao
     * potencialmente grande sem necessidade e preserva o diagnostico como uma
     * operacao estritamente somente-leitura.</p>
     */
    @Query("SELECT scheduledTask FROM ScheduledTaskAbstract scheduledTask "
            + "WHERE scheduledTask.ativo = true")
    public List<ScheduledTaskAbstract> customFindAllActiveForCutoverPreflight();

    /**
     * Carrega somente a task ativa explicitamente anotada em um request de
     * preview, sem transformar um preview unitário em inventário completo.
     */
    @Query("SELECT scheduledTask FROM ScheduledTaskAbstract scheduledTask "
            + "WHERE scheduledTask.ativo = true "
            + "AND scheduledTask.id = :scheduledTaskId")
    public Optional<ScheduledTaskAbstract> findActiveByIdForCutoverPreflight(
            @Param("scheduledTaskId") String scheduledTaskId);

    /**
     * Lê em lote somente os escalares necessários para um preflight de payload
     * de uma classe canônica de task.
     *
     * <p>O contrato deliberadamente não materializa a entidade, histórico de
     * execução ou associações. Consumers de diagnóstico devem analisar o JSON
     * como árvore e jamais reconstruir a task durante esta consulta.</p>
     */
    @Query("SELECT new com.opsfactor.community.platform.scheduler.repository.dto.ScheduledTaskPayloadPreflightSnapshot("
            + "scheduledTask.id, scheduledTask.classeTask, scheduledTask.configuracoesExecucaoJson) "
            + "FROM ScheduledTaskAbstract scheduledTask "
            + "WHERE scheduledTask.ativo = true "
            + "AND scheduledTask.classeTask = :taskClassName")
    public List<ScheduledTaskPayloadPreflightSnapshot> findActiveTaskPayloadsByTaskClassName(
            @Param("taskClassName") String taskClassName);

    /**
     * Lê a fotografia escalar de uma única task Orders selecionada no preview,
     * mantendo o JSON fora da reconstrução funcional do scheduler.
     */
    @Query("SELECT new com.opsfactor.community.platform.scheduler.repository.dto.ScheduledTaskPayloadPreflightSnapshot("
            + "scheduledTask.id, scheduledTask.classeTask, scheduledTask.configuracoesExecucaoJson) "
            + "FROM ScheduledTaskAbstract scheduledTask "
            + "WHERE scheduledTask.ativo = true "
            + "AND scheduledTask.classeTask = :taskClassName "
            + "AND scheduledTask.id = :scheduledTaskId")
    public Optional<ScheduledTaskPayloadPreflightSnapshot> findActiveTaskPayloadByIdAndTaskClassName(
            @Param("scheduledTaskId") String scheduledTaskId,
            @Param("taskClassName") String taskClassName);

    /**
     * Carrega uma única task ativa de uma classe canônica para um cutover
     * offline e toma lock pessimista antes que seu JSON seja atualizado.
     *
     * <p>A seleção permanece explícita por ID e classe. Não há fetch de
     * histórico porque a conversão não executa nem reconstrói a task; ela apenas
     * revalida e normaliza o payload gerenciado dentro da transação do
     * executor.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT scheduledTask FROM ScheduledTaskAbstract scheduledTask "
            + "WHERE scheduledTask.id = :scheduledTaskId "
            + "AND scheduledTask.ativo = true "
            + "AND scheduledTask.classeTask = :taskClassName")
    public Optional<ScheduledTaskAbstract> findActiveByIdAndTaskClassNameForCutover(
            @Param("scheduledTaskId") String scheduledTaskId,
            @Param("taskClassName") String taskClassName);

    /**
     * Informa se uma selecao de historico ainda possui trabalho ativo.
     *
     * <p>A consulta e feita em lote antes da limpeza da tela de Process Status.
     * No Community normalmente todos os itens ja terminaram, mas o mesmo
     * contrato e herdado pelo Enterprise, onde uma task ativa pode ainda estar
     * aguardando ou sendo executada por um job da fila.</p>
     */
    boolean existsByIdInAndAtivoTrue(Collection<String> scheduledTaskIdCollection);

}
