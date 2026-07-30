package com.opsfactor.community.platform.integration.service;

import com.opsfactor.community.platform.exception.DataUploadException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Validacoes comuns de persistencia em lote da infraestrutura de data upload.
 *
 * <p>Alguns uploads Community usam a interface generica de integracao, enquanto
 * outros ainda possuem services manuais por compatibilidade historica do
 * arquivo. Esta classe concentra o contrato que ambos precisam seguir quando
 * chamam `saveAll`: retorno nulo ou item nulo representa repository/snapshot
 * quebrado e deve falhar no ponto da persistencia, com mensagem funcional.
 * Quando o tamanho esperado do batch e conhecido, o overload com
 * {@code expectedSize} tambem protege contra snapshot salvo parcial.</p>
 */
public final class IntegrationPersistenceValidation {

    private IntegrationPersistenceValidation() {

    }

    /**
     * Valida e copia a colecao retornada pelo repository apos `saveAll`.
     *
     * <p>A copia para `List` estabiliza o retorno para a infraestrutura de
     * batch e evita que uma colecao lazy/mutavel do provider seja consumida mais
     * de uma vez com comportamento diferente. Colecao vazia segue valida para
     * batches sem efeito; colecao nula ou item nulo indicam quebra estrutural.</p>
     */
    public static <ENTITY> List<ENTITY> validaSavedEntityCollection(
            Collection<ENTITY> savedEntityCollection,
            String savedEntityCollectionDescription) {

        if (savedEntityCollection == null) {
            throw new DataUploadException(savedEntityCollectionDescription + " returned null.");
        }

        List<ENTITY> savedEntityList = new ArrayList<>();
        int indice = 0;
        for (ENTITY savedEntity : savedEntityCollection) {
            if (savedEntity == null) {
                throw new DataUploadException(
                        savedEntityCollectionDescription
                                + " returned null item at index "
                                + indice
                                + ".");
            }

            savedEntityList.add(savedEntity);
            indice++;
        }

        return savedEntityList;

    }

    /**
     * Valida e copia a colecao retornada pelo repository apos `saveAll`,
     * exigindo que a quantidade devolvida bata com a quantidade enviada.
     *
     * <p>Use este overload em fluxos de persistencia de batch nos quais o caller
     * acabou de montar a colecao enviada ao repository. Nao use em consultas ou
     * leituras por filtro, pois nesses casos subconjuntos sao resposta
     * funcionalmente valida.</p>
     */
    public static <ENTITY> List<ENTITY> validaSavedEntityCollection(
            Collection<ENTITY> savedEntityCollection,
            String savedEntityCollectionDescription,
            int expectedSize) {

        List<ENTITY> savedEntityList = validaSavedEntityCollection(
                savedEntityCollection,
                savedEntityCollectionDescription);
        if (savedEntityList.size() != expectedSize) {
            throw new DataUploadException(
                    savedEntityCollectionDescription
                            + " size "
                            + savedEntityList.size()
                            + " differs from expected saved batch size "
                            + expectedSize
                            + ".");
        }

        return savedEntityList;

    }

}
