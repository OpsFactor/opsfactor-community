package com.opsfactor.community.platform.exception;

/**
 * Indica que a chamada chegou a um ponto de extensao conhecido pelo Community,
 * mas cuja implementacao funcional pertence exclusivamente ao OpsFactor Enterprise.
 */
public class RequiresEnterpriseVersionException extends RuntimeException {

    public static final String ERROR_CODE = "REQUIRES_ENTERPRISE_VERSION";

    /**
     * Cria a excecao de fronteira Enterprise com mensagem publica estavel.
     *
     * @param featureName capacidade Enterprise acessada no runtime Community
     */
    public RequiresEnterpriseVersionException(String featureName) {

        super(ERROR_CODE + ": " + featureName + " requires OpsFactor Enterprise.");

    }

    /**
     * Cria a excecao de fronteira Enterprise preservando a causa tecnica.
     *
     * @param featureName capacidade Enterprise acessada no runtime Community
     * @param cause excecao que levou a borda Community a classificar o payload
     * como capacidade Enterprise
     */
    public RequiresEnterpriseVersionException(String featureName, Throwable cause) {

        super(ERROR_CODE + ": " + featureName + " requires OpsFactor Enterprise.", cause);

    }

}
