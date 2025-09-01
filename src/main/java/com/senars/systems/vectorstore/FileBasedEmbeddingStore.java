package com.senars.systems.vectorstore;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
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
 */
public class FileBasedEmbeddingStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedEmbeddingStore.class);
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel embeddingModel;
    private final Path storePath;

    /**
     * Initializes the embedding store, loading from a file if it exists, otherwise creating a new one.
     * @param filePath The path to the file for storing embeddings.
     * @param embeddingModel The model to use for creating embeddings.
     */
    public FileBasedEmbeddingStore(String filePath, EmbeddingModel embeddingModel) {
        this.storePath = Paths.get(filePath);
        this.embeddingModel = embeddingModel;

        if (Files.exists(storePath)) {
            LOGGER.info("Loading existing embedding store from: {}", storePath);
            this.store = InMemoryEmbeddingStore.fromFile(storePath);
        } else {
            LOGGER.info("No existing store found. Creating new embedding store.");
            this.store = new InMemoryEmbeddingStore<>();
        }
    }

    /**
     * Adds a text segment with associated metadata to the store.
     * @param id The ID of the thought to store.
     * @param text The text of the thought.
     */
    public void add(String id, String text) {
        TextSegment segment = TextSegment.from(text, new dev.langchain4j.data.document.Metadata().add("id", id));
        Embedding embedding = embeddingModel.embed(segment).content();
        store.add(embedding, segment);
    }

    /**
     * Finds the most similar concepts to a given query text.
     * @param queryEmbedding The embedding to search for.
     * @param maxResults The maximum number of results to return.
     * @return A list of concept IDs of the most similar concepts.
     */
    public List<String> findSimilar(Embedding queryEmbedding, int maxResults) {
        List<EmbeddingMatch<TextSegment>> relevant = store.findRelevant(queryEmbedding, maxResults);

        return relevant.stream()
                .map(match -> match.embedded().metadata().getString("id"))
                .collect(Collectors.toList());
    }

    /**
     * Persists the embedding store to the file system.
     */
    public void persist() {
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
