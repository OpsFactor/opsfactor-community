package com.opsfactor.community.platform.exception;

/**
 * Erro funcional da estrutura de BI/projection em memoria.
 */
public class InMemoryBIException extends RuntimeException {
    public InMemoryBIException(String errorMessage) {
        super(errorMessage);
    }
}
