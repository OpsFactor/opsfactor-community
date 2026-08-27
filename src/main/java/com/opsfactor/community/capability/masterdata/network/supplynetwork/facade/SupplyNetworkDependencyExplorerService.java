package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.*;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import jakarta.annotation.Nullable;
import jakarta.persistence.NoResultException;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Read-only explorer of material-location dependencies for a Supply Network.
 *
 * <p>The service deliberately works only on the cached {@link SupplyNetworkProjection}.
 * The explorer may recurse through a large graph, therefore issuing entity queries while
 * traversing it would make the UI an N+1 query source and diverge from planning semantics.</p>
 */
@Service
public class SupplyNetworkDependencyExplorerService {

    static final String NO_PRODUCTION_ROUTING_AVAILABLE = "No Production Routing Available";
    static final String NO_BILL_OF_MATERIALS_AVAILABLE = "No Bill of Materials Available";

    /** Factory do snapshot de malha reutilizado durante toda a travessia. */
    private final SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /**
     * Fixa a factory da projection de malha usada por toda a travessia do grafo.
     */
    @Autowired
    public SupplyNetworkDependencyExplorerService(
            SupplyNetworkProjectionFactory supplyNetworkProjectionFactory) {

        this.supplyNetworkProjectionFactory = supplyNetworkProjectionFactory;

    }

    /** Builds a tree rooted in the selected location/material or in every active material at the location. */
    public List<SupplyNetworkDependencyDTO> getDependencies(
            String supplyNetworkId,
            String locationId,
            @Nullable String materialId,
            int maximumTreeDepth) {

        SupplyNetworkProjection supplyNetworkProjection =
                supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        VersaoMalha supplyNetwork = supplyNetworkProjection.getVersaoMalhaDeId(supplyNetworkId)
                .orElseThrow(() -> new NoResultException("Supply Network with id " + supplyNetworkId + " not found"));
        ClusterEParametrosProjection parametersProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        Location location = parametersProjection.getLocationPersistida(locationId);
        Produto material = materialId == null ? null : parametersProjection.getMaterialPersistido(materialId);

        return getDependencies(supplyNetwork, location, material, supplyNetworkProjection, maximumTreeDepth);

    }

    private static List<SupplyNetworkDependencyDTO> getDependencies(
            VersaoMalha supplyNetwork,
            Location location,
            @Nullable Produto selectedMaterial,
            SupplyNetworkProjection supplyNetworkProjection,
            int maximumTreeDepth) {

        ClusterEParametrosProjection parametersProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        Set<Produto> materials = selectedMaterial == null
                ? parametersProjection.getMateriaisAtivosEmLocation(location)
                : Set.of(selectedMaterial);
        List<SupplyNetworkDependencyDTO> dependencies = new ArrayList<>();

        for (Produto material : materials) {
            MaterialLocationDependencyDTO dependency = createMaterialLocationDependency(location, material, true, 0);
            dependencies.add(dependency);
            expandMaterialLocation(
                    dependency,
                    supplyNetwork,
                    location,
                    material,
                    supplyNetworkProjection,
                    maximumTreeDepth);
        }

        return dependencies;

    }

