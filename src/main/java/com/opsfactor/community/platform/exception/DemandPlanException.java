package com.opsfactor.community.platform.exception;

/**
 * Erro funcional de orquestracao ou persistencia de Demand Planning.
 */
public class DemandPlanException extends RuntimeException {
    public DemandPlanException(String errorMessage) {
        super(errorMessage);
    }
}
