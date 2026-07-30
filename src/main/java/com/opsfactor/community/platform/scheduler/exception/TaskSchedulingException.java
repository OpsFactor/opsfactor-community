package com.opsfactor.community.platform.scheduler.exception;

/**
 * Excecao funcional da infraestrutura Community de scheduler.
 */
public class TaskSchedulingException extends RuntimeException {

    /**
     * Cria a excecao funcional de scheduler com a mensagem publica.
     *
     * @param errorMessage mensagem resumida da falha exibida ao caller
     */
    public TaskSchedulingException(String errorMessage) {

        super(errorMessage);

    }

    /**
     * Cria a excecao funcional de scheduler preservando a causa tecnica.
     *
     * @param errorMessage mensagem resumida da falha exibida ao caller
     * @param cause excecao original capturada durante a execucao da task
     */
    public TaskSchedulingException(String errorMessage, Throwable cause) {

        super(errorMessage, cause);

    }

}
