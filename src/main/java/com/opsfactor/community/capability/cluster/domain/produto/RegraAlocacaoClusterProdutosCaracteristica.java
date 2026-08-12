package com.opsfactor.community.capability.cluster.domain.produto;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;

/**
 * One selected value of a material characteristic in a cluster allocation rule.
 *
 * <p>A rule may contain several values of the same characteristic. They form an
 * OR set for that characteristic, while separate rules remain AND conditions of
 * the cluster. This mirrors the established legacy allocation contract and
 * keeps the decision data explicit instead of inferring membership from names.</p>
 */
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "regraAlocacaoClusterProdutosCaracteristicaCompositeKey")
public class RegraAlocacaoClusterProdutosCaracteristica implements Serializable {

    @EmbeddedId
    private RegraAlocacaoClusterProdutosCaracteristicaCompositeKey regraAlocacaoClusterProdutosCaracteristicaCompositeKey;

    /** Composite identity prevents duplicate selected values for one rule. */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class RegraAlocacaoClusterProdutosCaracteristicaCompositeKey implements Serializable {

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull
        private RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull
        private CaracteristicaProduto caracteristica;

        @NonNull
        private String atributo;
    }

    /** Returns the persisted characteristic value selected by the rule. */
    public String getAtributo() {

        return getCompositeKey().getAtributo();
    }

    /** Changes the selected characteristic value while preserving the composite key owner. */
    public void setAtributo(String atributo) {

        getCompositeKey().setAtributo(atributo);
    }

    /** Assigns the owning material-cluster allocation rule. */
    public void setRegraAlocacaoClusterProdutos(RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos) {

        getCompositeKey().setRegraAlocacaoClusterProdutos(regraAlocacaoClusterProdutos);
    }

    /** Assigns the public material characteristic selected by the rule. */
    public void setCaracteristica(CaracteristicaProduto caracteristica) {

        getCompositeKey().setCaracteristica(caracteristica);
    }

    /** Lazily materializes the historical composite key for compatibility with JPA setters. */
    public RegraAlocacaoClusterProdutosCaracteristicaCompositeKey getCompositeKey() {

        if (regraAlocacaoClusterProdutosCaracteristicaCompositeKey == null) {
            regraAlocacaoClusterProdutosCaracteristicaCompositeKey =
                    new RegraAlocacaoClusterProdutosCaracteristicaCompositeKey();
        }
        return regraAlocacaoClusterProdutosCaracteristicaCompositeKey;
    }

    /** Returns the material characteristic selected for this value. */
    public CaracteristicaProduto getCaracteristica() {

        return getCompositeKey().getCaracteristica();
    }

    /** Returns the allocation rule that owns this selected value. */
    public RegraAlocacaoClusterProdutos getRegraAlocacaoClusterProdutos() {

        return getCompositeKey().getRegraAlocacaoClusterProdutos();
    }
}
