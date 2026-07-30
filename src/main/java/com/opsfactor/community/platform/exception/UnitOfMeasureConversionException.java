package com.opsfactor.community.platform.exception;

/**
 * Erro funcional de conversao entre unidades de medida.
 */
public class UnitOfMeasureConversionException extends RuntimeException {

    /**
     * Cria a excecao funcional com mensagem publica de conversao.
     *
     * @param errorMessage mensagem contextual exibida ao chamador
     */
    public UnitOfMeasureConversionException(String errorMessage) {

        super(errorMessage);

    }

    /**
     * Cria a excecao funcional preservando a causa tecnica original.
     *
     * @param errorMessage mensagem contextual exibida ao chamador
     * @param cause excecao que originou a falha de conversao
     */
    public UnitOfMeasureConversionException(String errorMessage, Throwable cause) {

        super(errorMessage, cause);

    }

}
