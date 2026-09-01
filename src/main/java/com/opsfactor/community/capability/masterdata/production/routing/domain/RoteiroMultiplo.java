package com.opsfactor.community.capability.masterdata.production.routing.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Roteiro de uma execução física indivisível que gera vários outputs.
 *
 * <p>As operações e a quantidade-base pertencem ao pacote, enquanto os
 * materiais filhos são registrados individualmente nesta associação. A versão
 * de produção continua apontando para o tipo comum {@link Roteiro}.</p>
 */
@Entity
@DiscriminatorValue("multiplo")
@Getter
@Setter
@NoArgsConstructor
public class RoteiroMultiplo extends Roteiro {

    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "roteiroMultiploMaterialCompositeKey.roteiroMultiplo",
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<RoteiroMultiploMaterial> roteiroMultiploMaterialSet = new HashSet<>();

    @Override
    public Set<Produto> getMateriaisOutput() {

        return roteiroMultiploMaterialSet.stream()
                .map(RoteiroMultiploMaterial::getMaterial)
                .collect(Collectors.toSet());

    }

    /** Validação local do agregado antes de ele compor uma versão produtiva. */
    public void geraErroSeDadosInconsistentes() {

        if (getOperacaoRoteiroSet().isEmpty()) {
            throw new IllegalStateException("Multiple routing " + getId() + " must have at least one operation");
        }
        if (getMateriaisOutput().size() < 2) {
            throw new IllegalStateException("Multiple routing " + getId() + " must have at least two output materials");
        }
        getQuantidadeBase();

    }

    /** Pacotes múltiplos exigem uma versão persistida para fixar proporções. */
    @Override
    public boolean getHabilitadoParaUsoSemVersaoProducao() {

        return false;

    }
}
