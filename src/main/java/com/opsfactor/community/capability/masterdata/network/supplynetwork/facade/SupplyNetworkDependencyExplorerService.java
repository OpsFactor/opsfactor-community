package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.*;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.routing.domain.RoteiroMultiplo;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaMultiplo;
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
import java.util.*;

/**
 * Inspeciona dependências estruturais sobre um único snapshot de malha por requisição.
 *
 * <p>O grafo lógico tem identidade material/location e alternativas OR com requisitos
 * AND. Sua viabilidade é resolvida antes da exibição; atividade local, profundidade
 * solicitada e presença de um ciclo nunca constituem prova de abastecimento.</p>
 *
 * <p>A resposta é uma projeção visual BFS limitada: a ocorrência de menor profundidade
 * representa o nó canônico, e as demais apontam para ele por ID. Os DTOs não possuem
 * referências Java a ancestrais. Cadastros inviáveis continuam visíveis para diagnóstico,
 * inclusive quando outra alternativa torna o material/location viável.</p>
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

    /**
     * Seleciona um material ou todos os materiais ativos da localização.
     * A profundidade solicitada limita apenas a exibição sobre o snapshot em cache,
     * evitando consultas por entidade durante a travessia e preservando os gates do planner.
     */
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

        Set<Produto> materials = selectedMaterial == null
                ? supplyNetworkProjection.getClusterEParametrosProjection().getMateriaisAtivosEmLocation(location) : Set.of(selectedMaterial);
        return new ExplorerSession(supplyNetwork, supplyNetworkProjection, maximumTreeDepth).explore(location, materials);

    }

    /**
     * Monta um descritor local de um nível, com gates de produção/transporte e filhos
     * ainda não expandidos. Os booleanos locais são substituídos pelo resultado do
     * motor após a descoberta; não devem ser usados como prova da cadeia completa.
     */
    private static void expandMaterialLocation(
            MaterialLocationDependencyDTO dependency,
            VersaoMalha supplyNetwork,
            Location location,
            Produto material,
            SupplyNetworkProjection supplyNetworkProjection, Runnable checkBudget) {

        checkBudget.run();

        dependency.recursionCut = false;
        dependency.viableProduction = false;
        dependency.viableInbound = false;


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
                        supplyNetworkProjection, checkBudget);
            }
            for (VersaoProducao productionVersion : productionVersions) {
                checkBudget.run();
                ProductionVersionDependencyDTO productionVersionDependency = new ProductionVersionDependencyDTO();
                productionVersionDependency.elementType = SupplyNetworkDependencyDTO.ElementType.PRODUCTION_VERSION;
                productionVersionDependency.productionVersionId = productionVersion.getId();
                productionVersionDependency.active = productionVersion.getAtivo();
                productionVersionDependency.parallelRoutingsOmitted = false;
                productionVersionDependency.omittedParallelRoutingCount = 0;
                productionVersionDependency.viableStep = productionVersionDependency.active
                        && !productionVersion.getCombinacoesRoteiroListaTecnica().isEmpty();
                expandProductionVersion(
                        productionVersionDependency,
                        supplyNetwork,
                        productionVersion,
                        material,
                        dependency.depth,
                        supplyNetworkProjection, checkBudget);
                applyVersionOutputGates(productionVersionDependency, productionVersion, location, supplyNetworkProjection, checkBudget);
                dependency.productionVersionDependencies.add(productionVersionDependency);
            }
        }

        LocalDateTime materialStatusReference = LocalDateTime.now();
        for (LinhaTransporte transportationLine : supplyNetworkProjection.getLinhasTransporte(
                supplyNetwork, null, Set.of(location))) {
            checkBudget.run();
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
                    supplyNetworkProjection, checkBudget);
            dependency.inboundTransportationLineDependencies.add(transportationDependency);
        }

        dependency.viableProduction = dependency.productionVersionDependencies.stream()
                .anyMatch(productionVersion -> productionVersion.viableStep);
        dependency.viableInbound = dependency.inboundTransportationLineDependencies.stream()
                .anyMatch(transportationLine -> transportationLine.viableStep);
        dependency.viableStep = dependency.active && (dependency.viableProduction || dependency.viableInbound);

    }

    /**
     * Preserva roteiro/LT incompletos como diagnóstico, sem criar uma fonte produtiva.
     * A versão apenas visual fica inelegível, mesmo quando seus registros são ativos.
     */
    private static void addTemporaryProductionVersionForIncompleteRegistration(
            MaterialLocationDependencyDTO materialLocationDependency,
            VersaoMalha supplyNetwork,
            Location location,
            Produto material,
            SupplyNetworkProjection supplyNetworkProjection, Runnable checkBudget) {

        checkBudget.run();

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
                checkBudget.run();
                temporaryVersion.routingAndBomCombinationDependencies.add(createRoutingBomCombination(
                        null, bom, supplyNetwork, materialLocationDependency.depth, supplyNetworkProjection, false, checkBudget));
            }
        } else if (billsOfMaterials.isEmpty()) {
            for (Roteiro routing : routings) {
                checkBudget.run();
                temporaryVersion.routingAndBomCombinationDependencies.add(createRoutingBomCombination(
                        routing, null, supplyNetwork, materialLocationDependency.depth, supplyNetworkProjection, false, checkBudget));
            }
        } else {
            for (Roteiro routing : routings) {
                checkBudget.run();
                for (ListaTecnica bom : billsOfMaterials) {
                    checkBudget.run();
                    temporaryVersion.routingAndBomCombinationDependencies.add(createRoutingBomCombination(
                            routing, bom, supplyNetwork, materialLocationDependency.depth, supplyNetworkProjection, false, checkBudget));
                }
            }
        }

        temporaryVersion.viableStep = false;
        temporaryVersion.localBlockingReasons.add("No eligible production version links these registrations");
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

    /** Retains every joint production branch; all requirements belong to one AND alternative. */
    private static void expandProductionVersion(
            ProductionVersionDependencyDTO dependency,
            VersaoMalha supplyNetwork,
            VersaoProducao productionVersion,
            Produto focusedMaterial,
            int currentDepth,
            SupplyNetworkProjection supplyNetworkProjection, Runnable checkBudget) {

        checkBudget.run();

        List<Pair<Roteiro, ListaTecnica>> combinations = getFocusedRoutingBomCombinations(
                dependency, productionVersion, focusedMaterial);
        for (Pair<Roteiro, ListaTecnica> combination : combinations) {
            checkBudget.run();
            RoutingBomCombinationDependencyDTO combinationDependency = createRoutingBomCombination(
                    combination.getValue0(),
                    combination.getValue1(),
                    supplyNetwork,
                    currentDepth,
                    supplyNetworkProjection,
                    Boolean.TRUE.equals(dependency.parallelRoutingsOmitted), checkBudget);
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
            boolean omitRoutingResources, Runnable checkBudget) {

        checkBudget.run();

        RoutingBomCombinationDependencyDTO dependency = new RoutingBomCombinationDependencyDTO();
        dependency.elementType = SupplyNetworkDependencyDTO.ElementType.ROUTING_BOM_COMBINATION;
        dependency.viableStep = true;
        dependency.parallelRoutingsOmitted = omitRoutingResources;
        dependency.omittedParallelRoutingCount = omitRoutingResources ? 1 : 0;
        dependency.routingDependency = createRoutingDependency(routing, supplyNetworkProjection, omitRoutingResources, checkBudget);
        dependency.bomDependency = createBomDependency(
                bom, supplyNetwork, currentDepth, supplyNetworkProjection, checkBudget);
        // Every retained joint branch contributes its routing and BOM requirements.
        if ((!omitRoutingResources && !dependency.routingDependency.viableStep)
                || !dependency.bomDependency.viableStep) {
            dependency.viableStep = false;
        }
        if (routing != null && bom != null) {
            applyJointOutputGates(dependency.routingDependency, dependency.bomDependency, routing, bom, supplyNetworkProjection, checkBudget);
            if (!dependency.routingDependency.viableStep || !dependency.bomDependency.viableStep) dependency.viableStep = false;
        }
        return dependency;

    }

    private static RoutingDependencyDTO createRoutingDependency(
            @Nullable Roteiro routing,
            SupplyNetworkProjection supplyNetworkProjection,
            boolean omitResources, Runnable checkBudget) {

        checkBudget.run();

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
            expandRouting(dependency, routing, supplyNetworkProjection, checkBudget);
        }
        return dependency;

    }

    /**
     * Distingue LT ausente, explicitamente bloqueada, de LT cadastrada sem componentes.
     * Uma LT vazia válida tem AND sem dependências; a alternativa ainda exige todos
     * os demais gates de versão, roteiro, recursos e material/location.
     */
    private static BillOfMaterialsDependencyDTO createBomDependency(
            @Nullable ListaTecnica bom,
            VersaoMalha supplyNetwork,
            int currentDepth,
            SupplyNetworkProjection supplyNetworkProjection, Runnable checkBudget) {

        checkBudget.run();

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
        expandBom(dependency, supplyNetwork, bom, currentDepth, supplyNetworkProjection, checkBudget);
        return dependency;

    }

    private static void expandRouting(
            RoutingDependencyDTO dependency,
            Roteiro routing,
            SupplyNetworkProjection supplyNetworkProjection, Runnable checkBudget) {

        checkBudget.run();

        Roteiro routingWithResources = supplyNetworkProjection.getRoteiroFromId(routing.getId()).orElseThrow();
        for (RecursoProdutivo resource : supplyNetworkProjection.getRecursosProdutivos(routingWithResources)) {
            checkBudget.run();
            ProductionResourceDependencyDTO resourceDependency = new ProductionResourceDependencyDTO();
            resourceDependency.elementType = SupplyNetworkDependencyDTO.ElementType.PRODUCTION_RESOURCE;
            resourceDependency.productionResourceId = resource.getId();
            resourceDependency.active = resource.getAtivo();
            resourceDependency.viableStep = resourceDependency.active
                    && supplyNetworkProjection.getClusterEParametrosProjection()
                    .getLocationPersistida(resource.getLocation().getId()).getAtivo();
            if (!resourceDependency.viableStep)
                resourceDependency.localBlockingReasons.add("Inactive production resource or resource location");
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
            SupplyNetworkProjection supplyNetworkProjection, Runnable checkBudget) {

        checkBudget.run();

        ListaTecnica snapshotBom = supplyNetworkProjection.getListaTecnicaFromId(bom.getId()).orElseThrow();
        ClusterEParametrosProjection parameters = supplyNetworkProjection.getClusterEParametrosProjection();
        // Indexed inputs avoid lazy entity navigation and correctly include multi-output BOMs.
        for (Produto material : supplyNetworkProjection.getMateriaisInput(snapshotBom)) {
            checkBudget.run();
            Location location = snapshotBom.getLocation();
            MaterialLocationDependencyDTO child = createMaterialLocationDependency(location, material,
                    parameters.isDfuAtiva(material, location), currentDepth + 1);
            dependency.bomComponentDependencies.add(child);
            if (!child.viableStep) dependency.viableStep = false;
        }

    }

    private static void expandTransportationLine(
            TransportationLineDependencyDTO dependency,
            VersaoMalha supplyNetwork,
            Location origin,
            Produto material,
            int currentDepth,
            SupplyNetworkProjection supplyNetworkProjection, Runnable checkBudget) {

        checkBudget.run();

        ClusterEParametrosProjection parametersProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        MaterialLocationDependencyDTO originDependency = createMaterialLocationDependency(
                origin,
                material,
                parametersProjection.isDfuAtiva(material, origin),
                currentDepth + 1);
        dependency.materialAtOriginLocationDependency = originDependency;
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

        dependency.parallelRoutingsOmitted = false;
        dependency.omittedParallelRoutingCount = 0;
        return productionVersion.getCombinacoesRoteiroListaTecnica();

    }



    /** Joint outputs must be active at the execution DFU, independent of component location. */
    private static void applyVersionOutputGates(ProductionVersionDependencyDTO dependency, VersaoProducao version,
            Location executionLocation, SupplyNetworkProjection projection, Runnable checkBudget) {

        for (var combination : version.getCombinacoesRoteiroListaTecnica()) {
            checkBudget.run();
            Set<Produto> outputs = new HashSet<>(combination.getValue0().getMateriaisOutput());
            outputs.addAll(combination.getValue1().getMateriaisOutput());
            for (Produto output : outputs) {
                checkBudget.run();
                if (!projection.getClusterEParametrosProjection().isDfuAtiva(output, executionLocation)) {
                    dependency.viableStep = false;
                    dependency.localBlockingReasons.add("Inactive joint output at execution location: " + output.getId());
                }
            }
        }

    }

    /** Co-products gate the same execution; they are not upstream supply dependencies. */
    private static void applyJointOutputGates(RoutingDependencyDTO routingDependency, BillOfMaterialsDependencyDTO bomDependency,
            Roteiro routing, ListaTecnica bom, SupplyNetworkProjection projection, Runnable checkBudget) {

        checkBudget.run();

        if (routing instanceof RoteiroMultiplo
                && (routing.getMateriaisOutput().size() <= 1 || projection.getOperacoesRoteiro(routing).isEmpty())) {
            routingDependency.viableStep = false;
            routingDependency.localBlockingReasons.add("Multiple-output routing requires outputs and operations");
        }
        if (bom instanceof ListaTecnicaMultiplo && bom.getMateriaisOutput().size() <= 1) {
            bomDependency.viableStep = false;
            bomDependency.localBlockingReasons.add("Multiple-output BOM requires multiple outputs");
        }

    }

    /** Deterministic test seam; production requests use the bounded default policy. */
    List<SupplyNetworkDependencyDTO> getDependencies(String networkId, String locationId, String materialId, int depth,
            SupplyNetworkViabilityGraph.Limits limits) {

        SupplyNetworkProjection projection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        VersaoMalha network = projection.getVersaoMalhaDeId(networkId).orElseThrow();
        ClusterEParametrosProjection parameters = projection.getClusterEParametrosProjection();
        Location location = parameters.getLocationPersistida(locationId);
        return new ExplorerSession(network, projection, depth, limits).explore(location,
                materialId == null ? parameters.getMateriaisAtivosEmLocation(location)
                        : Set.of(parameters.getMaterialPersistido(materialId)));

    }

    /**
     * Identidade por IDs literais, sem depender de proxies JPA ou do caminho de chegada.
     * O ID com comprimentos explícitos evita colisões ao concatenar material e localização.
     */
    private record MaterialLocationKey(String materialId, String locationId) {

        private MaterialLocationKey {

            Objects.requireNonNull(materialId);
            Objects.requireNonNull(locationId);

        }

        String id() {

            return "material:" + materialId.length() + ":" + materialId + ":location:" + locationId.length() + ":" + locationId;

        }

    }

    private record DisplayWork(MaterialLocationDependencyDTO occurrence, MaterialLocationKey parent) { }

    /**
     * Mantém duas fases independentes por requisição: análise do grafo e projeção visual.
     *
     * <p>O ponto fixo do motor exige uma fonte válida para liberar dependências AND/OR;
     * um grupo circular fechado não gera sua própria viabilidade. A descoberta possui
     * orçamento de nós, incidências e tempo, distinto dos limites de DTOs e profundidade.</p>
     *
     * <p>A projeção reserva os filhos em ordem BFS antes da próxima camada, garantindo
     * expansão canônica na menor profundidade. Referências repetidas encerram o ramo,
     * mas conservam o status lógico e os defeitos locais do material/location.</p>
     */
    private static final class ExplorerSession {

        // Limites de serialização: contam também estágios de produção, recursos e referências.
        // O limite de profundidade protege o JSON aninhado; não alimenta o motor de viabilidade.
        private static final int MAX_DISPLAY_NODES = 2_200;
        private static final int MAX_DISPLAY_EDGES = 3_400;
        private static final int MAX_DISPLAY_DEPTH = 32;
        private final SupplyNetworkViabilityGraph.Limits limits;
        private final Runnable checkBudget = this::checkDescriptorBudget;
        private final long descriptorStarted = System.nanoTime();
        private int remainingInspections = 100_000;
        private final VersaoMalha network;
        private final SupplyNetworkProjection projection;
        private final ClusterEParametrosProjection parameters;
        private final int requestedDepth;
        private final int displayDepth;
        private final LocalDateTime referenceDate = LocalDateTime.now();
        private final Map<MaterialLocationKey, MaterialLocationDependencyDTO> templates = new LinkedHashMap<>();
        private final Map<MaterialLocationKey, Location> locations = new HashMap<>();
        private final Map<MaterialLocationKey, Produto> materials = new HashMap<>();
        private SupplyNetworkViabilityGraph.Analysis<MaterialLocationKey> analysis;
        private boolean displayTruncated;
        private int requestedRoots;
        private int returnedRoots;

        ExplorerSession(VersaoMalha network, SupplyNetworkProjection projection, int requestedDepth) {

            this(network, projection, requestedDepth, new SupplyNetworkViabilityGraph.Limits(10_000, 100_000, 10_000));

        }

        ExplorerSession(VersaoMalha network, SupplyNetworkProjection projection, int requestedDepth,
                        SupplyNetworkViabilityGraph.Limits limits) {

            if (requestedDepth < 0) throw new IllegalArgumentException("Explorer depth must be non-negative");
            this.network = network;
            this.projection = projection;
            this.parameters = projection.getClusterEParametrosProjection();
            this.requestedDepth = requestedDepth;
            this.displayDepth = Math.min(requestedDepth, MAX_DISPLAY_DEPTH);
            this.limits = limits;

        }

        List<SupplyNetworkDependencyDTO> explore(Location location, Collection<Produto> selected) {

            List<MaterialLocationKey> roots = selected.stream().sorted(Comparator.comparing(Produto::getId))
                    .map(material -> {
                        MaterialLocationKey key = new MaterialLocationKey(material.getId(), location.getId());
                        locations.put(key, location);
                        materials.put(key, material);
                        return key;
                    }).toList();
            requestedRoots = roots.size();
            analysis = SupplyNetworkViabilityGraph.analyze(roots, this::loadNode, limits);
            // Status annotation is shallow: templates contain only local stages and child stubs.
            templates.forEach(this::annotateTemplate);
            Map<MaterialLocationKey, MaterialLocationDependencyDTO> represented = new LinkedHashMap<>();
            Map<MaterialLocationKey, MaterialLocationKey> parents = new HashMap<>();
            ArrayDeque<DisplayWork> pending = new ArrayDeque<>();
            List<SupplyNetworkDependencyDTO> result = new ArrayList<>();
            for (MaterialLocationKey root : roots) {
                if (result.size() >= MAX_DISPLAY_NODES) { displayTruncated = true; break; }
                MaterialLocationDependencyDTO occurrence = stub(root, 0);
                represented.put(root, occurrence);
                pending.add(new DisplayWork(occurrence, null));
                result.add(occurrence);
            }
            returnedRoots = result.size();
            int displayedNodes = result.size();
            int displayedEdges = 0;
            while (!pending.isEmpty()) {
                DisplayWork work = pending.remove();
                MaterialLocationDependencyDTO occurrence = work.occurrence();
                MaterialLocationKey key = key(occurrence);
                MaterialLocationDependencyDTO template = templates.get(key);
                if (occurrence.depth >= displayDepth) {
                    cut(occurrence, "DEPTH_LIMIT", "Visual depth limit - not expanded");
                    displayTruncated = true;
                    continue;
                }
                if (template == null || !analysis.nodes().containsKey(key)) {
                    cut(occurrence, "DISPLAY_LIMIT", "Analysis incomplete - not expanded");
                    occurrence.cutReason = analysis.cutReason();
                    displayTruncated = true;
                    continue;
                }
                int addedNodes = localSize(template) - 1;
                if (displayedNodes + addedNodes > MAX_DISPLAY_NODES || displayedEdges + addedNodes > MAX_DISPLAY_EDGES) {
                    cut(occurrence, "DISPLAY_LIMIT", "Display size limit - not expanded");
                    displayTruncated = true;
                    continue;
                }
                displayedNodes += addedNodes;
                displayedEdges += addedNodes;
                occurrence.productionVersionDependencies = template.productionVersionDependencies;
                occurrence.inboundTransportationLineDependencies = template.inboundTransportationLineDependencies;
                // Reserva antes da próxima camada: nenhuma ocorrência profunda toma o lugar
                // de uma ocorrência mais rasa do mesmo material/location.
                for (MaterialLocationDependencyDTO child : children(occurrence)) {
                    MaterialLocationKey target = key(child);
                    child.depth = occurrence.depth + 1;
                    if (represented.containsKey(target)) {
                        // SCC identifica também ciclos entre irmãos da árvore BFS (B->C e C->B).
                        // Uma ligação circular é topologia, não bloqueio automático; compartilhamento
                        // fora da SCC recebe marcador distinto e ambos usam referência terminal por ID.
                        boolean circular = analysis.isCircularEdge(key, target) || isAncestor(key, target, parents);
                        cut(child, circular ? "CYCLE_REFERENCE" : "SHARED_REFERENCE",
                                circular ? "Circular network - not expanded" : "Already represented - not expanded");
                        child.referenceNodeId = target.id();
                    } else {
                        represented.put(target, child);
                        parents.put(target, key);
                        pending.add(new DisplayWork(child, key));
                    }
                }
            }
            // The response stays a finite nested DTO; references carry IDs, never ancestor objects.
            for (MaterialLocationDependencyDTO occurrence : represented.values()) metadata(occurrence);
            templates.values().forEach(template -> children(template).forEach(this::metadata));
            return result;

        }

        /**
         * Interrompe também loops dentro de um único DFU, antes de alocar um descritor enorme.
         * Só a exceção de orçamento vira análise parcial; inconsistências de cadastro não
         * são engolidas. O motor conserva provas concluídas e sinaliza o restante como UNKNOWN.
         */
        private void checkDescriptorBudget() {

            if (Thread.currentThread().isInterrupted()) throw new SupplyNetworkViabilityGraph.DiscoveryLimitException("CANCELLED");
            if ((System.nanoTime() - descriptorStarted) / 1_000_000L >= limits.maxMillis())
                throw new SupplyNetworkViabilityGraph.DiscoveryLimitException("TIME_LIMIT");
            if (--remainingInspections < 0) throw new SupplyNetworkViabilityGraph.DiscoveryLimitException("LOCAL_WORK_LIMIT");

        }

        /**
         * Adapta um descritor ao motor: versões e inbound são alternativas OR; todos os
         * componentes das combinações de uma versão pertencem à mesma alternativa AND.
         * Gates locais falsos mantêm a alternativa inelegível mesmo com filhos viáveis.
         * Uma fonte sem inputs só libera o nó quando sua alternativa é elegível.
         */
        private SupplyNetworkViabilityGraph.Node<MaterialLocationKey> loadNode(MaterialLocationKey key) {

            checkBudget.run();
            Location location = locations.computeIfAbsent(key, ignored -> parameters.getLocationPersistida(key.locationId()));
            Produto material = materials.computeIfAbsent(key, ignored -> parameters.getMaterialPersistido(key.materialId()));
            Objects.requireNonNull(location, "Explorer dependency location not in the snapshot");
            Objects.requireNonNull(material, "Explorer dependency material not in the snapshot");
            MaterialLocationDependencyDTO template = new MaterialLocationDependencyDTO();
            template.elementType = SupplyNetworkDependencyDTO.ElementType.MATERIAL_LOCATION;
            template.materialId = key.materialId();
            template.locationId = key.locationId();
            template.active = parameters.isDfuAtiva(material, location);
            template.depth = 0;
            expandMaterialLocation(template, network, location, material, projection, checkBudget);
            if (!Boolean.TRUE.equals(template.active)) template.localBlockingReasons.add("Material inactive at location");
            if (template.productionVersionDependencies.isEmpty()) template.localBlockingReasons.add("No production routing or BOM alternative");
            templates.put(key, template);
            List<SupplyNetworkViabilityGraph.Alternative<MaterialLocationKey>> alternatives = new ArrayList<>();
            for (ProductionVersionDependencyDTO version : template.productionVersionDependencies) {
                version.alternativeId = productionId(version);
                List<MaterialLocationKey> inputs = version.routingAndBomCombinationDependencies.stream()
                        .flatMap(combination -> combination.bomDependency.bomComponentDependencies.stream()).map(ExplorerSession::key).toList();
                alternatives.add(new SupplyNetworkViabilityGraph.Alternative<>(version.alternativeId,
                        Boolean.TRUE.equals(template.active) && Boolean.TRUE.equals(version.viableStep), inputs));
            }
            for (TransportationLineDependencyDTO line : template.inboundTransportationLineDependencies) {
                line.alternativeId = "inbound:" + line.originLocationId.length() + ":" + line.originLocationId;
                // Evaluate against one captured reference time, never against per-node clock changes.
                Location origin = parameters.getLocationPersistida(line.originLocationId);
                if (origin != null) line.active = !projection.getLinhaTransporteInboundViavelSetParaLocationMaterial(
                        network, location, material, referenceDate, Set.of(origin)).isEmpty();
                alternatives.add(new SupplyNetworkViabilityGraph.Alternative<>(line.alternativeId,
                        Boolean.TRUE.equals(template.active) && Boolean.TRUE.equals(line.active), List.of(key(line.materialAtOriginLocationDependency))));
            }
            return new SupplyNetworkViabilityGraph.Node<>(false, alternatives);

        }

        private static String productionId(ProductionVersionDependencyDTO version) {

            String combinations = version.routingAndBomCombinationDependencies.stream()
                    .map(combination -> combination.routingDependency.routingId + "\u0000" + combination.bomDependency.bomId)
                    .sorted().map(value -> value.length() + ":" + value).collect(java.util.stream.Collectors.joining());
            String id = String.valueOf(version.productionVersionId);
            return "production:" + id.length() + ":" + id + ":" + combinations;

        }

        private void annotateTemplate(MaterialLocationKey key, MaterialLocationDependencyDTO template) {

            children(template).forEach(child -> annotateMaterial(child, key(child)));
            for (ProductionVersionDependencyDTO version : template.productionVersionDependencies) {
                for (RoutingBomCombinationDependencyDTO combination : version.routingAndBomCombinationDependencies) {
                    RoutingDependencyDTO routing = combination.routingDependency;
                    routing.productionResourceDependencies.forEach(resource -> status(resource,
                            Boolean.TRUE.equals(resource.viableStep) ? "VIABLE" : "BLOCKED"));
                    status(routing, andStatus(Boolean.TRUE.equals(routing.viableStep), routing.productionResourceDependencies));
                    BillOfMaterialsDependencyDTO bom = combination.bomDependency;
                    status(bom, andStatus(Boolean.TRUE.equals(bom.active) && bom.localBlockingReasons.isEmpty(), bom.bomComponentDependencies));
                    status(combination, andStatus(true, List.of(routing, bom)));
                }
                status(version, analysis.nodes().containsKey(key)
                        ? analysis.alternativeStatus(key, version.alternativeId).name() : "UNKNOWN");
            }
            template.inboundTransportationLineDependencies.forEach(line -> status(line,
                    analysis.nodes().containsKey(key) ? analysis.alternativeStatus(key, line.alternativeId).name() : "UNKNOWN"));
            annotateMaterial(template, key);

        }

        private void annotateMaterial(MaterialLocationDependencyDTO occurrence, MaterialLocationKey key) {

            status(occurrence, analysis.status(key).name());
            occurrence.nodeId = key.id();
            occurrence.cycleGroupId = analysis.cycleGroupId(key);
            occurrence.recursionCut = false;
            MaterialLocationDependencyDTO template = templates.get(key);
            if (template != null && template != occurrence) {
                occurrence.localBlockingReasons.clear();
                occurrence.localBlockingReasons.addAll(template.localBlockingReasons);
            }
            if (template == null || !analysis.nodes().containsKey(key)) {
                occurrence.viableProduction = null;
                occurrence.viableInbound = null;
            } else {
                occurrence.viableProduction = orValue(key, template.productionVersionDependencies);
                occurrence.viableInbound = orValue(key, template.inboundTransportationLineDependencies);
            }

        }

        private Boolean orValue(MaterialLocationKey key, Collection<? extends SupplyNetworkDependencyDTO> alternatives) {

            boolean unknown = false;
            for (SupplyNetworkDependencyDTO alternative : alternatives) {
                SupplyNetworkViabilityGraph.Status status = analysis.alternativeStatus(key, alternative.alternativeId);
                if (status == SupplyNetworkViabilityGraph.Status.VIABLE) return true;
                unknown |= status == SupplyNetworkViabilityGraph.Status.UNKNOWN;
            }
            return unknown ? null : false;

        }

        private MaterialLocationDependencyDTO stub(MaterialLocationKey key, int depth) {

            MaterialLocationDependencyDTO result = new MaterialLocationDependencyDTO();
            result.elementType = SupplyNetworkDependencyDTO.ElementType.MATERIAL_LOCATION;
            result.materialId = key.materialId();
            result.locationId = key.locationId();
            result.depth = depth;
            MaterialLocationDependencyDTO template = templates.get(key);
            result.active = template == null ? null : template.active;
            if (template != null) result.localBlockingReasons.addAll(template.localBlockingReasons);
            annotateMaterial(result, key);
            return result;

        }

        /**
         * Publica completude do cálculo separada do corte visual de cada ocorrência.
         * analysisCutReason explica PARTIAL; cutReason/expansionStatus explicam a exibição.
         * Uma análise completa pode ter exibição truncada sem mudar sua viabilidade.
         */
        private void metadata(MaterialLocationDependencyDTO occurrence) {

            occurrence.analysisStatus = analysis.complete() ? "COMPLETE" : "PARTIAL";
            occurrence.analysisCutReason = analysis.cutReason();
            occurrence.analysisTruncated = !analysis.complete();
            occurrence.displayTruncated = displayTruncated;
            occurrence.analyzedNodeCount = analysis.nodeCount();
            occurrence.analyzedEdgeCount = analysis.edgeCount();
            occurrence.maximumAnalysisNodes = limits.maxNodes();
            occurrence.maximumAnalysisEdges = limits.maxEdges();
            occurrence.maximumAnalysisMillis = limits.maxMillis();
            occurrence.maximumDisplayNodes = MAX_DISPLAY_NODES;
            occurrence.maximumDisplayEdges = MAX_DISPLAY_EDGES;
            occurrence.maximumDisplayDepth = MAX_DISPLAY_DEPTH;
            occurrence.requestedTreeDepth = requestedDepth;
            occurrence.effectiveTreeDepth = displayDepth;
            occurrence.requestedRootCount = requestedRoots;
            occurrence.returnedRootCount = returnedRoots;

        }

        private static MaterialLocationKey key(MaterialLocationDependencyDTO node) {

            return new MaterialLocationKey(node.materialId, node.locationId);

        }

        private static boolean isAncestor(MaterialLocationKey source, MaterialLocationKey target,
                                          Map<MaterialLocationKey, MaterialLocationKey> parents) {

            for (MaterialLocationKey current = source; current != null; current = parents.get(current)) {
                if (current.equals(target)) return true;
            }
            return false;

        }

        private static List<MaterialLocationDependencyDTO> children(MaterialLocationDependencyDTO node) {

            List<MaterialLocationDependencyDTO> children = new ArrayList<>();
            for (ProductionVersionDependencyDTO version : node.productionVersionDependencies) {
                for (RoutingBomCombinationDependencyDTO combination : version.routingAndBomCombinationDependencies) {
                    children.addAll(combination.bomDependency.bomComponentDependencies);
                }
            }
            for (TransportationLineDependencyDTO line : node.inboundTransportationLineDependencies) children.add(line.materialAtOriginLocationDependency);
            children.sort(Comparator.comparing((MaterialLocationDependencyDTO dependency) -> dependency.locationId).thenComparing(dependency -> dependency.materialId));
            return children;

        }

        private static int localSize(MaterialLocationDependencyDTO node) {

            int size = 1;
            for (ProductionVersionDependencyDTO version : node.productionVersionDependencies) {
                size++;
                for (RoutingBomCombinationDependencyDTO combination : version.routingAndBomCombinationDependencies) {
                    size += 3 + combination.routingDependency.productionResourceDependencies.size()
                            + combination.bomDependency.bomComponentDependencies.size();
                }
            }
            return size + 2 * node.inboundTransportationLineDependencies.size();

        }

        /** AND conservador: defeito comprovado bloqueia; pendência impede declarar viabilidade. */
        private static String andStatus(boolean locallyEligible, Collection<? extends SupplyNetworkDependencyDTO> children) {

            if (!locallyEligible) return "BLOCKED";
            boolean unknown = false;
            for (SupplyNetworkDependencyDTO child : children) {
                if ("BLOCKED".equals(child.viabilityStatus)) return "BLOCKED";
                unknown |= "UNKNOWN".equals(child.viabilityStatus);
            }
            return unknown ? "UNKNOWN" : "VIABLE";

        }

        /** Mantém o booleano legado trivalente: UNKNOWN é null, nunca true por omissão. */
        private static void status(SupplyNetworkDependencyDTO node, String status) {

            node.viabilityStatus = status;
            node.viableStep = "UNKNOWN".equals(status) ? null : "VIABLE".equals(status);
            node.expansionStatus = "COMPLETE";

        }

        /** Encerra somente a expansão visual, preservando status lógico e causas locais. */
        private static void cut(MaterialLocationDependencyDTO node, String expansion, String message) {

            node.expansionStatus = expansion;
            node.cutReason = expansion;
            node.inspectionMessage = message;
            node.recursionCut = true;

        }

    }

}
