package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.SupplyNetworkViabilityGraph.Status.*;

/** Counterexamples that distinguish supply proof from graph reachability/cycle topology. */
class SupplyNetworkViabilityGraphTest {

    @Test
    void closedCycleAndSelfLoopNeverCreateSupply() {

        var result = solve(Map.of("A", node(alternative("ab", "B")), "B", node(alternative("ba", "A")),
                "SELF", node(alternative("self", "SELF"))), List.of("A", "SELF"));
        assertEquals(BLOCKED, result.status("A"));
        assertEquals(BLOCKED, result.status("B"));
        assertEquals(BLOCKED, result.status("SELF"));
        assertTrue(result.isCircularEdge("A", "B"));
        assertNotNull(result.cycleGroupId("SELF"));

    }

    @Test
    void externalInputDoesNotTurnAndSccIntoOr() {

        var result = solve(Map.of("A", node(alternative("both", "B", "C")),
                "B", node(alternative("back", "A")), "C", source()), List.of("A"));
        assertEquals(VIABLE, result.status("C"));
        assertEquals(BLOCKED, result.status("A"));
        assertEquals(BLOCKED, result.status("B"));
        assertEquals(BLOCKED, result.alternativeStatus("A", "both"));

    }

    @Test
    void independentSourceResolvesAllRootsAndWitnessesStayAcyclic() {

        Map<String, SupplyNetworkViabilityGraph.Node<String>> graph = Map.of(
                "A", node(alternative("cycle", "B"), alternative("source", "S")),
                "B", node(alternative("back", "A")), "S", source());
        for (List<String> roots : List.of(List.of("A", "B"), List.of("B", "A"))) {
            var result = solve(graph, roots);
            assertEquals(VIABLE, result.status("A"));
            assertEquals(VIABLE, result.status("B"));
            assertEquals("source", result.witness("A").id());
            assertEquals("back", result.witness("B").id());
            assertEquals(VIABLE, result.alternativeStatus("A", "cycle"));
            assertTrue(result.isCircularEdge("A", "B"));
        }

    }

    @Test
    void siblingCrossCycleIsDistinctFromSharedDag() {

        var result = solve(Map.of("A", node(alternative("root", "B", "C")),
                "B", node(alternative("bc", "C")), "C", node(alternative("cb", "B")),
                "D", node(alternative("shared", "B"))), List.of("A", "D"));
        assertTrue(result.isCircularEdge("B", "C"));
        assertTrue(result.isCircularEdge("C", "B"));
        assertFalse(result.isCircularEdge("A", "B"));
        assertFalse(result.isCircularEdge("D", "B"));

    }

    @Test
    void emptyBomNeedsEligibleLocalGatesAndDuplicateComponentsCountOnce() {

        var result = solve(Map.of("VALID", node(alternative("empty")),
                "INVALID", node(new SupplyNetworkViabilityGraph.Alternative<>("invalid", false, List.of())),
                "DUPLICATE", node(alternative("twice", "VALID", "VALID"))), List.of("VALID", "INVALID", "DUPLICATE"));
        assertEquals(VIABLE, result.status("VALID"));
        assertEquals(BLOCKED, result.status("INVALID"));
        assertEquals(VIABLE, result.status("DUPLICATE"));

    }

    @Test
    void partialDiscoveryRetainsSupplyAndLocalBlockProofWithoutInventingUnknown() {

        Map<String, SupplyNetworkViabilityGraph.Node<String>> graph = Map.of(
                "SOURCE", source(), "LOCAL_BLOCK", node(new SupplyNetworkViabilityGraph.Alternative<>("inactive", false, List.of())),
                "ROOT", node(alternative("unopened", "MISSING")));
        var result = SupplyNetworkViabilityGraph.analyze(List.of("SOURCE", "LOCAL_BLOCK", "ROOT"), graph::get,
                new SupplyNetworkViabilityGraph.Limits(3, 100, 5000));
        assertFalse(result.complete());
        assertEquals("NODE_LIMIT", result.cutReason());
        assertEquals(VIABLE, result.status("SOURCE"));
        assertEquals(BLOCKED, result.status("LOCAL_BLOCK"));
        assertEquals(UNKNOWN, result.status("ROOT"));
        assertEquals(UNKNOWN, result.status("MISSING"));

    }

