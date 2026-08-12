package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CaracteristicaProduto extends com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica implements CaracteristicaProdutoInterface {

    /**
     * Clusters de material que selecionam esta característica para modelos
     * analíticos e regras de agrupamento.
     *
     * <p>A relação pública permanece unidirecional: `ClusterProdutos` não
     * recebe coleção inversa. A tabela de associação usa o mapeamento padrão
     * do provider, sem nomes físicos legados presos no código.</p>
     */
    @ManyToMany(fetch = FetchType.LAZY)
    private Set<ClusterProdutos> materialClusters = new HashSet<>();
    
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "valorCaracteristicaProdutoCompositeKey.caracteristicaProduto", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ValorCaracteristicaProduto> listaValorCaracteristicaProduto = new ArrayList<>();

    /** Inclui este atributo no conjunto selecionado para um cluster de material. */
    public void addMaterialCluster(ClusterProdutos materialCluster) {

        materialClusters.add(materialCluster);

    }
        
    public List<String> getValoresCaracteristicaDeListaProdutos(List<Produto> produtos) {
        return produtos.stream()
                .map(p -> p.getValorCaracteristica(this))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public String getValorCaracteristicaDeProduto(Produto produto) {

        /*
         * Produto conhece apenas o contrato comum de característica. A
         * resolução concreta permanece na entidade dona da tabela pública de
         * valores, sem acoplar o material à persistência da classificação.
         */
        return listaValorCaracteristicaProduto.stream()
                .filter(valorCaracteristicaProduto ->
                        produto.equals(valorCaracteristicaProduto.getProduto()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No value configured for Material "
                                + produto.getId()
                                + " and Material Characteristic "
                                + getId()
                                + ". Characteristic filters require a value for every referenced Material."))
                .getAtributo();

    }

    /**
     * Finds a value without treating absence as a malformed master-data record.
     *
     * <p>Characteristics can legitimately apply to a subset of materials. A
     * cluster rule therefore needs a non-throwing lookup so that a material
     * without a value simply does not match that rule and can continue to the
     * next cluster or the default cluster.</p>
     */
    public Optional<String> findValorCaracteristicaDeProduto(Produto produto) {

        return listaValorCaracteristicaProduto.stream()
                .filter(valorCaracteristicaProduto -> produto.equals(valorCaracteristicaProduto.getProduto()))
                .map(ValorCaracteristicaProduto::getAtributo)
                .findFirst();

    }

    @Override
    public List<String> getValoresCaracteristica() {
        if (valoresCaracteristica == null) {
            valoresCaracteristica = listaValorCaracteristicaProduto.stream()
                    .map(ValorCaracteristicaProduto::getAtributo)
                    .distinct()
                    .collect(Collectors.toList());
        }

        return valoresCaracteristica;
    }
    
    /**
     * Necessário, pois @EqualsAndHashCode(super=true) não diferencia CaracteristicaProduto de CaracteristicaLocation
     *
     * @param o objeto comparado com esta característica de material.
     * @return `true` quando os ids representam a mesma característica de material.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CaracteristicaProduto that)) return false;
        return getId().equals(that.getId());
    }

    /**
     * Necessário, pois @EqualsAndHashCode(super=true) não diferencia CaracteristicaProduto de CaracteristicaLocation
     *
     * @return hash calculado pelo id, preservando fallback legado quando a entidade ainda não foi persistida.
     */
    @Override 
    public int hashCode() {
        if (getId() == null) return 43;
        return getId().hashCode();
    }
}
