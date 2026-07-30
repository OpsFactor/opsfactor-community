package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pseudo-caracteristica de material aceita no Community.
 *
 * <p>Implementacoes reais baseadas em cadastro dinamico pertencem ao
 * Enterprise. A implementacao Community viva e {@link CaracteristicaProdutoId},
 * usada apenas quando o fluxo precisa acessar o id do material pelo contrato
 * historico de caracteristicas.</p>
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
