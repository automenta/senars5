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
        var e = thought.content().embedding();
        if (e == null || e.isEmpty()) {
            return; // Cannot store a thought without an embedding
        }

        embeddingStore.add(thought.id(), Embedding.from(toFloatArray(e)));
    }

    @Override
    public List<ScoredId> findSimilar(List<Double> embedding, int topK) {
        Embedding queryEmbedding = Embedding.from(toFloatArray(embedding));

        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.findRelevant(queryEmbedding, topK);

        return relevant.stream()
                .map(match -> new ScoredId(match.embeddingId(), match.score()))
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
        var d = doubleList.size();
        float[] floatArray = new float[d];
        for (int i = 0; i < d; i++) {
            floatArray[i] = doubleList.get(i).floatValue();
        }
        return floatArray;
    }
}
