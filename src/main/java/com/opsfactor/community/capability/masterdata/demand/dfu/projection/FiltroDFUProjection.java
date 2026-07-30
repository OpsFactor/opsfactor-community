package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import org.javatuples.Pair;

import jakarta.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Projection de escopo DFU usado para restringir combinacoes material/location.
 *
 * <p>A entidade JPA de material ainda se chama {@link Produto}; por isso alguns
 * getters vindos de {@link DFU} continuam fisicamente como `getProduto()`. Esta
 * classe, porem, e uma borda de dominio e deve expor material/location como
 * linguagem de negocio da migracao Community/Enterprise.</p>
 */
public class FiltroDFUProjection {
    
    /**
     * Projection estrutural usada apenas nas operacoes que precisam consultar
     * se uma DFU esta ativa. Escopos puramente explicitos podem existir sem
     * essa projection, mas metodos como {@link #getDFUs()} e
     * {@link #getDFUsViaveis(Set, Set)} devem falhar cedo se ela nao tiver sido
     * materializada.
     */
    protected ClusterEParametrosProjection clusterEParametrosProjection;

    /**
     * TRUE: o escopo e o produto cartesiano entre {@link #locations} e
     * {@link #materiais}. FALSE: o escopo e o mapa explicito
     * location -> materiais.
     */
    private boolean todasCombinacoesLocationMaterialHabilitadas;
    
    /**
     * Usados apenas quando o escopo e produto cartesiano. Mantemos colecoes
     * mutaveis internas porque {@link #aplicaFiltroDFUs(Collection)} restringe
     * o escopo em memoria durante fluxos de Planning Book.
     */
    private Set<Location> locations;
    private Set<Produto> materiais;
    
    /**
     * Só usado se projection for criado com coleção de DFUs
     */
    private Map<Location,Set<Produto>> mapaMateriaisPorLocation;
    
    public FiltroDFUProjection(Collection<DFU> dfus, ClusterEParametrosProjection clusterEParametrosProjection) {
        
        this.clusterEParametrosProjection = clusterEParametrosProjection;
        todasCombinacoesLocationMaterialHabilitadas = false;
        validaDfus(dfus, "FiltroDFUProjection DFU collection");
        mapaMateriaisPorLocation = getMapaMateriaisPorLocationDeDFUs(dfus);
        
    }
    
    public FiltroDFUProjection(boolean todasCombinacoesLocationMaterialHabilitadas, ClusterEParametrosProjection clusterEParametrosProjection) {
        
        this.todasCombinacoesLocationMaterialHabilitadas = todasCombinacoesLocationMaterialHabilitadas;
        this.clusterEParametrosProjection = clusterEParametrosProjection;
        
        if (todasCombinacoesLocationMaterialHabilitadas) {
            locations = new HashSet<>();
            materiais = new HashSet<>();
        } else {
            mapaMateriaisPorLocation = new HashMap<>();
        }
    }
    
    /**
     * Construtor para todas as combinações de materiais e locations.
     * @param locations
     * @param materiais
     */
    public FiltroDFUProjection(Set<Location> locations, Set<Produto> materiais, ClusterEParametrosProjection clusterEParametrosProjection) {
        
        todasCombinacoesLocationMaterialHabilitadas = true;
        this.clusterEParametrosProjection = clusterEParametrosProjection;

        validaLocations(locations, "FiltroDFUProjection location set");
        validaMateriais(materiais, "FiltroDFUProjection material set");
        this.locations = new HashSet<>(locations);
        this.materiais = new HashSet<>(materiais);
        
    }

    /**
     * Construtor para todas as combinações de materiais e locations.
     * @param locations
     */
    public FiltroDFUProjection(Set<Location> locations, ClusterEParametrosProjection clusterEParametrosProjection) {
        
        todasCombinacoesLocationMaterialHabilitadas = true;
        this.clusterEParametrosProjection = clusterEParametrosProjection;

        validaClusterEParametrosProjection(
                clusterEParametrosProjection,
                "FiltroDFUProjection requires cluster/parameter projection to derive materials from locations.");
        validaLocations(locations, "FiltroDFUProjection location set");
        this.locations = new HashSet<>(locations);
        this.materiais = locations.stream()
                .flatMap(location -> clusterEParametrosProjection.getMateriaisAtivosEmLocation(location).stream())
                .collect(Collectors.toSet());
        
    }
    
