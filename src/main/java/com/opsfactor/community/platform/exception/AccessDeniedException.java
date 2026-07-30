package com.opsfactor.community.platform.exception;

/**
 * Erro funcional para acesso negado em regras de dominio Community.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String errorMessage) {
        super(errorMessage);
    }
}
