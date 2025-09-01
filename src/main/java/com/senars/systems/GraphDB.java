package com.senars.systems;

import com.senars.core.Thought;

import java.util.List;
import java.util.Optional;

/**
 * Interface for a Graph Database component responsible for storing the core
 * Thought objects and their relationships (provenance trace).
 */
public interface GraphDB {

    /**
     * Saves a Thought to the graph database.
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
     * Traverses the provenance graph to retrieve the chain of Thoughts that led to a specific Thought.
     * This method must handle complex graphs where a thought can have multiple parents.
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

    /**
     * Retrieves all thoughts in the database.
     * Primarily for debugging and system-wide operations.
     * @return A list of all thoughts.
     */
    List<Thought> getAllThoughts();

    /**
     * Deletes a thought from the graph database.
     *
     * @param thoughtId The ID of the thought to delete.
     */
    void deleteThought(String thoughtId);

    /**
     * Persists the current state of the graph to a file.
     */
    void persist();
}
