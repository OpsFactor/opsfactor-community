package com.opsfactor.community.capability.masterdata.production.billofmaterials.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
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
 * Lista técnica de um pacote produtivo com quantidades-base individuais por
 * output. Componentes pertencem ao pacote, não a uma linha filha específica.
 */
@Entity
@DiscriminatorValue("multiplo")
@Getter
@Setter
@NoArgsConstructor
public class ListaTecnicaMultiplo extends ListaTecnica {

    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "listaTecnicaMultiploOutputCompositeKey.listaTecnicaMultiplo",
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<ListaTecnicaMultiploOutput> listaTecnicaMultiploOutputSet = new HashSet<>();

    @Override
    public Set<Produto> getMateriaisOutput() {

        return listaTecnicaMultiploOutputSet.stream()
                .map(ListaTecnicaMultiploOutput::getMaterialOutput)
                .collect(Collectors.toSet());

    }

    @Override
    public double getQuantidadeBaseOutput(Produto produtoOutput) {

        return getOutput(produtoOutput).getQuantidadeBase();

    }

    @Override
    public UnidadeMedida getUnidadeMedidaMaterialOutput(
            Produto produtoOutput,
            ParametrosGlobais parametrosGlobais) {

        return getOutput(produtoOutput).getUnidadeMedida(parametrosGlobais);

    }

    public ListaTecnicaMultiploOutput getOutput(Produto produtoOutput) {

        return listaTecnicaMultiploOutputSet.stream()
                .filter(output -> output.getMaterialOutput().equals(produtoOutput))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material " + produtoOutput.getId() + " is not an output of multiple BOM " + getId()));

    }

    /** Validação local do agregado antes de ele compor uma versão produtiva. */
    public void geraErroSeDadosInconsistentes() {

        if (getMateriaisOutput().size() < 2) {
            throw new IllegalStateException("Multiple BOM " + getId() + " must have at least two outputs");
        }
        getListaTecnicaMultiploOutputSet().forEach(ListaTecnicaMultiploOutput::valida);

    }

    /** Pacotes múltiplos exigem uma versão persistida para fixar proporções. */
    @Override
    public boolean getHabilitadoParaUsoSemVersaoProducao() {

        return false;

    }
}