    /** Expands production and inbound alternatives while retaining even unusable registrations for diagnosis. */
    private static void expandMaterialLocation(
            MaterialLocationDependencyDTO dependency,
            VersaoMalha supplyNetwork,
            Location location,
            Produto material,
            SupplyNetworkProjection supplyNetworkProjection,
            int maximumTreeDepth) {

        dependency.recursionCut = false;
        dependency.viableProduction = false;
        dependency.viableInbound = false;

        if (dependency.depth >= maximumTreeDepth) {
            dependency.viableStep = dependency.active;
            return;
        }

        Set<VersaoProducao> productionVersions =
                supplyNetworkProjection.getTodasVersoesProducao(location, material, true, null);
        if (location.getTipoLocation().equals(LocationAbstract.TipoLocation.FORNECEDOR)
                && productionVersions.isEmpty()) {
            dependency.productionVersionDependencies.add(createSupplierProductionVersion(location, material));
        } else {
            if (productionVersions.isEmpty()) {
                addTemporaryProductionVersionForIncompleteRegistration(
                        dependency,
                        supplyNetwork,
                        location,
                        material,
                        supplyNetworkProjection,
                        maximumTreeDepth);
            }
            for (VersaoProducao productionVersion : productionVersions) {
                ProductionVersionDependencyDTO productionVersionDependency = new ProductionVersionDependencyDTO();
                productionVersionDependency.elementType = SupplyNetworkDependencyDTO.ElementType.PRODUCTION_VERSION;
                productionVersionDependency.productionVersionId = productionVersion.getId();
                productionVersionDependency.active = productionVersion.getAtivo();
                productionVersionDependency.parallelRoutingsOmitted = false;
                productionVersionDependency.omittedParallelRoutingCount = 0;
                productionVersionDependency.viableStep = productionVersionDependency.active;
                expandProductionVersion(
                        productionVersionDependency,
                        supplyNetwork,
                        productionVersion,
                        material,
                        dependency.depth,
                        supplyNetworkProjection,
                        maximumTreeDepth);
                dependency.productionVersionDependencies.add(productionVersionDependency);
            }
        }

        LocalDateTime materialStatusReference = LocalDateTime.now();
        for (LinhaTransporte transportationLine : supplyNetworkProjection.getLinhasTransporte(
                supplyNetwork, null, Set.of(location))) {
            if (!mustDisplayTransportationLineForMaterial(
                    supplyNetwork, transportationLine, material, supplyNetworkProjection)) {
                continue;
            }
            Location origin = transportationLine.getLocationOrigem();
            TransportationLineDependencyDTO transportationDependency = new TransportationLineDependencyDTO();
            transportationDependency.elementType = SupplyNetworkDependencyDTO.ElementType.TRANSPORTATION_LINE;
            transportationDependency.destinationLocationId = location.getId();
            transportationDependency.originLocationId = origin.getId();
            transportationDependency.materialId = material.getId();
            transportationDependency.active = !supplyNetworkProjection
                    .getLinhaTransporteInboundViavelSetParaLocationMaterial(
                            supplyNetwork, location, material, materialStatusReference, Set.of(origin))
                    .isEmpty();
            transportationDependency.viableStep = transportationDependency.active;
            expandTransportationLine(
                    transportationDependency,
                    supplyNetwork,
                    origin,
                    material,
                    dependency.depth,
                    supplyNetworkProjection,
                    maximumTreeDepth);
            dependency.inboundTransportationLineDependencies.add(transportationDependency);
        }

        dependency.viableProduction = dependency.productionVersionDependencies.stream()
                .anyMatch(productionVersion -> productionVersion.viableStep);
        dependency.viableInbound = dependency.inboundTransportationLineDependencies.stream()
                .anyMatch(transportationLine -> transportationLine.viableStep);
        dependency.viableStep = dependency.active && (dependency.viableProduction || dependency.viableInbound);

    }

