package com.opsfactor.community.platform.scheduler.domain;

import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * Registro de uma execucao realizada por uma ScheduledTask.
 *
 * <p>No Community este historico nasce apenas de {@link ScheduledTaskImediato}, pois
 * tarefas recorrentes e programadas pertencem ao scheduler Enterprise.</p>
 */
@Getter
@Setter
@EqualsAndHashCode(of="scheduledTaskExecutionCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class ScheduledTaskExecution implements Serializable {
    
    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private ScheduledTaskExecutionCompositeKey scheduledTaskExecutionCompositeKey;

    /**
     * Chave composta por tarefa agendada e identificador sequencial da execucao.
     */
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ScheduledTaskExecutionCompositeKey implements Serializable {

        @ManyToOne(cascade = {}, optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private ScheduledTaskAbstract scheduledTask;
        
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Long idExecucao;

    }
    
    // Ambos os horarios seguem o timezone do ScheduledTaskAbstract.
    LocalDateTime horarioInicio;
    LocalDateTime horarioFim;
    
    @Column(columnDefinition = "TEXT")
    String mensagemErroResumida;
    @Lob
    String mensagemErroStackTrace;
    
    public ScheduledTaskAbstract getScheduledTask() {
        return getScheduledTaskExecutionCompositeKey().getScheduledTask();
    }
    
    public Long getIdExecucao() {
        return getScheduledTaskExecutionCompositeKey().getIdExecucao();
    }

}
