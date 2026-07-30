package com.opsfactor.community.platform.exception;

/**
 * Erro funcional de conversao, validacao ou carga de dados de integracao.
 */
public class DataUploadException extends RuntimeException {

    /**
     * Cria erro de data upload com a mensagem exibida ao usuario/API.
     *
     * @param errorMessage mensagem funcional do erro de carga.
     */
    public DataUploadException(String errorMessage) {
        super(errorMessage);
    }

    /**
     * Cria erro de data upload preservando a causa tecnica original.
     *
     * <p>Usado nas bordas que enriquecem erros de mapper com linha de arquivo
     * ou batch. A mensagem publica continua simples, mas logs e testes podem
     * rastrear a excecao original que causou a falha.</p>
     *
     * @param errorMessage mensagem funcional do erro de carga.
     * @param cause causa original capturada na borda de integracao.
     */
    public DataUploadException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

}
