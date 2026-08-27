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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Versao de producao simples do Supply Planning Community.
 *
 * <p>Uma versao simples materializa exatamente uma combinacao location,
 * roteiro e lista tecnica. O material output e derivado desses mestres e nao
 * e persistido novamente na versao. Parallel routing/output e
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

    public VersaoProducaoSimples(
            String id,
            Location location,
            Integer prioridade,
            Roteiro roteiro,
            ListaTecnica listaTecnica) {
        
        if (roteiro == null) {
            throw new IllegalArgumentException("Simple production version routing is required");
        }
        if (listaTecnica == null) {
            throw new IllegalArgumentException("Simple production version bill of materials is required");
        }
        
        setId(id);
        setLocation(location);
        setPrioridade(prioridade);
        setRoteiro(roteiro);
        setListaTecnica(listaTecnica);

        geraErroSeDadosInconsistentes();

    }
    
    @Override
    public void geraErroSeDadosInconsistentes() {

        if (getLocation() == null) {
            throw new IllegalStateException("Simple production version location is required");
        }
        if (getRoteiro() == null) {
            throw new IllegalStateException("Simple production version routing is required");
        }
        if (getListaTecnica() == null) {
            throw new IllegalStateException("Simple production version Bill of Materials is required");
        }
        if (!getRoteiro().getLocation().equals(getLocation())) {
            throw new IllegalStateException("Routing location " + getRoteiro().getLocation().getId() + " different than version location " + getLocation().getId());
        } else if (!getListaTecnica().getLocation().equals(getLocation())) {
            throw new IllegalStateException("Bill of Materials location " + getListaTecnica().getLocation().getId() + " different than version location " + getLocation().getId());
        } else if (!getListaTecnica().getMaterialOutput().equals(getRoteiro().getMaterialOutput())) {
            throw new IllegalStateException("Bill of Materials output material " + getListaTecnica().getMaterialOutput().getId() + " different than version material " + getRoteiro().getMaterialOutput().getId());
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
        return Sets.newHashSet(getRoteiro().getMaterialOutput());
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
