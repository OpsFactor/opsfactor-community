package com.opsfactor.community.capability.masterdata.production.operation.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

/**
 * Unidade temporal persistida para a duração de uma operação de roteiro.
 * Todos os cálculos de capacidade continuam normalizados em horas.
 */
public enum UnidadeTempoOperacao {

    SEGUNDOS("S", 1d / 3_600d),
    MINUTOS("M", 1d / 60d),
    HORAS("H", 1d),
    DIAS("D", 24d);

    public static final UnidadeTempoOperacao PADRAO = HORAS;

    private final String codigo;
    private final double fatorParaHoras;

    UnidadeTempoOperacao(String codigo, double fatorParaHoras) {

        this.codigo = codigo;
        this.fatorParaHoras = fatorParaHoras;

    }

    public String getCodigo() {

        return codigo;

    }

    public double converteParaHoras(double valor) {

        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException("Operation duration must be finite");
        }
        return valor * fatorParaHoras;

    }

    public static UnidadeTempoOperacao deCodigoOuPadrao(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            return PADRAO;
        }
        return Arrays.stream(values())
                .filter(unidadeTempoOperacao -> unidadeTempoOperacao.codigo.equalsIgnoreCase(codigo.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid operation time unit '" + codigo + "'. Valid options: S, M, H, D"));

    }

    /** Persiste somente o código estável publicado nas interfaces de dados. */
    @Converter
    public static class JpaConverter implements AttributeConverter<UnidadeTempoOperacao, String> {

        @Override
        public String convertToDatabaseColumn(UnidadeTempoOperacao unidadeTempoOperacao) {

            return (unidadeTempoOperacao == null ? PADRAO : unidadeTempoOperacao).codigo;

        }

        @Override
        public UnidadeTempoOperacao convertToEntityAttribute(String codigo) {

            return deCodigoOuPadrao(codigo);

        }

    }

}
