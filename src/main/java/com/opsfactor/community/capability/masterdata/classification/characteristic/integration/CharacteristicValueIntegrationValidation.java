package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.platform.utility.MetodosUtilidade;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Validacoes compartilhadas pelas cargas Enterprise de valores de caracteristica.
 */
public final class CharacteristicValueIntegrationValidation {

    private CharacteristicValueIntegrationValidation() {

    }

    /**
     * Normaliza e valida o atributo de acordo com o tipo da caracteristica.
     *
     * @param rawAttributeValue valor recebido no arquivo/API.
     * @param caracteristica caracteristica que define o tipo esperado.
     * @return valor normalizado para persistencia.
     */
    public static String getNormalizedAttributeValue(
            String rawAttributeValue,
            Caracteristica caracteristica) {

        if (caracteristica == null) {
            throw new IllegalStateException("Characteristic is required to validate characteristic value.");
        }
        if (caracteristica.getTipoCaracteristica() == null) {
            throw new IllegalStateException(
                    "Characteristic "
                            + caracteristica.getId()
                            + " requires a type before values can be loaded.");
        }
        if (rawAttributeValue == null || rawAttributeValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Characteristic value is required for characteristic "
                            + caracteristica.getId()
                            + ".");
        }

        String trimmedAttributeValue = rawAttributeValue.trim();

        return switch (caracteristica.getTipoCaracteristica()) {
            case BINARIO -> Boolean.toString(
                    MetodosUtilidade.converteStringParaBoolean(trimmedAttributeValue));
            case NUMERICO -> getNormalizedNumericAttributeValue(
                    trimmedAttributeValue,
                    caracteristica);
            case CATEGORICO -> trimmedAttributeValue;
        };

    }

    /**
     * Valida uma colecao de entidades de valor antes de persistir/remover.
     *
     * @param entityCollection colecao recebida do fluxo generico.
     * @param collectionName nome funcional usado na mensagem de erro.
     */
    public static <T> void validaEntityCollection(
            Collection<T> entityCollection,
            String collectionName) {

        if (entityCollection == null) {
            throw new IllegalStateException(collectionName + " is required.");
        }

        int index = 0;
        for (T entity : entityCollection) {
            if (entity == null) {
                throw new IllegalStateException(collectionName + " returned null item at index " + index + ".");
            }
            index++;
        }

    }

    /**
     * Valida primary keys recebidas para lookup ou remocao antes que o service
     * reduza a colecao para um {@link HashSet}.
     *
     * <p>A infraestrutura generica ja valida o payload completo durante saves,
     * mas chamadas diretas de remocao entram por esta colecao de primary keys.
     * Por isso a duplicidade precisa falhar aqui com indice e chave funcional,
     * em vez de ser escondida pela deduplicacao em memoria.</p>
     *
     * @param primaryKeyDtoCollection colecao de primary keys recebida.
     * @param collectionName nome funcional usado em mensagens.
     * @param keyDescriptionFactory monta a descricao funcional da chave e pode
     * validar campos obrigatorios usando o indice recebido.
     * @return a mesma colecao validada, preservando o tipo do caller.
     */
    public static <T> Collection<T> validaPrimaryKeyDtoCollection(
            Collection<T> primaryKeyDtoCollection,
            String collectionName,
            BiFunction<T, Integer, String> keyDescriptionFactory) {

        if (primaryKeyDtoCollection == null) {
            throw new IllegalStateException(collectionName + " is required.");
        }
        if (keyDescriptionFactory == null) {
            throw new IllegalStateException(collectionName + " key description factory is required.");
        }

        Set<String> primaryKeyDescriptions = new HashSet<>();
        int index = 0;
        for (T primaryKeyDto : primaryKeyDtoCollection) {
            if (primaryKeyDto == null) {
                throw new IllegalArgumentException(collectionName + " item at index " + index + " is required.");
            }

            String primaryKeyDescription = keyDescriptionFactory.apply(primaryKeyDto, index);
            if (primaryKeyDescription == null || primaryKeyDescription.isBlank()) {
                throw new IllegalArgumentException(
                        collectionName
                                + " item at index "
                                + index
                                + " returned empty primary key.");
            }
            if (!primaryKeyDescriptions.add(primaryKeyDescription)) {
                throw new IllegalArgumentException(
                        collectionName
                                + " item at index "
                                + index
                                + " has duplicated key "
                                + primaryKeyDescription
                                + ".");
            }
            index++;
        }

        return primaryKeyDtoCollection;

    }

    /**
     * Valida um campo textual obrigatório da chave composta de valor.
     */
    public static String getRequiredPrimaryKeyField(
            String fieldValue,
            String fieldName,
            String collectionName,
            int index) {

        if (fieldValue == null || fieldValue.isBlank()) {
            throw new IllegalArgumentException(
                    collectionName
                            + " item at index "
                            + index
                            + " requires "
                            + fieldName
                            + ".");
        }
        return fieldValue;

    }

    /**
     * Valida support data e materializa mapa por id antes do mapper consumir o
     * snapshot.
     *
     * <p>Valores de caracteristica dependem de materials/locations e do
     * catalogo de caracteristicas. Id ausente, item nulo ou duplicidade nesses
     * snapshots indica quebra estrutural e precisa falhar com mensagem
     * funcional antes de um `Collectors.toMap(...)` generico.</p>
     *
     * @param entityCollection snapshot usado como support data.
     * @param collectionName nome funcional usado em mensagens.
     * @param idExtractor extrator do id funcional.
     * @param additionalValidator validacao complementar do item, opcional.
     * @return mapa por id validado.
     */
    public static <T> Map<String, T> getSupportEntityByIdMap(
            Collection<T> entityCollection,
            String collectionName,
            Function<T, String> idExtractor,
            Consumer<T> additionalValidator) {

        if (entityCollection == null) {
            throw new IllegalStateException(collectionName + " returned null.");
        }
        if (idExtractor == null) {
            throw new IllegalStateException(collectionName + " id extractor is required.");
        }

        Map<String, T> entityById = new LinkedHashMap<>();
        int index = 0;
        for (T entity : entityCollection) {
            if (entity == null) {
                throw new IllegalStateException(collectionName + " returned null item at index " + index + ".");
            }

            String id = idExtractor.apply(entity);
            if (id == null || id.isBlank()) {
                throw new IllegalStateException(collectionName + " returned item without id at index " + index + ".");
            }
            if (additionalValidator != null) {
                additionalValidator.accept(entity);
            }
            if (entityById.putIfAbsent(id, entity) != null) {
                throw new IllegalStateException(collectionName + " returned duplicated id " + id + ".");
            }
            index++;
        }

        return entityById;

    }

    /**
     * Valida snapshot de repository antes de download ou filtragem por primary key.
     */
    public static <T> Collection<T> validaPersistedEntityCollection(
            Collection<T> entityCollection,
            String collectionName) {

        if (entityCollection == null) {
            throw new IllegalStateException(collectionName + " returned null.");
        }

        validaEntityCollection(entityCollection, collectionName);
        return entityCollection;

    }

    private static String getNormalizedNumericAttributeValue(
            String trimmedAttributeValue,
            Caracteristica caracteristica) {

        try {
            return Double.toString(Double.parseDouble(trimmedAttributeValue));
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(
                    "Characteristic value for characteristic "
                            + caracteristica.getId()
                            + " must be numerical.",
                    numberFormatException);
        }

    }

}
