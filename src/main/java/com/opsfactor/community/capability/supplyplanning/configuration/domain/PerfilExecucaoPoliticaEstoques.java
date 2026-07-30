package com.opsfactor.community.capability.supplyplanning.configuration.domain;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import java.io.Serializable;
import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Vinculo entre um perfil de execucao de Supply Planning e uma politica
 * operacional de safety stock.
 *
 * <p>O Community usa esse relacionamento para disponibilizar politicas
 * material/location no plano heuristico. Ele nao representa execucao nem
 * resultado de Inventory Policy Optimization, que permanecem capacidades
 * Enterprise.</p>
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(of="perfilExecucaoPoliticaEstoquesCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class PerfilExecucaoPoliticaEstoques implements Serializable {
    
    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private PerfilExecucaoPoliticaEstoquesCompositeKey perfilExecucaoPoliticaEstoquesCompositeKey;

    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @Getter 
    @Setter
    @EqualsAndHashCode
    public static class PerfilExecucaoPoliticaEstoquesCompositeKey implements Serializable {
        
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan;
        
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private PoliticaEstoques politicaEstoques;
        
    }
        
   
    public PerfilExecucaoSupplyPlan getPerfilExecucaoSupplyPlan() {
        return getPerfilExecucaoPoliticaEstoquesCompositeKey().getPerfilExecucaoSupplyPlan();
    }
    
    public PoliticaEstoques getPoliticaEstoques() {
        return getPerfilExecucaoPoliticaEstoquesCompositeKey().getPoliticaEstoques();
    }
    
}
