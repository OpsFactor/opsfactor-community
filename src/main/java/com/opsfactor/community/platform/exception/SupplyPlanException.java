package com.opsfactor.community.platform.exception;

/**
 * Erro funcional de orquestracao ou persistencia de Supply Planning.
 */
public class SupplyPlanException extends RuntimeException {
    public SupplyPlanException(String errorMessage) {
        super(errorMessage);
    }
}
