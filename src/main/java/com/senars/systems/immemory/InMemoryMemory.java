package com.senars.systems.immemory;

import com.senars.config.AppConfig;
import com.senars.core.*;
import com.senars.db.DatabaseManager;
import com.senars.effort.EffortPredictor;
import com.senars.effort.LinearTextEffortModel;
import com.senars.systems.GraphDB;
import com.senars.systems.Memory;
import com.senars.systems.ScoredThought;
import com.senars.systems.VectorStore;
import com.senars.systems.graphdb.MapDBGraphStore;
import com.senars.systems.vectorstore.DefaultVectorStore;
import com.senars.systems.vectorstore.ScoredId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A persistent implementation of the Memory facade.
 * This class orchestrates a MapDB-backed graph database and vector store.
 */
public class InMemoryMemory implements Memory {

    private final GraphDB graphDB;
    private final VectorStore vectorStore;
    private final DatabaseManager dbManager;

    public InMemoryMemory(AppConfig config, DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.graphDB = new MapDBGraphStore(dbManager);
        this.vectorStore = new DefaultVectorStore(dbManager);

        load();
    }

    private void seedDefaultSchemas() {
        ThoughtContent content = new ThoughtContent(
                "Default effort prediction model based on text length.",
                EffortPredictor.EFFORT_MODEL_SCHEMA_NAME,
                null,
                null,
                new LinearTextEffortModel(0.01, 1.0), // The procedural content is the model object itself
                null,
                null
        );

        ThoughtMeta metadata = new ThoughtMeta(
                ThoughtType.SCHEMA,
                ThoughtOrigin.SYSTEM,
                Collections.emptyList(),
                java.time.Instant.now()
        );

        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0);

        Thought schemaThought = new Thought(
                UUID.nameUUIDFromBytes(EffortPredictor.EFFORT_MODEL_SCHEMA_NAME.getBytes()).toString(),
                content,
                state,
                metadata
        );

        graphDB.saveThought(schemaThought);
    }

    @Override
    public void saveThought(Thought thought) {
        graphDB.saveThought(thought);
        if (thought.content().embedding() != null && !thought.content().embedding().isEmpty()) {
            vectorStore.add(thought);
        }
    }

    @Override
    public void deleteThought(String thoughtId) {
        graphDB.deleteThought(thoughtId);
        vectorStore.remove(thoughtId);
    }

    @Override
    public Optional<Thought> getThoughtById(String id) {
        return graphDB.getThoughtById(id);
    }

    @Override
    public List<ScoredThought> retrieveSimilar(List<Double> embedding, int topK) {
        if (embedding == null || embedding.isEmpty()) {
            return Collections.emptyList();
        }
        List<ScoredId> similarIds = vectorStore.findSimilar(embedding, topK);
        return similarIds.stream()
                .map(scoredId -> getThoughtById(scoredId.id())
                        .map(thought -> new ScoredThought(thought, scoredId.score())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoredThought> retrieveSimilar(List<Double> embedding, int topK, ThoughtType type) {
        if (embedding == null || embedding.isEmpty()) {
            return Collections.emptyList();
        }
        // Fetch more candidates to account for filtering.
        int candidatesToFetch = topK * 5;
        List<ScoredId> similarIds = vectorStore.findSimilar(embedding, candidatesToFetch);

        return similarIds.stream()
                .map(scoredId -> getThoughtById(scoredId.id())
                        .map(thought -> new ScoredThought(thought, scoredId.score())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(scoredThought -> scoredThought.thought().metadata().type() == type)
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public List<Thought> getTrace(String thoughtId) {
        return graphDB.getTrace(thoughtId);
    }

    @Override
    public Optional<Thought> findSchemaBySymbolicName(String name) {
        return graphDB.findSchemaBySymbolicName(name);
    }

    @Override
    public List<Thought> getAllThoughts() {
        return graphDB.getAllThoughts();
    }

    @Override
    public Set<CausalLink> getCausalLinksFrom(String thoughtId) {
        return graphDB.getCausalLinksFrom(thoughtId);
    }

    @Override
    public Set<CausalLink> getCausalLinksTo(String thoughtId) {
        return graphDB.getCausalLinksTo(thoughtId);
    }

    @Override
    public Set<Thought> getCausallyConnectedThoughts(String thoughtId, int maxDepth) {
        return graphDB.getCausallyConnectedThoughts(thoughtId, maxDepth);
    }

    @Override
    public double calculateCausalLeverage(String thoughtId) {
        return graphDB.calculateCausalLeverage(thoughtId);
    }

    @Override
    public void persist() {
        dbManager.commit();
    }

    @Override
    public void load() {
        // Data is loaded from MapDB on initialization of the stores.
        // We just need to check if we need to seed the DB.
        if (graphDB.getAllThoughts().isEmpty()) {
            seedDefaultSchemas();
        }
    }
}
