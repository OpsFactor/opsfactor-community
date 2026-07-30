package com.opsfactor.community.capability.masterdata.production.operation.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A interface de uma operação pode ser implementada tanto pela operação em um roteiro
 * quanto pela operação em uma ordem de produção
 * A operação representa quanto de cada recurso é consumido para cada unidade output
 * do roteiro produzida
 */
@MappedSuperclass
@Getter
@Setter
public abstract class OperacaoAbstract {
    
    /**
     * Acessa o composite key da classe que extende OperacaoAbstract e extrai o Id da operação
     * Atributo Id não é declarado explicitamente pois em cada implementação está dentro de uma
     * chave composta diferente
     * ex : 10, 20, 30
     * @return 
     */
    public abstract Integer getPosicao();
    
    /**
     * Operação de Roteiro : extrai material output roteiro
     * Operação de ordem produção : extrai material de ordem produção
     * Operação de ordem planejada : extrai material de ordem planejada
     * @return 
     */
    public abstract Produto getMaterialOutput();
    
    @ManyToOne(optional = false)
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private RecursoProdutivo recursoProdutivo;
    
    @Getter(AccessLevel.NONE)
    @ManyToOne(optional = true)
    private UnidadeMedida unidadeMedida;
    
    /**
     * Quantidade do material output do roteiro associada ao tempo de produção
     */
    private Float quantidadeBase;

    /**
     * Quantas horas se consome do recurso produtivo 
     * para se produzir a quantidade base
     */
    private Float horasPorQuantidadeBase;
    
    public float getHorasPorQuantidadeBase() {
        if (horasPorQuantidadeBase == null) {
            return 1f;
        } else if (horasPorQuantidadeBase > 0.0000001) {
            return horasPorQuantidadeBase;
        } else {
            return 1f;
        }
    }
    
    public float getQuantidadeBase() {
        if (quantidadeBase == null) {
            return 1f;
        } else if (quantidadeBase > 0) {
            return quantidadeBase;
        } else {
            return 1f;
        }
    }
    
    public UnidadeMedida getUnidadeMedida(ParametrosGlobais parametrosGlobais) {
        if (unidadeMedida != null) return unidadeMedida;
        return parametrosGlobais.getUnidadeMedidaPadraoSNP();
    }
    
    public UnidadeMedida getUnidadeMedidaCadastrado() {
        return unidadeMedida;
    }

}
