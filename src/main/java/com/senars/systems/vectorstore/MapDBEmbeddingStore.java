package com.senars.systems.vectorstore;

import com.senars.db.DatabaseManager;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * An implementation of LangChain4j's EmbeddingStore interface that uses MapDB for persistence.
 * This provides a durable, serverless vector store.
 */
public class MapDBEmbeddingStore implements EmbeddingStore<TextSegment> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapDBEmbeddingStore.class);
    private static final String EMBEDDINGS_MAP = "embeddings";
    private final ConcurrentMap<String, float[]> embeddings;
    private final DatabaseManager dbManager;

    public MapDBEmbeddingStore(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.embeddings = dbManager.getPersistentMap(EMBEDDINGS_MAP);
        LOGGER.info("MapDBEmbeddingStore initialized with {} embeddings.", this.embeddings.size());
    }

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        embeddings.put(id, embedding.vector());
        dbManager.commit();
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        // The textSegment is not stored in this implementation, but the ID is linked to the thought in the GraphDB.
        return add(embedding);
    }

    @Override
    public List<String> addAll(List<Embedding> list) {
        List<String> ids = list.stream().map(embedding -> UUID.randomUUID().toString()).collect(Collectors.toList());
        var d = list.size();
        for (int i = 0; i < d; i++) {
            embeddings.put(ids.get(i), list.get(i).vector());
        }
        dbManager.commit();
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> list, List<TextSegment> list1) {
        return addAll(list);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        // Use a min-heap to keep track of the top N most relevant entries.
        PriorityQueue<EmbeddingMatch<TextSegment>> queue = new PriorityQueue<>(
                maxResults,
                Comparator.comparingDouble(EmbeddingMatch::score) // Min-heap compares scores directly
        );

        for (Map.Entry<String, float[]> entry : embeddings.entrySet()) {
            double cosineSimilarity = cosineSimilarity(referenceEmbedding.vector(), entry.getValue());
            double score = RelevanceScore.fromCosineSimilarity(cosineSimilarity);

            if (score >= minScore) {
                // The text segment is null because we don't store it here. The ID is the important part.
                EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(score, entry.getKey(), Embedding.from(entry.getValue()), null);
                queue.add(match);
                if (queue.size() > maxResults) {
                    queue.poll(); // Remove the element with the lowest score
                }
            }
        }

        return queue.stream()
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .toList();
    }

    public void clear() {
        embeddings.clear();
        dbManager.commit();
    }

    public void remove(String id) {
        embeddings.remove(id);
        dbManager.commit();
    }

    private double cosineSimilarity(float[] v1, float[] v2) {
        var d = v1.length;
        if (v1 == null || v2 == null || d != v2.length || d == 0) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < d; i++) {
            var i1 = v1[i];
            var i2 = v2[i];
            dotProduct += i1 * i2;
            normA += i1 * i1;
            normB += i2 * i2;
        }
        if (normA == 0.0 || normB == 0.0)
            return 0.0;
        else
            return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
