package com.opsfactor.community.capability.masterdata.production.productionversion.domain;

import com.google.common.collect.Sets;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Versao de producao simples do Supply Planning Community.
 *
 * <p>Uma versao simples materializa exatamente uma combinacao location,
 * material output, roteiro e lista tecnica. Parallel routing/output e
 * encadeamentos produtivos privados devem ser modelados no Enterprise por
 * subclasses/overlays proprios, sem alterar o contrato operacional desta
 * entidade.</p>
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@DiscriminatorValue("simples")
@NoArgsConstructor
public class VersaoProducaoSimples extends VersaoProducao {

    public VersaoProducaoSimples(String id, Location location, Integer prioridade, Produto materialOutput, Roteiro roteiro, ListaTecnica listaTecnica) {
        
        if (materialOutput == null) {
            throw new IllegalArgumentException("Simple production version output material is required");
        }
        if (roteiro == null) {
            throw new IllegalArgumentException("Simple production version routing is required");
        }
        if (listaTecnica == null) {
            throw new IllegalArgumentException("Simple production version bill of materials is required");
        }
        
        this.id = id;
        setLocation(location);
        setPrioridade(prioridade);
        this.materialOutput = materialOutput;
        this.roteiro = roteiro;
        this.listaTecnica = listaTecnica;
    }

    /**
     * Roteiro produtivo unico da versao simples.
     *
     * <p>O campo fica sem `@NonNull` porque o mapeamento JPA e compartilhado
     * pela hierarquia de versoes de producao. A obrigatoriedade funcional da
     * versao simples e verificada no construtor e em
     * `geraErroSeDadosInconsistentes()`.</p>
     */
    @ManyToOne
    private Roteiro roteiro;

    /**
     * Lista tecnica unica da versao simples.
     */
    @ManyToOne
    private ListaTecnica listaTecnica;

    /**
     * Material output produzido pela combinacao roteiro/lista tecnica.
     */
    @ManyToOne
    private Produto materialOutput;
    
    @Override
    public void geraErroSeDadosInconsistentes() {
        if (!getRoteiro().getLocation().equals(getLocation())) {
            throw new IllegalStateException("Routing location " + getRoteiro().getLocation().getId() + " different than version location " + getLocation().getId());
        } else if (!getRoteiro().getMaterialOutput().equals(getMaterialOutput())) {
            throw new IllegalStateException("Routing material " + getRoteiro().getMaterialOutput().getId() + " different than version material " + getMaterialOutput().getId());
        } else if (!getListaTecnica().getLocation().equals(getLocation())) {
            throw new IllegalStateException("Bill of Materials location " + getListaTecnica().getLocation().getId() + " different than version location " + getLocation().getId());
        } else if (!getListaTecnica().getMaterialOutput().equals(getMaterialOutput())) {
            throw new IllegalStateException("Bill of Materials output material " + getListaTecnica().getMaterialOutput().getId() + " different than version material " + getMaterialOutput().getId());
        }
    }
    
    @Override
    public boolean contemRoteiro(Roteiro roteiro) {
        return (getRoteiro().equals(roteiro));
    }
    
    @Override
    public boolean contemListaTecnica(ListaTecnica listaTecnica) {
        return (getListaTecnica().equals(listaTecnica));
    }
    
    @Override
    public Set<Produto> getMateriaisOutput() {
        return Sets.newHashSet(getMaterialOutput());
    }
    
    @Override
    public Set<Produto> getMateriaisInput() {
        return getListaTecnica().getMateriaisInput();
    }
    
    @Override
    public Set<Roteiro> getRoteiros() {
        return Sets.newHashSet(getRoteiro());
    }
    
    @Override
    public Set<ListaTecnica> getListasTecnicas() {
        return Sets.newHashSet(getListaTecnica());
    }
    
    @Override
    public List<Triplet<Roteiro,ListaTecnica,Double>> getDetalhePorVersaoProducao(
            UnidadeMedidaProjection unidadeMedidaProjection,
            Produto materialReferencia,
            UnidadeMedida unidadeMedidaMaterialReferencia,
            double quantidadeMaterialReferencia) {
        
        return Arrays.asList(
                Triplet.with(getRoteiro(), getListaTecnica(), 1.0));
        
    }
    
    public List<Pair<Roteiro,ListaTecnica>> getCombinacoesRoteiroListaTecnica() {
        return Collections.singletonList(
                Pair.with(getRoteiro(), getListaTecnica()));
    }
    
    @Override
    public String toString() {
        return (id == null) ?
                "No Production Version : " + getRoteiro().getId()
                : id;
    }
            
}
