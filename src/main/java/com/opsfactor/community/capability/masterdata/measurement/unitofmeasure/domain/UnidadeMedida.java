package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * Unidade de medida fisica usada por vendas, estoque, producao e transporte.
 *
 * <p>Conversoes globais e especificas por material ficam em entidades
 * separadas. Esta entidade e deliberadamente pequena porque o Community nao
 * modela unidades financeiras, precos ou custos.</p>
 */
@Getter
@Setter
@ToString(of="id")
@EqualsAndHashCode(of = "id")
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UnidadeMedida implements Serializable {

    @Id
    private String id;

    private String descricao;
    
    public UnidadeMedida(String id) {
        this.id = id;
    }
    
}
