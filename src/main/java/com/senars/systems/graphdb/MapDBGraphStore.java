package com.senars.systems.graphdb;

import com.senars.core.CausalLink;
import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import com.senars.db.DatabaseManager;
import com.senars.systems.GraphDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * An implementation of the GraphDB interface using MapDB for persistence.
 * This class stores thoughts and their causal relationships in persistent maps.
 */
public class MapDBGraphStore implements GraphDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapDBGraphStore.class);
    private static final String THOUGHTS_MAP = "thoughts";
    private static final String SYMBOLIC_INDEX_MAP = "symbolic_index";
    private static final String CAUSAL_LINKS_MAP = "causal_links";


    private final DatabaseManager dbManager;
    private final ConcurrentMap<String, Thought> thoughts;
    private final ConcurrentMap<String, String> symbolicIndex; // Maps symbolic name to thought ID
    private final ConcurrentMap<String, Set<CausalLink>> causalLinks; // Maps thought ID to its outgoing causal links

    public MapDBGraphStore(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.thoughts = dbManager.getPersistentMap(THOUGHTS_MAP);
        this.symbolicIndex = dbManager.getPersistentMap(SYMBOLIC_INDEX_MAP);
        this.causalLinks = dbManager.getPersistentMap(CAUSAL_LINKS_MAP);
        LOGGER.info("MapDBGraphStore initialized with {} thoughts.", thoughts.size());
    }

    @Override
    public void saveThought(Thought thought) {
        thoughts.put(thought.id(), thought);
        // Index schemas by their symbolic name for faster lookup
        if (thought.metadata().type() == ThoughtType.SCHEMA && thought.content().symbolic() != null) {
            symbolicIndex.put(thought.content().symbolic(), thought.id());
        }

        // Save causal links
        Set<CausalLink> causalLinksSet = thought.metadata().causalLinks();
        if (causalLinksSet != null && !causalLinksSet.isEmpty()) {
            for (CausalLink link : causalLinksSet) {
                // Only save links where this thought is the source
                if (link.sourceThoughtId().equals(thought.id())) {
                    addCausalLink(link);
                }
            }
        }

        dbManager.commit(); // Commit the transaction
    }

    @Override
    public Optional<Thought> getThoughtById(String id) {
        return Optional.ofNullable(thoughts.get(id));
    }

    @Override
    public List<Thought> getTrace(String thoughtId) {
        ArrayList<Thought> trace = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // Use a stack for iterative depth-first traversal to build the path
        //Stack<String> stack = new Stack<>();

        Optional<Thought> startNode = getThoughtById(thoughtId);
        if (startNode.isEmpty()) {
            return trace; // Return empty list if start thought doesn't exist
        }

        // We want to find all paths leading to the startNode, so we traverse backwards
        // This is complex with multiple parents. A simpler approach for SeNARS is
        // to assume a primary trace path.

        String currentId = thoughtId;
        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId);
            Optional<Thought> currentThoughtOpt = getThoughtById(currentId);
            if (currentThoughtOpt.isPresent()) {
                Thought currentThought = currentThoughtOpt.get();
                trace.add(currentThought);

                // For simplicity, we follow the first parent in the trace.
                // This assumes a singly-linked-list style of primary provenance.
                if (currentThought.metadata().trace() != null && !currentThought.metadata().trace().isEmpty()) {
                    currentId = currentThought.metadata().trace().getFirst();
                } else {
                    currentId = null; // End of trace
                }
            } else {
                currentId = null; // End of trace
            }
        }

        Collections.reverse(trace); // Reverse to get the trace from origin to target
        return trace;
    }

    @Override
    public Optional<Thought> findSchemaBySymbolicName(String name) {
        String thoughtId = symbolicIndex.get(name);
        return thoughtId == null ? Optional.empty() : getThoughtById(thoughtId);
    }

    @Override
    public List<Thought> getAllThoughts() {
        return new ArrayList<>(thoughts.values());
    }

    @Override
    public void deleteThought(String thoughtId) {
        Thought thought = thoughts.remove(thoughtId);
        if (thought != null && thought.metadata().type() == ThoughtType.SCHEMA && thought.content().symbolic() != null) {
            symbolicIndex.remove(thought.content().symbolic());
        }
        // Remove all causal links involving this thought
        causalLinks.remove(thoughtId);
        // Also remove any links pointing to this thought
        for (Map.Entry<String, Set<CausalLink>> entry : causalLinks.entrySet()) {
            Set<CausalLink> updatedLinks = entry.getValue().stream()
                    .filter(link -> !link.targetThoughtId().equals(thoughtId))
                    .collect(Collectors.toSet());
            if (updatedLinks.size() != entry.getValue().size()) {
                causalLinks.put(entry.getKey(), updatedLinks);
            }
        }
        dbManager.commit();
    }

    @Override
    public Set<CausalLink> getCausalLinksFrom(String thoughtId) {
        return causalLinks.getOrDefault(thoughtId, Collections.emptySet());
    }

    @Override
    public Set<CausalLink> getCausalLinksTo(String thoughtId) {
        Set<CausalLink> incomingLinks = new HashSet<>();
        for (Set<CausalLink> links : causalLinks.values()) {
            for (CausalLink link : links) {
                if (link.targetThoughtId().equals(thoughtId)) {
                    incomingLinks.add(link);
                }
            }
        }
        return incomingLinks;
    }

    @Override
    public void addCausalLink(CausalLink link) {
        // Add the link to the source thought's outgoing links
        causalLinks.computeIfAbsent(link.sourceThoughtId(), k -> new HashSet<>()).add(link);
        dbManager.commit();
    }

    @Override
    public void removeCausalLink(CausalLink link) {
        // Remove the link from the source thought's outgoing links
        Set<CausalLink> links = causalLinks.get(link.sourceThoughtId());
        if (links != null) {
            links.remove(link);
            if (links.isEmpty()) {
                causalLinks.remove(link.sourceThoughtId());
            } else {
                causalLinks.put(link.sourceThoughtId(), links);
            }
            dbManager.commit();
        }
    }

    @Override
    public Set<Thought> getCausallyConnectedThoughts(String thoughtId, int maxDepth) {
        Set<Thought> connectedThoughts = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Queue<BfsNode> queue = new LinkedList<>();

        // Add the starting thought
        queue.add(new BfsNode(thoughtId, 0, BfsDirection.BOTH));
        visited.add(thoughtId);

        // BFS traversal for both directions up to maxDepth
        while (!queue.isEmpty()) {
            BfsNode currentNode = queue.poll();

            // Skip if we've exceeded max depth
            if (currentNode.depth() > maxDepth) {
                continue;
            }

            getThoughtById(currentNode.thoughtId()).ifPresent(connectedThoughts::add);

            // Traverse in the specified direction
            if (currentNode.direction() == BfsDirection.FORWARD || currentNode.direction() == BfsDirection.BOTH) {
                // Add outgoing links (forward direction)
                Set<CausalLink> outgoingLinks = getCausalLinksFrom(currentNode.thoughtId());
                for (CausalLink link : outgoingLinks) {
                    String targetId = link.targetThoughtId();
                    if (!visited.contains(targetId)) {
                        visited.add(targetId);
                        queue.add(new BfsNode(targetId, currentNode.depth() + 1, BfsDirection.FORWARD));
                    }
                }
            }

            if (currentNode.direction() == BfsDirection.BACKWARD || currentNode.direction() == BfsDirection.BOTH) {
                // Add incoming links (backward direction)
                Set<CausalLink> incomingLinks = getCausalLinksTo(currentNode.thoughtId());
                for (CausalLink link : incomingLinks) {
                    String sourceId = link.sourceThoughtId();
                    if (!visited.contains(sourceId)) {
                        visited.add(sourceId);
                        queue.add(new BfsNode(sourceId, currentNode.depth() + 1, BfsDirection.BACKWARD));
                    }
                }
            }
        }

        return connectedThoughts;
    }

    @Override
    public Set<Thought> performCounterfactualAnalysis(String thoughtId) {
        Set<Thought> affectedThoughts = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        // Start with the given thought
        queue.add(thoughtId);
        visited.add(thoughtId);

        // Forward traversal to find all downstream effects
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            getThoughtById(currentId).ifPresent(affectedThoughts::add);

            // Follow outgoing causal links
            Set<CausalLink> outgoingLinks = getCausalLinksFrom(currentId);
            for (CausalLink link : outgoingLinks) {
                // Only follow direct causation and indirect causation links for counterfactual analysis
                if (link.relationType() == com.senars.core.CausalRelationType.DIRECT_CAUSATION ||
                        link.relationType() == com.senars.core.CausalRelationType.INDIRECT_CAUSATION) {
                    String targetId = link.targetThoughtId();
                    if (!visited.contains(targetId)) {
                        visited.add(targetId);
                        queue.add(targetId);
                    }
                }
            }
        }

        return affectedThoughts;
    }

    @Override
    public double calculateCausalLeverage(String thoughtId) {
        // Enhanced implementation: calculate the number of downstream thoughts that would be affected
        // by a change to this thought, weighted by their salience and adjusted for distance
        Set<Thought> downstreamThoughts = performCounterfactualAnalysis(thoughtId);
        double leverage = 0.0;

        // Also consider the depth of each thought in the causal chain
        Map<String, Integer> depths = calculateCausalDepths(thoughtId);

        for (Thought thought : downstreamThoughts) {
            // Skip the original thought
            if (!thought.id().equals(thoughtId)) {
                // Weight by salience and inverse depth (closer thoughts have more leverage)
                int depth = depths.getOrDefault(thought.id(), 1);
                double depthWeight = 1.0 / depth; // Closer thoughts have higher weight
                leverage += thought.state().salience() * depthWeight;
            }
        }

        return leverage;
    }

    /**
     * Calculate the depth of each causally connected thought from the given thought
     */
    private Map<String, Integer> calculateCausalDepths(String thoughtId) {
        Map<String, Integer> depths = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<BfsNode> queue = new LinkedList<>();

        // Add the starting thought with depth 0
        queue.add(new BfsNode(thoughtId, 0, BfsDirection.FORWARD));
        depths.put(thoughtId, 0);
        visited.add(thoughtId);

        // BFS traversal forward only to calculate depths
        while (!queue.isEmpty()) {
            BfsNode currentNode = queue.poll();

            // Add outgoing links (forward direction)
            Set<CausalLink> outgoingLinks = getCausalLinksFrom(currentNode.thoughtId());
            for (CausalLink link : outgoingLinks) {
                String targetId = link.targetThoughtId();
                if (!visited.contains(targetId)) {
                    visited.add(targetId);
                    int newDepth = currentNode.depth() + 1;
                    depths.put(targetId, newDepth);
                    queue.add(new BfsNode(targetId, newDepth, BfsDirection.FORWARD));
                }
            }
        }

        return depths;
    }

    @Override
    public void persist() {
        LOGGER.info("Persist is a no-op for MapDBGraphStore. Committing transaction instead.");
        dbManager.commit();
    }

    @Override
    public void load() {
        LOGGER.info("Load is a no-op for MapDBGraphStore. Data is loaded automatically on init.");
    }

    /**
     * Direction for BFS traversal
     */
    private enum BfsDirection {
        FORWARD,
        BACKWARD,
        BOTH
    }

    /**
     * Helper class for BFS traversal
     */
    private record BfsNode(String thoughtId, int depth, BfsDirection direction) {
    }
}
