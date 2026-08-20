package com.opsfactor.community.platform.scheduler.repository.dto;

import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import java.time.LocalDateTime;

/**
 * Linha escalar do histórico exibido no Process Status.
 *
 * <p>A task raiz permanece como entidade polimórfica para que o Enterprise
 * consiga identificar seus subtipos. A execução, porém, é projetada somente
 * nos campos usados pela tela. Assim, o histórico não materializa o stack
 * trace persistido como {@code @Lob} nem cria uma consulta por execução.</p>
 */
public record ScheduledTaskHistoryRowSnapshot(
        ScheduledTaskAbstract scheduledTask,
        Long taskInstance,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String errorMessage) {
}
