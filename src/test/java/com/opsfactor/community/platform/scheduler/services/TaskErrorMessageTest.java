package com.opsfactor.community.platform.scheduler.services;

import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskAbstract;
import com.opsfactor.community.platform.scheduler.domain.ScheduledTaskExecution;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies the persisted Process Status message at the task execution boundary.
 * The complete stack remains available without making the UI depend on its LOB.
 */
class TaskErrorMessageTest {

    @Test
    void persistsExecutionContextAndDeepestCauseWithCompleteStack() {

        IllegalArgumentException conversionFailure = new IllegalArgumentException(
                "No conversion available from FDO to TON for material MATERIAL_A");
        IllegalStateException modelFailure = new IllegalStateException(
                "Failed to build model", new RuntimeException("Variable indexing failed", conversionFailure));

        ScheduledTaskExecution execution = runFailingTask(modelFailure);

        assertEquals("IllegalStateException: Failed to build model Root cause: IllegalArgumentException: "
                + "No conversion available from FDO to TON for material MATERIAL_A",
                execution.getMensagemErroResumida());
        assertTrue(execution.getMensagemErroStackTrace().contains("Variable indexing failed"));
        assertTrue(execution.getMensagemErroStackTrace().contains(conversionFailure.getMessage()));
        assertNotNull(execution.getHorarioFim());

    }

    @Test
    void preservesUnwrappedFailureWithoutAddingRootLabel() {

        ScheduledTaskExecution execution = runFailingTask(new IllegalStateException("Invalid configuration"));

        assertEquals("IllegalStateException: Invalid configuration", execution.getMensagemErroResumida());
        assertFalse(execution.getMensagemErroResumida().contains("Root cause:"));

    }

    @Test
    void avoidsRepeatingRootAlreadyIncludedByExceptionConstructor() {

        IllegalArgumentException root = new IllegalArgumentException("Missing conversion");
        IllegalStateException wrapper = new IllegalStateException(root);

        ScheduledTaskExecution execution = runFailingTask(wrapper);

        assertEquals("IllegalStateException: java.lang.IllegalArgumentException: Missing conversion",
                execution.getMensagemErroResumida());
        assertFalse(execution.getMensagemErroResumida().contains("Root cause:"));

    }

    @Test
    void includesRootForErrorsWithoutChangingFailureCapture() {

        ScheduledTaskExecution execution = runFailingTask(
                new AssertionError("Model assembly failed", new IllegalArgumentException("Invalid quantity")));

        assertEquals("AssertionError: Model assembly failed Root cause: IllegalArgumentException: Invalid quantity",
                execution.getMensagemErroResumida());

    }

    @Test
    void handlesCircularCauseChainWithoutHangingOrDroppingFailure() {

        IllegalStateException wrapper = new IllegalStateException("Model assembly failed");
        IllegalArgumentException root = new IllegalArgumentException("Missing conversion");
        wrapper.initCause(root);
        root.initCause(wrapper);

        ScheduledTaskExecution execution = runFailingTask(wrapper);

        assertTrue(execution.getMensagemErroResumida().contains("Missing conversion"));
        assertTrue(execution.getMensagemErroStackTrace().contains("CIRCULAR REFERENCE"));

    }

    /** Executes the real task boundary while isolating persistence and time. */
    private ScheduledTaskExecution runFailingTask(Throwable failure) {

        ScheduledTaskAbstract scheduledTask = mock(ScheduledTaskAbstract.class);
        ScheduledTaskPersistenceService persistence = mock(ScheduledTaskPersistenceService.class);
        ScheduledTaskExecution execution = new ScheduledTaskExecution();
        when(scheduledTask.getAtivo()).thenReturn(true);
        when(scheduledTask.getId()).thenReturn("TASK_A");
        when(scheduledTask.getDataHorarioAtualNoTimeZone()).thenReturn(LocalDateTime.of(2026, 10, 14, 12, 0));
        when(persistence.createAndSaveExecution(scheduledTask)).thenReturn(execution);

        new FailingTask(scheduledTask, persistence, failure).run();

        verify(persistence).saveExecution(execution);
        return execution;

    }

    /** Concrete generic declaration matches the runtime construction contract. */
    private static class FailingTask extends Task<Void, Throwable> {

        FailingTask(ScheduledTaskAbstract scheduledTask, ScheduledTaskPersistenceService persistence, Throwable failure) {

            super(scheduledTask, persistence, failure);

        }

        @Override
        public void executaTask(Void parameters, Throwable failure) {

            if (failure instanceof Error error) {
                throw error;
            }
            throw (RuntimeException) failure;

        }

    }

}
