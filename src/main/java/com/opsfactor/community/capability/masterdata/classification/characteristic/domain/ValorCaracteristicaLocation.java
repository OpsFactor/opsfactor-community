package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Tabela many-to-many entre produtos e características, que indica para cada combinação caract-produto o respectivo valor 
 */
@Getter
@Setter
@EqualsAndHashCode(of="valorCaracteristicaLocationCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class ValorCaracteristicaLocation implements Serializable {
    
    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private ValorCaracteristicaLocationCompositeKey valorCaracteristicaLocationCompositeKey;

    /**
     * Chave composta de DemandPlanItem
     */
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ValorCaracteristicaLocationCompositeKey implements Serializable {

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Location location;
        
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation caracteristicaLocation;

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
        Logger.getLogger(ValorCaracteristicaLocation.class.getName()).log(Level.WARNING, 
                "Não foi possível ler o atributo binário (" + atributo + ") para location " + getLocation().getId() +
                        " e característica " + getCaracteristicaLocation().getDescricao());
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
                    "Não foi possível ler o atributo numérico (" + atributo + ") para location " + getLocation().getId() +
                            " e característica " + getCaracteristicaLocation().getDescricao(),
                    e);
        }
    }
    
    public String getAtributoCategorico() {
        return atributo;
    }
    
    public String getAtributoSemCast() {
        return atributo;
    }
    
    public String getAtributoCategoricoIdNormalizacao() {
        return "CATEGORICO - " + getCaracteristicaLocation().getId() + " - " + getAtributoCategorico();
    }
    
    public com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation getCaracteristicaLocation() {
        return valorCaracteristicaLocationCompositeKey.getCaracteristicaLocation();
    }
    
    public Location getLocation() {
        return valorCaracteristicaLocationCompositeKey.getLocation();
    }
    
    public void setCaracteristicaLocation(
            com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation caracteristicaLocation) {
        if (valorCaracteristicaLocationCompositeKey == null) valorCaracteristicaLocationCompositeKey = new ValorCaracteristicaLocationCompositeKey();
        valorCaracteristicaLocationCompositeKey.setCaracteristicaLocation(caracteristicaLocation);
    }
    
    public void setLocation(Location location) {
        if (valorCaracteristicaLocationCompositeKey == null) valorCaracteristicaLocationCompositeKey = new ValorCaracteristicaLocationCompositeKey();
        valorCaracteristicaLocationCompositeKey.setLocation(location);
    }

}
