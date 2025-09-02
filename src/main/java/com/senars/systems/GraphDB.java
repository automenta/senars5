package com.senars.systems;

import com.senars.core.CausalLink;
import com.senars.core.Thought;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for a Graph Database component responsible for storing the core
 * Thought objects and their relationships (causal links).
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
     * Gets all causal links where the specified thought is the source.
     *
     * @param thoughtId The ID of the source thought.
     * @return A set of causal links originating from the specified thought.
     */
    Set<CausalLink> getCausalLinksFrom(String thoughtId);

    /**
     * Gets all causal links where the specified thought is the target.
     *
     * @param thoughtId The ID of the target thought.
     * @return A set of causal links pointing to the specified thought.
     */
    Set<CausalLink> getCausalLinksTo(String thoughtId);

    /**
     * Adds a causal link to the graph.
     *
     * @param link The causal link to add.
     */
    void addCausalLink(CausalLink link);

    /**
     * Removes a causal link from the graph.
     *
     * @param link The causal link to remove.
     */
    void removeCausalLink(CausalLink link);

    /**
     * Finds all thoughts that are causally connected to the specified thought,
     * traversing both forward and backward in the causal graph up to the specified depth.
     *
     * @param thoughtId The ID of the thought to start from.
     * @param maxDepth  The maximum depth to traverse in either direction.
     * @return A set of thoughts that are causally connected to the specified thought.
     */
    Set<Thought> getCausallyConnectedThoughts(String thoughtId, int maxDepth);

    /**
     * Performs a counterfactual analysis by simulating what would happen if a thought
     * had a different outcome. This traverses forward from the specified thought
     * and identifies all downstream effects.
     *
     * @param thoughtId The ID of the thought to simulate a different outcome for.
     * @return A set of thoughts that would be affected by the counterfactual.
     */
    Set<Thought> performCounterfactualAnalysis(String thoughtId);

    /**
     * Calculates the causal leverage of a thought, which measures how much influence
     * the thought has on high-value outcomes in the causal graph.
     *
     * @param thoughtId The ID of the thought to calculate leverage for.
     * @return The causal leverage score.
     */
    double calculateCausalLeverage(String thoughtId);

    /**
     * Persists the current state of the graph to a file.
     */
    void persist();

    /**
     * Loads the graph state from a file.
     */
    void load();
}
