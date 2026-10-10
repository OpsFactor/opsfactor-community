package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.*;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Counterexamples exercise the service boundary and its finite serialized contract. */
class SupplyNetworkDependencyExplorerServiceTest {

    @Test
    void closedCycleRemainsBlockedAtEveryDisplayDepth() throws Exception {

        Fixture fixture = new Fixture();
        fixture.lane("A", "B"); fixture.lane("B", "A");
        for (int depth : List.of(0, 1, 8)) {
            MaterialLocationDependencyDTO root = fixture.root(depth);
            assertEquals("BLOCKED", root.viabilityStatus);
            assertFalse(root.viableStep);
            assertEquals("COMPLETE", root.analysisStatus);
            assertNotNull(root.cycleGroupId);
            assertDoesNotThrow(() -> new ObjectMapper().writeValueAsString(root));
            if (depth == 0) assertEquals("DEPTH_LIMIT", root.expansionStatus);
            if (depth == 8) {
                MaterialLocationDependencyDTO returned = child(child(root, "B"), "A");
                assertEquals("CYCLE_REFERENCE", returned.expansionStatus);
                assertEquals(root.nodeId, returned.referenceNodeId);
                assertEquals("Circular network - not expanded", returned.inspectionMessage);
                assertTrue(returned.inboundTransportationLineDependencies.isEmpty());
            }
        }

    }

    @Test
    void independentSupplierMakesCycleViableWithoutHidingCircularMarker() {

        Fixture fixture = new Fixture();
        fixture.lane("A", "B"); fixture.lane("B", "A"); fixture.lane("A", "S");
        fixture.supplier("S");
        MaterialLocationDependencyDTO root = fixture.root(8);
        assertEquals("VIABLE", root.viabilityStatus);
        assertEquals("CYCLE_REFERENCE", child(child(root, "B"), "A").expansionStatus);

    }

    @Test
    void siblingCrossEdgesAreCircularReferencesInsteadOfSharedReferences() {

        Fixture fixture = new Fixture();
        fixture.lane("A", "B"); fixture.lane("A", "C");
        fixture.lane("B", "C"); fixture.lane("C", "B");
        MaterialLocationDependencyDTO root = fixture.root(8);
        MaterialLocationDependencyDTO b = child(root, "B"), c = child(root, "C");
        assertEquals(1, b.depth); assertEquals(1, c.depth);
        assertEquals("CYCLE_REFERENCE", child(b, "C").expansionStatus);
        assertEquals("CYCLE_REFERENCE", child(c, "B").expansionStatus);
        assertEquals(b.cycleGroupId, c.cycleGroupId);
        assertEquals("BLOCKED", root.viabilityStatus);

    }

    @Test
    void diamondDagReusesOneMinimumDepthRepresentation() {

        Fixture fixture = new Fixture();
        fixture.lane("A", "B"); fixture.lane("A", "C");
        fixture.lane("B", "S"); fixture.lane("C", "S"); fixture.supplier("S");
        MaterialLocationDependencyDTO root = fixture.root(8);
        List<MaterialLocationDependencyDTO> shared = List.of(child(child(root, "B"), "S"), child(child(root, "C"), "S"));
        assertEquals(1, shared.stream().filter(node -> "SHARED_REFERENCE".equals(node.expansionStatus)).count());
        assertTrue(shared.stream().allMatch(node -> node.depth == 2 && "VIABLE".equals(node.viabilityStatus)));
        assertTrue(shared.stream().allMatch(node -> node.cycleGroupId == null));

    }

    @Test
    void analysisBudgetReturnsUnknownWithoutFalseGreen() {

        Fixture fixture = new Fixture(); fixture.lane("A", "B"); fixture.supplier("B");
        MaterialLocationDependencyDTO root = (MaterialLocationDependencyDTO) fixture.service.getDependencies(
                "NETWORK", "A", "M", 4, new SupplyNetworkViabilityGraph.Limits(1, 100, 10000)).getFirst();
        assertEquals("UNKNOWN", root.viabilityStatus); assertNull(root.viableStep);
        assertEquals("PARTIAL", root.analysisStatus); assertTrue(root.analysisTruncated);
        assertEquals("NODE_LIMIT", root.analysisCutReason);
        assertEquals("COMPLETE", root.expansionStatus);
        assertEquals("UNKNOWN", child(root, "B").viabilityStatus);
        assertEquals("DISPLAY_LIMIT", child(root, "B").expansionStatus);

    }

