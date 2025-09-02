package com.senars.systems;

import com.senars.core.Thought;
import com.senars.systems.vectorstore.ScoredId;

import java.util.List;

/**
 * Interface for a Vector Store component responsible for storing and searching
 * over vector embeddings of Thoughts.
 */
public interface VectorStore {

    /**
     * Adds a new thought's embedding to the store.
     * If an embedding for the given thoughtId already exists, it should be updated.
     *
     * @param thought The thought to add.
     */
    void add(Thought thought);

    /**
     * Performs a semantic search to find the IDs of Thoughts with content
     * similar to the given embedding.
     *
     * @param embedding The vector embedding to search against.
     * @param topK      The maximum number of similar Thought IDs to return.
     * @return A list of the most similar Thought IDs found, paired with their score and ordered by similarity.
     */
    List<ScoredId> findSimilar(List<Double> embedding, int topK);

    /**
     * Removes a thought's embedding from the store.
     *
     * @param thoughtId The ID of the thought to remove.
     */
    void remove(String thoughtId);

    /**
     * Persists the current state of the vector store to its backing storage.
     */
    void persist();

    /**
     * Loads the state of the vector store from its backing storage.
     */
    void load();
}
