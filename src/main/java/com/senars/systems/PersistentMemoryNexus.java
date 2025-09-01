package com.senars.systems;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMetadata;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import com.senars.core.ThoughtOrigin;
import com.senars.systems.graphdb.TinkerGraphDB;
import com.senars.systems.vectorstore.FileBasedEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class PersistentMemoryNexus implements IMemoryNexus {

    private final TinkerGraphDB graphDB;
    private final FileBasedEmbeddingStore embeddingStore;
    private final EmbeddingModel embeddingModel;

    public PersistentMemoryNexus(String graphDbPath, String embeddingStorePath, EmbeddingModel embeddingModel) {
        this.graphDB = new TinkerGraphDB(graphDbPath);
        this.embeddingStore = new FileBasedEmbeddingStore(embeddingStorePath, embeddingModel);
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void saveThought(Thought thought) {
        // Save to graph
        Map<String, Object> properties = new HashMap<>();
        properties.put("text", thought.content().text());
        properties.put("symbolic", thought.content().symbolic());
        properties.put("type", thought.metadata().type().name());
        properties.put("origin", thought.metadata().origin().name());
        properties.put("trace", String.join(",", thought.metadata().trace()));
        properties.put("timestamp", thought.metadata().timestamp().toString());
        properties.put("clarity", thought.state().clarity());
        properties.put("salience", thought.state().salience());
        properties.put("activation", thought.state().activation());

        if (thought.content().embedding() != null && !thought.content().embedding().isEmpty()) {
            String embeddingString = thought.content().embedding().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            properties.put("embedding", embeddingString);
        }

        graphDB.addVertex(thought.id(), "Thought", properties);

        // Create edges for trace
        for (String tracedId : thought.metadata().trace()) {
            graphDB.addEdge(thought.id(), tracedId, "traces");
        }

        // Save to vector store if embedding is present
        if (thought.content().embedding() != null && !thought.content().embedding().isEmpty()) {
            embeddingStore.add(thought.id(), thought.content().text());
        }
    }

    @Override
    public Optional<Thought> getThoughtById(String id) {
        return graphDB.getVertex(id).map(this::vertexToThought);
    }

    @Override
    public List<Thought> retrieveSimilar(List<Double> embedding, int topK) {
        Embedding queryEmbedding = Embedding.from(embedding.stream().map(Double::floatValue).collect(Collectors.toList()));
        List<String> similarIds = embeddingStore.findSimilar(queryEmbedding, topK);
        return similarIds.stream()
                .map(this::getThoughtById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<Thought> getTrace(String thoughtId) {
        List<Thought> trace = new ArrayList<>();
        Optional<Thought> currentThought = getThoughtById(thoughtId);
        while (currentThought.isPresent()) {
            trace.add(currentThought.get());
            List<String> traceIds = currentThought.get().metadata().trace();
            if (traceIds.isEmpty()) {
                break;
            }
            // This implementation assumes a single line of trace for simplicity.
            currentThought = getThoughtById(traceIds.get(0));
        }
        Collections.reverse(trace);
        return trace;
    }

    @Override
    public Optional<Thought> findSchemaBySymbolicName(String name) {
        return graphDB.getVerticesByProperty("symbolic", name).stream()
                .findFirst()
                .map(this::vertexToThought);
    }

    public void persist() {
        embeddingStore.persist();
        graphDB.persist();
    }

    private Thought vertexToThought(Vertex v) {
        List<Double> embedding = null;
        if (v.property("embedding").isPresent()) {
            String embeddingString = (String) v.property("embedding").value();
            if (embeddingString != null && !embeddingString.isEmpty()) {
                embedding = Arrays.stream(embeddingString.split(","))
                        .map(Double::valueOf)
                        .collect(Collectors.toList());
            }
        }
        ThoughtContent content = new ThoughtContent(
                (String) v.property("text").value(),
                (String) v.property("symbolic").value(),
                embedding, // embedding is now retrieved from graph
                null,
                null,
                null
        );
        ThoughtState state = new ThoughtState(
                (Double) v.property("clarity").value(),
                (Double) v.property("salience").value(),
                (Double) v.property("activation").value()
        );
        ThoughtMetadata metadata = new ThoughtMetadata(
                ThoughtType.valueOf((String) v.property("type").value()),
                ThoughtOrigin.valueOf((String) v.property("origin").value()),
                Arrays.asList(((String) v.property("trace").value()).split(",")),
                Instant.parse((String) v.property("timestamp").value())
        );
        return new Thought(v.id().toString(), content, state, metadata);
    }
}
