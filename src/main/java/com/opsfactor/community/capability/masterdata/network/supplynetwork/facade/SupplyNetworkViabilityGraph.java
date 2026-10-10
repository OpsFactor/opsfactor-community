package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade;

import java.util.*;
import java.util.function.Function;

/**
 * Request-scoped, bounded AND/OR supply graph. A cycle cannot create supply: only
 * a terminal source or an eligible alternative whose every input is proved can
 * seed the least fixed point. No path trees or recursive calls are constructed.
 *
 * <p>Supply choices are OR alternatives, but the inputs within one production
 * choice are AND requirements. Starting with an empty viable set prevents a
 * mutually dependent group from proving itself. For example, an external source
 * for C does not release A = B AND C when B still requires A.
 *
 * <p>This is structural feasibility over one caller-provided snapshot, not a
 * quantity/capacity calculation. The generic Community implementation also serves
 * the Explorer; importing it into Enterprise does not expose an alert capability
 * in Community. Every invocation owns its state and never caches a conclusion
 * derived from a particular root, ancestor path or display depth.
 */
public final class SupplyNetworkViabilityGraph<K> {

    /** UNKNOWN records missing proof after a cut; it must not be rendered as viable. */
    public enum Status { VIABLE, BLOCKED, UNKNOWN }

    /**
     * An alternative's local registration gates are separate from its input proofs.
     * A valid empty BOM may therefore seed supply, while a missing/inactive BOM
     * cannot do so merely because it has no dependencies. Duplicate inputs count
     * once; identifiers must remain unique within their output node.
     */
    public record Alternative<K>(String id, boolean eligible, List<K> dependencies) {

        public Alternative {

            Objects.requireNonNull(id);
            dependencies = List.copyOf(new LinkedHashSet<>(dependencies));

        }

    }

    public record Node<K>(boolean terminal, List<Alternative<K>> alternatives) {

        public Node {

            alternatives = List.copyOf(alternatives);
            Set<String> identifiers = new HashSet<>();
            for (Alternative<K> alternative : alternatives) {
                if (!identifiers.add(alternative.id())) throw new IllegalArgumentException("Duplicate supply alternative: " + alternative.id());
            }

        }

    }

    /** Edge budget also counts alternatives, including zero-input alternatives. */
    public record Limits(int maxNodes, int maxEdges, long maxMillis) {

        public Limits {

            if (maxNodes < 1 || maxEdges < 1 || maxMillis < 1) throw new IllegalArgumentException("Supply graph limits must be positive");

        }

        public static Limits defaults() {

            return new Limits(100_000, 1_000_000, 30_000);

        }

    }

    /**
     * Results retain the witness fixed at each node's first activation. Its inputs
     * were already proved, so following these witnesses cannot recreate a cycle.
     * Later alternatives never replace that witness. Terminal sources need no
     * alternative witness; SCC membership is a separate diagnostic annotation.
     */
    public static final class Analysis<K> {

        private final Map<K, Node<K>> nodes;
        private final Set<K> viable;
        private final Set<K> provedBlocked;
        private final Map<K, Map<String, Alternative<K>>> alternativesById;
        private final Map<K, Alternative<K>> witnesses;
        private final Map<K, String> cycleGroups;
        private final boolean complete;
        private final String cutReason;
        private final int edgeCount;

        private Analysis(Map<K, Node<K>> nodes, Set<K> viable, Set<K> provedBlocked, Map<K, Alternative<K>> witnesses,
                         Map<K, String> cycleGroups, String cutReason, int edgeCount) {

            this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
            this.viable = Set.copyOf(viable);
            this.provedBlocked = Set.copyOf(provedBlocked);
            Map<K, Map<String, Alternative<K>>> index = new HashMap<>();
            for (var entry : nodes.entrySet()) {
                Map<String, Alternative<K>> alternatives = new HashMap<>();
                for (Alternative<K> alternative : entry.getValue().alternatives()) alternatives.put(alternative.id(), alternative);
                index.put(entry.getKey(), Map.copyOf(alternatives));
            }
            this.alternativesById = Map.copyOf(index);
            this.witnesses = Map.copyOf(witnesses);
            this.cycleGroups = Map.copyOf(cycleGroups);
            this.complete = cutReason == null;
            this.cutReason = cutReason;
            this.edgeCount = edgeCount;

        }

