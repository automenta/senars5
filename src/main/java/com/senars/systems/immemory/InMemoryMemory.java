package com.senars.systems.immemory;

import com.senars.config.AppConfig;
import com.senars.core.*;
import com.senars.effort.EffortPredictor;
import com.senars.effort.LinearTextEffortModel;
import com.senars.systems.GraphDB;
import com.senars.systems.Memory;
import com.senars.systems.VectorStore;
import com.senars.systems.graphdb.TinkerGraphDB;
import com.senars.systems.vectorstore.FileBasedEmbeddingStore;
import com.senars.systems.vectorstore.LangChain4jVectorStore;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An in-memory implementation of the Memory facade.
 * This class orchestrates an in-memory graph database and an in-memory vector store.
 * It is suitable for testing and development without requiring external databases.
 */
public class InMemoryMemory implements Memory {

    private final GraphDB graphDB;
    private final VectorStore vectorStore;

    public InMemoryMemory() {
        // For tests or scenarios without config, use a default in-memory-only path.
        this(null);
    }

    public InMemoryMemory(AppConfig config) {
        String graphDbPath = (config != null) ? config.getGraphDbFilePath() : "target/test-db/graph.json";
        String vectorStorePath = (config != null) ? config.getVectorStoreFilePath() : "target/test-db/vector_store.json";

        this.graphDB = new TinkerGraphDB(graphDbPath);
        this.vectorStore = new FileBasedEmbeddingStore(vectorStorePath);

        load();
    }

    /**
     * Seeds the memory with essential, system-level schemas upon initialization.
     */
    private void seedDefaultSchemas() {
        ThoughtContent content = new ThoughtContent(
                "Default effort prediction model based on text length.",
                EffortPredictor.EFFORT_MODEL_SCHEMA_NAME,
                null,
                null,
                new LinearTextEffortModel(0.01, 1.0),
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
    public List<Thought> retrieveSimilar(List<Double> embedding, int topK) {
        if (embedding == null || embedding.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> similarIds = vectorStore.findSimilar(embedding, topK);
        return similarIds.stream()
                .map(this::getThoughtById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<Thought> retrieveSimilar(List<Double> embedding, int topK, ThoughtType type) {
        if (embedding == null || embedding.isEmpty()) {
            return Collections.emptyList();
        }
        // Fetch more candidates to account for filtering.
        int candidatesToFetch = topK * 5;
        List<String> similarIds = vectorStore.findSimilar(embedding, candidatesToFetch);

        return similarIds.stream()
                .map(this::getThoughtById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(thought -> thought.metadata().type() == type)
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
    public void persist() {
        graphDB.persist();
        vectorStore.persist();
    }

    public void load() {
        graphDB.load();
        vectorStore.load();
        // Only seed if the database is new (i.e., empty after loading)
        if (graphDB.getAllThoughts().isEmpty()) {
            seedDefaultSchemas();
        }
    }
}