    /**
     * Construtor para todas as combinações de materiais e locations.
     */
    public FiltroDFUProjection(ClusterEParametrosProjection clusterEParametrosProjection) {
        
        todasCombinacoesLocationMaterialHabilitadas = true;
        this.clusterEParametrosProjection = clusterEParametrosProjection;

        validaClusterEParametrosProjection(
                clusterEParametrosProjection,
                "FiltroDFUProjection requires cluster/parameter projection to build complete DFU scope.");
        this.locations = clusterEParametrosProjection.getLocationsAtivas();
        this.materiais = locations.stream()
                .flatMap(location -> clusterEParametrosProjection.getMateriaisAtivosEmLocation(location).stream())
                .collect(Collectors.toSet());
        
    }
    
    private static Map<Location,Set<Produto>> getMapaMateriaisPorLocationDeDFUs(Collection<DFU> dfus) {
        return dfus.stream()
                .collect(Collectors.groupingBy(
                        x -> x.getLocation(),
                        Collectors.mapping(x -> ((DFU) x).getProduto(), Collectors.toSet())));
    }
    
    /**
     * Retorna os materiais habilitados em uma location dentro deste escopo DFU.
     */
    public Set<Produto> getMateriaisDeLocation(Location location) {

        validaLocation(location, "FiltroDFUProjection location is required to read materials by location.");
        if (todasCombinacoesLocationMaterialHabilitadas == true) return materiais;
        
        return mapaMateriaisPorLocation
                .getOrDefault(location, new HashSet<>());

    }
    