        public Status status(K key) {

            if (viable.contains(key)) return Status.VIABLE;
            if (provedBlocked.contains(key)) return Status.BLOCKED;
            // A partial analysis never invents a negative result from an unopened frontier.
            return complete && nodes.containsKey(key) ? Status.BLOCKED : Status.UNKNOWN;

        }

        public Status alternativeStatus(K key, String alternativeId) {

            Node<K> node = nodes.get(key);
            if (node == null) return Status.UNKNOWN;
            Alternative<K> alternative = alternativesById.get(key).get(alternativeId);
            if (alternative == null) throw new IllegalArgumentException("Unknown supply alternative: " + alternativeId);
            if (!alternative.eligible()) return Status.BLOCKED;
            boolean unknown = false;
            for (K dependency : alternative.dependencies()) {
                Status status = status(dependency);
                if (status == Status.BLOCKED) return Status.BLOCKED;
                unknown |= status == Status.UNKNOWN;
            }
            return unknown ? Status.UNKNOWN : Status.VIABLE;

        }

        public String cycleGroupId(K key) { return cycleGroups.get(key); }
        /**
         * For an existing edge, equal cyclic SCCs prove a return path even when
         * the target is a sibling in the display tree rather than an ancestor.
         */
        public boolean isCircularEdge(K source, K target) {

            String group = cycleGroups.get(source);
            return group != null && group.equals(cycleGroups.get(target));

        }
        public Alternative<K> witness(K key) { return witnesses.get(key); }
        public Map<K, Node<K>> nodes() { return nodes; }
        public boolean complete() { return complete; }
        public String cutReason() { return cutReason; }
        public int nodeCount() { return nodes.size(); }
        public int edgeCount() { return edgeCount; }

    }

    private record Requirement<K>(K output, Alternative<K> alternative, int[] remaining) { }
    private record BlockingRequirement<K>(K output, boolean[] failed) { }
    private record Frame<K>(K key, Iterator<K> dependencies) { }

    /** A loader may explicitly stop bounded discovery; data/model exceptions still propagate. */
    public static final class DiscoveryLimitException extends RuntimeException {

        public DiscoveryLimitException(String reason) {

            super(Objects.requireNonNull(reason));

        }

    }

    private final Limits limits;
    private final long started = System.nanoTime();
    private String cutReason;
    private int edgeCount;

    private SupplyNetworkViabilityGraph(Limits limits) {

        this.limits = Objects.requireNonNull(limits);

    }

    /**
     * Discovers once for all roots, solves once, then identifies cycle groups.
     * Sharing descriptors across roots avoids repeated graph construction without
     * sharing mutable results between requests. The caller supplies already
     * materialized data and may signal a discovery budget cut explicitly; other
     * loader exceptions remain data/model errors and must propagate.
     */
    public static <K> Analysis<K> analyze(Collection<K> roots, Function<K, Node<K>> nodeLoader, Limits limits) {

        return new SupplyNetworkViabilityGraph<K>(limits).evaluate(roots, nodeLoader);

    }

