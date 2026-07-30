package com.opsfactor.community.capability.cluster.domain.location;

import lombok.*;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;

@Data @Entity
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(of = "regraAlocacaoClusterLocationsPaisEstadoCompositeKey")
public class RegraAlocacaoClusterLocationsPaisEstado implements Serializable{

    @EmbeddedId
    private RegraAlocacaoClusterLocationsPaisEstadoCompositeKey regraAlocacaoClusterLocationsPaisEstadoCompositeKey;

    @Embeddable 
    @Data 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode
    public static class RegraAlocacaoClusterLocationsPaisEstadoCompositeKey implements Serializable {

        @ManyToOne(optional = false)
        @NonNull
        private RegraAlocacaoClusterLocations regraAlocacaoClusterLocations;

        // armazenado como "" para representar locations sem pais preenchido
        @NonNull
        private String pais;

        // armazenado como "" para representar locations sem estado preenchido
        @NonNull
        private String estado;

    }

    public String getPais(){
        return getRegraAlocacaoClusterLocationsPaisEstadoCompositeKey().getPais();
    }
    public String getEstado(){
        return getRegraAlocacaoClusterLocationsPaisEstadoCompositeKey().getEstado();
    }
    public void setPais(String pais){
        getRegraAlocacaoClusterLocationsPaisEstadoCompositeKey().setPais(pais);
    }
    public void setEstado(String estado){
        getRegraAlocacaoClusterLocationsPaisEstadoCompositeKey().setEstado(estado);
    }
    public void setRegraAlocacaoClusterLocations(RegraAlocacaoClusterLocations regraAlocacaoClusterLocations) {
        getRegraAlocacaoClusterLocationsPaisEstadoCompositeKey().regraAlocacaoClusterLocations = regraAlocacaoClusterLocations;
    }

    public RegraAlocacaoClusterLocationsPaisEstadoCompositeKey getCompositeKey(){
        if (regraAlocacaoClusterLocationsPaisEstadoCompositeKey == null) regraAlocacaoClusterLocationsPaisEstadoCompositeKey = new RegraAlocacaoClusterLocationsPaisEstadoCompositeKey();
        return regraAlocacaoClusterLocationsPaisEstadoCompositeKey;
    }

    public RegraAlocacaoClusterLocations getRegraAlocacaoClusterLocations() {
        return regraAlocacaoClusterLocationsPaisEstadoCompositeKey.getRegraAlocacaoClusterLocations();
    }
}
