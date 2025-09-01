package com.senars.systems.vectorstore;

import com.senars.core.Thought;
import com.senars.systems.VectorStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A file-based embedding store that uses LangChain4j's InMemoryEmbeddingStore with file persistence.
 * This class implements the VectorStore interface for use within the SeNARS system.
 */
public class FileBasedEmbeddingStore implements VectorStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedEmbeddingStore.class);
    private EmbeddingStore<TextSegment> store;
    private final Path storePath;

    /**
     * Initializes the embedding store, loading from a file if it exists, otherwise creating a new one.
     * @param filePath The path to the file for storing embeddings.
     */
    public FileBasedEmbeddingStore(String filePath) {
        this.storePath = Paths.get(filePath);
        // Initialize with an empty store, which will be replaced if a file is found during load.
        this.store = new InMemoryEmbeddingStore<>();
        load();
    }

    @Override
    public void add(Thought thought) {
        if (thought.content().embedding() == null || thought.content().embedding().isEmpty()) {
            return; // Cannot store a thought without an embedding
        }
        Embedding embedding = Embedding.from(toFloatArray(thought.content().embedding()));
        // The segment is not strictly necessary for the current implementation of findSimilar,
        // but it's good practice to store it for future use.
        TextSegment segment = TextSegment.from(thought.content().text() != null ? thought.content().text() : "", null);
        store.add(thought.id(), embedding);
    }

    @Override
    public List<String> findSimilar(List<Double> embedding, int topK) {
        Embedding referenceEmbedding = Embedding.from(toFloatArray(embedding));
        List<EmbeddingMatch<TextSegment>> matches = store.findRelevant(referenceEmbedding, topK);
        return matches.stream()
                .map(EmbeddingMatch::embeddingId)
                .collect(Collectors.toList());
    }

    @Override
    public void remove(String thoughtId) {
        // The default InMemoryEmbeddingStore in LangChain4j does not support removal.
        LOGGER.warn("remove(thoughtId) is not supported by FileBasedEmbeddingStore and has been ignored for thoughtId: {}", thoughtId);
    }

    @Override
    public void persist() {
        if (store instanceof InMemoryEmbeddingStore) {
            try {
                // Ensure parent directory exists
                if (storePath.getParent() != null) {
                    Files.createDirectories(storePath.getParent());
                }
                ((InMemoryEmbeddingStore<TextSegment>) store).serializeToFile(storePath);
                LOGGER.info("Successfully persisted embedding store to: {}", storePath);
            } catch (IOException e) {
                LOGGER.error("Failed to persist embedding store to file: {}", storePath, e);
            }
        }
    }

    @Override
    public void load() {
        if (Files.exists(storePath)) {
            LOGGER.info("Loading existing embedding store from: {}", storePath);
            this.store = InMemoryEmbeddingStore.fromFile(storePath);
            LOGGER.info("Successfully loaded embedding store.");
        } else {
            LOGGER.info("No existing store found at {}. A new, empty store will be used.", storePath);
        }
    }

    private float[] toFloatArray(List<Double> doubleList) {
        if (doubleList == null) {
            return new float[0];
        }
        float[] floatArray = new float[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            floatArray[i] = doubleList.get(i).floatValue();
        }
        return floatArray;
    }
}
