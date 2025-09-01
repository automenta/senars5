package com.senars.systems;

import com.senars.core.Thought;

import java.util.List;
import java.util.Optional;

/**
 * Interface for the Memory Nexus, a hybrid Vector and Graph Database
 * responsible for storing and retrieving all Thought objects.
 */
public interface Memory {

    /**
     * Saves a Thought to the memory system.
     * This could be an insert or an update operation.
     *
     * @param thought The Thought to save.
     */
    void saveThought(Thought thought);

    /**
     * Retrieves a Thought by its unique ID.
     *
     * @param id The unique ID of the Thought.
     * @return An Optional containing the Thought if found, otherwise empty.
     */
    Optional<Thought> getThoughtById(String id);

    /**
     * Performs a semantic search to find Thoughts with content similar to the given embedding.
     *
     * @param embedding The vector embedding to search against.
     * @param topK The maximum number of similar Thoughts to return.
     * @return A list of the most similar Thoughts found.
     */
    List<Thought> retrieveSimilar(List<Double> embedding, int topK);

    /**
     * Traverses the provenance graph to retrieve the chain of Thoughts that led to a specific Thought.
     *
     * @param thoughtId The ID of the target Thought.
     * @return An ordered list of Thoughts representing the trace, from origin to the target.
     */
    List<Thought> getTrace(String thoughtId);

    /**
     * Finds a SCHEMA thought by its unique symbolic name.
     *
     * @param name The symbolic name to search for (e.g., "senars:effort_prediction_model_v1").
     * @return An Optional containing the SCHEMA Thought if found, otherwise empty.
     */
    Optional<Thought> findSchemaBySymbolicName(String name);
}