    @Test
    void deepNetworkUsesBoundedDtoNestingAndFullStructuralAnalysis() throws Exception {

        Fixture fixture = new Fixture();
        String previous = "A";
        for (int index = 0; index < 180; index++) {
            String next = "L" + index; fixture.lane(previous, next); previous = next;
        }
        fixture.supplier(previous);
        MaterialLocationDependencyDTO root = fixture.root(10000);
        assertEquals("VIABLE", root.viabilityStatus);
        assertEquals(181, root.analyzedNodeCount);
        MaterialLocationDependencyDTO leaf = root;
        for (int level = 0; level < 32; level++) leaf = leaf.inboundTransportationLineDependencies.iterator().next().materialAtOriginLocationDependency;
        assertEquals("DEPTH_LIMIT", leaf.expansionStatus);
        assertTrue(root.displayTruncated); assertFalse(root.analysisTruncated);
        assertDoesNotThrow(() -> new ObjectMapper().writeValueAsString(root));

    }

    @Test
    void emptyBomIsValidButInactiveDfuResourceAndResourceLocationBlockProduction() {

        Fixture fixture = new Fixture();
        fixture.production("A", "PV", Set.of(), true, Set.of());
        assertEquals("VIABLE", fixture.root(1).viabilityStatus);
        when(fixture.parameters.isDfuAtiva(fixture.material, fixture.location("A"))).thenReturn(false);
        assertEquals("BLOCKED", fixture.root(1).viabilityStatus);
        when(fixture.parameters.isDfuAtiva(fixture.material, fixture.location("A"))).thenReturn(true);
        RecursoProdutivo resource = mock(RecursoProdutivo.class);
        when(resource.getId()).thenReturn("RESOURCE"); when(resource.getAtivo()).thenReturn(false);
        Location resourceLocation = fixture.location("RESOURCE-LOCATION");
        when(resource.getLocation()).thenReturn(resourceLocation);
        fixture.production("A", "RESOURCE-PV", Set.of(), true, Set.of(resource));
        assertEquals("BLOCKED", fixture.root(1).viabilityStatus);
        when(resource.getAtivo()).thenReturn(true);
        when(fixture.location("RESOURCE-LOCATION").getAtivo()).thenReturn(false);
        assertEquals("BLOCKED", fixture.root(1).viabilityStatus);

    }

    @Test
    void everyBomComponentIsRequiredAndAutoreferenceNeedsIndependentSource() {

        Fixture fixture = new Fixture();
        fixture.production("A", "SELF", Set.of(fixture.material), true, Set.of());
        assertEquals("BLOCKED", fixture.root(5).viabilityStatus);
        fixture.lane("A", "S"); fixture.supplier("S");
        assertEquals("VIABLE", fixture.root(5).viabilityStatus);
        Produto unavailable = fixture.otherMaterial("MISSING");
        fixture.production("A", "AND", Set.of(fixture.material, unavailable), true, Set.of());
        MaterialLocationDependencyDTO root = fixture.root(5);
        assertEquals("VIABLE", root.viabilityStatus);
        assertEquals("BLOCKED", root.productionVersionDependencies.iterator().next().viabilityStatus);

    }

    @Test
    void supplierWithInactiveProductionVersionDoesNotBecomeAnUnconditionalSeed() {

        Fixture fixture = new Fixture(); fixture.supplier("A");
        fixture.production("A", "INACTIVE", Set.of(), false, Set.of());
        assertEquals("BLOCKED", fixture.root(5).viabilityStatus);

    }

    @Test
    void absentProductionDoesNotPreventIndependentInboundSupply() {

        Fixture fixture = new Fixture(); fixture.lane("A", "S"); fixture.supplier("S");
        MaterialLocationDependencyDTO root = fixture.root(1);
        assertTrue(root.viableInbound); assertFalse(root.viableProduction); assertTrue(root.viableStep);

    }

