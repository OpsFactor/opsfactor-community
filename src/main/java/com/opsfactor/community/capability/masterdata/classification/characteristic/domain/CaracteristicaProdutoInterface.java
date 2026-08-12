package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrato Community para características públicas de material.
 *
 * <p>{@link CaracteristicaProduto} representa o catálogo dinâmico e
 * {@link CaracteristicaProdutoId} preserva a dimensão técnica do ID.</p>
 */
public interface CaracteristicaProdutoInterface extends CaracteristicaInterface {

    public String getValorCaracteristicaDeProduto(Produto material);

    public default List<String> getValoresCaracteristicaDeListaProdutos(List<Produto> produtos) {

        return produtos.stream()
                .map(p -> getValorCaracteristicaDeProduto(p))
                .distinct()
                .collect(Collectors.toList());

    }

}
