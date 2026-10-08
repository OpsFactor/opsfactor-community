package com.opsfactor.community.platform.exception;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import lombok.Getter;

/**
 * Erro funcional de conversao entre unidades de medida.
 */
public class UnitOfMeasureConversionException extends RuntimeException {

    @Getter
    private final Produto material;
    @Getter
    private final UnidadeMedida unidadeMedidaOrigem;
    @Getter
    private final UnidadeMedida unidadeMedidaDestino;

    /**
     * Cria a excecao funcional com mensagem publica de conversao.
     *
     * @param errorMessage mensagem contextual exibida ao chamador
     */
    public UnitOfMeasureConversionException(String errorMessage) {

        this(errorMessage, null, null, null);

    }

    /**
     * Cria a excecao funcional preservando a causa tecnica original.
     *
     * @param errorMessage mensagem contextual exibida ao chamador
     * @param cause excecao que originou a falha de conversao
     */
    public UnitOfMeasureConversionException(String errorMessage, Throwable cause) {

        super(errorMessage, cause);
        material = null;
        unidadeMedidaOrigem = null;
        unidadeMedidaDestino = null;

    }


    /** Carries structured diagnostic context while preserving the historical public message. */
    public UnitOfMeasureConversionException(String errorMessage, Produto material,
            UnidadeMedida unidadeMedidaOrigem, UnidadeMedida unidadeMedidaDestino) {

        super(errorMessage);
        this.material = material;
        this.unidadeMedidaOrigem = unidadeMedidaOrigem;
        this.unidadeMedidaDestino = unidadeMedidaDestino;

    }

}
