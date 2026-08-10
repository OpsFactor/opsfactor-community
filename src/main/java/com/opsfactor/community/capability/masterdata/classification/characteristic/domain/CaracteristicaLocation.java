package com.opsfactor.community.capability.masterdata.classification.characteristic.domain;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CaracteristicaLocation extends com.opsfactor.community.capability.masterdata.classification.characteristic.domain.Caracteristica implements CaracteristicaLocationInterface, Serializable {

    /**
     * Tabela que determina quais são as principais características de um cluster locations
     */
    /*
     * ClusterLocations e entidade compartilhada do Community. Esta relacao
     * Enterprise pode persistir/atualizar a associacao quando a caracteristica
     * e salva, mas nao pode propagar remocao para o cluster em uma relacao
     * ManyToMany.
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(name = "caracteristica_cluster_locations", 
            joinColumns = @JoinColumn(name = "caracteristica_id"), 
            inverseJoinColumns = @JoinColumn(name = "cluster_locations_id"))
    private Set<ClusterLocations> clustersLocations = new HashSet<>();
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "valorCaracteristicaLocationCompositeKey.caracteristicaLocation",fetch = FetchType.LAZY)
    private List<ValorCaracteristicaLocation> listaValorCaracteristicaLocation = new ArrayList<>();
    
    /**
     * Inclui esta característica no conjunto de características padrão do cluster locations
     *
     * @param clusterLocations cluster de locations que passa a usar a característica como padrão.
     */
    @SuppressWarnings("unused")
    public void addClusterLocations(ClusterLocations clusterLocations) {
        clustersLocations.add(clusterLocations);
    }
    
    public List<String> getValoresCaracteristicaDeListaLocations(List<Location> locations) {
        return locations.stream()
                .map(p -> p.getValorCaracteristica(this))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public String getValorCaracteristicaDeLocation(Location location) {

        /*
         * Location Community conhece apenas o contrato comum de caracteristica.
         * A tabela dinamica de valores pertence ao Enterprise; por isso a
         * entidade Enterprise resolve o valor sem empurrar dependencia privada
         * para Location.
         */
        return listaValorCaracteristicaLocation.stream()
                .filter(valorCaracteristicaLocation ->
                        location.equals(valorCaracteristicaLocation.getLocation()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No value configured for Location "
                                + location.getId()
                                + " and Location Characteristic "
                                + getId()
                                + ". Enterprise characteristic filters require a value for every referenced Location."))
                .getAtributo();

    }

    @Override
    public List<String> getValoresCaracteristica() {
        if (valoresCaracteristica == null) {
            valoresCaracteristica = listaValorCaracteristicaLocation.stream()
                    .map(ValorCaracteristicaLocation::getAtributo)
                    .distinct()
                    .collect(Collectors.toList());
        }

        return valoresCaracteristica;
    }    
    
    /**
     * Necessário, pois @EqualsAndHashCode(super=true) não diferencia CaracteristicaProduto de CaracteristicaLocation
     *
     * @param o objeto comparado com esta característica de location.
     * @return `true` quando os ids representam a mesma característica de location.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CaracteristicaLocation that)) return false;
        return getId().equals(that.getId());
    }

    /**
     * Necessário, pois @EqualsAndHashCode(super=true) não diferencia CaracteristicaProduto de CaracteristicaLocation
     *
     * @return hash calculado pelo id, preservando fallback legado quando a entidade ainda não foi persistida.
     */
    @Override 
    public int hashCode() {
        if (getId() == null) return 43;
        return getId().hashCode();
    }
}