    @Test
    void jointVersionRequiresEveryBranchAndActiveCoProduct() {

        Fixture fixture = new Fixture(); fixture.production("A", "JOINT", Set.of(), true, Set.of());
        VersaoProducao version = fixture.projection.getTodasVersoesProducao(fixture.location("A"), fixture.material, true, null).iterator().next();
        var original = version.getCombinacoesRoteiroListaTecnica().getFirst();
        Produto coProduct = fixture.otherMaterial("CO-PRODUCT");
        Roteiro otherRouting = mock(Roteiro.class); ListaTecnica otherBom = mock(ListaTecnica.class);
        when(otherRouting.getId()).thenReturn("OTHER-ROUTE"); when(otherRouting.getAtivo()).thenReturn(false);
        when(otherRouting.getMateriaisOutput()).thenReturn(Set.of(coProduct));
        when(otherBom.getId()).thenReturn("OTHER-BOM"); when(otherBom.getAtivo()).thenReturn(true);
        when(otherBom.getLocation()).thenReturn(fixture.location("A")); when(otherBom.getMateriaisOutput()).thenReturn(Set.of(coProduct));
        when(fixture.projection.getRoteiroFromId("OTHER-ROUTE")).thenReturn(Optional.of(otherRouting));
        when(fixture.projection.getListaTecnicaFromId("OTHER-BOM")).thenReturn(Optional.of(otherBom));
        when(version.getCombinacoesRoteiroListaTecnica()).thenReturn(List.of(original, Pair.with(otherRouting, otherBom)));
        MaterialLocationDependencyDTO root = fixture.root(2);
        assertEquals("BLOCKED", root.viabilityStatus);
        assertEquals(2, root.productionVersionDependencies.iterator().next().routingAndBomCombinationDependencies.size());
        assertFalse(root.productionVersionDependencies.iterator().next().parallelRoutingsOmitted);
        when(otherRouting.getAtivo()).thenReturn(true);
        assertEquals("VIABLE", fixture.root(2).viabilityStatus);
        when(fixture.parameters.isDfuAtiva(coProduct, fixture.location("A"))).thenReturn(false);
        root = fixture.root(2);
        assertEquals("BLOCKED", root.viabilityStatus);
        assertFalse(root.productionVersionDependencies.iterator().next().localBlockingReasons.isEmpty());

    }

    @Test
    void closedCycleRetainsMissingBomRegistrationForRepair() {

        Fixture fixture = new Fixture(); fixture.lane("A", "B"); fixture.lane("B", "A");
        Roteiro routing = mock(Roteiro.class); when(routing.getId()).thenReturn("NO-BOM-ROUTE"); when(routing.getAtivo()).thenReturn(true);
        when(fixture.projection.getTodosRoteiros(fixture.location("A"), fixture.material)).thenReturn(Set.of(routing));
        when(fixture.projection.getRoteiroFromId(routing.getId())).thenReturn(Optional.of(routing));
        MaterialLocationDependencyDTO root = fixture.root(5);
        assertTrue(child(root, "B").localBlockingReasons.contains("No production routing or BOM alternative"));
        assertEquals("BLOCKED", root.viabilityStatus);
        var version = root.productionVersionDependencies.iterator().next();
        assertEquals("BLOCKED", version.viabilityStatus);
        assertEquals("No Bill of Materials Available", version.routingAndBomCombinationDependencies.iterator().next().bomDependency.bomId);
        assertEquals("CYCLE_REFERENCE", child(child(root, "B"), "A").expansionStatus);

    }

    private static MaterialLocationDependencyDTO child(MaterialLocationDependencyDTO root, String id) {

        return root.inboundTransportationLineDependencies.stream().filter(line -> id.equals(line.originLocationId))
                .findFirst().orElseThrow().materialAtOriginLocationDependency;

    }

    /** Indexed projection fixture; no repositories or deployment data are involved. */
    private static final class Fixture {
        final SupplyNetworkProjectionFactory factory = mock(SupplyNetworkProjectionFactory.class);
        final SupplyNetworkProjection projection = mock(SupplyNetworkProjection.class);
        final ClusterEParametrosProjection parameters = mock(ClusterEParametrosProjection.class);
        final VersaoMalha network = mock(VersaoMalha.class);
        final Produto material = mock(Produto.class);
        final Map<String, Location> locations = new HashMap<>();
        final Map<Location, Set<LinhaTransporte>> inbound = new HashMap<>();
        final SupplyNetworkDependencyExplorerService service = new SupplyNetworkDependencyExplorerService(factory);

