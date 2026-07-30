package com.opsfactor.community.platform.scheduler.facade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO de leitura do historico de processos executados pelo scheduler Community.
 *
 * <p>No Community todos os registros representam tarefas imediatas executadas de forma
 * sincronizada. O contrato conserva campos opcionais de subtipo para que o
 * overlay Enterprise possa representar uma agenda cron sem duplicar a leitura
 * de historico; a Community sempre os publica como {@code null}.</p>
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskSchedulingDTO {
    
    private String taskType;
    private String taskId;
    private String processType;
    private Boolean active;
    private String userId;
    private String description;
    String timeZone;
    private LocalDateTime taskCreationTime;

    // Para tarefas imediatas, equivale ao horario de criacao/solicitacao da tarefa.
    private LocalDateTime scheduledExecutionTime;

    /** Expressao cron opcional, preenchida somente pelo subtipo Enterprise recorrente. */
    private String cronExpression;
    
    // ScheduledTaskExecution
    private Long taskInstance;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
    
}
