package com.opsfactor.community.capability.configuration.domain.cluster.location;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import lombok.*;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;

/**
 * Parametros operacionais do cluster de locations.
 *
 * <p>No Community, este contrato informa se o cluster participa do Demand
 * Planning. Parametros de Pricing permanecem apenas como campo transicional de
 * schema e nao devem abrir runtime de pricing na edicao aberta.</p>
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class ParametrosClusterLocations implements Serializable {

    @NonNull
    @EmbeddedId
    private ParametrosClusterLocationsCompositeKey parametrosClusterLocationsCompositeKey;
    private Boolean planejaDP;
    private Boolean planejaPricing;

    public ClusterLocations getClusterLocations() {
        return parametrosClusterLocationsCompositeKey.getClusterLocations();
    }

    public void setClusterLocations(ClusterLocations clusterLocations) {
        parametrosClusterLocationsCompositeKey.setClusterLocations(clusterLocations);
    }

    public Boolean getPlanejaDP() {
        return (planejaDP == null) ? true : planejaDP;
    }

    public Boolean getPlanejaPricing() {
        return (planejaPricing == null) ? true : planejaPricing;
    }

    @Data
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ParametrosClusterLocationsCompositeKey implements Serializable {

        /**
         * Cluster dono dos parametros.
         *
         * <p>O lado inverso em `ClusterLocations` tambem e LAZY. Manter este
         * lado como LAZY evita que o bootstrap do Hibernate tente expandir um
         * ciclo eager entre cluster e parametros ao criar metadados/queries de
         * bulk operation.</p>
         */
        @NonNull
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private ClusterLocations clusterLocations;

    }
}
