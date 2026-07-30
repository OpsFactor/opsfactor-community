package com.opsfactor.community.platform.exception;

/**
 * Erro funcional de agendamento/processamento de tasks.
 */
public class SchedulingException extends RuntimeException {
    public SchedulingException(String errorMessage) {
        super(errorMessage);
    }
}
