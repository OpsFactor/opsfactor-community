package com.opsfactor.community.capability.transactionaldata.inventory.stock.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;

import jakarta.persistence.*;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Snapshot simples de estoque por location/material/data.
 *
 * <p>No OpsFactor Community este registro representa somente o ponto de partida
 * de estoque usado pelo Supply Planning. Estoque em lote, producao em lote,
 * aging/writeoff e rastreabilidade por batch pertencem ao OpsFactor Enterprise
 * e nao fazem parte desta entidade.</p>
 */
@Getter
@Setter
@EqualsAndHashCode(of="estoqueCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class Estoque implements Serializable {
    
    @EmbeddedId
    @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
    private EstoqueCompositeKey estoqueCompositeKey;
    
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class EstoqueCompositeKey implements Serializable {

        @ManyToOne(optional = false)
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private Location location;

        @ManyToOne(optional = false)
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private Produto produto;
        
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private LocalDateTime dataReferencia;

    }
    
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedida;
    
    @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
    private Double quantidade;
    
    public Location getLocation() {
        return estoqueCompositeKey.getLocation();
    }
    
    public void setLocation(Location location) {
        estoqueCompositeKey.setLocation(location);
    }
    
    public Produto getProduto() {
        return estoqueCompositeKey.getProduto();
    }

    public void setProduto(Produto produto) {
        estoqueCompositeKey.setProduto(produto);
    }
    
    /**
     * Retorna o valor exato da unidade cadastrada. A ser usado apenas no serviço DataUpload
     * @return 
     */
    public UnidadeMedida getUnidadeMedidaCadastrada() {
        return unidadeMedida;
    }
    
    public UnidadeMedida getUnidadeMedida(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedida == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedida;
    }    
    
    public LocalDateTime getDataReferencia() {
        return estoqueCompositeKey.getDataReferencia();
    }
    
    public void setDataReferencia(LocalDateTime dataReferencia) {
        estoqueCompositeKey.setDataReferencia(dataReferencia);
    }

}
