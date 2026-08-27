package com.opsfactor.community.capability.masterdata.production.operation.domain;

import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * A interface de uma operação pode ser implementada tanto pela operação em um roteiro
 * quanto pela operação em uma ordem de produção
 * A operação representa exclusivamente o consumo temporal de um recurso.
 * Quantidade-base, unidade de medida e outputs pertencem ao cabeçalho produtivo
 * que contém a operação e não podem ser alterados por esta entidade filha.
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
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private RecursoProdutivo recursoProdutivo;
    
    /** Retorna a duração da operação normalizada em horas. */
    public abstract double getHorasPorQuantidadeBase();

    /** Compatibilidade com integrações que enviam duração já em horas. */
    public abstract void setHorasPorQuantidadeBase(Double horasPorQuantidadeBase);

}
