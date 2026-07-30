package com.opsfactor.community.capability.transactionaldata.sales.sellout.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * O sell-out não possui destino. No caso de uma location CLIENTE_FINAL representa ou a venda para este cliente (sell-in) como o sell-out propriamente dito
 * No caso de ser associado a uma location interna representa uma venda sem destino definido (ex. vendas em lojas ou vendas onde o cliente final não é relevante)
 *
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@EqualsAndHashCode(of = "id")
public class Sellout {
    
    @Id
    @Column(length = 140)
    private String id;
        
    @ManyToOne(optional=false)
    @NonNull
    private Location locationOrigem;
    
    @NonNull
    private LocalDateTime dataVenda;
    
    @ManyToOne(optional = false)
    @NonNull
    private Produto produto;

    @ManyToOne(optional=true)
    @Getter(AccessLevel.NONE)
    private UnidadeMedida unidadeMedida;

    /**
     * Quantidade já entregue na unidade de medida do produto : peças, litros,
     * caixas, pallets, kg
     */
    private Double quantidade;

    // Valor Gross
    private Double valorTotal;

    // Para cálculo Net
    private Double valorDescontos;
    // impostos devidos pela origem
    private Double valorImpostosOrigem;
    
    // COGS
    private Double valorCusto;
    
    public UnidadeMedida getUnidadeMedida(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedida == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedida;
    }
    
    public UnidadeMedida getUnidadeMedidaCadastrada() {
        return unidadeMedida;
    }
    
}
