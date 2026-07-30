package com.opsfactor.community.capability.masterdata.production.billofmaterials.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Componente de uma lista tecnica Community.
 *
 * <p>A chave composta fixa a combinacao lista tecnica/material componente. A
 * linha informa quantidade e unidade de medida fisica consumida pelo Supply
 * Planning heuristico, sem carregar custos ou restricoes Enterprise.</p>
 */
@Entity
@Data
@ToString(of="listaTecnicaComponenteCompositeKey")
@EqualsAndHashCode(of = "listaTecnicaComponenteCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
public class ListaTecnicaComponente {

    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private ListaTecnicaComponenteCompositeKey listaTecnicaComponenteCompositeKey;

    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ListaTecnicaComponenteCompositeKey implements Serializable {

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private ListaTecnica listaTecnica;
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Produto materialComponente;
                
    }
        
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaMaterialComponente;
    
    private Float quantidade;
    
    public ListaTecnica getListaTecnica() {
        return listaTecnicaComponenteCompositeKey.getListaTecnica();
    }
    
    public Produto getMaterialComponente() {
        return listaTecnicaComponenteCompositeKey.getMaterialComponente();
    }
    
    public UnidadeMedida getUnidadeMedidaMaterialComponenteCadastrada() {
        return unidadeMedidaMaterialComponente;
    }

    public UnidadeMedida getUnidadeMedidaMaterialComponente(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaMaterialComponente == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedidaMaterialComponente;
    }
    
    public Float getQuantidade() {
        return (quantidade == null) ? 1 : quantidade;
    }
    
    public Produto getMaterial() {
        return getListaTecnicaComponenteCompositeKey().getMaterialComponente();
    }
        
}
