package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import lombok.*;

import jakarta.persistence.*;
import java.util.List;
/**
 * Base compartilhada das características dinâmicas de material e location.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of="id")
@MappedSuperclass // implementa modelo de herança. esta classe não é uma entidade, apenas define elementos básicos de outras entidades
public abstract class Caracteristica implements CaracteristicaInterface {
        
    @Id
    @Column(length = 50)
    private String id;
    
    @Column(unique=true)
    private String descricao;
   
    /**
     * Determina se a origem é uma caracteristica numerica/binaria ou um atributo categorico
     * Preço também é um tipo de característica
     */
    @Enumerated(EnumType.STRING)
    @NonNull
    private TipoCaracteristica tipoCaracteristica;
    
    public enum TipoCaracteristica {
        BINARIO, NUMERICO, CATEGORICO
    }

    @Transient
    protected List<String> valoresCaracteristica = null;

    /**
     * Extrai todas as instâncias dessa característica (ex. Marca A / Marca B se a característica for Marca)
     * a partir dos valores em cada produto (se CaracteristicaProduto) ou location (se CaracteristicaLocation)
     * Faz uso de atributo Transient para guardar o resultado para futuro reuso
     * @return
     */
    public abstract List<String> getValoresCaracteristica();

    @Override
    public String toString() {
        return descricao;
    }

}