        Fixture() {

            when(factory.getSupplyNetworkProjectionCompletoDeCache()).thenReturn(projection);
            when(projection.getClusterEParametrosProjection()).thenReturn(parameters);
            when(projection.getVersaoMalhaDeId("NETWORK")).thenReturn(Optional.of(network));
            when(material.getId()).thenReturn("M"); when(parameters.getMaterialPersistido("M")).thenReturn(material);
            location("A");

        }

        Location location(String id) {

            return locations.computeIfAbsent(id, ignored -> {
                Location result = mock(Location.class);
                when(result.getId()).thenReturn(id); when(result.getAtivo()).thenReturn(true);
                when(result.getTipoLocation()).thenReturn(LocationAbstract.TipoLocation.INTERNA);
                when(parameters.getLocationPersistida(id)).thenReturn(result);
                when(parameters.isDfuAtiva(material, result)).thenReturn(true);
                Set<LinhaTransporte> lanes = new LinkedHashSet<>(); inbound.put(result, lanes);
                when(projection.getLinhasTransporte(network, null, Set.of(result))).thenReturn(lanes);
                return result;
            });

        }

        void supplier(String id) {

            when(location(id).getTipoLocation()).thenReturn(LocationAbstract.TipoLocation.FORNECEDOR);

        }

        void lane(String destination, String origin) {

            Location target = location(destination), source = location(origin);
            LinhaTransporte lane = mock(LinhaTransporte.class);
            when(lane.getLocationOrigem()).thenReturn(source); when(lane.getLocationDestino()).thenReturn(target);
            when(lane.getAtivo()).thenReturn(true); when(lane.getHabilitadoProdutosNaoCadastradosLinhaTransporte()).thenReturn(true);
            inbound.get(target).add(lane);
            when(projection.getLinhaTransporteInboundViavelSetParaLocationMaterial(eq(network), eq(target), eq(material), any(), eq(Set.of(source))))
                    .thenReturn(Set.of(lane));

        }

        Produto otherMaterial(String id) {

            Produto result = mock(Produto.class); when(result.getId()).thenReturn(id);
            when(parameters.getMaterialPersistido(id)).thenReturn(result);
            when(parameters.isDfuAtiva(result, location("A"))).thenReturn(true);
            return result;

        }

        void production(String locationId, String id, Set<Produto> inputs, boolean active, Set<RecursoProdutivo> resources) {

            Location site = location(locationId);
            VersaoProducao version = mock(VersaoProducao.class); Roteiro routing = mock(Roteiro.class); ListaTecnica bom = mock(ListaTecnica.class);
            when(version.getId()).thenReturn(id); when(version.getAtivo()).thenReturn(active);
            when(version.getCombinacoesRoteiroListaTecnica()).thenReturn(List.of(Pair.with(routing, bom)));
            when(routing.getId()).thenReturn(id + "-ROUTING"); when(routing.getAtivo()).thenReturn(true);
            when(routing.getLocation()).thenReturn(site); when(routing.getMateriaisOutput()).thenReturn(Set.of(material));
            when(routing.getRecursoProdutivoSet()).thenReturn(resources);
            when(bom.getId()).thenReturn(id + "-BOM"); when(bom.getAtivo()).thenReturn(true); when(bom.getLocation()).thenReturn(site);
            when(bom.getMateriaisOutput()).thenReturn(Set.of(material));
            when(projection.getRoteiroFromId(routing.getId())).thenReturn(Optional.of(routing));
            when(projection.getListaTecnicaFromId(bom.getId())).thenReturn(Optional.of(bom));
            when(projection.getRecursosProdutivos(routing)).thenReturn(resources);
            when(projection.getMateriaisInput(bom)).thenReturn(inputs);
            when(projection.getTodasVersoesProducao(site, material, true, null)).thenReturn(Set.of(version));

        }

        MaterialLocationDependencyDTO root(int depth) {

            return (MaterialLocationDependencyDTO) service.getDependencies("NETWORK", "A", "M", depth).getFirst();

        }
    }
}