    /** Mirrors the planner's synthetic production version when routing and/or BOM were registered independently. */
    private static void addTemporaryProductionVersionForIncompleteRegistration(
            MaterialLocationDependencyDTO materialLocationDependency,
            VersaoMalha supplyNetwork,
            Location location,
            Produto material,
            SupplyNetworkProjection supplyNetworkProjection,
            int maximumTreeDepth) {

        Set<Roteiro> routings = supplyNetworkProjection.getTodosRoteiros(location, material);
        Set<ListaTecnica> billsOfMaterials = supplyNetworkProjection.getTodasListasTecnicas(location, material, null);
        if (routings.isEmpty() && billsOfMaterials.isEmpty()) {
            return;
        }

        ProductionVersionDependencyDTO temporaryVersion = new ProductionVersionDependencyDTO();
        temporaryVersion.elementType = SupplyNetworkDependencyDTO.ElementType.PRODUCTION_VERSION;
        temporaryVersion.productionVersionId = "Temporary Production Version - " + location.getId() + " - " + material.getId();
        temporaryVersion.active = true;
        temporaryVersion.viableStep = true;
        temporaryVersion.parallelRoutingsOmitted = false;
        temporaryVersion.omittedParallelRoutingCount = 0;

        if (routings.isEmpty()) {
            for (ListaTecnica bom : billsOfMaterials) {
                temporaryVersion.routingAndBomCombinationDependencies.add(createRoutingBomCombination(
                        null, bom, supplyNetwork, materialLocationDependency.depth, supplyNetworkProjection, maximumTreeDepth, false));
            }
        } else if (billsOfMaterials.isEmpty()) {
            for (Roteiro routing : routings) {
                temporaryVersion.routingAndBomCombinationDependencies.add(createRoutingBomCombination(
                        routing, null, supplyNetwork, materialLocationDependency.depth, supplyNetworkProjection, maximumTreeDepth, false));
            }
        } else {
            for (Roteiro routing : routings) {
                for (ListaTecnica bom : billsOfMaterials) {
                    temporaryVersion.routingAndBomCombinationDependencies.add(createRoutingBomCombination(
                            routing, bom, supplyNetwork, materialLocationDependency.depth, supplyNetworkProjection, maximumTreeDepth, false));
                }
            }
        }

        temporaryVersion.viableStep = temporaryVersion.routingAndBomCombinationDependencies.stream()
                .anyMatch(combination -> combination.viableStep);
        materialLocationDependency.productionVersionDependencies.add(temporaryVersion);

    }

    private static ProductionVersionDependencyDTO createSupplierProductionVersion(Location location, Produto material) {

        ProductionVersionDependencyDTO dependency = new ProductionVersionDependencyDTO();
        dependency.elementType = SupplyNetworkDependencyDTO.ElementType.PRODUCTION_VERSION;
        dependency.productionVersionId = "Supplier with No Production Version - " + location.getId() + " - " + material.getId();
        dependency.active = true;
        dependency.viableStep = true;
        dependency.parallelRoutingsOmitted = false;
        dependency.omittedParallelRoutingCount = 0;
        return dependency;

    }

    private static boolean mustDisplayTransportationLineForMaterial(
            VersaoMalha supplyNetwork,
            LinhaTransporte transportationLine,
            Produto material,
            SupplyNetworkProjection supplyNetworkProjection) {

        return transportationLine.getHabilitadoProdutosNaoCadastradosLinhaTransporte()
                || supplyNetworkProjection.getLinhaTransporteProduto(supplyNetwork, transportationLine, material).isPresent();

    }

    /** Keeps only the focused output on a parallel version to avoid combinatorial expansion. */
    private static void expandProductionVersion(
            ProductionVersionDependencyDTO dependency,
            VersaoMalha supplyNetwork,
            VersaoProducao productionVersion,
            Produto focusedMaterial,
            int currentDepth,
            SupplyNetworkProjection supplyNetworkProjection,
            int maximumTreeDepth) {

        List<Pair<Roteiro, ListaTecnica>> combinations = getFocusedRoutingBomCombinations(
                dependency, productionVersion, focusedMaterial);
        for (Pair<Roteiro, ListaTecnica> combination : combinations) {
            RoutingBomCombinationDependencyDTO combinationDependency = createRoutingBomCombination(
                    combination.getValue0(),
                    combination.getValue1(),
                    supplyNetwork,
                    currentDepth,
                    supplyNetworkProjection,
                    maximumTreeDepth,
                    Boolean.TRUE.equals(dependency.parallelRoutingsOmitted));
            combinationDependency.parallelRoutingsOmitted = dependency.parallelRoutingsOmitted;
            combinationDependency.omittedParallelRoutingCount = dependency.omittedParallelRoutingCount;
            if (!combinationDependency.viableStep) {
                dependency.viableStep = false;
            }
            dependency.routingAndBomCombinationDependencies.add(combinationDependency);
        }

    }

