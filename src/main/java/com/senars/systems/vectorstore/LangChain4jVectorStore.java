package com.senars.systems.vectorstore;

import com.senars.core.Thought;
import com.senars.systems.VectorStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An in-memory implementation of the VectorStore interface using LangChain4j's
 * efficient in-memory embedding store.
 */
public class LangChain4jVectorStore implements VectorStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(LangChain4jVectorStore.class);

    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;

    public LangChain4jVectorStore() {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
    }

    @Override
    public void add(Thought thought) {
        if (thought.content().embedding() == null || thought.content().embedding().isEmpty()) {
            return; // Cannot store a thought without an embedding
        }

        Embedding embedding = Embedding.from(toFloatArray(thought.content().embedding()));

        // We store the thought's text as the content of the segment for potential future use,
        // but the ID is the crucial part for linking back to the GraphDB.
        TextSegment segment = TextSegment.from(
                thought.content().text() != null ? thought.content().text() : "",
                new dev.langchain4j.data.document.Metadata());

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
        // The default InMemoryEmbeddingStore in LangChain4j does not support removal of individual embeddings.
        // This is a known limitation of this specific implementation. For a production system,
        // a different EmbeddingStore (like Chroma, Milvus, etc.) that supports deletion would be required.
        LOGGER.warn("remove(thoughtId) is not supported by LangChain4jVectorStore and has been ignored for thoughtId: {}", thoughtId);
    }

    @Override
    public void persist() {
        LOGGER.warn("persist() is not supported by the non-persistent LangChain4jVectorStore.");
    }

    @Override
    public void load() {
        LOGGER.warn("load() is not supported by the non-persistent LangChain4jVectorStore.");
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
