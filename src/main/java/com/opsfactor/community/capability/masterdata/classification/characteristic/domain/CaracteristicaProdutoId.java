package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;

/**
 * Pseudo-caracteristica Community que expõe somente o id tecnico do material.
 *
 * <p>Ela coexiste com o catálogo dinâmico público para manter fluxos capazes de
 * tratar `materialId` como atributo técnico.</p>
 */
public class CaracteristicaProdutoId implements CaracteristicaProdutoInterface {

    @Override
    public String getValorCaracteristicaDeProduto(Produto produto) {

        return produto.getId();

    }

    @Override
    public String getId() {

        return "materialId";

    }

    @Override
    public String toString() {

        return "materialId";

    }

}
