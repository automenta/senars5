package com.senars.systems.graphdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.senars.core.*;
import com.senars.systems.GraphDB;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONReader;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class TinkerGraphDB implements GraphDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(TinkerGraphDB.class);
    private static final String TRACE_EDGE_LABEL = "trace";

    private final Graph graph;
    private final ObjectMapper objectMapper;
    private final Path dbPath;

    public TinkerGraphDB(String filePath) {
        this.dbPath = Paths.get(filePath);
        this.graph = TinkerGraph.open();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        if (Files.exists(dbPath)) {
            LOGGER.info("Loading existing graph database from: {}", dbPath);
            try (InputStream is = new FileInputStream(dbPath.toFile())) {
                GraphSONReader reader = GraphSONReader.build().create();
                reader.readGraph(is, graph);
            } catch (IOException e) {
                LOGGER.error("Error loading graph database from {}", dbPath, e);
            }
        } else {
            LOGGER.info("No existing graph database found at {}. Creating a new one.", dbPath);
        }
    }

    @Override
    public synchronized void saveThought(Thought thought) {
        try {
            Vertex v = graph.traversal().V(thought.id()).tryNext().orElseGet(() -> graph.addVertex(T.id, thought.id()));

            v.property("content_text", thought.content().text());
            v.property("content_symbolic", thought.content().symbolic());
            serializeAndStore(v, "content_perceptual", thought.content().perceptual());
            serializeAndStore(v, "content_procedural", thought.content().procedural());
            serializeAndStore(v, "content_feedback", thought.content().feedback());
            v.property("state_clarity", thought.state().clarity());
            v.property("state_salience", thought.state().salience());
            v.property("state_activation", thought.state().activation());
            v.property("meta_type", thought.metadata().type().name());
            v.property("meta_origin", thought.metadata().origin().name());
            v.property("meta_timestamp", thought.metadata().timestamp().toString());

            v.edges(org.apache.tinkerpop.gremlin.structure.Direction.OUT, TRACE_EDGE_LABEL).forEachRemaining(org.apache.tinkerpop.gremlin.structure.Edge::remove);
            if (thought.metadata().trace() != null) {
                for (String parentId : thought.metadata().trace()) {
                    Optional<Vertex> parentV = graph.traversal().V(parentId).tryNext();
                    parentV.ifPresent(parent -> v.addEdge(TRACE_EDGE_LABEL, parent));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error saving thought with id {}", thought.id(), e);
        }
    }

    @Override
    public Optional<Thought> getThoughtById(String id) {
        return graph.traversal().V(id).tryNext().map(this::vertexToThought);
    }

    @Override
    public void deleteThought(String thoughtId) {
        graph.traversal().V(thoughtId).tryNext().ifPresent(Vertex::remove);
    }

    @Override
    public List<Thought> getTrace(String thoughtId) {
        List<Thought> sortedTrace = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        if (graph.traversal().V(thoughtId).tryNext().isEmpty()) {
            return sortedTrace;
        }
        topologicalSortUtil(thoughtId, visited, sortedTrace);
        return sortedTrace;
    }

    private void topologicalSortUtil(String thoughtId, Set<String> visited, List<Thought> sortedTrace) {
        visited.add(thoughtId);
        Optional<Vertex> currentVertexOpt = graph.traversal().V(thoughtId).tryNext();
        if (currentVertexOpt.isEmpty()) return;
        Vertex currentVertex = currentVertexOpt.get();
        Iterator<Vertex> parents = currentVertex.vertices(org.apache.tinkerpop.gremlin.structure.Direction.OUT, TRACE_EDGE_LABEL);
        while (parents.hasNext()) {
            Vertex parent = parents.next();
            if (!visited.contains(parent.id().toString())) {
                topologicalSortUtil(parent.id().toString(), visited, sortedTrace);
            }
        }
        sortedTrace.add(vertexToThought(currentVertex));
    }

    @Override
    public Optional<Thought> findSchemaBySymbolicName(String name) {
        return graph.traversal().V()
                .has("meta_type", ThoughtType.SCHEMA.name())
                .has("content_symbolic", name)
                .tryNext()
                .map(this::vertexToThought);
    }

    @Override
    public List<Thought> getAllThoughts() {
        return graph.traversal().V().toList().stream()
                .map(this::vertexToThought)
                .collect(Collectors.toList());
    }

    @Override
    public void persist() {
        LOGGER.info("Persisting graph database to: {}", dbPath);
        try (OutputStream os = new FileOutputStream(dbPath.toFile())) {
            if (dbPath.getParent() != null) {
                Files.createDirectories(dbPath.getParent());
            }
            GraphSONWriter.build().create().writeGraph(os, graph);
            LOGGER.info("Successfully persisted graph database.");
        } catch (IOException e) {
            LOGGER.error("Failed to persist graph database to file: {}", dbPath, e);
        }
    }

    private void serializeAndStore(Vertex v, String key, Object obj) {
        if (obj != null) {
            try {
                String json = objectMapper.writeValueAsString(obj);
                v.property(key, json);
                v.property(key + "_class", obj.getClass().getName());
            } catch (IOException e) {
                LOGGER.error("Failed to serialize object for key {}", key, e);
            }
        }
    }

    private <X> X deserialize(Vertex v, String key, Class<? super X> defaultClass) {
        if (v.property(key).isPresent()) {
            String json = v.property(key).value().toString();
            String className = v.property(key + "_class").value().toString();
            try {
                Class<?> clazz = Class.forName(className);
                //var clazz = defaultClass;
                return (X) objectMapper.readValue(json, clazz);
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.error("Failed to deserialize object for key {}", key, e);
            }
        }
        return null;
    }

    private Thought vertexToThought(Vertex v) {
        if (v == null) return null;
        String text = v.property("content_text").isPresent() ? v.property("content_text").value().toString() : null;
        String symbolic = v.property("content_symbolic").isPresent() ? v.property("content_symbolic").value().toString() : null;
        ThoughtContent content = new ThoughtContent(
                text,
                symbolic,
                null,
                deserialize(v, "content_perceptual", Object.class),
                deserialize(v, "content_procedural", Object.class),
                deserialize(v, "content_feedback", Feedback.class)
        );
        double clarity = v.property("state_clarity").isPresent() ? (double) v.property("state_clarity").value() : 0.0;
        double salience = v.property("state_salience").isPresent() ? (double) v.property("state_salience").value() : 0.0;
        double activation = v.property("state_activation").isPresent() ? (double) v.property("state_activation").value() : 0.0;
        ThoughtState state = new ThoughtState(clarity, salience, activation);
        List<String> trace = new ArrayList<>();
        v.vertices(org.apache.tinkerpop.gremlin.structure.Direction.OUT, TRACE_EDGE_LABEL).forEachRemaining(parent -> trace.add(parent.id().toString()));
        String typeStr = v.property("meta_type").isPresent() ? v.property("meta_type").value().toString() : "BELIEF";
        String originStr = v.property("meta_origin").isPresent() ? v.property("meta_origin").value().toString() : "SYSTEM";
        String timestampStr = v.property("meta_timestamp").isPresent() ? v.property("meta_timestamp").value().toString() : Instant.now().toString();
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.valueOf(typeStr),
                ThoughtOrigin.valueOf(originStr),
                trace,
                Instant.parse(timestampStr)
        );
        return new Thought(v.id().toString(), content, state, meta);
    }
}
