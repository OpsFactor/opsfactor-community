package com.opsfactor.community.platform.scheduler.repository.dto;

/**
 * Fotografia escalar de uma task ativa para diagnósticos de cutover.
 *
 * <p>Não contém entidade JPA, histórico de execução ou associações. O JSON
 * permanece opaco para a camada Community: cada capability Enterprise decide
 * se pode validá-lo somente como árvore, sem reconstruir a task.</p>
 */
public record ScheduledTaskPayloadPreflightSnapshot(
        String taskId,
        String taskClassName,
        String executionConfigurationJson) {

}
