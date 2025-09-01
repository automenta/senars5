package com.senars.systems.graphdb;

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
import java.util.Map;
import java.util.Optional;

/**
 * A file-based graph database that uses Apache TinkerPop's TinkerGraph.
 * It persists the graph to a file in GraphSON format.
 */
public class TinkerGraphDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(TinkerGraphDB.class);
    private final Graph graph;
    private final Path dbPath;

    /**
     * Initializes the graph database, loading from a file if it exists.
     * @param filePath The path to the database file.
     */
    public TinkerGraphDB(String filePath) {
        this.dbPath = Paths.get(filePath);
        this.graph = TinkerGraph.open();
        if (Files.exists(dbPath)) {
            LOGGER.info("Loading existing graph database from: {}", dbPath);
            try (InputStream is = new FileInputStream(dbPath.toFile())) {
                GraphSONReader reader = GraphSONReader.build().create();
                reader.readGraph(is, graph);
            } catch (IOException e) {
                LOGGER.error("Error loading graph database from {}", dbPath, e);
            }
        } else {
            LOGGER.info("No existing graph database found. Creating a new one.");
        }
    }

    /**
     * Adds or updates a vertex in the graph.
     * If a vertex with the given id already exists, it will be updated with the new properties.
     * @param id The unique ID of the vertex.
     * @param label The label of the vertex (e.g., "Concept", "Experience").
     * @param properties A map of properties for the vertex.
     */
    public void addVertex(String id, String label, Map<String, Object> properties) {
        // Check if vertex exists
        Optional<Vertex> existing = getVertex(id);
        Vertex v;
        v = existing.orElseGet(() -> graph.addVertex(T.id, id, T.label, label));

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            v.property(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Retrieves a vertex by its ID.
     * @param id The ID of the vertex.
     * @return An Optional containing the vertex if found.
     */
    public Optional<Vertex> getVertex(String id) {
        return graph.traversal().V(id).tryNext();
    }

    /**
     * Adds an edge between two vertices.
     * @param fromId The ID of the source vertex.
     * @param toId The ID of the target vertex.
     * @param label The label for the edge.
     */
    public void addEdge(String fromId, String toId, String label) {
        Optional<Vertex> fromV = getVertex(fromId);
        Optional<Vertex> toV = getVertex(toId);
        if (fromV.isPresent() && toV.isPresent()) {
            fromV.get().addEdge(label, toV.get());
        } else {
            LOGGER.warn("Could not create edge from {} to {}. One or both vertices not found.", fromId, toId);
        }
    }

    /**
     * Retrieves vertices connected to a given vertex by an edge with a specific label.
     * @param fromId The ID of the source vertex.
     * @param edgeLabel The label of the edge to traverse.
     * @return A list of connected vertices.
     */
    public java.util.List<Vertex> getRelatedVertices(String fromId, String edgeLabel) {
        Optional<Vertex> fromV = getVertex(fromId);
        if (fromV.isPresent()) {
            return fromV.get().graph().traversal().V(fromV.get()).out(edgeLabel).toList();
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Retrieves vertices that have an incoming edge with a specific label to a given vertex.
     * @param toId The ID of the target vertex.
     * @param edgeLabel The label of the incoming edge.
     * @return A list of source vertices.
     */
    public java.util.List<Vertex> getVerticesWithIncomingEdge(String toId, String edgeLabel) {
        Optional<Vertex> toV = getVertex(toId);
        if (toV.isPresent()) {
            return toV.get().graph().traversal().V(toV.get()).in(edgeLabel).toList();
        }
        return java.util.Collections.emptyList();
    }


    /**
     * Persists the graph to the file system.
     */
    public void persist() {
        try (OutputStream os = new FileOutputStream(dbPath.toFile())) {
            if (dbPath.getParent() != null) {
                Files.createDirectories(dbPath.getParent());
            }
            GraphSONWriter writer = GraphSONWriter.build().create();
            writer.writeGraph(os, graph);
            LOGGER.info("Successfully persisted graph database to: {}", dbPath);
        } catch (IOException e) {
            LOGGER.error("Failed to persist graph database to file: {}", dbPath, e);
        }
    }

    /**
     * Closes the graph database connection.
     */
    public void close() {
        try {
            graph.close();
        } catch (Exception e) {
            LOGGER.error("Error closing graph database.", e);
        }
    }

    public java.util.List<Vertex> getVerticesByProperty(String key, Object value) {
        return graph.traversal().V().has(key, value).toList();
    }
}
