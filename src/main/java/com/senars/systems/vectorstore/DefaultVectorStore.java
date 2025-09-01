package com.senars.systems.vectorstore;

import com.senars.core.Thought;
import com.senars.db.DatabaseManager;
import com.senars.systems.VectorStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The default implementation of the VectorStore interface.
 * It uses a persistent MapDB-backed EmbeddingStore for durability and semantic search.
 */
public class DefaultVectorStore implements VectorStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultVectorStore.class);

    private final MapDBEmbeddingStore embeddingStore;

    public DefaultVectorStore(DatabaseManager dbManager) {
        this.embeddingStore = new MapDBEmbeddingStore(dbManager);
    }

    @Override
    public void add(Thought thought) {
        if (thought.content().embedding() == null || thought.content().embedding().isEmpty()) {
            return; // Cannot store a thought without an embedding
        }

        Embedding embedding = Embedding.from(toFloatArray(thought.content().embedding()));
        embeddingStore.add(thought.id(), embedding);
    }

    @Override
    public List<String> findSimilar(List<Double> embedding, int topK) {
        Embedding queryEmbedding = Embedding.from(toFloatArray(embedding));

        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, topK);

        return relevant.stream()
                .map(EmbeddingMatch::embeddingId)
                .collect(Collectors.toList());
    }

    @Override
    public void remove(String thoughtId) {
        embeddingStore.remove(thoughtId);
        LOGGER.debug("Removed embedding for thoughtId: {}", thoughtId);
    }

    @Override
    public void persist() {
        // No-op, persistence is handled by MapDB transactions
        LOGGER.info("Persist is a no-op for DefaultVectorStore.");
    }

    @Override
    public void load() {
        // No-op, loading is handled by MapDB on init
        LOGGER.info("Load is a no-op for DefaultVectorStore.");
    }

    private float[] toFloatArray(List<Double> doubleList) {
        if (doubleList == null) {
            return null;
        }
        float[] floatArray = new float[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            floatArray[i] = doubleList.get(i).floatValue();
        }
        return floatArray;
    }
}
