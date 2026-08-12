package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory de escopos de locations usados por projections em memoria.
 *
 * <p>Além dos subconjuntos técnicos, esta classe é a dona compartilhada da
 * semântica pública de seleção por ids e características: ids explícitos são
 * intersectados com as características, características diferentes usam AND
 * e valores da mesma característica usam OR.</p>
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
     * Converte ids públicos de características e locations no recorte
     * canônico sobre o snapshot central.
     *
     * <p>O método recupera o contrato do legado sem realizar consultas por
     * item: características, seus valores e locations já estão indexados no
     * {@link ClusterEParametrosProjection}.</p>
     */
    public static LocationProjection getLocationProjectionFiltroCombinacoesCaracteristicasIds(
            Map<String, ? extends Collection<String>> valuesByLocationCharacteristicId,
            Collection<String> locationIds,
            ClusterEParametrosProjection clusterEParametrosProjection,
            boolean activeLocationsOnly) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);

        return getLocationProjectionFiltroCombinacoesCaracteristicasIds(
                valuesByLocationCharacteristicId,
                locationIds,
                clusterEParametrosProjection.getLocations(activeLocationsOnly),
                clusterEParametrosProjection);

    }

    /**
     * Aplica a mesma semântica canônica sobre candidatos previamente
     * restringidos pelo caller.
     *
     * <p>Este overload permite ao Enterprise compor filtros privados ou salvos
     * depois do recorte físico comum, sem duplicar a resolução de ids e
     * características.</p>
     */
    public static LocationProjection getLocationProjectionFiltroCombinacoesCaracteristicasIds(
            Map<String, ? extends Collection<String>> valuesByLocationCharacteristicId,
            Collection<String> locationIds,
            Collection<Location> candidateLocations,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);
        validaLocations(candidateLocations);

        Map<CaracteristicaLocation, Set<String>> valuesByLocationCharacteristic =
                resolveLocationCharacteristicValues(
                        valuesByLocationCharacteristicId,
                        clusterEParametrosProjection);
        Set<Location> filteredLocations = new LinkedHashSet<>(candidateLocations);

        if (locationIds != null && !locationIds.isEmpty()) {
            Set<Location> explicitlySelectedLocations = locationIds.stream()
                    .map(clusterEParametrosProjection::getLocationPersistida)
                    .collect(Collectors.toSet());
            filteredLocations.retainAll(explicitlySelectedLocations);
        }

        if (!valuesByLocationCharacteristic.isEmpty()) {
            filteredLocations.removeIf(location ->
                    !matchesAllLocationCharacteristics(
                            location,
                            valuesByLocationCharacteristic));
        }

        return getProjectionSetLocations(filteredLocations, clusterEParametrosProjection);

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

    /**
     * Resolve ids de características no mapa indexado do snapshot e descarta
     * somente dimensões com seleção vazia, que não restringem o escopo.
     */
    private static Map<CaracteristicaLocation, Set<String>> resolveLocationCharacteristicValues(
            Map<String, ? extends Collection<String>> valuesByLocationCharacteristicId,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (valuesByLocationCharacteristicId == null) {
            return Map.of();
        }

        Map<CaracteristicaLocation, Set<String>> resolvedValues = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Collection<String>> entry :
                valuesByLocationCharacteristicId.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(
                        "Location characteristic values must not be null.");
            }
            if (entry.getValue().isEmpty()) {
                continue;
            }

            CaracteristicaLocation locationCharacteristic =
                    clusterEParametrosProjection.getCaracteristicaLocationDeId(entry.getKey());
            Set<String> selectedValues = new LinkedHashSet<>();
            for (String selectedValue : entry.getValue()) {
                if (selectedValue == null) {
                    throw new IllegalArgumentException(
                            "Location characteristic values must not contain null.");
                }
                selectedValues.add(selectedValue);
            }
            resolvedValues.put(locationCharacteristic, selectedValues);
        }

        return Collections.unmodifiableMap(resolvedValues);

    }

    /**
     * Implementa AND entre características e OR entre os valores selecionados
     * da mesma característica, com comparação case-insensitive do legado.
     */
    private static boolean matchesAllLocationCharacteristics(
            Location location,
            Map<CaracteristicaLocation, Set<String>> valuesByLocationCharacteristic) {

        return valuesByLocationCharacteristic.entrySet().stream()
                .allMatch(entry -> entry.getKey()
                        .findValorCaracteristicaDeLocation(location)
                        .map(configuredValue -> entry.getValue().stream()
                                .anyMatch(selectedValue ->
                                        selectedValue.equalsIgnoreCase(configuredValue)))
                        .orElse(false));

    }

    private static void validaLocation(
            Location location,
            String mensagemErro) {

        if (location == null) {
            throw new IllegalArgumentException(mensagemErro);
        }

    }

}
