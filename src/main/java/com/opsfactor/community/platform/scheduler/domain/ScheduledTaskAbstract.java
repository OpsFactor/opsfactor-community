package com.opsfactor.community.platform.scheduler.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidade base do historico de tasks exibido no Process Status Community.
 *
 * <p>O Community materializa apenas execucoes imediatas, mas preserva a tabela
 * unica para permitir que o overlay Enterprise adicione outros discriminators
 * sem mudar o historico ja gravado.</p>
 */
@Getter
@Setter
@Table(name = "scheduled_task")
@EqualsAndHashCode(of = "id")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_trigger")
@NoArgsConstructor
@Entity
public abstract class ScheduledTaskAbstract implements Serializable {

    @Id
    private String id;
    
    private String tipoProcesso;
    
    private String classeTask;
    private String beanServico;
    
    private String userId;
    
    private LocalDateTime horarioCriacao;
    String timeZone;
    
    private String descricao;
    
    private Boolean ativo;

    /**
     * Parametros da execucao imediata serializados em JSON.
     *
     * <p>O Community nao persiste mapa chave/valor auxiliar nem copia textual de
     * dados de input. O Enterprise pode reintroduzir persistencia adicional de
     * auditoria quando trouxer filas e jobs assíncronos.</p>
     */
    /*
     * Delega o tipo JSON ao dialeto ativo. Em PostgreSQL isto gera jsonb,
     * que possui operador de igualdade e pode participar de SELECT DISTINCT.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private String configuracoesExecucaoJson;
    
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy="scheduledTaskExecutionCompositeKey.scheduledTask", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ScheduledTaskExecution> scheduledTaskExecutionSet = new HashSet<>();
        
    public ScheduledTaskAbstract(String id) {
        this.id = id;
    }
    
    public boolean getAtivo() {
        return (ativo == null) ? false : ativo;
    }
    
    public LocalDateTime getDataHorarioAtualNoTimeZone() {
        ZoneId zoneId = ZoneId.of(getTimeZone());
        return LocalDateTime.now(zoneId);        
    }
    
    public static LocalDateTime getDataHorarioAtualNoTimeZone(String timeZone) {
        ZoneId zoneId = ZoneId.of(timeZone);
        return LocalDateTime.now(zoneId);        
    }
    
    public Long getUltimoIdExecucao() {
        return scheduledTaskExecutionSet.stream()
                .mapToLong(x -> x.getIdExecucao())
                .max().orElse(0);
    }
    
    public void addScheduledTaskExecution(ScheduledTaskExecution scheduledTaskExecution) {
        
        if (scheduledTaskExecutionSet.contains(scheduledTaskExecution)) {
            throw new IllegalStateException(
                    "Scheduled Task Execution with id "
                            + scheduledTaskExecution.getIdExecucao()
                            + " already present at Scheduled Task "
                            + getId());
        }
        
        scheduledTaskExecutionSet.add(scheduledTaskExecution);
        
    }
        
}
