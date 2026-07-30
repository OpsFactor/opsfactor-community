package com.opsfactor.community.capability.cluster.domain.location;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;

@Data @Entity
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(of = "regraAlocacaoClusterLocationsTipoLocationCompositeKey")
public class RegraAlocacaoClusterLocationsTipoLocation implements Serializable{

    @EmbeddedId
    private RegraAlocacaoClusterLocationsTipoLocationCompositeKey regraAlocacaoClusterLocationsTipoLocationCompositeKey;

    @Embeddable 
    @Data 
    @NoArgsConstructor 
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class RegraAlocacaoClusterLocationsTipoLocationCompositeKey implements Serializable {

        @ManyToOne(optional = false)
        @NonNull
        private RegraAlocacaoClusterLocations regraAlocacaoClusterLocations;

        @Enumerated(EnumType.STRING)
        private Location.TipoLocation tipoLocation;

    }

    public Location.TipoLocation getTipoLocation(){
        return getRegraAlocacaoClusterLocationsTipoLocationCompositeKey().getTipoLocation();
    }
    public String getTipoLocationComoString(){
        return MetodosUtilidade.getValorJsonPropertyDeEnum(getTipoLocation());
    }
    public void setTipoLocation(Location.TipoLocation tipoLocation){
        getRegraAlocacaoClusterLocationsTipoLocationCompositeKey().setTipoLocation(tipoLocation);
    }
    public void setRegraAlocacaoClusterLocations(RegraAlocacaoClusterLocations regraAlocacaoClusterLocations) {
        getRegraAlocacaoClusterLocationsTipoLocationCompositeKey().regraAlocacaoClusterLocations = regraAlocacaoClusterLocations;
    }

    public RegraAlocacaoClusterLocationsTipoLocationCompositeKey getCompositeKey(){
        if (regraAlocacaoClusterLocationsTipoLocationCompositeKey == null) regraAlocacaoClusterLocationsTipoLocationCompositeKey = new RegraAlocacaoClusterLocationsTipoLocationCompositeKey();
        return regraAlocacaoClusterLocationsTipoLocationCompositeKey;
    }

    public RegraAlocacaoClusterLocations getRegraAlocacaoClusterLocations() {
        return regraAlocacaoClusterLocationsTipoLocationCompositeKey.getRegraAlocacaoClusterLocations();
    }
}
