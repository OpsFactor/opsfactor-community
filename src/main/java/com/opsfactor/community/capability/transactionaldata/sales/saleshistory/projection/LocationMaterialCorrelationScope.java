package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import org.javatuples.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Escopo correlacionado de leitura de dados por location e material.
 *
 * <p>Ao contrario de dois {@link Set}s independentes, este value object
 * preserva quais materiais pertencem a cada location. Ele permite que uma
 * factory consulte o envelope dos IDs em uma unica JPQL e remova, em memoria,
 * as combinacoes cruzadas que a sintaxe JPQL portavel nao consegue representar
 * como parametro de pares. Portanto, o escopo nao autoriza produto cartesiano
 * entre suas dimensoes.</p>
 *
 * <p>O tipo vive no Community porque e uma fronteira quantitativa neutra de
 * projections. O Enterprise reutiliza o mesmo contrato para sell-in e sales
 * orders, sem precisar expor entidades privadas no modelo aberto.</p>
 */
public final class LocationMaterialCorrelationScope {

    private final Map<String, Set<String>> materialIdsByLocationId;
    private final Set<Location> locations;
    private final Set<Produto> materiais;

    private LocationMaterialCorrelationScope(
            Map<String, Set<String>> materialIdsByLocationId,
            Set<Location> locations,
            Set<Produto> materiais) {

        this.materialIdsByLocationId = materialIdsByLocationId;
        this.locations = locations;
        this.materiais = materiais;

    }

    /**
     * Materializa um escopo a partir de pares location/material ja decididos
     * pelo chamador.
     *
     * <p>Locations sem material sao preservadas apenas como ausencia de pares:
     * elas nao entram no envelope da query, pois nao podem produzir dado
     * historico valido para este escopo.</p>
     */
    public static LocationMaterialCorrelationScope of(
            Map<Location, ? extends Collection<Produto>> materiaisPorLocation) {

        if (materiaisPorLocation == null) {
            throw new IllegalArgumentException(
                    "Location/material correlation scope requires location-to-material map.");
        }

        Map<String, Set<String>> materialIdsByLocationId = new LinkedHashMap<>();
        Set<Location> locations = new LinkedHashSet<>();
        Set<Produto> materiais = new LinkedHashSet<>();

        int locationIndex = 0;
        for (Map.Entry<Location, ? extends Collection<Produto>> entry : materiaisPorLocation.entrySet()) {
            Location location = entry.getKey();
            validaLocation(location, locationIndex);

            Collection<Produto> materiaisDaLocation = entry.getValue();
            if (materiaisDaLocation == null) {
                throw new IllegalArgumentException(
                        "Location/material correlation scope contains null material collection for location "
                                + location.getId() + ".");
            }

            Set<String> materialIds = new LinkedHashSet<>();
            int materialIndex = 0;
            for (Produto material : materiaisDaLocation) {
                validaMaterial(material, location.getId(), materialIndex);
                materialIds.add(material.getId());
                materiais.add(material);
                materialIndex++;
            }

            if (!materialIds.isEmpty()) {
                materialIdsByLocationId
                        .computeIfAbsent(location.getId(), ignored -> new LinkedHashSet<>())
                        .addAll(materialIds);
                locations.add(location);
            }
            locationIndex++;
        }

        Map<String, Set<String>> immutableMaterialIdsByLocationId = new LinkedHashMap<>();
        materialIdsByLocationId.forEach((locationId, materialIds) ->
                immutableMaterialIdsByLocationId.put(
                        locationId,
                        Collections.unmodifiableSet(new LinkedHashSet<>(materialIds))));

        return new LocationMaterialCorrelationScope(
                Collections.unmodifiableMap(immutableMaterialIdsByLocationId),
                Collections.unmodifiableSet(new LinkedHashSet<>(locations)),
                Collections.unmodifiableSet(new LinkedHashSet<>(materiais)));

    }

    /**
     * Converte a projection DFU em escopo de leitura sem recorrer a
     * {@link FiltroDFUProjection#getDFUs()}, que pode eliminar pares inativos.
     */
    public static LocationMaterialCorrelationScope fromDfuScope(
            FiltroDFUProjection filtroDfuProjection) {

        if (filtroDfuProjection == null) {
            throw new IllegalArgumentException(
                    "Location/material correlation scope requires DFU scope.");
        }

        Map<Location, Collection<Produto>> materiaisPorLocation = new LinkedHashMap<>();
        filtroDfuProjection.getStreamMateriaisPorLocation()
                .forEach(locationAndMaterials -> addLocationMaterials(
                        materiaisPorLocation,
                        locationAndMaterials));
        return of(materiaisPorLocation);

    }

    /**
     * Locations que formam o envelope da unica consulta JPQL.
     */
    public Set<Location> getLocations() {

        return locations;

    }

    /**
     * Materiais que formam o envelope da unica consulta JPQL.
     */
    public Set<Produto> getMateriais() {

        return materiais;

    }

    /**
     * Retorna se o par agregado pertence exatamente ao escopo solicitado.
     */
    public boolean contains(
            Location location,
            Produto material) {

        if (location == null || material == null) {
            return false;
        }

        return materialIdsByLocationId
                .getOrDefault(location.getId(), Collections.emptySet())
                .contains(material.getId());

    }

    public boolean isEmpty() {

        return materialIdsByLocationId.isEmpty();

    }

    private static void addLocationMaterials(
            Map<Location, Collection<Produto>> materiaisPorLocation,
            Pair<Location, Set<Produto>> locationAndMaterials) {

        if (locationAndMaterials == null) {
            throw new IllegalArgumentException(
                    "Location/material correlation scope DFU stream contains null pair.");
        }

        materiaisPorLocation.put(
                locationAndMaterials.getValue0(),
                locationAndMaterials.getValue1());

    }

    private static void validaLocation(
            Location location,
            int locationIndex) {

        if (location == null) {
            throw new IllegalArgumentException(
                    "Location/material correlation scope contains null location at index " + locationIndex + ".");
        }
        if (location.getId() == null || location.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Location/material correlation scope contains location without id at index " + locationIndex + ".");
        }

    }

    private static void validaMaterial(
            Produto material,
            String locationId,
            int materialIndex) {

        if (material == null) {
            throw new IllegalArgumentException(
                    "Location/material correlation scope contains null material at index "
                            + materialIndex + " for location " + locationId + ".");
        }
        if (material.getId() == null || material.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Location/material correlation scope contains material without id at index "
                            + materialIndex + " for location " + locationId + ".");
        }

    }

}