    private static RoutingBomCombinationDependencyDTO createRoutingBomCombination(
            @Nullable Roteiro routing,
            @Nullable ListaTecnica bom,
            VersaoMalha supplyNetwork,
            int currentDepth,
            SupplyNetworkProjection supplyNetworkProjection,
            int maximumTreeDepth,
            boolean omitRoutingResources) {

        RoutingBomCombinationDependencyDTO dependency = new RoutingBomCombinationDependencyDTO();
        dependency.elementType = SupplyNetworkDependencyDTO.ElementType.ROUTING_BOM_COMBINATION;
        dependency.viableStep = true;
        dependency.parallelRoutingsOmitted = omitRoutingResources;
        dependency.omittedParallelRoutingCount = omitRoutingResources ? 1 : 0;
        dependency.routingDependency = createRoutingDependency(routing, supplyNetworkProjection, omitRoutingResources);
        dependency.bomDependency = createBomDependency(
                bom, supplyNetwork, currentDepth, supplyNetworkProjection, maximumTreeDepth);
        /*
         * Parallel versions intentionally hide the non-focused routing branch.
         * The legacy explorer therefore does not let that omitted branch make
         * the focused combination infeasible; the BOM remains fully evaluated.
         */
        if ((!omitRoutingResources && !dependency.routingDependency.viableStep)
                || !dependency.bomDependency.viableStep) {
            dependency.viableStep = false;
        }
        return dependency;

    }

    private static RoutingDependencyDTO createRoutingDependency(
            @Nullable Roteiro routing,
            SupplyNetworkProjection supplyNetworkProjection,
            boolean omitResources) {

        RoutingDependencyDTO dependency = new RoutingDependencyDTO();
        dependency.elementType = SupplyNetworkDependencyDTO.ElementType.ROUTING;
        if (routing == null) {
            dependency.routingId = NO_PRODUCTION_ROUTING_AVAILABLE;
            dependency.active = false;
            dependency.viableStep = false;
            return dependency;
        }
        dependency.routingId = routing.getId();
        dependency.active = routing.getAtivo();
        dependency.viableStep = dependency.active;
        if (!omitResources) {
            expandRouting(dependency, routing, supplyNetworkProjection);
        }
        return dependency;

    }

    private static BillOfMaterialsDependencyDTO createBomDependency(
            @Nullable ListaTecnica bom,
            VersaoMalha supplyNetwork,
            int currentDepth,
            SupplyNetworkProjection supplyNetworkProjection,
            int maximumTreeDepth) {

        BillOfMaterialsDependencyDTO dependency = new BillOfMaterialsDependencyDTO();
        dependency.elementType = SupplyNetworkDependencyDTO.ElementType.BILL_OF_MATERIALS;
        if (bom == null) {
            dependency.bomId = NO_BILL_OF_MATERIALS_AVAILABLE;
            dependency.active = false;
            dependency.viableStep = false;
            return dependency;
        }
        dependency.bomId = bom.getId();
        dependency.active = bom.getAtivo();
        dependency.viableStep = dependency.active;
        expandBom(dependency, supplyNetwork, bom, currentDepth, supplyNetworkProjection, maximumTreeDepth);
        return dependency;

    }

    private static void expandRouting(
            RoutingDependencyDTO dependency,
            Roteiro routing,
            SupplyNetworkProjection supplyNetworkProjection) {

        Roteiro routingWithResources = supplyNetworkProjection.getRoteiroFromId(routing.getId()).orElseThrow();
        for (RecursoProdutivo resource : routingWithResources.getRecursoProdutivoSet()) {
            ProductionResourceDependencyDTO resourceDependency = new ProductionResourceDependencyDTO();
            resourceDependency.elementType = SupplyNetworkDependencyDTO.ElementType.PRODUCTION_RESOURCE;
            resourceDependency.productionResourceId = resource.getId();
            resourceDependency.active = resource.getAtivo();
            resourceDependency.viableStep = resourceDependency.active;
            dependency.productionResourceDependencies.add(resourceDependency);
            if (!resourceDependency.viableStep) {
                dependency.viableStep = false;
            }
        }

    }

