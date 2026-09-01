package com.opsfactor.community.capability.masterdata.production.billofmaterials.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/** Quantidade-base e unidade de um output individual de lista técnica múltipla. */
@Entity
@Table(name = "lista_tecnica_multiplo_output")
@Data
@ToString(of = "listaTecnicaMultiploOutputCompositeKey")
@EqualsAndHashCode(of = "listaTecnicaMultiploOutputCompositeKey")
@NoArgsConstructor
public class ListaTecnicaMultiploOutput {

    @EmbeddedId
    @NonNull
    private ListaTecnicaMultiploOutputCompositeKey listaTecnicaMultiploOutputCompositeKey;

    private Double quantidadeBase;

    @lombok.Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    private UnidadeMedida unidadeMedida;

    public ListaTecnicaMultiploOutput(ListaTecnicaMultiplo listaTecnicaMultiplo, Produto materialOutput) {

        this.listaTecnicaMultiploOutputCompositeKey = new ListaTecnicaMultiploOutputCompositeKey(
                listaTecnicaMultiplo,
                materialOutput);

    }

    public ListaTecnicaMultiplo getListaTecnicaMultiplo() {

        return listaTecnicaMultiploOutputCompositeKey.getListaTecnicaMultiplo();

    }

    public Produto getMaterialOutput() {

        return listaTecnicaMultiploOutputCompositeKey.getMaterialOutput();

    }

    public double getQuantidadeBase() {

        if (quantidadeBase == null || !Double.isFinite(quantidadeBase) || quantidadeBase <= 0d) {
            throw new IllegalStateException("Multiple BOM output base quantity must be finite and positive");
        }
        return quantidadeBase;

    }

    public UnidadeMedida getUnidadeMedida(ParametrosGlobais parametrosGlobais) {

        return unidadeMedida == null ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedida;

    }

    public void valida() {

        getQuantidadeBase();

    }

    @Data
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ListaTecnicaMultiploOutputCompositeKey implements Serializable {

        @NonNull
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private ListaTecnicaMultiplo listaTecnicaMultiplo;

        @NonNull
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private Produto materialOutput;
    }
}
