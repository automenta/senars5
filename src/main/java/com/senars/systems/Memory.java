package com.senars.systems;

import com.senars.core.CausalLink;
import com.senars.core.Thought;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The high-level facade for the Memory Nexus.
 * This interface orchestrates the underlying GraphDB and VectorStore components
 * to provide a unified memory system to the rest of the cognitive architecture.
 */
public interface Memory {

    /**
     * Saves a Thought to the memory system, persisting it in both the
     * graph and vector stores.
     *
     * @param thought The Thought to save.
     */
    void saveThought(Thought thought);

    /**
     * Deletes a Thought from the memory system, removing it from both
     * the graph and vector stores.
     * @param thoughtId The ID of the thought to delete.
     */
    void deleteThought(String thoughtId);

    /**
     * Retrieves a Thought by its unique ID from the graph database.
     *
     * @param id The unique ID of the Thought.
     * @return An Optional containing the Thought if found, otherwise empty.
     */
    Optional<Thought> getThoughtById(String id);

    /**
     * Performs a semantic search using the vector store and retrieves the full
     * Thought objects from the graph database.
     *
     * @param embedding The vector embedding to search against.
     * @param topK      The maximum number of similar Thoughts to return.
     * @return A list of the most similar Thoughts found, paired with their relevance score.
     */
    List<ScoredThought> retrieveSimilar(List<Double> embedding, int topK);

    /**
     * Performs a semantic search filtered by a specific ThoughtType.
     *
     * @param embedding The vector embedding to search against.
     * @param topK      The maximum number of similar Thoughts to return.
     * @param type      The ThoughtType to filter by.
     * @return A list of the most similar Thoughts of the specified type, paired with their relevance score.
     */
    List<ScoredThought> retrieveSimilar(List<Double> embedding, int topK, com.senars.core.ThoughtType type);

    /**
     * Traverses the provenance graph to retrieve the chain of Thoughts that led
     * to a specific Thought.
     *
     * @param thoughtId The ID of the target Thought.
     * @return An ordered list of Thoughts representing the trace.
     */
    List<Thought> getTrace(String thoughtId);

    /**
     * Finds a SCHEMA thought by its unique symbolic name from the graph database.
     *
     * @param name The symbolic name to search for.
     * @return An Optional containing the SCHEMA Thought if found, otherwise empty.
     */
    Optional<Thought> findSchemaBySymbolicName(String name);

    /**
     * Retrieves all thoughts from the graph database.
     * @return A list of all thoughts.
     */
    List<Thought> getAllThoughts();

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
     * Finds all thoughts that are causally connected to the specified thought,
     * traversing both forward and backward in the causal graph up to the specified depth.
     *
     * @param thoughtId The ID of the thought to start from.
     * @param maxDepth  The maximum depth to traverse in either direction.
     * @return A set of thoughts that are causally connected to the specified thought.
     */
    Set<Thought> getCausallyConnectedThoughts(String thoughtId, int maxDepth);

    /**
     * Calculates the causal leverage of a thought, which measures how much influence
     * the thought has on high-value outcomes in the causal graph.
     *
     * @param thoughtId The ID of the thought to calculate leverage for.
     * @return The causal leverage score.
     */
    double calculateCausalLeverage(String thoughtId);

    /**
     * Persists the underlying memory stores.
     */
    void persist();

    /**
     * Loads the underlying memory stores.
     */
    void load();
}
