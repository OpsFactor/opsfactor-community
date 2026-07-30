package com.opsfactor.community.capability.masterdata.production.operation.domain;

import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Operacao posicionada dentro de um roteiro produtivo.
 *
 * <p>O Community usa a operacao para associar sequencia, recurso produtivo e
 * consumo de capacidade a um roteiro simples. Scheduling detalhado, line
 * scheduling e otimizacao de sequenciamento ficam no Enterprise.</p>
 */
@Entity
@Data
@ToString(of="operacaoRoteiroCompositeKey")
@EqualsAndHashCode(of = "operacaoRoteiroCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
public class OperacaoRoteiro extends OperacaoAbstract {

    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private OperacaoRoteiroCompositeKey operacaoRoteiroCompositeKey;

    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class OperacaoRoteiroCompositeKey implements Serializable {

        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        @Column(length = 10)
        private Integer posicao;
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Roteiro roteiro;

    }
    
    @Override
    public Integer getPosicao() {
        return getOperacaoRoteiroCompositeKey().getPosicao();
    }
    
    public Roteiro getRoteiro() {
        return getOperacaoRoteiroCompositeKey().getRoteiro();
    }
    
    @Override
    public Produto getMaterialOutput() {
        return getRoteiro().getMaterialOutput();
    }
    
    public void valida() {
        if (!getRoteiro().getLocation().equals(getRecursoProdutivo().getLocation())) {
            throw new IllegalStateException("Routing " + getRoteiro().getId() + " location " + 
                    getRoteiro().getLocation().getId() + " does not match production resource location " +
                    getRecursoProdutivo().getLocation().getId() + " at operation " + getPosicao());
        }
    }
    
}
