package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import lombok.EqualsAndHashCode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Escopo de locations usado por projections e rotinas de calculo.
 *
 * <p>No Community esta classe nao representa Location Level configuravel nem
 * filtros/agrupadores funcionais de location. Ela e apenas um value object em
 * memoria para carregar todas as locations ativas ou subconjuntos tecnicos
 * derivados pelo fluxo.</p>
 */
@EqualsAndHashCode(of = {"setLocations"})
public class LocationProjection {
    
    /**
     * Projection base que conhece status, clusters e DFUs ativas.
     */
    protected ClusterEParametrosProjection clusterEParametrosProjection;

    /**
     * Locations candidatas do escopo em memoria. Factories entregam conjuntos
     * imutaveis para evitar mudanca de escopo durante calculos paralelos.
     */
    protected Set<Location> setLocations = new HashSet<>();

    /**
     * Cache lazy das locations ativas dentro do escopo.
     */
    protected Set<Location> setLocationsAtivas;
        
    public Set<Location> getLocationSet() {
        return setLocations;
    }
        
    /**
     * Retorna `null` quando este objeto representa o escopo completo.
     *
     * <p>Algumas queries e projections usam `null` como atalho tecnico para
     * nao aplicar filtro de location. Isso nao representa Location Level
     * configuravel no Community.</p>
     */
    public Set<Location> getLocationsAtivasOuNuloSeLocationProjectionCompleto() {
        if (this instanceof LocationProjectionCompleto) return null;
        return getLocationsAtivas();
    }
    
    public Set<Location> getLocationsAtivas() {
        if (setLocationsAtivas == null) {
            setLocationsAtivas = Location.filtraLocationsAtivasSet(setLocations);
            setLocationsAtivas = Collections.unmodifiableSet(setLocationsAtivas);
        }
        return setLocationsAtivas;
    }    
    
    public Set<Location> getLocationsAtivasSetComTipoLocation(Location.TipoLocation tipoLocation) {
        return Location.filtraLocationsAtivasSetComTipoLocation(setLocations, tipoLocation);
    }
    public Set<Location> getLocationsAtivasSetComTiposLocation(Location.TipoLocation... tiposLocation) {
        return Location.filtraLocationsAtivasSetComTiposLocation(setLocations, tiposLocation);
    }

    public LocationProjection getLocationProjectionComFiltroAdicionalTipoLocation(Location.TipoLocation tipoLocation) {
        LocationProjection novoLocationProjection = new LocationProjection();
        novoLocationProjection.clusterEParametrosProjection = clusterEParametrosProjection;
        novoLocationProjection.setLocations = Collections.unmodifiableSet(getLocationsAtivasSetComTipoLocation(tipoLocation));
        return novoLocationProjection;
    }
    public LocationProjection getLocationProjectionComFiltroAdicionalTiposLocation(Location.TipoLocation... tiposLocation) {
        LocationProjection novoLocationProjection = new LocationProjection();
        novoLocationProjection.clusterEParametrosProjection = clusterEParametrosProjection;
        novoLocationProjection.setLocations = Collections.unmodifiableSet(getLocationsAtivasSetComTiposLocation(tiposLocation));
        return novoLocationProjection;
    }

    public void removeLocations(Collection<Location> locationsARemover) {
        if (locationsARemover == null || locationsARemover.isEmpty()) return;

        setLocations = Collections.unmodifiableSet(
                setLocations
                        .stream()
                        .filter(location -> !locationsARemover.contains(location))
                        .collect(Collectors.toSet()));
        // será recalculado na próxima chamada de getLocationsAtivas
        setLocationsAtivas = null;
    }

}