    private static void expandBom(
            BillOfMaterialsDependencyDTO dependency,
            VersaoMalha supplyNetwork,
            ListaTecnica bom,
            int currentDepth,
            SupplyNetworkProjection supplyNetworkProjection,
            int maximumTreeDepth) {

        ClusterEParametrosProjection parametersProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        ListaTecnica bomWithComponents = supplyNetworkProjection.getListaTecnicaFromId(bom.getId()).orElseThrow();
        for (ListaTecnicaComponente component : bomWithComponents.getListaTecnicaComponenteSet()) {
            Produto material = component.getMaterial();
            /*
             * A component is scoped by its parent BOM. It intentionally has
             * no independent location column, so the dependency must inherit
             * the location from the fully materialized BOM in the projection.
             */
            Location location = bomWithComponents.getLocation();
            MaterialLocationDependencyDTO componentDependency = createMaterialLocationDependency(
                    location,
                    material,
                    parametersProjection.isDfuAtiva(material, location),
                    currentDepth + 1);
            dependency.bomComponentDependencies.add(componentDependency);
            expandMaterialLocation(
                    componentDependency,
                    supplyNetwork,
                    location,
                    material,
                    supplyNetworkProjection,
                    maximumTreeDepth);
            if (!componentDependency.viableStep) {
                dependency.viableStep = false;
            }
        }

    }

    private static void expandTransportationLine(
            TransportationLineDependencyDTO dependency,
            VersaoMalha supplyNetwork,
            Location origin,
            Produto material,
            int currentDepth,
            SupplyNetworkProjection supplyNetworkProjection,
            int maximumTreeDepth) {

        ClusterEParametrosProjection parametersProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        MaterialLocationDependencyDTO originDependency = createMaterialLocationDependency(
                origin,
                material,
                parametersProjection.isDfuAtiva(material, origin),
                currentDepth + 1);
        dependency.materialAtOriginLocationDependency = originDependency;
        expandMaterialLocation(
                originDependency,
                supplyNetwork,
                origin,
                material,
                supplyNetworkProjection,
                maximumTreeDepth);
        if (!originDependency.viableStep) {
            dependency.viableStep = false;
        }

    }

    private static MaterialLocationDependencyDTO createMaterialLocationDependency(
            Location location,
            Produto material,
            boolean active,
            int depth) {

        MaterialLocationDependencyDTO dependency = new MaterialLocationDependencyDTO();
        dependency.elementType = SupplyNetworkDependencyDTO.ElementType.MATERIAL_LOCATION;
        dependency.locationId = location.getId();
        dependency.materialId = material.getId();
        dependency.active = active;
        dependency.viableStep = active;
        dependency.depth = depth;
        dependency.recursionCut = false;
        return dependency;

    }

    private static List<Pair<Roteiro, ListaTecnica>> getFocusedRoutingBomCombinations(
            ProductionVersionDependencyDTO dependency,
            VersaoProducao productionVersion,
            Produto focusedMaterial) {

        List<Pair<Roteiro, ListaTecnica>> combinations = productionVersion.getCombinacoesRoteiroListaTecnica();
        if (combinations.size() <= 1) {
            return combinations;
        }
        List<Pair<Roteiro, ListaTecnica>> focusedCombinations = combinations.stream()
                .filter(combination -> getCombinationOutputMaterial(combination).equals(focusedMaterial))
                .toList();
        if (focusedCombinations.isEmpty()) {
            throw new IllegalStateException("Production version " + productionVersion.getId()
                    + " does not contain focused output material " + focusedMaterial.getId());
        }
        int omittedCount = combinations.size() - focusedCombinations.size();
        dependency.parallelRoutingsOmitted = omittedCount > 0;
        dependency.omittedParallelRoutingCount = omittedCount;
        return focusedCombinations;

    }

    private static Produto getCombinationOutputMaterial(Pair<Roteiro, ListaTecnica> combination) {

        Roteiro routing = combination.getValue0();
        ListaTecnica bom = combination.getValue1();
        if (!routing.getMaterialOutput().equals(bom.getMaterialOutput())) {
            throw new IllegalStateException("Routing material " + routing.getMaterialOutput().getId()
                    + " different than BOM output material " + bom.getMaterialOutput().getId());
        }
        return routing.getMaterialOutput();

    }
}
