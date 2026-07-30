package com.opsfactor.community.platform.exception;

/**
 * Erro funcional para operacoes entre calendarios incompatíveis.
 */
public class IncompatibleCalendarException extends RuntimeException {
    public IncompatibleCalendarException(String errorMessage) {
        super(errorMessage);
    }
}