    @Test
    void denseGraphLoadsEachDescriptorOnceAndStoresOnlyNodesAndIncidences() {

        assertTimeoutPreemptively(Duration.ofSeconds(8), () -> {
            int size = 500;
            List<Integer> keys = IntStream.range(0, size).boxed().toList();
            AtomicInteger loads = new AtomicInteger();
            var result = SupplyNetworkViabilityGraph.analyze(keys, key -> {
                loads.incrementAndGet();
                return new SupplyNetworkViabilityGraph.Node<>(false,
                        List.of(new SupplyNetworkViabilityGraph.Alternative<>("dense", true, keys)));
            }, SupplyNetworkViabilityGraph.Limits.defaults());
            assertEquals(size, loads.get());
            assertEquals(size, result.nodeCount());
            assertEquals(size * (size + 1), result.edgeCount());
            assertEquals(BLOCKED, result.status(0));
            assertTrue(result.isCircularEdge(0, size - 1));
        });

    }

    @Test
    void longChainDoesNotUseJavaCallStack() {

        int size = 20_000;
        var result = SupplyNetworkViabilityGraph.analyze(List.of(0), key ->
                new SupplyNetworkViabilityGraph.Node<>(key == size - 1, key == size - 1 ? List.of()
                        : List.of(new SupplyNetworkViabilityGraph.Alternative<>("next", true, List.of(key + 1)))),
                SupplyNetworkViabilityGraph.Limits.defaults());
        assertEquals(size, result.nodeCount());
        assertEquals(VIABLE, result.status(0));

    }

    @Test
    void interruptionIsCooperativeAndDoesNotClearInterruptFlag() {

        try {
            Thread.currentThread().interrupt();
            var result = SupplyNetworkViabilityGraph.analyze(List.of("ROOT"), key -> source(), SupplyNetworkViabilityGraph.Limits.defaults());
            assertEquals("CANCELLED", result.cutReason());
            assertEquals(UNKNOWN, result.status("ROOT"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

    }

    @Test
    void localLoaderBudgetBecomesPartialButUnexpectedDataErrorsRemainExplicit() {

        var result = SupplyNetworkViabilityGraph.analyze(List.of("ROOT"), key -> {
            throw new SupplyNetworkViabilityGraph.DiscoveryLimitException("LOCAL_WORK_LIMIT");
        }, SupplyNetworkViabilityGraph.Limits.defaults());
        assertEquals("LOCAL_WORK_LIMIT", result.cutReason());
        assertEquals(UNKNOWN, result.status("ROOT"));
        assertThrows(IllegalStateException.class, () -> SupplyNetworkViabilityGraph.analyze(List.of("ROOT"), key -> {
            throw new IllegalStateException("Missing required registration");
        }, SupplyNetworkViabilityGraph.Limits.defaults()));

    }

    private SupplyNetworkViabilityGraph.Analysis<String> solve(Map<String, SupplyNetworkViabilityGraph.Node<String>> graph, List<String> roots) {

        return SupplyNetworkViabilityGraph.analyze(roots, key -> Objects.requireNonNull(graph.get(key)), SupplyNetworkViabilityGraph.Limits.defaults());

    }

    @SafeVarargs
    private static SupplyNetworkViabilityGraph.Node<String> node(SupplyNetworkViabilityGraph.Alternative<String>... alternatives) {

        return new SupplyNetworkViabilityGraph.Node<>(false, List.of(alternatives));

    }

    private static SupplyNetworkViabilityGraph.Node<String> source() {

        return new SupplyNetworkViabilityGraph.Node<>(true, List.of());

    }

    private static SupplyNetworkViabilityGraph.Alternative<String> alternative(String id, String... dependencies) {

        return new SupplyNetworkViabilityGraph.Alternative<>(id, true, List.of(dependencies));

    }

}
