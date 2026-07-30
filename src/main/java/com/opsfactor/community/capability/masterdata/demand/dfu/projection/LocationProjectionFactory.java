package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory de escopos de locations usados por projections em memoria.
 *
 * <p>No Community ela monta o escopo completo ou subconjuntos tecnicos. O
 * conceito de Location Level configuravel e filtros/agrupadores de location
 * permanece reservado ao Enterprise.</p>
 */
public class LocationProjectionFactory {

    // não se criam instâncias desta classe
    private LocationProjectionFactory() {
    }
    
    public static LocationProjectionCompleto getLocationProjectionCompleto(ClusterEParametrosProjection clusterEParametrosProjection) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);

        LocationProjectionCompleto locationProjectionCompleto = new LocationProjectionCompleto();
        locationProjectionCompleto.setLocations = Collections.unmodifiableSet(clusterEParametrosProjection.getLocationsAtivas()); // já é UnmodifiableSet
        locationProjectionCompleto.clusterEParametrosProjection = clusterEParametrosProjection;
        
        return locationProjectionCompleto;
    }

    public static LocationProjection getProjectionDeDfus(Collection<DFU> dfus, ClusterEParametrosProjection clusterEParametrosProjection) {

        validaDfus(dfus);

        Set<Location> locations = dfus.stream().map(DFU::getLocation).collect(Collectors.toSet());
        return getProjectionSetLocations(locations, clusterEParametrosProjection);
    }

    public static LocationProjection getProjectionSetLocations(Collection<Location> locations, ClusterEParametrosProjection clusterEParametrosProjection) {

        validaLocations(locations);

        LocationProjection locationProjection = new LocationProjection();
        locationProjection.clusterEParametrosProjection = clusterEParametrosProjection;
        locationProjection.setLocations = Collections.unmodifiableSet(new HashSet(locations));
        
        return locationProjection;
        
    }

    public static LocationProjection getProjectionClusterLocations(ClusterLocations clusterLocations, ClusterEParametrosProjection clusterEParametrosProjection, boolean somenteLocationsAtivas) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);
        if (clusterLocations == null) {
            throw new IllegalArgumentException("LocationProjectionFactory received null location cluster.");
        }

        LocationProjection locationProjection = new LocationProjection();
        locationProjection.clusterEParametrosProjection = clusterEParametrosProjection;
        locationProjection.setLocations = Collections.unmodifiableSet(clusterEParametrosProjection.getLocationsDeClusterLocations(clusterLocations, somenteLocationsAtivas));

        return locationProjection;

    }

    public static LocationProjection getProjectionSetLocationIds(Collection<String> locationIds, ClusterEParametrosProjection clusterEParametrosProjection) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);
        validaLocationIds(locationIds);

        Set<Location> locations = locationIds.stream()
                .map(locationId -> clusterEParametrosProjection.getLocationPersistida(locationId))
                .collect(Collectors.toSet());
        
        return getProjectionSetLocations(locations, clusterEParametrosProjection);
        
    }

    /**
     * Cria a projection de locations considerada pelo Supply Planning
     * Community.
     *
     * <p>Overrides por location pertencem ao Enterprise; por isso a base do
     * Community e sempre a projection completa de locations ativas. O unico
     * refinamento mantido aqui e o filtro global de propagacao de demanda, que
     * evita processar clientes/regioes como locations produtivas quando o
     * perfil indica consolidacao de demanda.</p>
     *
     * @param perfilExecucaoSupplyPlan
     * @param clusterEParametrosProjection 
     */
    public static LocationProjection getLocationProjectionDePerfilExecucaoSupplyPlan(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (perfilExecucaoSupplyPlan == null) {
            throw new IllegalArgumentException("Supply Planning execution profile is required for location projection.");
        }
        validaClusterEParametrosProjection(clusterEParametrosProjection);

        LocationProjection locationProjection = getLocationProjectionCompleto(clusterEParametrosProjection);
        
        // limita o LocationProjection a somente locations internas / fornecedores caso o parametro 'LocationsClienteApenasPropagamDemanda'
        // do perfil de execução tenha sido habilitado
        if (perfilExecucaoSupplyPlan.getLocationsClienteApenasPropagamDemanda()) {
            List<Location.TipoLocation> tiposLocationsConsiderados = Arrays.stream(LocationAbstract.TipoLocation.values())
                    .collect(Collectors.toList());
            // remove TipoLocation cliente e, se a propagação tiver como destino locations internas, também TipoLocation regiao comercial
            tiposLocationsConsiderados.removeAll(
                    perfilExecucaoSupplyPlan
                            .getModoPropagacaoDemanda()
                            .getTiposLocationOrigemPropagacao());
            locationProjection = locationProjection.getLocationProjectionComFiltroAdicionalTiposLocation(
                    tiposLocationsConsiderados
                            .toArray(Location.TipoLocation[]::new));
        }

        return locationProjection;
                                
    }

    /**
     * Valida a projection base que acompanha todo escopo de locations.
     */
    private static void validaClusterEParametrosProjection(
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (clusterEParametrosProjection == null) {
            throw new IllegalArgumentException("Cluster/parameter projection is required for location projection.");
        }

    }

    /**
     * Valida uma colecao explicita de locations antes de publica-la como set
     * imutavel.
     *
     * <p>Colecao vazia e valida: representa um recorte tecnico sem locations.
     * Colecao nula ou item nulo indica snapshot quebrado de DFU/filtro tecnico
     * e deve falhar antes de virar NPE em calculo paralelo.</p>
     */
    private static void validaLocations(Collection<Location> locations) {

        if (locations == null) {
            throw new IllegalArgumentException("LocationProjectionFactory received null location collection.");
        }

        int indice = 0;
        for (Location location : locations) {
            validaLocation(
                    location,
                    "LocationProjectionFactory received null location at index " + indice + ".");
            indice++;
        }

    }

    /**
     * Valida DFUs antes de extrair suas locations.
     */
    private static void validaDfus(Collection<DFU> dfus) {

        if (dfus == null) {
            throw new IllegalArgumentException("LocationProjectionFactory received null DFU collection.");
        }

        int indice = 0;
        for (DFU dfu : dfus) {
            if (dfu == null) {
                throw new IllegalArgumentException("LocationProjectionFactory received null DFU at index " + indice + ".");
            }
            validaLocation(
                    dfu.getLocation(),
                    "LocationProjectionFactory received DFU without location at index " + indice + ".");
            indice++;
        }

    }

    /**
     * Valida ids antes de chamar a projection central para resolver entidades
     * persistidas.
     */
    private static void validaLocationIds(Collection<String> locationIds) {

        if (locationIds == null) {
            throw new IllegalArgumentException("LocationProjectionFactory received null location id collection.");
        }

        int indice = 0;
        for (String locationId : locationIds) {
            if (locationId == null || locationId.isBlank()) {
                throw new IllegalArgumentException("LocationProjectionFactory received blank location id at index "
                        + indice + ".");
            }
            indice++;
        }

    }

    private static void validaLocation(
            Location location,
            String mensagemErro) {

        if (location == null) {
            throw new IllegalArgumentException(mensagemErro);
        }

    }

}
