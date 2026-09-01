package com.opsfactor.community.capability.masterdata.production.routing.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/** Material pertencente ao conjunto de outputs de um roteiro múltiplo. */
@Entity
@Table(name = "roteiro_multiplo_material")
@Data
@ToString(of = "roteiroMultiploMaterialCompositeKey")
@EqualsAndHashCode(of = "roteiroMultiploMaterialCompositeKey")
@NoArgsConstructor
public class RoteiroMultiploMaterial {

    @EmbeddedId
    @NonNull
    private RoteiroMultiploMaterialCompositeKey roteiroMultiploMaterialCompositeKey;

    public RoteiroMultiploMaterial(RoteiroMultiplo roteiroMultiplo, Produto material) {

        this.roteiroMultiploMaterialCompositeKey = new RoteiroMultiploMaterialCompositeKey(roteiroMultiplo, material);

    }

    public RoteiroMultiplo getRoteiroMultiplo() {

        return roteiroMultiploMaterialCompositeKey.getRoteiroMultiplo();

    }

    public Produto getMaterial() {

        return roteiroMultiploMaterialCompositeKey.getMaterial();

    }

    @Data
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class RoteiroMultiploMaterialCompositeKey implements Serializable {

        @NonNull
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private RoteiroMultiplo roteiroMultiplo;

        @NonNull
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private Produto material;
    }
}
