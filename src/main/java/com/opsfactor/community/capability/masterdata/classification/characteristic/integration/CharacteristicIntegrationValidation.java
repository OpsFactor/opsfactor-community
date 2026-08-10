package com.opsfactor.community.capability.masterdata.classification.characteristic.integration;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Validacoes compartilhadas pelas cargas Enterprise de caracteristicas.
 *
 * <p>Material e location possuem entidades e repositories separados, mas o
 * contrato operacional e o mesmo: id obrigatorio, descricao obrigatoria, tipo
 * obrigatorio e proibicao de colisao de ids entre os dois catalogos dinamicos.
 * Manter esta regra em uma classe pequena evita que os dois mappers/services
 * passem a divergir silenciosamente.</p>
 */
public final class CharacteristicIntegrationValidation {

    private CharacteristicIntegrationValidation() {

    }

    /**
     * Valida colecao recebida do fluxo generico antes de chamar o repository.
     *
     * @param characteristicCollection colecao a salvar/remover.
     * @param collectionName nome funcional usado na mensagem de erro.
     */
    public static <T extends Caracteristica> void validaCharacteristicCollection(
            Collection<T> characteristicCollection,
            String collectionName) {

        if (characteristicCollection == null) {
            throw new IllegalStateException(collectionName + " is required.");
        }

        int index = 0;
        for (T characteristic : characteristicCollection) {
            validaCharacteristicItem(
                    characteristic,
                    collectionName + " returned invalid item at index " + index);
            index++;
        }

    }

    /**
     * Valida primary keys recebidas para lookup/remocao antes que o service
     * reduza a colecao por {@link java.util.HashSet}.
     *
     * <p>O data upload do catalogo usa chaves simples, mas a deduplicacao
     * silenciosa ainda esconderia payload duplicado quando a chamada entra
     * diretamente por remocao ou por lookup de entidades persistidas.</p>
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

        Set<String> primaryKeyDescriptions = new LinkedHashSet<>();
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
     * Valida um campo textual obrigatorio da chave do catalogo.
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
     * Valida snapshot retornado por repository antes de exportar ou indexar.
     *
     * @param characteristicCollection snapshot do repository.
     * @param collectionName nome funcional usado na mensagem de erro.
     * @return a mesma colecao validada, preservando o tipo do caller.
     */
    public static <T extends Caracteristica> Collection<T> validaPersistedCharacteristicCollection(
            Collection<T> characteristicCollection,
            String collectionName) {

        if (characteristicCollection == null) {
            throw new IllegalStateException(collectionName + " returned null.");
        }

        int index = 0;
        for (T characteristic : characteristicCollection) {
            validaCharacteristicItem(
                    characteristic,
                    collectionName + " returned invalid item at index " + index);
            index++;
        }

        return characteristicCollection;

    }

    /**
     * Extrai ids de um snapshot ja carregado para validar colisao entre
     * catalogos sem consultar o banco linha a linha.
     *
     * @param characteristicCollection caracteristicas persistidas.
     * @param collectionName nome funcional usado na mensagem de erro.
     * @return ids das caracteristicas validadas.
     */
    public static <T extends Caracteristica> Set<String> getCharacteristicIdSet(
            Collection<T> characteristicCollection,
            String collectionName) {

        Collection<T> validatedCharacteristicCollection =
                validaPersistedCharacteristicCollection(
                        characteristicCollection,
                        collectionName);

        Set<String> characteristicIdSet = new LinkedHashSet<>();
        for (T characteristic : validatedCharacteristicCollection) {
            if (!characteristicIdSet.add(characteristic.getId())) {
                throw new IllegalStateException(
                        collectionName
                                + " returned duplicated characteristic id "
                                + characteristic.getId()
                                + ".");
            }
        }
        return characteristicIdSet;

    }

    /**
     * Valida campos obrigatorios de uma caracteristica dinamica.
     */
    private static void validaCharacteristicItem(
            Caracteristica characteristic,
            String itemDescription) {

        if (characteristic == null) {
            throw new IllegalStateException(itemDescription + " because it is null.");
        }
        if (characteristic.getId() == null || characteristic.getId().isBlank()) {
            throw new IllegalStateException(itemDescription + " because id is required.");
        }
        if (characteristic.getDescricao() == null || characteristic.getDescricao().isBlank()) {
            throw new IllegalStateException(itemDescription + " because description is required.");
        }
        if (characteristic.getTipoCaracteristica() == null) {
            throw new IllegalStateException(itemDescription + " because characteristic type is required.");
        }

    }

}