    private Analysis<K> evaluate(Collection<K> roots, Function<K, Node<K>> nodeLoader) {

        Map<K, Node<K>> nodes = new LinkedHashMap<>();
        Set<K> discovered = new HashSet<>();
        ArrayDeque<K> pending = new ArrayDeque<>();
        for (K root : roots) {
            if (discovered.add(root)) {
                if (discovered.size() > limits.maxNodes()) { cutReason = "NODE_LIMIT"; break; }
                pending.add(root);
            }
        }
        // Node/edge cuts stop discovery but still allow the bounded loaded prefix to prove supply.
        discover: while (!pending.isEmpty() && withinTime()) {
            K key = pending.remove();
            Node<K> descriptor;
            try {
                descriptor = Objects.requireNonNull(nodeLoader.apply(key), "Supply graph node loader returned null");
            } catch (DiscoveryLimitException limit) {
                cutReason = limit.getMessage();
                break;
            }
            long descriptorEdges = descriptor.alternatives().size();
            for (Alternative<K> alternative : descriptor.alternatives()) descriptorEdges += alternative.dependencies().size();
            if ((long) edgeCount + descriptorEdges > limits.maxEdges()) { cutReason = "EDGE_LIMIT"; break; }
            edgeCount += (int) descriptorEdges;
            nodes.put(key, descriptor);
            for (Alternative<K> alternative : descriptor.alternatives()) {
                for (K dependency : alternative.dependencies()) {
                    if (!withinTime()) break discover;
                    if (discovered.contains(dependency)) continue;
                    if (discovered.size() >= limits.maxNodes()) { if (cutReason == null) cutReason = "NODE_LIMIT"; continue; }
                    discovered.add(dependency);
                    pending.add(dependency);
                }
            }
        }

        Map<K, List<K>> adjacency = new LinkedHashMap<>();
        Map<K, List<K>> reverse = new HashMap<>();
        Map<K, List<Requirement<K>>> consumers = new HashMap<>();
        Set<K> viable = new HashSet<>();
        Map<K, Alternative<K>> witnesses = new HashMap<>();
        ArrayDeque<K> activated = new ArrayDeque<>();
        buildIndexes: for (Map.Entry<K, Node<K>> entry : nodes.entrySet()) {
            if (!withinTime()) break;
            K key = entry.getKey();
            Node<K> descriptor = entry.getValue();
            Set<K> links = new LinkedHashSet<>();
            if (descriptor.terminal() && viable.add(key)) activated.add(key);
            for (Alternative<K> alternative : descriptor.alternatives()) {
                if (!withinTime()) break buildIndexes;
                // Keep ineligible edges for inspection/SCC topology. Only eligible
                // alternatives receive counters and can contribute a supply proof.
                links.addAll(alternative.dependencies());
                if (!alternative.eligible()) continue;
                Requirement<K> requirement = new Requirement<>(key, alternative, new int[] { alternative.dependencies().size() });
                if (requirement.remaining()[0] == 0 && viable.add(key)) {
                    witnesses.put(key, alternative);
                    activated.add(key);
                }
                for (K dependency : alternative.dependencies()) {
                    if (!withinTime()) break buildIndexes;
                    consumers.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(requirement);
                }
            }
            List<K> loadedLinks = links.stream().filter(nodes::containsKey).toList();
            adjacency.put(key, loadedLinks);
            for (K dependency : loadedLinks) reverse.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(key);
        }
        // Each dependency incidence decrements once: propagation is linear in the
        // loaded graph, not in the number of possible paths. viable.add fixes the
        // first witness only after all inputs activate; later cycles cannot replace
        // that acyclic proof. AND gates never become OR gates inside an SCC.
        propagateSupply: while (!activated.isEmpty() && withinTime()) {
            K source = activated.remove();
            for (Requirement<K> consumer : consumers.getOrDefault(source, List.of())) {
                if (!withinTime()) break propagateSupply;
                if (--consumer.remaining()[0] == 0 && viable.add(consumer.output())) {
                    witnesses.put(consumer.output(), consumer.alternative());
                    activated.add(consumer.output());
                }
            }
        }
        Map<K, String> groups = stronglyConnectedGroups(adjacency, reverse);
        Set<K> provedBlocked = cutReason == null ? Set.of() : proveLocalBlocks(nodes);
        return new Analysis<>(nodes, viable, provedBlocked, witnesses, groups, cutReason, edgeCount);

    }

