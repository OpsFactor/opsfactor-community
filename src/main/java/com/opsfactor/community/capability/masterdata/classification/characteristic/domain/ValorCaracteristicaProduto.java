package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
/**
 * Tabela many-to-many entre produtos e características, que indica para cada combinação caract-produto o respectivo valor 
 */
@Getter
@Setter
@EqualsAndHashCode(of="valorCaracteristicaProdutoCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class ValorCaracteristicaProduto implements Serializable {
    
    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private ValorCaracteristicaProdutoCompositeKey valorCaracteristicaProdutoCompositeKey;

    /**
     * Chave composta de DemandPlanItem
     */
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ValorCaracteristicaProdutoCompositeKey implements Serializable {

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Produto produto;
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private CaracteristicaProduto caracteristicaProduto;

    }
    
    // contém o valor do atributo : binario (true/false), numerico ou categorico
    // deve ser convertível para o tipo Boolean, Double ou String , respectivamente
    @NonNull
    private String atributo;

    public Boolean getAtributoBinario() {
        if (atributo.equals("true")) {
            return true;
        } else if (atributo.equals("false")) {
            return false;
        }
        Logger.getLogger(ValorCaracteristicaProduto.class.getName()).log(Level.WARNING, 
                "Não foi possível ler o atributo binário (" + atributo + ") para produto " + getProduto().getId() +
                        " e característica " + getCaracteristicaProduto().getDescricao());
        return false;
    }
    
    /**
     * Converte o valor cadastrado para uso em coeficientes e filtros numericos
     * do modelo Enterprise de Supply Planning.
     *
     * <p>O valor nao pode cair em fallback silencioso. Retornar zero para um
     * cadastro invalido alteraria o modelo otimizado sem deixar claro que a
     * entrada esta inconsistente.</p>
     */
    public Double getAtributoNumerico() {
        try {
            return Double.valueOf(atributo);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Não foi possível ler o atributo numérico (" + atributo + ") para produto " + getProduto().getId() +
                            " e característica " + getCaracteristicaProduto().getDescricao(),
                    e);
        }
    }
    
    public String getAtributoCategorico() {
        return atributo;
    }
    
    public String getAtributoSemCast() {
        return atributo;
    }
    
    /**
     * Retorna o que seria o Id da característica normalizada
     * @return 
     */
    public String getAtributoCategoricoIdNormalizacao() {
        return "CATEGORICO - " + getCaracteristicaProduto().getId() + " - " + getAtributoCategorico();
    }
    
    public CaracteristicaProduto getCaracteristicaProduto() {
        return valorCaracteristicaProdutoCompositeKey.getCaracteristicaProduto();
    }
    
    public Produto getProduto() {
        return valorCaracteristicaProdutoCompositeKey.getProduto();
    }
    
    public void setCaracteristicaProduto(CaracteristicaProduto caracteristica) {
        if (valorCaracteristicaProdutoCompositeKey == null) valorCaracteristicaProdutoCompositeKey = new ValorCaracteristicaProdutoCompositeKey();
        valorCaracteristicaProdutoCompositeKey.setCaracteristicaProduto(caracteristica);
    }
    
    public void setProduto(Produto produto) {
        if (valorCaracteristicaProdutoCompositeKey == null) valorCaracteristicaProdutoCompositeKey = new ValorCaracteristicaProdutoCompositeKey();
        valorCaracteristicaProdutoCompositeKey.setProduto(produto);
    }

}
