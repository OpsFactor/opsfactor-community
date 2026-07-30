package com.opsfactor.community.platform.integration.dto;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Centraliza a normalizacao textual aplicada aos DTOs do modelo generico de
 * integracao, antes que chaves ou entidades sejam calculadas.
 */
final class IntegrationTextNormalization {

    private IntegrationTextNormalization() {

    }

    static void normalizaCamposPublicos(Object objeto) {

        if (objeto == null) return;

        try {
            for (Field field : objeto.getClass().getFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                if (String.class.equals(field.getType())) {
                    field.set(objeto, normalizaValorTexto((String) field.get(objeto)));
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    normalizaValoresTextoDoMapa(field.get(objeto));
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Erro ao normalizar campos texto do DTO de integracao", e);
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void normalizaValoresTextoDoMapa(Object valorCampo) {

        if (!(valorCampo instanceof Map mapa)) return;

        for (Object chave : mapa.keySet()) {
            Object valor = mapa.get(chave);
            if (valor instanceof String) {
                mapa.put(chave, normalizaValorTexto((String) valor));
            }
        }

    }

    private static String normalizaValorTexto(String valorOriginal) {

        if (valorOriginal == null) return null;

        String valorNormalizado = valorOriginal.stripTrailing();
        if (valorNormalizado.isEmpty()) return null;

        return valorNormalizado;

    }

}
