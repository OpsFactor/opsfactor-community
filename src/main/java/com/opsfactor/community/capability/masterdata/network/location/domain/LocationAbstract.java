package com.opsfactor.community.capability.masterdata.network.location.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.persistence.*;

/**
 * Base JPA das locations Community.
 *
 * <p>O Community mantem apenas identificacao, tipo funcional e endereco
 * geografico simples. Caracteristicas dinamicas, capacidade logistica por data,
 * GIS e demais estruturas de visualizacao/otimizacao permanecem no
 * Enterprise.</p>
 */
@Data
@MappedSuperclass
public class LocationAbstract {

    /**
     * Tipos funcionais de location usados em todo o fluxo de supply.
     * O novo tipo de transbordo se comporta como uma location interna na maior
     * parte da malha, mas será tratado de forma distinta nas rotinas fiscais.
     */
    public enum TipoLocation {
        @JsonProperty("Internal") INTERNA,
        @JsonProperty("End Client") CLIENTE_FINAL, // sempre primeiro low level code. pedidos/vendas para clientes finais são considerados sell-out e portanto passíveis de forecast
        @JsonProperty("Distributor") DISTRIBUIDOR, // tratado como cliente final nos fluxos Community quando nao houver recorte Enterprise especifico
        @JsonProperty("Supplier") FORNECEDOR, // qualquer material com linha transporte outbound poderá ser produzido mesmo sem roteiro
        @JsonProperty("Commercial Region") REGIAO_COMERCIAL, // tipicamente usado para representar regiões que consolidam diferentes clientes
        @JsonProperty("Transshipment Point") PONTO_TRANSBORDO;
    }

    @Enumerated(EnumType.STRING)
    protected TipoLocation tipoLocation;

    protected String descricao;

    @Column(length = 50)
    protected String pais;
    @Column(length = 50)
    protected String estado;
    protected String cidade;

    protected Double latitude;
    protected Double longitude;

    public TipoLocation getTipoLocation() {
        return (tipoLocation == null) ? TipoLocation.INTERNA : tipoLocation;
    }

    public TipoLocation getTipoLocationCadastrada() {
        return tipoLocation;
    }

}
