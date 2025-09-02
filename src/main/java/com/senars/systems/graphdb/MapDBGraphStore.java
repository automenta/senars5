package com.senars.systems.graphdb;

import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import com.senars.db.DatabaseManager;
import com.senars.systems.GraphDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * An implementation of the GraphDB interface using MapDB for persistence.
 * This class stores thoughts and their relationships in persistent maps.
 */
public class MapDBGraphStore implements GraphDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapDBGraphStore.class);
    private static final String THOUGHTS_MAP = "thoughts";
    private static final String SYMBOLIC_INDEX_MAP = "symbolic_index";


    private final DatabaseManager dbManager;
    private final ConcurrentMap<String, Thought> thoughts;
    private final ConcurrentMap<String, String> symbolicIndex; // Maps symbolic name to thought ID

    public MapDBGraphStore(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.thoughts = dbManager.getPersistentMap(THOUGHTS_MAP);
        this.symbolicIndex = dbManager.getPersistentMap(SYMBOLIC_INDEX_MAP);
        LOGGER.info("MapDBGraphStore initialized with {} thoughts.", thoughts.size());
    }

    @Override
    public void saveThought(Thought thought) {
        thoughts.put(thought.id(), thought);
        // Index schemas by their symbolic name for faster lookup
        if (thought.metadata().type() == ThoughtType.SCHEMA && thought.content().symbolic() != null) {
            symbolicIndex.put(thought.content().symbolic(), thought.id());
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
        dbManager.commit();
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
}