    /**
     * Retorna todos os materiais presentes no escopo DFU.
     */
    public Set<Produto> getMateriais() {

        if (todasCombinacoesLocationMaterialHabilitadas == true) return materiais;
        
        return mapaMateriaisPorLocation.values().stream()
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());               

    }
    
    public Set<Location> getLocations() {
        if (todasCombinacoesLocationMaterialHabilitadas == true) return locations;
        
        return mapaMateriaisPorLocation.keySet();
    }
    
    /**
     * Streama pares location -> materiais sem materializar uma lista
     * intermediaria de DFUs.
     */
    public Stream<Pair<Location,Set<Produto>>> getStreamMateriaisPorLocation() {

        if (todasCombinacoesLocationMaterialHabilitadas == true) {
            return locations.stream()
                    .map(location -> Pair.with(location, materiais));
        } else {
            return mapaMateriaisPorLocation.entrySet().stream()
                    .map(entry -> Pair.with(entry.getKey(), entry.getValue()));
        }

    }
    
    public void addDFU(Location location, Produto material) {
        validaLocation(location, "FiltroDFUProjection cannot add DFU without location.");
        validaMaterial(material, "FiltroDFUProjection cannot add DFU without material.");

        if (todasCombinacoesLocationMaterialHabilitadas == true) {
            materiais.add(material);
            locations.add(location);
            return;
        } else {
            mapaMateriaisPorLocation
                    .computeIfAbsent(location, loc -> new HashSet<>())
                    .add(material);
        }
    }
    
    public void removeDFU(Location location, Produto material) {

        validaLocation(location, "FiltroDFUProjection cannot remove DFU without location.");
        validaMaterial(material, "FiltroDFUProjection cannot remove DFU without material.");
        if (todasCombinacoesLocationMaterialHabilitadas == true) return;

        Set<Produto> materiaisFiltradosNaLocation = mapaMateriaisPorLocation.get(location);
        
        if (materiaisFiltradosNaLocation != null) {
            materiaisFiltradosNaLocation.remove(material);
            if (materiaisFiltradosNaLocation.isEmpty()) {
                mapaMateriaisPorLocation.remove(location);
            }
        }
         
    }
    
    public long getNumeroDFUs() {
        if (todasCombinacoesLocationMaterialHabilitadas == true) {
            return materiais.size() * locations.size();
        } else {
            return mapaMateriaisPorLocation.entrySet().stream()
                    .flatMap(x -> x.getValue().stream())
                    .count();
        }
    }
    
    public boolean contemCombinacaoLocationMaterial(Location location, Produto material) {
        validaLocation(location, "FiltroDFUProjection location is required to test DFU scope.");
        validaMaterial(material, "FiltroDFUProjection material is required to test DFU scope.");

        if (todasCombinacoesLocationMaterialHabilitadas == true) {
            if (!locations.contains(location)) return false;
            if (!materiais.contains(material)) return false;
            return true;
        } else {
            Set<Produto> materiaisLocation = mapaMateriaisPorLocation.get(location);
            if (materiaisLocation == null || materiaisLocation.isEmpty()) return false;
            if (!materiaisLocation.contains(material)) return false;
            return true;
        }
    }
    
    public boolean getTodasCombinacoesLocationMaterialHabilitadas() {
        return todasCombinacoesLocationMaterialHabilitadas;
    }
    
    public List<DFU> getDFUs() {

        validaClusterEParametrosProjection(
                clusterEParametrosProjection,
                "FiltroDFUProjection requires cluster/parameter projection to list active DFUs.");
        List<DFU> dfus = new ArrayList<>();
        
        if (todasCombinacoesLocationMaterialHabilitadas) {
            return locations.stream()
                    .flatMap(location -> materiais.stream()
                            .filter(material -> clusterEParametrosProjection.isDfuAtiva(material, location))
                            .map(material -> new DFU(material, location)))
                    .collect(Collectors.toList());
        } else {
        
            for (Entry<Location,Set<Produto>> entry : mapaMateriaisPorLocation.entrySet()) {
                Location location = entry.getKey();
                for (Produto material : entry.getValue()) {
                    if (clusterEParametrosProjection.isDfuAtiva(material, location)) {
                        dfus.add(new DFU(material, location));
                    }
                }
            }

            return dfus;
            
        }
                
    }
    
    /**
     * Traz DFUs da interseccao do DFUProjection com escopos tecnicos de
     * materiais e locations.
     *
     * <p>Esses parametros sao restricoes locais do processamento em memoria,
     * nao os filtros/agregadores configuraveis Enterprise.</p>
     *
     * @param filtroMateriais
     * @param filtroLocations
     * @return 
     */
    public List<DFU> getDFUsViaveis(@Nullable Set<Produto> filtroMateriais, @Nullable Set<Location> filtroLocations) {
        validaClusterEParametrosProjection(
                clusterEParametrosProjection,
                "FiltroDFUProjection requires cluster/parameter projection to list viable active DFUs.");
        validaMateriaisOpcionais(filtroMateriais, "FiltroDFUProjection material filter");
        validaLocationsOpcionais(filtroLocations, "FiltroDFUProjection location filter");

        if (todasCombinacoesLocationMaterialHabilitadas == true) { // visão não possui filtros nível DFU
            if (filtroLocations != null && !filtroLocations.isEmpty()) {
                if (filtroMateriais != null && !filtroMateriais.isEmpty()) {
                    return filtroLocations.stream()
                            .filter(location -> locations.contains(location))
                            .flatMap(location -> filtroMateriais.stream()
                                    .filter(material -> materiais.contains(material))
                                    .filter(material -> clusterEParametrosProjection.isDfuAtiva(material, location))
                                    .map(material -> new DFU(material, location)))
                            .collect(Collectors.toList());
                } else {
                    return filtroLocations.stream()
                            .filter(location -> locations.contains(location))
                            .flatMap(location -> materiais.stream()
                                    .filter(material -> clusterEParametrosProjection.isDfuAtiva(material, location))
                                    .map(material -> new DFU(material, location)))
                            .collect(Collectors.toList());
                }
            } else {
                if (filtroMateriais != null && !filtroMateriais.isEmpty()) {
                    return locations.stream()
                            .flatMap(location -> filtroMateriais.stream()
                                    .filter(material -> materiais.contains(material))
                                    .filter(material -> clusterEParametrosProjection.isDfuAtiva(material, location))
                                    .map(material -> new DFU(material, location)))
                            .collect(Collectors.toList());
                } else {
                    return locations.stream()
                            .flatMap(location -> materiais.stream()
                                    .filter(material -> clusterEParametrosProjection.isDfuAtiva(material, location))
                                    .map(material -> new DFU(material, location)))
                            .collect(Collectors.toList());
                }
            }
        } else { // há filtros nível DFU
            if (filtroLocations != null && !filtroLocations.isEmpty()) {
                if (filtroMateriais != null && !filtroMateriais.isEmpty()) {
                    return filtroLocations.stream()
                            .flatMap(location -> filtroMateriais.stream()
                                    .filter(material -> mapaMateriaisPorLocation.getOrDefault(location, new HashSet<>()).contains(material))
                                    .filter(material -> clusterEParametrosProjection.isDfuAtiva(material, location))
                                    .map(material -> new DFU(material, location)))
                            .collect(Collectors.toList());
                } else {
                    return filtroLocations.stream()
                            .flatMap(location -> mapaMateriaisPorLocation.getOrDefault(location, new HashSet<>()).stream()
                                    .filter(material -> clusterEParametrosProjection.isDfuAtiva(material, location))
                                    .map(material -> new DFU(material, location)))
                            .collect(Collectors.toList());
                }
            } else {
                if (filtroMateriais != null && !filtroMateriais.isEmpty()) {
                    return mapaMateriaisPorLocation.entrySet().stream()
                            .flatMap(entry -> filtroMateriais.stream()
                                    .filter(material -> entry.getValue().contains(material))
                                    .filter(material -> clusterEParametrosProjection.isDfuAtiva(material, entry.getKey()))
                                    .map(material -> new DFU(material, entry.getKey())))
                            .collect(Collectors.toList());
                } else {
                    return mapaMateriaisPorLocation.entrySet().stream()
                            .flatMap(entry -> entry.getValue().stream()
                                    .filter(material -> clusterEParametrosProjection.isDfuAtiva(material, entry.getKey()))
                                    .map(material -> new DFU(material, entry.getKey())))
                            .collect(Collectors.toList());
                }
            }
        }
    }
    
    /**
     * Atualiza lista de materiais/locations ou mapa de combinações material/location restringindo os valores
     * possíveis aos DFUs passados como argumento
     * @param dfusFiltro
     */
    public void aplicaFiltroDFUs(Collection<DFU> dfusFiltro) {

        validaDfus(dfusFiltro, "FiltroDFUProjection DFU filter collection");
        if (todasCombinacoesLocationMaterialHabilitadas) {
            Set<Location> locationsFiltroDFU = dfusFiltro.stream()
                    .map(DFU::getLocation)
                    .collect(Collectors.toSet());
            locations.retainAll(locationsFiltroDFU);
            
            Set<Produto> materiaisFiltroDFU = dfusFiltro.stream()
                    .map(DFU::getProduto)
                    .collect(Collectors.toSet());
            materiais.retainAll(materiaisFiltroDFU);
        } else {
            Map<Location,Set<Produto>> mapaLocationMateriaisDeDfuFiltro = dfusFiltro.stream()
                    .collect(Collectors.groupingBy(DFU::getLocation, Collectors.mapping(
                            DFU::getProduto, Collectors.toSet())));
            
            // remove locations de mapaMateriaisPorLocation que nao estejam no mapaLocationMateriaisDeDfuFiltro
            Set<Location> locationsARemover = new HashSet<>(mapaMateriaisPorLocation.keySet());
            locationsARemover.removeAll(mapaLocationMateriaisDeDfuFiltro.keySet());
            for (Location locationARemover : locationsARemover) {
                mapaMateriaisPorLocation.remove(locationARemover);
            }
            
            for (Entry<Location,Set<Produto>> entryMapaMateriaisPorLocation : mapaMateriaisPorLocation.entrySet()) {
                
                Location location = entryMapaMateriaisPorLocation.getKey();
                
                Set<Produto> materiaisFiltro = mapaLocationMateriaisDeDfuFiltro.get(location);
                
                entryMapaMateriaisPorLocation.getValue().retainAll(materiaisFiltro);
                
            }
        }
        
    }

    public MaterialProjection getMaterialProjection() {
        return MaterialProjectionFactory.getProjectionSetMateriais(getMateriais(), clusterEParametrosProjection);
    }
    public MaterialProjection getMaterialProjectionAtivosEmLocation(Location location) {
        return MaterialProjectionFactory.getProjectionSetMateriais(getMateriaisDeLocation(location), clusterEParametrosProjection);
    }
    public LocationProjection getLocationProjection() {
        return LocationProjectionFactory.getProjectionSetLocations(getLocations(), clusterEParametrosProjection);
    }

    /**
     * Valida snapshots DFU antes de agrupar por location.
     *
     * <p>O agrupamento por stream geraria NPE ou mapas com chave nula quando o
     * snapshot estivesse quebrado. A validacao explicita preserva a diferenca
     * entre ausencia operacional valida (colecao vazia) e erro estrutural de
     * support data.</p>
     */
    private static void validaDfus(Collection<DFU> dfus, String context) {

        if (dfus == null) {
            throw new IllegalArgumentException(context + " is required.");
        }

        int index = 0;
        for (DFU dfu : dfus) {
            if (dfu == null) {
                throw new IllegalArgumentException(context + " contains null DFU at index " + index + ".");
            }
            if (dfu.getLocation() == null) {
                throw new IllegalArgumentException(context + " contains DFU without location at index " + index + ".");
            }
            if (dfu.getLocation().getId() == null || dfu.getLocation().getId().isBlank()) {
                throw new IllegalArgumentException(context + " contains DFU with location without id at index " + index + ".");
            }
            if (dfu.getProduto() == null) {
                throw new IllegalArgumentException(context + " contains DFU without material at index " + index + ".");
            }
            if (dfu.getProduto().getId() == null || dfu.getProduto().getId().isBlank()) {
                throw new IllegalArgumentException(context + " contains DFU with material without id at index " + index + ".");
            }
            index++;
        }

    }

    private static void validaLocations(Set<Location> locations, String context) {

        if (locations == null) {
            throw new IllegalArgumentException(context + " is required.");
        }
        validaLocationsOpcionais(locations, context);

    }

    private static void validaLocationsOpcionais(Set<Location> locations, String context) {

        if (locations == null) {
            return;
        }

        int index = 0;
        for (Location location : locations) {
            if (location == null) {
                throw new IllegalArgumentException(context + " contains null location at index " + index + ".");
            }
            if (location.getId() == null || location.getId().isBlank()) {
                throw new IllegalArgumentException(context + " contains location without id at index " + index + ".");
            }
            index++;
        }

    }

    private static void validaMateriais(Set<Produto> materiais, String context) {

        if (materiais == null) {
            throw new IllegalArgumentException(context + " is required.");
        }
        validaMateriaisOpcionais(materiais, context);

    }

    private static void validaMateriaisOpcionais(Set<Produto> materiais, String context) {

        if (materiais == null) {
            return;
        }

        int index = 0;
        for (Produto material : materiais) {
            if (material == null) {
                throw new IllegalArgumentException(context + " contains null material at index " + index + ".");
            }
            if (material.getId() == null || material.getId().isBlank()) {
                throw new IllegalArgumentException(context + " contains material without id at index " + index + ".");
            }
            index++;
        }

    }

    private static void validaLocation(Location location, String message) {

        if (location == null) {
            throw new IllegalArgumentException(message);
        }
        if (location.getId() == null || location.getId().isBlank()) {
            throw new IllegalArgumentException(message + " Location id is required.");
        }

    }

    private static void validaMaterial(Produto material, String message) {

        if (material == null) {
            throw new IllegalArgumentException(message);
        }
        if (material.getId() == null || material.getId().isBlank()) {
            throw new IllegalArgumentException(message + " Material id is required.");
        }

    }

    private static void validaClusterEParametrosProjection(
            ClusterEParametrosProjection clusterEParametrosProjection,
            String message) {

        if (clusterEParametrosProjection == null) {
            throw new IllegalArgumentException(message);
        }

    }
    
    
}