    /**
     * On a discovery cut, local impossibility can still be proved without assuming
     * anything about unopened nodes. One impossible AND input defeats its whole
     * alternative; every OR alternative must fail before its output is blocked.
     */
    private Set<K> proveLocalBlocks(Map<K, Node<K>> nodes) {

        Map<K, int[]> remainingAlternatives = new HashMap<>();
        Map<K, List<BlockingRequirement<K>>> consumers = new HashMap<>();
        Set<K> blocked = new HashSet<>();
        ArrayDeque<K> pending = new ArrayDeque<>();
        for (var entry : nodes.entrySet()) {
            if (!withinTime()) return blocked;
            if (entry.getValue().terminal()) continue;
            int count = 0;
            for (Alternative<K> alternative : entry.getValue().alternatives()) {
                if (!withinTime()) return blocked;
                if (!alternative.eligible()) continue;
                count++;
                BlockingRequirement<K> requirement = new BlockingRequirement<>(entry.getKey(), new boolean[1]);
                for (K dependency : alternative.dependencies()) {
                    if (!withinTime()) return blocked;
                    consumers.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(requirement);
                }
            }
            remainingAlternatives.put(entry.getKey(), new int[] { count });
            if (count == 0 && blocked.add(entry.getKey())) pending.add(entry.getKey());
        }
        while (!pending.isEmpty()) {
            if (!withinTime()) return blocked;
            for (BlockingRequirement<K> consumer : consumers.getOrDefault(pending.remove(), List.of())) {
                if (!withinTime()) return blocked;
                if (!consumer.failed()[0]) {
                    consumer.failed()[0] = true;
                    if (--remainingAlternatives.get(consumer.output())[0] == 0 && blocked.add(consumer.output())) pending.add(consumer.output());
                }
            }
        }
        return blocked;

    }

    /**
     * Iterative Kosaraju avoids stack overflow on long chains and detects sibling
     * cross-cycles that an ancestor-only recursion guard would miss. Components
     * describe topology, including inspectable ineligible alternatives; they never
     * grant viability or collapse AND requirements into one reachable source.
     * On an interrupted traversal, no partial SCC partition is published.
     */
    private Map<K, String> stronglyConnectedGroups(Map<K, List<K>> adjacency, Map<K, List<K>> reverse) {

        Set<K> seen = new HashSet<>();
        List<K> finishOrder = new ArrayList<>();
        ArrayDeque<Frame<K>> stack = new ArrayDeque<>();
        for (K key : adjacency.keySet()) {
            if (!withinTime()) return Map.of();
            if (!seen.add(key)) continue;
            stack.push(new Frame<>(key, adjacency.get(key).iterator()));
            while (!stack.isEmpty()) {
                if (!withinTime()) return Map.of();
                Frame<K> frame = stack.peek();
                if (frame.dependencies().hasNext()) {
                    K dependency = frame.dependencies().next();
                    if (seen.add(dependency)) stack.push(new Frame<>(dependency, adjacency.getOrDefault(dependency, List.of()).iterator()));
                } else finishOrder.add(stack.pop().key());
            }
        }
        seen.clear();
        Map<K, String> groups = new HashMap<>();
        ArrayDeque<K> pending = new ArrayDeque<>();
        for (int index = finishOrder.size() - 1; index >= 0; index--) {
            if (!withinTime()) return Map.of();
            K first = finishOrder.get(index);
            if (!seen.add(first)) continue;
            List<K> component = new ArrayList<>();
            pending.push(first);
            while (!pending.isEmpty()) {
                if (!withinTime()) return Map.of();
                K key = pending.pop();
                component.add(key);
                for (K predecessor : reverse.getOrDefault(key, List.of())) if (seen.add(predecessor)) pending.push(predecessor);
            }
            if (component.size() > 1 || adjacency.getOrDefault(first, List.of()).contains(first)) {
                String group = "cycle-" + (groups.size() + 1);
                for (K key : component) groups.put(key, group);
            }
        }
        return groups;

    }

    /**
     * Cooperative cancellation/deadline cuts preserve the interrupt flag and
     * already established proofs. Unproved nodes remain UNKNOWN; a budget limit
     * is not evidence that their supply network is blocked.
     */
    private boolean withinTime() {

        if (Thread.currentThread().isInterrupted()) { cutReason = "CANCELLED"; return false; }
        if ((System.nanoTime() - started) / 1_000_000 >= limits.maxMillis()) { cutReason = "TIME_LIMIT"; return false; }
        return true;

    }

}
